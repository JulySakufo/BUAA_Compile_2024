package middle.Value.Instruction;

import backend.Assembly.AluIAsm;
import backend.Assembly.LiAsm;
import backend.Assembly.SyscallAsm;
import backend.MipsGenerator;
import backend.Register;
import backend.RegisterController;
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
            if (!(funcRParams.get(0).getType() instanceof ArrayType)) {
                if (funcRParams.get(0) instanceof GetElementInstr) { //是arrayType,但是用的相对位移
                    sb.append(funcRParams.get(0).getType()).append("* ").append(funcRParams.get(0).getName());
                } else { //不是arrayType
                    sb.append(funcRParams.get(0).getType()).append(" ").append(funcRParams.get(0).getName());
                }
            } else { //是arrayType,但是call指令的参数不能有数组长度[5 x i32]，只能表示成i32*,所以要取getElementType
                sb.append(((ArrayType) funcRParams.get(0).getType()).getElementType()).append("* ").append(funcRParams.get(0).getName());
            }
            for (int i = 1; i < funcRParams.size(); i++) {
                if (!(funcRParams.get(i).getType() instanceof ArrayType)) { //不是arrayType
                    if (funcRParams.get(i) instanceof GetElementInstr) { //是arrayType,但是用的相对位移，所以这里的type是i32
                        sb.append(", ").append(funcRParams.get(i).getType()).append("* ").append(funcRParams.get(i).getName());
                    } else {
                        sb.append(", ").append(funcRParams.get(i).getType()).append(" ").append(funcRParams.get(i).getName());
                    }
                } else { //是arrayType,但是call指令的参数不能有数组长度[5 x i32]，只能表示成i32*,所以要取getElementType
                    sb.append(", ").append(((ArrayType) funcRParams.get(i).getType()).getElementType()).append("* ").append(funcRParams.get(i).getName());
                }
            }
        }
        sb.append(")");
        return sb.toString();
    }
    
    @Override
    public void generateMips() { //%reg = call i32 @getint()
        if (!(type instanceof VoidType)) { //有返回值的要接住返回值
            int allocOffset = -4;
            RegisterController.getRegisterController().addCurOffset(allocOffset);
            RegisterController.getRegisterController().addValue(name);
            AluIAsm addiuAsm = new AluIAsm("addiu", Register.SP, Register.SP, allocOffset);
            MipsGenerator.getMipsGenerator().addAsm(addiuAsm);
        }
        if (functionName.equals("getint")) { //这些都是系统调用
            LiAsm liAsm = new LiAsm(Register.V0, 5);
            MipsGenerator.getMipsGenerator().addAsm(liAsm);
            SyscallAsm syscallAsm = new SyscallAsm();
            MipsGenerator.getMipsGenerator().addAsm(syscallAsm);
            RegisterController.getRegisterController().storeToMemoryFromRegister(Register.V0, name);
        } else if (functionName.equals("getchar")) {
            LiAsm liAsm = new LiAsm(Register.V0, 12);
            MipsGenerator.getMipsGenerator().addAsm(liAsm);
            SyscallAsm syscallAsm = new SyscallAsm();
            MipsGenerator.getMipsGenerator().addAsm(syscallAsm);
            RegisterController.getRegisterController().storeToMemoryFromRegister(Register.V0, name);
        } else if (functionName.equals("putint")) { //只有一个参数
            RegisterController.getRegisterController().loadToRegisterFromMemory(funcRParams.get(0).getName(), Register.A0);
            LiAsm liAsm = new LiAsm(Register.V0, 1);
            MipsGenerator.getMipsGenerator().addAsm(liAsm);
            SyscallAsm syscallAsm = new SyscallAsm();
            MipsGenerator.getMipsGenerator().addAsm(syscallAsm);
        } else if (functionName.equals("putch")) { //只有一个参数
            RegisterController.getRegisterController().loadToRegisterFromMemory(funcRParams.get(0).getName(), Register.A0);
            LiAsm liAsm = new LiAsm(Register.V0, 11);
            MipsGenerator.getMipsGenerator().addAsm(liAsm);
            SyscallAsm syscallAsm = new SyscallAsm();
            MipsGenerator.getMipsGenerator().addAsm(syscallAsm);
        } else {
            /*TODO*/
            
        }
    }
}
