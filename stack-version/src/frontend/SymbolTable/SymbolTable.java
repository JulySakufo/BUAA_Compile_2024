package frontend.SymbolTable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class SymbolTable {
    private ArrayList<Symbol> symbols;
    
    public SymbolTable() {
        this.symbols = new ArrayList<>();
    }
    
    public void addSymbol(Symbol symbol) {
        symbols.add(symbol);
    }
    
    public boolean hasSymbol(String name) {
        for (Symbol symbol : symbols) {
            if (symbol.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
    
    public Symbol getSymbol(String name) {
        for (Symbol symbol : symbols) {
            if (symbol.getName().equals(name)) {
                return symbol;
            }
        }
        return null;
    }
    
    public Symbol getSymbol(int index) {
        return symbols.get(index);
    }
    
    public ArrayList<Symbol> getSymbols() {
        return symbols;
    }
    
    public int getParaCount() { //获取自定义函数的参数个数
        int paraCount = 0;
        for (Symbol symbol : symbols) {
            if (symbol.getKind().equals("para")) {
                paraCount++;
            } else { //因为自定义函数的符号表开始一定为para，所以不是para代表参数已经结束
                break;
            }
        }
        return paraCount;
    }
    
    public void printSymbol(BufferedWriter stdout) throws IOException {
        for (Symbol symbol : symbols) {
            if (symbol.isVoidFunc()) {
                stdout.write(symbol.getLevel() + " " + symbol.getName() + " " + "VoidFunc\n");
            } else if (symbol.isCharFunc()) {
                stdout.write(symbol.getLevel() + " " + symbol.getName() + " " + "CharFunc\n");
            } else if (symbol.isIntFunc()) {
                stdout.write(symbol.getLevel() + " " + symbol.getName() + " " + "IntFunc\n");
            } else if (symbol.isConstChar()) {
                stdout.write(symbol.getLevel() + " " + symbol.getName() + " " + "ConstChar\n");
            } else if (symbol.isConstInt()) {
                stdout.write(symbol.getLevel() + " " + symbol.getName() + " " + "ConstInt\n");
            } else if (symbol.isConstCharArray()) {
                stdout.write(symbol.getLevel() + " " + symbol.getName() + " " + "ConstCharArray\n");
            } else if (symbol.isConstIntArray()) {
                stdout.write(symbol.getLevel() + " " + symbol.getName() + " " + "ConstIntArray\n");
            } else if (symbol.isChar()) {
                stdout.write(symbol.getLevel() + " " + symbol.getName() + " " + "Char\n");
            } else if (symbol.isInt()) {
                stdout.write(symbol.getLevel() + " " + symbol.getName() + " " + "Int\n");
            } else if (symbol.isCharArray()) {
                stdout.write(symbol.getLevel() + " " + symbol.getName() + " " + "CharArray\n");
            } else if (symbol.isIntArray()) {
                stdout.write(symbol.getLevel() + " " + symbol.getName() + " " + "IntArray\n");
            }
        }
    }
}
