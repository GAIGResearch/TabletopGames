package players.mcts;

import core.actions.AbstractAction;
import core.interfaces.IGameAttribute;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class ExpertIterationDataGatherer {

    File logFileV, logFileQ;
    List<IGameAttribute> features;
    FileWriter writerV, writerQ;
    public int visitThreshold = 10;

    public ExpertIterationDataGatherer(String fileStem, List<IGameAttribute> features) {
        this.logFileV = new File(fileStem + "_V.txt");
        this.logFileQ = new File(fileStem + "_Q.txt");
        this.features = features;
        try {
            // if file does not already exist, add a header row
            if (!logFileV.exists()) {
                writerV = new FileWriter(logFileV, false);
                StringBuilder header = new StringBuilder();
                header.append("Value\tDepth\tVisits");
                for (IGameAttribute feature : features) {
                    header.append("\t").append(feature.name());
                }
                header.append(System.lineSeparator());
                writerV.write(header.toString());
            } else {
                writerV = new FileWriter(logFileV, true);
            }

            if (!logFileQ.exists()) {
                writerQ = new FileWriter(logFileQ, false);
                StringBuilder header = new StringBuilder();
                header.append("Action\tActionHash\tValue\tAdvantage\tVisits\tN");
                for (IGameAttribute feature : features) {
                    header.append("\t").append(feature.name());
                }
                header.append(System.lineSeparator());
                writerQ.write(header.toString());
            } else {
                writerQ = new FileWriter(logFileQ, true);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void recordData(SingleTreeNode root) {

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
                output.append(String.format("%.3g\t%d\t%d", stateValue, node.depth, node.getVisits()));
                for (IGameAttribute feature : features) {
                    coreData.append("\t").append(feature.get(node.getState(), player));
                }
                output.append(coreData).append(System.lineSeparator());
                writerV.write(output.toString());

                // then write action data : the core feature data is the same, but we write one row per action, and the value we reach for that action
                for (AbstractAction action : node.children.keySet()) {
                    output = new StringBuilder();
                    if (node.children.get(action) == null || node.children.get(action)[player] == null)
                        continue;
                    SingleTreeNode childNode = node.children.get(action)[player];
                    double actionValue = childNode.getTotValue()[player] / childNode.getVisits();
                    output.append(String.format("%s\t%d\t%.3g\t%.3g\t%d\t%d", action.toString(), action.hashCode(),
                            actionValue, actionValue - stateValue, childNode.getVisits(), node.getVisits()));
                    output.append(coreData).append(System.lineSeparator());
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

}
