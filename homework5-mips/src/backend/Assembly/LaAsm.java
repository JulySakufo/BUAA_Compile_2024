package backend.Assembly;

import backend.Register;

public class LaAsm extends Asm {
    private Register rt;
    private LabelAsm labelAsm;
    
    public LaAsm(Register rt, LabelAsm labelAsm) {
        this.rt = rt;
        this.labelAsm = labelAsm;
    }
    
    @Override
    public String toString() {
        return "\tla" + " " + rt + ", " + labelAsm.getName();
    }
}
