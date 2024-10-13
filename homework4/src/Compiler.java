import frontend.Lexer.Lexer;
import frontend.Parser.Parser;

import java.io.BufferedReader;
import java.io.FileReader;

public class Compiler {
    public static void main(String[] args) {
        try (BufferedReader stdin = new BufferedReader(new FileReader("D:\\BUAA_Compile_2024\\homework4\\src\\testfile.txt"))) {
            Lexer lexer = new Lexer(stdin);
            lexer.analyse(); //开始词法分析
            Parser parser = new Parser(lexer.getTokenList(), lexer.getErrorList()); //将词法分析得到的词法单元流传入给parser
            parser.parseCompUnit(); //开始语法分析
        } catch (Exception e) {
            System.out.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
