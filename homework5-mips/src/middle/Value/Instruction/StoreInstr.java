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
            LiAsm liAsm = new LiAsm(Register.T0, value);
            MipsGenerator.getMipsGenerator().addAsm(liAsm); //将值加载到寄存器t0里
            int offset = RegisterController.getRegisterController().getValueOffset(name);
            MemAsm swAsm = new MemAsm(Register.SP, Register.T0, offset, "sw"); //存到该寄存器对应的内存位置
            MipsGenerator.getMipsGenerator().addAsm(swAsm);
        } else { //store i32 operand, i32* %reg2
            if (isOperandRegister(operands.get(0))) { //operand是寄存器
                int reg1Offset = RegisterController.getRegisterController().getValueOffset(operands.get(0).getName());
                MemAsm lwAsm = new MemAsm(Register.SP, Register.T0, reg1Offset, "lw");
                MipsGenerator.getMipsGenerator().addAsm(lwAsm);
                int reg2Offset = RegisterController.getRegisterController().getValueOffset(name);
                MemAsm swAsm = new MemAsm(Register.SP, Register.T0, reg2Offset, "sw");
                MipsGenerator.getMipsGenerator().addAsm(swAsm);
            } else { //是个值
                int value = Integer.parseInt(operands.get(0).getName());
                LiAsm liAsm = new LiAsm(Register.T0, value);
                MipsGenerator.getMipsGenerator().addAsm(liAsm);
                int offset = RegisterController.getRegisterController().getValueOffset(name);
                MemAsm swAsm = new MemAsm(Register.SP, Register.T0, offset, "sw");
                MipsGenerator.getMipsGenerator().addAsm(swAsm);
            }
            
        }
    }
}
