package middle.Value;

import backend.Assembly.Data;
import backend.MipsGenerator;
import frontend.SymbolTable.Symbol;
import middle.Type.Integer32Type;
import middle.Type.Integer8Type;
import middle.Type.Type;

import java.util.ArrayList;

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
    
    @Override
    public void generateMips() {
        String name = symbol.getName();
        String type = this.type instanceof Integer32Type ? ".word" : ".byte";
        ArrayList<Integer> values = new ArrayList<>();
        if (!symbol.getIsArray()) { //如果是常量,values.get(0)是值
            values.add(symbol.getValue());
        } else { //如果是数组，对应数组
            for (int i = 0; i < symbol.getValues().size(); i++) {
                values.add(symbol.getValue(i));
            }
        }
        MipsGenerator.getMipsGenerator().addData(new Data(name, type, values));
    }
}
