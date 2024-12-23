package backend.Assembly;

import backend.Register;

public class MemAsm extends IAsm {
    private String op;
    
    public MemAsm(String op, Register rt, int offset, Register rs) {
        super(rs, rt, offset);
        this.op = op;
    }
    
    @Override
    public String toString() {
        return "\t" + op + " " + rt + ", " + immediate + "(" + rs + ")";
    }
}
