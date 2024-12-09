package middle.Value.Instruction;

import backend.Assembly.*;
import backend.MipsGenerator;
import backend.Register;
import backend.RegisterController;
import middle.Type.Type;
import middle.Value.Value;

public class BinaryInstr extends Instr {
    private String op; //二目运算符
    
    public BinaryInstr(Type type, Value operand1, Value operand2, String op, String virtualReg) {
        super(type, virtualReg);
        operands.add(operand1);
        operands.add(operand2);
        this.op = op;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" = ");
        switch (op) {
            case "+":
                sb.append("add ");
                break;
            case "-":
                sb.append("sub ");
                break;
            case "*":
                sb.append("mul ");
                break;
            case "/":
                sb.append("sdiv ");
                break;
            case "%":
                sb.append("srem ");
                break;
        }
        sb.append(type).append(" ").append(operands.get(0).getName()).append(", ").append(operands.get(1).getName());
        return sb.toString();
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
        if (op.equals("+") || op.equals("-")) { // + -的运算
            switch (op) {
                case "+":
                    asmOp = "addu";
                    break;
                case "-":
                    asmOp = "subu";
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
        } else { // * / %的运算
            switch (op) {
                case "*":
                    asmOp = "mult";
                    break;
                case "/":
                case "%":
                    asmOp = "div";
                    break;
            }
            MulDivAsm mulDivAsm = new MulDivAsm(rs, rt, asmOp);
            MipsGenerator.getMipsGenerator().addAsm(mulDivAsm);
            if (op.equals("%")) {
                if (rd == null) { //无需存入内存
                    rd = Register.K1;
                    HiLoAsm hiLoAsm = new HiLoAsm(rd, "mfhi");
                    MipsGenerator.getMipsGenerator().addAsm(hiLoAsm);
                    RegisterController.getRegisterController().storeToMemoryFromRegister(rd, name); //将binary的运算结果保存到栈中
                } else {
                    HiLoAsm hiLoAsm = new HiLoAsm(rd, "mfhi");
                    MipsGenerator.getMipsGenerator().addAsm(hiLoAsm);
                }
            } else {
                if (rd == null) { //无需存入内存
                    rd = Register.K1;
                    HiLoAsm hiLoAsm = new HiLoAsm(rd, "mflo");
                    MipsGenerator.getMipsGenerator().addAsm(hiLoAsm);
                    RegisterController.getRegisterController().storeToMemoryFromRegister(rd, name);
                } else {
                    HiLoAsm hiLoAsm = new HiLoAsm(rd, "mflo");
                    MipsGenerator.getMipsGenerator().addAsm(hiLoAsm);
                }
            }
        }
    }
}
