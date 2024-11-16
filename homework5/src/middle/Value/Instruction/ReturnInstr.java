package middle.Value.Instruction;

import middle.Type.Type;
import middle.Type.VoidType;

public class ReturnInstr extends Instr {
    public ReturnInstr(Type type, String name) {
        super(type, name);
    }
    
    @Override
    public String toString() {
        if (type instanceof VoidType) {
            return "ret " + type;
        } else {
            return "ret " + type + " " + name;
        }
    }
}
