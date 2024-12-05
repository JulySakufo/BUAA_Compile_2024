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
        RegisterController.getRegisterController().dealAluRAsmRsRt(name, operands);
        int regOffset = RegisterController.getRegisterController().getValueOffset(name);
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
            AluRAsm adduAsm = new AluRAsm(asmOp,Register.T2,Register.T0,Register.T1);
            MipsGenerator.getMipsGenerator().addAsm(adduAsm);
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
            MulDivAsm mulDivAsm = new MulDivAsm(Register.T0, Register.T1, asmOp);
            MipsGenerator.getMipsGenerator().addAsm(mulDivAsm);
            if (op.equals("%")) {
                HiLoAsm hiLoAsm = new HiLoAsm(Register.T2, "mfhi");
                MipsGenerator.getMipsGenerator().addAsm(hiLoAsm);
            } else {
                HiLoAsm hiLoAsm = new HiLoAsm(Register.T2, "mflo");
                MipsGenerator.getMipsGenerator().addAsm(hiLoAsm);
            }
        }
        MemAsm swAsm = new MemAsm("sw", Register.T2, regOffset, Register.SP);//将binary的运算结果保存到栈中
        MipsGenerator.getMipsGenerator().addAsm(swAsm);
    }
}
