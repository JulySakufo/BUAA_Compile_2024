package middle.Value;

import backend.Assembly.AluIAsm;
import backend.MipsGenerator;
import backend.Register;
import backend.RegisterController;
import middle.Type.*;

public class Param extends Value {
    public Param(Type type, String name) {
        super(type, name);
    }
    
    @Override
    public String toString() {
        return type + " " + name;
    }
    
    @Override
    public void generateMips() {
        int allocOffset = -4;
        RegisterController.getRegisterController().addCurOffset(allocOffset);
        RegisterController.getRegisterController().addValue(name);
        AluIAsm addiuAsm = new AluIAsm("addiu", Register.SP, Register.SP, allocOffset);
        MipsGenerator.getMipsGenerator().addAsm(addiuAsm);
        /*TODO 感觉逻辑还是没太想清楚 等其他写完了再回来写*/
        if (type instanceof Integer32PointerType || type instanceof Integer8PointerType) { //这是个i32*，里面装的是地址
            RegisterController.getRegisterController().addContent(name);
        }
    }
}
