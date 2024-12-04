package backend.Assembly;

public class JAsm extends Asm {
    private String op; //j || jal
    private LabelAsm labelAsm;
    
    public JAsm(String op, LabelAsm label) {
        this.op = op;
        this.labelAsm = label;
    }
    
    @Override
    public String toString() {
        return "\t" + op + " " + labelAsm.getName();
    }
}
