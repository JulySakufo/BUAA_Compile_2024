package middle.Type;

import frontend.Lexer.TokenType;
import frontend.Lexer.TokenTypeMap;

public class ArrayType extends Type {
    private int arrayLength;
    private Type elementType;
    
    public ArrayType(int arrayLength, Type elementType) {
        this.arrayLength = arrayLength;
        this.elementType = elementType;
    }
    
    public ArrayType(int arrayLength, String elementType) {
        this.arrayLength = arrayLength;
        this.elementType = TokenTypeMap.getInstance().getTokenType(elementType) == TokenType.INTTK ? new Integer32Type() : new Integer8Type();
    }
    
    @Override
    public String toString() {
        return "[" + arrayLength + " x " + elementType + "]";
    }
    
    public Type getElementType() {
        return elementType;
    }
    
    public int getArrayLength() {
        return arrayLength;
    }
}
