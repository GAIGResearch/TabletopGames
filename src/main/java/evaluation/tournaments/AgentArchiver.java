package evaluation.tournaments;

import core.AbstractPlayer;
import org.apache.commons.io.FileUtils;
import utilities.Pair;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Utility class to archive agents from a tournament that are on the Pareto front.
 */
public class AgentArchiver {

    /**
     * Archives agents from the first Pareto front of the tournament results.
     *
     * @param results    The results of the tournament.
     * @param agents     The list of agents that participated in the tournament.
     * @param agentFiles The list of source JSON files for the agents. Must be in the same order as agents.
     * @param destDir    The destination directory where the archived agents will be stored.
     */
    public void archive(TournamentResults results, List<AbstractPlayer> agents, List<File> agentFiles, String destDir) {
        if (agentFiles == null || agentFiles.isEmpty()) {
            return;
        }
        if (agents.size() != agentFiles.size()) {
            throw new IllegalArgumentException("Agents and agentFiles must be the same size (" + agents.size() + " vs " + agentFiles.size() + ")");
        }

        // 1. Perform ParetoAnalysis to find the first Pareto front.
        ParetoAnalysis paretoAnalysis = new ParetoAnalysis();
        Map<String, Pair<Double, Double>> paretoRankings = paretoAnalysis.getRanking(results);

        List<String> firstParetoFront = paretoRankings.entrySet().stream()
                .filter(e -> e.getValue().a == 1.0)
                .map(Map.Entry::getKey)
                .toList();

        // 2. Perform AlphaRankAnalysis to provide secondary sorting and naming.
        AlphaRankAnalysis alphaRankAnalysis = new AlphaRankAnalysis(false);
        Map<String, Pair<Double, Double>> alphaRankings = alphaRankAnalysis.getRanking(results);

        // 2.5 Identify and remove close duplicates from archiving consideration
        List<String> poorClusterPerformers = alphaRankAnalysis.identifyCloseDuplicates(results, alphaRankings, 0.02);

        // Sort indices by alpha rank descending (highest first)
        Integer[] indices = new Integer[agents.size()];
        for (int i = 0; i < indices.length; i++) indices[i] = i;
        Arrays.sort(indices, (i1, i2) -> {
            String name1 = agents.get(i1).toString();
            String name2 = agents.get(i2).toString();
            Double rank1 = alphaRankings.getOrDefault(name1, new Pair<>(0.0, 0.0)).a;
            Double rank2 = alphaRankings.getOrDefault(name2, new Pair<>(0.0, 0.0)).a;
            return Double.compare(rank2, rank1);
        });

        // 3. Create destination directory: [destDir]/FinalAgents/
        File finalAgentsDir = new File(destDir, "FinalAgents");
        if (!finalAgentsDir.exists()) {
            if (!finalAgentsDir.mkdirs()) {
                System.err.println("Could not create directory: " + finalAgentsDir.getAbsolutePath());
                return;
            }
        }

        // 4. Copy identified files to standardized names (e.g., FinalAgent_R[rank]_A[alpha].json).
        int archivedCount = 0;
        for (int i = 0; i < indices.length; i++) {
            int originalIndex = indices[i];
            AbstractPlayer agent = agents.get(originalIndex);
            String agentName = agent.toString();
            if (firstParetoFront.contains(agentName) && !poorClusterPerformers.contains(agentName)) {
                File sourceFile = agentFiles.get(originalIndex);
                if (sourceFile == null || !sourceFile.exists()) {
                    System.err.println("Source file for agent " + agent + " not found: " + (sourceFile == null ? "null" : sourceFile.getAbsolutePath()));
                    continue;
                }

                archivedCount++;
                int rank = archivedCount;
                int alpha = (int) Math.round(alphaRankings.getOrDefault(agentName, new Pair<>(0.0, 0.0)).a * 100.0);
                String newFileName = String.format("FinalAgent_R%02d_A%02d.json", rank, alpha);
                File destFile = new File(finalAgentsDir, newFileName);

                try {
                    FileUtils.copyFile(sourceFile, destFile);
                } catch (IOException e) {
                    System.err.println("Error copying agent file: " + e.getMessage());
                }
            }
        }
    }
}
