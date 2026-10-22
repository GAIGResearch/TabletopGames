package players.mcts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds the table for the EXP-MOVES strategy (MCTSParams.decisionTable) from the GAME_OVER.csv files written by
 * MCTSBudgetSweep (or any run with the GameMetrics$Decisions metric). Entry k is the mean number of decisions still
 * to make (including the current one) by a player who has already made k decisions, over all players and games in
 * which k decisions were reached. Only decisions with more than one action are counted, as only those are searched.
 * <p>
 * With event=ROUND_OVER the table is for BUDGET_ROUND_SECONDS, by decisions made in the round, from the ROUND_OVER.csv
 * files of the GameMetrics$RoundDecisions metric.
 * <p>
 * Usage: DecisionTableBuilder out=data/timeManagement/Connect4_2p.txt in=dir1,dir2,file.csv [event=ROUND_OVER]
 * where each input is a GAME_OVER.csv (or ROUND_OVER.csv) file, or a directory searched for them.
 */
public class DecisionTableBuilder {

    public static void main(String[] args) throws IOException {
        String out = null, in = null, event = "GAME_OVER";
        for (String arg : args) {
            if (arg.startsWith("out=")) out = arg.substring(4);
            if (arg.startsWith("in=")) in = arg.substring(3);
            if (arg.startsWith("event=")) event = arg.substring(6);
        }
        if (out == null || in == null)
            throw new IllegalArgumentException("Usage: DecisionTableBuilder out=table.txt in=dir1,dir2,file.csv " +
                    "[event=ROUND_OVER]");
        List<Integer> decisionCounts = new ArrayList<>();
        for (String input : in.split(","))
            for (Path file : eventFiles(Path.of(input), event + ".csv"))
                decisionCounts.addAll(readDecisionCounts(file));
        double[] table = table(decisionCounts);
        System.out.printf("%d records, mean decisions %.2f%n", decisionCounts.size(),
                decisionCounts.stream().mapToInt(i -> i).average().orElse(0));
        String content = Arrays.stream(table).mapToObj(v -> String.format("%.3f", v)).collect(Collectors.joining("\n"));
        Files.writeString(Path.of(out), content + "\n");
    }

    private static List<Path> eventFiles(Path input, String fileName) throws IOException {
        if (!Files.isDirectory(input))
            return List.of(input);
        try (Stream<Path> files = Files.walk(input)) {
            return files.filter(f -> f.getFileName().toString().equals(fileName)).toList();
        }
    }

    /**
     * @return the decisions made by each player in each game (or round), from the Decisions-P* columns
     */
    static List<Integer> readDecisionCounts(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        String[] header = lines.get(0).split(",");
        List<Integer> columns = new ArrayList<>();
        for (int i = 0; i < header.length; i++)
            if (header[i].matches(".*Decisions-P\\d+\\)?"))
                columns.add(i);
        if (columns.isEmpty())
            throw new IllegalArgumentException("No Decisions-P* columns in " + file);
        List<Integer> counts = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) continue;
            String[] values = line.split(",");
            for (int c : columns)
                if (c < values.length && !values[c].isEmpty())
                    counts.add(Integer.parseInt(values[c].trim()));
        }
        return counts;
    }

    /**
     * @param decisionCounts - the number of decisions made by each player in each game (or round)
     */
    public static double[] table(List<Integer> decisionCounts) {
        int max = decisionCounts.stream().mapToInt(i -> i).max().orElse(1);
        double[] table = new double[Math.max(max, 1)];
        for (int k = 0; k < table.length; k++) {
            final int made = k;
            table[k] = decisionCounts.stream().filter(d -> d > made).mapToInt(d -> d - made).average().orElse(1.0);
        }
        return table;
    }
}
