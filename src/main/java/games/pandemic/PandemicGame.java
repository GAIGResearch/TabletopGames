package games.pandemic;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import evaluation.listeners.IGameListener;
import evaluation.listeners.MetricsGameListener;
import evaluation.loggers.SummaryLogger;
import evaluation.summarisers.TAGNumericStatSummary;
import games.GameType;
import games.pandemic.stats.PandemicMetrics;
import players.PlayerType;
import players.human.ActionController;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
public class PandemicGame extends Game {

    public PandemicGame(List<AbstractPlayer> agents, PandemicParameters params) {
        super(GameType.Pandemic, agents, new PandemicForwardModel(params, agents.size()),
                new PandemicGameState(params, agents.size()));
    }
    public PandemicGame(AbstractForwardModel model, AbstractGameState gameState) {
        super(GameType.Pandemic, model, gameState);
    }

    public static Game runCompetition(String parameterConfigFile, List<AbstractPlayer> players, long seed,
                                      boolean randomizeParameters, List<IGameListener> listeners, int nRepetitions, ActionController ac){
        boolean detailedStatistics = true;
        boolean printStatSummary = false;

        // Save win rate statistics over all games
        TAGNumericStatSummary statSummary = new TAGNumericStatSummary(""+seed);

        // Play n repetitions of this game and record player results
        Game game = null;
        int offset = 0;
        for (int i = 0; i < nRepetitions; i++) {
            Long s = seed;
            if (s == -1) s = System.currentTimeMillis();
            s += offset;
            game = runOne(GameType.Pandemic, parameterConfigFile, players, s, randomizeParameters, listeners, ac, 0);
            if (game != null) {
                statSummary.add(game.getGameState().getGameStatus().value);
                offset = game.getGameState().getRoundCounter() * game.getGameState().getNPlayers();
            } else {
                break;
            }
        }

        if (game != null && printStatSummary) {
            System.out.println("---------------------");
            // Print statistics for this game
            if (detailedStatistics) {
                System.out.println(statSummary);
            } else {
                System.out.println(statSummary.name + ": " + statSummary.mean() + " (n=" + statSummary.n() + ")");
            }
        }

        return game;
    }

    public static void runWithStats(String config, int nPlayers, int nRepetition) {
        String logFile = "results.csv";
        ActionController ac =  null; // new ActionController();

        // logging setup
        MetricsGameListener pl = new MetricsGameListener(new PandemicMetrics().getAllMetrics());

        List<IGameListener> listeners = new ArrayList<>();
        listeners.add((pl));

        List<AbstractPlayer> players = new ArrayList<>();

        for (int i = 0; i < nPlayers; i++){
//            players.add(new MCTSPlayer());
            players.add(new RandomPlayer());
        }

        PandemicParameters params = new PandemicParameters("data/pandemic/");
        runCompetition(config, players, -1, false, listeners, nRepetition, ac);
        pl.report();
    }



    /**
     * Retrieves the ordinal position of the player relative to all others, as given by stat summaries recorded.
     * Tiebreaks as set up in the enum are followed to determine order.
     */
    public static int getOrdinalPosition(SummaryLogger[] statSummaries, int playerId, int nPlayers) {
        int ordinal = 1;
        for (int i = 0; i < nPlayers; i++) {
            if (compare(statSummaries, playerId, i, TieBreak.values(), 0) < 0) ordinal++;  // player is worse than i
        }
        return ordinal;
    }

    /**
     * Recursive comparison of 2 players. Returns 1 if player is better than other, -1 if worse, and 0 if tied. Will
     * iterate through all tiebreaks before returning 0 (tied)
     */
    public static int compare(SummaryLogger[] statSummaries, int playerId, int otherId, TieBreak[] tieBreaks, int tieBreakTier) {
        double player = ((TAGNumericStatSummary)statSummaries[playerId].summary().get(tieBreaks[tieBreakTier].name())).mean();
        double other = ((TAGNumericStatSummary)statSummaries[otherId].summary().get(tieBreaks[tieBreakTier].name())).mean();
        if (player == other && tieBreakTier < tieBreaks.length-1) {
            return compare(statSummaries, playerId, otherId, tieBreaks, tieBreakTier + 1);
        }
        if (tieBreaks[tieBreakTier].max) {
            if (other > player) return -1;
            else if (other < player) return 1;
        } else {
            if (other < player) return -1;
            else if (other > player) return 1;
        }
        return 0;
    }

    /**
     * Tie breaks, in order. Specifying whether agents should be maximising or minimising each feature.
     */
    public enum TieBreak {
        GAME_WIN(true),
        GAME_TICKS(false),
        N_DISEASE_CURED(true),
        N_OUTBREAKS(false),
        N_CITY_DANGER(false),
        N_DISEASE_CUBES_LEFT(true),
        N_DISEASE_ERADICATED(true);
        boolean max;
        TieBreak(boolean max) { this.max = max; }
    }

    /**
     * Run a series of different players in a specific configuration N times (same player duplicated for all players in
     * one game, players of different types do not play with each other at any point).
     * Record statistics as per PandemicCompetitionRankingAttributes
     * Print statistics for each player type
     * Print final ranking as per competition rules
     */
    public static void runCompetition(String configFile, int nPlayers, PlayerType[] playersToTest, int nRepetitions) {

        ActionController ac = null; // new ActionController();
        SummaryLogger[] sumLogs = new SummaryLogger[playersToTest.length];
        for (PlayerType playerType : playersToTest) {

            // logging setup
            MetricsGameListener pl = new MetricsGameListener(new PandemicMetrics().getAllMetrics());
            List<IGameListener> listeners = new ArrayList<>();
            listeners.add((pl));

            PandemicParameters params = new PandemicParameters("data/pandemic/");
            List<AbstractPlayer> players = new ArrayList<>();
            for (int i = 0; i < nPlayers; i++) {
                players.add(playerType.createPlayerInstance(params.getRandomSeed()));
            }
            runCompetition(configFile, players, params.getRandomSeed(), false, listeners, nRepetitions, ac);

            System.out.println(playerType.name());
            System.out.println("-----------------");
            pl.report();
        }

        // Calculate ranking as per competition rules
        TreeMap<Integer, ArrayList<String>> rankings = new TreeMap<>();
        for (int p = 0; p < playersToTest.length; p++) {
            int ordinal = getOrdinalPosition(sumLogs, p, playersToTest.length);
            if (!rankings.containsKey(ordinal)) {
                rankings.put(ordinal, new ArrayList<>());
            }
            rankings.get(ordinal).add(playersToTest[p].name());
        }
        System.out.println(rankings);
    }

    public static void main(String[] args){

        /*
         * Settings: configuration file, ac (visuals on if initialised, off if null), number of players,
         * number of repetitions per player, list of players to test.
         */
        String config = "data/pandemic/train/param-config-v2.json";
//        String config = "data/pandemic/param-config-easy.json";
        int nPlayers = 4;
        PlayerType[] playersToTest = new PlayerType[] {
                PlayerType.Random, PlayerType.MCTS, PlayerType.OSLA
        };
        int nRepetitions = 10;

        // Run
        runCompetition(config, nPlayers, playersToTest, nRepetitions);
//        runWithStats(config, nPlayers, nRepetitions);
    }
}
