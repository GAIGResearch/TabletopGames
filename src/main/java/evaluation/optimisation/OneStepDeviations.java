package evaluation.optimisation;

import core.AbstractPlayer;
import core.actions.AbstractAction;
import evaluation.RunArg;
import evaluation.tournaments.RoundRobinTournament;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Pair;
import utilities.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static evaluation.RunArg.*;

public class OneStepDeviations {

    NTBEAParameters params;
    ITPSearchSpace<?> searchSpace;

    public OneStepDeviations(NTBEAParameters params) {
        this.params = params;
        this.searchSpace = (ITPSearchSpace<?>) params.searchSpace;
    }

    public static void main(String[] args) {

        Map<RunArg, Object> config = parseConfig(args, Collections.singletonList(RunArg.Usage.ParameterSearch));

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
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        }

        NTBEAParameters params = new NTBEAParameters(config);
        ITPSearchSpace<?> searchSpace = (ITPSearchSpace<?>) params.searchSpace;
        int[] baseSettings = searchSpace.settingsFromJSON(params.opponentDescriptor);

        int[] tweakedSettings = new OneStepDeviations(params).run(baseSettings);

        // Now write the tweaked agent to JSON
        String oldStem = new File(params.opponentDescriptor).getName();
        oldStem = oldStem.substring(0, oldStem.lastIndexOf('.'));
        searchSpace.writeAgentJSON(tweakedSettings, params.destDir + File.separator + oldStem + "_tweaked.json");
    }

    public int[] run(int[] baseSettings) {

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

            System.out.printf("Running OSD tournament with remaining players: %d%n", players.size());

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
            // Now run through all players, and discard any that are significantly worse than the best
            List<Pair<Integer, AbstractPlayer>> newPlayers = new ArrayList<>();
            for (Pair<Integer, AbstractPlayer> player : players) {
                int i = player.a;
                if (i == 0 || i == bestIndex) {
                    newPlayers.add(player); // never discard the baseline agent, or the best agent
                    continue;
                }
                double stdError = Utils.meanDiffStandardError(totalScore[bestIndex], totalScore[i],
                        totalScoreSquared[bestIndex], totalScoreSquared[i], (int) gamesPlayed[bestIndex], (int) gamesPlayed[i]);
                double stdErrorMultiple = Utils.standardZScore(0.01, players.size() - 1);
                // We go for a 99% confidence that any given agent is worse than the best (adjusted for multiple comparisons)
                // This technically does not adjust for the fact we compare against the MAX agent, so implicitly there are N(N-1)/2 comparisons
                // instead of N-1, but the full set of comparisons are not independent. We adjust for this by choosing a 1% confidence interval.
                if (totalScore[i] / gamesPlayed[i] > bestScore - stdErrorMultiple * stdError) {
                    newPlayers.add(Pair.of(i, player.b));
                    //     System.out.printf("Retaining %s (%d) with score %.3f +/- %.3f %n", players.get(i), i, totalScore[i] / gamesPlayed[i], stdError);
                } else {
                    System.out.printf("Discarding %s (%d) with score %.3f, and mean diff of %.3f +/- %.3f %n",
                            player.b, i, totalScore[i] / gamesPlayed[i], bestScore - totalScore[i] / gamesPlayed[i], stdError);
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
                double stdError = Utils.meanDiffStandardError(totalScore[0], totalScore[stuff.a],
                        totalScoreSquared[0], totalScoreSquared[stuff.a], (int) gamesPlayed[0], (int) gamesPlayed[stuff.a]);
                double adjustedZScore = Utils.standardZScore(0.05, players.size() - 1);
                System.out.printf("Processing Agent %s (%d) with score %.3f, diff to base of %.3f +/- %.3f %n",
                        stuff.b, stuff.a, score, score - baseAgentScore, stdError);
                double adjustedZDefaultComparison = Utils.standardZScore(0.25, baseSettings.length);
                if (score > baseAgentScore + adjustedZScore * stdError) {
                    System.out.printf("Tweaking %s to %s with score %.3f, better than base agent by %.3f +/- %.3f%n",
                            searchSpace.name(dimension), searchSpace.allValues(dimension).get(settings[1]), score,
                            score - baseAgentScore, stdError);
                    tweakedSettings[dimension] = settings[1];
                    break;
                } else if (score > baseAgentScore - adjustedZDefaultComparison * stdError) {
                    if (settings[1] == defaultSettings[dimension]) {
                        double bestScore = totalScore[dimensionPlayers.get(0).a] / gamesPlayed[dimensionPlayers.get(0).a];
                        // add an additional check that the default agent is also within spitting distance of the best agent
                        // to avoid cases where we set to the default even though this is quite a bit worse than the best agent on this setting
                        // (in the worst case this could be 2.99 standard deviations worse than the best...so sticking to the tuned value is a better idea)
                        if (score > bestScore - adjustedZDefaultComparison * stdError) {
                            System.out.printf("Setting to default %s (%s) with score %.3f, improving over base agent by %.3f +/- %.3f%n",
                                    searchSpace.name(dimension), searchSpace.allValues(dimension).get(settings[1]), score,
                                    score - baseAgentScore, stdError);
                            tweakedSettings[dimension] = settings[1];
                            break;
                        }
                    }
                } else {
                    break;  // we have reached the worse agents
                }
            }
        }
        return tweakedSettings;
    }
}
