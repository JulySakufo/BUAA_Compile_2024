package frontend.SyntaxTree;

import frontend.Lexer.Token;

public class ConstDef implements Def {
    private String ident;
    
    public ConstDef(String ident) {
        this.ident = ident;
    }
}
