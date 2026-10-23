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
 * With table=round it is the table of decisions in a round (for BUDGET_ROUND_SECONDS and roundAllocation), by
 * decisions made in the round, and with table=rounds the table of rounds in a game (MCTSParams.roundTable), by rounds
 * played. Both come from the GameMetrics$RoundDecisions metric, in ROUND_OVER.csv and GAME_OVER.csv.
 * <p>
 * Usage: DecisionTableBuilder out=data/timeManagement/Connect4_2p.txt in=dir1,dir2,file.csv [table=round|rounds]
 * where each input is a GAME_OVER.csv (or ROUND_OVER.csv) file, or a directory searched for them.
 */
public class DecisionTableBuilder {

    public static void main(String[] args) throws IOException {
        String out = null, in = null, type = "game";
        for (String arg : args) {
            if (arg.startsWith("out=")) out = arg.substring(4);
            if (arg.startsWith("in=")) in = arg.substring(3);
            if (arg.startsWith("table=")) type = arg.substring(6);
        }
        if (out == null || in == null || !List.of("game", "round", "rounds").contains(type))
            throw new IllegalArgumentException("Usage: DecisionTableBuilder out=table.txt in=dir1,dir2,file.csv " +
                    "[table=round|rounds]");
        List<String> fileNames = type.equals("round") ? List.of("ROUND_OVER.csv", "GAME_OVER.csv")
                : List.of("GAME_OVER.csv");
        String columns = switch (type) {
            case "round" -> "RoundDecisions\\(Decisions-P\\d+\\)";
            case "rounds" -> "RoundDecisions\\(Rounds\\)";
            default -> "(Decisions\\()?Decisions-P\\d+\\)?";
        };
        List<Integer> counts = new ArrayList<>();
        for (String input : in.split(","))
            for (String fileName : fileNames)
                for (Path file : eventFiles(Path.of(input), fileName))
                    counts.addAll(readCounts(file, columns));
        if (counts.isEmpty())
            throw new IllegalArgumentException("No " + columns + " columns in " + in);
        double[] table = table(counts);
        System.out.printf("%d records, mean %.2f%n", counts.size(),
                counts.stream().mapToInt(i -> i).average().orElse(0));
        String content = Arrays.stream(table).mapToObj(v -> String.format("%.3f", v)).collect(Collectors.joining("\n"));
        Files.writeString(Path.of(out), content + "\n");
    }

    private static List<Path> eventFiles(Path input, String fileName) throws IOException {
        if (!Files.isDirectory(input))
            return input.getFileName().toString().equals(fileName) ? List.of(input) : List.of();
        try (Stream<Path> files = Files.walk(input)) {
            return files.filter(f -> f.getFileName().toString().equals(fileName)).toList();
        }
    }

    /**
     * @return the values in every row of each column whose header matches the regular expression (none if no header
     * matches)
     */
    static List<Integer> readCounts(Path file, String columnRegex) throws IOException {
        List<String> lines = Files.readAllLines(file);
        String[] header = lines.get(0).split(",");
        List<Integer> columns = new ArrayList<>();
        for (int i = 0; i < header.length; i++)
            if (header[i].matches(columnRegex))
                columns.add(i);
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
     * @param decisionCounts - the number of decisions made by each player in each game (or round), or the number of
     *                       rounds in each game
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
