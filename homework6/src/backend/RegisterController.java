package backend;

import backend.Assembly.*;
import middle.Type.ArrayType;
import middle.Type.Type;
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
    private ArrayList<Register> freeRegisters; //当前能用的寄存器
    private ArrayList<Register> usedRegisters; //当前被使用的寄存器
    private ArrayList<Register> freeArgs; //当前能用的参数寄存器
    private HashMap<String, HashMap<String, Register>> value2RegMap; // 虚拟寄存器或者全局变量 与 真实寄存器的对应关系
    
    public RegisterController() {
        this.spStack = new HashMap<>();
        this.contentIsAddress = new HashMap<>();
        this.curOffset = 0;
        this.curFunction = null;
        this.freeRegisters = Register.getFreeRegisters();
        this.usedRegisters = new ArrayList<>();
        this.freeArgs = Register.getFreeArgs();
        this.value2RegMap = new HashMap<>();
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
        HashMap<String, Register> hashMap2 = new HashMap<>();
        value2RegMap.put(curFunction.getName(), hashMap2);
        this.freeRegisters = Register.getFreeRegisters();
        this.usedRegisters = new ArrayList<>();
        this.freeArgs = Register.getFreeArgs();
        curOffset = 0;
    }
    
    public void leaveFunction() { //记录该函数用到的栈空间
        curFunction = null;
        curOffset = 0;
    }
    
    public void addCurOffset(int offset) {
        curOffset += offset;
    }
    
    
    public void addValue(String name) {
        spStack.get(curFunction.getName()).put(name, curOffset);
    }
    
    public void addContent(String name) {
        contentIsAddress.get(curFunction.getName()).add(name);
    }
    
    public boolean isContentAddress(String name) { //true代表装的是地址，false代表装的不是地址
        return contentIsAddress.get(curFunction.getName()).contains(name);
    }
    
    public int getValueOffset(String name) { //[spStack.get(name) - curOffset]($sp)即该value的值
        /*
         * 在不移动sp的情况下，该函数返回的就应该是距sp的偏移量，不能用相对偏移，那样会造成compile和run的不一致
         */
        return spStack.get(curFunction.getName()).get(name);
    }
    
    public Function getCurFunction() {
        return curFunction;
    }
    
    public int getCurOffset() {
        return curOffset;
    }
    
    public boolean isRegister(String name) {
        return name.charAt(0) == '%';
    }
    
    public boolean isGlobalVar(String name) {
        return name.charAt(0) == '@';
    }
    
    public void distributeRegister(Value value) {
        /*
         * 尝试给value对应的name分配一个真实寄存器
         * 建立一个freeReg的表 里面存着可以使用的寄存器
         * 来一个变量就用一个 当前没有能用的寄存器才放入内存
         * 调用函数的时候将freeReg重置表示当前所有都可以用
         * 如果不能分配寄存器就用内存来存
         *
         * 分配寄存器的步骤：
         * 从freeRegisters移除第一个寄存器，加入usedRegisters，设置value的register属性，建立value2RegMap的关系
         * 这样就实现了知道(1)哪些寄存器可用和不可用 (2)找对应的value直接去找register看有无，有,用register，无，用内存寻址
         */
        if (value.getType() instanceof ArrayType) { //数组不存在寄存器
            int allocOffset = -((ArrayType) value.getType()).getArrayLength() * 4;
            registerController.addCurOffset(allocOffset); //申请连续的内存空间
            registerController.addValue(value.getName()); //将%reg与内存建立关系，后面的getElement以此索引
        } else {
            if (value.getRegister() == null) {
                if (!freeRegisters.isEmpty()) { //数组不存在寄存器
                    Register register = freeRegisters.remove(0);
                    value.setRegister(register);
                    usedRegisters.add(register);
                    HashMap<String, Register> hashMap = value2RegMap.get(curFunction.getName());
                    hashMap.put(value.getName(), register); //建立value的name和真实寄存器的联系
                } else {
                    int allocOffset = -4;
                    registerController.addCurOffset(allocOffset); //申请一个空间
                    registerController.addValue(value.getName()); //保存在sp里，建立%reg与offset的联系
                }
            }
        }
    }
    
    public void distributeArg(Value value) {
        if (value.getRegister() == null) {
            if (!freeArgs.isEmpty()) {
                Register register = freeArgs.remove(0);
                value.setRegister(register);
                usedRegisters.add(register);
                HashMap<String, Register> hashMap = value2RegMap.get(curFunction.getName());
                hashMap.put(value.getName(), register);
            } else {
                int allocOffset = -4;
                registerController.addCurOffset(allocOffset);
                registerController.addValue(value.getName());
            }
        }
    }
    
    public Register getRegister(String name) { //返回寄存器
        if (hasRegister(name)) {
            return value2RegMap.get(curFunction.getName()).get(name);
        } else {
            return null;
        }
    }
    
    public boolean hasRegister(String name) { //检查value对应的name是否持有寄存器
        return value2RegMap.get(curFunction.getName()).containsKey(name);
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
                AluIAsm addiuAsm = new AluIAsm("addiu", Register.FP, Register.SP, offset);
                MipsGenerator.getMipsGenerator().addAsm(addiuAsm);
                MemAsm lwAsm = new MemAsm("lw", register, 0, Register.FP);
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
        /*
         * call指令的传参只有%reg和立即数两种情况，这样已经考虑了可能的数组情况
         */
        if (isRegister(name)) {
            Register nameRegister = getRegister(name);
            if (nameRegister == null) { //无，从内存加载
                int regOffset = registerController.getValueOffset(name);
                MemAsm lwAsm = new MemAsm("lw", register, regOffset, Register.SP);
                MipsGenerator.getMipsGenerator().addAsm(lwAsm);
            } else { //有，move操作
                MoveAsm moveAsm = new MoveAsm(register, nameRegister);
                MipsGenerator.getMipsGenerator().addAsm(moveAsm);
            }
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
         * FP寄存器用来活用，该函数结束后可以随时被覆盖
         */
        if (isRegister(name)) { //是%reg，局部变量
            if (isContentAddress(name)) { //对应的是地址
                int regOffset = registerController.getValueOffset(name);
                MemAsm lwAsm = new MemAsm("lw", Register.FP, regOffset, Register.SP);
                MipsGenerator.getMipsGenerator().addAsm(lwAsm);
                MemAsm lwAsm2 = new MemAsm("lw", register, 0, Register.FP);
                MipsGenerator.getMipsGenerator().addAsm(lwAsm2);
            } else {
                int regOffset = registerController.getValueOffset(name);
                MemAsm lwAsm = new MemAsm("lw", register, regOffset, Register.SP);
                MipsGenerator.getMipsGenerator().addAsm(lwAsm);
            }
        } else if (isGlobalVar(name)) { //是全局变量，用la从内存中加载出来
            LaAsm laAsm = new LaAsm(Register.FP, new LabelAsm(name.substring(1))); //去掉前面的@
            MipsGenerator.getMipsGenerator().addAsm(laAsm);
            MemAsm lwAsm = new MemAsm("lw", register, 0, Register.FP); //将值加载到指定寄存器中
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
                MemAsm lwAsm = new MemAsm("lw", Register.FP, regOffset, Register.SP);
                MipsGenerator.getMipsGenerator().addAsm(lwAsm);
                MemAsm swAsm = new MemAsm("sw", register, 0, Register.FP);
                MipsGenerator.getMipsGenerator().addAsm(swAsm);
            } else {
                int regOffset = registerController.getValueOffset(name);
                MemAsm swAsm = new MemAsm("sw", register, regOffset, Register.SP);
                MipsGenerator.getMipsGenerator().addAsm(swAsm);
            }
        } else if (isGlobalVar(name)) { //是全局变量
            LaAsm laAsm = new LaAsm(Register.FP, new LabelAsm(name.substring(1)));
            MipsGenerator.getMipsGenerator().addAsm(laAsm);
            MemAsm swAsm = new MemAsm("sw", register, 0, Register.FP);
            MipsGenerator.getMipsGenerator().addAsm(swAsm);
        } else {
            System.out.println("store may happen some error!");
        }
    }
    
    public void storeUsedRegisters() {
        /*
         * 函数调用前保存使用的寄存器到栈中
         */
        for (int i = 0; i < usedRegisters.size(); i++) {
            registerController.addCurOffset(-4); //向下移动
            Register register = usedRegisters.get(i);
            MemAsm swAsm = new MemAsm("sw", register, curOffset, Register.SP); //将寄存器的值保存进去
            MipsGenerator.getMipsGenerator().addAsm(swAsm);
        }
    }
    
    public void restoreUsedRegisters() {
        /*
         * 函数调用后从栈中恢复调用者的寄存器
         */
        for (int i = usedRegisters.size() - 1; i >= 0; i--) {
            Register register = usedRegisters.get(i);
            MemAsm lwAsm = new MemAsm("lw", register, curOffset, Register.SP);
            MipsGenerator.getMipsGenerator().addAsm(lwAsm);
            registerController.addCurOffset(4); //向上移动
        }
    }
    
    public int getUsedRegsSize() {
        return usedRegisters.size();
    }
}
