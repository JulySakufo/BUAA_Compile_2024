package middle.Value.Instruction;

import middle.Type.Integer32PointerType;
import middle.Type.Integer32Type;
import middle.Type.Integer8PointerType;
import middle.Type.Type;

public class StoreInstr extends Instr {
    private Type pointerType;
    private int value;
    private String to;
    private boolean flag;
    
    public StoreInstr(Type type, String name, int value) {
        super(type, name);
        this.pointerType = type instanceof Integer32Type ? new Integer32PointerType() : new Integer8PointerType();
        this.value = value;
        this.flag = false;
    }
    
    public StoreInstr(Type type, String from, String to) {
        super(type, from);
        this.pointerType = type instanceof Integer32Type ? new Integer32PointerType() : new Integer8PointerType();
        this.to = to;
        this.flag = true;
    }
    
    @Override
    public String toString() {
        if (!flag) {
            return "store " + type + " " + value + ", " + pointerType + " " + name;
        } else {
            return "store " + type + " " + name + ", " + pointerType + " " + to;
        }
    }
}
