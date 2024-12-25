package middle.Value.Instruction;

import backend.Assembly.AluIAsm;
import backend.Assembly.MemAsm;
import backend.Assembly.MoveAsm;
import backend.MipsGenerator;
import backend.Register;
import backend.RegisterController;
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
    
    @Override
    public void generateMips() { //扩展只需要分配一个空间即可
        RegisterController.getRegisterController().distributeRegister(this);
        Register operandRegister = RegisterController.getRegisterController().getRegister(operands.get(0).getName());
        if (operandRegister == null) {
            operandRegister = Register.K1;
            RegisterController.getRegisterController().loadToRegisterFromMemory(operands.get(0).getName(), operandRegister); //加载到K1中
        }
        Register nameRegister = RegisterController.getRegisterController().getRegister(name);
        if (nameRegister == null) { //存入内存
            RegisterController.getRegisterController().storeToMemoryFromRegister(operandRegister, name);
        } else { //寄存器暂存
            MoveAsm moveAsm = new MoveAsm(nameRegister, operandRegister);
            MipsGenerator.getMipsGenerator().addAsm(moveAsm);
        }
    }
}
