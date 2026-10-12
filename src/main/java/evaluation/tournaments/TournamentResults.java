package evaluation.tournaments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.AbstractPlayer;
import core.Game;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.*;

/**
 * Class to store the core results of a tournament.
 * This includes:
 * Agent_Name1, Agent_Name2, NumberOfGames, VictoriesOfPlayer1, OrdinalDeltaOf1Over2
 * Map from AgentName to the AbstractPlayer
 */
public class TournamentResults {

    public static class Result {
        public final double points;
        public final int ordinal;
        public final double score;
        public final int win; // 1 for win, 0 otherwise

        @JsonCreator
        public Result(@JsonProperty("points") double points,
                      @JsonProperty("ordinal") int ordinal,
                      @JsonProperty("score") double score,
                      @JsonProperty("win") int win) {
            this.points = points;
            this.ordinal = ordinal;
            this.score = score;
            this.win = win;
        }
    }

    /**
     * One record per game, used by analyses that need the seed and seating (e.g. SeedClusteredAnalysis).
     * All arrays are indexed by player id within the game.
     */
    public static class GameRecord {
        public final long seed;
        public final String[] agents;
        public final double[] points;
        public final int[] ordinals;

        @JsonCreator
        public GameRecord(@JsonProperty("seed") long seed,
                          @JsonProperty("agents") String[] agents,
                          @JsonProperty("points") double[] points,
                          @JsonProperty("ordinals") int[] ordinals) {
            this.seed = seed;
            this.agents = agents;
            this.points = points;
            this.ordinals = ordinals;
            this.seating = String.join("|", agents);
        }

        @JsonIgnore
        private final String seating;

        // Agent names by player id; this identifies the seating within a tournament
        public String seating() {
            return seating;
        }
    }

    @JsonIgnore
    Map<String, AbstractPlayer> agentsByName = new HashMap<>();
    Map<String, Map<String, Integer>> nGamesPlayedPerOpponent = new HashMap<>();
    Map<String, Map<String, Integer>> winsPerPlayerPerOpponent = new HashMap<>();
    Map<String, Map<String, Integer>> ordinalDeltaPerOpponent = new HashMap<>();
    Map<String, List<Result>> playerResults = new HashMap<>();
    List<GameRecord> gameRecords = new ArrayList<>();

    public void record(Game game) {
        record(game, game.getGameState().getGameParameters().getRandomSeed());
    }

    /**
     * Records the result of a finished game.
     *
     * @param seed - the seed the game was reset with (needed for seed-clustered analysis)
     */
    public void record(Game game, long seed) {
        if (game.getGameState().isNotTerminal())
            throw new IllegalArgumentException("Game has not finished yet");

        int nPlayers = game.getPlayers().size();
        String[] names = new String[nPlayers];
        double[] allPoints = new double[nPlayers];
        int[] allOrdinals = new int[nPlayers];
        int winningPlayerCount = game.getGameState().getTied().size() + game.getGameState().getWinners().size();
        for (int j = 0; j < nPlayers; j++) {
            // Firstly we record the raw result for each player
            boolean playerIsWinner = game.getGameState().getOrdinalPosition(j) == 1;
            names[j] = game.getPlayers().get(j).toString();
            allPoints[j] = playerIsWinner ? (1.0 / winningPlayerCount) : 0.0;
            allOrdinals[j] = game.getGameState().getOrdinalPosition(j);
            addResult(
                    game.getPlayers().get(j),
                    allPoints[j],
                    allOrdinals[j],
                    game.getGameState().getGameScore(j),
                    playerIsWinner ? 1 : 0
            );

            // Then we record the 1:1 comparisons against all other agents in the game
            AbstractPlayer playerJ = game.getPlayers().get(j);
            for (int k = 0; k < game.getPlayers().size(); k++) {
                AbstractPlayer playerK = game.getPlayers().get(k);
                if (k != j && !playerJ.toString().equals(playerK.toString())) {
                    updateGamePlayed(playerJ, playerK);
                    updateOrdinalResults(playerJ, playerK,
                            game.getGameState().getOrdinalPosition(k) - game.getGameState().getOrdinalPosition(j));
                    // negative is good (we came lower than them in ordinal order)
                    if (playerIsWinner && game.getGameState().getOrdinalPosition(k) != game.getGameState().getOrdinalPosition(j)) {
                        updateWins(playerJ, playerK, 1);
                    }
                }
            }
        }
        gameRecords.add(new GameRecord(seed, names, allPoints, allOrdinals));
    }

    public List<GameRecord> getGameRecords() {
        return gameRecords;
    }

    public AbstractPlayer getAgent(String name) {
        return agentsByName.get(name);
    }

    public void registerAgent(AbstractPlayer newPlayer) {
        String name = newPlayer.toString();
        agentsByName.putIfAbsent(name, newPlayer);
    }


    @JsonIgnore
    public List<String> getAllAgentNames() {
        return playerResults.keySet().stream().toList();
    }

    public void addResult(AbstractPlayer player, double points, int ordinal, double score, int win) {
        String name = player.toString();
        agentsByName.putIfAbsent(name, player);
        playerResults.computeIfAbsent(name, k -> new ArrayList<>()).add(new Result(points, ordinal, score, win));
    }

    public List<String> getDominatedAgents() {
        List<String> dominatedAgents = new ArrayList<>();
        for (String agent : getAllAgentNames()) {
            boolean isDominated = true;
            for (String opponent : getAllAgentNames()) {
                if (!agent.equals(opponent) && getGamesPlayed(agent, opponent) > 0) {
                    int wins = getWins(agent, opponent);
                    int losses = getWins(opponent, agent);
                    if (wins > losses) {
                        isDominated = false;
                        break;
                    }
                }
            }
            if (isDominated) {
                dominatedAgents.add(agent);
            }
        }
        return dominatedAgents;
    }

    public int totalResults() {
        return playerResults.values().stream().mapToInt(List::size).sum();
    }

    public void filterPlayer(String name) {
        playerResults.remove(name);
        nGamesPlayedPerOpponent.remove(name);
        winsPerPlayerPerOpponent.remove(name);
        ordinalDeltaPerOpponent.remove(name);
        agentsByName.remove(name);
        gameRecords.removeIf(g -> Arrays.asList(g.agents).contains(name));

        // We then also remove the records of other players against this player
        for (String other : getAllAgentNames()) {
            nGamesPlayedPerOpponent.getOrDefault(other, Collections.emptyMap()).remove(name);
            winsPerPlayerPerOpponent.getOrDefault(other, Collections.emptyMap()).remove(name);
            ordinalDeltaPerOpponent.getOrDefault(other, Collections.emptyMap()).remove(name);
        }
    }

    public List<Result> getPlayerResults(String name) {
        return playerResults.getOrDefault(name, Collections.emptyList());
    }

    public List<Double> getPlayerPoints(String name) {
        return getPlayerResults(name).stream().map(r -> r.points).toList();
    }

    public List<Integer> getPlayerOrdinals(String name) {
        return getPlayerResults(name).stream().map(r -> r.ordinal).toList();
    }

    public List<Double> getPlayerScores(String name) {
        return getPlayerResults(name).stream().map(r -> r.score).toList();
    }

    public List<Integer> getPlayerWins(String name) {
        return getPlayerResults(name).stream().map(r -> r.win).toList();
    }

    public void updateGamePlayed(AbstractPlayer one, AbstractPlayer two) {
        increment(nGamesPlayedPerOpponent, one, two, 1);
    }

    public void updateOrdinalResults(AbstractPlayer one, AbstractPlayer two, int ordinalDelta) {
        increment(ordinalDeltaPerOpponent, one, two, ordinalDelta);
    }

    public void updateWins(AbstractPlayer one, AbstractPlayer two, int wins) {
        increment(winsPerPlayerPerOpponent, one, two, wins);
    }

    private void increment(Map<String, Map<String, Integer>> map, AbstractPlayer one, AbstractPlayer two, int delta) {
        String key1 = one.toString();
        String key2 = two.toString();
        agentsByName.putIfAbsent(key1, one);
        agentsByName.putIfAbsent(key2, two);
        map.computeIfAbsent(key1, k -> new HashMap<>())
                .merge(key2, delta, Integer::sum);
    }

    public int getGamesPlayed(String agentName1, String agentName2) {
        return mapGet(nGamesPlayedPerOpponent, agentName1, agentName2);
    }

    public int getWins(String winner, String loser) {
        return mapGet(winsPerPlayerPerOpponent, winner, loser);
    }
    public double getWinRate(String winner, String loser) {
        int wins = getWins(winner, loser);
        int gamesPlayed = getGamesPlayed(winner, loser);
        return gamesPlayed > 0 ? (double) wins / gamesPlayed : 0.0;
    }

    public int getOrdinalDelta(String agentName1, String agentName2) {
        return mapGet(ordinalDeltaPerOpponent, agentName1, agentName2);
    }

    private int mapGet(Map<String, Map<String, Integer>> map, String key1, String key2) {
        Map<String, Integer> subMap = map.get(key1);
        if (subMap != null) {
            return subMap.getOrDefault(key2, 0);
        }
        return 0;
    }

    public TournamentResults shallowCopy() {
        // the top level maps are copied, but the inner maps and lists are shared
        TournamentResults copy = new TournamentResults();
        copy.agentsByName = new HashMap<>(this.agentsByName);
        copy.nGamesPlayedPerOpponent = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : this.nGamesPlayedPerOpponent.entrySet()) {
            copy.nGamesPlayedPerOpponent.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        copy.winsPerPlayerPerOpponent = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : this.winsPerPlayerPerOpponent.entrySet()) {
            copy.winsPerPlayerPerOpponent.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        copy.ordinalDeltaPerOpponent = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : this.ordinalDeltaPerOpponent.entrySet()) {
            copy.ordinalDeltaPerOpponent.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        copy.playerResults = new HashMap<>();
        for (Map.Entry<String, List<Result>> entry : this.playerResults.entrySet()) {
            copy.playerResults.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        copy.gameRecords = new ArrayList<>(this.gameRecords);
        return copy;
    }

    // ------------------ Jackson (de)serialisation helpers ------------------

    /**
     * DTO used to safely serialise/deserialise the parts of TournamentResults that are JSON-friendly.
     * We intentionally exclude the transient agentsByName map (AbstractPlayer instances) from JSON.
     */
    public static class TournamentResultsDTO {
        public Map<String, Map<String, Integer>> nGamesPlayedPerOpponent;
        public Map<String, Map<String, Integer>> winsPerPlayerPerOpponent;
        public Map<String, Map<String, Integer>> ordinalDeltaPerOpponent;
        public Map<String, List<Result>> playerResults;
        public List<GameRecord> gameRecords;

        // no-arg constructor for Jackson
        public TournamentResultsDTO() {
        }
    }

    // Expose DTO for Jackson serialization
    @JsonValue
    public TournamentResultsDTO toDTO() {
        TournamentResultsDTO dto = new TournamentResultsDTO();
        dto.nGamesPlayedPerOpponent = this.nGamesPlayedPerOpponent;
        dto.winsPerPlayerPerOpponent = this.winsPerPlayerPerOpponent;
        dto.ordinalDeltaPerOpponent = this.ordinalDeltaPerOpponent;
        dto.playerResults = this.playerResults;
        dto.gameRecords = this.gameRecords;
        return dto;
    }

    @JsonCreator
    public static TournamentResults getTournamentResults(TournamentResultsDTO dto) {
        return extractFromDTO(dto);
    }

    private static TournamentResults extractFromDTO(TournamentResultsDTO dto) {
        TournamentResults tr = new TournamentResults();
        if (dto.nGamesPlayedPerOpponent != null) tr.nGamesPlayedPerOpponent = dto.nGamesPlayedPerOpponent;
        if (dto.winsPerPlayerPerOpponent != null) tr.winsPerPlayerPerOpponent = dto.winsPerPlayerPerOpponent;
        if (dto.ordinalDeltaPerOpponent != null) tr.ordinalDeltaPerOpponent = dto.ordinalDeltaPerOpponent;
        if (dto.playerResults != null) tr.playerResults = dto.playerResults;
        if (dto.gameRecords != null) tr.gameRecords = new ArrayList<>(dto.gameRecords);
        return tr;
    }

}
