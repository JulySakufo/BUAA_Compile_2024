package frontend.SyntaxTree;

import java.util.ArrayList;

public class ConstDecl implements Decl {
    private String type; //int | char
    private ArrayList<ConstDef> constDefs;
    
    public ConstDecl() {
        this.constDefs = new ArrayList<>();
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public void addConstDef(ConstDef constDef) {
        constDefs.add(constDef);
    }
}
