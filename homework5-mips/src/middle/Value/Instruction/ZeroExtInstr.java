package middle.Value.Instruction;

import backend.Assembly.AluIAsm;
import backend.Assembly.MemAsm;
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
        int allocOffset = -4;
        RegisterController.getRegisterController().addCurOffset(allocOffset);
        RegisterController.getRegisterController().addValue(name);
        AluIAsm addiuAsm = new AluIAsm("addiu", Register.SP, Register.SP, allocOffset);
        MipsGenerator.getMipsGenerator().addAsm(addiuAsm);
        RegisterController.getRegisterController().loadToRegisterFromMemory(operands.get(0).getName(), Register.T0); //加载到t0中
        RegisterController.getRegisterController().storeToMemoryFromRegister(Register.T0, name);
    }
}
