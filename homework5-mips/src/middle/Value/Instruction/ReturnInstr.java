package middle.Value.Instruction;

import backend.Assembly.JrAsm;
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
            RegisterController.getRegisterController().dealLwAndLi(name, Register.V0);
        }
        JrAsm jrAsm = new JrAsm(Register.RA);
        MipsGenerator.getMipsGenerator().addAsm(jrAsm);
    }
}
