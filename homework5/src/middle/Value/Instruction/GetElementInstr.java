package middle.Value.Instruction;

import middle.Type.ArrayType;
import middle.Type.Integer32Type;
import middle.Type.Type;

public class GetElementInstr extends Instr {
    private int index; //数组的索引 (不清楚是否要采用Value)
    private String lastName; //上一次alloca得到的编号
    
    public GetElementInstr(Type type, String name, int index, String lastName) {
        super(type, name);
        this.index = index;
        this.lastName = lastName;
    }
    
    @Override
    public String toString() {
        if (((ArrayType) type).getElementType() instanceof Integer32Type) {
            return name + " = getelementptr " + type + ", " + type + "* " + lastName + ", i32 0, i32 " + index;
        } else {
            return name + " = getelementptr " + type + ", " + type + "* " + lastName + ", i8 0, i8 " + index;
        }
    }
}
