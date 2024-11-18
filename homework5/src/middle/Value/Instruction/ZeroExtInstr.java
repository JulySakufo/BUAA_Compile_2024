package middle.Value.Instruction;

import middle.Type.Type;
import middle.Value.Value;

public class ZeroExtInstr extends Instr {
    public ZeroExtInstr(Type type, String name, Value operand) {
        super(type, name);
        operands.add(operand);
    }
    
    @Override
    public String toString() {
        return name + " = zext i8 " + operands.get(0).getName() + " to i32";
    }
}
