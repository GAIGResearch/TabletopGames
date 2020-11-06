package players.mcts;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class TreeStatistics {

    int maxDepth = 100;
    int depthReached = 0;
    int[] nodesAtDepth = new int[maxDepth];
    int[] leavesAtDepth = new int[maxDepth];
    int totalNodes;
    int totalLeaves;
    double[] nodeDistribution;
    double[] leafDistribution;

    public TreeStatistics(SingleTreeNode root) {
        Queue<SingleTreeNode> nodeQueue = new ArrayDeque<>();
        nodeQueue.add(root);
        while (!nodeQueue.isEmpty()) {
            SingleTreeNode node = nodeQueue.poll();
            if (node.depth < maxDepth) {
                nodesAtDepth[node.depth]++;
                for (SingleTreeNode child : node.children.values()) {
                    if (child != null)
                        nodeQueue.add(child);
                }
                if (node.children.values().stream().allMatch(Objects::isNull))
                    leavesAtDepth[node.depth]++;
            }
            if (node.depth > depthReached)
                depthReached = node.depth;
        }

        totalNodes = Arrays.stream(nodesAtDepth).sum();
        totalLeaves = Arrays.stream(leavesAtDepth).sum();
        nodeDistribution = Arrays.stream(nodesAtDepth, 0, Math.min(depthReached + 1, maxDepth)).asDoubleStream().map(i -> i / totalNodes).toArray();
        leafDistribution = Arrays.stream(leavesAtDepth, 0, Math.min(depthReached + 1, maxDepth)).asDoubleStream().map(i -> i / totalLeaves).toArray();
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
