package middle.Value;

import middle.Type.Type;
import middle.Value.Instruction.Instr;
import middle.Value.Instruction.ReturnInstr;

import java.util.ArrayList;

public class BasicBlock extends Value {
    private boolean isFirstBlock;
    private ArrayList<Instr> instructions;
    
    public BasicBlock(Type type, String name) {
        super(type, name);
        this.instructions = new ArrayList<>();
        this.isFirstBlock = false;
    }
    
    public void addInstruction(Instr instr) {
        instructions.add(instr);
    }
    
    public boolean hasReturnInstr() { //看该块的最后一条语句是否是return(块里没有语句,有语句)
        if (!instructions.isEmpty()) { //instructions不为空，最后一条语句是不是return
            return instructions.get(instructions.size() - 1) instanceof ReturnInstr;
        } else { //instructions是空，绝对没有return
            return false;
        }
    }
    
    public void setFirstBlock(boolean isFirstBlock) { //是第一个块，不用输出编号
        this.isFirstBlock = isFirstBlock;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!isFirstBlock) { //不是第一个块，输出当前的编号,name就是块的编号
            sb.append(name.substring(1)).append(":").append("\n");
        }
        for (Instr instruction : instructions) {
            sb.append("    ").append(instruction).append("\n");
        }
        return sb.toString();
    }
}
