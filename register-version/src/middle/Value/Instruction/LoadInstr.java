package middle.Value.Instruction;

import backend.Assembly.AluIAsm;
import backend.Assembly.MemAsm;
import backend.Assembly.MoveAsm;
import backend.MipsGenerator;
import backend.Register;
import backend.RegisterController;
import middle.Type.Type;

public class LoadInstr extends Instr {
    private String from;
    
    public LoadInstr(Type type, String from, String to) { //to是当前块的虚拟寄存器的编号
        super(type, to);
        this.from = from;
    }
    
    @Override
    public String toString() { //from是全局变量或局部变量的虚拟寄存器
        return name + " = load " + type + ", " + type + "* " + from;
    }
    
    @Override
    public void generateMips() { //%reg1 = load i32, i32* %reg2
        RegisterController.getRegisterController().distributeRegister(this);
        Register fromRegister = RegisterController.getRegisterController().getRegister(from);
        Register nameRegister = RegisterController.getRegisterController().getRegister(name);
        if (fromRegister == null) {
            fromRegister = Register.K1;
            RegisterController.getRegisterController().loadToRegisterFromMemory(from, fromRegister); //将值加载到K1中
        }
        if (nameRegister != null) { //当前有寄存器，存在寄存器里
            MoveAsm moveAsm = new MoveAsm(nameRegister, fromRegister);
            MipsGenerator.getMipsGenerator().addAsm(moveAsm);
        } else { //无寄存器，存在内存里
            int regOffset = RegisterController.getRegisterController().getValueOffset(name);
            MemAsm swAsm = new MemAsm("sw", fromRegister, regOffset, Register.SP);
            MipsGenerator.getMipsGenerator().addAsm(swAsm);
        }
    }
}
