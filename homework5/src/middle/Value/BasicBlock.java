package middle.Value;

import middle.Type.Type;
import middle.Value.Instruction.Instr;
import middle.Value.Instruction.ReturnInstr;

import java.util.ArrayList;

public class BasicBlock extends Value {
    private ArrayList<Instr> instructions;
    
    public BasicBlock(Type type, String name) {
        super(type, name);
        this.instructions = new ArrayList<>();
    }
    
    public void addInstruction(Instr instr) {
        instructions.add(instr);
    }
    
    public boolean hasReturnInstr() { //看该块的最后一条语句是否是return
        return instructions.get(instructions.size() - 1) instanceof ReturnInstr;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Instr instruction : instructions) {
            sb.append("    ").append(instruction).append("\n");
        }
        return sb.toString();
    }
}
