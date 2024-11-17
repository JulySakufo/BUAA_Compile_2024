package middle.Value.Instruction;

import middle.Type.Type;
import middle.Type.VoidType;
import middle.Value.Value;

public class ReturnInstr extends Instr {
    
    public ReturnInstr(Type type) {
        super(type, null);
    }
    
    public ReturnInstr(Type type, Value operand) {
        super(type, operand.getName());
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
