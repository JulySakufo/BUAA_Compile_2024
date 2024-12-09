package middle.Value.Instruction;

import backend.Assembly.AluIAsm;
import backend.Assembly.MoveAsm;
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
        RegisterController.getRegisterController().distributeRegister(this);
        Register operandRegister = RegisterController.getRegisterController().getRegister(operands.get(0).getName());
        if (operandRegister == null) {
            operandRegister = Register.K0;
            RegisterController.getRegisterController().loadToRegisterFromMemory(operands.get(0).getName(), operandRegister);
        }
        AluIAsm andiAsm = new AluIAsm("andi", Register.K0, operandRegister, 0xff); //截断后的值始终保存在K0中
        MipsGenerator.getMipsGenerator().addAsm(andiAsm);
        Register nameRegister = RegisterController.getRegisterController().getRegister(name);
        if (nameRegister == null) {
            RegisterController.getRegisterController().storeToMemoryFromRegister(Register.K0, name);
        } else {
            MoveAsm moveAsm = new MoveAsm(nameRegister, Register.K0);
            MipsGenerator.getMipsGenerator().addAsm(moveAsm);
        }
    }
}
