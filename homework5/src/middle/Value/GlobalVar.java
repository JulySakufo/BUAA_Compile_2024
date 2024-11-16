package middle.Value;

import frontend.SymbolTable.Symbol;
import middle.Type.Integer32Type;
import middle.Type.Integer8Type;
import middle.Type.Type;

public class GlobalVar extends Value {
    private Symbol symbol;
    
    public GlobalVar(Symbol symbol) {
        super(symbol.getType().equals("int") ? new Integer32Type() : new Integer8Type(), "@" + symbol.getName());
        this.symbol = symbol;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" = dso_local ");
        if (symbol.isConst()) {
            sb.append("constant ");
        } else {
            sb.append("global ");
        }
        if (!symbol.getIsArray()) {
            sb.append(type).append(" ").append(symbol.getValue()); //i32 0 | i8 0
        } else {
            sb.append("[").append(symbol.getArrayLength()).append(" x ").append(type).append("] ");
            sb.append("[");
            sb.append(type).append(" ").append(symbol.getValue(0));
            for (int i = 1; i < symbol.getValues().size(); i++) {
                sb.append(", ");
                sb.append(type).append(" ").append(symbol.getValue(i));
            }
            sb.append("]");
        }
        return sb.toString();
    }
}
