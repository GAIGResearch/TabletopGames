package evaluation.optimisation;

import core.AbstractPlayer;
import evaluation.RunArg;
import evaluation.optimisation.ntbea.SolutionEvaluator;
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
import java.util.stream.IntStream;

import static evaluation.RunArg.*;

public class OneStepDeviations {

    NTBEAParameters params;
    SolutionEvaluator evaluator;
    boolean itpSearchSpace;

    public OneStepDeviations(NTBEAParameters params) {
        this(params, null);
    }

    public OneStepDeviations(NTBEAParameters params, SolutionEvaluator evaluator) {
        this.params = params;
        this.evaluator = evaluator;
        this.itpSearchSpace = params.searchSpace instanceof ITPSearchSpace;
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
                config = parseConfig(json, RunArg.Usage.ParameterSearch, true);
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

        int[] defaultSettings = itpSearchSpace ?
                ((ITPSearchSpace<?>) params.searchSpace).defaultSettings() :
                baseSettings.clone();
        List<List<Integer>> playerSettings = new ArrayList<>();

        playerSettings.add(Arrays.stream(baseSettings).boxed().toList()); // dummy for baseline agent
        for (int i = 0; i < baseSettings.length; i++) {
            for (int j = 0; j < params.searchSpace.nValues(i); j++) {
                if (j == baseSettings[i]) {
                    continue;
                }
                int[] settings = baseSettings.clone();
                settings[i] = j;
                playerSettings.add(Arrays.stream(settings).boxed().toList());
            }
        }

        // we track the stats for each player (these need to be lists, as we'll be adding new players as we proceed)
        List<Integer> gamesPlayed = new ArrayList<>(playerSettings.size());
        List<Double> totalScore = new ArrayList<>(playerSettings.size());
        List<Double> totalScoreSquared = new ArrayList<>(playerSettings.size());
        List<Boolean> stillUnderConsideration = new ArrayList<>(playerSettings.size());
        for (int i = 0; i < playerSettings.size(); i++) {
            gamesPlayed.add(0);
            totalScore.add(0.0);
            totalScoreSquared.add(0.0);
            stillUnderConsideration.add(true);
        }
        List<Integer> agentsMuchBetterThanBaseline = new ArrayList<>();
        boolean finished;
        int iteration = 1;

        do {
            int numberStillUnderConsideration = (int) stillUnderConsideration.stream().filter(b -> b).count();
            if (params.verbose)
                System.out.printf("Running OSD tournament with remaining players: %d%n", numberStillUnderConsideration);

            // store the results
            int bestIndex = 0;
            double bestScore = -Double.MAX_VALUE;
            double bestStdError = 0.0;
            if (itpSearchSpace && (evaluator == null || params.OSDTournament)) {
                // we set up players to be all the current playerSettings
                List<Pair<Integer, Object>> players = new ArrayList<>();
                for (int i = 0; i < playerSettings.size(); i++) {
                    if (stillUnderConsideration.get(i)) {
                        int[] settings = playerSettings.get(i).stream().mapToInt(Integer::intValue).toArray();
                        AbstractPlayer player = (AbstractPlayer) ((ITPSearchSpace<?>) params.searchSpace).instantiate(settings);
                        // the name is set to indicate how it deviates from the baseline
                        player.setName(getAgentName(settings, baseSettings));
                        players.add(Pair.of(i, player));
                    }
                }

                // We now have all agents, with [0] being the baseline agent
                Map<RunArg, Object> tournamentConfig = new HashMap<>();
                tournamentConfig.put(matchups, params.tournamentGames);
                tournamentConfig.put(RunArg.mode, players.size() > 6 ? "random" : "exhaustive");
                tournamentConfig.put(byTeam, true);
                tournamentConfig.put(RunArg.distinctRandomSeeds, 0);
                tournamentConfig.put(RunArg.budget, params.budget);
                tournamentConfig.put(RunArg.verbose, false);
                tournamentConfig.put(RunArg.destDir, params.destDir);
                List<AbstractPlayer> tournamentPlayers = players.stream().map(p -> p.b).map(o -> (AbstractPlayer) o).toList();
                RoundRobinTournament tournament = new RoundRobinTournament(
                        tournamentPlayers,
                        params.gameType,
                        params.nPlayers,
                        params.gameParams,
                        tournamentConfig);
                tournament.run();

                for (int loop = 0; loop < players.size(); loop++) {
                    int i = players.get(loop).a;
                    int g = tournament.getNGamesPlayed()[loop];
                    gamesPlayed.set(i, gamesPlayed.get(i) + g);
                    totalScore.set(i, totalScore.get(i) + (params.evalMethod.equals("Win")
                            ? tournament.getWinRate(loop) * g
                            : -tournament.getOrdinalRank(loop) * g));
                    totalScoreSquared.set(i, totalScoreSquared.get(i) + tournament.getSumOfSquares(loop, params.evalMethod));
                }
            } else {
                // we instead use the SolutionEvaluator to get the fitness
                // We still populated gamesPlayed, totalScore and totalScoreSquared, but using the results from the evaluator
                // we divide the total budget between the number of agents
                int gamesPerPlayer = params.tournamentGames / numberStillUnderConsideration;
                for (int i = 0; i < playerSettings.size(); i++) {
                    if (stillUnderConsideration.get(i)) {
                        int[] settings = playerSettings.get(i).stream().mapToInt(Integer::intValue).toArray();
                        for (int g = 0; g < gamesPerPlayer; g++) {
                            double fitness = evaluator.evaluate(settings);
                            totalScore.set(i, totalScore.get(i) + fitness);
                            totalScoreSquared.set(i, totalScoreSquared.get(i) + Math.pow(fitness, 2));
                        }
                        gamesPlayed.set(i, gamesPlayed.get(i) + gamesPerPlayer);
                    }
                }
            }

            int robustGamesThreshold = Math.min(iteration * params.tournamentGames / gamesPlayed.size(), 3);
            for (int i = 0; i < playerSettings.size(); i++) {
                if (stillUnderConsideration.get(i)) {
                    // if we have played enough games, then check if best
                    if (gamesPlayed.get(i) >= robustGamesThreshold && totalScore.get(i) / gamesPlayed.get(i) > bestScore) {
                        bestIndex = i;
                        bestScore = totalScore.get(i) / gamesPlayed.get(i);
                        int n = gamesPlayed.get(i);
                        bestStdError = Math.sqrt(totalScoreSquared.get(i) / n - Math.pow(bestScore, 2))
                                / Math.sqrt(n - 1);
                    }
                }
            }
            double baseAgentScore = totalScore.get(0) / gamesPlayed.get(0);
            if (params.verbose) {
                System.out.printf("Iteration %d: Best robust agent is %s with score %.3f +/- %.3f%n",
                        iteration, getAgentName(playerSettings.get(bestIndex).stream().mapToInt(i -> i).toArray(), baseSettings),
                        bestScore, bestStdError);
                System.out.printf("Baseline agent has score %.3f%n", baseAgentScore);
            }
            // Now run through all players, and discard any that are significantly worse than the best
            double stdErrorMultiple = Utils.standardZScore((1.0 - params.OSDConfidence) / 5.0, numberStillUnderConsideration - 1);
            double stdBetterMultiple = Utils.standardZScore((1.0 - params.OSDConfidence), numberStillUnderConsideration - 1);
            //       System.out.printf("Discarding agents with score more than %.3f standard errors worse than best%n", stdErrorMultiple);
            //       System.out.printf("Checking for agents significantly better than baseline by more than %.3f standard errors%n", stdBetterMultiple);
            for (int i = 1; i < playerSettings.size(); i++) { // we skip the baseline agent at position 0, which is always retained
                if (!stillUnderConsideration.get(i) || gamesPlayed.get(i) < 3) {  // need a minimum number of games to be considered
                    continue;
                }
                double stdError = Utils.meanDiffStandardError(totalScore.get(bestIndex), totalScore.get(i),
                        totalScoreSquared.get(bestIndex), totalScoreSquared.get(i), gamesPlayed.get(bestIndex), gamesPlayed.get(i));
                // We go for a 99% confidence that any given agent is worse than the best (adjusted for multiple comparisons)
                // This technically does not adjust for the fact we compare against the MAX agent, so implicitly there are N(N-1)/2 comparisons
                // instead of N-1, but the full set of comparisons are not independent. We adjust for this by choosing a 1% confidence interval.
                if (totalScore.get(i) / gamesPlayed.get(i) >= bestScore - stdErrorMultiple * stdError) {
                    // we keep this agent
                    // and also check for significant improvement over the baseline agent
                    if (!agentsMuchBetterThanBaseline.contains(i)) {
                        double stdErrorToBase = Utils.meanDiffStandardError(totalScore.get(0), totalScore.get(i),
                                totalScoreSquared.get(0), totalScoreSquared.get(i), gamesPlayed.get(0), gamesPlayed.get(i));
                        if (totalScore.get(i) / gamesPlayed.get(i) > baseAgentScore + stdBetterMultiple * stdErrorToBase) {
                            if (params.verbose)
                                System.out.printf("Agent %s is significantly better than baseline with score %.3f, and mean diff of %.3f +/- %.3f %n",
                                        getAgentName(playerSettings.get(i).stream().mapToInt(n -> n).toArray(), baseSettings),
                                        totalScore.get(i) / gamesPlayed.get(i), totalScore.get(i) / gamesPlayed.get(i) - baseAgentScore, stdErrorToBase);
                            agentsMuchBetterThanBaseline.add(i);
                        }
                    }
                } else {
                    stillUnderConsideration.set(i, false);
                    if (params.verbose)
                        System.out.printf("Discarding %s with score %.3f, and mean diff of %.3f +/- %.3f %n",
                                getAgentName(playerSettings.get(i).stream().mapToInt(n -> n).toArray(), baseSettings),
                                totalScore.get(i) / gamesPlayed.get(i), bestScore - totalScore.get(i) / gamesPlayed.get(i), stdError);
                }
            }
            // Now add in new combo-players that combine the setting deviations of players that are better then baseline
            // We use all compatible combinations of agentsMuchBetterThanBaseline, and add them to the list of players
            // after checking that these settings have not already been tried
            // A 'compatible' combination is one where the deviation indices do not overlap

            // First all possible combinations of agentsMuchBetterThanBaseline
            for (int i = 0; i < agentsMuchBetterThanBaseline.size(); i++) {
                for (int j = i + 1; j < agentsMuchBetterThanBaseline.size(); j++) {
                    List<Integer> settings1 = playerSettings.get(agentsMuchBetterThanBaseline.get(i));
                    List<Integer> settings2 = playerSettings.get(agentsMuchBetterThanBaseline.get(j));
                    boolean compatible = true;
                    for (int k = 0; k < settings1.size(); k++) {
                        if (!settings1.get(k).equals(playerSettings.get(0).get(k)) &&
                                !settings2.get(k).equals(playerSettings.get(0).get(k))) {
                            compatible = false;
                            break;
                        }
                    }
                    if (compatible) {
                        List<Integer> combinedSettings = new ArrayList<>(settings1);
                        // use 1 as the base, and add in the changes that 2 makes
                        for (int k = 0; k < settings1.size(); k++) {
                            if (!settings2.get(k).equals(playerSettings.get(0).get(k))) {
                                combinedSettings.set(k, settings2.get(k));
                            }
                        }
                        // check for duplicate
                        if (!playerSettings.contains(combinedSettings)) {
                            // add new player
                            int[] combinedSettingsArray = combinedSettings.stream().mapToInt(Integer::intValue).toArray();
                            playerSettings.add(combinedSettings);
                            gamesPlayed.add(0);
                            totalScore.add(0.0);
                            totalScoreSquared.add(0.0);
                            stillUnderConsideration.add(true);
                            if (params.verbose)
                                System.out.printf("Adding new player %s with settings %s%n", getAgentName(combinedSettingsArray, baseSettings),
                                        Arrays.toString(combinedSettingsArray));
                        }
                    }
                }
            }
            iteration++;
            numberStillUnderConsideration = (int) stillUnderConsideration.stream().filter(b -> b).count();
            finished = numberStillUnderConsideration <= params.nPlayers + 2 || iteration > params.repeats;

        } while (!finished);

        // Now we work out the final tweaked 'best' agent
        // We take the single best agent that is better than the baseline
        // As long as this improvement is significant. Keep working through all agents, but now track the best lower bound
        // If there is no significant improvement, then we will consider any settings that are just better than the
        // baseline as long as the changes are all to default settings
        int survivingAgentCount = (int) stillUnderConsideration.stream().filter(b -> b).count();
        double baseAgentScore = totalScore.get(0) / gamesPlayed.get(0);
        double adjustedZScore = Utils.standardZScore(1.0 - params.OSDConfidence, survivingAgentCount - 1);
        double bestLowerBound = baseAgentScore;
        int[] bestDefaultSettings = null;
        double bestDefaultScore = -Double.MAX_VALUE;
        int selectedPlayer = -1;
        if (params.verbose)
            System.out.printf("Base agent score is %.3f, and zScore needed is %.3f%n", baseAgentScore, adjustedZScore);
        for (int i = 1; i < playerSettings.size(); i++) {
            if (!stillUnderConsideration.get(i) || gamesPlayed.get(i) < 3) {
                continue;
            }
            double score = totalScore.get(i) / gamesPlayed.get(i);
            double stdError = Utils.meanDiffStandardError(totalScore.get(0), totalScore.get(i),
                    totalScoreSquared.get(0), totalScoreSquared.get(i), gamesPlayed.get(0), gamesPlayed.get(i));
            if (params.verbose)
                System.out.printf("Processing Agent %s with score %.3f, diff to base of %.3f +/- %.3f %n",
                        getAgentName(playerSettings.get(i).stream().mapToInt(Integer::intValue).toArray(), baseSettings),
                        score, score - baseAgentScore, stdError);
            if (score > baseAgentScore + adjustedZScore * stdError && score - adjustedZScore * stdError > bestLowerBound) {
                // best so far
                bestLowerBound = score - adjustedZScore * stdError;
                selectedPlayer = i;
            } else if (score > baseAgentScore) {
                // check for default settings
                int playerIndex = i;
                boolean allDefault = IntStream.range(0, playerSettings.get(i).size())
                        .allMatch(d -> playerSettings.get(playerIndex).get(d).equals(baseSettings[d])
                                || playerSettings.get(playerIndex).get(d).equals(defaultSettings[d]));
                if (allDefault && score > bestDefaultScore) {
                    bestDefaultScore = score;
                    bestDefaultSettings = playerSettings.get(i).stream().mapToInt(Integer::intValue).toArray();
                }
            }
        }
        if (selectedPlayer == -1) {
            if (params.verbose)
                System.out.println("No significant improvement found");
            if (bestDefaultScore > -Double.MAX_VALUE) {
                if (params.verbose)
                    System.out.printf("Best default settings agent is %s with score %.3f%n", getAgentName(bestDefaultSettings, baseSettings), bestDefaultScore);
                return bestDefaultSettings;
            } else {
                return baseSettings;
            }
        } else {
            int[] finalSettings = playerSettings.get(selectedPlayer).stream().mapToInt(Integer::intValue).toArray();
            if (params.verbose)
                System.out.printf("Selected agent is %s with score %.3f%n", getAgentName(finalSettings, baseSettings),
                        totalScore.get(selectedPlayer) / gamesPlayed.get(selectedPlayer));
            return finalSettings;
        }
    }

    private String getAgentName(int[] settings, int[] baseSettings) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < settings.length; i++) {
            if (settings[i] == baseSettings[i]) {
                continue;
            }
            sb.append(params.searchSpace.name(i)).append(": ").append(params.searchSpace.value(i, settings[i])).append(", ");
        }
        // remove last comma and space
        if (sb.length() > 2)
            sb.delete(sb.length() - 2, sb.length());
        return sb.toString();
    }
}
