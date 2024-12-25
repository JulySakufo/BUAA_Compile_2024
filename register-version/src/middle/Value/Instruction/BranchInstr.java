package middle.Value.Instruction;

import backend.Assembly.BranchAsm;
import backend.Assembly.JAsm;
import backend.Assembly.LabelAsm;
import backend.MipsGenerator;
import backend.Register;
import backend.RegisterController;
import middle.Type.Type;
import middle.Value.BasicBlock;
import middle.Value.Function;

public class BranchInstr extends Instr {
    public BranchInstr(Type type, String name, BasicBlock label1, BasicBlock label2) { //先真后假
        super(type, name);
        operands.add(label1);
        operands.add(label2);
    }
    
    public BranchInstr(BasicBlock label1) {
        super(null, null);
        operands.add(label1);
    }
    
    @Override
    public String toString() {
        if (operands.size() == 1) { //无条件跳转
            return "br label " + operands.get(0).getName();
        } else {
            return "br " + type + " " + name + ", label " + operands.get(0).getName() + ", label " + operands.get(1).getName();
        }
    }
    
    @Override
    public void generateMips() {
        if (operands.size() == 1) { //无条件跳转,j指令
            Function function = RegisterController.getRegisterController().getCurFunction();
            LabelAsm labelAsm = new LabelAsm(function.getName() + "_" + function.getIndexOfBlock((BasicBlock) (operands.get(0))));
            JAsm jAsm = new JAsm("j", labelAsm);
            MipsGenerator.getMipsGenerator().addAsm(jAsm);
        } else { //有条件跳转
            Function function = RegisterController.getRegisterController().getCurFunction();
            LabelAsm labelAsm1 = new LabelAsm(function.getName() + "_" + function.getIndexOfBlock((BasicBlock) (operands.get(0))));
            LabelAsm labelAsm2 = new LabelAsm(function.getName() + "_" + function.getIndexOfBlock((BasicBlock) (operands.get(1))));
            Register register = RegisterController.getRegisterController().getRegister(name);
            if (register == null) { //从内存中加载出值
                register = Register.K1;
                RegisterController.getRegisterController().loadToRegisterFromMemory(name, register);
            }
            BranchAsm bneAsm = new BranchAsm("bne", register, Register.ZERO, labelAsm1);
            MipsGenerator.getMipsGenerator().addAsm(bneAsm);
            BranchAsm beqAsm = new BranchAsm("beq", register, Register.ZERO, labelAsm2);
            MipsGenerator.getMipsGenerator().addAsm(beqAsm);
        }
    }
}
