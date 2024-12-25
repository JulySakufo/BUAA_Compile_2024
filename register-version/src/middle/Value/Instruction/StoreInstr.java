package middle.Value.Instruction;

import backend.Assembly.MoveAsm;
import backend.MipsGenerator;
import backend.Register;
import backend.RegisterController;
import middle.Type.Integer32PointerType;
import middle.Type.Integer32Type;
import middle.Type.Integer8PointerType;
import middle.Type.Type;
import middle.Value.Value;

public class StoreInstr extends Instr {
    private Type pointerType;
    
    public StoreInstr(Type type, String name, int value) {
        super(type, name);
        this.pointerType = type instanceof Integer32Type ? new Integer32PointerType() : new Integer8PointerType();
        operands.add(new Value(new Type(), String.valueOf(value)));
    }
    
    public StoreInstr(Type type, Value operand, String to) {
        super(type, to);
        this.pointerType = type instanceof Integer32Type ? new Integer32PointerType() : new Integer8PointerType();
        operands.add(operand); //把值存进从父类继承的operands里
    }
    
    @Override
    public String toString() {
        return "store " + type + " " + operands.get(0).getName() + ", " + pointerType + " " + name;
    }
    
    @Override
    public void generateMips() {
        /*
         *  store i32 1, i32* %1
         *  store i32 operand, i32* %reg2
         */
        Register fromRegister = RegisterController.getRegisterController().getRegister(operands.get(0).getName()); //operand的寄存器
        Register nameRegister = RegisterController.getRegisterController().getRegister(name); //to的寄存器
        if (fromRegister == null) { //operand没有寄存器，先将值它虚拟寄存器的值存到K1寄存器里
            fromRegister = Register.K1; //当前指令用完即可被覆盖掉,K0在RegisterController用了，为了避免冲突
            RegisterController.getRegisterController().loadToRegisterFromMemory(operands.get(0).getName(), fromRegister); //从内存中加载值到fromReg中
        }
        if (nameRegister != null) { //保存到该寄存器就行，反正是用寄存器的值
            MoveAsm moveAsm = new MoveAsm(nameRegister, fromRegister);
            MipsGenerator.getMipsGenerator().addAsm(moveAsm);
        } else {
            RegisterController.getRegisterController().storeToMemoryFromRegister(fromRegister, name);
        }
    }
}
