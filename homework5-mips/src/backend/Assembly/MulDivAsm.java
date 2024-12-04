package backend.Assembly;

import backend.Register;

public class MulDivAsm extends Asm {
    private Register rs;
    private Register rt;
    private String op;
    
    public MulDivAsm(Register rs, Register rt, String op) {
        this.rs = rs;
        this.rt = rt;
        this.op = op;
    }
    
    @Override
    public String toString() {
        return "\t" + op + " " + rs + ", " + rt;
    }
}
