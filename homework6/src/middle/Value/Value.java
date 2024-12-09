package middle.Value;

import backend.Register;
import middle.Type.Type;

public class Value {
    protected Type type;
    protected String name; //变量名或者寄存器的名字
    protected Register register; //value与使用的寄存器对应起来
    
    public Value(Type type, String name) {
        this.type = type;
        this.name = name;
        this.register = null;
    }
    
    public Type getType() {
        return type;
    }
    
    public String getName() {
        return name;
    }
    
    public Register getRegister() {
        return register;
    }
    
    public void setRegister(Register register) {
        this.register = register;
    }
    
    public void generateMips() {
    
    }
}
