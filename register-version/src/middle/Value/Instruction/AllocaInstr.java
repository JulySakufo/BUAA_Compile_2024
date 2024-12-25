package middle.Value.Instruction;

import backend.Assembly.AluIAsm;
import backend.Assembly.MemAsm;
import backend.MipsGenerator;
import backend.Register;
import backend.RegisterController;
import middle.Type.ArrayType;
import middle.Type.Type;

public class AllocaInstr extends Instr {
    
    
    public AllocaInstr(Type type, String name) {
        super(type, name);
    }
    
    @Override
    public String toString() {
        return name + " = alloca " + type;
    }
    
    @Override
    public void generateMips() {
        /*
         * 明确凡是%reg = ...的结构，对于栈式mips，都要跟左侧的reg申请空间，把它放在栈里
         * 指令形式:
         * (1) %reg = alloca [arrayLength x i32]
         * (2) %reg = alloca i32(i8) || i32*(i8*)
         */
        //尝试分配寄存器，如果没寄存器实际上是保存在内存中
        RegisterController.getRegisterController().distributeRegister(this);
    }
}
