package evaluation.tournaments;

import core.AbstractGameState;
import core.AbstractParameters;
import core.AbstractPlayer;
import evaluation.RunArg;
import evaluation.listeners.IGameListener;
import evaluation.listeners.TournamentMetricsGameListener;
import games.GameType;
import players.IAnyTimePlayer;
import utilities.*;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static core.CoreConstants.GameResult;
import static evaluation.tournaments.AbstractTournament.TournamentMode.*;
import static java.util.stream.Collectors.toList;

public class RoundRobinTournament extends AbstractTournament {
    private static boolean debug = false;
    public TournamentMode tournamentMode;
    final int totalGameBudget;
    int gamesPerMatchup;
    protected List<IGameListener> listeners = new ArrayList<>();
    private boolean verbose;
    protected IResultsAnalysis winRateAnalysis = new WinRateAnalysis();
    protected IResultsAnalysis ordinalAnalysis = new OrdinalAnalysis();
    protected IResultsAnalysis alphaRankWinAnalysis = new AlphaRankAnalysis(false);
    protected IResultsAnalysis alphaRankOrdinalAnalysis = new AlphaRankAnalysis(true);
    protected LinkedHashMap<String, Pair<Double, Double>> finalWinRanking; // contains name of agent
    protected LinkedHashMap<String, Pair<Double, Double>> finalOrdinalRanking; // contains name of agent
    protected LinkedHashMap<String, Pair<Double, Double>> finalAlphaRankWinRanking;
    protected LinkedHashMap<String, Pair<Double, Double>> finalAlphaRankOrdinalRanking;
    LinkedList<Integer> allAgentIds;
    protected boolean randomGameParams;
    public String name;
    public boolean byTeam;
    protected String evalMethod;

    protected long randomSeed;
    List<Integer> gameSeeds = new ArrayList<>();
    int tournamentSeeds;
    String seedFile;
    Random seedRnd;
    Map<RunArg, Object> config;

    /**
     * Create a round robin tournament, which plays all agents against all others.
     *
     * @param agents         - players for the tournament.
     * @param gameToPlay     - game to play in this tournament.
     * @param playersPerGame - number of players per game.
     */
    public RoundRobinTournament(List<? extends AbstractPlayer> agents, GameType gameToPlay, int playersPerGame,
                                AbstractParameters gameParams, Map<RunArg, Object> config) {
        super(agents, gameToPlay, playersPerGame, gameParams);
        for (AbstractPlayer agent : this.agents) {
            String name = agent.toString();
            agent.setName(name);
        }
        this.config = config;
        int nTeams = game.getGameState().getNTeams();
        this.verbose = (boolean) config.getOrDefault(RunArg.verbose, false);
        this.tournamentMode = switch (config.get(RunArg.mode).toString().toUpperCase()) {
            case "EXHAUSTIVE" -> EXHAUSTIVE;
            case "EXHAUSTIVESP" -> EXHAUSTIVE_SELF_PLAY;
            case "ONEVSALL" -> ONE_VS_ALL;
            case "FIXED" -> FIXED;
            default -> RANDOM;
        };
        if (tournamentMode == EXHAUSTIVE && nTeams > this.agents.size()) {
            throw new IllegalArgumentException("Not enough agents to fill a match without self-play." +
                    "Either add more agents, reduce the number of players per game, or switch to RANDOM mode.");
        }
        if (tournamentMode == FIXED && this.agents.size() != playersPerGame) {
            throw new IllegalArgumentException("In FIXED mode, the number of agents must match the number of players per game.");
        }
        this.evalMethod = (String) config.getOrDefault(RunArg.evalMethod, "Win");

        this.allAgentIds = new LinkedList<>();
        for (int i = 0; i < this.agents.size(); i++)
            this.allAgentIds.add(i);

        this.totalGameBudget = (int) config.getOrDefault(RunArg.matchups, 100);
        int budget = (int) config.get(RunArg.budget);
        if (budget > 0) {
            // in this case we set the budget of the players
            for (AbstractPlayer player : agents) {
                if (player instanceof IAnyTimePlayer) {
                    ((IAnyTimePlayer) player).setBudget(budget);
                }
            }
        }
        this.byTeam = (boolean) config.getOrDefault(RunArg.byTeam, false);
        this.tournamentSeeds = (int) config.getOrDefault(RunArg.distinctRandomSeeds, 0);
        this.seedFile = (String) config.getOrDefault(RunArg.seedFile, "");
        if (!seedFile.isEmpty()) {
            this.gameSeeds = loadSeedsFromFile();
            if (gameSeeds.isEmpty()) {
                throw new AssertionError("No seeds found in file " + seedFile);
            }
            this.tournamentSeeds = gameSeeds.size();
        }
        int agentPositions = byTeam ? nTeams : playersPerGame;
        int actualGames = this.totalGameBudget;
        switch (tournamentMode) {
            case ONE_VS_ALL:
                gamesPerMatchup = totalGameBudget / agentPositions;
                actualGames = this.gamesPerMatchup * agentPositions;
                break;
            case EXHAUSTIVE_SELF_PLAY:
            case EXHAUSTIVE:
                boolean selfPlay = tournamentMode == EXHAUSTIVE_SELF_PLAY;
                this.gamesPerMatchup = Utils.gamesPerMatchup(agentPositions, agents.size(), totalGameBudget, selfPlay);
                if (this.gamesPerMatchup < 1) {
                    throw new IllegalArgumentException(String.format("Higher budget needed. There are %d permutations of agents to positions in exhaustive mode, which is more than %d game in the available budget.",
                            Utils.playerPermutations(agentPositions, agents.size(), selfPlay), totalGameBudget));
                }
                actualGames = this.gamesPerMatchup * Utils.playerPermutations(agentPositions, agents.size(), selfPlay);
                break;
            case FIXED:
                // we run the totalGameBudget number of games with no change to agent order
            case RANDOM:
                this.gamesPerMatchup = totalGameBudget; // not actually used, we just run the totalGameBudget number of games
                break;
            default:
                throw new IllegalArgumentException("Unknown tournament mode " + config.get(RunArg.mode));
        }
        this.randomSeed = ((Number) config.getOrDefault(RunArg.seed, System.currentTimeMillis())).longValue();
        this.seedRnd = new Random(randomSeed);
        this.randomGameParams = (boolean) config.getOrDefault(RunArg.randomGameParams, false);

        this.name = String.format("Game: %s, Players: %d, Mode: %s, TotalGames: %d, GamesPerMatchup: %d",
                gameToPlay.name(), playersPerGame, tournamentMode, actualGames, gamesPerMatchup);
        System.out.println(name);
        String destDir = (String) config.getOrDefault(RunArg.destDir, "");
        if (!destDir.isEmpty())
            this.resultsFile = destDir + File.separator + resultsFile;
    }

    /**
     * Runs the round robin tournament.
     */
    @Override
    public void run() {
        if (verbose)
            System.out.println("Playing " + game.getGameType().name());

        Set<String> agentNames = agents.stream()
                //           .peek(a -> System.out.println(a.toString()))
                .map(AbstractPlayer::toString).collect(Collectors.toSet());

        for (IGameListener gameTracker : listeners) {
            gameTracker.init(game, nPlayers, agentNames);
            game.addListener(gameTracker);
        }

        LinkedList<Integer> matchUp = new LinkedList<>();
        // add outer loop if we have tournamentSeeds enabled; if not this will just run once
        List<Integer> allSeeds = new ArrayList<>(gameSeeds);
        for (int iter = 0; iter < Math.max(1, tournamentSeeds); iter++) {
            if (tournamentSeeds > 0) {
                // use the same seed for each game in the tournament
                // allSeeds contains the ones loaded from file - if empty then use a random one
                int nextRnd = allSeeds.isEmpty() ? seedRnd.nextInt() : allSeeds.get(iter);
                gameSeeds = IntStream.range(0, gamesPerMatchup).mapToObj(i -> nextRnd).collect(toList());
            } else {
                // use a seed per matchup
                gameSeeds = IntStream.range(0, gamesPerMatchup).mapToObj(i -> seedRnd.nextInt()).collect(toList());
            }
            createAndRunMatchUp(matchUp);
        }
        reportResults();

        for (IGameListener listener : listeners)
            listener.report();
    }

    protected List<Integer> loadSeedsFromFile() {
        // we open seedFile, and read in the comma-delimited list of seeds, and put this in an array
        try {
            Scanner scanner = new Scanner(new File(seedFile)).useDelimiter("\\s*,\\s*");
            List<Integer> seeds = new ArrayList<>();
            while (scanner.hasNextInt()) {
                seeds.add(scanner.nextInt());
            }
            return new ArrayList<>(seeds);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not load seeds from file " + seedFile);
        }

    }

    public int getWinnerIndex() {
        LinkedHashMap<String, Pair<Double, Double>> ranking = switch (evalMethod) {
            case "Ordinal", "Score" -> finalOrdinalRanking;
            default -> finalWinRanking;
        };
        if (ranking == null || ranking.isEmpty())
            throw new UnsupportedOperationException("Cannot get winner before results have been calculated");

        // The winner is the first key in finalRanking
        String winnerName = ranking.keySet().iterator().next();
        for (int i = 0; i < agents.size(); i++) {
            if (agents.get(i).toString().equals(winnerName)) return i;
        }
        return -1;
    }

    public AbstractPlayer getWinner() {
        return agents.get(getWinnerIndex());
    }

    /**
     * Recursively creates one combination of players and evaluates it.
     *
     * @param matchUp - current combination of players, updated recursively.
     */
    public void createAndRunMatchUp(List<Integer> matchUp) {

        int nTeams = byTeam ? game.getGameState().getNTeams() : nPlayers;
        if (gameSeeds == null || gameSeeds.isEmpty()) {
            gameSeeds = IntStream.range(0, gamesPerMatchup).mapToObj(i -> seedRnd.nextInt()).collect(toList());
        }
        switch (tournamentMode) {
            case FIXED:
                // we add the agents to the matchUp in the order they are in the list
                // we always run the same fixed set of agents, so ignore the input matchup
                matchUp.clear();
                for (int i = 0; i < agents.size(); i++) {
                    matchUp.add(i);
                }
                evaluateMatchUp(matchUp, gamesPerMatchup, gameSeeds);
                break;
            case RANDOM:
                // In the RANDOM case we use a new seed for each game
                PermutationCycler idStream = new PermutationCycler(agents.size(), seedRnd, nTeams);
                for (int i = 0; i < totalGameBudget; i++) {
                    // System.out.println("Playing game " + (i+1) + " out of " + totalGameBudget);
                    List<Integer> matchup = new ArrayList<>(nTeams);
                    for (int j = 0; j < nTeams; j++)
                        matchup.add(idStream.getAsInt());
                    evaluateMatchUp(matchup, 1, Collections.singletonList(gameSeeds.get(i)));
                }
                break;
            case ONE_VS_ALL:
                // In this case agents.get(0) must always play
                List<Integer> agentOrder = new ArrayList<>(this.allAgentIds);
                agentOrder.remove(Integer.valueOf(0));
                for (int p = 0; p < nTeams; p++) {
                    // we put the focus player at each position (p) in turn
                    if (agentOrder.size() == 1) {
                        // to reduce variance in this case we can use the same set of seeds for each case
                        List<Integer> matchup = new ArrayList<>(nTeams);
                        for (int j = 0; j < nTeams; j++) {
                            if (j == p)
                                matchup.add(0); // focus player
                            else {
                                matchup.add(agentOrder.get(0));
                            }
                        }
                        // We split the total budget equally across the possible positions the focus player can be in
                        // We will therefore use the first chunk of gameSeeds only (but use the same gameSeeds for each position)
                        evaluateMatchUp(matchup, totalGameBudget / nTeams, gameSeeds);
                    } else {
                        for (int m = 0; m < this.totalGameBudget / nTeams; m++) {
                            Collections.shuffle(agentOrder, seedRnd);
                            List<Integer> matchup = new ArrayList<>(nTeams);
                            for (int j = 0; j < nTeams; j++) {
                                if (j == p)
                                    matchup.add(0); // focus player
                                else {
                                    matchup.add(agentOrder.get(j % agentOrder.size()));
                                }
                            }
                            evaluateMatchUp(matchup, 1, Collections.singletonList(gameSeeds.get(m)));
                        }
                    }
                }
                break;
            case EXHAUSTIVE:
            case EXHAUSTIVE_SELF_PLAY:
                // in this case we are in exhaustive mode, so we recursively construct all possible combinations of players
                if (matchUp.size() == nTeams) {
                    evaluateMatchUp(matchUp, gamesPerMatchup, gameSeeds);
                } else {
                    for (Integer agentID : this.allAgentIds) {
                        if (tournamentMode == EXHAUSTIVE_SELF_PLAY || !matchUp.contains(agentID)) {
                            matchUp.add(agentID);
                            createAndRunMatchUp(matchUp);
                            matchUp.remove(agentID);
                        }
                    }
                }
        }
    }

    /**
     * Evaluates one combination of players.
     *
     * @param agentIDsInThisGame - IDs of agents participating in this run.
     */
    protected void evaluateMatchUp(List<Integer> agentIDsInThisGame, int nGames, List<Integer> seeds) {
        if (seeds.size() < nGames)
            throw new AssertionError("Not enough seeds for the number of games requested");
        if (debug)
            System.out.printf("Evaluate %s at %tT%n", agentIDsInThisGame.toString(), System.currentTimeMillis());
        LinkedList<AbstractPlayer> matchUpPlayers = new LinkedList<>();

        // create a copy of the player to avoid them sharing the same state
        for (int agentID : agentIDsInThisGame)
            matchUpPlayers.add(this.agents.get(agentID).copy());

        if (verbose) {
            StringBuffer sb = new StringBuffer();
            sb.append("[");
            for (int agentID : agentIDsInThisGame)
                sb.append(this.agents.get(agentID).toString()).append(",");
            sb.setCharAt(sb.length() - 1, ']');
            System.out.println(sb);
        }

        // TODO : Not sure this is the ideal place for this...ask Raluca
        Set<String> agentNames = agents.stream().map(AbstractPlayer::toString).collect(Collectors.toSet());
        for (IGameListener listener : listeners) {
            if (listener instanceof TournamentMetricsGameListener) {
                ((TournamentMetricsGameListener) listener).tournamentInit(game, nPlayers, agentNames, new HashSet<>(matchUpPlayers));
            }
        }

        // Run the game N = gamesPerMatchUp times with these players
        for (int i = 0; i < nGames; i++) {
            // if tournamentSeeds > 0, then we are running this many tournaments, each with a different random seed fixed for the whole tournament
            // so we override the standard random seeds
            game.reset(matchUpPlayers, seeds.get(i));

            // if we are starting from a specific state, load it and reset the game
            if (!((String) config.getOrDefault(RunArg.gameState, "")).isEmpty()) {
                AbstractGameState startState = JSONUtils.loadClassFromFile((String) config.get(RunArg.gameState));
                game.reset(startState);
            }

            // Randomize parameters
            if (randomGameParams) {
                game.getGameState().getGameParameters().randomize();
                System.out.println("Game parameters: " + game.getGameState().getGameParameters());
            }

            game.run();  // Always running tournaments without visuals
            GameResult[] results = game.getGameState().getPlayerResults();

            tournamentResults.record(game);

            if (verbose) {
                StringBuffer sb = new StringBuffer();
                sb.append("[");
                for (int j = 0; j < matchUpPlayers.size(); j++) {
                    for (int player = 0; player < game.getGameState().getNPlayers(); player++) {
                        if (game.getGameState().getTeam(player) == j) {
                            sb.append(results[player]).append(",");
                            break; // we stop after one player on the team to avoid double counting
                        }
                    }
                }
                sb.setCharAt(sb.length() - 1, ']');
                System.out.println(sb);
            }

        }
    }

    protected void calculateFinalResults() {
        finalWinRanking = winRateAnalysis.getRanking(tournamentResults);
        finalOrdinalRanking = ordinalAnalysis.getRanking(tournamentResults);
        if (agents.size() > game.getGameState().getNPlayers()) {
            // We only calculate alpha-rank if we have more agents than players
            // otherwise the Transition matrix is singular
            finalAlphaRankWinRanking = alphaRankWinAnalysis.getRanking(tournamentResults);
            finalAlphaRankOrdinalRanking = alphaRankOrdinalAnalysis.getRanking(tournamentResults);
        }
    }

    protected void reportResults() {
        calculateFinalResults();
        boolean toFile = resultsFile != null && !resultsFile.isEmpty();
        List<String> dataDump = new ArrayList<>();
        dataDump.add(name + "\n");

        // To console
        int totalGamesRun = tournamentResults.totalResults() / game.getGameState().getNPlayers();
        if (verbose)
            System.out.printf("============= %s - %d games played ============= \n", game.getGameType().name(), totalGamesRun);
        for (int i = 0; i < this.agents.size(); i++) {
            String name = agents.get(i).toString();
            List<TournamentResults.Result> agentResults = tournamentResults.getPlayerResults(name);
            double totalPoints = agentResults.stream().mapToDouble(r -> r.points).sum();
            int totalWins = agentResults.stream().mapToInt(r -> r.win).sum();
            int nGames = agentResults.size();
            double totalScore = agentResults.stream().mapToDouble(r -> r.score).sum();

            String str = String.format("%s got %.2f points. ", name, totalPoints);
            if (toFile) dataDump.add(str);
            if (verbose) System.out.print(str);

            str = String.format("%s won %.1f%% of the %d games of the tournament. ",
                    name, 100.0 * totalWins / totalGamesRun, totalGamesRun);
            if (toFile) dataDump.add(str);
            if (verbose) System.out.print(str);

            str = String.format("%s won %.1f%% of the %d games it played during the tournament.\n",
                    name, 100.0 * totalWins / nGames, nGames);
            if (toFile) dataDump.add(str);
            if (verbose) System.out.print(str);
            str = String.format("%s got a mean score of %.2f.\n", name, totalScore / nGames);
            if (toFile) dataDump.add(str);
            if (verbose) System.out.print(str);

            for (int j = 0; j < this.agents.size(); j++) {
                if (i != j) {
                    int gamesPlayed = tournamentResults.getGamesPlayed(agents.get(i).toString(), agents.get(j).toString());
                    str = String.format("%s won %.1f%% of the %d games against %s.\n",
                            agents.get(i), 100.0 * tournamentResults.getWins(agents.get(i).toString(), agents.get(j).toString()) / gamesPlayed, gamesPlayed, agents.get(j));
                    if (toFile) dataDump.add(str);
                    if (verbose) System.out.print(str);
                }
            }

            if (toFile) dataDump.add("\n");
            if (verbose) System.out.println();
        }

        String str = "---- Ranking ---- (+/- are standard errors on the mean calculated using a Normal approximation) \n";
        if (toFile) dataDump.add(str);
        if (verbose) System.out.print(str);

        for (String agentName : finalWinRanking.keySet()) {
            str = String.format("%s: Win rate %.2f +/- %.3f\tMean Ordinal %.2f +/- %.2f\n",
                    agentName,
                    finalWinRanking.get(agentName).a, finalWinRanking.get(agentName).b,
                    finalOrdinalRanking.get(agentName).a, finalOrdinalRanking.get(agentName).b);
            if (toFile) dataDump.add(str);
            if (verbose) System.out.print(str);
        }
        str = "\n\n---- Alpha Rank ----  \n";
        if (toFile) dataDump.add(str);
        if (verbose) System.out.print(str);

        if (finalAlphaRankWinRanking != null && !finalAlphaRankWinRanking.isEmpty()) {
            for (String agentNames : finalAlphaRankWinRanking.keySet()) {
                str = String.format("%s: Alpha-rank by Win %.2f\tAlpha-rank by Ordinal %.2f\n",
                        agentNames,
                        finalAlphaRankWinRanking.get(agentNames).a, finalAlphaRankOrdinalRanking.get(agentNames).a);
                if (toFile) dataDump.add(str);
                if (verbose) System.out.print(str);
            }
        }

        // To file
        if (toFile) {
            try {
                File resultsFile = new File(this.resultsFile);
                if (!resultsFile.exists()) {
                    File dir = resultsFile.getParentFile();
                    if (dir != null && !dir.exists())
                        dir.mkdirs();
                }
                FileWriter writer = new FileWriter(resultsFile, true);
                for (String line : dataDump)
                    writer.write(line);
                writer.write("\n");
                writer.close();
            } catch (Exception e) {
                System.out.println("Unable to write results to " + resultsFile);
                resultsFile = null;
            }
        }
    }


    public double getWinRate(int agentID) {
        String name = agents.get(agentID).toString();
        return finalWinRanking.get(name) == null ? 0.0 : finalWinRanking.get(name).a;
    }

    public double getWinStdErr(int agentID) {
        String name = agents.get(agentID).toString();
        return finalWinRanking.get(name) == null ? 0.0 : finalWinRanking.get(name).b;
    }

    public double getSumOfSquares(int agentID, String type) {
        String name = agents.get(agentID).toString();
        List<TournamentResults.Result> agentResults = tournamentResults.getPlayerResults(name);
        if (type.equals("Win")) {
            return agentResults.stream().mapToDouble(r -> r.points * r.points).sum();
        } else {
            return agentResults.stream().mapToDouble(r -> (double) r.ordinal * r.ordinal).sum();
        }
    }

    public double getOrdinalRank(int agentID) {
        String name = agents.get(agentID).toString();
        return finalOrdinalRanking.get(name).a;
    }

    public double getOrdinalStdErr(int agentID) {
        String name = agents.get(agentID).toString();
        return finalOrdinalRanking.get(name).b;
    }

    public void addListener(IGameListener gameTracker) {
        listeners.add(gameTracker);
    }

    public int[] getNGamesPlayed() {
        int[] retValue = new int[agents.size()];
        for (int i = 0; i < agents.size(); i++) {
            retValue[i] = tournamentResults.getPlayerResults(agents.get(i).toString()).size();
        }
        return retValue;
    }
}
