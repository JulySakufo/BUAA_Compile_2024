package backend.Assembly;

import backend.Register;

public class MoveAsm extends Asm {
    private Register rs;
    private Register rt;
    
    public MoveAsm(Register rt, Register rs) {
        this.rs = rs;
        this.rt = rt;
    }
    
    @Override
    public String toString() {
        return "\tmove " + rt + ", " + rs;
    }
}
