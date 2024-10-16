package frontend.SyntaxTree;

import java.util.ArrayList;

public class SyntaxNode {
    private String name;
    private ArrayList<SyntaxNode> children;
    
    public SyntaxNode(String name) {
        this.name = name;
        this.children = new ArrayList<>();
    }
    
    public void addChild(SyntaxNode syntaxNode) {
        children.add(syntaxNode);
    }
    
    public void removeChild() {
        children.remove(children.size() - 1);
    }
    
    public String getName() {
        return name;
    }
    
    public ArrayList<SyntaxNode> getChildren() {
        return children;
    }
    
    public SyntaxNode getLastChild() {
        return children.get(children.size() - 1);
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (children.isEmpty()) { //分析到叶结点
            sb.append(name);
        } else { //非叶结点，语法树从左到右遍历
            for (SyntaxNode child : children) {
                sb.append(child.toString());
            }
        }
        return sb.toString();
    }
}
