package backend.Assembly;

public class LabelAsm extends Asm {
    private String name;
    
    public LabelAsm(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return name + ":";
    }
}
