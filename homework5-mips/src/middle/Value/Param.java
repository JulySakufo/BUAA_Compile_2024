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
    public void generateMips() { //只需要分配空间即可，剩余的事不归它管
        int allocOffset = -4;
        RegisterController.getRegisterController().addCurOffset(allocOffset);
        RegisterController.getRegisterController().addValue(name);
    }
}
