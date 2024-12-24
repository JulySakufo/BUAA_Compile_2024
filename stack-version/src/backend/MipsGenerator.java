package backend;

import backend.Assembly.Asm;
import backend.Assembly.Data;
import middle.Value.Module;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;

public class MipsGenerator {
    private static final MipsGenerator mipsGenerator = new MipsGenerator();
    private ArrayList<Data> dataSegment;
    private ArrayList<Asm> textSegment;
    
    public MipsGenerator() {
        this.dataSegment = new ArrayList<>();
        this.textSegment = new ArrayList<>();
    }
    
    public void generateMips(Module module) {
        module.generateMips();
        try (BufferedWriter stdout = new BufferedWriter(new FileWriter("mips.txt"))) {
            stdout.write(".data\n");
            for (Data data : dataSegment) {
                stdout.write(data.toString());
            }
            stdout.write(".text\n");
            stdout.write("\tjal main\n");
            stdout.write("\tli $v0, 10\n");
            stdout.write("\tsyscall\n");
            for (Asm asm : textSegment) {
                stdout.write(asm.toString() + "\n");
            }
        } catch (Exception ignored) {
        
        }
    }
    
    
    public static MipsGenerator getMipsGenerator() {
        return mipsGenerator;
    }
    
    
    public void addData(Data data) {
        dataSegment.add(data);
    }
    
    public void addAsm(Asm asm) {
        textSegment.add(asm);
    }
}
