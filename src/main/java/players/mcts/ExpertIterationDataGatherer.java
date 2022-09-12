package players.mcts;

import core.AbstractForwardModel;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import core.interfaces.IStateFeatureVector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class ExpertIterationDataGatherer {

    public int visitThreshold = 50;
    File logFileV, logFileQ;
    //   List<IGameAttribute> features;
    IStateFeatureVector stateFeatures;
    IActionFeatureVector actionFeatures;
    FileWriter writerV, writerQ;

    /**
     * Two sets of features are provided.
     *
     * @param fileStem
     * @param stateFeatures  - the features that will be record to value a state (V)
     * @param actionFeatures - the additional features used for Q
     */
    public ExpertIterationDataGatherer(String fileStem, IStateFeatureVector stateFeatures, IActionFeatureVector actionFeatures) {
        if (stateFeatures == null)
            throw new IllegalArgumentException("stateFeatures must be specified - actionFeatures are optional");
        this.logFileV = new File(fileStem + "_V.txt");
        if (actionFeatures != null)
            this.logFileQ = new File(fileStem + "_Q.txt");
        this.stateFeatures = stateFeatures;
        this.actionFeatures = actionFeatures;
        try {
            // if file does not already exist, add a header row
            if (!logFileV.exists()) {
                writerV = new FileWriter(logFileV, false);
                StringBuilder header = new StringBuilder();
                header.append("Value\tDepth\tVisits");
                for (String feature : stateFeatures.names())
                    header.append("\t").append(feature);
                header.append(System.lineSeparator());
                writerV.write(header.toString());
            } else {
                writerV = new FileWriter(logFileV, true);
            }

            if (actionFeatures != null)
                if (!logFileQ.exists()) {
                    writerQ = new FileWriter(logFileQ, false);
                    StringBuilder header = new StringBuilder();
                    header.append("Action\tActionHash\tValue\tAdvantage\tVisits\tN");
                    for (String feature : stateFeatures.names())
                        header.append("\t").append(feature);
                    for (String feature : actionFeatures.names())
                        header.append("\t").append(feature);
                    header.append(System.lineSeparator());
                    writerQ.write(header.toString());
                } else {
                    writerQ = new FileWriter(logFileQ, true);
                }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void recordData(SingleTreeNode root, AbstractForwardModel forwardModel) {

        // Now do our stuff, and trawl through the root to record data
        Queue<SingleTreeNode> nodeQueue = new ArrayDeque<>();
        nodeQueue.add(root);

        try {
            while (!nodeQueue.isEmpty()) {
                SingleTreeNode node = nodeQueue.poll();
                // process this node
                // we record its depth, value, visits, and the full feature list
                StringBuilder output = new StringBuilder();
                StringBuilder coreData = new StringBuilder();
                int player = node.getActor();
                double stateValue = node.getTotValue()[player] / node.getVisits();
                List<AbstractAction> actionsFromState = forwardModel.computeAvailableActions(node.state);
                output.append(String.format("%.3g\t%d\t%d", stateValue, node.depth, node.getVisits()));
                double[] stateVector = stateFeatures.featureVector(node.state, player);
                String stateVectorAsString = Arrays.stream(stateVector).mapToObj(d -> String.format("%.4g", d)).collect(Collectors.joining("\t"));
                coreData.append("\t").append(stateVectorAsString);

                output.append(coreData).append(System.lineSeparator());
                writerV.write(output.toString());

                // then write action data : the core feature data is the same, but we write one row per action, and the value we reach for that action
                // plus any additional features that are action-specific
                if (actionFeatures != null)
                    for (AbstractAction action : actionsFromState) {
                        output = new StringBuilder();
                        if (node.children.get(action) == null || node.children.get(action)[player] == null)
                            continue;
                        SingleTreeNode childNode = node.children.get(action)[player];
                        double actionValue = childNode.getTotValue()[player] / childNode.getVisits();
                        output.append(String.format("%s\t%d\t%.3g\t%.3g\t%d\t%d", action.toString(), action.hashCode(),
                                actionValue, actionValue - stateValue, childNode.getVisits(), node.getVisits()));
                        output.append("\t").append(coreData); // the state features
                        double[] actionVector = actionFeatures.featureVector(action, node.state, player);
                        String actionVectorAsString = Arrays.stream(actionVector).mapToObj(d -> String.format("%.4g", d)).collect(Collectors.joining("\t"));
                        coreData.append("\t").append(actionVectorAsString).append(System.lineSeparator());
                        writerQ.write(output.toString());
                    }


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
            writerV.flush();
            writerQ.flush();
        } catch (IOException e) {
            System.out.println("Error writing file in MCTSRecordingPlayer");
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (writerQ != null)
                writerQ.close();
            if (writerV != null)
                writerV.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
