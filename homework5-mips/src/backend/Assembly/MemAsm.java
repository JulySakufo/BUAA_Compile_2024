package backend.Assembly;

import backend.Register;

public class MemAsm extends IAsm {
    private String op;
    
    public MemAsm(Register rs, Register rt, int offset, String op) {
        super(rs, rt, offset);
        this.op = op;
    }
    
    @Override
    public String toString() {
        return "\t" + op + " " + rt + ", " + immediate + "(" + rs + ")";
    }
}
