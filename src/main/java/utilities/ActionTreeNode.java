package utilities;

import core.actions.AbstractAction;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ActionTreeNode {

    int value;
    AbstractAction action;
    String name;
    List<ActionTreeNode> children;
    int SubNodes;
    ActionTreeNode parent;

    // Constructors
    public ActionTreeNode() {
        this.children = new ArrayList<ActionTreeNode>();
        this.value = 0;
        this.action = null;
        this.name = "";
        this.SubNodes = 0;
    }
    public ActionTreeNode(int value) {
        this.children = new ArrayList<ActionTreeNode>();
        this.value = value;
        this.action = null;
        this.name = "";
        this.SubNodes = 0;
    }
    public ActionTreeNode(int value, String name) {
        this.children = new ArrayList<ActionTreeNode>();
        this.value = value;
        this.action = null;
        this.name = name;
        this.SubNodes = 0;
    }

    // Methods

    public List<ActionTreeNode> flattenTree(){
        List<ActionTreeNode> nodes = new ArrayList<ActionTreeNode>();
        nodes.add(this);
        for(ActionTreeNode child : this.children){
            nodes.addAll(child.flattenTree());
        }
        return nodes;
    }

    public int[] getActionMask() {
        List<Integer> vals = this.flattenValues();
        vals.remove(0);
        int[] valsArray = vals.stream().mapToInt(i -> i).toArray();
        return valsArray;
    }

    public List<String> getActionMaskNames() {
        List<String> vals = this.flattenNames();
        vals.remove(0);
        return vals;
    }

    public List<Integer> flattenValues(){
        List<Integer> values = new ArrayList<Integer>();
        values.add(this.value);
        for(ActionTreeNode child : this.children){
            values.addAll(child.flattenValues());
        }
        return values;
    }

    public List<String> flattenNames(){
        List<String> names = new ArrayList<String>();
        names.add(this.name);
        for(ActionTreeNode child : this.children){
            names.addAll(child.flattenNames());
        }
        return names;
    }

    public List<Integer> flattenBreadthFirst(){
        List<Integer> values = new ArrayList<Integer>();
        List<ActionTreeNode> nodes = new ArrayList<ActionTreeNode>();
        nodes.add(this);
        while(nodes.size() > 0){
            ActionTreeNode node = nodes.remove(0);
            values.add(node.value);
            nodes.addAll(node.children);
        }
        return values;
    }

    // Reset all the nodes in the tree, keeps structure only sets value and action to 0/null
    public void resetTree(){
        List<ActionTreeNode> nodes = new ArrayList<ActionTreeNode>();
        nodes.add(this);
        while(nodes.size() > 0){
            ActionTreeNode node = nodes.remove(0);
            node.value = 0;
            node.action = null;
            nodes.addAll(node.children);
        }
    }

    public ActionTreeNode findChildrenByName(String name){
        return findChildrenByName(name, false);
    }
    // sets the value of the node to 1 if it is found
    public ActionTreeNode findChildrenByName(String name, boolean setAvailable){
        List<ActionTreeNode> nodes = new ArrayList<ActionTreeNode>();
        nodes.add(this);
        while(nodes.size() > 0){
            ActionTreeNode node = nodes.remove(0);
            nodes.addAll(node.children);
            if (node.name.equals(name)){
                if(setAvailable){
                    node.value = 1;
                }
                return node;
            }
        }
        return null;
    }

    public String toJsonString() {
        return this.toJson().toJSONString();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("value", this.value);
        json.put("name", this.name);
        json.put("noSubNodes", this.SubNodes);
        List<JSONObject> children = new ArrayList<JSONObject>();
        for (ActionTreeNode child : this.children) {
            children.add(child.toJson());
        }
        json.put("children", children);
        return json;
    }

    // Add Child Functions
    public ActionTreeNode addChild(){
        ActionTreeNode child = new ActionTreeNode();
        this.children.add(child);
        child.parent = this;
        child.updateSubNodes();
        return child;
    }

    public ActionTreeNode addChild(int value){
        ActionTreeNode child = new ActionTreeNode(value);
        this.children.add(child);
        child.parent = this;
        child.updateSubNodes();
        return child;
    }

    public ActionTreeNode addChild(int value, String name){
        ActionTreeNode child = new ActionTreeNode(value, name);
        this.children.add(child);
        child.parent = this;
        child.updateSubNodes();
        return child;
    }

    public void updateSubNodes(){
        if(parent != null){
            parent.setSubNodes(parent.getSubNodes() + 1);
            parent.updateSubNodes();
        }
    }

    // Getters and Setters

    public AbstractAction getAction() {return action;}
    public void setAction(AbstractAction action) {
        this.value = 1;
        this.action = action;
    }
    public AbstractAction getActionByVector(int[] vector){
        ActionTreeNode node = this;
        for (int i = 0; i < vector.length; i++) {
            node = node.children.get(i);
        }
        return node.action;
    }
    public int getValue() {
        return value;
    }
    public void setValue(int value) {
        this.value = value;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public List<ActionTreeNode> getChildren() {
        return children;
    }
    public int getSubNodes() {
        return SubNodes;
    }

    public void setSubNodes(int subNodes) {
        SubNodes = subNodes;
    }

}
