package frontend.Calculator;

import frontend.SymbolTable.SymbolTable;
import frontend.SyntaxTree.SyntaxNode;

import java.util.Stack;

public class Calculator {
    public static int calConstExp(SyntaxNode node, Stack<SymbolTable> stack) {
        return calAddExp(node.getChildren().get(0), stack);
    }
    
    public static int calExp(SyntaxNode node, Stack<SymbolTable> stack) {
        return calAddExp(node.getChildren().get(0), stack);
    }
    
    public static int calAddExp(SyntaxNode node, Stack<SymbolTable> stack) {
        if (node.getChildren().get(0).getName().equals("MulExp")) { //只有一个MulExp
            return calMulExp(node.getChildren().get(0), stack);
        } else { //是AddExp op MulExp
            int left = calAddExp(node.getChildren().get(0), stack);
            int right = calMulExp(node.getChildren().get(2), stack);
            if (node.getChildren().get(1).getName().equals("+")) {
                return left + right;
            } else {
                return left - right;
            }
        }
    }
    
    public static int calMulExp(SyntaxNode node, Stack<SymbolTable> stack) {
        if (node.getChildren().get(0).getName().equals("UnaryExp")) {
            return calUnaryExp(node.getChildren().get(0), stack);
        } else {
            int left = calMulExp(node.getChildren().get(0), stack);
            int right = calUnaryExp(node.getChildren().get(2), stack);
            if (node.getChildren().get(1).getName().equals("*")) {
                return left * right;
            } else if (node.getChildren().get(1).getName().equals("/")) {
                return right == 0 ? 0 : left / right; //如果除数为0设置结果为0
            } else {
                return right == 0 ? 0 : left % right;
            }
        }
    }
    
    public static int calUnaryExp(SyntaxNode node, Stack<SymbolTable> stack) {
        if (node.getChildren().get(0).getName().equals("UnaryOp")) {
            SyntaxNode opNode = node.getChildren().get(0);
            if (opNode.getChildren().get(0).getName().equals("+")) {
                return calUnaryExp(node.getChildren().get(1), stack);
            } else if (opNode.getChildren().get(0).getName().equals("-")) {
                return calUnaryExp(node.getChildren().get(1), stack);
            } else { // op 为 !
                return calUnaryExp(node.getChildren().get(1), stack) == 0 ? 1 : 0;
            }
        } else if (node.getChildren().get(0).getName().equals("PrimaryExp")) {
            return calPrimaryExp(node.getChildren().get(0), stack);
        } else { //函数调用不应该在中间代码的时候计算值
            System.out.println("calUnaryExp may happen some error!");
            return 0;
        }
    }
    
    public static int calPrimaryExp(SyntaxNode node, Stack<SymbolTable> stack) {
        if (node.getChildren().get(0).getName().equals("Number")) {
            return calNumber(node.getChildren().get(0), stack);
        } else if (node.getChildren().get(0).getName().equals("Character")) {
            return calCharacter(node.getChildren().get(0), stack);
        } else if (node.getChildren().get(0).getName().equals("(")) {
            return calExp(node.getChildren().get(1), stack);
        } else {
            return calLVal(node.getChildren().get(0), stack);
        }
    }
    
    public static int calLVal(SyntaxNode node, Stack<SymbolTable> stack) {
        String name = node.getChildren().get(0).getName();
        if (isConst(name, stack)) { /*TODO 如果采用等号右端均用cal计算的话，这里要去除isConst */
            if (node.getChildren().size() == 1) { //非数组元素
                return getValue(name, stack);
            } else { //数组元素
                int index = calExp(node.getChildren().get(2), stack);
                return getValue(name, stack, index);
            }
        }
        return 0;
    }
    
    public static int calNumber(SyntaxNode node, Stack<SymbolTable> stack) {
        return Integer.parseInt(node.getChildren().get(0).getName());
    }
    
    public static int calCharacter(SyntaxNode node, Stack<SymbolTable> table) {
        if (node.getChildren().get(0).getName().length() > 3) { //转义字符
            return node.getChildren().get(0).getName().charAt(2);
        } else {
            return node.getChildren().get(0).getName().charAt(1); //本就只有一个字符，考虑外层包围的引号
        }
    }
    
    public static boolean isConst(String name, Stack<SymbolTable> stack) {
        int size = stack.size() - 1;
        for (int i = size; i >= 0; i--) {
            SymbolTable symbolTable = stack.get(i);
            if (symbolTable.hasSymbol(name)) {
                return symbolTable.getSymbol(name).isConst();
            }
        }
        return false;
    }
    
    public static int getValue(String name, Stack<SymbolTable> stack) {
        int size = stack.size() - 1;
        for (int i = size; i >= 0; i--) {
            SymbolTable symbolTable = stack.get(i);
            if (symbolTable.hasSymbol(name)) {
                if (symbolTable.getSymbol(name).isConst()) {
                    return symbolTable.getSymbol(name).getValue();
                }
            }
        }
        return 0;
    }
    
    public static int getValue(String name, Stack<SymbolTable> stack, int index) {
        int size = stack.size() - 1;
        for (int i = size; i >= 0; i--) {
            SymbolTable symbolTable = stack.get(i);
            if (symbolTable.hasSymbol(name)) {
                if (symbolTable.getSymbol(name).isConst()) {
                    return symbolTable.getSymbol(name).getValue(index);
                }
            }
        }
        return 0;
    }
}
