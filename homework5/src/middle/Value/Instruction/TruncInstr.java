package middle.Value.Instruction;

public class TruncInstr extends Instr {
    private int virtualReg;
    
    public TruncInstr(int virtualReg) {
        super(null, null);
        this.virtualReg = virtualReg;
    }
    
    @Override
    public String toString() { //这是截断指令,用于getchar
        return "%" + virtualReg + " = trunc " + "i32 " + "%" + (virtualReg - 1) + " to i8";
    }
}
