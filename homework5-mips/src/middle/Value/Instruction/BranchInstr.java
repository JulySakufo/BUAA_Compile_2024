package middle.Value.Instruction;

import middle.Type.Type;
import middle.Value.BasicBlock;

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
}
