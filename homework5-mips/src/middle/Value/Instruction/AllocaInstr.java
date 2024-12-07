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
         */
        if (type instanceof ArrayType) { //%reg = alloca [arrayLength x i32]
            int allocOffset = -((ArrayType) type).getArrayLength() * 4;
            RegisterController.getRegisterController().addCurOffset(allocOffset);//申请连续的数组空间
            RegisterController.getRegisterController().addValue(name); //将%reg与内存建立关系，后面的getElement以此索引
        } else { //%reg = alloca i32(i8) || i32*(i8*)
            int offset = -4;
            RegisterController.getRegisterController().addCurOffset(offset); //申请一个空间
            RegisterController.getRegisterController().addValue(name); //保存在sp里，建立%reg与offset的联系
        }
    }
}
