package middle.Value.Instruction;

import backend.Assembly.AluIAsm;
import backend.Assembly.AluRAsm;
import backend.Assembly.MemAsm;
import backend.MipsGenerator;
import backend.Register;
import backend.RegisterController;
import middle.Type.BoolType;
import middle.Type.Type;
import middle.Value.Value;

public class ICmpInstr extends Instr {
    private String compareOp;
    private Type compareType;
    
    public ICmpInstr(Type compareType, String name, Value operand1, Value operand2, String compareOp) {
        super(new BoolType(), name);
        this.compareType = compareType;
        this.operands.add(operand1);
        this.operands.add(operand2);
        this.compareOp = compareOp;
    }
    
    @Override
    public String toString() {
        switch (compareOp) {
            case "==":
                compareOp = "eq";
                break;
            case "!=":
                compareOp = "ne";
                break;
            case "<":
                compareOp = "slt";
                break;
            case ">":
                compareOp = "sgt";
                break;
            case "<=":
                compareOp = "sle";
                break;
            case ">=":
                compareOp = "sge";
                break;
        }
        return name + " = icmp " + compareOp + " " + compareType + " " + operands.get(0).getName() + ", " + operands.get(1).getName();
    }
    
    @Override
    public void generateMips() {
        int regOffset = RegisterController.getRegisterController().getValueOffset(name);
        RegisterController.getRegisterController().dealAluRAsmRsRt(name, operands);
        String asmOp = null;
        switch (compareOp) {
            case "==":
                asmOp = "seq";
                break;
            case "!=":
                asmOp = "sne";
                break;
            case "<":
                asmOp = "slt";
                break;
            case ">":
                asmOp = "sgt";
                break;
            case "<=":
                asmOp = "sle";
                break;
            case ">=":
                asmOp = "sge";
                break;
        }
        AluRAsm aluRAsm = new AluRAsm(asmOp, Register.T2, Register.T0, Register.T1);
        MipsGenerator.getMipsGenerator().addAsm(aluRAsm);
        MemAsm swAsm = new MemAsm("sw", Register.T2, regOffset, Register.SP);
        MipsGenerator.getMipsGenerator().addAsm(swAsm);
    }
}
