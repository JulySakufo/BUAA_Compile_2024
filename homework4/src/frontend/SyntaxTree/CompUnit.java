package frontend.SyntaxTree;

import java.util.ArrayList;

public class CompUnit {
    private ArrayList<Decl> decls;
    private ArrayList<FuncDef> funcDefs;
    private FuncDef mainFuncDef;
    
    public CompUnit() {
        this.decls = new ArrayList<>();
        this.funcDefs = new ArrayList<>();
    }
    
    public void addDecl(Decl decl) {
        decls.add(decl);
    }
    
    public void addFuncDefs(FuncDef funcDef) {
        funcDefs.add(funcDef);
    }
    
    public void setMainFuncDef(FuncDef mainFuncDef) {
        this.mainFuncDef = mainFuncDef;
    }
}
