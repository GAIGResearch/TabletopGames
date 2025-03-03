package evaluation.optimisation;

import core.AbstractPlayer;
import core.actions.AbstractAction;
import evaluation.RunArg;
import evaluation.tournaments.RoundRobinTournament;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Pair;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static evaluation.RunArg.*;

public class OneStepDeviations {

    public static void main(String[] args) {

        Map<RunArg, Object> config = parseConfig(args, Collections.singletonList(RunArg.Usage.ParameterSearch));

        // the ones we use are:
        // opponent to get the baseline player
        // searchSpace
        // game, nPlayers, gameParams etc.
        // destDir

        String setupFile = config.getOrDefault(RunArg.config, "").toString();
        if (!setupFile.isEmpty()) {
            // Read from file instead
            try {
                FileReader reader = new FileReader(setupFile);
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(reader);
                config = parseConfig(json, RunArg.Usage.ParameterSearch);
            } catch (FileNotFoundException ignored) {
                throw new AssertionError("Config file not found : " + setupFile);
                //    parseConfig(runGames, args);
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        }

        NTBEAParameters params = new NTBEAParameters(config);
        ITPSearchSpace<?> searchSpace = (ITPSearchSpace<?>) params.searchSpace;
        int[] baseSettings = searchSpace.settingsFromJSON(params.opponentDescriptor);
        int[] defaultSettings = searchSpace.defaultSettings();

        List<Pair<Integer, AbstractPlayer>> players = new ArrayList<>();
        players.add(Pair.of(0, (AbstractPlayer) searchSpace.instantiate(baseSettings)));
        int nextIndex = 1;
        for (int i = 0; i < baseSettings.length; i++) {
            for (int j = 0; j < params.searchSpace.nValues(i); j++) {
                if (j == baseSettings[i]) {
                    continue;
                }
                int[] settings = baseSettings.clone();
                settings[i] = j;
                AbstractPlayer player = (AbstractPlayer) searchSpace.instantiate(settings);
                // we set the name to indicate the one step deviation
                player.setName(String.format("%s: %s", searchSpace.name(i), searchSpace.allValues(i).get(j)));
                players.add(Pair.of(nextIndex, player));
                nextIndex++;
            }
        }
        double[] gamesPlayed = new double[players.size()];
        double[] totalScore = new double[players.size()];
        double[] totalScoreSquared = new double[players.size()];
        boolean finished;
        int iteration = 1;

        do {

            System.out.printf("Running tournament with remaining players: %d%n", players.size());

            // We now have all agents, with [0] being the baseline agent
            Map<RunArg, Object> tournamentConfig = new HashMap<>();
            tournamentConfig.put(matchups, params.tournamentGames);
            tournamentConfig.put(RunArg.mode, players.size() > 6 ? "random" : "exhaustive");
            tournamentConfig.put(byTeam, true);
            tournamentConfig.put(RunArg.distinctRandomSeeds, 0);
            tournamentConfig.put(RunArg.budget, params.budget);
            tournamentConfig.put(RunArg.verbose, false);
            tournamentConfig.put(RunArg.destDir, params.destDir);
            List<AbstractPlayer> tournamentPlayers = players.stream().map(p -> p.b).toList();
            RoundRobinTournament tournament = new RoundRobinTournament(
                    tournamentPlayers,
                    params.gameType,
                    params.nPlayers,
                    params.gameParams,
                    tournamentConfig);
            tournament.run();

            // extract the results
            int bestIndex = 0;
            double bestScore = -Double.MAX_VALUE;
            double bestStdError= 0.0;
            for (int loop = 0; loop < players.size(); loop++) {
                int i = players.get(loop).a;
                gamesPlayed[i] += tournament.getNGamesPlayed()[loop];
                totalScore[i] += params.evalMethod.equals("Win")
                        ? tournament.getWinRate(loop) * tournament.getNGamesPlayed()[loop]
                        : -tournament.getOrdinalRank(loop) * tournament.getNGamesPlayed()[loop];
                totalScoreSquared[i] += tournament.getSumOfSquares(loop, params.evalMethod);
                if (totalScore[i] / gamesPlayed[i] > bestScore) {
                    bestIndex = loop;
                    bestScore = totalScore[i] / gamesPlayed[i];
                    bestStdError = Math.sqrt(totalScoreSquared[i] / gamesPlayed[i] - Math.pow(totalScore[i] / gamesPlayed[i], 2))
                            / Math.sqrt(gamesPlayed[i]);
                }
            }

            System.out.printf("Iteration %d: Best agent is %s (%d) with score %.3f +/- %.3f%n",
                    iteration, players.get(bestIndex), bestIndex, bestScore, bestStdError);
            // Now run through all players, and discard any more than 4 standard deviations below the best
            List<Pair<Integer, AbstractPlayer>> newPlayers = new ArrayList<>();
            for (Pair<Integer, AbstractPlayer> player : players) {
                int i = player.a;
                double stdError = Math.sqrt(totalScoreSquared[i] / gamesPlayed[i] - Math.pow(totalScore[i] / gamesPlayed[i], 2)) / Math.sqrt(gamesPlayed[i]);
                if (totalScore[i] / gamesPlayed[i] > bestScore - 4 * Math.max(stdError, bestStdError)) {
                    newPlayers.add(Pair.of(i, player.b));
               //     System.out.printf("Retaining %s (%d) with score %.3f +/- %.3f %n", players.get(i), i, totalScore[i] / gamesPlayed[i], stdError);
                } else {
                    System.out.printf("Discarding %s (%d) with score %.3f +/- %.3f %n", player.b, i, totalScore[i] / gamesPlayed[i], stdError);
                }
            }

            players = newPlayers;
            iteration++;
            finished = players.size() == 1;

            // TODO: Add in checking of default settings
            // If there is no significant difference between two agents that differ in 1 setting, then we can discard the one that is not the default
            // This could be the one that is formally 'better': the question is when to start this filtering
            // it only makes sense once the difference is smaller than some pre-defined threshold

        } while (!finished);

    }
}
