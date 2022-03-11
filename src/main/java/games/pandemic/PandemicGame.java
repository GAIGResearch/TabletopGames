package games.pandemic;

import core.*;
import core.interfaces.IGameListener;
import games.GameType;
import games.pandemic.gui.PandemicGUIManager;
import players.human.ActionController;
import players.mcts.MCTSPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;
import utilities.FileStatsLogger;
import utilities.TAGStatSummary;

import java.util.*;

public class PandemicGame extends Game {

    public PandemicGame(List<AbstractPlayer> agents, PandemicParameters params) {
        super(GameType.Pandemic, agents, new PandemicForwardModel(params, agents.size()),
                new PandemicGameState(params, agents.size()));
    }
    public PandemicGame(AbstractForwardModel model, AbstractGameState gameState) {
        super(GameType.Pandemic, model, gameState);
    }

    public static Game runCompetition(GameType gameToPlay, String parameterConfigFile, List<AbstractPlayer> players, long seed,
                          boolean randomizeParameters, List<IGameListener> listeners, int nRepetitions, ActionController ac){
        int nPlayers = players.size();
        boolean detailedStatistics = true;

        // Save win rate statistics over all games
        TAGStatSummary[] overall = new TAGStatSummary[nPlayers];
        String[] agentNames = new String[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            String[] split = players.get(i).getClass().toString().split("\\.");
            String agentName = split[split.length - 1] + "-" + i;
            overall[i] = new TAGStatSummary("Overall " + agentName);
            agentNames[i] = agentName;
        }

        // Save win rate statistics over all repetitions of this game
        TAGStatSummary[] statSummaries = new TAGStatSummary[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            statSummaries[i] = new TAGStatSummary("{Game: " + gameToPlay.name() + "; Player: " + agentNames[i] + "}");
        }

        // Play n repetitions of this game and record player results
        Game game = null;
        int offset = 0;
        for (int i = 0; i < nRepetitions; i++) {
            Long s = seed;
            if (s == null) s = System.currentTimeMillis();
            s += offset;
            game = runOne(gameToPlay, parameterConfigFile, players, s, randomizeParameters, listeners, ac, 0);
            if (game != null) {
                recordPlayerResults(statSummaries, game);
                offset = game.getGameState().getTurnOrder().getRoundCounter() * game.getGameState().getNPlayers();
            } else {
                break;
            }
        }

        if (game != null) {
            System.out.println("---------------------");
            for (int i = 0; i < nPlayers; i++) {
                // Print statistics for this game
                if (detailedStatistics) {
                    System.out.println(statSummaries[i].toString());
                } else {
                    System.out.println(statSummaries[i].name + ": " + statSummaries[i].mean() + " (n=" + statSummaries[i].n() + ")");
                }

                // Record in overall statistics
                overall[i].add(statSummaries[i]);
            }
        }

        // Print final statistics
        System.out.println("\n=====================\n");
        for (int i = 0; i < nPlayers; i++) {
            // Print statistics for this game
            if (detailedStatistics) {
                System.out.println(overall[i].toString());
            } else {
                System.out.println(overall[i].name + ": " + overall[i].mean());
            }
        }

        return game;
    }

    public static void main(String[] args){

        String logFile = "results.csv";
        ActionController ac = null; // new ActionController();
        int nPlayers = 4;
        int nRepetition = 10;

        // logging setup
        FileStatsLogger logger = new FileStatsLogger(logFile);
        PandemicListener pl = new PandemicListener(logger);
        ArrayList<IGameListener> listeners = new ArrayList<>();
        listeners.add((pl));

        List<AbstractPlayer> players = new ArrayList<>();

        for (int i = 0; i < nPlayers; i++){
//            players.add(new MCTSPlayer());
            players.add(new RandomPlayer());
        }

        PandemicParameters params = new PandemicParameters("data/pandemic/", System.currentTimeMillis());
        Game game = runCompetition(GameType.Pandemic, "data/pandemic/param-config-easy.json", players, params.getRandomSeed(), false, listeners, nRepetition, ac);
        logger.processDataAndFinish();

    }
}
