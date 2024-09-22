package frontend.Lexer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Lexer {
    private final BufferedReader stdin;
    private String line;
    private int lineNum;
    private int pos;
    private ArrayList<Token> tokenList;
    private boolean isError;
    private boolean isBlockComment;
    
    public Lexer(BufferedReader stdin) {
        this.stdin = stdin;
        this.lineNum = 0;
        this.pos = 0;
        this.tokenList = new ArrayList<>();
        this.isError = false;
        this.isBlockComment = false;
    }
    
    public void analyse() throws IOException {
        while ((line = stdin.readLine()) != null) {
            lineNum++;
            for (pos = 0; pos < line.length(); pos++) {
                if (isBlockComment) {
                    if (!line.contains("*/")) {
                        break;
                    } else {
                        pos = line.indexOf("*/") + 2;
                        isBlockComment = false;
                        if (pos >= line.length()) {
                            break;
                        }
                    }
                }
                if (isIdent(line.charAt(pos))) {
                    parseIdent();
                } else if (isDigit(line.charAt(pos))) {
                    parseNumber();
                } else if (line.charAt(pos) == '\"') {
                    parseString();
                } else if (line.charAt(pos) == '\'') {
                    parseChar();
                } else {
                    parseSign();
                }
            }
        }
        if (!isError) {
            try (BufferedWriter stdout = new BufferedWriter(new FileWriter("D:\\BUAA_Compile_2024\\homework2\\src\\lexer.txt"))) {
                for (Token token : tokenList) {
                    stdout.write(token.getTokenType() + " " + token.getToken() + "\n");
                }
            } catch (Exception ignored) {
            
            }
        }
    }
    
    public void parseIdent() { //用于分析保留字和标识符
        StringBuilder sb = new StringBuilder();
        sb.append(line.charAt(pos++));
        while (pos < line.length() && (isIdent(line.charAt(pos)) || isDigit(line.charAt(pos)))) {
            sb.append(line.charAt(pos++));
        }
        if (isReserve(sb.toString())) {
            TokenType tokenType = getTokenType(sb.toString());
            Token token = new Token(sb.toString(), tokenType);
            tokenList.add(token);
        } else {
            Token token = new Token(sb.toString(), TokenType.IDENFR);
            tokenList.add(token);
        }
        pos--;
    }
    
    public void parseNumber() {
        StringBuilder sb = new StringBuilder();
        sb.append(line.charAt(pos++));
        while (pos < line.length() && isDigit(line.charAt(pos))) {
            sb.append(line.charAt(pos++));
        }
        Token token = new Token(sb.toString(), TokenType.INTCON);
        tokenList.add(token);
        pos--;
    }
    
    public void parseString() {
        StringBuilder sb = new StringBuilder();
        sb.append(line.charAt(pos++));
        while (pos < line.length() && line.charAt(pos) != '\"') {
            if (line.charAt(pos) == '\\') {
                sb.append(line.charAt(pos++)); //考虑转义符的影响
            }
            sb.append(line.charAt(pos++));
        }
        sb.append(line.charAt(pos++));
        Token token = new Token(sb.toString(), TokenType.STRCON);
        tokenList.add(token);
        pos--;
    }
    
    public void parseChar() {
        StringBuilder sb = new StringBuilder();
        sb.append(line.charAt(pos++));
        while (pos < line.length() && line.charAt(pos) != '\'') {
            if (line.charAt(pos) == '\\') { //考虑转义符的影响 '\''
                sb.append(line.charAt(pos++)); //要将转义符写入
            }
            sb.append(line.charAt(pos++));
        }
        sb.append(line.charAt(pos++));
        Token token = new Token(sb.toString(), TokenType.CHRCON);
        tokenList.add(token);
        pos--;
    }
    
    public void parseSign() throws IOException {
        StringBuilder sb = new StringBuilder();
        switch (line.charAt(pos)) {
            case '&':
            case '|':
                char ch = line.charAt(pos++);
                sb.append(ch);
                if (line.charAt(pos) == ch) { // (&&) | (||)
                    sb.append(line.charAt(pos));
                    TokenType tokenType = getTokenType(sb.toString());
                    Token token = new Token(sb.toString(), tokenType);
                    tokenList.add(token); //停在最后一个&或者|，等待for循环将位置往前推
                } else { // (&) | (|)
                    pos--; //回退到&或者|身上
                    isError = true;
                    dealError();
                }
                break;
            case '!':
            case '<':
            case '>':
            case '=':
                char ch2 = line.charAt(pos++);
                sb.append(ch2);
                if (line.charAt(pos) == '=') { //!= || <= || >= || ==
                    sb.append(line.charAt(pos));
                    TokenType tokenType = getTokenType(sb.toString());
                    Token token = new Token(sb.toString(), tokenType);
                    tokenList.add(token);
                } else { //!
                    pos--; //回退到!或者<或者>或者=
                    TokenType tokenType = getTokenType(sb.toString());
                    Token token = new Token(sb.toString(), tokenType);
                    tokenList.add(token);
                }
                break;
            case '/':
                char ch3 = line.charAt(pos++);
                sb.append(ch3);
                if (line.charAt(pos) == '/') { //为单行注释，将pos直接置于行末
                    pos = line.length() - 1;
                } else if (line.charAt(pos) == '*') { //为多行注释，先将pos置于这一行的行末
                    if (!line.contains("*/")) { //多行注释未结束在本行
                        pos = line.length() - 1;
                        isBlockComment = true;
                    } else {
                        pos = line.indexOf("*/") + 1; //pos指在/
                    }
                } else {
                    pos--;
                    Token token = new Token(sb.toString(), TokenType.DIV);
                    tokenList.add(token);
                }
            default:
                sb.append(line.charAt(pos));
                TokenType tokenType = getTokenType(sb.toString());
                if (tokenType != null) {
                    Token token = new Token(sb.toString(), tokenType);
                    tokenList.add(token);
                }
        }
    }
    
    public boolean isIdent(char ch) { //标识符或者保留字
        return ('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || (ch == '_');
    }
    
    public boolean isDigit(char ch) { //无符号整数
        return ('0' <= ch && ch <= '9');
    }
    
    public boolean isReserve(String string) {
        return TokenTypeMap.getInstance().contains(string);
    }
    
    public TokenType getTokenType(String token) {
        return TokenTypeMap.getInstance().getTokenType(token);
    }
    
    public void dealError() throws IOException {
        try (BufferedWriter stderr = new BufferedWriter(new FileWriter("D:\\BUAA_Compile_2024\\homework2\\src\\error.txt"))) {
            stderr.write(lineNum + " " + "a\n"); //目前只有a类错误
        } catch (Exception ignored) {
        
        }
    }
}
