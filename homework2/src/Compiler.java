import frontend.Lexer.Lexer;

import java.io.BufferedReader;
import java.io.FileReader;

public class Compiler {
    public static void main(String[] args) {
        try (BufferedReader stdin = new BufferedReader(new FileReader("D:\\BUAA_Compile_2024\\homework2\\src\\testfile.txt"))) {
            Lexer lexer = new Lexer(stdin);
            lexer.analyse(); //开始词法分析
        } catch (Exception ignored) {
        
        }
    }
}
