package players.mcts;

import core.AbstractGameState;
import core.interfaces.IPrintable;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;
import org.knowm.xchart.style.markers.None;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class TreeRecorder {
    public int visitThreshold = 1;
    String filename;
    IStateFeatureVector stateFeatures;
    IStateHeuristic heuristic;
    File file;
    FileWriter fileWriter;

    public TreeRecorder(String folder_name, IStateFeatureVector stateFeatures, AbstractGameState gameState,
                        IStateHeuristic heuristic) {
        this.filename = getRecordId(folder_name);
        file = new File(this.filename);
        this.stateFeatures = stateFeatures;
        this.heuristic = heuristic;

        try {
            fileWriter = new FileWriter(file, false);
            StringBuilder header = new StringBuilder();
            header.append("Depth\tName\tValue\tVisits\tParent_Name\tGame_State\tGame_Features\tGame_State_Heuristic\tAction_Name\tBest_Action");
            header.append(System.lineSeparator());
            fileWriter.write(header.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void recordData(SingleTreeNode root) {
        int current_idx = 0;
        Map<SingleTreeNode, String> nameMap = new HashMap<>();

        Queue<SingleTreeNode> nodeQueue = new ArrayDeque<>();
        nodeQueue.add(root);

        try {
            while (!nodeQueue.isEmpty()) {
                SingleTreeNode node = nodeQueue.poll();

                if (nameMap.get(node) == null) {
                    nameMap.put(node, "Node_" + (++current_idx));
                }

                StringBuilder output = new StringBuilder();
                int player = node.getActor();
                double stateValue = node.nodeValue(player);
                output.append(String.format("%d\t%s\t%.3g\t%d", node.depth, nameMap.get(node), stateValue , node.getVisits()));

                String parentName;
                if (node.parent == null) {
                    parentName = "None";
                } else {
                    parentName = nameMap.get(node.parent);
                }

                output.append("\t").append(parentName);
                output.append("\t").append(node.state.toString());
                output.append("\t").append(getFeaturesJson(node.state, player));
                output.append("\t").append(String.format("%.3g",heuristic.evaluateState(node.state, player)));
                if (node.actionToReach == null) {
                    output.append("\t").append("None");
                } else {
                    output.append("\t").append(node.actionToReach);
                }

                String bestAction;
                try {
                    bestAction = node.bestAction().toString();
                } catch (AssertionError e) {
                    bestAction = "None";
                }
                output.append("\t").append(bestAction);

                output.append(System.lineSeparator());

                fileWriter.write(output.toString());

                // add children of current node to queue
                for (SingleTreeNode child : node.children.values().stream()
                        .filter(Objects::nonNull)
                        .flatMap(Arrays::stream)
                        .filter(Objects::nonNull)
                        .filter(n -> n.getVisits() >= visitThreshold)
                        .collect(toList())) {
                    if (child != null)
                        nodeQueue.add(child);
                }
            }

            fileWriter.flush();
        } catch (IOException e) {
            System.out.println("Error writing file in MCTSRecordingPlayer");
            e.printStackTrace();
        }
    }

    String getFeaturesJson(AbstractGameState state, int player) {
        double[] stateVector = stateFeatures.featureVector(state, player);
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < stateVector.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append("\"").append(stateFeatures.names()[i]).append("\":").append(stateVector[i]);
        }
        sb.append("}");
        return sb.toString();
    }

    public void close() {
        try {
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getRecordId(String folder_name) {
        File directory = new File(folder_name);
        if (!directory.exists()){
            directory.mkdir();
            return folder_name + "/record_0.csv";
        }

        int max_idx = 0;

        for (final File fileEntry : directory.listFiles()) {
            String idx = fileEntry.getName().split("_")[1];
            idx = idx.split(".csv")[0];
            if (Integer.parseInt(idx) > max_idx) {
                max_idx = Integer.parseInt(idx);
            }
        }

        return folder_name + "/record_" + (max_idx + 1) + ".csv";
    }
}
