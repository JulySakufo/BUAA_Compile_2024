package backend.Assembly;

import backend.Register;

public class AluIAsm extends IAsm {
    /*
    * addi, addiu, slti, sltiu, andi, ori, xori, lui, sll, srl, subiu
    */
    private String op;
    
    public AluIAsm(Register rs, Register rt, int immediate, String op) {
        super(rs, rt, immediate);
        this.op = op;
    }
    
    @Override
    public String toString() {
        return "\t" + op + " " + rt + ", " + rs + ", " + immediate;
    }
}
