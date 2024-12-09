package middle.Value.Instruction;

import backend.Assembly.*;
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
            Register baseRegister = RegisterController.getRegisterController().getRegister(lastName);
            if (baseRegister == null) {
                baseRegister = Register.K0;
                RegisterController.getRegisterController().getBaseAddressOfArray(lastName, baseRegister, false);
            }
            int elementOffset = 4 * index;
            AluIAsm addiuAsm2 = new AluIAsm("addiu", Register.K0, baseRegister, elementOffset); //此处地址也保存在K0处
            MipsGenerator.getMipsGenerator().addAsm(addiuAsm2); //将该位置保存在t0中
            Register nameRegister = RegisterController.getRegisterController().getRegister(name);
            if (nameRegister == null) { //无寄存器
                RegisterController.getRegisterController().storeToMemoryFromRegister(Register.K0, name); //将元素位置保存在%reg对应的内存里
                RegisterController.getRegisterController().addContent(name);
            } else { //有寄存器
                MoveAsm moveAsm = new MoveAsm(nameRegister, Register.K0); //nameRegister存放地址值
                MipsGenerator.getMipsGenerator().addAsm(moveAsm);
            }
        } else if (flag == 1) {
            if (!RegisterController.getRegisterController().isRegister(operands.get(0).getName())) { //不是寄存器
                Register baseRegister = RegisterController.getRegisterController().getRegister(lastName);
                int index = Integer.parseInt(operands.get(0).getName());
                if (baseRegister == null) {
                    baseRegister = Register.K0;
                    RegisterController.getRegisterController().getBaseAddressOfArray(lastName, baseRegister, false);
                }
                int elementOffset = 4 * index;
                AluIAsm addiuAsm2 = new AluIAsm("addiu", Register.K0, baseRegister, elementOffset); //此处地址也保存在K0处
                MipsGenerator.getMipsGenerator().addAsm(addiuAsm2); //将该位置保存在t0中
                Register nameRegister = RegisterController.getRegisterController().getRegister(name);
                if (nameRegister == null) { //无寄存器
                    RegisterController.getRegisterController().storeToMemoryFromRegister(Register.K0, name); //将元素位置保存在%reg对应的内存里
                    RegisterController.getRegisterController().addContent(name);
                } else { //有寄存器
                    MoveAsm moveAsm = new MoveAsm(nameRegister, Register.K0); //nameRegister存放地址值
                    MipsGenerator.getMipsGenerator().addAsm(moveAsm);
                }
            } else { //i32 0, i32 %reg
                Register baseRegister = RegisterController.getRegisterController().getRegister(lastName);
                if (baseRegister == null) {
                    baseRegister = Register.K0;
                    RegisterController.getRegisterController().getBaseAddressOfArray(lastName, baseRegister, false); //数组首元素的地址
                }
                Register operandRegister = RegisterController.getRegisterController().getRegister(operands.get(0).getName());
                if (operandRegister == null) {
                    operandRegister = Register.K1;
                    RegisterController.getRegisterController().loadToRegisterFromMemory(operands.get(0).getName(), operandRegister); //将operand对应的值lw到operandRegister
                }
                AluIAsm sllAsm = new AluIAsm("sll", Register.K1, operandRegister, 2); //operand*4保存在K1中
                MipsGenerator.getMipsGenerator().addAsm(sllAsm); //与数组首元素的偏移保存在K1中
                AluRAsm aluRAsm = new AluRAsm("addu", Register.K0, baseRegister, Register.K1);
                MipsGenerator.getMipsGenerator().addAsm(aluRAsm); //该元素所在的位置(绝对地址)保存在K0中
                Register nameRegister = RegisterController.getRegisterController().getRegister(name);
                if (nameRegister == null) {
                    RegisterController.getRegisterController().storeToMemoryFromRegister(Register.K0, name);
                    RegisterController.getRegisterController().addContent(name); //这个地址装的是地址
                } else {
                    MoveAsm moveAsm = new MoveAsm(nameRegister, Register.K0);
                    MipsGenerator.getMipsGenerator().addAsm(moveAsm);
                }
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
                Register baseRegister = RegisterController.getRegisterController().getRegister(lastName); //数组首地址保存在这个寄存器
                if (baseRegister == null) {
                    baseRegister = Register.K0;
                    RegisterController.getRegisterController().getBaseAddressOfArray(lastName, baseRegister, true); //将数组首地址加载到t0中
                }
                AluIAsm addiuAsm3 = new AluIAsm("addiu", Register.K0, baseRegister, elementOffset); //元素地址保存在K0中
                MipsGenerator.getMipsGenerator().addAsm(addiuAsm3);
                Register nameRegister = RegisterController.getRegisterController().getRegister(name);
                if (nameRegister == null) {
                    RegisterController.getRegisterController().storeToMemoryFromRegister(Register.K0, name);
                    RegisterController.getRegisterController().addContent(name);
                } else {
                    MoveAsm moveAsm = new MoveAsm(nameRegister, Register.K0);
                    MipsGenerator.getMipsGenerator().addAsm(moveAsm);
                }
            } else {
                /*TODO*/
                Register baseRegister = RegisterController.getRegisterController().getRegister(lastName);
                if (baseRegister == null) {
                    baseRegister = Register.K1;
                    RegisterController.getRegisterController().getBaseAddressOfArray(lastName, baseRegister, true);
                }
                Register operandRegister = RegisterController.getRegisterController().getRegister(operands.get(0).getName());
                if (operandRegister == null) {
                    operandRegister = Register.K0;
                    RegisterController.getRegisterController().loadToRegisterFromMemory(operands.get(0).getName(), baseRegister);
                }
                AluIAsm sllAsm = new AluIAsm("sll", Register.K0, operandRegister, 2);
                MipsGenerator.getMipsGenerator().addAsm(sllAsm);
                AluRAsm aluRAsm = new AluRAsm("addu", Register.K0, baseRegister, Register.K0); //位置保存在K0中
                MipsGenerator.getMipsGenerator().addAsm(aluRAsm);
                Register nameRegister = RegisterController.getRegisterController().getRegister(name);
                if (nameRegister == null) {
                    RegisterController.getRegisterController().storeToMemoryFromRegister(Register.K0, name);
                    RegisterController.getRegisterController().addContent(name);
                } else {
                    MoveAsm moveAsm = new MoveAsm(nameRegister, Register.K0);
                    MipsGenerator.getMipsGenerator().addAsm(moveAsm);
                }
            }
        }
    }
}
