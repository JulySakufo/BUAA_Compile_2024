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
        AluIAsm addiuAsm = new AluIAsm(Register.SP, Register.SP, allocOffset, "addiu");
        MipsGenerator.getMipsGenerator().addAsm(addiuAsm);
        RegisterController.getRegisterController().addValue(name); //将%reg与内存对应
        if (flag == 0) { //%reg = getelementptr i32,i32* %1, i32 0,i32 index
            //把数组元素的地址放在reg里
            int initialOffset = RegisterController.getRegisterController().getValueOffset(lastName);
            int elementOffset = 4 * index;
            AluIAsm addiuAsm2 = new AluIAsm(Register.SP, Register.T0, initialOffset + elementOffset, "addiu");
            MipsGenerator.getMipsGenerator().addAsm(addiuAsm2); //将该位置保存在t0中
            int regOffset = RegisterController.getRegisterController().getValueOffset(name);
            MemAsm swAsm = new MemAsm(Register.SP, Register.T0, regOffset, "sw");
            MipsGenerator.getMipsGenerator().addAsm(swAsm); //将元素位置保存在%reg对应的内存里
        } else {
            int initialOffset = RegisterController.getRegisterController().getValueOffset(lastName);
            int operandOffset = RegisterController.getRegisterController().getValueOffset(operands.get(0).getName());
            MemAsm lwAsm = new MemAsm(Register.SP, Register.T0, operandOffset, "lw"); //将operand对应的值lw到t0
            MipsGenerator.getMipsGenerator().addAsm(lwAsm);
            AluIAsm sllAsm = new AluIAsm(Register.T0, Register.T0, 2, "sll"); //operand*4
            MipsGenerator.getMipsGenerator().addAsm(sllAsm); //与数组首元素的偏移保存在t0中
            AluIAsm addiuAsm2 = new AluIAsm(Register.SP, Register.T1, initialOffset, "addiu");
            MipsGenerator.getMipsGenerator().addAsm(addiuAsm2); //数组首元素的地址
            AluRAsm aluRAsm = new AluRAsm(Register.T2, Register.T0, Register.T1, "addu"); //相对于sp的偏移
            MipsGenerator.getMipsGenerator().addAsm(aluRAsm); //该元素所在的位置
            MemAsm swAsm = new MemAsm(Register.T2, Register.T2, 0, "sw");
            MipsGenerator.getMipsGenerator().addAsm(swAsm); //存的地址
        }
    }
}
