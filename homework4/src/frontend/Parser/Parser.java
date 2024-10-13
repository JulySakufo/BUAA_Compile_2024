package frontend.Parser;

import frontend.Error.MyError;
import frontend.Lexer.Token;
import frontend.Lexer.TokenType;
import frontend.SyntaxTree.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;

public class Parser {
    private ArrayList<Token> tokenList;
    private ArrayList<MyError> errorList;
    private boolean isError;
    private int pos;
    private ArrayList<String> infos;
    private SyntaxNode root; //语法树的根节点
    
    public Parser(ArrayList<Token> tokenList, ArrayList<MyError> errorList) {
        this.tokenList = tokenList;
        this.errorList = errorList;
        this.isError = false;
        this.pos = -1;
        this.infos = new ArrayList<>();
        this.root = new SyntaxNode("compUnit");
    }
    
    public void parseCompUnit() { //CompUnit → {Decl} {FuncDef} MainFuncDef
        getNextToken();
        while (peekToken().getTokenType() == TokenType.CONSTTK || peekToken().getTokenType() == TokenType.INTTK
                || peekToken().getTokenType() == TokenType.CHARTK) {
            if (peekToken().getTokenType() == TokenType.CONSTTK) { //一定是变量声明
                root.addChild(parseDecl());
            } else { //可能是变量声明，可能不是
                getNextToken(); //int a || int a() || int main()
                if (peekToken().getTokenType() == TokenType.IDENFR) { //int a || int a() var|func
                    getNextToken();
                    if (peekToken().getTokenType() != TokenType.LPARENT) { // var
                        pos = pos - 2; //回退到int | char上
                        root.addChild(parseDecl());
                    } else { //func,准备进行func识别
                        pos = pos - 2;
                        break;
                    }
                } else { //mainFuncDef
                    pos = pos - 1;
                    break;
                }
            }
        }
        while (peekToken().getTokenType() == TokenType.INTTK || peekToken().getTokenType() == TokenType.VOIDTK
                || peekToken().getTokenType() == TokenType.CHARTK) { // {funcDef}
            getNextToken();
            if (peekToken().getTokenType() == TokenType.IDENFR) { //不是mainFuncDef
                pos = pos - 1;
                root.addChild(parseFuncDef());
            } else { //int main
                pos = pos - 1;
                break;
            }
        }
        root.addChild(parseMainFuncDef());
        infos.add("<CompUnit>");
        if (!isError) {
            try (BufferedWriter stdout = new BufferedWriter(new FileWriter("D:\\BUAA_Compile_2024\\homework4\\src\\parser.txt"))) {
                for (String info : infos) {
                    stdout.write(info + "\n");
                }
            } catch (Exception ignored) {
            
            }
        } else {
            errorList.sort(Comparator.comparingInt(MyError::getLineNum));
            try (BufferedWriter stderr = new BufferedWriter(new FileWriter("D:\\BUAA_Compile_2024\\homework4\\src\\error.txt", true))) {
                for (MyError error : errorList) {
                    stderr.write(error.getLineNum() + " " + error.getType() + "\n");
                }
            } catch (Exception ignored) {
            
            }
        }
    }
    
    public SyntaxNode parseDecl() {
        SyntaxNode node = new SyntaxNode("Decl");
        Token curToken = peekToken();
        addInfo(); //const | int | char
        if (curToken.getTokenType() == TokenType.CONSTTK) {
            SyntaxNode child = new SyntaxNode("ConstDecl");
            node.addChild(child);
            child.addChild(new SyntaxNode("const"));
            getNextToken();
            addInfo(); // int | char
            child.addChild(new SyntaxNode(peekToken().getToken())); // int | char
            getNextToken(); //指向下一个
            child.addChild(parseConstDef());
            while (peekToken().getTokenType() == TokenType.COMMA) {
                addInfo();
                child.addChild(new SyntaxNode(","));
                getNextToken();
                child.addChild(parseConstDef());
            }
            if (peekToken().getTokenType() == TokenType.SEMICN) { //遇到;结束
                addInfo();
                child.addChild(new SyntaxNode(";"));
                getNextToken();
            } else { //没遇到分号
                dealError(getMinErrorLineNum(), "i");
            }
            infos.add("<ConstDecl>");
            return node;
        } else {
            SyntaxNode child = new SyntaxNode("VarDecl");
            node.addChild(child);
            child.addChild(new SyntaxNode(peekToken().getToken())); //int | char
            getNextToken(); // varDef
            child.addChild(parseVarDef());
            while (peekToken().getTokenType() == TokenType.COMMA) {
                addInfo();
                child.addChild(new SyntaxNode(","));
                getNextToken();
                child.addChild(parseVarDef());
            }
            if (peekToken().getTokenType() == TokenType.SEMICN) {
                addInfo();
                child.addChild(new SyntaxNode(";"));
                getNextToken();
            } else {
                dealError(getMinErrorLineNum(), "i");
            }
            infos.add("<VarDecl>");
            return node;
        }
    }
    
    public SyntaxNode parseConstDef() {
        SyntaxNode node = new SyntaxNode("ConstDef");
        node.addChild(new SyntaxNode(peekToken().getToken()));
        addInfo(); //标识符
        getNextToken();
        if (peekToken().getTokenType() == TokenType.LBRACK) {
            addInfo();
            node.addChild(new SyntaxNode("["));
            getNextToken();
            node.addChild(parseConstExp());
            if (peekToken().getTokenType() == TokenType.RBRACK) {
                addInfo();
                node.addChild(new SyntaxNode("]"));
                getNextToken();
            } else {
                dealError(peekToken().getLineNum(), "k");
            }
        }
        addInfo(); // =
        node.addChild(new SyntaxNode("="));
        getNextToken();
        node.addChild(parseConstInitVal());
        infos.add("<ConstDef>");
        return node;
    }
    
    public SyntaxNode parseVarDef() {
        SyntaxNode node = new SyntaxNode("VarDef");
        node.addChild(new SyntaxNode(peekToken().getToken()));
        addInfo(); //ident
        getNextToken();
        if (peekToken().getTokenType() == TokenType.LBRACK) {
            node.addChild(new SyntaxNode("["));
            addInfo();
            getNextToken();
            node.addChild(parseConstExp());
            if (peekToken().getTokenType() == TokenType.RBRACK) {
                node.addChild(new SyntaxNode("]"));
                addInfo();
                getNextToken();
            } else {
                dealError(peekToken().getLineNum(), "k");
            }
        }
        if (peekToken().getTokenType() == TokenType.ASSIGN) {
            node.addChild(new SyntaxNode("="));
            addInfo();
            getNextToken();
            node.addChild(parseInitVal());
        }
        infos.add("<VarDef>");
        return node;
    }
    
    public SyntaxNode parseConstInitVal() {
        SyntaxNode node = new SyntaxNode("ConstInitVal");
        if (peekToken().getTokenType() == TokenType.LBRACE) {
            node.addChild(new SyntaxNode("{"));
            addInfo();
            getNextToken();
            if (peekToken().getTokenType() != TokenType.RBRACE) { //{1,2}不是{}的情况
                node.addChild(parseConstExp());
                while (peekToken().getTokenType() == TokenType.COMMA) {
                    node.addChild(new SyntaxNode(","));
                    addInfo();
                    getNextToken();
                    node.addChild(parseConstExp());
                }
            } //考虑constInitVal的集合情况为空
            node.addChild(new SyntaxNode(peekToken().getToken()));
            addInfo(); // }
            getNextToken();
        } else if (peekToken().getTokenType() == TokenType.STRCON) {
            /*TODO:未添加节点的，不知道怎么写了，待完成*/
            addInfo();
            getNextToken();
        } else { //ConstExp
            node.addChild(parseConstExp());
        }
        infos.add("<ConstInitVal>");
        return node;
    }
    
    public SyntaxNode parseInitVal() {
        SyntaxNode node = new SyntaxNode("InitVal");
        if (peekToken().getTokenType() == TokenType.LBRACE) {
            node.addChild(new SyntaxNode("{"));
            addInfo();
            getNextToken();
            if (peekToken().getTokenType() != TokenType.RBRACE) {
                node.addChild(parseExp());
                while (peekToken().getTokenType() == TokenType.COMMA) {
                    node.addChild(new SyntaxNode(","));
                    addInfo();
                    getNextToken();
                    node.addChild(parseExp());
                }
            } //考虑int a[2] = {};的情况
            node.addChild(new SyntaxNode("}"));
            addInfo(); // }
            getNextToken();
        } else if (peekToken().getTokenType() == TokenType.STRCON) {
            /*TODO:未添加节点的，不知道怎么写了，待完成*/
            addInfo();
            getNextToken();
        } else { //ConstExp
            node.addChild(parseExp());
        }
        infos.add("<InitVal>");
        return node;
    }
    
    public SyntaxNode parseFuncDef() {
        SyntaxNode node = new SyntaxNode("FuncDef");
        SyntaxNode child = new SyntaxNode("FuncType");
        node.addChild(child); //funcdef-functype
        child.addChild(new SyntaxNode(peekToken().getToken())); //functype-int|char
        addInfo();
        infos.add("<FuncType>");
        getNextToken(); //ident
        node.addChild(new SyntaxNode(peekToken().getToken()));
        addInfo();
        getNextToken(); //(
        node.addChild(new SyntaxNode("("));
        addInfo();
        getNextToken();
        if (peekToken().getTokenType() == TokenType.RPARENT) {
            addInfo();
            node.addChild(new SyntaxNode(")"));
            getNextToken();
            node.addChild(parseBlock());
        } else {
            if (peekToken().getTokenType() == TokenType.INTTK || peekToken().getTokenType() == TokenType.CHARTK) { //FuncFParams
                node.addChild(parseFuncFParams());
                if (peekToken().getTokenType() == TokenType.RPARENT) {
                    node.addChild(new SyntaxNode(")"));
                    addInfo();
                    getNextToken();
                } else {
                    dealError(peekToken().getLineNum(), "j");
                }
            }
            node.addChild(parseBlock());
        }
        infos.add("<FuncDef>");
        return node;
    }
    
    public SyntaxNode parseMainFuncDef() { // 'int' 'main' '(' ')' Block // j
        SyntaxNode node = new SyntaxNode("MainFuncDef");
        node.addChild(new SyntaxNode("int"));
        addInfo(); // int
        getNextToken(); //main
        node.addChild(new SyntaxNode("main"));
        addInfo();
        getNextToken(); // (
        node.addChild(new SyntaxNode("("));
        addInfo();
        getNextToken();
        if (peekToken().getTokenType() == TokenType.RPARENT) {
            node.addChild(new SyntaxNode(")"));
            addInfo();
            getNextToken(); //BLOCK
        } else {
            dealError(peekToken().getLineNum(), "j");
        }
        node.addChild(parseBlock());
        infos.add("<MainFuncDef>");
        return node;
    }
    
    public SyntaxNode parseBlock() {
        SyntaxNode node = new SyntaxNode("Block");
        node.addChild(new SyntaxNode("{"));
        addInfo(); //{
        getNextToken();
        while (peekToken().getTokenType() != TokenType.RBRACE) {
            node.addChild(parseBlockItem());
        }
        node.addChild(new SyntaxNode("}"));
        addInfo();
        getNextToken();
        infos.add("<Block>");
        return node;
    }
    
    public SyntaxNode parseBlockItem() {
        SyntaxNode node = new SyntaxNode("BlockItem");
        if (peekToken().getTokenType() == TokenType.CONSTTK || peekToken().getTokenType() == TokenType.INTTK || peekToken().getTokenType() == TokenType.CHARTK) {
            node.addChild(parseDecl());
        } else {
            node.addChild(parseStmt());
        }
        return node;
    }
    
    public SyntaxNode parseFuncFParams() { // FuncFParam{,FuncFParam}
        SyntaxNode node = new SyntaxNode("FuncFParams");
        node.addChild(parseFuncFParam());
        while (peekToken().getTokenType() == TokenType.COMMA) {
            node.addChild(new SyntaxNode(","));
            addInfo();
            getNextToken();
            node.addChild(parseFuncFParam());
        }
        infos.add("<FuncFParams>");
        return node;
    }
    
    public SyntaxNode parseFuncFParam() {
        SyntaxNode node = new SyntaxNode("FuncFParam");
        node.addChild(new SyntaxNode(peekToken().getToken()));
        addInfo();
        getNextToken(); //ident
        node.addChild(new SyntaxNode(peekToken().getToken()));
        addInfo();
        getNextToken(); //[或者nothing
        if (peekToken().getTokenType() == TokenType.LBRACK) {
            node.addChild(new SyntaxNode("["));
            addInfo();
            getNextToken();
            if (peekToken().getTokenType() == TokenType.RBRACK) {
                node.addChild(new SyntaxNode("]"));
                addInfo();
                getNextToken();
            } else {
                dealError(peekToken().getLineNum(), "k");
            }
        } //始终指向下一个未分析的token
        infos.add("<FuncFParam>");
        return node;
    }
    
    public SyntaxNode parseConstExp() {
        SyntaxNode node = new SyntaxNode("ConstExp");
        node.addChild(parseAddExp());
        infos.add("<ConstExp>");
        return node;
    }
    
    public SyntaxNode parseAddExp() { //MulExp{+-MulExp}
        SyntaxNode node = new SyntaxNode("AddExp");
        node.addChild(parseMulExp());
        infos.add("<AddExp>");
        while (peekToken().getTokenType() == TokenType.PLUS || peekToken().getTokenType() == TokenType.MINU) {
            node.addChild(new SyntaxNode(peekToken().getToken()));
            addInfo();
            getNextToken();
            node.addChild(parseMulExp());
            infos.add("<AddExp>");
        }
        return node;
    }
    
    public SyntaxNode parseMulExp() { //UnaryExp{*/%UnaryExp}
        SyntaxNode node = new SyntaxNode("MulExp");
        node.addChild(parseUnaryExp());
        infos.add("<MulExp>");
        while (peekToken().getTokenType() == TokenType.MULT || peekToken().getTokenType() == TokenType.DIV || peekToken().getTokenType() == TokenType.MOD) {
            node.addChild(new SyntaxNode(peekToken().getToken()));
            addInfo();
            getNextToken();
            node.addChild(parseUnaryExp());
            infos.add("<MulExp>");
        }
        return node;
    }
    
    public SyntaxNode parseUnaryExp() {
        SyntaxNode node = new SyntaxNode("UnaryExp");
        if (peekToken().getTokenType() == TokenType.PLUS || peekToken().getTokenType() == TokenType.MINU || peekToken().getTokenType() == TokenType.NOT) {
            SyntaxNode child = new SyntaxNode("UnaryOp");
            node.addChild(child); //UnaryExp-UnaryOp
            child.addChild(new SyntaxNode(peekToken().getToken())); //UnaryOp-(+|-)
            addInfo();
            infos.add("<UnaryOp>");
            getNextToken();
            node.addChild(parseUnaryExp());
        } else if (peekToken().getTokenType() == TokenType.IDENFR) { //ident ([FuncParams])
            node.addChild(new SyntaxNode(peekToken().getToken()));
            addInfo();
            getNextToken(); //判断是否是(
            if (peekToken().getTokenType() == TokenType.LPARENT) {
                node.addChild(new SyntaxNode("("));
                addInfo();
                getNextToken();
                if (peekToken().getTokenType() == TokenType.RPARENT) {
                    node.addChild(new SyntaxNode(")"));
                    addInfo();
                    getNextToken();
                } else {
                    if (peekToken().getTokenType() == TokenType.INTCON || peekToken().getTokenType() == TokenType.CHRCON
                            || peekToken().getTokenType() == TokenType.IDENFR || peekToken().getTokenType() == TokenType.LPARENT
                            || peekToken().getTokenType() == TokenType.PLUS || peekToken().getTokenType() == TokenType.MINU || peekToken().getTokenType() == TokenType.NOT) {
                        node.addChild(parseFuncRParams()); //实参的第一个字符可能的情况
                    }
                    if (peekToken().getTokenType() == TokenType.RPARENT) { // )
                        node.addChild(new SyntaxNode(")"));
                        addInfo();
                        getNextToken();
                    } else { //没有右括号
                        dealError(getMinErrorLineNum(), "j");
                    }
                }
            } else { //是primaryExp的ident
                pos--; //指到ident
                node.removeChild();
                infos.remove(infos.size() - 1); //将之前的info移除，到primaryExp再添加
                node.addChild(parsePrimaryExp());
            }
        } else { //primaryExp
            node.addChild(parsePrimaryExp());
        }
        infos.add("<UnaryExp>");
        return node;
    }
    
    public SyntaxNode parsePrimaryExp() {
        SyntaxNode node = new SyntaxNode("PrimaryExp");
        if (peekToken().getTokenType() == TokenType.LPARENT) { //(Exp)
            node.addChild(new SyntaxNode("("));
            addInfo();
            getNextToken();
            node.addChild(parseExp());
            if (peekToken().getTokenType() == TokenType.RPARENT) {
                node.addChild(new SyntaxNode(")"));
                addInfo();
                getNextToken();
            } else {
                dealError(peekToken().getLineNum(), "j");
            }
        } else if (peekToken().getTokenType() == TokenType.INTCON) {
            SyntaxNode child = new SyntaxNode("Number");
            node.addChild(child);
            child.addChild(new SyntaxNode(peekToken().getToken()));
            addInfo();
            getNextToken();
            infos.add("<Number>");
        } else if (peekToken().getTokenType() == TokenType.CHRCON) {
            SyntaxNode child = new SyntaxNode("Character");
            node.addChild(child);
            child.addChild(new SyntaxNode(peekToken().getToken()));
            addInfo();
            getNextToken();
            infos.add("<Character>");
        } else { // LVal
            node.addChild(parseLVal());
        }
        infos.add("<PrimaryExp>");
        return node;
    }
    
    public SyntaxNode parseLVal() { // ident['['Exp']']
        SyntaxNode node = new SyntaxNode("LVal");
        node.addChild(new SyntaxNode(peekToken().getToken()));
        addInfo(); //ident
        getNextToken();
        if (peekToken().getTokenType() == TokenType.LBRACK) {
            node.addChild(new SyntaxNode("["));
            addInfo();
            getNextToken();
            node.addChild(parseExp());
            if (peekToken().getTokenType() == TokenType.RBRACK) {
                node.addChild(new SyntaxNode("]"));
                addInfo();
                getNextToken();
            } else {
                dealError(peekToken().getLineNum(), "k");
            }
        }
        infos.add("<LVal>");
        return node;
    }
    
    public SyntaxNode parseFuncRParams() { //Exp{,Exp}
        SyntaxNode node = new SyntaxNode("FuncRParams");
        node.addChild(parseExp());
        while (peekToken().getTokenType() == TokenType.COMMA) {
            node.addChild(new SyntaxNode(","));
            addInfo();
            getNextToken();
            node.addChild(parseExp());
        }
        infos.add("<FuncRParams>");
        return node;
    }
    
    public SyntaxNode parseExp() {
        SyntaxNode node = new SyntaxNode("Exp");
        node.addChild(parseAddExp());
        infos.add("<Exp>");
        return node;
    }
    
    public SyntaxNode parseRelExp() { //AddExp{<><=>=AddExp}
        SyntaxNode node = new SyntaxNode("RelExp");
        node.addChild(parseAddExp());
        infos.add("<RelExp>");
        while (peekToken().getTokenType() == TokenType.LSS || peekToken().getTokenType() == TokenType.GRE
                || peekToken().getTokenType() == TokenType.LEQ || peekToken().getTokenType() == TokenType.GEQ) {
            node.addChild(new SyntaxNode(peekToken().getToken()));
            addInfo();
            getNextToken();
            node.addChild(parseAddExp());
            infos.add("<RelExp>");
        }
        return node;
    }
    
    public SyntaxNode parseEqExp() { //RelExp{==!=RelExp}
        SyntaxNode node = new SyntaxNode("EqExp");
        node.addChild(parseRelExp());
        infos.add("<EqExp>");
        while (peekToken().getTokenType() == TokenType.EQL || peekToken().getTokenType() == TokenType.NEQ) {
            node.addChild(new SyntaxNode(peekToken().getToken()));
            addInfo();
            getNextToken();
            node.addChild(parseRelExp());
            infos.add("<EqExp>");
        }
        return node;
    }
    
    public SyntaxNode parseLAndExp() { //EqExp{&&EqExp}
        SyntaxNode node = new SyntaxNode("LAndExp");
        node.addChild(parseEqExp());
        infos.add("<LAndExp>");
        while (peekToken().getTokenType() == TokenType.AND) {
            node.addChild(new SyntaxNode(peekToken().getToken()));
            addInfo();
            getNextToken();
            node.addChild(parseEqExp());
            infos.add("<LAndExp>");
        }
        return node;
    }
    
    public SyntaxNode parseLOrExp() {
        SyntaxNode node = new SyntaxNode("LOrExp");
        node.addChild(parseLAndExp());
        infos.add("<LOrExp>");
        while (peekToken().getTokenType() == TokenType.OR) {
            node.addChild(new SyntaxNode(peekToken().getToken()));
            addInfo();
            getNextToken();
            node.addChild(parseLAndExp());
            infos.add("<LOrExp>");
        }
        return node;
    }
    
    public SyntaxNode parseCond() {
        SyntaxNode node = new SyntaxNode("Cond");
        node.addChild(parseLOrExp());
        infos.add("<Cond>");
        return node;
    }
    
    public SyntaxNode parseForStmt() {
        SyntaxNode node = new SyntaxNode("ForStmt");
        node.addChild(parseLVal());
        node.addChild(new SyntaxNode("="));
        addInfo();// =
        getNextToken();
        node.addChild(parseExp());
        infos.add("<ForStmt>");
        return node;
    }
    
    public void parseIf(SyntaxNode node) {
        node.addChild(new SyntaxNode("if"));
        addInfo();
        getNextToken(); // (
        node.addChild(new SyntaxNode("("));
        addInfo();
        getNextToken(); //Cond
        node.addChild(parseCond());
        if (peekToken().getTokenType() == TokenType.RPARENT) {
            node.addChild(new SyntaxNode(")"));
            addInfo();
            getNextToken(); //Stmt
        } else {
            dealError(peekToken().getLineNum(), "j");
        }
        node.addChild(parseStmt());
        if (peekToken().getTokenType() == TokenType.ELSETK) {
            node.addChild(new SyntaxNode("else"));
            addInfo();
            getNextToken();
            node.addChild(parseStmt());
        }
    }
    
    public void parsePrintf(SyntaxNode node) {
        node.addChild(new SyntaxNode("printf"));
        addInfo();
        getNextToken(); // (
        node.addChild(new SyntaxNode("("));
        addInfo();
        getNextToken(); //StringConst
        /*TODO:不知道怎么写，待完成*/
        addInfo();
        getNextToken();
        while (peekToken().getTokenType() == TokenType.COMMA) {
            node.addChild(new SyntaxNode(","));
            addInfo();
            getNextToken();
            node.addChild(parseExp());
        }
        if (peekToken().getTokenType() == TokenType.RPARENT) {
            node.addChild(new SyntaxNode(")"));
            addInfo();
            getNextToken();
            if (peekToken().getTokenType() == TokenType.SEMICN) {
                node.addChild(new SyntaxNode(";"));
                addInfo();
                getNextToken();
            } else { //缺少;
                dealError(getMinErrorLineNum(), "i");
            }
        } else { //缺少)
            if (peekToken().getTokenType() == TokenType.SEMICN) {
                dealError(peekToken().getLineNum(), "j");
                node.addChild(new SyntaxNode(";"));
                addInfo();
                getNextToken();
            } else { //缺少)和;同时犯i,j类错误
                dealError(getMinErrorLineNum(), "j");
                dealError(getMinErrorLineNum(), "i");
            }
        }
    }
    
    public void parseBreakOrContinue(SyntaxNode node) {
        node.addChild(new SyntaxNode(peekToken().getToken()));
        addInfo();
        getNextToken();
        if (peekToken().getTokenType() == TokenType.SEMICN) {
            node.addChild(new SyntaxNode(";"));
            addInfo();
            getNextToken();
        } else {
            dealError(getMinErrorLineNum(), "i");
        }
    }
    
    public void parseReturn(SyntaxNode node) {
        node.addChild(new SyntaxNode("return"));
        addInfo(); //return
        getNextToken();
        if (peekToken().getTokenType() == TokenType.INTCON || peekToken().getTokenType() == TokenType.CHRCON
                || peekToken().getTokenType() == TokenType.IDENFR || peekToken().getTokenType() == TokenType.LPARENT
                || peekToken().getTokenType() == TokenType.PLUS || peekToken().getTokenType() == TokenType.MINU || peekToken().getTokenType() == TokenType.NOT) {
            node.addChild(parseExp());
        }
        if (peekToken().getTokenType() == TokenType.SEMICN) {
            node.addChild(new SyntaxNode(";"));
            addInfo();
            getNextToken();
        } else {
            dealError(getMinErrorLineNum(), "i");
        }
    }
    
    public void parseFor(SyntaxNode node) {
        node.addChild(new SyntaxNode("for"));
        addInfo();
        getNextToken(); // (
        node.addChild(new SyntaxNode("("));
        addInfo();
        getNextToken();
        if (peekToken().getTokenType() != TokenType.SEMICN) {
            node.addChild(parseForStmt());
        }
        node.addChild(new SyntaxNode(";"));
        addInfo(); // ;
        getNextToken();
        if (peekToken().getTokenType() != TokenType.SEMICN) {
            node.addChild(parseCond());
        }
        node.addChild(new SyntaxNode(";"));
        addInfo(); // ;
        getNextToken();
        if (peekToken().getTokenType() != TokenType.RPARENT) {
            node.addChild(parseForStmt());
        }
        node.addChild(new SyntaxNode(")"));
        addInfo(); // )
        getNextToken();
        node.addChild(parseStmt());
    }
    
    public SyntaxNode parseStmt() {
        SyntaxNode node = new SyntaxNode("Stmt");
        switch (peekToken().getTokenType()) {
            case IFTK:
                parseIf(node);
                break;
            case PRINTFTK:
                parsePrintf(node);
                break;
            case BREAKTK:
            case CONTINUETK:
                parseBreakOrContinue(node);
                break;
            case RETURNTK:
                parseReturn(node);
                break;
            case FORTK:
                parseFor(node);
                break;
            case LBRACE:
                node.addChild(parseBlock());
                break;
            case IDENFR:
                int lastPos = pos; //标识符的位置
                getNextToken(); // a[   ||  a = ， a=一定是LVal = exp的形式，a[还要判断
                if (peekToken().getTokenType() == TokenType.ASSIGN) { //是LVal = exp
                    pos = pos - 1; //回退到ident
                    LVal2Exp(node);
                    break;
                } else if (peekToken().getTokenType() == TokenType.LBRACK) { //a[
                    getNextToken(); //exp
                    int oldInfoSize = infos.size() - 1;
                    parseExp(); //出来应该指到的是]
                    int newSize = infos.size() - 1;
                    for (int i = oldInfoSize; i < newSize; i++) {
                        infos.remove(infos.size() - 1);
                    } //删除在parseExp中加的info，每次移除掉最尾部的即可
                    getNextToken(); //看是=还是其他
                    if (peekToken().getTokenType() == TokenType.ASSIGN) { //是LVal = Exp a[Exp] = exp
                        pos = lastPos;
                        LVal2Exp(node);
                        break;
                    } else { //是[Exp]，准备进行下面的parseExp
                        pos = lastPos;
                    }
                } else {
                    pos = pos - 1; //回退到ident 可能是函数调用啥的用下面的parseExp
                }
            default: //[Exp];
                if (peekToken().getTokenType() == TokenType.INTCON || peekToken().getTokenType() == TokenType.CHRCON
                        || peekToken().getTokenType() == TokenType.IDENFR || peekToken().getTokenType() == TokenType.LPARENT
                        || peekToken().getTokenType() == TokenType.PLUS || peekToken().getTokenType() == TokenType.MINU || peekToken().getTokenType() == TokenType.NOT) {
                    node.addChild(parseExp());
                }
                if (peekToken().getTokenType() == TokenType.SEMICN) {
                    node.addChild(new SyntaxNode(";"));
                    addInfo();
                    getNextToken();
                } else {
                    dealError(getMinErrorLineNum(), "i");
                }
        }
        infos.add("<Stmt>");
        return node;
    }
    
    public void LVal2Exp(SyntaxNode node) { //处理getint|getchar
        node.addChild(parseLVal());
        node.addChild(new SyntaxNode("="));
        addInfo(); // =
        getNextToken();
        if (peekToken().getTokenType() == TokenType.GETINTTK || peekToken().getTokenType() == TokenType.GETCHARTK) {
            node.addChild(new SyntaxNode(peekToken().getToken()));
            addInfo();
            getNextToken();// (
            node.addChild(new SyntaxNode("("));
            addInfo();
            getNextToken();
            if (peekToken().getTokenType() == TokenType.RPARENT) {
                node.addChild(new SyntaxNode(")"));
                addInfo();
                getNextToken();
                if (peekToken().getTokenType() == TokenType.SEMICN) {
                    node.addChild(new SyntaxNode(";"));
                    addInfo();
                    getNextToken();
                } else {
                    dealError(getMinErrorLineNum(), "i");
                }
            } else {
                if (peekToken().getTokenType() == TokenType.SEMICN) {
                    dealError(peekToken().getLineNum(), "j");
                    node.addChild(new SyntaxNode(";"));
                    addInfo();
                    getNextToken();
                } else {
                    dealError(getMinErrorLineNum(), "j");
                    dealError(getMinErrorLineNum(), "i");
                }
            }
        } else {
            node.addChild(parseExp());
            if (peekToken().getTokenType() == TokenType.SEMICN) {
                node.addChild(new SyntaxNode(";"));
                addInfo();
                getNextToken();
            } else {
                dealError(getMinErrorLineNum(), "i");
            }
        }
    }
    
    public Token getNextToken() {
        return pos < tokenList.size() - 1 ? tokenList.get(++pos) : null;
    }
    
    public Token peekToken() { //保证最顶端的一定是还未分析过的token
        return tokenList.get(pos);
    }
    
    public void addInfo() {
        infos.add(peekToken().getTokenType().toString() + " " + peekToken().getToken());
    }
    
    public void dealError(int lineNum, String type) { //考虑到词法分析会先生成错误信息，在最后的错误信息输出时应先按行号排序
        isError = true;
        errorList.add(new MyError(lineNum, type));
    }
    
    public Token getLastToken() { //得到上一个token
        return tokenList.get(pos - 1);
    }
    
    public int getMinErrorLineNum() { //取两者小的那个，因为分号一定在上一行
        return Math.min(getLastToken().getLineNum(), peekToken().getLineNum());
    }
    
    public void printTree() {
        Queue<SyntaxNode> queue = new LinkedList<>();
        queue.add(root);
        Queue<SyntaxNode> temp = new LinkedList<>();
        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                SyntaxNode node = queue.poll();
                System.out.print(node.getName()+" ");
                for (SyntaxNode child : node.getChildren()) {
                    temp.add(child);
                }
            }
            queue.addAll(temp);
            temp.clear();
            System.out.println();
        }
    }
}
