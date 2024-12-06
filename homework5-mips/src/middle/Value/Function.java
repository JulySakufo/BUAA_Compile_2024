package middle.Value;

import backend.Assembly.LabelAsm;
import backend.MipsGenerator;
import backend.RegisterController;
import middle.Type.Type;
import middle.Type.VoidType;
import middle.Value.Instruction.AllocaInstr;
import middle.Value.Instruction.Instr;
import middle.Value.Instruction.ReturnInstr;
import middle.Value.Instruction.StoreInstr;

import java.util.ArrayList;

public class Function extends Value {
    private ArrayList<Param> params;
    private ArrayList<BasicBlock> basicBlocks;
    
    public Function(Type type, String name) {
        super(type, name);
        this.params = new ArrayList<>();
        this.basicBlocks = new ArrayList<>();
    }
    
    public void addParam(Param param) {
        params.add(param);
    }
    
    public void addBasicBlock(BasicBlock basicBlock) {
        if (basicBlocks.isEmpty()) {
            basicBlock.setFirstBlock(true);
        }
        basicBlocks.add(basicBlock);
    }
    
    public ArrayList<Param> getParams() {
        return params;
    }
    
    public int getIndexOfBlock(BasicBlock basicBlock) { //返回基本块是function的第几个
        return basicBlocks.indexOf(basicBlock);
    }
    
    public boolean isLastInstrReturnVoid() {
        Instr instr = basicBlocks.get(basicBlocks.size() - 1).getLastInstr();
        return instr instanceof ReturnInstr && instr.getType() instanceof VoidType;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (params.isEmpty()) { //无参数
            sb.append("define dso_local ").append(type).append(" @").append(name).append("() {\n");
            for (BasicBlock basicBlock : basicBlocks) {
                sb.append(basicBlock.toString());
            }
            sb.append("}\n");
        } else {
            sb.append("define dso_local ").append(type).append(" @").append(name).append("(");
            sb.append(params.get(0));
            for (int i = 1; i < params.size(); i++) {
                sb.append(" ,").append(params.get(i));
            }
            sb.append("){\n");
            for (BasicBlock basicBlock : basicBlocks) {
                sb.append(basicBlock.toString());
            }
            sb.append("}\n");
        }
        return sb.toString();
    }
    
    @Override
    public void generateMips() {
        RegisterController.getRegisterController().enterFunction(this); //准备对当前函数生成mips
        MipsGenerator.getMipsGenerator().addAsm(new LabelAsm(name)); //对当前是哪个函数生成标签名
        for (Param param : params) {
            param.generateMips();
        }
        for (BasicBlock basicBlock : basicBlocks) {
            basicBlock.generateMips();
        }
        RegisterController.getRegisterController().leaveFunction(this);
    }
}
