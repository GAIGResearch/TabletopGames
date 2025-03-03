package evaluation.optimisation;

import core.AbstractPlayer;
import core.actions.AbstractAction;
import evaluation.RunArg;
import evaluation.tournaments.RoundRobinTournament;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Pair;

import java.io.File;
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
        List<int[]> deviations = new ArrayList<>();

        List<Pair<Integer, AbstractPlayer>> players = new ArrayList<>();
        players.add(Pair.of(0, (AbstractPlayer) searchSpace.instantiate(baseSettings)));
        deviations.add(new int[2]); // dummy for baseline agent
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
                deviations.add(new int[]{i, j});  // the dimension and value of the one step deviation
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
            double bestStdError = 0.0;
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
                if (i == 0) {
                    newPlayers.add(player); // never discard the baseline agent
                    continue;
                }
                double stdError = Math.sqrt(totalScoreSquared[i] / gamesPlayed[i] - Math.pow(totalScore[i] / gamesPlayed[i], 2)) / Math.sqrt(gamesPlayed[i]);
                double stdErrorMultiple = 2.5 + 0.08 * players.size();
                // this is a very rough adjustment. 2.58 is about a 1% confidence interval, but is only valid if we have
                // a single comparison. Here we have many, so we need to adjust for multiple comparisons
                if (totalScore[i] / gamesPlayed[i] > bestScore - stdErrorMultiple * Math.max(stdError, bestStdError)) {
                    newPlayers.add(Pair.of(i, player.b));
                    //     System.out.printf("Retaining %s (%d) with score %.3f +/- %.3f %n", players.get(i), i, totalScore[i] / gamesPlayed[i], stdError);
                } else {
                    System.out.printf("Discarding %s (%d) with score %.3f +/- %.3f %n", player.b, i, totalScore[i] / gamesPlayed[i], stdError);
                }
            }

            players = newPlayers;
            iteration++;
            finished = players.size() <= params.nPlayers + 2 || iteration >= params.repeats;

        } while (!finished);

        // Now we work out the final tweaked 'best' agent
        int[] tweakedSettings = baseSettings.clone();
        double baseAgentScore = totalScore[0] / gamesPlayed[0];
        System.out.printf("Base agent score is %.3f%n", baseAgentScore);
        for (int dimension = 0; dimension < baseSettings.length; dimension++) {
            // firstly extract all the players that affect this dimension, and sort them in descending order of score
            int finalDimension = dimension;
            List<Pair<Integer, AbstractPlayer>> dimensionPlayers = players.stream()
                    .filter(p -> p.a > 0)  // ignore the baseline agent
                    .filter(p -> deviations.get(p.a)[0] == finalDimension)
                    .sorted(Comparator.comparingDouble(p -> -totalScore[p.a] / gamesPlayed[p.a]))
                    .toList();
            if (dimensionPlayers.isEmpty()) {
                System.out.printf("No players for dimension %s, keeping base value%n", searchSpace.name(dimension));
                continue;  // no change to baseline
            }
            for (Pair<Integer, AbstractPlayer> stuff : dimensionPlayers) {
                int[] settings = deviations.get(stuff.a);
                if (settings[0] != dimension) {
                    throw new AssertionError("Dimension mismatch");
                }
                double score = totalScore[stuff.a] / gamesPlayed[stuff.a];
                double stdError = Math.sqrt(totalScoreSquared[stuff.a] / gamesPlayed[stuff.a] - Math.pow(totalScore[stuff.a] / gamesPlayed[stuff.a], 2))
                        / Math.sqrt(gamesPlayed[stuff.a]);
                System.out.printf("Processing Agent %s (%d) with score %.3f +/- %.3f %n", stuff.b, stuff.a, score, stdError);
                if (score > baseAgentScore + 2 * stdError) {
                    System.out.printf("Tweaking %s to %s with score %.3f +/- %.3f versus base agent score of %.3f%n",
                            searchSpace.name(dimension), searchSpace.allValues(dimension).get(settings[1]), score, stdError, baseAgentScore);
                    tweakedSettings[dimension] = settings[1];
                    break;
                } else if (score > baseAgentScore - 1 * stdError) {
                    if (settings[1] == defaultSettings[dimension]) {
                        System.out.printf("Setting to default %s (%s) with score %.3f +/- %.3f versus base agent score of %.3f%n",
                                searchSpace.name(dimension), searchSpace.allValues(dimension).get(settings[1]), score, stdError, baseAgentScore);
                        tweakedSettings[dimension] = settings[1];
                        break;
                    }
                } else {
                    break;  // we have reached the worse agents
                }
            }
        }
        // Now write the tweaked agent to JSON
        String oldStem = new File(params.opponentDescriptor).getName();
        oldStem = oldStem.substring(0, oldStem.lastIndexOf('.'));
        searchSpace.writeAgentJSON(tweakedSettings, params.destDir + File.separator + oldStem + "_tweaked.json");
    }
}
