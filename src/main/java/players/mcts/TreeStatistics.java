package players.mcts;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class TreeStatistics {

    final public int maxDepth = 100;
    public int depthReached;
    public double meanLeafDepth;
    public double meanNodeDepth;
    public int[] nodesAtDepth = new int[maxDepth];
    public int[] leavesAtDepth = new int[maxDepth];
    public int[] gameTerminalNodesAtDepth = new int[maxDepth];
    public int totalNodes;
    public int totalLeaves;
    //   public int totalTerminalNodes;
    public double[] nodeDistribution;
    public double[] leafDistribution;
    public int maxActionsAtNode;
    public double meanActionsAtNode;
    public double meanActionsExpanded;
    public int oneActionNodes;


    public void mcgsStats(MCGSNode root) {
        Map<Object, MCGSNode> transpositionMap = root.getTranspositionMap();
        totalNodes = transpositionMap.size();
        Map<Integer, List<MCGSNode>> byDepth = transpositionMap.values().stream()
                .collect(Collectors.groupingBy(MCGSNode::getDepth));
        depthReached = byDepth.keySet().stream().max(Integer::compareTo).orElse(0);
        nodeDistribution = IntStream.range(0, depthReached + 1)
                .mapToDouble(i -> byDepth.getOrDefault(i, new ArrayList<>()).size() / (double) totalNodes)
                .toArray();
        totalLeaves = (int) transpositionMap.values().stream().filter(n -> n.nVisits <= root.params.initialiseVisits).count();
        meanActionsAtNode = transpositionMap.values().stream().mapToInt(n -> n.actionValues.size()).sum() / (double) totalNodes;
        meanActionsExpanded = transpositionMap.values().stream()
                .mapToInt(
                        n -> (int) n.actionValues.values().stream()
                                .filter(stats -> stats.nVisits > root.params.initialiseVisits)
                                .count()
                ).sum() / (double) Math.max(totalNodes - totalLeaves, 1);
        maxActionsAtNode = transpositionMap.values().stream().mapToInt(n -> n.actionValues.size()).max().orElse(0);

        // totalTerminalNodes = (int) transpositionMap.values().stream().filter(n -> !n.state.isNotTerminal()).count();
        leafDistribution = IntStream.range(0, depthReached + 1)
                .mapToDouble(i -> byDepth.getOrDefault(i, new ArrayList<>()).stream().filter(n -> n.nVisits == 0).count() / (double) totalLeaves)
                .toArray();
        meanLeafDepth = totalLeaves > 0 ? IntStream.range(0, depthReached + 1).mapToDouble(i -> i * leafDistribution[i]).sum() : 0;
        meanNodeDepth = IntStream.range(0, depthReached + 1).mapToDouble(i -> i * nodeDistribution[i]).sum();
        oneActionNodes = (int) transpositionMap.values().stream().filter(n -> n.actionValues.size() == 1).count();
    }

    public TreeStatistics(SingleTreeNode root) {
        if (root instanceof MCGSNode)
            mcgsStats((MCGSNode) root);
        else if (root instanceof MultiTreeNode)
            throw new AssertionError("Not expected");
        else
            mctsStats(root);
    }

    public void mctsStats(SingleTreeNode root) {
        Queue<SingleTreeNode> nodeQueue = new ArrayDeque<>();
        if (root instanceof MultiTreeNode) {
            throw new AssertionError("Not expected");
            //         for (SingleTreeNode node : ((MultiTreeNode) root).roots)
            //             if (node != null) nodeQueue.add(node);
        } else {
            nodeQueue.add(root);
        }

        int greatestDepth = 0;
        int maxActions = 0;
        int totalActions = 0;
        int expandedActions = 0;
        int oneAction = 0;
        while (!nodeQueue.isEmpty()) {
            SingleTreeNode node = nodeQueue.poll();
            if (node.depth < maxDepth) {
                nodesAtDepth[node.depth]++;
                if (node.terminalNode)
                    gameTerminalNodesAtDepth[node.depth]++;
                totalActions += node.actionValues.size();
                // We have expanded an action if it has been visited more often than initialiseVisits
                expandedActions += (int) node.actionValues.values().stream().filter(stats -> stats.nVisits > root.params.initialiseVisits).count();
                if (node.actionValues.size() == 1)
                    oneAction++;
                if (node.actionValues.size() > maxActions)
                    maxActions = node.actionValues.size();
                for (SingleTreeNode child : node.children.values().stream()
                        .filter(Objects::nonNull)
                        .flatMap(Arrays::stream)
                        .filter(Objects::nonNull)
                        .toList()) {
                    if (child != null)
                        nodeQueue.add(child);
                }
                if (node.actionValues.values().stream().allMatch(stats -> stats.nVisits <= root.params.initialiseVisits))
                    leavesAtDepth[node.depth]++;
            }
            if (node.depth > greatestDepth)
                greatestDepth = node.depth;
        }

        maxActionsAtNode = maxActions;
        depthReached = greatestDepth;
        totalNodes = Arrays.stream(nodesAtDepth).sum();
        oneActionNodes = oneAction;
        totalLeaves = Arrays.stream(leavesAtDepth).sum();

        meanActionsAtNode = (double) totalActions / totalNodes;
        meanActionsExpanded = (double) expandedActions / Math.max(totalNodes - totalLeaves, 1);
        //   totalTerminalNodes = Arrays.stream(gameTerminalNodesAtDepth).sum();
        nodeDistribution = Arrays.stream(nodesAtDepth, 0, Math.min(depthReached + 1, maxDepth)).asDoubleStream().map(i -> i / totalNodes).toArray();
        leafDistribution = Arrays.stream(leavesAtDepth, 0, Math.min(depthReached + 1, maxDepth)).asDoubleStream().map(i -> i / totalLeaves).toArray();
        meanLeafDepth = totalLeaves > 0 ? IntStream.range(0, depthReached + 1).mapToDouble(i -> i * leafDistribution[i]).sum() : 0;
        meanNodeDepth = IntStream.range(0, Math.min(depthReached + 1, maxDepth)).mapToDouble(i -> i * nodeDistribution[i]).sum();
    }


    @Override
    public String toString() {
        StringBuilder retValue = new StringBuilder();
        retValue.append(String.format("%d nodes and %d leaves, with maximum depth %d\n", totalNodes, totalLeaves, depthReached));
        List<String> nodeDist = Arrays.stream(nodeDistribution).mapToObj(n -> String.format("%2.0f%%", n * 100.0)).collect(toList());
        List<String> leafDist = Arrays.stream(leafDistribution).mapToObj(n -> String.format("%2.0f%%", n * 100.0)).collect(toList());
        retValue.append(String.format("\tNodes  by depth: %s\n", String.join(", ", nodeDist)));
        retValue.append(String.format("\tLeaves by depth: %s\n", String.join(", ", leafDist)));

        return retValue.toString();
    }
}
