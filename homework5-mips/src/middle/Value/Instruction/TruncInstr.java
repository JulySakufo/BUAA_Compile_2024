package middle.Value.Instruction;

import backend.Assembly.AluIAsm;
import backend.Assembly.MemAsm;
import backend.MipsGenerator;
import backend.Register;
import backend.RegisterController;
import middle.Value.Value;

public class TruncInstr extends Instr {
    
    public TruncInstr(int virtualReg, Value operand) {
        super(null, "%" + virtualReg);
        operands.add(operand);
    }
    
    @Override
    public String toString() { //这是截断指令,用于getchar
        return name + " = trunc " + "i32 " + operands.get(0).getName() + " to i8";
    }
    
    @Override
    public void generateMips() {
        int allocaOffset = -4;
        RegisterController.getRegisterController().addCurOffset(allocaOffset);
        RegisterController.getRegisterController().addValue(name);
        AluIAsm addiuAsm = new AluIAsm("addiu", Register.SP, Register.SP, allocaOffset);
        MipsGenerator.getMipsGenerator().addAsm(addiuAsm);
        RegisterController.getRegisterController().loadToRegisterFromMemory(operands.get(0).getName(), Register.T0);
        AluIAsm andiAsm = new AluIAsm("andi", Register.T1, Register.T0, 0xff);
        MipsGenerator.getMipsGenerator().addAsm(andiAsm);
        RegisterController.getRegisterController().storeToMemoryFromRegister(Register.T1,name);
    }
}
