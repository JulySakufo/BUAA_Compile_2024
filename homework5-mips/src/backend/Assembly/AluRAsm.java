package backend.Assembly;

import backend.Register;

public class AluRAsm extends RAsm {
    /*
     * addu, subu, and, or, xor, slt, sgt, sge, seq(set equal), sne, sle
     */
    private String op;
    
    public AluRAsm(String op, Register rd, Register rs, Register rt) {
        super(rs, rt, rd);
        this.op = op;
    }
    
    @Override
    public String toString() {
        return "\t" + op + " " + rd + ", " + rs + ", " + rt;
    }
}
