package frontend.Parser;

import frontend.Lexer.Token;
import frontend.Lexer.TokenType;
import frontend.SyntaxTree.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;

public class ParserSimplify {
    private ArrayList<Token> tokenList;
    private boolean isError;
    private int pos;
    private ArrayList<String> infos;
    private int count;
    
    public ParserSimplify(ArrayList<Token> tokenList) {
        this.tokenList = tokenList;
        this.isError = false;
        this.pos = -1;
        this.infos = new ArrayList<>();
        this.count = 0;
    }
    
    public void parseCompUnit() { //CompUnit → {Decl} {FuncDef} MainFuncDef
        CompUnit compUnit = new CompUnit();
        getNextToken(); //Decl → ConstDecl | VarDecl
        while (peekToken().getTokenType() == TokenType.CONSTTK || peekToken().getTokenType() == TokenType.INTTK
                || peekToken().getTokenType() == TokenType.CHARTK) { // {Decl}
            if (peekToken().getTokenType() == TokenType.CONSTTK) { //一定是变量声明
                compUnit.addDecl(parseDecl());
            } else { //可能是变量声明，可能不是
                getNextToken(); //int a || int a() || int main()
                if (peekToken().getTokenType() == TokenType.IDENFR) { //int a || int a() var|func
                    getNextToken();
                    if (peekToken().getTokenType() != TokenType.LPARENT) { // var
                        pos = pos - 2; //回退到int | char上
                        compUnit.addDecl(parseDecl());
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
                parseFuncDef();
            } else { //int main
                pos = pos - 1;
                break;
            }
        }
        parseMainFuncDef();
        infos.add("<CompUnit>");
        if (!isError) {
            try (BufferedWriter stdout = new BufferedWriter(new FileWriter("D:\\BUAA_Compile_2024\\homework3\\src\\parser.txt"))) {
                for (String info : infos) {
                    stdout.write(info + "\n");
                }
            } catch (Exception ignored) {
            
            }
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
                addInfo();
                getNextToken();
                constDecl.addConstDef(parseConstDef());
            }
            if (peekToken().getTokenType() == TokenType.SEMICN) { //遇到;结束
                addInfo();
                getNextToken();
            } else {
                dealError(peekToken().getLineNum() - 1, "i");
            }
            infos.add("<ConstDecl>");
            return constDecl;
        } else {
            VarDecl varDecl = new VarDecl();
            varDecl.setType(peekToken().getToken()); //int | char
            getNextToken(); // varDef
            varDecl.addVarDef(parseVarDef());
            while (peekToken().getTokenType() == TokenType.COMMA) {
                addInfo();
                getNextToken();
                varDecl.addVarDef(parseVarDef());
            }
            if (peekToken().getTokenType() == TokenType.SEMICN) {
                addInfo();
                getNextToken();
            } else {
                dealError(peekToken().getLineNum() - 1, "i");
            }
            infos.add("<VarDecl>");
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
            parseConstExp();
            if (peekToken().getTokenType() == TokenType.RBRACK) {
                addInfo();
                getNextToken();
            } else {
                dealError(peekToken().getLineNum(), "k");
            }
        }
        addInfo(); // =
        getNextToken();
        parseConstInitVal();
        infos.add("<ConstDef>");
        return null;
    }
    
    public VarDef parseVarDef() {
        addInfo(); //ident
        getNextToken();
        if (peekToken().getTokenType() == TokenType.LBRACK) {
            addInfo();
            getNextToken();
            parseConstExp();
            if (peekToken().getTokenType() == TokenType.RBRACK) {
                addInfo();
                getNextToken();
            } else {
                dealError(peekToken().getLineNum(), "k");
            }
        }
        if (peekToken().getTokenType() == TokenType.ASSIGN) {
            addInfo();
            getNextToken();
            parseInitVal();
        }
        infos.add("<VarDef>");
        return null;
    }
    
    public void parseConstInitVal() {
        if (peekToken().getTokenType() == TokenType.LBRACE) {
            addInfo();
            getNextToken();
            if (peekToken().getTokenType() != TokenType.RBRACE) { //{1,2}不是{}的情况
                parseConstExp();
                while (peekToken().getTokenType() == TokenType.COMMA) {
                    addInfo();
                    getNextToken();
                    parseConstExp();
                }
            } //考虑constInitVal的集合情况为空
            addInfo(); // }
            getNextToken();
        } else if (peekToken().getTokenType() == TokenType.STRCON) {
            addInfo();
            getNextToken();
        } else { //ConstExp
            parseConstExp();
        }
        infos.add("<ConstInitVal>");
    }
    
    public void parseInitVal() {
        if (peekToken().getTokenType() == TokenType.LBRACE) {
            addInfo();
            getNextToken();
            if (peekToken().getTokenType() != TokenType.RBRACE) {
                parseExp();
                while (peekToken().getTokenType() == TokenType.COMMA) {
                    addInfo();
                    getNextToken();
                    parseExp();
                }
            } //考虑int a[2] = {};的情况
            addInfo(); // }
            getNextToken();
        } else if (peekToken().getTokenType() == TokenType.STRCON) {
            addInfo();
            getNextToken();
        } else { //ConstExp
            parseExp();
        }
        infos.add("<InitVal>");
    }
    
    public void parseFuncDef() {
        addInfo();
        infos.add("<FuncType>");
        getNextToken(); //ident
        addInfo();
        getNextToken(); //(
        addInfo();
        getNextToken();
        if (peekToken().getTokenType() == TokenType.RPARENT) {
            addInfo();
            getNextToken();
            parseBlock();
        } else {
            if (peekToken().getTokenType() == TokenType.INTTK || peekToken().getTokenType() == TokenType.CHARTK) { //FuncFParams
                parseFuncFParams();
                if (peekToken().getTokenType() == TokenType.RPARENT) {
                    addInfo();
                    getNextToken();
                } else {
                    dealError(peekToken().getLineNum(), "j");
                }
            }
            parseBlock();
        }
        infos.add("<FuncDef>");
    }
    
    public void parseMainFuncDef() { // 'int' 'main' '(' ')' Block // j
        addInfo(); // int
        getNextToken(); //main
        addInfo();
        getNextToken(); // (
        addInfo();
        getNextToken();
        if (peekToken().getTokenType() == TokenType.RPARENT) {
            addInfo();
            getNextToken(); //BLOCK
        } else {
            dealError(peekToken().getLineNum(), "j");
        }
        parseBlock();
        infos.add("<MainFuncDef>");
    }
    
    public void parseBlock() {
        addInfo();
        getNextToken();
        while (peekToken().getTokenType() != TokenType.RBRACE) {
            //findError(); 不加这个会卡死，说明parseBlockitem后找不到}
            parseBlockItem();
        }
        addInfo();
        getNextToken();
        infos.add("<Block>");
    }
    
    public void parseBlockItem() {
        if (peekToken().getTokenType() == TokenType.CONSTTK || peekToken().getTokenType() == TokenType.INTTK || peekToken().getTokenType() == TokenType.CHARTK) {
            parseDecl();
        } else {
            parseStmt();
        }
    }
    
    public void parseFuncFParams() { // FuncFParam{,FuncFParam}
        parseFuncFParam();
        while (peekToken().getTokenType() == TokenType.COMMA) {
            addInfo();
            getNextToken();
            parseFuncFParam();
        }
        infos.add("<FuncFParams>");
    }
    
    public void parseFuncFParam() {
        addInfo();
        getNextToken(); //ident
        addInfo();
        getNextToken(); //[或者nothing
        if (peekToken().getTokenType() == TokenType.LBRACK) {
            addInfo();
            getNextToken();
            if (peekToken().getTokenType() == TokenType.RBRACK) {
                addInfo();
                getNextToken();
            } else {
                dealError(peekToken().getLineNum(), "k");
            }
        } //始终指向下一个未分析的token
        infos.add("<FuncFParam>");
    }
    
    public void parseConstExp() {
        parseAddExp();
        infos.add("<ConstExp>");
    }
    
    public void parseAddExp() { //MulExp{+-MulExp}
        parseMulExp();
        infos.add("<AddExp>");
        while (peekToken().getTokenType() == TokenType.PLUS || peekToken().getTokenType() == TokenType.MINU) {
            addInfo();
            getNextToken();
            parseMulExp();
            infos.add("<AddExp>");
        }
    }
    
    public void parseMulExp() { //UnaryExp{*/%UnaryExp}
        parseUnaryExp();
        infos.add("<MulExp>");
        while (peekToken().getTokenType() == TokenType.MULT || peekToken().getTokenType() == TokenType.DIV || peekToken().getTokenType() == TokenType.MOD) {
            addInfo();
            getNextToken();
            parseUnaryExp();
            infos.add("<MulExp>");
        }
    }
    
    public void parseUnaryExp() {
        if (peekToken().getTokenType() == TokenType.PLUS || peekToken().getTokenType() == TokenType.MINU || peekToken().getTokenType() == TokenType.NOT) {
            addInfo();
            infos.add("<UnaryOp>");
            getNextToken();
            parseUnaryExp();
        } else if (peekToken().getTokenType() == TokenType.IDENFR) { //ident ([FuncParams])
            addInfo();
            getNextToken(); //判断是否是(
            if (peekToken().getTokenType() == TokenType.LPARENT) {
                addInfo();
                getNextToken();
                if (peekToken().getTokenType() == TokenType.RPARENT) {
                    addInfo();
                    getNextToken();
                } else {
                    if (peekToken().getTokenType() == TokenType.INTCON || peekToken().getTokenType() == TokenType.CHRCON
                            || peekToken().getTokenType() == TokenType.IDENFR || peekToken().getTokenType() == TokenType.LPARENT
                            || peekToken().getTokenType() == TokenType.PLUS || peekToken().getTokenType() == TokenType.MINU || peekToken().getTokenType() == TokenType.NOT) {
                        parseFuncRParams(); //实参的第一个字符可能的情况
                    }
                    if (peekToken().getTokenType() == TokenType.RPARENT) { // )
                        addInfo();
                        getNextToken();
                    } else { //没有右括号
                        dealError(peekToken().getLineNum(), "i");
                    }
                }
            } else { //是primaryExp的ident
                pos--; //指到ident
                infos.remove(infos.size() - 1); //将之前的info移除，到primaryExp再添加
                parsePrimaryExp();
            }
        } else { //primaryExp
            parsePrimaryExp();
        }
        infos.add("<UnaryExp>");
    }
    
    public void parsePrimaryExp() {
        if (peekToken().getTokenType() == TokenType.LPARENT) { //(Exp)
            addInfo();
            getNextToken();
            parseExp();
            if (peekToken().getTokenType() == TokenType.RPARENT) {
                addInfo();
                getNextToken();
            } else {
                dealError(peekToken().getLineNum(), "j");
            }
        } else if (peekToken().getTokenType() == TokenType.INTCON) {
            addInfo();
            getNextToken();
            infos.add("<Number>");
        } else if (peekToken().getTokenType() == TokenType.CHRCON) {
            addInfo();
            getNextToken();
            infos.add("<Character>");
        } else { // LVal
            parseLVal();
        }
        infos.add("<PrimaryExp>");
    }
    
    public void parseLVal() { // ident['['Exp']']
        addInfo(); //ident
        getNextToken();
        if (peekToken().getTokenType() == TokenType.LBRACK) {
            addInfo();
            getNextToken();
            parseExp();
            if (peekToken().getTokenType() == TokenType.RBRACK) {
                addInfo();
                getNextToken();
            } else {
                dealError(peekToken().getLineNum(), "k");
            }
        }
        infos.add("<LVal>");
    }
    
    public void parseFuncRParams() { //Exp{,Exp}
        parseExp();
        while (peekToken().getTokenType() == TokenType.COMMA) {
            addInfo();
            getNextToken();
            parseExp();
        }
        infos.add("<FuncRParams>");
    }
    
    public void parseExp() {
        parseAddExp();
        infos.add("<Exp>");
    }
    
    public void parseRelExp() { //AddExp{<><=>=AddExp}
        parseAddExp();
        infos.add("<RelExp>");
        while (peekToken().getTokenType() == TokenType.LSS || peekToken().getTokenType() == TokenType.GRE
                || peekToken().getTokenType() == TokenType.LEQ || peekToken().getTokenType() == TokenType.GEQ) {
            addInfo();
            getNextToken();
            parseAddExp();
            infos.add("<RelExp>");
        }
    }
    
    public void parseEqExp() { //RelExp{==!=RelExp}
        parseRelExp();
        infos.add("<EqExp>");
        while (peekToken().getTokenType() == TokenType.EQL || peekToken().getTokenType() == TokenType.NEQ) {
            addInfo();
            getNextToken();
            parseRelExp();
            infos.add("<EqExp>");
        }
    }
    
    public void parseLAndExp() { //EqExp{&&EqExp}
        parseEqExp();
        infos.add("<LAndExp>");
        while (peekToken().getTokenType() == TokenType.AND) {
            addInfo();
            getNextToken();
            parseEqExp();
            infos.add("<LAndExp>");
        }
    }
    
    public void parseLOrExp() {
        parseLAndExp();
        infos.add("<LOrExp>");
        while (peekToken().getTokenType() == TokenType.OR) {
            addInfo();
            getNextToken();
            parseLAndExp();
            infos.add("<LOrExp>");
        }
    }
    
    public void parseCond() {
        parseLOrExp();
        infos.add("<Cond>");
    }
    
    public void parseForStmt() {
        parseLVal();
        addInfo();// =
        getNextToken();
        parseExp();
        infos.add("<ForStmt>");
    }
    
    public void parseStmt() {
        switch (peekToken().getTokenType()) {
            case IFTK:
                addInfo();
                getNextToken(); // (
                addInfo();
                getNextToken(); //Cond
                parseCond();
                if (peekToken().getTokenType() == TokenType.RPARENT) {
                    addInfo();
                    getNextToken(); //Stmt
                } else {
                    dealError(peekToken().getLineNum(), "j");
                }
                parseStmt();
                if (peekToken().getTokenType() == TokenType.ELSETK) {
                    addInfo();
                    getNextToken();
                    parseStmt();
                }
                break;
            case PRINTFTK:
                addInfo();
                getNextToken(); // (
                addInfo();
                getNextToken(); //StringConst
                addInfo();
                getNextToken();
                while (peekToken().getTokenType() == TokenType.COMMA) {
                    addInfo();
                    getNextToken();
                    parseExp();
                }
                if (peekToken().getTokenType() == TokenType.RPARENT) {
                    addInfo();
                    getNextToken();
                    if (peekToken().getTokenType() == TokenType.SEMICN) {
                        addInfo();
                        getNextToken();
                    } else { //缺少;
                        dealError(peekToken().getLineNum() - 1, "i");
                    }
                } else { //缺少)
                    if (peekToken().getTokenType() == TokenType.SEMICN) {
                        dealError(peekToken().getLineNum(), "j");
                        addInfo();
                        getNextToken();
                    } else { //缺少)和;同时犯i,j类错误
                        dealError(peekToken().getLineNum() - 1, "j");
                        dealError(peekToken().getLineNum() - 1, "i");
                    }
                }
                break;
            case BREAKTK:
            case CONTINUETK:
                addInfo();
                getNextToken();
                if (peekToken().getTokenType() == TokenType.SEMICN) {
                    addInfo();
                    getNextToken();
                } else {
                    dealError(peekToken().getLineNum() - 1, "i");
                }
                break;
            case RETURNTK:
                addInfo(); //return
                getNextToken();
                if (peekToken().getTokenType() == TokenType.INTCON || peekToken().getTokenType() == TokenType.CHRCON
                        || peekToken().getTokenType() == TokenType.IDENFR || peekToken().getTokenType() == TokenType.LPARENT
                        || peekToken().getTokenType() == TokenType.PLUS || peekToken().getTokenType() == TokenType.MINU || peekToken().getTokenType() == TokenType.NOT) {
                    parseExp();
                }
                if (peekToken().getTokenType() == TokenType.SEMICN) {
                    addInfo();
                    getNextToken();
                } else {
                    dealError(peekToken().getLineNum() - 1, "i");
                }
                break;
            case FORTK:
                addInfo();
                getNextToken(); // (
                addInfo();
                getNextToken();
                if (peekToken().getTokenType() != TokenType.SEMICN) {
                    parseForStmt();
                }
                addInfo(); // ;
                getNextToken();
                if (peekToken().getTokenType() != TokenType.SEMICN) {
                    parseCond();
                }
                addInfo(); // ;
                getNextToken();
                if (peekToken().getTokenType() != TokenType.RPARENT) {
                    parseForStmt();
                }
                addInfo(); // )
                getNextToken();
                parseStmt();
                break;
            case LBRACE:
                parseBlock();
                break;
            case IDENFR:
                int lastPos = pos; //标识符的位置
                getNextToken(); // a[   ||  a = ， a=一定是LVal = exp的形式，a[还要判断
                if (peekToken().getTokenType() == TokenType.ASSIGN) { //是LVal = exp
                    pos = pos - 1; //回退到ident
                    LVal2Exp();
                    break;
                } else if (peekToken().getTokenType() == TokenType.LBRACK) { //a[ TODO：又臭又长的代码，记得优化
                    getNextToken(); //exp
                    int oldInfoSize = infos.size() - 1;
                    parseExp(); //出来应该指到的是]
                    int newSize = infos.size() - 1;
                    for (int i = oldInfoSize; i < newSize; i++) {
                        infos.remove(infos.size() - 1);
                    } //key:删除在parseExp中加的info，每次移除掉最尾部的即可
                    //getNextToken(); //]
                    getNextToken(); //看是=还是其他
                    if (peekToken().getTokenType() == TokenType.ASSIGN) { //是LVal = Exp a[Exp] = exp
                        pos = lastPos;
                        LVal2Exp();
                        break;
                    } else { //是[Exp]，准备进行下面的parseExp
                        pos = lastPos;
                    }
                } else {
                    pos = pos - 1; //回退到ident 可能是函数调用啥的用下面的parse
                }
            default:
                //findError(); 不加会死循环
                if (peekToken().getTokenType() == TokenType.INTCON || peekToken().getTokenType() == TokenType.CHRCON
                        || peekToken().getTokenType() == TokenType.IDENFR || peekToken().getTokenType() == TokenType.LPARENT
                        || peekToken().getTokenType() == TokenType.PLUS || peekToken().getTokenType() == TokenType.MINU || peekToken().getTokenType() == TokenType.NOT) {
                    parseExp();
                }
                if (peekToken().getTokenType() == TokenType.SEMICN) {
                    addInfo();
                    getNextToken();
                } else {
                    dealError(peekToken().getLineNum() - 1, "i");
                }
        }
        infos.add("<Stmt>");
    }
    
    public void LVal2Exp() {
        parseLVal();
        addInfo(); // =
        getNextToken();
        if (peekToken().getTokenType() == TokenType.GETINTTK || peekToken().getTokenType() == TokenType.GETCHARTK) {
            addInfo();
            getNextToken();// (
            addInfo();
            getNextToken();
            if (peekToken().getTokenType() == TokenType.RPARENT) {
                addInfo();
                getNextToken();
                if (peekToken().getTokenType() == TokenType.SEMICN) {
                    addInfo();
                    getNextToken();
                } else {
                    dealError(peekToken().getLineNum() - 1, "i");
                }
            } else {
                if (peekToken().getTokenType() == TokenType.SEMICN) {
                    dealError(peekToken().getLineNum(), "j");
                    addInfo();
                    getNextToken();
                } else {
                    dealError(peekToken().getLineNum() - 1, "j");
                    dealError(peekToken().getLineNum() - 1, "i");
                }
            }
        } else {
            parseExp();
            if (peekToken().getTokenType() == TokenType.SEMICN) {
                addInfo();
                getNextToken();
            } else {
                dealError(peekToken().getLineNum() - 1, "i");
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
        try (BufferedWriter stderr = new BufferedWriter(new FileWriter("D:\\BUAA_Compile_2024\\homework3\\src\\error.txt", true))) {
            stderr.write(lineNum + " " + type + "\n");
        } catch (Exception ignored) {
        
        }
    }
    
    public void findError() {
        count++;
        if (count >= 40000) {
            try (BufferedWriter stdout = new BufferedWriter(new FileWriter("error.txt"))) {
                for (String info : infos) {
                    stdout.write(info + "\n");
                }
            } catch (Exception ignored) {
            
            }
            System.exit(0);
        }
    }
}
