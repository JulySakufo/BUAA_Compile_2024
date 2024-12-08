package backend.Assembly;

import java.util.ArrayList;

public class Data extends Asm {
    private String name;
    private String align;
    private ArrayList<Integer> values;
    private boolean isAsciiz;
    
    public Data(String name, String align, ArrayList<Integer> values) {
        this.name = name;
        this.align = align;
        this.values = values;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!isAsciiz) {
            sb.append("\t").append(name).append(": ").append(align).append(" ");
            sb.append(values.get(0));
            for (int i = 1; i < values.size(); i++) {
                sb.append(", ");
                sb.append(values.get(i));
            }
            sb.append("\n");
            return sb.toString();
        } else {
            /*TODO printf中的StringConst处理*/
            return null;
        }
    }
}
