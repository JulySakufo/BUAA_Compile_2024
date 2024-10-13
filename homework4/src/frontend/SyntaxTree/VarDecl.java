package frontend.SyntaxTree;

import java.util.ArrayList;

public class VarDecl implements Decl {
    private String type;
    private ArrayList<VarDef> varDefs;
    
    public VarDecl() {
        this.varDefs = new ArrayList<>();
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public void addVarDef(VarDef varDef) {
        varDefs.add(varDef);
    }
}
