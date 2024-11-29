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
import java.util.Stack;

public class IRGenerator {
    private SyntaxNode root;
    private HashMap<Integer, SymbolTable> symbolTables;
    private Stack<SymbolTable> stack;
    private int level;
    private String curFuncType;
    private int virtualReg;
    private Module module;
    private Function curFunction;
    private BasicBlock curBasicBlock;
    private Stack<ArrayList<BasicBlock>> forLoopStack;
    
    public IRGenerator(SyntaxNode root) {
        this.root = root;
        this.symbolTables = new HashMap<>();
        this.stack = new Stack<>();
        this.level = 0;
        this.curFuncType = null;
        this.virtualReg = 0;
        this.module = new Module();
        this.curFunction = null;
        this.curBasicBlock = null;
        this.forLoopStack = new Stack<>();
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
        try (BufferedWriter stdout = new BufferedWriter(new FileWriter("D:\\BUAA_Compile_2024\\homework5-2\\src\\llvm_ir.txt"))) {
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
            symbol.setVirtualReg("@" + name);
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
                symbol.setIsArray(true);
                SyntaxNode constExpNode = node.getChildren().get(2);
                int arrayLength = Calculator.calConstExp(constExpNode, stack);
                symbol.setArrayLength(arrayLength);
                curBasicBlock.addInstruction(new AllocaInstr(new ArrayType(arrayLength, type.equals("int") ? new Integer32Type() : new Integer8Type()), "%" + virtualReg));
                symbol.setVirtualReg("%" + virtualReg); //只需要记录第一个,数组通过getElementType移动
                virtualReg++; //申请一个[arrayLength x type]的数组空间
                generateConstInitVal(node.getChildren().get(5), symbol);
            } else { //非数组局部变量
                generateConstInitVal(node.getChildren().get(2), symbol);
                curBasicBlock.addInstruction(new AllocaInstr(type.equals("int") ? new Integer32Type() : new Integer8Type(), "%" + virtualReg));
                curBasicBlock.addInstruction(new StoreInstr(type.equals("int") ? new Integer32Type() : new Integer8Type(), "%" + virtualReg, symbol.getValue()));
                symbol.setVirtualReg("%" + virtualReg);
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
            symbol.setVirtualReg("@" + name);
            if (children.size() > 1 && node.getChildren().get(1).getName().equals("[")) { //数组变量
                symbol.setIsArray(true);
                SyntaxNode constExpNode = node.getChildren().get(2);
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
                symbol.setVirtualReg("%" + virtualReg);
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
                curBasicBlock.addInstruction(new AllocaInstr(type.equals("int") ? new Integer32Type() : new Integer8Type(), "%" + virtualReg));
                symbol.setVirtualReg("%" + virtualReg);
                virtualReg++;
                if (node.getLastChild().getName().equals("InitVal")) { //赋值了
                    generateInitVal(node.getLastChild(), symbol);
                } else { //没有赋值，默认值为0
                    symbol.setValue(0);
                }
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
                        curBasicBlock.addInstruction(new GetElementInstr(new ArrayType(symbol.getArrayLength(), symbol.getType()), "%" + virtualReg, index, symbol.getVirtualReg()));
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
                            result = '\n'; //转义字符中只会出现 '\n'
                        } else {
                            result = string.charAt(i);
                        }
                        values.add(result);
                        if (curFunction != null) { //局部变量数组，初值进行getElementPtr并进行store
                            curBasicBlock.addInstruction(new GetElementInstr(new ArrayType(symbol.getArrayLength(), symbol.getType()), "%" + virtualReg, index, symbol.getVirtualReg()));
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
            if (symbol.getIsGlobal()) { //一个非数组的变量是全局变量，要给予初值
                int result = Calculator.calConstExp(node.getChildren().get(0), stack);
                symbol.setValue(result);
            } else {
                /*
                 * 局部变量不计算，通过binaryInstr用寄存器进行中间的运算，且不存初值进去，问就是virtualReg代替计算
                 * alloca(上层已调用) + store
                 */
                Value operand = generateExp(node.getChildren().get(0));
                if (!twoTypeMatch(operand.getType(), getLLVMFunctionType(symbol.getType()))) { //operand是i8,symbol是i32，需要扩展
                    if (getLLVMFunctionType(symbol.getType()) instanceof Integer32Type) {
                        ZeroExtInstr zeroExtInstr = new ZeroExtInstr(new Integer8Type(), "%" + virtualReg, operand, operand.getType());
                        curBasicBlock.addInstruction(zeroExtInstr);
                        virtualReg++;
                        operand = zeroExtInstr;
                    } else { //operand是i32,symbol是i8，需要截断
                        TruncInstr truncInstr = new TruncInstr(virtualReg, operand);
                        curBasicBlock.addInstruction(truncInstr);
                        virtualReg++;
                        operand = truncInstr;
                    }
                }
                StoreInstr storeInstr = new StoreInstr(symbol.getType().equals("int") ? new Integer32Type() : new Integer8Type(), operand, symbol.getVirtualReg());
                curBasicBlock.addInstruction(storeInstr);
            }
        } else { //数组
            int arrayLength = symbol.getArrayLength();
            ArrayList<SyntaxNode> children = node.getChildren();
            ArrayList<Integer> values = new ArrayList<>();//作为数组的变量的值
            int index = 0;
            for (SyntaxNode child : children) { //看有多少是有初值的
                if (child.getName().equals("Exp")) {
                    if (symbol.getIsGlobal()) { //全局变量，要计算出数组的值的
                        int result = Calculator.calConstExp(child, stack);
                        values.add(result);
                    } else { //局部变量数组，初值进行getElementPtr并进行store
                        curBasicBlock.addInstruction(new GetElementInstr(new ArrayType(symbol.getArrayLength(), symbol.getType()), "%" + virtualReg, index, symbol.getVirtualReg()));
                        int lastVirtualReg = virtualReg; // 记录store需要的寄存器
                        virtualReg++; //提前给表达式可能产生的一系列指令留出可以使用的寄存器，因为lastVirtualReg已经预留给store使用了
                        Value operand = generateExp(child); //得到存储结果的寄存器
                        if (!twoTypeMatch(getLLVMFunctionType(symbol.getType()), operand.getType())) { //不匹配则需要截断或扩展
                            if (getLLVMFunctionType(symbol.getType()) instanceof Integer32Type) { //operand是i8，扩展
                                ZeroExtInstr zeroExtInstr = new ZeroExtInstr(new Integer32Type(), "%" + virtualReg, operand, operand.getType());
                                curBasicBlock.addInstruction(zeroExtInstr);
                                operand = zeroExtInstr;
                                virtualReg++; //指向未使用的寄存器
                            } else {
                                TruncInstr truncInstr = new TruncInstr(virtualReg, operand);
                                curBasicBlock.addInstruction(truncInstr);
                                operand = truncInstr;
                                virtualReg++;
                            }
                        }
                        curBasicBlock.addInstruction(new StoreInstr(symbol.getType().equals("int") ? new Integer32Type() : new Integer8Type(), operand, "%" + lastVirtualReg));
                        index++;
                    }
                } else if (child.getName().equals("StringConst")) {
                    String string = child.getChildren().get(0).getName(); //得到字符串常量
                    string = string.substring(1, string.length() - 1);
                    for (int i = 0; i < string.length(); i++) {
                        int result = 0;
                        if (string.charAt(i) == '\\') { //考虑转义
                            i++;
                            result = '\n'; //转义字符中只会出现 '\n'
                        } else {
                            result = string.charAt(i);
                        }
                        values.add(result);
                        if (curFunction != null) { //局部变量数组，初值进行getElementPtr并进行store
                            curBasicBlock.addInstruction(new GetElementInstr(new ArrayType(symbol.getArrayLength(), symbol.getType()), "%" + virtualReg, index, symbol.getVirtualReg()));
                            curBasicBlock.addInstruction(new StoreInstr(symbol.getType().equals("int") ? new Integer32Type() : new Integer8Type(), "%" + virtualReg, result));
                            virtualReg++;
                            index++;
                        }
                    }
                }
            }
            if (symbol.getIsGlobal()) { //全局变量才需要补足初值，局部变量不需要
                for (int i = values.size(); i < arrayLength; i++) { //补足未初始化的元素
                    values.add(0);
                }
                symbol.setValues(values); //给元素赋值
            }
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
            symbol.setVirtualReg("%" + virtualReg);
            virtualReg++;
            curFunction.addParam(param);
        } else { //普通变量
            Param param = new Param(type.equals("int") ? new Integer32Type() : new Integer8Type(), "%" + virtualReg);
            symbol.setVirtualReg("%" + virtualReg);
            virtualReg++;
            curFunction.addParam(param);
        }
    }
    
    public void generateFuncRParams(SyntaxNode node, ArrayList<Value> funcRParams) {
        for (SyntaxNode child : node.getChildren()) {
            if (child.getName().equals("Exp")) {
                funcRParams.add(generateExp(child)); //保存参数
            }
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
        if (isFuncDef) { //函数才占寄存器
            curBasicBlock = new BasicBlock(null, "%" + virtualReg);
            curFunction.addBasicBlock(curBasicBlock); //当前函数拥有该块
            virtualReg++; //函数本身占一个虚拟寄存器
        }
        if (!isFuncDef) { //函数内部的{BlockItem}块，创建一个表，负责只需要level++即可
            SymbolTable symbolTable = new SymbolTable();
            stack.push(symbolTable);
            symbolTables.put(level, symbolTable);
        }
        if (isFuncDef) { //是函数才把之前的参数指令打印出来
            ArrayList<Param> params = curFunction.getParams();
            for (int i = 0; i < params.size(); i++) {
                Param param = params.get(i);
                if ((param.getType() instanceof Integer32Type) || (param.getType() instanceof Integer8Type)) { //数组不需要重新分配
                    curBasicBlock.addInstruction(new AllocaInstr(param.getType(), "%" + virtualReg));
                    curBasicBlock.addInstruction(new StoreInstr(param.getType(), param.getName(), "%" + virtualReg));
                    SymbolTable symbolTable = stack.peek();
                    symbolTable.getSymbol(i).setVirtualReg("%" + virtualReg); //函数参数重新分配寄存器
                    virtualReg++;
                }
            }
        }
        ArrayList<SyntaxNode> children = node.getChildren();
        for (SyntaxNode child : children) {
            if (child.getName().equals("BlockItem")) {
                generateBlockItem(child);
            }
        }
        if (!curFunction.isLastInstrReturnVoid() && curFuncType.equals("void") && isFuncDef) {
            curBasicBlock.addInstruction(new ReturnInstr(new VoidType()));
        }
//        if (!curBasicBlock.hasReturnInstr() && isFuncDef && curFuncType.equals("void")) { //是函数的block并且该block没有return指令
//            curBasicBlock.addInstruction(new ReturnInstr(new VoidType())); //加一条ret void指令
//        }
        /*TODO 这里不知道要不要切块*/
        stack.pop();
    }
    
    public void generateBlockItem(SyntaxNode node) {
        if (node.getChildren().get(0).getName().equals("Decl")) {
            generateDecl(node.getChildren().get(0));
        } else {
            generateStmt(node.getChildren().get(0));
        }
    }
    
    public void generateStmt(SyntaxNode node) { //node是stmt
        switch (node.getChildren().get(0).getName()) {
            case "return":
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
                generateBlock(node.getChildren().get(0), false);
                break;
            case "LVal":
                generateLVal(node);
                break;
            case "Exp":
                generateExp(node.getChildren().get(0));
                break;
            default: //;的情况，啥也不做
        }
    }
    
    public void generateReturn(SyntaxNode node) {
        if (node.getChildren().get(1).getName().equals("Exp")) { //有返回值
            Value operand = generateExp(node.getChildren().get(1));
            if (curFuncType.equals("int")) { //int型函数
                if (operand.getType() instanceof Integer8Type) { //扩展
                    ZeroExtInstr zeroExtInstr = new ZeroExtInstr(new Integer32Type(), "%" + virtualReg, operand, operand.getType());
                    operand = zeroExtInstr; //截断后的寄存器才是对的
                    virtualReg++;
                    curBasicBlock.addInstruction(zeroExtInstr);
                }
                curBasicBlock.addInstruction(new ReturnInstr(new Integer32Type(), operand));
            } else { //char型函数
                if (operand.getType() instanceof Integer32Type) { //截断
                    TruncInstr truncInstr = new TruncInstr(virtualReg, operand);
                    operand = truncInstr; //截断后的寄存器才是对的
                    virtualReg++;
                    curBasicBlock.addInstruction(truncInstr);
                }
                curBasicBlock.addInstruction(new ReturnInstr(new Integer8Type(), operand));
            }
        } else { //无返回值
            curBasicBlock.addInstruction(new ReturnInstr(new VoidType()));
        }
        curBasicBlock = new BasicBlock(null, "%" + virtualReg); //return也要切块
        curFunction.addBasicBlock(curBasicBlock);
        virtualReg++;
    }
    
    public void generatePrintf(SyntaxNode node) {
        /*
         * 其中格式字符只包含 %d 与 %c ，其他 C 语言中的格式字符，如 %f 都当做普通字符原样输出。
         * node是stmt,node.getChildren.get(2)是StringConst
         */
        String stringConst = node.getChildren().get(2).getLastChild().getName();
        stringConst = stringConst.substring(1, stringConst.length() - 1); //去除双引号
        ArrayList<Value> operands = new ArrayList<>();
        for (SyntaxNode child : node.getChildren()) {
            if (child.getName().equals("Exp")) { //对应一个格式符
                Value operand = generateExp(child);
                operands.add(operand);
            }
        }
        int operandIndex = 0; //轮流对应格式符
        for (int i = 0; i < stringConst.length(); i++) {
            ArrayList<Value> funcRParams = new ArrayList<>(); //putint,putch的参数只有一个
            if (stringConst.charAt(i) == '\\') { //该字符对应换行符\n
                i++;
                funcRParams.add(new Value(new Integer32Type(), "10"));
                CallInstr callInstr = new CallInstr(new VoidType(), "putch", "%" + virtualReg, funcRParams);
                curBasicBlock.addInstruction(callInstr);
            } else if (stringConst.charAt(i) == '%') { //对应格式符%d,%c
                i++;
                funcRParams.add(operands.get(operandIndex));
                operandIndex++;
                if (funcRParams.get(0).getType() instanceof Integer8Type) { //putint,putch都是i32,需要扩展
                    ZeroExtInstr zeroExtInstr = new ZeroExtInstr(new Integer32Type(), "%" + virtualReg, funcRParams.get(0), funcRParams.get(0).getType());
                    curBasicBlock.addInstruction(zeroExtInstr);
                    virtualReg++;
                    funcRParams.clear();
                    funcRParams.add(zeroExtInstr); //换条指令给call
                }
                if (stringConst.charAt(i) == 'd') { //对应%d,使用putint
                    CallInstr callInstr = new CallInstr(new VoidType(), "putint", "%" + virtualReg, funcRParams);
                    curBasicBlock.addInstruction(callInstr);
                } else {
                    CallInstr callInstr = new CallInstr(new VoidType(), "putch", "%" + virtualReg, funcRParams);
                    curBasicBlock.addInstruction(callInstr);
                }
            } else { //普通字符
                int value = stringConst.charAt(i);
                funcRParams.add(new Value(new Integer32Type(), String.valueOf(value)));
                CallInstr callInstr = new CallInstr(new VoidType(), "putch", "%" + virtualReg, funcRParams);
                curBasicBlock.addInstruction(callInstr);
            }
        }
    }
    
    public void generateIf(SyntaxNode node) { //node是stmt
        ArrayList<SyntaxNode> children = node.getChildren();
        BasicBlock trueBlock = new BasicBlock(null, null);
        BasicBlock falseBlock = new BasicBlock(null, null);
        BasicBlock nextBlock = new BasicBlock(null, null);
        
        if (children.size() > 5) { //有else语句
            /*TODO 下面这个式子真的对吗*/
            Value operand = generateCond(children.get(2), trueBlock, falseBlock, nextBlock); //分析出来的寄存器应该是个ICmpInstr
            curBasicBlock.addInstruction(new BranchInstr(new BoolType(), operand.getName(), trueBlock, falseBlock));
        } else { //无else语句
            /*TODO 下面这个式子真的对吗*/
            Value operand = generateCond(children.get(2), trueBlock, null, nextBlock); //分析出来的寄存器应该是个ICmpInstr
            curBasicBlock.addInstruction(new BranchInstr(new BoolType(), operand.getName(), trueBlock, nextBlock));
        }
        trueBlock.setName("%" + virtualReg);
        curBasicBlock = trueBlock;
        virtualReg++;
        curFunction.addBasicBlock(curBasicBlock);
        generateStmt(children.get(4)); //解析if块语句
        if (children.size() > 5) { //有else语句
            falseBlock.setName("%" + virtualReg);
            virtualReg++;
            curBasicBlock.addInstruction(new BranchInstr(nextBlock)); //if结束后跳转到if-else语句结束的block
            curBasicBlock = falseBlock; //即将进入else进行解析
            curFunction.addBasicBlock(curBasicBlock);
            generateStmt(children.get(6)); //解析else块语句
        }
        nextBlock.setName("%" + virtualReg);
        virtualReg++;
        curBasicBlock.addInstruction(new BranchInstr(nextBlock)); //无条件跳转到nextBlock块
        curBasicBlock = nextBlock; //if块加上br后切换到if语句之后的block
        curFunction.addBasicBlock(curBasicBlock);
    }
    
    public Value generateCond(SyntaxNode node, BasicBlock trueBlock, BasicBlock falseBlock, BasicBlock nextBlock) {
        return generateLOrExp(node.getChildren().get(0), trueBlock, falseBlock, nextBlock);
    }
    
    public void generateBreak(SyntaxNode node) {
        BasicBlock nextBlock = forLoopStack.peek().get(1);
        BranchInstr branchInstr = new BranchInstr(nextBlock);
        curBasicBlock.addInstruction(branchInstr);
        curBasicBlock = new BasicBlock(null, "%" + virtualReg);
        curFunction.addBasicBlock(curBasicBlock);
        virtualReg++;
    }
    
    public void generateContinue(SyntaxNode node) {
        BasicBlock forStmtBlock = forLoopStack.peek().get(0);
        BranchInstr branchInstr = new BranchInstr(forStmtBlock);
        curBasicBlock.addInstruction(branchInstr);
        curBasicBlock = new BasicBlock(null, "%" + virtualReg);
        curFunction.addBasicBlock(curBasicBlock);
        virtualReg++;
    }
    
    public void generateFor(SyntaxNode node) { //node是Stmt
        /*
         * ForStmt->Cond->Stmt->ForStmt(如果都有的话分析顺序)
         */
        ArrayList<SyntaxNode> children = node.getChildren();
        ArrayList<BasicBlock> arrayList = new ArrayList<>();
        forLoopStack.push(arrayList);
        boolean isFirstForStmt = true;
        BasicBlock condBlock = new BasicBlock(null, null); //对应cond所在块编号
        BasicBlock stmtBlock = new BasicBlock(null, null); //对应stmt所在块编号
        BasicBlock forStmtBlock = new BasicBlock(null, null); //对应最后一个forstmt所在块编号
        BasicBlock nextBlock = new BasicBlock(null, null); //对应for循环结束后的块编号
        arrayList.add(forStmtBlock);
        arrayList.add(nextBlock); //去指引stmt的br
        boolean isFirstSemicn = true; //由分号指引br
        Value operand = null;
        for (SyntaxNode child : children) {
            if (child.getName().equals("ForStmt")) {
                generateForStmt(child);
            } else if (child.getName().equals("Cond")) {
                operand = generateCond(child, stmtBlock, null, nextBlock);
            } else if (child.getName().equals(";")) {
                if (isFirstSemicn) { //第一次遇到分号，第一个forStmt分析完了，在forStmt的block里设置cond的块号
                    isFirstSemicn = false;
                    BranchInstr branchInstr = new BranchInstr(condBlock);
                    condBlock.setName("%" + virtualReg);
                    virtualReg++;
                    curBasicBlock.addInstruction(branchInstr);
                    curBasicBlock = condBlock; //无条件跳转到cond对应的block
                    curFunction.addBasicBlock(curBasicBlock);
                } else { //第二个分号处理完了
                    BranchInstr branchInstr = null;
                    if (operand != null) { //for语句里有cond
                        branchInstr = new BranchInstr(new BoolType(), operand.getName(), stmtBlock, nextBlock);
                    } else {
                        branchInstr = new BranchInstr(stmtBlock);
                    }
                    stmtBlock.setName("%" + virtualReg);
                    virtualReg++;
                    curBasicBlock.addInstruction(branchInstr);
                    curBasicBlock = stmtBlock;
                    curFunction.addBasicBlock(curBasicBlock);
                    generateStmt(node.getLastChild()); //分析stmt
                    forStmtBlock.setName("%" + virtualReg);
                    virtualReg++;
                    curBasicBlock.addInstruction(new BranchInstr(forStmtBlock));
                    curBasicBlock = forStmtBlock;
                    curFunction.addBasicBlock(forStmtBlock);
                }
            } else if (child.getName().equals(")")) { //第二个forStmt分析完了
                BranchInstr branchInstr = new BranchInstr(condBlock);
                nextBlock.setName("%" + virtualReg);
                virtualReg++;
                curBasicBlock.addInstruction(branchInstr);
                curBasicBlock = nextBlock; //切块，到nextBlock
                curFunction.addBasicBlock(curBasicBlock);
            }
        }
        forLoopStack.pop(); //弹出
    }
    
    public void generateForStmt(SyntaxNode node) {
        generateLVal(node); //LVal = Exp最后一条是Store，接下来接一个无条件跳转,到下一个块
    }
    
    public Value generateLVal(SyntaxNode node) {
        /* node是Stmt,node.getChildren.get(0)是LVal,node.getChildren.get(1)是getint|getchar|Exp
         *  LVal '=' Exp ';'
         *  LVal '=' 'getint''('')'';'
         *  LVal '=' 'getchar''('')'';'
         */
        if (node.getName().equals("Stmt") || node.getName().equals("ForStmt")) {
            //此处的ForStmt仅仅为了重复利用LVal = Exp的解析
            /*TODO 因为有Stmt->LVal和Stmt->Exp->...->LVal两种情况，所以后面可以分成两个函数 */
            String name = node.getChildren().get(0).getChildren().get(0).getName(); //ident
            Symbol symbol = getSymbol(name);
            String symbolReg = symbol.getVirtualReg(); //symbol对应的寄存器
            if (node.getChildren().get(0).getChildren().size() > 1) { //数组 ident[Exp]
                Value operand = generateExp(node.getChildren().get(0).getChildren().get(2)); //exp的寄存器
                if (!curFunction.getName().equals("main") && !symbol.getIsGlobal()) { //局部函数且使用的非全局变量数组
                    //相对位移，防止调用的是参数的数组
                    GetElementInstr getElementInstr = new GetElementInstr(symbol.getType().equals("int") ? new Integer32Type() : new Integer8Type(), "%" + virtualReg, operand, symbolReg, 2);
                    curBasicBlock.addInstruction(getElementInstr); //得到数组元素
                } else {
                    GetElementInstr getElementInstr = new GetElementInstr(new ArrayType(symbol.getArrayLength(), symbol.getType()), "%" + virtualReg, operand, symbolReg);
                    curBasicBlock.addInstruction(getElementInstr); //得到数组元素
                }
                symbolReg = "%" + virtualReg; //数组的寄存器要换过来，因为使用了getElementType
                virtualReg++;
            }
            //非数组的无需任何操作改变
            switch (node.getChildren().get(2).getName()) {
                case "getint":
                    curBasicBlock.addInstruction(new CallInstr(new Integer32Type(), "getint", "%" + virtualReg));
                    if (!twoTypeMatch(getLLVMFunctionType(symbol.getType()), new Integer32Type())) {
                        virtualReg++;
                        curBasicBlock.addInstruction(new TruncInstr(virtualReg));
                        curBasicBlock.addInstruction(new StoreInstr(new Integer8Type(), "%" + virtualReg, symbolReg));
                    } else {
                        curBasicBlock.addInstruction(new StoreInstr(new Integer32Type(), "%" + virtualReg, symbolReg));
                    }
                    virtualReg++;
                    break;
                case "getchar":
                    curBasicBlock.addInstruction(new CallInstr(new Integer32Type(), "getchar", "%" + virtualReg));
                    if (!twoTypeMatch(getLLVMFunctionType(symbol.getType()), new Integer32Type())) { //char = getchar()，getchar返回值是i32
                        virtualReg++;
                        curBasicBlock.addInstruction(new TruncInstr(virtualReg));
                        curBasicBlock.addInstruction(new StoreInstr(new Integer8Type(), "%" + virtualReg, symbolReg));
                    } else { //i32对i32
                        curBasicBlock.addInstruction(new StoreInstr(new Integer32Type(), "%" + virtualReg, symbolReg));
                    }
                    virtualReg++;
                    break;
                default: // LVal = Exp形式的Exp分析,赋值语句
                    Value operand = generateExp(node.getChildren().get(2)); //拿的是exp运算出来的储存结果的寄存器%virtualReg或者纯数字
                    if (!twoTypeMatch(getLLVMFunctionType(symbol.getType()), operand.getType())) { //二者类型不匹配，截断或扩展
                        if (getLLVMFunctionType(symbol.getType()) instanceof Integer32Type) { //8->32,zext
                            ZeroExtInstr zeroExtInstr = new ZeroExtInstr(new Integer8Type(), "%" + virtualReg, operand, operand.getType());
                            curBasicBlock.addInstruction(zeroExtInstr);
                            operand = zeroExtInstr; //指向新寄存器
                        } else { //32->8,trunc
                            TruncInstr truncInstr = new TruncInstr(virtualReg, operand);
                            curBasicBlock.addInstruction(truncInstr);
                            operand = truncInstr;
                        }
                        virtualReg++; //新增了一条语句，virtualReg应该自增，指向下一个未分配的虚拟寄存器
                    }
                    curBasicBlock.addInstruction(new StoreInstr(getLLVMFunctionType(symbol.getType()), operand, symbolReg));
            }
            return null;
        } else {
            /* node是LVal
             * Exp->...->LVal形式，是在表达式中出现的LVal，而不是由LVal = 右值的形式
             * 函数调用
             */
            String name = node.getChildren().get(0).getName(); //ident
            Symbol symbol = getSymbol(name);
            String symbolReg = symbol.getVirtualReg(); //symbol对应的寄存器
            if (node.getChildren().size() == 1) { //ident
                if (!symbol.getIsArray()) {
                    LoadInstr loadInstr = new LoadInstr(symbol.getType().equals("int") ? new Integer32Type() : new Integer8Type(), symbolReg, "%" + virtualReg);
                    curBasicBlock.addInstruction(loadInstr);
                    virtualReg++;
                    return loadInstr; //把load指令返回回去，由binary取load的最前面的虚拟寄存器作为binary的operand
                } else { //数组整个整体，不是单独的元素
                    if (!curFunction.getName().equals("main") && !symbol.getIsGlobal()) { //相对位移
                        GetElementInstr getElementInstr = new GetElementInstr(symbol.getType().equals("int") ? new Integer32Type() : new Integer8Type(), "%" + virtualReg, new Value(symbol.getType().equals("int") ? new Integer32Type() : new Integer8Type(), String.valueOf(0)), symbolReg, 2);
                        curBasicBlock.addInstruction(getElementInstr);
                        virtualReg++;
                        return getElementInstr;
                    } else {
                        GetElementInstr getElementInstr = new GetElementInstr(new ArrayType(symbol.getArrayLength(), symbol.getType()), "%" + virtualReg, 0, symbolReg);
                        curBasicBlock.addInstruction(getElementInstr);
                        virtualReg++;
                        return getElementInstr;
                    }
                }
            } else { //ident[Exp]
                /*TODO getelementType的方式有问题 */
                Value operand = generateExp(node.getChildren().get(2)); //得到exp的寄存器
                if (!curFunction.getName().equals("main") && !symbol.getIsGlobal()) { //相对位移，防止调用的是参数的数组
                    GetElementInstr getElementInstr = new GetElementInstr(symbol.getType().equals("int") ? new Integer32Type() : new Integer8Type(), "%" + virtualReg, operand, symbolReg, 2);
                    curBasicBlock.addInstruction(getElementInstr); //得到数组元素
                } else {
                    GetElementInstr getElementInstr = new GetElementInstr(new ArrayType(symbol.getArrayLength(), symbol.getType()), "%" + virtualReg, operand, symbolReg);
                    curBasicBlock.addInstruction(getElementInstr); //得到数组元素
                }
                virtualReg++;
                LoadInstr loadInstr = new LoadInstr(symbol.getType().equals("int") ? new Integer32Type() : new Integer8Type(), "%" + (virtualReg - 1), "%" + virtualReg);
                curBasicBlock.addInstruction(loadInstr);
                virtualReg++;
                return loadInstr;
            }
        }
    }
    
    public Value generateExp(SyntaxNode node) { /*generateExp通通改为Value类型？*/
        return generateAddExp(node.getChildren().get(0));
    }
    
    public Value generateAddExp(SyntaxNode node) {
        ArrayList<SyntaxNode> children = node.getChildren();
        if (children.size() > 1) { //AddExp -> AddExp op MulExp
            Value operand1 = generateAddExp(children.get(0)); //AddExp生成指令，得出operand1
            Value operand2 = generateMulExp(children.get(2)); //MulExp右边生成指令，得出operand2
            String op = children.get(1).getName();
            if (!(operand1.getType() instanceof Integer32Type)) { //如果是i8就扩展
                curBasicBlock.addInstruction(new ZeroExtInstr(new Integer32Type(), "%" + virtualReg, operand1, operand1.getType()));
                operand1 = new Value(new Integer32Type(), "%" + virtualReg); //后续运算的寄存器是扩展后的寄存器
                virtualReg++;
            }
            if (!(operand2.getType() instanceof Integer32Type)) {
                curBasicBlock.addInstruction(new ZeroExtInstr(new Integer32Type(), "%" + virtualReg, operand2, operand2.getType()));
                operand2 = new Value(new Integer32Type(), "%" + virtualReg);
                virtualReg++;
            }
            curBasicBlock.addInstruction(new BinaryInstr(new Integer32Type(), operand1, operand2, op, "%" + virtualReg));
            virtualReg++;
            return new Value(new Integer32Type(), "%" + (virtualReg - 1)); //运算的结果保存在virtualReg，由virtualReg参与后续的binaryInstr
        } else { // AddExp -> MulExp
            return generateMulExp(children.get(0)); //只有MulExp生成指令s
        }
    }
    
    public Value generateMulExp(SyntaxNode node) {
        ArrayList<SyntaxNode> children = node.getChildren();
        if (children.size() > 1) { // MulExp -> MulExp op UnaryExp
            Value operand1 = generateMulExp(children.get(0));
            Value operand2 = generateUnaryExp(children.get(2));
            String op = children.get(1).getName();
            if (!(operand1.getType() instanceof Integer32Type)) { //如果是i8就扩展
                curBasicBlock.addInstruction(new ZeroExtInstr(new Integer32Type(), "%" + virtualReg, operand1, operand1.getType()));
                operand1 = new Value(new Integer32Type(), "%" + virtualReg); //后续运算的寄存器是扩展后的寄存器
                virtualReg++;
            }
            if (!(operand2.getType() instanceof Integer32Type)) {
                curBasicBlock.addInstruction(new ZeroExtInstr(new Integer32Type(), "%" + virtualReg, operand2, operand2.getType()));
                operand2 = new Value(new Integer32Type(), "%" + virtualReg);
                virtualReg++;
            }
            curBasicBlock.addInstruction(new BinaryInstr(new Integer32Type(), operand1, operand2, op, "%" + virtualReg));
            virtualReg++;
            return new Value(new Integer32Type(), "%" + (virtualReg - 1));
        } else {
            return generateUnaryExp(children.get(0));
        }
    }
    
    public Value generateUnaryExp(SyntaxNode node) {
        if (node.getChildren().get(0).getName().equals("PrimaryExp")) {
            return generatePrimaryExp(node.getChildren().get(0));
        } else if (node.getChildren().get(0).getName().equals("UnaryOp")) {
            /*
             * 对于数字前的正负，可以看做是 0 和其做一次运算。
             * 即 +1 其实就是 0 + 1(其实正号甚至都不用去管他)
             * -1 其实就是 0 - 1。
             * 所以在生成代码的时候，可以当作一个特殊的 AddExp 来处理。
             */
            switch (node.getChildren().get(0).getChildren().get(0).getName()) {
                case "+":
                    return generateUnaryExp(node.getChildren().get(1));
                case "-":
                    Value operand1 = generateUnaryExp(node.getChildren().get(1)); //exp得到的寄存器，拿来做二目运算
                    BinaryInstr binaryInstr = new BinaryInstr(new Integer32Type(), operand1, "-", "%" + virtualReg);
                    virtualReg++;
                    curBasicBlock.addInstruction(binaryInstr);
                    return binaryInstr; //将二目运算的结果的寄存器返回去用于store
                case "!":
                    /*TODO !仅出现在条件表达式 */
                    Value operand2 = generateUnaryExp(node.getChildren().get(1));
                    ICmpInstr iCmpInstr = new ICmpInstr(operand2.getType(), "%" + virtualReg, operand2, new Value(new Integer32Type(), "0"), "eq");
                    virtualReg++;
                    curBasicBlock.addInstruction(iCmpInstr);
                    return iCmpInstr;
            }
        } else { //函数调用
            String name = node.getChildren().get(0).getName(); //函数名ident
            Symbol symbol = getSymbol(name);
            if (!node.getChildren().get(2).getName().equals(")")) { //有参情况
                ArrayList<Value> funcRParams = new ArrayList<>();
                generateFuncRParams(node.getChildren().get(2), funcRParams);
                CallInstr callInstr = new CallInstr(getLLVMFunctionType(symbol.getType()), name, "%" + virtualReg, funcRParams);
                curBasicBlock.addInstruction(callInstr);
                if (!(getLLVMFunctionType(symbol.getType()) instanceof VoidType)) { //没有返回值就无需存储
                    virtualReg++;
                }
                return callInstr;
            } else { //无参情况
                CallInstr callInstr = new CallInstr(getLLVMFunctionType(symbol.getType()), name, "%" + virtualReg);
                curBasicBlock.addInstruction(callInstr);
                if (!(getLLVMFunctionType(symbol.getType()) instanceof VoidType)) { //没有返回值就无需存储
                    virtualReg++;
                }
                return callInstr;
            }
        }
        return null;
    }
    
    public Value generatePrimaryExp(SyntaxNode node) {
        switch (node.getChildren().get(0).getName()) {
            case "Number":
                return new Value(new Integer32Type(), node.getChildren().get(0).getChildren().get(0).getName());
            case "Character":
                //return new Value(new Integer8Type(), node.getChildren().get(0).getChildren().get(0).getName());
                String charConst = node.getChildren().get(0).getChildren().get(0).getName();
                charConst = charConst.substring(1, charConst.length() - 1);
                if (charConst.charAt(0) == '\\') { //考虑转义字符
                    switch (charConst.charAt(1)) {
                        case '\"':
                            charConst = String.valueOf(34);
                        case '\'':
                            charConst = String.valueOf(39);
                        default:
                            charConst = String.valueOf(92);
                    }
                } else {
                    charConst = String.valueOf((int) charConst.charAt(0));
                }
                return new Value(new Integer8Type(), charConst);
            case "LVal":
                return generateLVal(node.getChildren().get(0));
            default:
                return generateExp(node.getChildren().get(1));
        }
    }
    
    public Value generateLOrExp(SyntaxNode node, BasicBlock trueBlock, BasicBlock falseBlock, BasicBlock nextBlock) { //短路求值
        /*
         * LOrExp → LAndExp | LOrExp '||' LAndExp
         * trueBlock代表这个表达式为真的时候去往的块
         * falseBlock代表这个表达式为假的时候且有else去往的块
         * nextBlock代表这个表达式为假且无else去往的块
         */
        ArrayList<SyntaxNode> children = node.getChildren();
        if (children.size() > 1) {
            BasicBlock basicBlock = new BasicBlock(null, null); //遇到||需要创块，这个块是LAndExp所在的块号
            Value operand1 = generateLOrExp(children.get(0), trueBlock, basicBlock, nextBlock); //显然当LOrExp为假的时候去往的块应该是||后面那个块
            basicBlock.setName("%" + virtualReg);
            if (falseBlock != null) {
                curBasicBlock.addInstruction(new BranchInstr(new BoolType(), operand1.getName(), trueBlock, basicBlock));
            } else {
                curBasicBlock.addInstruction(new BranchInstr(new BoolType(), operand1.getName(), trueBlock, basicBlock));
            }
            virtualReg++;
            curBasicBlock = basicBlock;
            curFunction.addBasicBlock(curBasicBlock);
            return generateLAndExp(children.get(2), trueBlock, falseBlock, nextBlock);
        } else {
            return generateLAndExp(children.get(0), trueBlock, falseBlock, nextBlock);
        }
    }
    
    public Value generateLAndExp(SyntaxNode node, BasicBlock trueBlock, BasicBlock falseBlock, BasicBlock nextBlock) { //短路求值，在这一步进行比较操作，返回的一定是一个ICmpInstr
        /*
         *  LAndExp → EqExp | LAndExp '&&' EqExp
         */
        ArrayList<SyntaxNode> children = node.getChildren();
        if (children.size() > 1) {
            Value operand1 = generateLAndExp(children.get(0), trueBlock, falseBlock, nextBlock);
            BasicBlock basicBlock = new BasicBlock(null, "%" + virtualReg); //遇到&&需要创块，短路求值的，看需要是否跳转到下一个求值
            if (falseBlock != null) { //有else语句
                curBasicBlock.addInstruction(new BranchInstr(new BoolType(), operand1.getName(), basicBlock, falseBlock));
            } else { //无else语句直接跳转到if语句后的那个语句块
                curBasicBlock.addInstruction(new BranchInstr(new BoolType(), operand1.getName(), basicBlock, nextBlock));
            }
            virtualReg++;
            curBasicBlock = basicBlock; //切换到&&后的这个表达式对应的block
            curFunction.addBasicBlock(curBasicBlock); //切换到短路求值的下一个基本块
            Value operand2 = generateEqExp(children.get(2)); //得到iCmpInstr,icmp后一定紧跟有条件跳转
            if (!(operand2.getType() instanceof BoolType)) {
                ICmpInstr iCmpInstr = new ICmpInstr(operand2.getType(), "%" + virtualReg, operand2, new Value(new Integer32Type(), "0"), "ne");
                curBasicBlock.addInstruction(iCmpInstr);
                virtualReg++;
                return iCmpInstr;
            }
            return operand2;
        } else { //与0进行判断
            Value operand = generateEqExp(node.getChildren().get(0)); //要么是一个数值，要么是一个关系表达式已经cmp过了
            if (!(operand.getType() instanceof BoolType)) { //不是一个icmpInstr，比如是一个变量c，需要与0进行cmp
                ICmpInstr iCmpInstr = new ICmpInstr(operand.getType(), "%" + virtualReg, operand, new Value(new Integer32Type(), "0"), "ne");
                curBasicBlock.addInstruction(iCmpInstr);
                virtualReg++;
                return iCmpInstr;
            }
            return operand; //比较过了，它本身是一个iCmpInstr
        }
    }
    
    public Value generateEqExp(SyntaxNode node) {
        /*
         * EqExp → RelExp | EqExp ('==' | '!=') RelExp
         */
        ArrayList<SyntaxNode> children = node.getChildren();
        if (children.size() > 1) {
            Value operand1 = generateEqExp(children.get(0)); //EqExp生成指令，得出operand1
            Value operand2 = generateRelExp(children.get(2)); //RelExp右边生成指令，得出operand2
            String op = children.get(1).getName();
            if (!(operand1.getType() instanceof Integer32Type)) { //如果是i8就扩展
                curBasicBlock.addInstruction(new ZeroExtInstr(new Integer32Type(), "%" + virtualReg, operand1, operand1.getType()));
                operand1 = new Value(new Integer32Type(), "%" + virtualReg); //后续运算的寄存器是扩展后的寄存器
                virtualReg++;
            }
            if (!(operand2.getType() instanceof Integer32Type)) {
                curBasicBlock.addInstruction(new ZeroExtInstr(new Integer32Type(), "%" + virtualReg, operand2, operand2.getType()));
                operand2 = new Value(new Integer32Type(), "%" + virtualReg);
                virtualReg++;
            }
            ICmpInstr iCmpInstr = new ICmpInstr(new Integer32Type(), "%" + virtualReg, operand1, operand2, op);
            curBasicBlock.addInstruction(iCmpInstr);
            virtualReg++;
            return iCmpInstr;
        } else {
            return generateRelExp(node.getChildren().get(0));
        }
    }
    
    public Value generateRelExp(SyntaxNode node) {
        /*
         * RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
         */
        ArrayList<SyntaxNode> children = node.getChildren();
        if (children.size() > 1) {
            Value operand1 = generateRelExp(children.get(0)); //RelExp生成指令，得出operand1
            Value operand2 = generateAddExp(children.get(2)); //AddExp右边生成指令，得出operand2
            String op = children.get(1).getName();
            if (!(operand1.getType() instanceof Integer32Type)) { //如果是i8就扩展
                curBasicBlock.addInstruction(new ZeroExtInstr(new Integer32Type(), "%" + virtualReg, operand1, operand1.getType()));
                operand1 = new Value(new Integer32Type(), "%" + virtualReg); //后续运算的寄存器是扩展后的寄存器
                virtualReg++;
            }
            if (!(operand2.getType() instanceof Integer32Type)) {
                curBasicBlock.addInstruction(new ZeroExtInstr(new Integer32Type(), "%" + virtualReg, operand2, operand2.getType()));
                operand2 = new Value(new Integer32Type(), "%" + virtualReg);
                virtualReg++;
            }
            ICmpInstr iCmpInstr = new ICmpInstr(new Integer32Type(), "%" + virtualReg, operand1, operand2, op);
            curBasicBlock.addInstruction(iCmpInstr);
            virtualReg++;
            return iCmpInstr; //运算的结果保存在virtualReg，由virtualReg参与后续的Instr
        } else {
            return generateAddExp(node.getChildren().get(0));
        }
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
    
    public Symbol getSymbol(String name) {
        int size = stack.size() - 1;
        for (int i = size; i >= 0; i--) {
            SymbolTable symbolTable = stack.get(i);
            if (name.contains("[")) { //是数组的一个选项
                int index = name.indexOf("[");
                name = name.substring(0, index);
            }
            if (symbolTable.hasSymbol(name)) {
                return symbolTable.getSymbol(name);
            }
        }
        return null;
    }
    
    public Type getLLVMFunctionType(String type) {
        switch (type) {
            case "int":
                return new Integer32Type();
            case "char":
                return new Integer8Type();
            default:
                return new VoidType();
        }
    }
    
    public boolean twoTypeMatch(Type type1, Type type2) { //两个类型是否是一种类型
        return type1.toString().equals(type2.toString());
    }
}
