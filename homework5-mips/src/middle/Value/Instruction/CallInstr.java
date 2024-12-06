package middle.Value.Instruction;

import backend.Assembly.*;
import backend.MipsGenerator;
import backend.Register;
import backend.RegisterController;
import middle.Type.ArrayType;
import middle.Type.Type;
import middle.Type.VoidType;
import middle.Value.Function;
import middle.Value.Value;

import java.lang.invoke.SwitchPoint;
import java.util.ArrayList;
import java.util.HashMap;

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
            /*int allocOffset = -4;
            RegisterController.getRegisterController().addCurOffset(allocOffset);
            RegisterController.getRegisterController().addValue(name);
            AluIAsm addiuAsm = new AluIAsm("addiu", Register.SP, Register.SP, allocOffset);
            MipsGenerator.getMipsGenerator().addAsm(addiuAsm);*/ //存取函数调用返回值
            
            RegisterController.getRegisterController().addCurOffset(-4);
            AluIAsm addiuAsm = new AluIAsm("addiu", Register.SP, Register.SP, -4);
            MipsGenerator.getMipsGenerator().addAsm(addiuAsm);
            MemAsm swAsm = new MemAsm("sw", Register.RA, 0, Register.SP); //记录函数调用返回时调用者的ra
            MipsGenerator.getMipsGenerator().addAsm(swAsm);
            
            for (int i = 0; i < funcRParams.size(); i++) { //参数属于下一个函数，因此sp放在参数区即可
                Value funcRParam = funcRParams.get(i);
                RegisterController.getRegisterController().passArguments(funcRParam.getName(), Register.T0);
                MemAsm swAsm2 = new MemAsm("sw", Register.T0, -4 * (i + 1), Register.SP); //将参数存进栈里
                MipsGenerator.getMipsGenerator().addAsm(swAsm2); //将数保存在内存
            }
            
            JAsm jalAsm = new JAsm("jal", new LabelAsm(functionName));
            MipsGenerator.getMipsGenerator().addAsm(jalAsm);
            
            int functionOffset = -RegisterController.getRegisterController().getFunctionOffset(functionName); //得到该操作对sp进行了多少的偏移，恢复取-号
            MemAsm lwAsm = new MemAsm("lw", Register.RA, functionOffset, Register.SP); //函数调用结束，恢复调用者的相关信息
            MipsGenerator.getMipsGenerator().addAsm(lwAsm);
            AluIAsm addiuAsm3 = new AluIAsm("addiu", Register.SP, Register.SP, functionOffset + 8); //恢复调用者的当前sp
            MipsGenerator.getMipsGenerator().addAsm(addiuAsm3);
            RegisterController.getRegisterController().addCurOffset(8); //回到调用函数前的offset，调用函数额外花费了8个偏移
        }
    }
}
