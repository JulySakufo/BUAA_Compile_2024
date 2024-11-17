package middle.Value;

import middle.Type.Type;

public class Value {
    protected Type type;
    protected String name; //变量名或者寄存器的名字
    
    public Value(Type type, String name) {
        this.type = type;
        this.name = name;
    }
    
    public Type getType() {
        return type;
    }
    
    public String getName() {
        return name;
    }
}
