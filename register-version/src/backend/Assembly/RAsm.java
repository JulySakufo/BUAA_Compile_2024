package backend.Assembly;

import backend.Register;

public class RAsm extends Asm {
    protected Register rs;
    protected Register rt;
    protected Register rd;
    
    public RAsm(Register rs, Register rt, Register rd) {
        this.rs = rs;
        this.rt = rt;
        this.rd = rd;
    }
    
}
