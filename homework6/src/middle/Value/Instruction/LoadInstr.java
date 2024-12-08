package middle.Value.Instruction;

import backend.Assembly.AluIAsm;
import backend.Assembly.MemAsm;
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
        int allocOffset = -4;
        RegisterController.getRegisterController().addCurOffset(allocOffset); //为了将这个reg1存进去，新开辟空间
        RegisterController.getRegisterController().addValue(name); //建立%reg与内存的关系
        int regOffset = RegisterController.getRegisterController().getValueOffset(name);
        RegisterController.getRegisterController().loadToRegisterFromMemory(from, Register.T0); //值加载到t0中
        MemAsm swAsm = new MemAsm("sw", Register.T0, regOffset, Register.SP);
        MipsGenerator.getMipsGenerator().addAsm(swAsm);
    }
}
