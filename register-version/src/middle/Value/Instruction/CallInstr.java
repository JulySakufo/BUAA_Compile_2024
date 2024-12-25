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
        if (functionName.equals("getint")) { //这些都是系统调用
            RegisterController.getRegisterController().distributeRegister(this); //有返回值的要接住返回值
            LiAsm liAsm = new LiAsm(Register.V0, 5);
            MipsGenerator.getMipsGenerator().addAsm(liAsm);
            SyscallAsm syscallAsm = new SyscallAsm();
            MipsGenerator.getMipsGenerator().addAsm(syscallAsm);
            Register register = RegisterController.getRegisterController().getRegister(name);
            if (register == null) {
                RegisterController.getRegisterController().storeToMemoryFromRegister(Register.V0, name);
            } else {
                MoveAsm moveAsm = new MoveAsm(register, Register.V0);
                MipsGenerator.getMipsGenerator().addAsm(moveAsm);
            }
        } else if (functionName.equals("getchar")) {
            RegisterController.getRegisterController().distributeRegister(this); //有返回值的要接住返回值
            LiAsm liAsm = new LiAsm(Register.V0, 12);
            MipsGenerator.getMipsGenerator().addAsm(liAsm);
            SyscallAsm syscallAsm = new SyscallAsm();
            MipsGenerator.getMipsGenerator().addAsm(syscallAsm);
            Register register = RegisterController.getRegisterController().getRegister(name);
            if (register == null) {
                RegisterController.getRegisterController().storeToMemoryFromRegister(Register.V0, name);
            } else {
                MoveAsm moveAsm = new MoveAsm(register, Register.V0);
                MipsGenerator.getMipsGenerator().addAsm(moveAsm);
            }
        } else if (functionName.equals("putint")) { //只有一个参数
            Register register = RegisterController.getRegisterController().getRegister(funcRParams.get(0).getName());
            if (register == null) {
                RegisterController.getRegisterController().loadToRegisterFromMemory(funcRParams.get(0).getName(), Register.A0);
            } else {
                MoveAsm moveAsm = new MoveAsm(Register.A0, register);
                MipsGenerator.getMipsGenerator().addAsm(moveAsm);
            }
            LiAsm liAsm = new LiAsm(Register.V0, 1);
            MipsGenerator.getMipsGenerator().addAsm(liAsm);
            SyscallAsm syscallAsm = new SyscallAsm();
            MipsGenerator.getMipsGenerator().addAsm(syscallAsm);
        } else if (functionName.equals("putch")) { //只有一个参数
            Register register = RegisterController.getRegisterController().getRegister(funcRParams.get(0).getName());
            if (register == null) {
                RegisterController.getRegisterController().loadToRegisterFromMemory(funcRParams.get(0).getName(), Register.A0);
            } else {
                MoveAsm moveAsm = new MoveAsm(Register.A0, register);
                MipsGenerator.getMipsGenerator().addAsm(moveAsm);
            }
            LiAsm liAsm = new LiAsm(Register.V0, 11);
            MipsGenerator.getMipsGenerator().addAsm(liAsm);
            SyscallAsm syscallAsm = new SyscallAsm();
            MipsGenerator.getMipsGenerator().addAsm(syscallAsm);
        } else { //先返回值，再ra，再寄存器
            if (!(type instanceof VoidType)) {
                RegisterController.getRegisterController().distributeRegister(this); //有返回值的要接住返回值
            }
            RegisterController.getRegisterController().addCurOffset(-4); //给ra挪位置，但无需绑定，因为会被覆盖
            int tempOffset = RegisterController.getRegisterController().getCurOffset(); //记录当前的移动量，不移动sp，sp唯一的移动就是调用函数时候切换
            MemAsm swAsm = new MemAsm("sw", Register.RA, tempOffset, Register.SP); //记录函数调用返回时调用者的ra
            MipsGenerator.getMipsGenerator().addAsm(swAsm);
            RegisterController.getRegisterController().storeUsedRegisters(); //将当前寄存器的值全压入栈中，现在这些寄存器可以被覆盖
            int totalOffset = RegisterController.getRegisterController().getCurOffset(); //当前真正的移动量
            int regOffset = RegisterController.getRegisterController().getUsedRegsSize() * 4;
            /*TODO 传递参数*/
            ArrayList<Register> args = Register.getFreeArgs();
            for (int i = 0; i < funcRParams.size(); i++) { //参数属于下一个函数，因此sp放在参数区即可
                Value funcRParam = funcRParams.get(i);
                if (args.isEmpty()) {
                    RegisterController.getRegisterController().passArguments(funcRParam.getName(), Register.K0);
                    MemAsm swAsm2 = new MemAsm("sw", Register.K0, totalOffset + -4 * (i-2), Register.SP); //将参数存进栈里
                    MipsGenerator.getMipsGenerator().addAsm(swAsm2); //将数保存在内存
                } else {
                    Register register = args.remove(0);
                    RegisterController.getRegisterController().passArguments(funcRParam.getName(), register);
                }
            }
            
            /*移动sp，唯一移动的地方*/
            AluIAsm addiuAsm = new AluIAsm("addiu", Register.SP, Register.SP, totalOffset);
            MipsGenerator.getMipsGenerator().addAsm(addiuAsm); //拥有了新的栈顶
            JAsm jalAsm = new JAsm("jal", new LabelAsm(functionName));
            MipsGenerator.getMipsGenerator().addAsm(jalAsm);
            AluIAsm addiuAsm2 = new AluIAsm("addiu", Register.SP, Register.SP, -totalOffset); //恢复栈顶位置
            MipsGenerator.getMipsGenerator().addAsm(addiuAsm2);
            /*TODO 好像感觉这里恢复的有点问题? 不太确定*/
            RegisterController.getRegisterController().restoreUsedRegisters(); //恢复寄存器的值
            if (!(type instanceof VoidType)) { //有返回值的要存储返回值
                Register register = RegisterController.getRegisterController().getRegister(name);
                if (register == null) { //将返回值保存在内存里
                    MemAsm swAsm2 = new MemAsm("sw", Register.V0, 4 + tempOffset, Register.SP);
                    MipsGenerator.getMipsGenerator().addAsm(swAsm2); //存储函数调用的返回值
                } else { //将返回值保存在寄存器里
                    MoveAsm moveAsm = new MoveAsm(register, Register.V0);
                    MipsGenerator.getMipsGenerator().addAsm(moveAsm);
                }
            }
            MemAsm lwAsm = new MemAsm("lw", Register.RA, tempOffset, Register.SP); //函数调用结束，恢复调用者的相关信息
            MipsGenerator.getMipsGenerator().addAsm(lwAsm);
            if (!(type instanceof VoidType)) { //有返回值的要考虑到新增了一个返回值需要用内存
                Register register = RegisterController.getRegisterController().getRegister(name);
                if (register == null) { //
                    RegisterController.getRegisterController().addCurOffset(4); //回到调用函数前的offset，调用函数额外花费了4个偏移，因为返回值是需要的
                }
            }
        }
    }
}
