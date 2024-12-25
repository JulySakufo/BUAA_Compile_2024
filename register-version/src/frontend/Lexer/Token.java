package frontend.Lexer;

public class Token {
    private String token;
    private TokenType tokenType;
    private int lineNum;
    
    public Token(String token, TokenType tokenType, int lineNum) {
        this.token = token;
        this.tokenType = tokenType;
        this.lineNum = lineNum;
    }
    
    public String getToken() {
        return token;
    }
    
    public TokenType getTokenType() {
        return tokenType;
    }
    public int getLineNum(){
        return lineNum;
    }
}
