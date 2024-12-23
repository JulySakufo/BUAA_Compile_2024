package backend.Assembly;

import backend.Register;

public class BranchAsm extends IAsm {
    /*
     * beq, bne, blez, bgtz, bltz, bgez
     */
    private String op;
    private LabelAsm labelAsm;
    
    public BranchAsm(String op, Register rs, Register rt, LabelAsm labelAsm) {
        super(rs, rt, 0);
        this.op = op;
        this.labelAsm = labelAsm;
    }
    
    @Override
    public String toString() {
        return "\t" + op + " " + rs + ", " + rt + ", " + labelAsm.getName();
    }
}
