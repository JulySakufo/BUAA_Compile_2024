package frontend.Parser;

import frontend.Error.MyError;
import frontend.Lexer.Token;
import frontend.Lexer.TokenType;
import frontend.SymbolTable.Symbol;
import frontend.SymbolTable.SymbolTable;
import frontend.SyntaxTree.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;

public class Parser {
    private ArrayList<Token> tokenList;
    private ArrayList<MyError> errorList;
    private boolean isError;
    private int pos;
    private ArrayList<String> infos;
    private SyntaxNode root; //语法树的根节点
    private HashMap<Integer, SymbolTable> symbolTables; //记录每个符号表的信息level-table
    private Stack<SymbolTable> stack; //栈式符号表，记录当前栈的
    private int level;
    private String curFuncType;
    private int forStmtCount; //判断当前是否在解析for语句中
    private int formatCount;
    
    private int curParaType; //记录当前分析的实参类型,0代表值，1代表int型数组，2代表char型数组
    
    public Parser(ArrayList<Token> tokenList, ArrayList<MyError> errorList) {
        this.tokenList = tokenList;
        this.errorList = errorList;
        this.isError = false;
        this.pos = -1;
        this.infos = new ArrayList<>();
        this.root = new SyntaxNode("compUnit");
        this.symbolTables = new HashMap<>();
        this.stack = new Stack<>();
        this.level = 0;
        this.curFuncType = null;
        this.forStmtCount = 0;
        this.formatCount = 0;
        this.curParaType = 0;
        initializeSymbolTable();
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
            try (BufferedWriter stdout = new BufferedWriter(new FileWriter("D:\\BUAA_Compile_2024\\homework4\\src\\symbol.txt"))) {
                for (int i = 1; i <= level; i++) {
                    symbolTables.get(i).printSymbol(stdout);
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
        addInfo(); //const | int | char
        if (peekToken().getTokenType() == TokenType.CONSTTK) {
            SyntaxNode child = new SyntaxNode("ConstDecl");
            node.addChild(child);
            child.addChild(new SyntaxNode("const"));
            getNextToken();
            addInfo(); // int | char
            child.addChild(new SyntaxNode(peekToken().getToken())); // int | char
            String type = peekToken().getToken();
            getNextToken(); //指向下一个
            child.addChild(parseConstDef(type));
            while (peekToken().getTokenType() == TokenType.COMMA) {
                addInfo();
                child.addChild(new SyntaxNode(","));
                getNextToken();
                child.addChild(parseConstDef(type));
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
            String type = peekToken().getToken();
            getNextToken(); // varDef
            child.addChild(parseVarDef(type));
            while (peekToken().getTokenType() == TokenType.COMMA) {
                addInfo();
                child.addChild(new SyntaxNode(","));
                getNextToken();
                child.addChild(parseVarDef(type));
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
    
    public SyntaxNode parseConstDef(String type) {
        SyntaxNode node = new SyntaxNode("ConstDef");
        node.addChild(new SyntaxNode(peekToken().getToken()));
        addInfo(); //标识符
        String name = peekToken().getToken();
        Symbol symbol = new Symbol(name, "const", type, getStackLevel(stack.peek()));
        if (isRedefined(name)) {
            dealError(peekToken().getLineNum(), "b");
        } else {
            stack.peek().addSymbol(symbol);
        }
        getNextToken();
        /*TODO:变量值的赋值处理，暂时还没做，现在只做符号表的录入*/
        if (peekToken().getTokenType() == TokenType.LBRACK) {
            symbol.setIsArray(true); //是数组
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
    
    public SyntaxNode parseVarDef(String type) {
        SyntaxNode node = new SyntaxNode("VarDef");
        node.addChild(new SyntaxNode(peekToken().getToken()));
        addInfo(); //ident
        String name = peekToken().getToken();
        Symbol symbol = new Symbol(name, "var", type, getStackLevel(stack.peek()));
        if (isRedefined(name)) {
            dealError(peekToken().getLineNum(), "b");
        } else {
            stack.peek().addSymbol(symbol);
        }
        getNextToken();
        /*TODO:变量值的赋值处理，暂时还没做，现在只做符号表的录入*/
        if (peekToken().getTokenType() == TokenType.LBRACK) {
            symbol.setIsArray(true); //是数组
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
        child.addChild(new SyntaxNode(peekToken().getToken())); //functype-void|int|char
        addInfo();
        String type = peekToken().getToken();
        infos.add("<FuncType>");
        getNextToken(); //ident
        node.addChild(new SyntaxNode(peekToken().getToken()));
        addInfo();
        String name = peekToken().getToken();
        Symbol symbol = new Symbol(name, "func", type, getStackLevel(stack.peek()));
        curFuncType = type;
        if (isRedefined(name)) {
            dealError(peekToken().getLineNum(), "b");
        } else {
            stack.peek().addSymbol(symbol);
        }
        getNextToken(); //(
        node.addChild(new SyntaxNode("("));
        addInfo();
        getNextToken();
        //createSymbolTable();
        //addLevel(); //准备进入另一个作用域，level++，创建新的symbolTable
        SymbolTable symbolTable = new SymbolTable();
        stack.push(symbolTable);
        symbolTables.put(level + 1, symbolTable);
        symbol.setSymbolTable(symbolTable); //将这个符号表设置为该function symbol的symbolTable用来快速计算function的para
        if (peekToken().getTokenType() == TokenType.RPARENT) { //无参情况
            addInfo();
            node.addChild(new SyntaxNode(")"));
            getNextToken();
            node.addChild(parseBlock(true));
        } else { //有参情况
            if (peekToken().getTokenType() == TokenType.INTTK || peekToken().getTokenType() == TokenType.CHARTK) { //FuncFParams
                ArrayList<Integer> parasType = new ArrayList<>();
                node.addChild(parseFuncFParams(parasType));
                symbol.setParasType(parasType); //完善kind为func类型的symbol
                if (peekToken().getTokenType() == TokenType.RPARENT) {
                    node.addChild(new SyntaxNode(")"));
                    addInfo();
                    getNextToken();
                } else {
                    dealError(peekToken().getLineNum(), "j");
                }
            }
            node.addChild(parseBlock(true));
        }
        curFuncType = null; //该函数已经分析完毕
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
        curFuncType = "int"; // main 是保留的关键字，不纳入符号表中，但是考虑g类错误，就需要设置funcType供block检查用
        //createSymbolTable(); //main作用域的符号表
        SymbolTable symbolTable = new SymbolTable();
        stack.push(symbolTable);
        symbolTables.put(level + 1, symbolTable);
        if (peekToken().getTokenType() == TokenType.RPARENT) {
            node.addChild(new SyntaxNode(")"));
            addInfo();
            getNextToken(); //BLOCK
        } else {
            dealError(peekToken().getLineNum(), "j");
        }
        node.addChild(parseBlock(true));
        curFuncType = null; //main函数解析完毕
        infos.add("<MainFuncDef>");
        return node;
    }
    
    public SyntaxNode parseBlock(boolean isFuncDef) {
        SyntaxNode node = new SyntaxNode("Block");
        node.addChild(new SyntaxNode("{"));
        addInfo(); //{
        getNextToken();
        addLevel();
        if (!isFuncDef) { //函数内部的{BlockItem}块，创建一个表，负责只需要level++即可
            SymbolTable symbolTable = new SymbolTable();
            stack.push(symbolTable);
            symbolTables.put(level, symbolTable);
        }
        while (peekToken().getTokenType() != TokenType.RBRACE) {
            node.addChild(parseBlockItem());
        }
        boolean hasReturn = checkHasReturn(node);
        if (isFuncDef && !hasReturn && (curFuncType.equals("int") || curFuncType.equals("char"))) { //函数没有return语句
            dealError(peekToken().getLineNum(), "g");
        }
        node.addChild(new SyntaxNode("}"));
        stack.pop(); //弹出当前作用域的符号表，保持没分析完的作用域在stack里
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
    
    public SyntaxNode parseFuncFParams(ArrayList<Integer> parasType) { // FuncFParam{,FuncFParam}
        SyntaxNode node = new SyntaxNode("FuncFParams");
        node.addChild(parseFuncFParam(parasType));
        while (peekToken().getTokenType() == TokenType.COMMA) {
            node.addChild(new SyntaxNode(","));
            addInfo();
            getNextToken();
            node.addChild(parseFuncFParam(parasType));
        }
        infos.add("<FuncFParams>");
        return node;
    }
    
    public SyntaxNode parseFuncFParam(ArrayList<Integer> parasType) {
        SyntaxNode node = new SyntaxNode("FuncFParam");
        node.addChild(new SyntaxNode(peekToken().getToken()));
        addInfo();
        String type = peekToken().getToken();
        getNextToken(); //ident
        node.addChild(new SyntaxNode(peekToken().getToken()));
        addInfo();
        String name = peekToken().getToken();
        Symbol symbol = new Symbol(name, "para", type, getStackLevel(stack.peek()));
        if (isRedefined(name)) {
            dealError(peekToken().getLineNum(), "b");
        } else {
            stack.peek().addSymbol(symbol);
        }
        getNextToken(); //[或者nothing
        if (peekToken().getTokenType() == TokenType.LBRACK) {
            symbol.setIsArray(true);
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
        if (!symbol.getIsArray()) { //加入形参类型
            parasType.add(0);
        } else {
            if (type.equals("int")) {
                parasType.add(1);
            } else if (type.equals("char")) {
                parasType.add(2);
            }
        }
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
            curParaType = 0; //0代表值(变量)的意思
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
            curParaType = 0; //包是int型数值
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
            curParaType = 0; //带+-号的一定是数值
        } else if (peekToken().getTokenType() == TokenType.IDENFR) { //ident ([FuncParams]) 函数调用
            node.addChild(new SyntaxNode(peekToken().getToken()));
            addInfo();
            String name = peekToken().getToken(); //标识符名称
            Symbol symbol = getSymbol(name);
            int lineNum = peekToken().getLineNum();
            if (isUndefined(name)) {
                dealError(peekToken().getLineNum(), "c");
            }
            getNextToken(); //判断是否是(
            if (peekToken().getTokenType() == TokenType.LPARENT) { //明确是调用function了
                node.addChild(new SyntaxNode("("));
                addInfo();
                getNextToken();
                if (peekToken().getTokenType() == TokenType.RPARENT) { //无参情况
                    node.addChild(new SyntaxNode(")"));
                    addInfo();
                    getNextToken();
                } else { //有参情况
                    if (peekToken().getTokenType() == TokenType.INTCON || peekToken().getTokenType() == TokenType.CHRCON
                            || peekToken().getTokenType() == TokenType.IDENFR || peekToken().getTokenType() == TokenType.LPARENT
                            || peekToken().getTokenType() == TokenType.PLUS || peekToken().getTokenType() == TokenType.MINU || peekToken().getTokenType() == TokenType.NOT) {
                        ArrayList<Integer> paraList = new ArrayList<>(); //判断实参类型与个数，里面记录实参的类型
                        node.addChild(parseFuncRParams(paraList)); //实参的第一个字符可能的情况
                        if (paraList.size() != symbol.getParasType().size()) { //实参个数与形参个数不相等
                            dealError(lineNum, "d");
                        }
                        for (int i = 0; i < paraList.size(); i++) {
                            if (!paraList.get(i).equals(symbol.getParasType().get(i))) {
                                dealError(lineNum, "e"); //只记录一次e类错误
                                break;
                            }
                        }
                    }
                    if (peekToken().getTokenType() == TokenType.RPARENT) { // )
                        node.addChild(new SyntaxNode(")"));
                        addInfo();
                        getNextToken();
                    } else { //没有右括号
                        dealError(getMinErrorLineNum(), "j");
                    }
                    if (symbol.getIsArray()) {
                        if (symbol.getType().equals("int")) {
                            curParaType = 1;
                        } else if (symbol.getType().equals("char")) {
                            curParaType = 2;
                        }
                    } else {
                        curParaType = 0;
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
            curParaType = 0; //数组不参与运算，即不会带括号
        } else if (peekToken().getTokenType() == TokenType.INTCON) {
            SyntaxNode child = new SyntaxNode("Number");
            node.addChild(child);
            child.addChild(new SyntaxNode(peekToken().getToken()));
            addInfo();
            getNextToken();
            infos.add("<Number>");
            curParaType = 0;
        } else if (peekToken().getTokenType() == TokenType.CHRCON) {
            SyntaxNode child = new SyntaxNode("Character");
            node.addChild(child);
            child.addChild(new SyntaxNode(peekToken().getToken()));
            addInfo();
            getNextToken();
            infos.add("<Character>");
            curParaType = 0;
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
        String name = peekToken().getToken();
        Symbol symbol = getSymbol(name);
        if (getSymbol(name).getIsArray()) { //是数组
            if (symbol.getType().equals("int")) {
                curParaType = 1;
            } else if (symbol.getType().equals("char")) {
                curParaType = 2;
            }
        } else { //不是数组
            curParaType = 0;
        }
        if (isUndefined(name)) {
            dealError(peekToken().getLineNum(), "c");
        }
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
            curParaType = 0; //带[]一定是对数组的引用，值
        }
        infos.add("<LVal>");
        return node;
    }
    
    public SyntaxNode parseFuncRParams(ArrayList<Integer> paraList) { //Exp{,Exp}  FuncRParams->
        SyntaxNode node = new SyntaxNode("FuncRParams");
        node.addChild(parseExp());
        paraList.add(curParaType); //分析一个实参，记录一次实参类型
        while (peekToken().getTokenType() == TokenType.COMMA) {
            node.addChild(new SyntaxNode(","));
            addInfo();
            getNextToken();
            node.addChild(parseExp());
            paraList.add(curParaType);
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
        String name = peekToken().getToken();
        if (isConst(name)) {
            dealError(peekToken().getLineNum(), "h");
        }
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
    
    public void parsePrintf(SyntaxNode node) { // node.name = stmt
        node.addChild(new SyntaxNode("printf"));
        addInfo();
        int lineNum = peekToken().getLineNum(); //printf所在行号
        getNextToken(); // (
        node.addChild(new SyntaxNode("("));
        addInfo();
        getNextToken(); //StringConst "%d%c"
        /*TODO:不知道怎么写StringConst的语法树，待完成(有必要吗？)*/
        setFormatCount(peekToken().getToken()); //将string拿进去分析，看看里面有几个格式符，存进了formatCount里面
        addInfo();
        getNextToken();
        int count = 0; //计数有几个匹配的格式符
        while (peekToken().getTokenType() == TokenType.COMMA) {
            count++;
            node.addChild(new SyntaxNode(","));
            addInfo();
            getNextToken();
            node.addChild(parseExp());
        }
        if (formatCount != count) { //printf中格式符与表达式个数不匹配
            dealError(lineNum, "l");
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
        if (forStmtCount == 0) { //没有for循环却用了break或者continue
            dealError(peekToken().getLineNum(), "m");
        }
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
        int lineNum = peekToken().getLineNum();
        node.addChild(new SyntaxNode("return"));
        addInfo(); //return
        getNextToken();
        if (peekToken().getTokenType() == TokenType.INTCON || peekToken().getTokenType() == TokenType.CHRCON
                || peekToken().getTokenType() == TokenType.IDENFR || peekToken().getTokenType() == TokenType.LPARENT
                || peekToken().getTokenType() == TokenType.PLUS || peekToken().getTokenType() == TokenType.MINU || peekToken().getTokenType() == TokenType.NOT) {
            if (!(curFuncType.equals("int") || curFuncType.equals("char"))) { //不能return [exp]的情况
                dealError(lineNum, "f");
            }
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
    
    public void parseFor(SyntaxNode node) { //node.getName() = stmt stmt->for ( ... ) Block ----- Block -> { BlockItem } ----
        forStmtCount++; //嵌套层数++
        node.addChild(new SyntaxNode("for"));// BlockItem -> Stmt -> break|continue;
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
        forStmtCount--;//结束一层嵌套
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
                node.addChild(parseBlock(false));
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
        String name = peekToken().getToken();
        if (isConst(name)) { //常量不可被修改值
            dealError(peekToken().getLineNum(), "h");
        }
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
    
    public void addLevel() {
        level++;
    }
    
    public boolean isType(TokenType tokenType) {
        return peekToken().getTokenType() == tokenType;
    }
    
    public boolean isRedefined(String name) {
        return stack.peek().hasSymbol(name);
    }
    
    public boolean isUndefined(String name) {
        int size = stack.size() - 1;
        for (int i = size; i >= 0; i--) {
            SymbolTable symbolTable = stack.get(i);
            if (symbolTable.hasSymbol(name)) {
                return false;
            }
        }
        return true;
    }
    
    public void initializeSymbolTable() {
        //addLevel(); //准备进入另一个作用域，level++，创建新的symbolTable
        SymbolTable symbolTable = new SymbolTable();
        stack.push(symbolTable);
        symbolTables.put(++level, symbolTable);
    }
    
    public boolean checkHasReturn(SyntaxNode node) {
        if (curFuncType.equals("int") || curFuncType.equals("char")) { //g类错误只考虑}前面一行是否是return即可
            if (node.getLastChild() != null && node.getLastChild().getName().equals("BlockItem")) {
                SyntaxNode blockItemNode = node.getLastChild();
                if (blockItemNode.getLastChild() != null && blockItemNode.getLastChild().getName().equals("Stmt")) {
                    SyntaxNode stmtNode = blockItemNode.getLastChild();
                    if (stmtNode != null && stmtNode.getChildren().get(0).getName().equals("return")
                            && stmtNode.getChildren().get(1) != null && stmtNode.getChildren().get(1).getName().equals("Exp")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public boolean isConst(String name) {
        int size = stack.size() - 1;
        for (int i = size; i >= 0; i--) {
            SymbolTable symbolTable = stack.get(i); //从内层向外层看，找作用域最小的且在stack中的那一个
            if (symbolTable.hasSymbol(name) && symbolTable.getSymbol(name).isConst()) {
                return true;
            }
        }
        return false;
    }
    
    public void setFormatCount(String string) {
        formatCount = 0;
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) == '%') {
                if ((i + 1 < string.length()) && (string.charAt(i + 1) == 'd' || string.charAt(i + 1) == 'c')) {
                    formatCount++; //格式符的数量++
                    i++; //跳过d或者c，防止重复分析
                }
            }
        }
    }
    
    public Symbol getSymbol(String name) {
        int size = stack.size() - 1;
        for (int i = size; i >= 0; i--) {
            SymbolTable symbolTable = stack.get(i);
            if (name.contains("[")) { //是数组的一个选项
                int index = name.indexOf("[");
                name = name.substring(0, index);
            }
            if (symbolTable.hasSymbol(name)) {
                return symbolTable.getSymbol(name);
            }
        }
        return null;
    }
    
    public boolean isNumberPara(char ch) {
        return ch >= '0' && ch <= '9';
    }
    
    public boolean isCharPara(char ch) {
        return ch == '\'';
    }
    
    public int getStackLevel(SymbolTable symbolTable) {
        for (int i = 1; i <= symbolTables.size(); i++) { //level有可能还未更新，以tables.size作为衡量level的标准，因为11对应
            if (symbolTables.get(i) != null && symbolTables.get(i).equals(symbolTable)) {
                return i;
            }
        }
        return -1;
    }
}
