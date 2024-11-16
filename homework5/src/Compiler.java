import frontend.Lexer.Lexer;
import frontend.Parser.Parser;
import middle.IRGenerator;
import middle.IRGenerator3;

import java.io.BufferedReader;
import java.io.FileReader;

public class Compiler {
    public static void main(String[] args) {
        try (BufferedReader stdin = new BufferedReader(new FileReader("D:\\BUAA_Compile_2024\\homework5\\src\\testfile.txt"))) {
            Lexer lexer = new Lexer(stdin);
            lexer.analyse(); //开始词法分析
            Parser parser = new Parser(lexer.getTokenList(), lexer.getErrorList()); //将词法分析得到的词法单元流传入给parser
            IRGenerator3 irGenerator = new IRGenerator3(parser.parseCompUnit()); //将语法分析得到的syntaxTree拿进去进行语义分析及代码生成
            irGenerator.generateModule(); //生成中间代码
        } catch (Exception e) {
            System.out.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
