package backend.Assembly;

import backend.Register;

public class AluRAsm extends RAsm {
    /*
    * addu, subu, and, or, xor, slt, sgt, sge, seq(set equal), sne, sle
    */
    private String op;
    
    public AluRAsm(Register rs, Register rt, Register rd, String op) {
        super(rs, rt, rd);
        this.op = op;
    }
    
    @Override
    public String toString() {
        return "\t" + op + " " + rd + ", " + rs + ", " + rt;
    }
}
