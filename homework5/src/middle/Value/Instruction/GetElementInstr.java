package middle.Value.Instruction;

import middle.Type.ArrayType;
import middle.Type.Integer32Type;
import middle.Type.Type;
import middle.Value.Value;

public class GetElementInstr extends Instr {
    private int index; //数组的索引 (/*TODO 改为value以统一toString结构和初始化 */)
    private String lastName; //数组的首地址
    private boolean flag;
    
    public GetElementInstr(Type type, String name, int index, String lastName) {
        super(type, name);
        this.index = index;
        this.lastName = lastName;
        this.flag = false;
    }
    
    public GetElementInstr(Type type, String name, Value operand, String lastName) {
        super(type, name);
        operands.add(operand);
        this.lastName = lastName;
        this.flag = true;
    }
    
    @Override
    public String toString() {
        if (!flag) {
            if (((ArrayType) type).getElementType() instanceof Integer32Type) {
                return name + " = getelementptr " + type + ", " + type + "* " + lastName + ", i32 0, i32 " + index;
            } else {
                return name + " = getelementptr " + type + ", " + type + "* " + lastName + ", i8 0, i8 " + index;
            }
        } else {
            if (((ArrayType) type).getElementType() instanceof Integer32Type) {
                return name + " = getelementptr " + type + ", " + type + "* " + lastName + ", i32 0, i32 " + operands.get(0).getName();
            } else {
                return name + " = getelementptr " + type + ", " + type + "* " + lastName + ", i8 0, i8 " + operands.get(0).getName();
            }
        }
    }
}
