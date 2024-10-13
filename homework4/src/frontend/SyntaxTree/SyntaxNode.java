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
}
