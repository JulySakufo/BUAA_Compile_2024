package middle.Value.Instruction;

import middle.Type.ArrayType;
import middle.Type.Type;
import middle.Type.VoidType;
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
        if (type instanceof VoidType) { //无返回值的函数直接调用
            sb.append("call ").append(type).append(" @").append(functionName).append("(");
        } else { //有返回值的函数都要装起来
            sb.append(name).append(" = call ").append(type).append(" @").append(functionName).append("(");
        }
        if (!funcRParams.isEmpty()) {
            if (!(funcRParams.get(0).getType() instanceof ArrayType)) { //不是arrayType
                sb.append(funcRParams.get(0).getType()).append(" ").append(funcRParams.get(0).getName());
            } else { //是arrayType,但是call指令的参数不能有数组长度[5 x i32]，只能表示成i32*,所以要取getElementType
                sb.append(((ArrayType) funcRParams.get(0).getType()).getElementType()).append("* ").append(funcRParams.get(0).getName());
            }
            for (int i = 1; i < funcRParams.size(); i++) {
                if (!(funcRParams.get(i).getType() instanceof ArrayType)) { //不是arrayType
                    sb.append(", ").append(funcRParams.get(i).getType()).append(" ").append(funcRParams.get(i).getName());
                } else { //是arrayType,但是call指令的参数不能有数组长度[5 x i32]，只能表示成i32*,所以要取getElementType
                    sb.append(", ").append(((ArrayType) funcRParams.get(i).getType()).getElementType()).append("* ").append(funcRParams.get(i).getName());
                }
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
