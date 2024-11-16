package middle;

import frontend.Calculator.Calculator;
import frontend.SymbolTable.Symbol;
import frontend.SymbolTable.SymbolTable;
import frontend.SyntaxTree.SyntaxNode;
import middle.Type.*;
import middle.Value.*;
import middle.Value.Instruction.*;
import middle.Value.Module;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Spliterator;
import java.util.Stack;

public class IRGenerator3 {
    private SyntaxNode root;
    private HashMap<Integer, SymbolTable> symbolTables;
    private Stack<SymbolTable> stack;
    private int level;
    private String curFuncType;
    private int virtualReg;
    private Module module;
    private Function curFunction;
    private BasicBlock curBasicBlock;
    
    public IRGenerator3(SyntaxNode root) {
        this.root = root;
        this.symbolTables = new HashMap<>();
        this.stack = new Stack<>();
        this.level = 0;
        this.curFuncType = null;
        this.virtualReg = 0;
        this.module = new Module();
        this.curFunction = null;
        this.curBasicBlock = null;
        initializeSymbolTable();
    }
    
    public void generateModule() {
        ArrayList<SyntaxNode> children = root.getChildren();
        for (SyntaxNode child : children) {
            if (child.getName().equals("Decl")) {
                generateDecl(child);
            } else if (child.getName().equals("FuncDef")) {
                generateFuncDef(child);
            } else {
                generateMainFuncDef(child);
            }
        }
        try (BufferedWriter stdout = new BufferedWriter(new FileWriter("D:\\BUAA_Compile_2024\\homework5\\src\\llvm_ir.txt"))) {
            stdout.write(module.toString());
        } catch (Exception ignored) {
        
        }
    }
    
    public void generateDecl(SyntaxNode node) {
        if (node.getChildren().get(0).getName().equals("ConstDecl")) {
            generateConstDecl(node.getChildren().get(0));
        } else {
            generateVarDecl(node.getChildren().get(0));
        }
    }
    
    public void generateConstDecl(SyntaxNode node) {
        String type = node.getChildren().get(1).getName(); //int | char
        for (SyntaxNode child : node.getChildren()) {
            if (child.getName().equals("ConstDef")) {
                generateConstDef(child, type);
            }
        }
    }
    
    public void generateVarDecl(SyntaxNode node) {
        String type = node.getChildren().get(0).getName();
        for (SyntaxNode child : node.getChildren()) {
            if (child.getName().equals("VarDef")) {
                generateVarDef(child, type);
            }
        }
    }
    
    public void generateConstDef(SyntaxNode node, String type) {
        if (curFuncType == null) { //全局变量
            String name = node.getChildren().get(0).getName(); //变量名称
            Symbol symbol = new Symbol(name, "const", type, getStackLevel(stack.peek()), true);
            stack.peek().addSymbol(symbol);
            if (node.getChildren().get(1).getName().equals("[")) { //数组变量
                symbol.setIsArray(true);
                SyntaxNode constExpNode = node.getChildren().get(2);
                int arrayLength = Calculator.calConstExp(constExpNode, stack);
                symbol.setArrayLength(arrayLength);
                generateConstInitVal(node.getChildren().get(5), symbol);
            } else { //非数组变量
                generateConstInitVal(node.getChildren().get(2), symbol);
            }
            module.addGlobalVar(new GlobalVar(symbol)); //加入一个全局变量
        } else { //局部变量
            String name = node.getChildren().get(0).getName(); //变量名称
            Symbol symbol = new Symbol(name, "const", type, getStackLevel(stack.peek()), false);
            stack.peek().addSymbol(symbol);
            if (node.getChildren().get(1).getName().equals("[")) { //数组变量
                /*TODO*/
                symbol.setIsArray(true);
                SyntaxNode constExpNode = node.getChildren().get(2);
                int arrayLength = Calculator.calConstExp(constExpNode, stack);
                symbol.setArrayLength(arrayLength);
                curBasicBlock.addInstruction(new AllocaInstr(new ArrayType(arrayLength, type.equals("int") ? new Integer32Type() : new Integer8Type()), "%" + virtualReg));
                virtualReg++; //申请一个[arrayLength x type]的数组空间
                
                generateConstInitVal(node.getChildren().get(5), symbol);
                
                
            } else { //非数组局部变量
                generateConstInitVal(node.getChildren().get(2), symbol);
                curBasicBlock.addInstruction(new AllocaInstr(type.equals("int") ? new Integer32Type() : new Integer8Type(), "%" + virtualReg));
                curBasicBlock.addInstruction(new StoreInstr(type.equals("int") ? new Integer32Type() : new Integer8Type(), "%" + virtualReg, symbol.getValue()));
                virtualReg++;
            }
        }
    }
    
    public void generateVarDef(SyntaxNode node, String type) {
        if (curFuncType == null) {
            ArrayList<SyntaxNode> children = node.getChildren();
            String name = node.getChildren().get(0).getName();
            Symbol symbol = new Symbol(name, "var", type, getStackLevel(stack.peek()), true);
            stack.peek().addSymbol(symbol);
            if (children.size() > 1 && node.getChildren().get(1).getName().equals("[")) { //数组变量
                symbol.setIsArray(true);
                SyntaxNode constExpNode = node.getChildren().get(2);
                /*TODO 现在是优化后的版本 记得做一个优化开关optimize 使得可以输出优化前和优化后的代码*/
                int arrayLength = Calculator.calConstExp(constExpNode, stack);
                symbol.setArrayLength(arrayLength);
                if (node.getLastChild().getName().equals("InitVal")) { //赋值了
                    generateInitVal(node.getLastChild(), symbol);
                } else { //没有赋值，默认值为0
                    ArrayList<Integer> values = new ArrayList<>();
                    for (int i = 0; i < symbol.getArrayLength(); i++) {
                        values.add(0);
                    }
                    symbol.setValues(values);
                }
            } else { //非数组变量
                if (node.getLastChild().getName().equals("InitVal")) { //赋值了
                    generateInitVal(node.getLastChild(), symbol);
                } else { //没有赋值，默认值为0
                    symbol.setValue(0);
                }
            }
            module.addGlobalVar(new GlobalVar(symbol)); //新增一个全局变量
        } else { //局部变量
            ArrayList<SyntaxNode> children = node.getChildren();
            String name = node.getChildren().get(0).getName(); //变量名称
            Symbol symbol = new Symbol(name, "var", type, getStackLevel(stack.peek()), false);
            stack.peek().addSymbol(symbol);
            if (children.size() > 1 && node.getChildren().get(1).getName().equals("[")) { //数组变量
                symbol.setIsArray(true);
                SyntaxNode constExpNode = node.getChildren().get(2);
                int arrayLength = Calculator.calConstExp(constExpNode, stack);
                symbol.setArrayLength(arrayLength);
                curBasicBlock.addInstruction(new AllocaInstr(new ArrayType(arrayLength, type.equals("int") ? new Integer32Type() : new Integer8Type()), "%" + virtualReg));
                virtualReg++; //申请一个[arrayLength x type]的数组空间
                if (node.getLastChild().getName().equals("InitVal")) { //赋值了
                    generateInitVal(node.getLastChild(), symbol);
                } else { //没有赋值，默认值为0
                    ArrayList<Integer> values = new ArrayList<>();
                    for (int i = 0; i < symbol.getArrayLength(); i++) {
                        values.add(0);
                    }
                    symbol.setValues(values);
                }
            } else { //非数组局部变量
                if (node.getLastChild().getName().equals("InitVal")) { //赋值了
                    generateInitVal(node.getLastChild(), symbol);
                } else { //没有赋值，默认值为0
                    symbol.setValue(0);
                }
                curBasicBlock.addInstruction(new AllocaInstr(type.equals("int") ? new Integer32Type() : new Integer8Type(), "%" + virtualReg));
                curBasicBlock.addInstruction(new StoreInstr(type.equals("int") ? new Integer32Type() : new Integer8Type(), "%" + virtualReg, symbol.getValue()));
                virtualReg++;
            }
        }
    }
    
    
    public void generateConstInitVal(SyntaxNode node, Symbol symbol) {
        if (!symbol.getIsArray()) { //非数组
            int result = Calculator.calConstExp(node.getChildren().get(0), stack);
            symbol.setValue(result); //符号表有初值
        } else { //数组
            int arrayLength = symbol.getArrayLength();
            ArrayList<SyntaxNode> children = node.getChildren();
            ArrayList<Integer> values = new ArrayList<>();//作为数组的变量的值
            int index = 0;
            for (SyntaxNode child : children) { //看有多少是有初值的
                if (child.getName().equals("ConstExp")) {
                    int result = Calculator.calConstExp(child, stack);
                    values.add(result);
                    if (curFunction != null) { //局部变量数组，初值进行getElementPtr并进行store
                        curBasicBlock.addInstruction(new GetElementInstr(new ArrayType(symbol.getArrayLength(), symbol.getType()), "%" + virtualReg, index, "%" + (virtualReg - 1)));
                        curBasicBlock.addInstruction(new StoreInstr(symbol.getType().equals("int") ? new Integer32Type() : new Integer8Type(), "%" + virtualReg, result));
                        virtualReg++;
                        index++;
                    }
                } else if (child.getName().equals("StringConst")) {
                    String string = child.getChildren().get(0).getName(); //得到字符串常量
                    string = string.substring(1, string.length() - 1);
                    for (int i = 0; i < string.length(); i++) {
                        int result = 0;
                        if (string.charAt(i) == '\\') { //考虑转义
                            i++;
                            if (string.charAt(i) == '\"') {
                                result = 34;
                            } else if (string.charAt(i) == '\'') {
                                result = 39;
                            } else if (string.charAt(i) == '\\') {
                                result = 92;
                            }
                        } else {
                            result = string.charAt(i);
                        }
                        values.add(result);
                        if (curFunction != null) { //局部变量数组，初值进行getElementPtr并进行store
                            curBasicBlock.addInstruction(new GetElementInstr(new ArrayType(symbol.getArrayLength(), symbol.getType()), "%" + virtualReg, index, "%" + (virtualReg - 1)));
                            curBasicBlock.addInstruction(new StoreInstr(symbol.getType().equals("int") ? new Integer32Type() : new Integer8Type(), "%" + virtualReg, result));
                            virtualReg++;
                            index++;
                        }
                    }
                }
            }
            for (int i = values.size(); i < arrayLength; i++) { //补足未初始化的元素
                values.add(0);
            }
            symbol.setValues(values); //给元素赋值
        }
    }
    
    public void generateInitVal(SyntaxNode node, Symbol symbol) {
        if (!symbol.getIsArray()) { //非数组
            int result = Calculator.calConstExp(node.getChildren().get(0), stack);
            symbol.setValue(result);
        } else { //数组
            int arrayLength = symbol.getArrayLength();
            ArrayList<SyntaxNode> children = node.getChildren();
            ArrayList<Integer> values = new ArrayList<>();//作为数组的变量的值
            int index = 0;
            for (SyntaxNode child : children) { //看有多少是有初值的
                if (child.getName().equals("Exp")) {
                    int result = Calculator.calConstExp(child, stack);
                    values.add(result);
                    if (curFunction != null) { //局部变量数组，初值进行getElementPtr并进行store
                        curBasicBlock.addInstruction(new GetElementInstr(new ArrayType(symbol.getArrayLength(), symbol.getType()), "%" + virtualReg, index, "%" + (virtualReg - 1)));
                        curBasicBlock.addInstruction(new StoreInstr(symbol.getType().equals("int") ? new Integer32Type() : new Integer8Type(), "%" + virtualReg, result));
                        virtualReg++;
                        index++;
                    }
                } else if (child.getName().equals("StringConst")) {
                    String string = child.getChildren().get(0).getName(); //得到字符串常量
                    string = string.substring(1, string.length() - 1);
                    for (int i = 0; i < string.length(); i++) {
                        int result = 0;
                        if (string.charAt(i) == '\\') { //考虑转义
                            i++;
                            if (string.charAt(i) == '\"') {
                                result = 34;
                            } else if (string.charAt(i) == '\'') {
                                result = 39;
                            } else if (string.charAt(i) == '\\') {
                                result = 92;
                            }
                        } else {
                            result = string.charAt(i);
                        }
                        values.add(result);
                        if (curFunction != null) { //局部变量数组，初值进行getElementPtr并进行store
                            curBasicBlock.addInstruction(new GetElementInstr(new ArrayType(symbol.getArrayLength(), symbol.getType()), "%" + virtualReg, index, "%" + (virtualReg - 1)));
                            curBasicBlock.addInstruction(new StoreInstr(symbol.getType().equals("int") ? new Integer32Type() : new Integer8Type(), "%" + virtualReg, result));
                            virtualReg++;
                            index++;
                        }
                    }
                }
            }
            for (int i = values.size(); i < arrayLength; i++) { //补足未初始化的元素
                values.add(0);
            }
            symbol.setValues(values); //给元素赋值
        }
    }
    
    
    public void generateFuncDef(SyntaxNode node) {
        ArrayList<SyntaxNode> children = node.getChildren();
        curFuncType = node.getChildren().get(0).getChildren().get(0).getName();
        String name = node.getChildren().get(1).getName();
        Symbol symbol = new Symbol(name, "func", curFuncType, getStackLevel(stack.peek()));
        stack.peek().addSymbol(symbol);
        SymbolTable symbolTable = new SymbolTable();
        stack.push(symbolTable);
        symbolTables.put(level + 1, symbolTable);
        symbol.setSymbolTable(symbolTable); //将这个符号表设置为该function symbol的symbolTable用来快速计算function的para
        if (curFuncType.equals("void")) {
            curFunction = new Function(new VoidType(), name);
        } else {
            curFunction = new Function(curFuncType.equals("int") ? new Integer32Type() : new Integer8Type(), name);
        }
        module.addFunction(curFunction);
        for (SyntaxNode child : children) { //分析参数
            if (child.getName().equals("FuncFParams")) {
                generateFuncFParams(child);
            }
        }
        generateBlock(node.getLastChild(), true);
        curFuncType = null;
        curFunction = null;
        clearVirtualReg();
    }
    
    public void generateFuncFParams(SyntaxNode node) {
        for (SyntaxNode child : node.getChildren()) {
            if (child.getName().equals("FuncFParam")) {
                generateFuncFParam(child);
            }
        }
    }
    
    public void generateFuncFParam(SyntaxNode node) {
        String type = node.getChildren().get(0).getName();
        String name = node.getChildren().get(1).getName();
        Symbol symbol = new Symbol(name, "para", type, getStackLevel(stack.peek()));
        stack.peek().addSymbol(symbol);
        if (node.getLastChild().getName().equals("]")) { //数组参数
            symbol.setIsArray(true);
            Param param = new Param(type.equals("int") ? new Integer32PointerType() : new Integer8PointerType(), "%" + virtualReg);
            virtualReg++;
            curFunction.addParam(param);
        } else { //普通变量
            Param param = new Param(type.equals("int") ? new Integer32Type() : new Integer8Type(), "%" + virtualReg);
            virtualReg++;
            curFunction.addParam(param);
        }
    }
    
    public void generateMainFuncDef(SyntaxNode node) {
        curFuncType = "int";
        SymbolTable symbolTable = new SymbolTable();
        stack.push(symbolTable);
        symbolTables.put(level + 1, symbolTable);
        curFunction = new Function(new Integer32Type(), "main"); //指向该函数
        module.addFunction(curFunction); //加入一个函数
        generateBlock(node.getLastChild(), true);
        curFuncType = null;
        curFunction = null; //函数解析完毕，当前没有函数
        clearVirtualReg();
    }
    
    public void generateBlock(SyntaxNode node, boolean isFuncDef) {
        addLevel();
        curBasicBlock = new BasicBlock(null, "%" + virtualReg);
        curFunction.addBasicBlock(curBasicBlock); //当前函数拥有该块
        virtualReg++; //函数本身占一个虚拟寄存器
        if (!isFuncDef) { //函数内部的{BlockItem}块，创建一个表，负责只需要level++即可
            SymbolTable symbolTable = new SymbolTable();
            stack.push(symbolTable);
            symbolTables.put(level, symbolTable);
        }
        if (isFuncDef) { //是函数才把之前的参数指令打印出来
            ArrayList<Param> params = curFunction.getParams();
            for (Param param : params) {
                curBasicBlock.addInstruction(new AllocaInstr(param.getType(), "%" + virtualReg));
                curBasicBlock.addInstruction(new StoreInstr(param.getType(), param.getName(), "%" + virtualReg));
                virtualReg++;
            }
        }
        ArrayList<SyntaxNode> children = node.getChildren();
        for (SyntaxNode child : children) {
            if (child.getName().equals("BlockItem")) {
                generateBlockItem(child);
            }
        }
        stack.pop();
    }
    
    public void generateBlockItem(SyntaxNode node) {
        if (node.getChildren().get(0).getName().equals("Decl")) {
            generateDecl(node.getChildren().get(0));
        } else {
            generateStmt(node.getChildren().get(0));
        }
    }
    
    public void generateStmt(SyntaxNode node) {
        switch (node.getChildren().get(0).getName()) {
            case "return":
                /*TODO 如果不进行运算的情况 表达式要先进行add load一系列的计算 最后返回"%" + virtualReg*/
                generateReturn(node);
                break;
            case "printf":
                generatePrintf(node);
                break;
            case "if":
                generateIf(node);
                break;
            case "break":
                generateBreak(node);
                break;
            case "continue":
                generateContinue(node);
                break;
            case "for":
                generateFor(node);
                break;
            case "Block":
                generateBlock(node, false);
                break;
            case "LVal":
                generateLVal(node);
                break;
            case "Exp":
                generateExp(node);
                break;
            default: //;的情况，啥也不做
        }
    }
    
    public void generateReturn(SyntaxNode node) {
        if (node.getChildren().get(1).getName().equals("Exp")) { //有返回值
            int result = Calculator.calExp(node.getChildren().get(1), stack); //计算表达式的结果
            /*generateExp(node.getChildren.get(1)) 在此里面进行virtualReg的更新以及指令的增加*/
            if (curFuncType.equals("int")) { //int型函数
                curBasicBlock.addInstruction(new ReturnInstr(new Integer32Type(), String.valueOf(result)));
            } else { //char型函数
                curBasicBlock.addInstruction(new ReturnInstr(new Integer8Type(), String.valueOf(result)));
            }
        } else { //无返回值
            curBasicBlock.addInstruction(new ReturnInstr(new VoidType(), "ReturnInstr"));
        }
    }
    
    public void generatePrintf(SyntaxNode node) {
        /*TODO*/
    }
    
    public void generateIf(SyntaxNode node) {
    
    }
    
    public void generateBreak(SyntaxNode node) {
    
    }
    
    public void generateContinue(SyntaxNode node) {
    
    }
    
    public void generateFor(SyntaxNode node) {
    
    }
    
    public void generateLVal(SyntaxNode node) {
    
    }
    
    public void generateExp(SyntaxNode node) {
        generateAddExp(node.getChildren().get(0));
    }
    
    public void generateAddExp(SyntaxNode node) {
        ArrayList<SyntaxNode> children = node.getChildren();
        if (children.size() > 1) { //AddExp -> AddExp op MulExp
            generateAddExp(children.get(0)); //AddExp生成指令
            /*TODO 添加BinaryInstr指令*/
            generateMulExp(children.get(2)); //MulExp右边生成指令
        } else { // AddExp -> MulExp
            generateMulExp(children.get(0)); //只有MulExp生成指令s
            /*TODO 添加BinaryInstr指令*/
        }
    }
    
    public void generateMulExp(SyntaxNode node) {
        ArrayList<SyntaxNode> children = node.getChildren();
        if (children.size() > 1) {
            generateMulExp(children.get(0));
            /*TODO 添加BinaryInstr指令*/
            generateUnaryExp(children.get(2));
        } else {
            generateUnaryExp(children.get(0));
            /*TODO 添加BinaryInstr指令*/
        }
    }
    
    public void generateUnaryExp(SyntaxNode node) {
    
    }
    
    public void initializeSymbolTable() {
        //addLevel(); //准备进入另一个作用域，level++，创建新的symbolTable
        SymbolTable symbolTable = new SymbolTable();
        stack.push(symbolTable);
        symbolTables.put(++level, symbolTable);
    }
    
    public void addLevel() {
        level++;
    }
    
    public int getStackLevel(SymbolTable symbolTable) {
        for (int i = 1; i <= symbolTables.size(); i++) { //level有可能还未更新，以tables.size作为衡量level的标准，因为11对应
            if (symbolTables.get(i) != null && symbolTables.get(i).equals(symbolTable)) {
                return i;
            }
        }
        return -1;
    }
    
    public void clearVirtualReg() { //LLVM IR 限制了一个函数内所有数字命名的虚拟寄存器必须严格从 0 开始递增
        virtualReg = 0;
    }
}
