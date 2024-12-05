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
    public void generateMips() { //扩展不需要干啥
        int allocOffset = -4;
        RegisterController.getRegisterController().addCurOffset(allocOffset);
        RegisterController.getRegisterController().addValue(name);
        AluIAsm addiuAsm = new AluIAsm("addiu", Register.SP, Register.SP, allocOffset);
        MipsGenerator.getMipsGenerator().addAsm(addiuAsm);
        RegisterController.getRegisterController().loadToRegisterFromMemory(operands.get(0).getName(), Register.T0); //加载到t0中
        int regOffset = RegisterController.getRegisterController().getValueOffset(name);
        MemAsm swAsm = new MemAsm("sw", Register.T0, regOffset, Register.SP);
        MipsGenerator.getMipsGenerator().addAsm(swAsm); //存入
    }
}
