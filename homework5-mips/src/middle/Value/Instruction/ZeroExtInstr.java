package middle.Value.Instruction;

import middle.Type.Type;
import middle.Value.Value;

public class ZeroExtInstr extends Instr {
    private Type selfType;
    
    public ZeroExtInstr(Type type, String name, Value operand, Type selfType) { //selfType代表这条指令本身的type
        super(type, name);
        this.selfType = selfType;
        operands.add(operand);
    }
    
    @Override
    public String toString() {
        return name + " = zext " + selfType + " " + operands.get(0).getName() + " to i32";
    }
}
