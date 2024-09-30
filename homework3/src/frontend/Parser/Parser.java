package frontend.Parser;

import frontend.Lexer.Token;
import frontend.Lexer.TokenType;
import frontend.SyntaxTree.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;

public class Parser {
    private ArrayList<Token> tokenList;
    private boolean isError;
    private int pos;
    private ArrayList<String> infos;
    
    public Parser(ArrayList<Token> tokenList) {
        this.tokenList = tokenList;
        this.isError = false;
        this.pos = -1;
        this.infos = new ArrayList<>();
    }
    
    public void parseCompUnit() { //CompUnit → {Decl} {FuncDef} MainFuncDef
        CompUnit compUnit = new CompUnit();
        Token curToken = getNextToken(); //Decl → ConstDecl | VarDecl
        while (curToken.getTokenType() == TokenType.CONSTTK || curToken.getTokenType() == TokenType.INTTK || curToken.getTokenType() == TokenType.CHARTK) {
            compUnit.addDecl(parseDecl());
        }
    }
    
    public Decl parseDecl() {
        Token curToken = peekToken();
        addInfo(); //const | int | char
        if (curToken.getTokenType() == TokenType.CONSTTK) {
            ConstDecl constDecl = new ConstDecl();
            constDecl.setType(getNextToken().getToken());
            addInfo(); // int | char
            getNextToken(); //指向下一个
            constDecl.addConstDef(parseConstDef());
            while (peekToken().getTokenType() == TokenType.COMMA) {
                getNextToken();
                constDecl.addConstDef(parseConstDef());
            }
            if (peekToken().getTokenType() == TokenType.SEMICN) { //遇到;结束
                getNextToken();
                return constDecl;
            } else {
                dealError(peekToken().getLineNum() - 1, "i");
            }
            return constDecl;
        } else {
            VarDecl varDecl = new VarDecl();
            varDecl.setType(peekToken().getToken()); //int | char
            getNextToken(); // varDef
            varDecl.addVarDef(parseVarDef());
            while (peekToken().getTokenType() == TokenType.COMMA) {
                getNextToken();
                varDecl.addVarDef(parseVarDef());
            }
            if (peekToken().getTokenType() == TokenType.SEMICN) {
                getNextToken();
                return varDecl;
            } else {
                dealError(peekToken().getLineNum() - 1, "i");
            }
            return varDecl;
        }
    }
    
    public ConstDef parseConstDef() {
        addInfo();
        ConstDef constDef = new ConstDef(peekToken().getToken()); //标识符
        getNextToken();
        if (peekToken().getTokenType() == TokenType.LBRACK) {
            addInfo();
            getNextToken();
            /*TODO:语法树有点不会写了，先写与这次输出相关的吧，后面来补语法树*/
            
        }
        return null;
    }
    
    public VarDef parseVarDef() {
        return null;
    }
    
    public void parseConstExp() {
    
    }
    
    
    public Token getNextToken() {
        return tokenList.get(++pos);
    }
    
    public Token peekToken() { //保证最顶端的一定是还未分析过的token
        return tokenList.get(pos);
    }
    
    public void addInfo() {
        infos.add(peekToken().getTokenType().toString() + " " + peekToken().getToken());
    }
    
    public void dealError(int lineNum, String type) { //考虑到词法分析会先生成错误信息，在最后的错误信息输出时应先按行号排序
        isError = true;
        try (BufferedWriter stderr = new BufferedWriter(new FileWriter("D:\\BUAA_Compile_2024\\homework3\\src\\error.txt", true))) {
            stderr.write(lineNum + " " + type + "\n");
        } catch (Exception ignored) {
        
        }
    }
}
