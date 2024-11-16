package middle.Value.Instruction;

import middle.Type.Type;

public class LoadInstr extends Instr {
    private String from;
    public LoadInstr(Type type, String from, String to) { //to是当前块的虚拟寄存器的编号
        super(type, to);
        this.from = from;
    }
    
    @Override
    public String toString() { //from是全局变量或局部变量的虚拟寄存器
        return name + " = load " + type + ", " + type + "*" + from;
    }
}
