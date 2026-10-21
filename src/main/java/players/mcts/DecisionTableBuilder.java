package players.mcts;

import core.AbstractPlayer;
import core.Game;
import evaluation.listeners.IGameListener;
import evaluation.metrics.Event;
import games.GameType;
import players.PlayerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the table for the EXP-MOVES strategy (MCTSParams.decisionTable) from self-play: entry k is the mean number
 * of decisions still to make (including the current one) by a player who has already made k decisions, over all
 * players and games in which k decisions were reached. Only decisions with more than one action are counted, as
 * those are the only ones searched.
 * <p>
 * Usage: DecisionTableBuilder game=Connect4 player=json/players/mcts.json games=1000 out=table.txt [nPlayers=2]
 * [seed=1]
 * <p>
 * The decisions made by each player in each game are also written to out.counts. To combine runs (for example from
 * parallel processes), use: DecisionTableBuilder counts=a.counts,b.counts out=table.txt
 */
public class DecisionTableBuilder {

    public static void main(String[] args) throws IOException {
        Map<String, String> argMap = Arrays.stream(args).map(a -> a.split("=", 2))
                .collect(Collectors.toMap(a -> a[0], a -> a[1]));
        Path out = Path.of(argMap.get("out"));
        if (argMap.containsKey("counts")) {
            List<Integer> decisionCounts = new ArrayList<>();
            for (String file : argMap.get("counts").split(","))
                for (String line : Files.readAllLines(Path.of(file)))
                    if (!line.isBlank())
                        decisionCounts.add(Integer.parseInt(line.trim()));
            writeTable(out, decisionCounts);
            return;
        }
        GameType gameType = GameType.valueOf(argMap.get("game"));
        String playerFile = argMap.get("player");
        int nGames = Integer.parseInt(argMap.getOrDefault("games", "1000"));
        int nPlayers = Integer.parseInt(argMap.getOrDefault("nPlayers", "2"));
        long seed = Long.parseLong(argMap.getOrDefault("seed", "1"));

        List<Integer> decisionCounts = new ArrayList<>();
        for (int g = 0; g < nGames; g++) {
            List<AbstractPlayer> players = new ArrayList<>();
            for (int p = 0; p < nPlayers; p++)
                players.add(PlayerFactory.createPlayer(playerFile));
            Game game = gameType.createGameInstance(nPlayers, seed + g);
            game.reset(players);
            int[] counts = new int[nPlayers];
            game.addListener(new DecisionCounter(counts));
            game.run();
            for (int c : counts)
                decisionCounts.add(c);
            if ((g + 1) % 50 == 0)
                System.out.printf("%d games, mean decisions per player %.2f%n", g + 1,
                        decisionCounts.stream().mapToInt(i -> i).average().orElse(0));
        }
        Files.writeString(Path.of(out + ".counts"),
                decisionCounts.stream().map(String::valueOf).collect(Collectors.joining("\n")) + "\n");
        writeTable(out, decisionCounts);
    }

    private static void writeTable(Path out, List<Integer> decisionCounts) throws IOException {
        double[] table = table(decisionCounts);
        String content = Arrays.stream(table).mapToObj(v -> String.format("%.3f", v)).collect(Collectors.joining("\n"));
        Files.writeString(out, content + "\n");
    }

    /**
     * @param decisionCounts - the number of decisions made by each player in each game
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

    private static class DecisionCounter implements IGameListener {
        private final int[] counts;
        private Game game;

        DecisionCounter(int[] counts) {
            this.counts = counts;
        }

        @Override
        public void onEvent(Event event) {
            if (event.type == Event.GameEvent.ACTION_CHOSEN && event.actions != null && event.actions.size() > 1)
                counts[event.playerID]++;
        }

        @Override
        public void report() {
        }

        @Override
        public void setGame(Game game) {
            this.game = game;
        }

        @Override
        public Game getGame() {
            return game;
        }
    }
}
