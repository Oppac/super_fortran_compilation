import java.util.ArrayList;
import java.util.List;

public class AbstractSyntaxTree {

  private String label;
  private List<AbstractSyntaxTree> children = new ArrayList<AbstractSyntaxTree>();

  public AbstractSyntaxTree() {
  }

  public AbstractSyntaxTree(String label) {
    this.label = label;
  }

  public AbstractSyntaxTree(List<AbstractSyntaxTree> children) {
    this.children = children;
  }

  public AbstractSyntaxTree(String label, List<AbstractSyntaxTree> children) {
    this.label = label;
    this.children = children;
  }

  public void addLabel(String label) {
    this.label = label;
  }

  public void addChild(AbstractSyntaxTree child) {
    this.children.add(child);
  }

  public void addChildLabel(AbstractSyntaxTree child) {
    this.label = child.getLabel();
    for (AbstractSyntaxTree c: child.getChildren()) {
      this.children.add(c);
    }
  }

  public void addChild(List<AbstractSyntaxTree> children) {
    this.children.addAll(children);
  }

  public String getLabel() {
      return this.label;
  }

  public AbstractSyntaxTree getChild(int n) {
    return children.get(n);
  }

  public List<AbstractSyntaxTree> getChildren() {
    return this.children;
  }

  public void removeEpsilons() {
    List<AbstractSyntaxTree> toRemove = new ArrayList<AbstractSyntaxTree>();
    for (AbstractSyntaxTree child: children) {
      if (child.getLabel() == "Epsilon") {
        toRemove.add(child);
      } else {
        child.removeEpsilons();
      }
    }
    children.removeAll(toRemove);
  }

  //Remove duplicate minus when it is before an ExprArith
  public void removeBadMinus() {
    List<AbstractSyntaxTree> toRemove = new ArrayList<AbstractSyntaxTree>();
    List<AbstractSyntaxTree> toAdd = new ArrayList<AbstractSyntaxTree>();
    for (AbstractSyntaxTree child: children) {
      if (label.equals(child.getLabel()) && child.getChildren().size() == 1) {
        toAdd.addAll(child.getChildren());
        toRemove.add(child);
      } else if (label.equals("-e") && child.getLabel().equals("-e")) {
        toAdd.addAll(child.getChildren());
        toRemove.add(child);
      } else {
        child.removeBadMinus();
      }
    }
    children.removeAll(toRemove);
    children.addAll(toAdd);
  }

  public AbstractSyntaxTree reverseCond(AbstractSyntaxTree cond) {
    if (cond.getLabel().equals("=")) {
      cond.addLabel("<>");
    } else if (cond.getLabel().equals(">=")) {
      cond.addLabel("<");
    } else if (cond.getLabel().equals(">")) {
      cond.addLabel("<=");
    } else if (cond.getLabel().equals("<=")) {
      cond.addLabel(">");
    } else if (cond.getLabel().equals("<")) {
      cond.addLabel(">=");
    } else if (cond.getLabel().equals("<>")) {
      cond.addLabel("=");
    }
    return cond;
  }

  public String printTree() {
      StringBuilder tree = new StringBuilder();
      tree.append("\n[");
      tree.append(label);
      if (children != null) {
          for (AbstractSyntaxTree child: children) {
              tree.append(child.printTree());
          }
      }
      tree.append("]\n");
      return tree.toString();
  }

}
