package backend.Assembly;

import backend.Register;

public class JrAsm extends Asm {
    private Register rs;
    
    public JrAsm(Register rs) {
        this.rs = rs;
    }
    
    @Override
    public String toString() {
        return "\tjr" + " " + rs;
    }
}
