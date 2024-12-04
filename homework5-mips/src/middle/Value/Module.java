package middle.Value;

import java.util.ArrayList;

public class Module extends Value {
    private ArrayList<GlobalVar> globalVars;
    private ArrayList<Function> functions;
    
    public Module() {
        super(null, "CompUnit");
        this.globalVars = new ArrayList<>();
        this.functions = new ArrayList<>();
    }
    
    public void addGlobalVar(GlobalVar globalVar) {
        globalVars.add(globalVar);
    }
    
    public void addFunction(Function function) {
        functions.add(function);
    }
    
    public ArrayList<GlobalVar> getGlobalVars() {
        return globalVars;
    }
    
    public ArrayList<Function> getFunctions() {
        return functions;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("declare i32 @getint()    \n" +
                "declare i32 @getchar()   \n" +
                "declare void @putint(i32)\n" +
                "declare void @putch(i32) \n" +
                "declare void @putstr(i8*)\n");
        for (GlobalVar globalVar : globalVars) {
            sb.append(globalVar.toString()).append("\n");
        }
        for (Function function : functions) {
            sb.append(function.toString()).append("\n");
        }
        return sb.toString();
    }
    
    @Override
    public void generateMips() {
        for (GlobalVar globalVar : globalVars) {
            globalVar.generateMips();
        }
        for (Function function : functions) {
            function.generateMips();
        }
    }
}
