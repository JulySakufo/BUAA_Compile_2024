package middle.Value.Instruction;

import middle.Type.Type;
import middle.Value.Value;

public class BinaryInstr extends Instr {
    private String op; //二目运算符
    
    public BinaryInstr(Type type, Value operand1, Value operand2, String op, String virtualReg) {
        super(type, virtualReg);
        operands.add(operand1);
        operands.add(operand2);
        this.op = op;
    }
    
    public BinaryInstr(Type type, Value operand1, String op, String virtualReg) {
        super(type, virtualReg);
        operands.add(operand1);
        this.op = op;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" = ");
        switch (op) {
            case "+":
                sb.append("add ");
                break;
            case "-":
                sb.append("sub ");
                break;
            case "*":
                sb.append("mul ");
                break;
            case "/":
                sb.append("sdiv ");
                break;
            case "%":
                sb.append("srem ");
                break;
        }
        if (operands.size() == 2) {
            sb.append(type).append(" ").append(operands.get(0).getName()).append(", ").append(operands.get(1).getName());
        } else {
            sb.append(type).append(" ").append("0 ").append(", ").append(operands.get(0).getName());
        }
        return sb.toString();
    }
}
