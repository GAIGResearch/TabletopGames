package utilities;

import java.util.ArrayList;
import java.util.List;

public class ActionTreeTesting {

    public static void TestOrderOfTree() {
        // Create Tic Tac Toe Tree
        ActionTreeNode root = new ActionTreeNode(0, "root");
        ActionTreeNode x1 = root.addChild(1, "x1");
        ActionTreeNode x2 = root.addChild(2, "x2");
        ActionTreeNode x3 = root.addChild(3, "x3");

        // Y Values

        //x1
        ActionTreeNode y1 = x1.addChild(4, "y1-1");
        ActionTreeNode y2 = x1.addChild(5, "y1-2");
        ActionTreeNode y3 = x1.addChild(6, "y1-3");

        //x2
        ActionTreeNode y4 = x2.addChild(7, "y2-1");
        ActionTreeNode y5 = x2.addChild(8, "y2-2");
        ActionTreeNode y6 = x2.addChild(9, "y2-3");

        //x3
        ActionTreeNode y7 = x3.addChild(10, "y3-1");
        ActionTreeNode y8 = x3.addChild(11, "y3-2");
        ActionTreeNode y9 = x3.addChild(12, "y3-3");

        // Print root subnodes
        System.out.println("Root Subnodes: "+root.getSubNodes());
        System.out.println("X1 Subnodes: "+x1.getSubNodes());
        System.out.println("X2 Subnodes: "+x2.getSubNodes());
        System.out.println("X3 Subnodes: "+x3.getSubNodes());
        System.out.println("Y1 Subnodes: "+y1.getSubNodes());

        // Print flattend tree
        System.out.println(root.flattenValues());
        System.out.println(root.flattenNames());

        String jsonString = root.toJsonString();
        System.out.println(jsonString);
    }

    public static void TestChosenAction() {
        // Create Tic Tac Toe Tree
        ActionTreeNode root = new ActionTreeNode(0, "root");
        ActionTreeNode x1 = root.addChild(0, "x1");
        ActionTreeNode x2 = root.addChild(0, "x2");
        ActionTreeNode x3 = root.addChild(1, "x3");

        // Y Values

        //x1
        ActionTreeNode y1 = x1.addChild(0, "y1");
        ActionTreeNode y2 = x1.addChild(0, "y2");
        ActionTreeNode y3 = x1.addChild(0, "y3");

        //x2
        ActionTreeNode y4 = x2.addChild(0, "y1");
        ActionTreeNode y5 = x2.addChild(0, "y2");
        ActionTreeNode y6 = x2.addChild(0, "y3");

        //x3
        ActionTreeNode y7 = x3.addChild(0, "y1");
        ActionTreeNode y8 = x3.addChild(1, "y2");
        ActionTreeNode y9 = x3.addChild(0, "y3");

        //x3 y2 is the only valid action
        List<Integer> values = root.flattenValues();
        List<ActionTreeNode> nodes = root.flattenTree();

        // Remove the root node from the lists
        values.remove(0);
        nodes.remove(0);

        //Sanity Check
        assert values.size() == nodes.size();

        List<String> chosenNodes = new ArrayList<String>();

        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) == 1) {
                chosenNodes.add(nodes.get(i).getName());
            }
        }

        System.out.println("Chosen Position " + chosenNodes);

    }

    public static void DepthVsBreadth() {
        // Create Tic Tac Toe Tree
        ActionTreeNode root = new ActionTreeNode(0, "root");
        ActionTreeNode x1 = root.addChild(1, "x1");
        ActionTreeNode x2 = root.addChild(2, "x2");
        ActionTreeNode x3 = root.addChild(3, "x3");

        // Y Values

        //x1
        ActionTreeNode y1 = x1.addChild(4, "y1-1");
        ActionTreeNode y2 = x1.addChild(5, "y1-2");
        ActionTreeNode y3 = x1.addChild(6, "y1-3");

        //x2
        ActionTreeNode y4 = x2.addChild(7, "y2-1");
        ActionTreeNode y5 = x2.addChild(8, "y2-2");
        ActionTreeNode y6 = x2.addChild(9, "y2-3");

        //x3
        ActionTreeNode y7 = x3.addChild(10, "y3-1");
        ActionTreeNode y8 = x3.addChild(11, "y3-2");
        ActionTreeNode y9 = x3.addChild(12, "y3-3");

        // Print flattend tree
        List<ActionTreeNode> leafs = root.getLeafNodes();
        for (ActionTreeNode leaf : leafs) {
            System.out.println(leaf.getName());
        }
    }

    public static void main(String[] args) {
        DepthVsBreadth();
    }
}
