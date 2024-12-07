package backend;

import backend.Assembly.*;
import middle.Value.Function;
import middle.Value.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class RegisterController {
    private static final RegisterController registerController = new RegisterController();
    private HashMap<String, HashMap<String, Integer>> spStack; //value对应的在sp中的offset，可以通过offset($sp)去得到对应的值
    private HashMap<String, HashSet<String>> contentIsAddress; //判断该虚拟寄存器对应的内存地址里装的内容是值还是地址
    private int curOffset; //现在的sp的偏移量
    private Function curFunction; //当前是哪个函数
    private HashMap<String, Integer> offsetMap; //记录函数编译时候的偏移量，用于结束函数调用时调整sp的值
    
    public RegisterController() {
        this.spStack = new HashMap<>();
        this.contentIsAddress = new HashMap<>();
        this.curOffset = 0;
        this.curFunction = null;
        this.offsetMap = new HashMap<>();
    }
    
    public static RegisterController getRegisterController() {
        return registerController;
    }
    
    public void enterFunction(Function function) { //更新当前函数的情况，保证对于每一个函数进行操作的时候，都是针对的自己的栈顶sp
        this.curFunction = function;
        HashMap<String, Integer> hashMap = new HashMap<>();
        spStack.put(curFunction.getName(), hashMap);
        HashSet<String> hashSet = new HashSet<>();
        contentIsAddress.put(curFunction.getName(), hashSet);
        curOffset = 0;
    }
    
    public void leaveFunction() { //记录该函数用到的栈空间
        offsetMap.put(curFunction.getName(), curOffset);
        curFunction = null;
        curOffset = 0;
    }
    
    public void addCurOffset(int offset) {
        curOffset += offset;
    }
    
    
    public void addValue(String name) {
        spStack.get(curFunction.getName()).put(name, curOffset);
    }
    
    public void addValue(String name, Integer offset) {
        spStack.get(curFunction.getName()).put(name, curOffset + offset);
    }
    
    public void addContent(String name) {
        contentIsAddress.get(curFunction.getName()).add(name);
    }
    
    public boolean isContentAddress(String name) { //true代表装的是地址，false代表装的不是地址
        return contentIsAddress.get(curFunction.getName()).contains(name);
    }
    
    public int getValueOffset(String name) { //[spStack.get(name) - curOffset]($sp)即该value的值
        return spStack.get(curFunction.getName()).get(name) - curOffset;
    } /*TODO*/
    
    public Function getCurFunction() {
        return curFunction;
    }
    
    public int getFunctionOffset(String functionName) { //得到该函数对sp进行了多少的偏移操作
        return offsetMap.get(functionName);
    }
    
    public boolean isRegister(String name) {
        return name.charAt(0) == '%';
    }
    
    public boolean isGlobalVar(String name) {
        return name.charAt(0) == '@';
    }
    
    public void getBaseAddressOfArray(String name, Register register, boolean relative) {
        /*
         * 得到数组基地址
         */
        if (isRegister(name)) { //局部数组取得基地址的方式
            if (!relative) { //绝对寻址取得基地址的方式
                int offset = registerController.getValueOffset(name);
                AluIAsm addiuAsm = new AluIAsm("addiu", register, Register.SP, offset);
                MipsGenerator.getMipsGenerator().addAsm(addiuAsm);
            } else { //相对寻址取得基地址的方式
                int offset = registerController.getValueOffset(name);
                AluIAsm addiuAsm = new AluIAsm("addiu", Register.K0, Register.SP, offset);
                MipsGenerator.getMipsGenerator().addAsm(addiuAsm);
                MemAsm lwAsm = new MemAsm("lw", register, 0, Register.K0);
                MipsGenerator.getMipsGenerator().addAsm(lwAsm);
            }
        } else if (isGlobalVar(name)) { //全局数组取得基地址的方式
            LaAsm laAsm = new LaAsm(register, new LabelAsm(name.substring(1)));
            MipsGenerator.getMipsGenerator().addAsm(laAsm);
        } else {
            System.out.println("getBaseAddressOfArray may happen some error!");
        }
    }
    
    public void passArguments(String name, Register register) {
        if (isRegister(name)) {
            int regOffset = registerController.getValueOffset(name);
            MemAsm lwAsm = new MemAsm("lw", register, regOffset, Register.SP);
            MipsGenerator.getMipsGenerator().addAsm(lwAsm);
        } else if (isGlobalVar(name)) {
            /*TODO 我似乎没有考虑数组 */
            LaAsm laAsm = new LaAsm(Register.K0, new LabelAsm(name.substring(1)));
            MipsGenerator.getMipsGenerator().addAsm(laAsm);
            MemAsm lwAsm = new MemAsm("lw", register, 0, Register.K0);
            MipsGenerator.getMipsGenerator().addAsm(lwAsm);
        } else {
            LiAsm liAsm = new LiAsm(register, Integer.parseInt(name));
            MipsGenerator.getMipsGenerator().addAsm(liAsm);
        }
    }
    
    public void loadToRegisterFromMemory(String name, Register register) { //register是最终加载到值的寄存器
        /*
         * 该函数是栈式计算的核心函数
         * 根据该name是%reg还是imm来进行对应的lw 或者 li操作
         * 把name对应的内存的值加载到给定的寄存器register
         * K0,K1寄存器用来活用，该函数结束后可以随时被覆盖
         */
        if (isRegister(name)) { //是%reg，局部变量
            if (isContentAddress(name)) { //对应的是地址
                int regOffset = registerController.getValueOffset(name);
                MemAsm lwAsm = new MemAsm("lw", Register.K0, regOffset, Register.SP);
                MipsGenerator.getMipsGenerator().addAsm(lwAsm);
                MemAsm lwAsm2 = new MemAsm("lw", register, 0, Register.K0);
                MipsGenerator.getMipsGenerator().addAsm(lwAsm2);
            } else {
                int regOffset = registerController.getValueOffset(name);
                MemAsm lwAsm = new MemAsm("lw", register, regOffset, Register.SP);
                MipsGenerator.getMipsGenerator().addAsm(lwAsm);
            }
        } else if (isGlobalVar(name)) { //是全局变量，用la从内存中加载出来
            LaAsm laAsm = new LaAsm(Register.K0, new LabelAsm(name.substring(1))); //去掉前面的@
            MipsGenerator.getMipsGenerator().addAsm(laAsm);
            MemAsm lwAsm = new MemAsm("lw", register, 0, Register.K0); //将值加载到指定寄存器中
            MipsGenerator.getMipsGenerator().addAsm(lwAsm);
        } else { //是immediate，立即数
            LiAsm liAsm = new LiAsm(register, Integer.parseInt(name));
            MipsGenerator.getMipsGenerator().addAsm(liAsm);
        }
    }
    
    public void storeToMemoryFromRegister(Register register, String name) {
        /*
         * 该函数是栈式计算的核心函数，如此包装便可以不管操作的究竟是局部变量、全局变量、立即数了，只需要调用
         * 把register的值存入name对应的内存
         */
        if (isRegister(name)) {
            if (isContentAddress(name)) { //对应的是地址，要存到里面的地址
                int regOffset = registerController.getValueOffset(name);
                MemAsm lwAsm = new MemAsm("lw", Register.K0, regOffset, Register.SP);
                MipsGenerator.getMipsGenerator().addAsm(lwAsm);
                MemAsm swAsm = new MemAsm("sw", register, 0, Register.K0);
                MipsGenerator.getMipsGenerator().addAsm(swAsm);
            } else {
                int regOffset = registerController.getValueOffset(name);
                MemAsm swAsm = new MemAsm("sw", register, regOffset, Register.SP);
                MipsGenerator.getMipsGenerator().addAsm(swAsm);
            }
        } else if (isGlobalVar(name)) { //是全局变量
            LaAsm laAsm = new LaAsm(Register.K0, new LabelAsm(name.substring(1)));
            MipsGenerator.getMipsGenerator().addAsm(laAsm);
            MemAsm swAsm = new MemAsm("sw", register, 0, Register.K0);
            MipsGenerator.getMipsGenerator().addAsm(swAsm);
        } else {
            System.out.println("store may happen some error!");
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
        AluIAsm addiuAsm = new AluIAsm("addiu", Register.SP, Register.SP, allocOffset);
        MipsGenerator.getMipsGenerator().addAsm(addiuAsm);
        //四元式两个操作数的处理部分 lw 和 li
        loadToRegisterFromMemory(operands.get(0).getName(), Register.T0);
        loadToRegisterFromMemory(operands.get(1).getName(), Register.T1);
    }
}
