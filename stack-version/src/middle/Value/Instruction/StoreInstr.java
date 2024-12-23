package middle.Value.Instruction;

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
         * store i32 1, i32* %1
         * store i32 operand, i32* %reg2
         */
        RegisterController.getRegisterController().loadToRegisterFromMemory(operands.get(0).getName(), Register.T0);
        RegisterController.getRegisterController().storeToMemoryFromRegister(Register.T0, name);
    }
}
