package backend.Assembly;

import backend.Register;

public class HiLoAsm extends Asm {
    private Register rs;
    private String op;
    
    public HiLoAsm(Register rs, String op) {
        this.rs = rs;
        this.op = op;
    }
    
    @Override
    public String toString() {
        return "\t" + op + " " + rs;
    }
}
