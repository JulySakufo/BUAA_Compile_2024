package frontend.Lexer;

public class Token {
    private String token;
    private TokenType tokenType;
    
    public Token(String token, TokenType tokenType) {
        this.token = token;
        this.tokenType = tokenType;
    }
    
    public String getToken() {
        return token;
    }
    
    public TokenType getTokenType() {
        return tokenType;
    }
}
