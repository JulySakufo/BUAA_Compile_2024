package middle.Value.Instruction;

import middle.Type.Type;
import middle.Value.Value;

import java.util.ArrayList;

public class CallInstr extends Instr {
    private String functionName;
    private ArrayList<Value> funcRParams; //实参
    
    public CallInstr(Type type, String functionName, String virtualReg) {
        super(type, virtualReg);
        this.functionName = functionName;
        this.funcRParams = new ArrayList<>();
    }
    
    public CallInstr(Type type, String functionName, String virtualReg, ArrayList<Value> funcRParams) {
        super(type, virtualReg);
        this.functionName = functionName;
        this.funcRParams = funcRParams;
    }
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" = call ").append(type).append(" @").append(functionName).append("(");
        if (!funcRParams.isEmpty()) {
            sb.append(funcRParams.get(0).getType()).append(" ").append(funcRParams.get(0).getName());
            for (int i = 1; i < funcRParams.size(); i++) {
                sb.append(", ").append(funcRParams.get(i).getType()).append(" ").append(funcRParams.get(i).getName());
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
