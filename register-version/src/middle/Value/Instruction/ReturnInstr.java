package middle.Value.Instruction;

import backend.Assembly.JrAsm;
import backend.Assembly.MoveAsm;
import backend.MipsGenerator;
import backend.Register;
import backend.RegisterController;
import middle.Type.Type;
import middle.Type.VoidType;
import middle.Value.Value;

public class ReturnInstr extends Instr {
    
    public ReturnInstr(Type type) {
        super(type, null);
    }
    
    public ReturnInstr(Type type, Value operand) {
        super(type, operand.getName());
    }
    
    @Override
    public String toString() {
        if (type instanceof VoidType) {
            return "ret " + type;
        } else {
            return "ret " + type + " " + name;
        }
    }
    
    @Override
    public void generateMips() {
        if (!(type instanceof VoidType)) { //如果有返回值，存到v0中
            Register register = RegisterController.getRegisterController().getRegister(name);
            if (register == null) {
                RegisterController.getRegisterController().loadToRegisterFromMemory(name, Register.V0);
            } else {
                MoveAsm moveAsm = new MoveAsm(Register.V0, register);
                MipsGenerator.getMipsGenerator().addAsm(moveAsm);
            }
        }
        JrAsm jrAsm = new JrAsm(Register.RA);
        MipsGenerator.getMipsGenerator().addAsm(jrAsm);
    }
}
