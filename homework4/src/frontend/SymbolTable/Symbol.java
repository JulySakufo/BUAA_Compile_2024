package frontend.SymbolTable;

import java.util.ArrayList;

public class Symbol {
    private String name;
    private String kind;
    private String type;
    private int level;
    private boolean isArray;
    private SymbolTable symbolTable; //当该symbol是func时，可以拥有自己的symbolTable，方便选取参数
    private ArrayList<Integer> parasType; //当kind是func时，此内容存放形参para的类型
    
    public Symbol(String name, String kind, String type, int level) {
        this.name = name;
        this.kind = kind;
        this.type = type;
        this.level = level;
        this.isArray = false;
        this.parasType = new ArrayList<>();
    }
    
    public String getName() {
        return name;
    }
    
    public String getKind() {
        return kind;
    }
    
    public String getType() {
        return type;
    }
    
    public int getLevel() {
        return level;
    }
    
    public boolean getIsArray() {
        return isArray;
    }
    
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }
    
    public ArrayList<Integer> getParasType() {
        return parasType;
    }
    
    public void setParasType(ArrayList<Integer> parasType) {
        this.parasType = parasType;
    }
    
    public boolean isConst() {
        return kind.equals("const");
    }
    
    public void setIsArray(boolean isArray) {
        this.isArray = isArray;
    }
    
    public void setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }
    
    public boolean isConstChar() {
        return isConst() && type.equals("char") && !getIsArray();
    }
    
    public boolean isConstInt() {
        return isConst() && type.equals("int") && !getIsArray();
    }
    
    public boolean isConstCharArray() {
        return isConst() && type.equals("char") && getIsArray();
    }
    
    public boolean isConstIntArray() {
        return isConst() && type.equals("int") && getIsArray();
    }
    
    public boolean isChar() {
        return !isConst() && type.equals("char") && !getIsArray();
    }
    
    public boolean isInt() {
        return !isConst() && type.equals("int") && !getIsArray();
    }
    
    public boolean isCharArray() {
        return !isConst() && type.equals("char") && getIsArray();
    }
    
    public boolean isIntArray() {
        return !isConst() && type.equals("int") && getIsArray();
    }
    
    public boolean isVoidFunc() {
        return kind.equals("func") && type.equals("void");
    }
    
    public boolean isCharFunc() {
        return kind.equals("func") && type.equals("char");
    }
    
    public boolean isIntFunc() {
        return kind.equals("func") && type.equals("int");
    }
}
