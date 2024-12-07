package middle.Value.Instruction;

import backend.Assembly.AluIAsm;
import backend.Assembly.AluRAsm;
import backend.Assembly.LiAsm;
import backend.Assembly.MemAsm;
import backend.MipsGenerator;
import backend.Register;
import backend.RegisterController;
import middle.Type.ArrayType;
import middle.Type.Integer32Type;
import middle.Type.Type;
import middle.Value.Value;

import java.lang.invoke.SwitchPoint;

public class GetElementInstr extends Instr {
    private int index; //数组的索引 (/*TODO 改为value以统一toString结构和初始化 */)
    private String lastName; //数组的首地址
    private int flag;
    
    public GetElementInstr(Type type, String name, int index, String lastName) {
        super(type, name);
        this.index = index;
        this.lastName = lastName;
        this.flag = 0;
    }
    
    public GetElementInstr(Type type, String name, Value operand, String lastName) {
        super(type, name);
        operands.add(operand);
        this.lastName = lastName;
        this.flag = 1;
    }
    
    public GetElementInstr(Type type, String name, Value operand, String lastName, int flag) { //相对位移
        super(type, name);
        operands.add(operand);
        this.lastName = lastName;
        this.flag = 2;
    }
    
    @Override
    public String toString() {
        if (flag == 0) {
            return name + " = getelementptr " + type + ", " + type + "* " + lastName + ", i32 0, i32 " + index;
        } else if (flag == 1) {
            return name + " = getelementptr " + type + ", " + type + "* " + lastName + ", i32 0, i32 " + operands.get(0).getName();
        } else {
            return name + " = getelementptr " + type + ", " + type + "* " + lastName + ", i32 " + operands.get(0).getName();
        }
    }
    
    @Override
    public void generateMips() { //与alloca起的作用差不多
        int allocOffset = -4;
        RegisterController.getRegisterController().addCurOffset(allocOffset);
        RegisterController.getRegisterController().addValue(name); //将%reg与内存对应
        if (flag == 0) { //%reg = getelementptr i32,i32* %1, i32 0,i32 index
            //把数组元素的地址放在reg里
            RegisterController.getRegisterController().getBaseAddressOfArray(lastName, Register.T0, false);
            int elementOffset = 4 * index;
            AluIAsm addiuAsm2 = new AluIAsm("addiu", Register.T0, Register.T0, elementOffset);
            MipsGenerator.getMipsGenerator().addAsm(addiuAsm2); //将该位置保存在t0中
            RegisterController.getRegisterController().storeToMemoryFromRegister(Register.T0, name); //将元素位置保存在%reg对应的内存里
            RegisterController.getRegisterController().addContent(name);
        } else if (flag == 1) {
            if (!RegisterController.getRegisterController().isRegister(operands.get(0).getName())) { //不是寄存器
                RegisterController.getRegisterController().getBaseAddressOfArray(lastName, Register.T0, false);
                int index = Integer.parseInt(operands.get(0).getName());
                int elementOffset = 4 * index;
                AluIAsm addiuAsm2 = new AluIAsm("addiu", Register.T0, Register.T0, elementOffset);
                MipsGenerator.getMipsGenerator().addAsm(addiuAsm2); //将该位置保存在t0中
                RegisterController.getRegisterController().storeToMemoryFromRegister(Register.T0, name);
                RegisterController.getRegisterController().addContent(name);
            } else { //i32 0, i32 %reg
                RegisterController.getRegisterController().loadToRegisterFromMemory(operands.get(0).getName(), Register.T0);//将operand对应的值lw到t0
                AluIAsm sllAsm = new AluIAsm("sll", Register.T0, Register.T0, 2);//operand*4
                MipsGenerator.getMipsGenerator().addAsm(sllAsm); //与数组首元素的偏移保存在t0中
                RegisterController.getRegisterController().getBaseAddressOfArray(lastName, Register.T1, false); //数组首元素的地址
                AluRAsm aluRAsm = new AluRAsm("addu", Register.T2, Register.T0, Register.T1);
                MipsGenerator.getMipsGenerator().addAsm(aluRAsm); //该元素所在的位置(绝对地址)
                RegisterController.getRegisterController().storeToMemoryFromRegister(Register.T2, name);
                RegisterController.getRegisterController().addContent(name); //这个地址装的是地址
            }
        } else {
            /*
             * 因为相对位移的getelementptr指令使用的时候，数组并不是连续存储的，只有首地址在当前sp中，要找到元素在的位置进行存储
             * 换句话说得到正确的数组存储位置应该是lw当前这个地址的内容得到数组首地址
             * 然后再去加偏移
             */
            if (!RegisterController.getRegisterController().isRegister(operands.get(0).getName())) {
                int index = Integer.parseInt(operands.get(0).getName());
                int elementOffset = 4 * index;
                RegisterController.getRegisterController().getBaseAddressOfArray(lastName, Register.T0, true); //将数组首地址加载到t0中
                AluIAsm addiuAsm3 = new AluIAsm("addiu", Register.T0, Register.T0, elementOffset);
                MipsGenerator.getMipsGenerator().addAsm(addiuAsm3);
                RegisterController.getRegisterController().storeToMemoryFromRegister(Register.T0, name);
                RegisterController.getRegisterController().addContent(name);
            } else {
                RegisterController.getRegisterController().loadToRegisterFromMemory(operands.get(0).getName(), Register.T0);
                AluIAsm sllAsm = new AluIAsm("sll", Register.T0, Register.T0, 2);
                MipsGenerator.getMipsGenerator().addAsm(sllAsm);
                RegisterController.getRegisterController().getBaseAddressOfArray(lastName, Register.T1, true);
                AluRAsm aluRAsm = new AluRAsm("addu", Register.T2, Register.T0, Register.T1);
                MipsGenerator.getMipsGenerator().addAsm(aluRAsm);
                RegisterController.getRegisterController().storeToMemoryFromRegister(Register.T2, name);
                RegisterController.getRegisterController().addContent(name);
            }
        }
    }
}
