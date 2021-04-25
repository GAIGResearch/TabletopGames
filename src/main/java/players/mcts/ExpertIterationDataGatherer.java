package players.mcts;

import core.interfaces.IGameAttribute;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class ExpertIterationDataGatherer {

    File logFile;
    List<IGameAttribute> features;
    FileWriter writer;
    public int visitThreshold = 10;

    public ExpertIterationDataGatherer(String log, List<IGameAttribute> features) {
        this.logFile = new File(log);
        this.features = features;
        try {
            // if file does not already exist, add a header row
            if (!logFile.exists()) {
                writer = new FileWriter(logFile, false);
                StringBuilder header = new StringBuilder();
                header.append("Value\tDepth\tVisits");
                for (IGameAttribute feature : features) {
                    header.append("\t").append(feature.name());
                }
                header.append(System.lineSeparator());
                writer.write(header.toString());
            } else {
                writer = new FileWriter(logFile, true);
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
                int player = node.getActor();
                output.append(String.format("%.3g\t%d\t%d", node.getTotValue()[player] / node.getVisits(), node.depth, node.getVisits()));
                for (IGameAttribute feature : features) {
                    output.append("\t").append(feature.get(node.getState(), player));
                }
                output.append(System.lineSeparator());
                writer.write(output.toString());

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
            writer.flush();
        } catch (IOException e) {
            System.out.println("Error writing file in MCTSRecordingPlayer");
            e.printStackTrace();
        }
    }

}
