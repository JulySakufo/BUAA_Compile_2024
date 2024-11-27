package middle.Value.Instruction;

import middle.Value.Value;

public class TruncInstr extends Instr {
    private int virtualReg;
    private boolean flag;
    
    public TruncInstr(int virtualReg) {
        super(null, "%" + virtualReg);
        this.virtualReg = virtualReg;
        this.flag = false;
    }
    
    public TruncInstr(int virtualReg, Value operand) {
        super(null, "%" + virtualReg);
        this.virtualReg = virtualReg;
        operands.add(operand);
        this.flag = true;
    }
    
    @Override
    public String toString() { //这是截断指令,用于getchar
        if (!flag) {
            return name + " = trunc " + "i32 " + "%" + (virtualReg - 1) + " to i8";
        } else {
            return name + " = trunc " + "i32 " + operands.get(0).getName() + " to i8";
        }
    }
}
