package middle.Value;

import middle.Type.Type;

import java.util.ArrayList;

public class User extends Value {
    protected ArrayList<Value> operands; //使用的操作数
    
    public User(Type type, String name) {
        super(type, name);
        this.operands = new ArrayList<>();
    }
}
