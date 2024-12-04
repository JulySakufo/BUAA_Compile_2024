package backend;

import backend.Assembly.AluIAsm;
import backend.Assembly.LiAsm;
import backend.Assembly.MemAsm;
import middle.Value.Value;

import java.util.ArrayList;
import java.util.HashMap;

public class RegisterController {
    private static final RegisterController registerController = new RegisterController();
    private HashMap<String, Integer> spStack; //value对应的在sp中的offset，可以通过offset($sp)去得到对应的值
    private int curOffset; //现在的sp的偏移量
    
    public RegisterController() {
        this.spStack = new HashMap<>();
        this.curOffset = 0;
    }
    
    public static RegisterController getRegisterController() {
        return registerController;
    }
    
    public int getCurOffset() {
        return curOffset;
    }
    
    public void addCurOffset(int offset) {
        curOffset += offset;
    }
    
    public HashMap<String, Integer> getSpStack() {
        return spStack;
    }
    
    public void addValue(String name) {
        spStack.put(name, curOffset);
    }
    
    public void addValue(String name, Integer offset) {
        spStack.put(name, curOffset + offset);
    }
    
    public int getValueOffset(String name) { //[spStack.get(name) - curOffset]($sp)即该value的值
        return spStack.get(name) - curOffset;
    }
    
    public boolean isRegister(String name) {
        return name.charAt(0) == '%';
    }
    
    public void dealLwAndLi(String name, Register register) {
        /*
         * 根据该name是%reg还是imm来进行对应的lw 或者 li操作
         * 均加载到给定的寄存器register
         */
        if (isRegister(name)) { //是%reg
            int regOffset = registerController.getValueOffset(name);
            MemAsm lwAsm = new MemAsm(Register.SP, register, regOffset, "lw");
            MipsGenerator.getMipsGenerator().addAsm(lwAsm);
        } else { //是imm
            LiAsm liAsm = new LiAsm(register, Integer.parseInt(name));
            MipsGenerator.getMipsGenerator().addAsm(liAsm);
        }
    }
    
    public void dealAluRAsmRsRt(String resultRegName, ArrayList<Value> operands) {
        /*
         * R型汇编指令rd = op rs rt的将rs,rt加载到T0,T1寄存器的操作
         * 这种指令最后都要把rd压入栈
         */
        int allocOffset = -4;
        registerController.addCurOffset(allocOffset);
        registerController.addValue(resultRegName);
        AluIAsm addiuAsm = new AluIAsm(Register.SP, Register.SP, allocOffset, "addiu");
        MipsGenerator.getMipsGenerator().addAsm(addiuAsm);
        //四元式两个操作数的处理部分 lw 和 li
        dealLwAndLi(operands.get(0).getName(), Register.T0);
        dealLwAndLi(operands.get(1).getName(), Register.T1);
    }
}
