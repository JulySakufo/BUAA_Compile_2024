package middle.Value.Instruction;

import middle.Type.Type;

public class AllocaInstr extends Instr {
    
    
    public AllocaInstr(Type type, String name) {
        super(type, name);
    }
    
    @Override
    public String toString() {
        return name + " = alloca " + type;
    }
}
