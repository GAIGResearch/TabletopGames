package gui;

import core.rules.Node;
import core.rules.nodetypes.ConditionNode;
import core.rules.nodetypes.BranchingRuleNode;
import core.rules.nodetypes.RuleNode;
import utilities.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * Shows game flow given root node rule for a game.
 * Legend:
 * - rectangles = rules
 * - circles = conditions
 * - blue nodes = require action
 * - green outline = root node
 * - red outline = terminal node for one game turn
 * - red X = node contains game over conditions and can trigger end of game
 */
public class GameFlowDiagram extends JFrame {

    public GameFlowDiagram(Node root) {

        JComponent mainArea = new TreeDraw(root);
        JScrollPane pane = new JScrollPane();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        pane.setPreferredSize(new Dimension(Math.min(screenSize.width, mainArea.getPreferredSize().width),
                Math.min(screenSize.height, mainArea.getPreferredSize().height)));
        getContentPane().add(mainArea);

        // Frame properties
        pack();
        this.setVisible(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        repaint();
    }

    private static class TreeDraw extends JComponent {
        Node root;
        HashMap<Integer, TreeNode> treeNodes;
        HashMap<Integer, Rectangle> treeNodeLocations;
        HashSet<TreeNode> drawn;
        int nodeSize = 15;
        int nodeGapX = 150;
        int nodeGapY = 50;
        int arrowSize = 8;
        Dimension size;
        int maxY = 0, maxX = 0;

        Map.Entry<Integer, Rectangle> dragging;

        TreeDraw(Node root) {
            this.root = root;
            treeNodes = new HashMap<>();
            treeNodeLocations = new HashMap<>();
            drawn = new HashSet<>();
            traverseNodes(root, 0);
            size = new Dimension((maxX+1) * (nodeSize + nodeGapX), (maxY+1) * (nodeSize + nodeGapY));
            treeNodes.get(root.getId()).root = true;

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    for (Map.Entry<Integer, Rectangle> en: treeNodeLocations.entrySet()) {
                        if (en.getValue().contains(e.getPoint())) {
                            // clicked on this rectangle
                            dragging = en;
                            break;
                        }
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (dragging != null) {
                        dragging.getValue().setLocation(e.getPoint());
                        treeNodeLocations.put(dragging.getKey(), dragging.getValue());
                        repaint();
                        dragging = null;
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragging != null) {
                        dragging.getValue().setLocation(e.getPoint());
                        treeNodeLocations.put(dragging.getKey(), dragging.getValue());
                        repaint();
                        dragging = null;
                    }
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return size;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawTree((Graphics2D)g);
        }

        private void traverseNodes(Node node, int level) {
            if (node == null || treeNodes.containsKey(node.getId())) return;
            TreeNode n = new TreeNode(node, level);
            treeNodes.put(node.getId(), n);
            int x = (n.x + 1) * nodeSize + n.x * nodeGapX;
            int y = (n.y + 1) * nodeSize + n.y * nodeGapY;
            treeNodeLocations.put(node.getId(), new Rectangle(x, y, nodeSize, nodeSize));
            if (n.y > maxY) maxY = n.y;
            if (n.x > maxX) maxX = n.x;

            if (node instanceof RuleNode) {
                if (node instanceof BranchingRuleNode) {
                    Node[] children = ((BranchingRuleNode) node).getChildren();
                    for (Node child : children) {
                        traverseNodes(child, level + 1);
                    }
                } else {
                    traverseNodes(node.getNext(), level + 1);
                }
            } if (node instanceof ConditionNode) {
                traverseNodes(((ConditionNode) node).getYesNo()[1], level + 1);
                traverseNodes(((ConditionNode) node).getYesNo()[0], level + 1);
            }
        }
        private void drawTree(Graphics2D g) {
            for (Map.Entry<Integer, TreeNode> e: treeNodes.entrySet()) {
                drawNode(g, e.getValue());
            }
        }

        private void drawNode(Graphics2D g, TreeNode n) {
            if (n != null) {
                drawn.add(n);

                int x = (int)treeNodeLocations.get(n.id).getX();
                int y = (int)treeNodeLocations.get(n.id).getY();

                // Draw the node
                g.setColor(Color.darkGray);
                if (n.actionRequired) {
                    g.setColor(Color.blue);
                }
                if (n.nextPlayer) {
                    g.setColor(Color.cyan);
                }
                if (n.type == TreeNode.NodeType.RULE) {
                    g.fillRect(x, y, nodeSize, nodeSize);
                    if (n.root) {
                        g.setColor(Color.green);
                        g.drawRect(x, y, nodeSize, nodeSize);
                    } else if (n.terminal) {
                        g.setColor(Color.red);
                        g.drawRect(x, y, nodeSize, nodeSize);
                    }
                } else {
                    g.fillOval(x, y, nodeSize, nodeSize);
                    if (n.root) {
                        g.setColor(Color.green);
                        g.drawOval(x, y, nodeSize, nodeSize);
                    } else if (n.terminal) {
                        g.setColor(Color.red);
                        g.drawOval(x, y, nodeSize, nodeSize);
                    }
                }
                if (n.gameOver) {
                    g.setColor(Color.red);
                    int fontSize = getFont().getSize();
                    g.drawString("X", x + nodeSize/2 - fontSize/4, y + nodeSize/2 + fontSize/2);
                }
                g.setColor(Color.black);
                g.drawString(n.name, x + nodeSize, y + nodeSize/2);

                // Draw lines to children
                g.setColor(Color.black);
                int nChildren = n.childrenId.length;
                for (int i = 0; i < nChildren; i++) {
                    int c = n.childrenId[i];
                    if (c != -1) {
                        TreeNode tn = treeNodes.get(c);
                        int dirX = tn.x - n.x;
                        int dirY = tn.y - n.y;

                        int x1 = x + nodeSize/2;
                        int y1 = y + nodeSize/2;
                        int x2 = (int)treeNodeLocations.get(tn.id).getX();
                        int y2 = (int)treeNodeLocations.get(tn.id).getY();

                        if (dirX > 0) {
                            // >
                            y2 += nodeSize/2;
                        } else if (dirX < 0) {
                            // <
                            y2 += nodeSize/2;
                            x2 += nodeSize;
                        } else {
                            x2 += nodeSize/2;
                            if (dirY < 0) {
                                // ^
                                y2 += nodeSize;
                            }
                        }

                        int optX = (x1+x2)/2;
                        int optY = (y1+y2)/2;
                        if ((n.x == tn.x && Math.abs(n.y - tn.y) > 1)
                                || Utils.indexOf(tn.childrenId, n.id) != -1 && drawn.contains(tn)) {  // vertical arc
                            Path2D.Double path = new Path2D.Double();
                            path.moveTo(x1, y1);
                            path.curveTo(x1, y1, x1 - nodeGapX/4.0, (y1 + y2)/2.0, x2, y2);
                            g.draw(path);
                            drawArrowHead(g, x2, y2,x1 - nodeGapX/4, (y1 + y2)/2);
                            optX = (x1+x1 - nodeGapX/4)/2;
                        } else if (Math.abs(n.x - tn.x) > 1) {  // horizontal arc
                            Path2D.Double path = new Path2D.Double();
                            path.moveTo(x1, y1);
                            path.curveTo(x1, y1, (x1+x2)/2.0, y1 - nodeGapY/4.0, x2, y2);
                            g.draw(path);
                            drawArrowHead(g, x2, y2, (x1+x2)/2, y1 - nodeGapY/4);
                            optY = (y1+(y1 + y2)/2)/2;
                        } else {
                            g.drawLine(x1, y1, x2, y2);
                            drawArrowHead(g, x2, y2, x1, y1);
                        }
                        g.fillOval(x1-arrowSize/4, y1-arrowSize/4, arrowSize/2, arrowSize/2);

                        if (n.type == TreeNode.NodeType.CONDITION) {
                            if (i == 0) {
                                // yes
                                g.drawString("yes", optX, optY);
                            } else {
                                // no
                                g.drawString("no", optX, optY);
                            }
                        }
                    }
                }
            }
        }

        private void drawArrowHead(Graphics2D g, int endX, int endY, int fromX, int fromY) {
            int dx = endX - fromX, dy = endY - fromY;
            double D = Math.sqrt(dx*dx + dy*dy);
            double xm = D - arrowSize, xn = xm, ym = arrowSize, yn = -arrowSize, x;
            double sin = dy / D, cos = dx / D;

            x = xm*cos - ym*sin + fromX;
            ym = xm*sin + ym*cos + fromY;
            xm = x;

            x = xn*cos - yn*sin + fromX;
            yn = xn*sin + yn*cos + fromY;
            xn = x;

            int[] xpoints = {endX, (int) xm, (int) xn};
            int[] ypoints = {endY, (int) ym, (int) yn};
            g.fillPolygon(xpoints, ypoints, 3);
        }
    }

    private static class TreeNode {
        int id;
        int x, y;
        String name;
        int[] childrenId;
        boolean gameOver, actionRequired, nextPlayer;
        boolean root, terminal;
        NodeType type;

        enum NodeType {
            RULE,
            CONDITION,
            BRANCHING
        }

        static int[] xAllocation = new int[20];

        TreeNode(Node n, int level) {
            this.id = n.getId();
            this.y = level;
            this.x = xAllocation[level]++;
            String[] sp = n.getClass().toString().split("\\.");
            if (sp.length > 4) {
                this.name = sp[4];
            } else {
                this.name = sp[3];
            }
            this.actionRequired = n.requireAction();
            this.nextPlayer = n.isNextPlayerNode();
            if (n instanceof RuleNode) {
                if (n instanceof BranchingRuleNode) {
                    this.type = NodeType.BRANCHING;
                    Node[] children = ((BranchingRuleNode) n).getChildren();
                    this.childrenId = new int[children.length];
                    terminal = true;
                    for (int i = 0; i < children.length; i++) {
                        if (children[i] != null) {
                            this.childrenId[i] = children[i].getId();
                            terminal = false;
                        } else this.childrenId[i] = -1;
                    }
                } else {
                    this.type = NodeType.RULE;
                    this.gameOver = ((RuleNode) n).getGameOverConditions().size() > 0;
                    this.childrenId = new int[1];
                    if (n.getNext() != null) this.childrenId[0] = n.getNext().getId();
                    else {
                        this.childrenId[0] = -1;
                        terminal = true;
                    }
                }
            } else if (n instanceof ConditionNode) {
                this.type = NodeType.CONDITION;
                this.childrenId = new int[2];
                Node yes = ((ConditionNode) n).getYesNo()[0];
                Node no = ((ConditionNode) n).getYesNo()[1];
                terminal = true;
                if (yes != null) {
                    this.childrenId[0] = yes.getId();
                    terminal = false;
                }
                else this.childrenId[0] = -1;
                if (no != null) {
                    this.childrenId[1] = no.getId();
                    terminal = false;
                }
                else this.childrenId[1] = -1;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TreeNode treeNode = (TreeNode) o;
            return id == treeNode.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }
}
