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
        String instrOp = null;
        switch (compareOp) {
            case "==":
                instrOp = "eq";
                break;
            case "!=":
                instrOp = "ne";
                break;
            case "<":
                instrOp = "slt";
                break;
            case ">":
                instrOp = "sgt";
                break;
            case "<=":
                instrOp = "sle";
                break;
            case ">=":
                instrOp = "sge";
                break;
        }
        return name + " = icmp " + instrOp + " " + compareType + " " + operands.get(0).getName() + ", " + operands.get(1).getName();
    }
}
