package middle.Value;

import middle.Type.Type;

public class Param extends Value {
    public Param(Type type, String name) {
        super(type, name);
    }
    
    @Override
    public String toString() {
        return type + " " + name;
    }
}
