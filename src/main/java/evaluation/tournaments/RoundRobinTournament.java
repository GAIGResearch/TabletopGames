package evaluation.tournaments;

import core.AbstractParameters;
import core.AbstractPlayer;
import evaluation.listeners.IGameListener;
import games.GameType;
import utilities.Pair;

import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

import static core.CoreConstants.GameResult;
import static evaluation.tournaments.AbstractTournament.TournamentMode.*;

public class RoundRobinTournament extends AbstractTournament {
    private static final boolean debug = false;

    // Configuration
    private final int gamesPerMatchUp;
    protected List<IGameListener> listeners = new ArrayList<>();
    public boolean verbose = true;
    double[] pointsPerPlayer;
    double[] winsPerPlayer;
    double[][] winsPerPlayerPerOpponent;
    int[] nGamesPlayed;
    int[][] nGamesPlayedPerOpponent;
    protected final String destDir, finalDir;
    double[] pointsPerPlayerSquared;
    double[] rankPerPlayer;
    double[] rankPerPlayerSquared;
    protected LinkedHashMap<Integer, Pair<Double, Double>> finalWinRanking; // contains index of agent in agents
    protected LinkedHashMap<Integer, Pair<Double, Double>> finalOrdinalRanking; // contains index of agent in agents
    LinkedList<Integer> agentIDs;
    private int matchUpsRun;
    protected boolean randomGameParams;
    public final String name;

    private boolean listenersInitialized = false;

    /**
     * Create a round robin tournament, which plays all agents against all others.
     *
     * @param agents          - players for the tournament.
     * @param gameToPlay      - game to play in this tournament.
     * @param playersPerGame  - number of players per game.
     * @param gamesPerMatchUp - number of games for each combination of players.
     * @param mode            - SELF_PLAY, NO_SELF_PLAY, or ONE_VS_ALL
     */
    public RoundRobinTournament(List<? extends AbstractPlayer> agents, GameType gameToPlay, int playersPerGame,
                                int gamesPerMatchUp, TournamentMode mode, AbstractParameters gameParams,
                                String finalDir, String destDir) {
        super(mode, agents, gameToPlay, playersPerGame, gameParams);
        if (mode == NO_SELF_PLAY && playersPerGame > this.agents.size()) {
            throw new IllegalArgumentException("Not enough agents to fill a match without self-play." +
                    "Either add more agents, reduce the number of players per game, or allow self-play.");
        }
        this.agentIDs = new LinkedList<>();
        for (int i = 0; i < this.agents.size(); i++)
            this.agentIDs.add(i);

        this.gamesPerMatchUp = gamesPerMatchUp;
        this.pointsPerPlayer = new double[agents.size()];
        this.pointsPerPlayerSquared = new double[agents.size()];
        this.winsPerPlayer = new double[agents.size()];
        this.nGamesPlayed = new int[agents.size()];
        this.nGamesPlayedPerOpponent = new int[agents.size()][];
        this.winsPerPlayerPerOpponent = new double[agents.size()][];
        for (int i = 0; i < agents.size(); i++) {
            this.winsPerPlayerPerOpponent[i] = new double[agents.size()];
            this.nGamesPlayedPerOpponent[i] = new int[agents.size()];
        }
        this.rankPerPlayer = new double[agents.size()];
        this.rankPerPlayerSquared = new double[agents.size()];

        this.destDir = destDir;
        this.finalDir = finalDir;

        this.name = String.format("Game: %s, Players: %d, GamesPerMatchup: %d, Mode: %s", gameToPlay.name(), playersPerGame, gamesPerMatchUp, mode.name());
    }

    /**
     * Runs the round robin tournament.
     */
    @Override
    public void run() {
        if (verbose)
            System.out.println("Playing " + game.getGameType().name());
        LinkedList<Integer> matchUp = new LinkedList<>();
        createAndRunMatchUp(matchUp);
        reportResults();

        for (IGameListener listener : listeners)
            listener.report();
    }


    public int getWinnerIndex() {
        if (finalWinRanking == null || finalWinRanking.isEmpty())
            throw new UnsupportedOperationException("Cannot get winner before results have been calculated");

        // The winner is the first key in finalRanking
        for (Integer key : finalWinRanking.keySet()) {
            return key;
        }
        throw new AssertionError("Should not be reachable");
    }

    public AbstractPlayer getWinner() {
        return agents.get(getWinnerIndex());
    }

    /**
     * Recursively creates one combination of players and evaluates it.
     *
     * @param matchUp - current combination of players, updated recursively.
     */
    public void createAndRunMatchUp(LinkedList<Integer> matchUp) {
        if (tournamentMode == ONE_VS_ALL) {
            // In this case agents.get(0) must always play
            Random rnd = new Random(System.currentTimeMillis());
            int nPlayers = this.nPlayers;
            List<Integer> randomAgentOrder = new ArrayList<>(this.agentIDs);
            randomAgentOrder.remove(Integer.valueOf(0));
            for (int p = 0; p < nPlayers; p++) {
                // we put the focus player at each position (p) in turn
                for (int m = 0; m < this.gamesPerMatchUp; m++) {
                    Collections.shuffle(randomAgentOrder, rnd);
                    List<Integer> matchup = new ArrayList<>(nPlayers);
                    for (int j = 0; j < nPlayers; j++) {
                        if (j == p)
                            matchup.add(0); // focus player
                        else {
                            matchup.add(randomAgentOrder.get(j % randomAgentOrder.size()));
                        }
                    }
                    evaluateMatchUp(matchup, 1);
                }
            }
        } else {
            // in this case we are in exhaustive mode, so we recursively construct all possible combinations of players
            if (matchUp.size() == nPlayers) {
                evaluateMatchUp(matchUp);
            } else {
                for (Integer agentID : this.agentIDs) {
                    if (tournamentMode == SELF_PLAY || !matchUp.contains(agentID)) {
                        matchUp.add(agentID);
                        createAndRunMatchUp(matchUp);
                        matchUp.remove(agentID);
                    }
                }
            }
        }
    }

    protected void evaluateMatchUp(List<Integer> agentIDs) {
        evaluateMatchUp(agentIDs, gamesPerMatchUp);
    }

    /**
     * Evaluates one combination of players.
     *
     * @param agentIDs - IDs of agents participating in this run.
     */
    protected void evaluateMatchUp(List<Integer> agentIDs, int nGames) {
        if (debug)
            System.out.printf("Evaluate %s at %tT%n", agentIDs.toString(), System.currentTimeMillis());
        LinkedList<AbstractPlayer> matchUpPlayers = new LinkedList<>();

        for (int agentID : agentIDs)
            matchUpPlayers.add(this.agents.get(agentID));

        if (verbose) {
            StringBuffer sb = new StringBuffer();
            sb.append("[");
            for (int agentID : agentIDs)
                sb.append(this.agents.get(agentID).toString()).append(",");
            sb.setCharAt(sb.length() - 1, ']');
            System.out.println(sb);
        }

        Set<String> agentNames = new HashSet<>();
        for (AbstractPlayer agent : agents)
            agentNames.add(agent.toString());
        for (IGameListener listener : listeners) {
            game.addListener(listener);
            listener.tournamentInit(game, nPlayers, agentNames, new HashSet<>(matchUpPlayers));
        }


        // Run the game N = gamesPerMatchUp times with these players
        long currentSeed = game.getGameState().getGameParameters().getRandomSeed();
        for (int i = 0; i < nGames; i++) {

            if (randomGameParams)
                game.getGameState().getGameParameters().randomize();

            game.reset(matchUpPlayers, currentSeed + i + 1);
            if (!listenersInitialized) {
                for (IGameListener gameTracker : listeners) {
                    gameTracker.init(game, nPlayers, agentNames);
                }
                listenersInitialized = true;
            }

            // Randomize parameters
            if (randomGameParams) {
                game.getGameState().getGameParameters().randomize();
                System.out.println("Game parameters: " + game.getGameState().getGameParameters());
            }

            game.run();  // Always running tournaments without visuals
            GameResult[] results = game.getGameState().getPlayerResults();

            int numDraws = 0;
            for (int j = 0; j < matchUpPlayers.size(); j++) {
                nGamesPlayed[agentIDs.get(j)] += 1;
                for (int k = 0; k < matchUpPlayers.size(); k++) {
                    if (k != j) {
                        nGamesPlayedPerOpponent[agentIDs.get(j)][agentIDs.get(k)] += 1;
                    }
                }

                int ordinalPos = game.getGameState().getOrdinalPosition(j);
                rankPerPlayer[agentIDs.get(j)] += ordinalPos;
                rankPerPlayerSquared[agentIDs.get(j)] += ordinalPos * ordinalPos;
                if (results[j] == GameResult.WIN_GAME) {
                    pointsPerPlayer[agentIDs.get(j)] += 1;
                    winsPerPlayer[agentIDs.get(j)] += 1;
                    pointsPerPlayerSquared[agentIDs.get(j)] += 1;
                    for (int k = 0; k < matchUpPlayers.size(); k++) {
                        if (k != j) {
                            winsPerPlayerPerOpponent[agentIDs.get(j)][agentIDs.get(k)] += 1;
                        }
                    }
                }
                if (results[j] == GameResult.DRAW_GAME)
                    numDraws++;
            }

            if (numDraws > 0) {
                double pointsPerDraw = 1.0 / numDraws;
                for (int j = 0; j < matchUpPlayers.size(); j++) {
                    if (results[j] == GameResult.DRAW_GAME) pointsPerPlayer[agentIDs.get(j)] += pointsPerDraw;
                    if (results[j] == GameResult.DRAW_GAME)
                        pointsPerPlayerSquared[agentIDs.get(j)] += pointsPerDraw * pointsPerDraw;
                }
            }

            if (verbose) {
                StringBuffer sb = new StringBuffer();
                sb.append("[");
                for (int j = 0; j < matchUpPlayers.size(); j++)
                    sb.append(results[j]).append(",");
                sb.setCharAt(sb.length() - 1, ']');
                System.out.println(sb);
            }

        }
        game.clearListeners();
        matchUpsRun++;
    }


    protected void calculateFinalResults() {
        finalWinRanking = new LinkedHashMap<>();
        finalOrdinalRanking = new LinkedHashMap<>();
        for (int i = 0; i < this.agents.size(); i++) {
            // We calculate the standard deviation, and hence the standard error on the mean value
            // (using a normal approximation, which is valid for large N)
            double stdDev = pointsPerPlayerSquared[i] / nGamesPlayed[i] - (pointsPerPlayer[i] / nGamesPlayed[i])
                    * (pointsPerPlayer[i] / nGamesPlayed[i]);
            finalWinRanking.put(i, new Pair<>(pointsPerPlayer[i] / nGamesPlayed[i], stdDev / Math.sqrt(nGamesPlayed[i])));
            stdDev = rankPerPlayerSquared[i] / nGamesPlayed[i] - (rankPerPlayer[i] / nGamesPlayed[i]) * (rankPerPlayer[i] / nGamesPlayed[i]);
            finalOrdinalRanking.put(i, new Pair<>(rankPerPlayer[i] / nGamesPlayed[i], stdDev / Math.sqrt(nGamesPlayed[i])));
        }
        // Sort by points.
        finalWinRanking = finalWinRanking.entrySet().stream()
                .sorted(Map.Entry.comparingByValue((o1, o2) -> o2.a.compareTo(o1.a)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1,
                        LinkedHashMap::new));
        finalOrdinalRanking = finalOrdinalRanking.entrySet().stream()
                .sorted(Map.Entry.comparingByValue((o1, o2) -> o2.a.compareTo(o1.a)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1,
                        LinkedHashMap::new));
    }

    protected void reportResults() {
        calculateFinalResults();
        int gameCounter = (gamesPerMatchUp * matchUpsRun);
        boolean toFile = resultsFile != null && !resultsFile.equals("");
        ArrayList<String> dataDump = new ArrayList<>();
        dataDump.add(name + "\n");

        // To console
        if (verbose)
            System.out.printf("============= %s - %d games played ============= \n", game.getGameType().name(), gameCounter);
        for (int i = 0; i < this.agents.size(); i++) {
            String str = String.format("%s got %.2f points. ", agents.get(i), pointsPerPlayer[i]);
            if (toFile) dataDump.add(str);
            if (verbose) System.out.print(str);

            str = String.format("%s won %.1f%% of the %d games of the tournament. ",
                    agents.get(i), 100.0 * winsPerPlayer[i] / gameCounter, gameCounter);
            if (toFile) dataDump.add(str);
            if (verbose) System.out.print(str);

            str = String.format("%s won %.1f%% of the %d games it played during the tournament.\n",
                    agents.get(i), 100.0 * winsPerPlayer[i] / nGamesPlayed[i], nGamesPlayed[i]);
            if (toFile) dataDump.add(str);
            if (verbose) System.out.print(str);

            for (int j = 0; j < this.agents.size(); j++) {
                if (i != j) {
                    str = String.format("%s won %.1f%% of the %d games against %s.\n",
                            agents.get(i), 100.0 * winsPerPlayerPerOpponent[i][j] / nGamesPlayedPerOpponent[i][j], nGamesPlayedPerOpponent[i][j], agents.get(j));
                    if (toFile) dataDump.add(str);
                    if (verbose) System.out.print(str);
                }
            }

            if (toFile) dataDump.add("\n");
            if (verbose) System.out.println();
        }

        String str = "---- Ranking ---- \n";
        if (toFile) dataDump.add(str);
        if (verbose) System.out.print(str);

        for (Integer i : finalWinRanking.keySet()) {
            str = String.format("%s: Win rate %.2f +/- %.2f\tMean Ordinal %.2f +/- %.2f\n",
                    agents.get(i).toString(),
                    finalWinRanking.get(i).a, finalWinRanking.get(i).b,
                    finalOrdinalRanking.get(i).a, finalOrdinalRanking.get(i).b);
            if (toFile) dataDump.add(str);
            if (verbose) System.out.print(str);
        }

        // To file
        if (toFile) {
            try {
                FileWriter writer = new FileWriter(resultsFile, true);
                for (String line : dataDump)
                    writer.write(line);
                writer.write("\n");
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public double getWinRate(int agentID) {
        return finalWinRanking.get(agentID).a;
    }

    public double getWinStdErr(int agentID) {
        return finalWinRanking.get(agentID).b;
    }

    public double getOrdinalRank(int agentID) {
        return finalOrdinalRanking.get(agentID).a;
    }

    public double getOrdinalStdErr(int agentID) {
        return finalOrdinalRanking.get(agentID).b;
    }

    public List<IGameListener> getListeners() {
        return listeners;
    }

    public void setListeners(List<IGameListener> listeners) {
        this.listeners = listeners;
    }

    public void addListener(IGameListener gameTracker) {
        listeners.add(gameTracker);
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setRandomGameParams(boolean randomGameParams) {
        this.randomGameParams = randomGameParams;
    }
    public void setResultsFile(String resultsFile) {
        this.resultsFile = resultsFile;
    }
}
