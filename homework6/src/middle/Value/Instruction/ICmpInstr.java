package middle.Value.Instruction;

import backend.Assembly.AluIAsm;
import backend.Assembly.AluRAsm;
import backend.Assembly.MemAsm;
import backend.MipsGenerator;
import backend.Register;
import backend.RegisterController;
import middle.Type.BoolType;
import middle.Type.Type;
import middle.Value.Value;

public class ICmpInstr extends Instr {
    private String compareOp;
    private Type compareType;
    
    public ICmpInstr(Type compareType, String name, Value operand1, Value operand2, String compareOp) {
        super(new BoolType(), name);
        this.compareType = compareType;
        this.operands.add(operand1);
        this.operands.add(operand2);
        this.compareOp = compareOp;
    }
    
    @Override
    public String toString() {
        String instrOp = null;
        switch (compareOp) {
            case "==":
                instrOp = "eq";
                break;
            case "!=":
                instrOp = "ne";
                break;
            case "<":
                instrOp = "slt";
                break;
            case ">":
                instrOp = "sgt";
                break;
            case "<=":
                instrOp = "sle";
                break;
            case ">=":
                instrOp = "sge";
                break;
        }
        return name + " = icmp " + instrOp + " " + compareType + " " + operands.get(0).getName() + ", " + operands.get(1).getName();
    }
    
    @Override
    public void generateMips() {
        RegisterController.getRegisterController().distributeRegister(this);
        Register rs = RegisterController.getRegisterController().getRegister(operands.get(0).getName());
        Register rt = RegisterController.getRegisterController().getRegister(operands.get(1).getName());
        Register rd = RegisterController.getRegisterController().getRegister(name);
        if (rs == null) { //向rs，rt存入值
            rs = Register.K0;
            RegisterController.getRegisterController().loadToRegisterFromMemory(operands.get(0).getName(), rs);
        }
        if (rt == null) {
            rt = Register.K1;
            RegisterController.getRegisterController().loadToRegisterFromMemory(operands.get(1).getName(), rt);
        }
        String asmOp = null;
        switch (compareOp) {
            case "==":
                asmOp = "seq";
                break;
            case "!=":
                asmOp = "sne";
                break;
            case "<":
                asmOp = "slt";
                break;
            case ">":
                asmOp = "sgt";
                break;
            case "<=":
                asmOp = "sle";
                break;
            case ">=":
                asmOp = "sge";
                break;
        }
        if (rd == null) { //存入内存
            rd = Register.K1;
            AluRAsm aluRAsm = new AluRAsm(asmOp, rd, rs, rt);
            MipsGenerator.getMipsGenerator().addAsm(aluRAsm);
            RegisterController.getRegisterController().storeToMemoryFromRegister(rd, name);
        } else { //无需存入内存
            AluRAsm aluRAsm = new AluRAsm(asmOp, rd, rs, rt);
            MipsGenerator.getMipsGenerator().addAsm(aluRAsm);
        }
        
    }
}
