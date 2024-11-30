package middle.Value.Instruction;

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
}
