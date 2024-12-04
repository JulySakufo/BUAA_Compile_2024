package backend.Assembly;

import backend.Register;

public class IAsm extends Asm {
    protected Register rs;
    protected Register rt;
    protected int immediate;
    
    public IAsm(Register rs, Register rt, int immediate) {
        this.rs = rs;
        this.rt = rt;
        this.immediate = immediate;
    }
}
