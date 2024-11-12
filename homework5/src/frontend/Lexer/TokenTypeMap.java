package frontend.Lexer;

import java.util.HashMap;

public class TokenTypeMap {
    private static final TokenTypeMap instance = new TokenTypeMap();
    private HashMap<String, TokenType> tokenTypeMap = new HashMap<>();
    
    public TokenTypeMap() {
        tokenTypeMap.put("main", TokenType.MAINTK);
        tokenTypeMap.put("const", TokenType.CONSTTK);
        tokenTypeMap.put("int", TokenType.INTTK);
        tokenTypeMap.put("char", TokenType.CHARTK);
        tokenTypeMap.put("break", TokenType.BREAKTK);
        tokenTypeMap.put("continue", TokenType.CONTINUETK);
        tokenTypeMap.put("if", TokenType.IFTK);
        tokenTypeMap.put("else", TokenType.ELSETK);
        tokenTypeMap.put("!", TokenType.NOT);
        tokenTypeMap.put("&&", TokenType.AND);
        tokenTypeMap.put("||", TokenType.OR);
        tokenTypeMap.put("for", TokenType.FORTK);
        tokenTypeMap.put("getint", TokenType.GETINTTK);
        tokenTypeMap.put("getchar", TokenType.GETCHARTK);
        tokenTypeMap.put("printf", TokenType.PRINTFTK);
        tokenTypeMap.put("return", TokenType.RETURNTK);
        tokenTypeMap.put("+", TokenType.PLUS);
        tokenTypeMap.put("-", TokenType.MINU);
        tokenTypeMap.put("void", TokenType.VOIDTK);
        tokenTypeMap.put("*", TokenType.MULT);
        tokenTypeMap.put("/", TokenType.DIV);
        tokenTypeMap.put("%", TokenType.MOD);
        tokenTypeMap.put("<", TokenType.LSS);
        tokenTypeMap.put("<=", TokenType.LEQ);
        tokenTypeMap.put(">", TokenType.GRE);
        tokenTypeMap.put(">=", TokenType.GEQ);
        tokenTypeMap.put("==", TokenType.EQL);
        tokenTypeMap.put("!=", TokenType.NEQ);
        tokenTypeMap.put("=", TokenType.ASSIGN);
        tokenTypeMap.put(";", TokenType.SEMICN);
        tokenTypeMap.put(",", TokenType.COMMA);
        tokenTypeMap.put("(", TokenType.LPARENT);
        tokenTypeMap.put(")", TokenType.RPARENT);
        tokenTypeMap.put("[", TokenType.LBRACK);
        tokenTypeMap.put("]", TokenType.RBRACK);
        tokenTypeMap.put("{", TokenType.LBRACE);
        tokenTypeMap.put("}", TokenType.RBRACE);
    }
    
    public static TokenTypeMap getInstance() {
        return instance;
    }
    
    public boolean contains(String token) {
        return tokenTypeMap.containsKey(token);
    }
    
    public TokenType getTokenType(String token) {
        return tokenTypeMap.get(token);
    }
}
