package middle;

import frontend.Calculator.Calculator;
import frontend.SymbolTable.Symbol;
import frontend.SymbolTable.SymbolTable;
import frontend.SyntaxTree.SyntaxNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class IRGenerator {
    private SyntaxNode root;
    private HashMap<Integer, SymbolTable> symbolTables;
    private Stack<SymbolTable> stack;
    private int level;
    private String curFuncType;
    private ArrayList<String> irCodes;
    private int virtualReg;
    
    public IRGenerator(SyntaxNode root) {
        this.root = root;
        this.symbolTables = new HashMap<>();
        this.stack = new Stack<>();
        this.level = 0;
        this.curFuncType = null;
        this.irCodes = new ArrayList<>();
        this.virtualReg = 0;
        initializeSymbolTable();
    }
    
    public void generateCompUnit() {
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
        for (String string : irCodes) {
            System.out.println(string);
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
            irCodes.add("@" + name + " = dso_local constant ");
            if (node.getChildren().get(1).getName().equals("[")) { //数组变量
                symbol.setIsArray(true);
                SyntaxNode constExpNode = node.getChildren().get(2);
                int arrayLength = Calculator.calConstExp(constExpNode, stack);
                symbol.setArrayLength(arrayLength);
                if (type.equals("int")) {
                    connectIRCode("[" + arrayLength + " x i32] ");
                } else {
                    connectIRCode("[" + arrayLength + " x i8] ");
                }
                generateConstInitVal(node.getChildren().get(5), symbol);
            } else { //非数组变量
                if (type.equals("int")) {
                    connectIRCode("i32 ");
                } else {
                    connectIRCode("i8 ");
                }
                generateConstInitVal(node.getChildren().get(2), symbol);
            }
        } else { //局部变量
            String name = node.getChildren().get(0).getName(); //变量名称
            Symbol symbol = new Symbol(name, "const", type, getStackLevel(stack.peek()), false);
            stack.peek().addSymbol(symbol);
            /*TODO*/
        }
    }
    
    public void generateVarDef(SyntaxNode node, String type) {
        if (curFuncType == null) {
            ArrayList<SyntaxNode> children = node.getChildren();
            String name = node.getChildren().get(0).getName();
            Symbol symbol = new Symbol(name, "var", type, getStackLevel(stack.peek()), true);
            stack.peek().addSymbol(symbol);
            irCodes.add("@" + name + " = dso_local global ");
            if (children.size() > 1 && node.getChildren().get(1).getName().equals("[")) { //数组变量
                symbol.setIsArray(true);
                SyntaxNode constExpNode = node.getChildren().get(2);
                int arrayLength = Calculator.calConstExp(constExpNode, stack);
                symbol.setArrayLength(arrayLength);
                if (type.equals("int")) {
                    connectIRCode("[" + arrayLength + " x i32] ");
                } else {
                    connectIRCode("[" + arrayLength + " x i8] ");
                }
                if (node.getLastChild().getName().equals("InitVal")) { //赋值了
                    generateInitVal(node.getLastChild(), symbol);
                } else { //没有赋值，默认值为0
                    ArrayList<Integer> values = new ArrayList<>();
                    for (int i = 0; i < symbol.getArrayLength(); i++) {
                        values.add(0);
                    }
                    symbol.setValues(values);
                    connectIRCode("[");
                    connectIRCode(symbol.getType(), String.valueOf(values.get(0)));
                    for (int i = 1; i < values.size(); i++) {
                        connectIRCode(", ");
                        connectIRCode(symbol.getType(), String.valueOf(values.get(i)));
                    }
                    connectIRCode("]");
                }
            } else { //非数组变量
                if (type.equals("int")) {
                    connectIRCode("i32 ");
                } else {
                    connectIRCode("i8 ");
                }
                if (node.getLastChild().getName().equals("InitVal")) { //赋值了
                    generateInitVal(node.getLastChild(), symbol);
                } else { //没有赋值，默认值为0
                    symbol.setValue(0);
                    connectIRCode("0");
                }
            }
        } else { //局部变量
            String name = node.getChildren().get(0).getName(); //变量名称
            Symbol symbol = new Symbol(name, "const", type, getStackLevel(stack.peek()), false);
            stack.peek().addSymbol(symbol);
            /*TODO*/
        }
    }
    
    
    public void generateConstInitVal(SyntaxNode node, Symbol symbol) {
        if (symbol.getIsGlobal()) { //全局
            if (!symbol.getIsArray()) { //非数组
                int result = Calculator.calConstExp(node.getChildren().get(0), stack);
                connectIRCode(String.valueOf(result));
                symbol.setValue(result); //符号表有初值
            } else { //数组
                int arrayLength = symbol.getArrayLength();
                ArrayList<SyntaxNode> children = node.getChildren();
                ArrayList<Integer> values = new ArrayList<>();//作为数组的变量的值
                for (SyntaxNode child : children) { //看有多少是有初值的
                    if (child.getName().equals("ConstExp")) {
                        int result = Calculator.calConstExp(child, stack);
                        values.add(result);
                    }
                }
                for (int i = values.size(); i < arrayLength; i++) { //补足未初始化的元素
                    values.add(0);
                }
                symbol.setValues(values); //给元素赋值
                connectIRCode("[");
                connectIRCode(symbol.getType(), String.valueOf(values.get(0)));
                for (int i = 1; i < values.size(); i++) {
                    connectIRCode(", ");
                    connectIRCode(symbol.getType(), String.valueOf(values.get(i)));
                }
                connectIRCode("]");
            }
        }
    }
    
    public void generateInitVal(SyntaxNode node, Symbol symbol) {
        if (symbol.getIsGlobal()) { //全局
            if (!symbol.getIsArray()) { //非数组
                int result = Calculator.calConstExp(node.getChildren().get(0), stack);
                connectIRCode(String.valueOf(result));
                symbol.setValue(result);
            } else { //数组
                int arrayLength = symbol.getArrayLength();
                ArrayList<SyntaxNode> children = node.getChildren();
                ArrayList<Integer> values = new ArrayList<>();//作为数组的变量的值
                for (SyntaxNode child : children) { //看有多少是有初值的
                    if (child.getName().equals("Exp")) {
                        int result = Calculator.calConstExp(child, stack);
                        values.add(result);
                    }
                }
                for (int i = values.size(); i < arrayLength; i++) { //补足未初始化的元素
                    values.add(0);
                }
                symbol.setValues(values); //给元素赋值
                connectIRCode("[");
                connectIRCode(symbol.getType(), String.valueOf(values.get(0)));
                for (int i = 1; i < values.size(); i++) {
                    connectIRCode(", ");
                    connectIRCode(symbol.getType(), String.valueOf(values.get(i)));
                }
                connectIRCode("]");
            }
        }
    }
    
    
    public void generateFuncDef(SyntaxNode node) {
    
    }
    
    public void generateMainFuncDef(SyntaxNode node) {
        curFuncType = "int";
        SymbolTable symbolTable = new SymbolTable();
        stack.push(symbolTable);
        symbolTables.put(level + 1, symbolTable);
        irCodes.add("define dso_local i32 @main() #0 {");
        generateBlock(node.getLastChild(), true);
        irCodes.add("}");
        curFuncType = null;
    }
    
    public void generateBlock(SyntaxNode node, boolean isFuncDef) {
        addLevel();
        if (!isFuncDef) { //函数内部的{BlockItem}块，创建一个表，负责只需要level++即可
            SymbolTable symbolTable = new SymbolTable();
            stack.push(symbolTable);
            symbolTables.put(level, symbolTable);
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
                if (node.getChildren().get(1).getName().equals("Exp")) { //有返回值
                    int result = Calculator.calExp(node.getChildren().get(1), stack); //计算表达式的结果
                    if (curFuncType.equals("int")) { //int型函数
                        irCodes.add("ret i32 " + result);
                    } else { //char型函数
                        irCodes.add("ret i8 " + result);
                    }
                } else { //无返回值
                    irCodes.add("ret void");
                }
                break;
            
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
    
    public void connectIRCode(String string) {
        String irCode = irCodes.get(irCodes.size() - 1);
        irCode += string;
        irCodes.set(irCodes.size() - 1, irCode);
    }
    
    public void connectIRCode(String type, String string) {
        if (type.equals("int")) {
            String irCode = irCodes.get(irCodes.size() - 1);
            irCode += "i32 " + string;
            irCodes.set(irCodes.size() - 1, irCode);
        } else {
            String irCode = irCodes.get(irCodes.size() - 1);
            irCode += "i8 " + string;
            irCodes.set(irCodes.size() - 1, irCode);
        }
    }
    
    public void addIRAlloca(Symbol symbol) { //局部变量的llvm ir指令
        String type = symbol.getType();
        if (!symbol.getIsArray()) { //非数组变量的申请
            if (type.equals("int")) {
                irCodes.add("%" + virtualReg + " = alloca i32");
            } else {
                irCodes.add("%" + virtualReg + " = alloca i8");
            }
        }
        virtualReg++; //分配一个就++，实时指向未分配的虚拟寄存器
    }
}
