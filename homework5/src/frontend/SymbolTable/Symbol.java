package frontend.SymbolTable;

import java.util.ArrayList;

public class Symbol {
    private String name;
    private String kind;
    private String type;
    private int level;
    private boolean isArray;
    private int arrayLength;
    private boolean isGlobal; //是否为全局变量
    private SymbolTable symbolTable; //当该symbol是func时，可以拥有自己的symbolTable，方便选取参数
    private ArrayList<Integer> parasType; //当kind是func时，此内容存放形参para的类型
    private int value; //是常数时的值
    private ArrayList<Integer> values; //是数组时对应的数组元素值
    private int virtualReg; //局部变量的虚拟寄存器
    
    public Symbol(String name, String kind, String type, int level) {
        this.name = name;
        this.kind = kind;
        this.type = type;
        this.level = level;
        this.isArray = false;
        this.arrayLength = 0;
        this.parasType = new ArrayList<>();
        this.value = 0;
        this.values = new ArrayList<>();
    }
    
    public Symbol(String name, String kind, String type, int level, boolean isGlobal) {
        this.name = name;
        this.kind = kind;
        this.type = type;
        this.level = level;
        this.isGlobal = isGlobal;
        this.isArray = false;
        this.arrayLength = 0;
        this.parasType = new ArrayList<>();
        this.value = 0;
        this.values = new ArrayList<>();
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
    
    public int getArrayLength() {
        return arrayLength;
    }
    
    public int getValue() {
        return value;
    }
    
    public ArrayList<Integer> getValues() {
        return values;
    }
    
    public int getValue(int index) {
        return values.get(index);
    }
    
    public boolean getIsGlobal() {
        return isGlobal;
    }
    
    public String getVirtualReg() { //全局变量只需要@+name即可，局部变量只需要%+virtualReg
        if (isGlobal) {
            return "@" + name;
        } else {
            return "%" + virtualReg;
        }
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
    
    public void setArrayLength(int arrayLength) {
        this.arrayLength = arrayLength;
    }
    
    public void setValue(int value) {
        this.value = value;
    }
    
    public void setValues(ArrayList<Integer> values) {
        this.values = values;
    }
    
    public void setIsGlobal(boolean isGlobal) {
        this.isGlobal = isGlobal;
    }
    
    public void setVirtualReg(int virtualReg) { //局部变量的赋值
        this.virtualReg = virtualReg;
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
