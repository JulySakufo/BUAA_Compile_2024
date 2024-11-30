package middle.Value.Instruction;

import middle.Type.Integer32PointerType;
import middle.Type.Integer32Type;
import middle.Type.Integer8PointerType;
import middle.Type.Type;
import middle.Value.Value;

public class StoreInstr extends Instr {
    private Type pointerType;
    private int value;
    private String to;
    private int flag;
    
    public StoreInstr(Type type, String name, int value) {
        super(type, name);
        this.pointerType = type instanceof Integer32Type ? new Integer32PointerType() : new Integer8PointerType();
        this.value = value;
        this.flag = 0;
    }
    
    public StoreInstr(Type type, String from, String to) {
        super(type, from);
        this.pointerType = type instanceof Integer32Type ? new Integer32PointerType() : new Integer8PointerType();
        this.to = to;
        this.flag = 1;
    }
    
    public StoreInstr(Type type, Value operand, String to) {
        super(type, to);
        this.pointerType = type instanceof Integer32Type ? new Integer32PointerType() : new Integer8PointerType();
        operands.add(operand); //把值存进从父类继承的operands里
        this.flag = 2;
    }
    
    @Override
    public String toString() {
        if (flag == 0) {
            return "store " + type + " " + value + ", " + pointerType + " " + name;
        } else if (flag == 1) {
            return "store " + type + " " + name + ", " + pointerType + " " + to;
        } else {
            return "store " + type + " " + operands.get(0).getName() + ", " + pointerType + " " + name;
        }
    }
}
