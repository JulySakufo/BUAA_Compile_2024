package middle.Value;

import middle.Type.Type;
import middle.Value.Instruction.Instr;

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
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Instr instruction : instructions) {
            sb.append("    ").append(instruction).append("\n");
        }
        return sb.toString();
    }
}
