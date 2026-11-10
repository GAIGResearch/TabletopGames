package evaluation;

import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import evaluation.listeners.IGameListener;
import evaluation.summarisers.TAGNumericStatSummary;
import games.GameType;
import players.human.ActionController;

import java.util.List;

/**
 * Runs a series of games for a fixed set of players and summarises the results.
 * <p>
 * This is the multi-game helper that used to live on {@link Game}: it plays each {@link GameType} in turn with
 * {@link Game#runOne}, either a fixed number of repetitions from one seed or once per entry in a seed array, and
 * reports WIN/LOSE/DRAW values per player. It builds no tournaments, matchups or rankings, so for agent
 * comparisons prefer {@link RunGames} with {@code mode=fixed} or
 * {@link evaluation.tournaments.RoundRobinTournament}. It stays outside {@code Game} because the game loop does
 * not need it: it exists for drivers that already hold constructed {@link AbstractPlayer} instances and want the
 * numbers back in memory rather than in files.
 */
public class MultiGameRunner {

public static void runMany(List<GameType> gamesToPlay, List<AbstractPlayer> players, Long seed,
                               int nRepetitions, boolean randomizeParameters,
                               boolean detailedStatistics, List<IGameListener> listeners, int turnPause) {
        int nPlayers = players.size();

        // Save win rate statistics over all games
        TAGNumericStatSummary[] overall = new TAGNumericStatSummary[nPlayers];
        String[] agentNames = new String[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            String[] split = players.get(i).getClass().toString().split("\\.");
            String agentName = split[split.length - 1] + "-" + i;
            overall[i] = new TAGNumericStatSummary("Overall " + agentName);
            agentNames[i] = agentName;
        }

        // For each game...
        for (GameType gt : gamesToPlay) {

            // Save win rate statistics over all repetitions of this game
            TAGNumericStatSummary[] statSummaries = new TAGNumericStatSummary[nPlayers];
            for (int i = 0; i < nPlayers; i++) {
                statSummaries[i] = new TAGNumericStatSummary("{Game: " + gt.name() + "; Player: " + agentNames[i] + "}");
            }

            // Play n repetitions of this game and record player results
            Game game = null;
            int offset = 0;
            for (int i = 0; i < nRepetitions; i++) {
                Long s = seed;
                if (s == null) s = System.currentTimeMillis();
                s += offset;
                game = Game.runOne(gt, null, players, s, randomizeParameters, listeners, null, turnPause);
                if (game != null) {
                    recordPlayerResults(statSummaries, game);
                    offset = game.getGameState().getRoundCounter() * game.getGameState().getNPlayers();
                } else {
                    break;
                }
//                System.out.println("Game " + i + "/" + nRepetitions);
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
    }

public static TAGNumericStatSummary[] runMany(List<GameType> gamesToPlay, List<AbstractPlayer> players, int nRepetitions,
                                                  long[] seeds, ActionController ac, boolean randomizeParameters, List<IGameListener> listeners, int turnPause) {
        int nPlayers = players.size();

        // Save win rate statistics over all games
        TAGNumericStatSummary[] overall = new TAGNumericStatSummary[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            overall[i] = new TAGNumericStatSummary("Overall Player " + i);
        }

        // For each game...
        for (GameType gt : gamesToPlay) {

            // Save win rate statistics over all repetitions of this game
            TAGNumericStatSummary[] statSummaries = new TAGNumericStatSummary[nPlayers];
            for (int i = 0; i < nPlayers; i++) {
                statSummaries[i] = new TAGNumericStatSummary("Game: " + gt.name() + "; Player: " + i);
            }

            // Play n repetitions of this game and record player results
            for (int i = 0; i < nRepetitions; i++) {
                Game game = Game.runOne(gt, null, players, seeds[i], randomizeParameters, listeners, null, turnPause);
                if (game != null) {
                    recordPlayerResults(statSummaries, game);
                }
            }

            for (int i = 0; i < nPlayers; i++) {
                // Print statistics for this game
                System.out.println(statSummaries[i].toString());

                // Record in overall statistics
                overall[i].add(statSummaries[i]);
            }
        }

        // Print final statistics
        System.out.println("\n---------------------\n");
        for (int i = 0; i < nPlayers; i++) {
            // Print statistics for this game
            System.out.println(overall[i].toString());
        }
        return overall;
    }

public static void recordPlayerResults(TAGNumericStatSummary[] statSummaries, Game game) {
        int nPlayers = statSummaries.length;
        CoreConstants.GameResult[] results = game.getGameState().getPlayerResults();
        for (int p = 0; p < nPlayers; p++) {
            if (results[p] == CoreConstants.GameResult.WIN_GAME || results[p] == CoreConstants.GameResult.LOSE_GAME || results[p] == CoreConstants.GameResult.DRAW_GAME) {
                statSummaries[p].add(results[p].value);
            }
        }
    }
}
