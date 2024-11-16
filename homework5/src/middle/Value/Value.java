package middle.Value;

import middle.Type.Type;

public class Value {
    protected Type type;
    protected String name;
    
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
