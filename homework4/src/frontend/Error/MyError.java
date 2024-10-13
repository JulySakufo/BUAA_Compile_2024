package frontend.Error;

public class MyError {
    private int lineNum;
    private String type;
    
    public MyError(int lineNum, String type) {
        this.lineNum = lineNum;
        this.type = type;
    }
    
    public int getLineNum() {
        return lineNum;
    }
    
    public String getType() {
        return type;
    }
}

