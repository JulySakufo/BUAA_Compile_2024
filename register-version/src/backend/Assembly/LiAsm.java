package backend.Assembly;

import backend.Register;

public class LiAsm extends Asm{
    private Register rt;
    private int immediate;
    
    public LiAsm(Register rt, int immediate) {
        this.rt = rt;
        this.immediate = immediate;
    }
    
    @Override
    public String toString() {
        return "\tli " + rt + ", " + immediate;
    }
}
