package backend.Assembly;

public class CommentAsm extends Asm {
    private String comment;
    
    public CommentAsm(String comment) {
        this.comment = comment;
    }
    
    @Override
    public String toString() {
        return "\t# " + comment; //在生成llvm对应的asm之前先打印这条llvm
    }
}
