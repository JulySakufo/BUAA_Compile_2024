package middle.Value.Instruction;

import backend.Assembly.LiAsm;
import backend.Assembly.MemAsm;
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
    private int value;
    private String to;
    private boolean flag;
    
    public StoreInstr(Type type, String name, int value) {
        super(type, name);
        this.pointerType = type instanceof Integer32Type ? new Integer32PointerType() : new Integer8PointerType();
        this.value = value;
        this.flag = false;
    }
    
    public StoreInstr(Type type, Value operand, String to) {
        super(type, to);
        this.pointerType = type instanceof Integer32Type ? new Integer32PointerType() : new Integer8PointerType();
        operands.add(operand); //把值存进从父类继承的operands里
        this.flag = true;
    }
    
    public boolean isOperandRegister(Value operand) {
        return operand.getName().charAt(0) == '%';
    }
    
    @Override
    public String toString() {
        if (!flag) {
            return "store " + type + " " + value + ", " + pointerType + " " + name;
        } else {
            return "store " + type + " " + operands.get(0).getName() + ", " + pointerType + " " + name;
        }
    }
    
    @Override
    public void generateMips() {
        if (!flag) { //store i32 1, i32* %1
            RegisterController.getRegisterController().loadToRegisterFromMemory(String.valueOf(value), Register.T0);
            RegisterController.getRegisterController().storeToMemoryFromRegister(Register.T0, name);
        } else { //store i32 operand, i32* %reg2
            RegisterController.getRegisterController().loadToRegisterFromMemory(operands.get(0).getName(), Register.T0);
            RegisterController.getRegisterController().storeToMemoryFromRegister(Register.T0, name);
        }
    }
}
