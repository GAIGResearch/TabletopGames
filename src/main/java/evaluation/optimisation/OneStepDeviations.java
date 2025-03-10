package evaluation.optimisation;

import core.AbstractPlayer;
import core.actions.AbstractAction;
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
    ITPSearchSpace<?> searchSpace;
    SolutionEvaluator evaluator;

    public OneStepDeviations(NTBEAParameters params) {
        this(params, null);
    }

    public OneStepDeviations(NTBEAParameters params, SolutionEvaluator evaluator) {
        this.params = params;
        this.searchSpace = (ITPSearchSpace<?>) params.searchSpace;
        this.evaluator = evaluator;
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
        List<List<Integer>> playerSettings = new ArrayList<>();

        // we store the players with their index in the list, as this makes it easier later when we filter the list by various criteria
        List<Pair<Integer, Object>> players = new ArrayList<>();
        players.add(Pair.of(0, searchSpace.instantiate(baseSettings)));
        playerSettings.add(Arrays.stream(baseSettings).boxed().toList()); // dummy for baseline agent
        int nextIndex = 1;
        for (int i = 0; i < baseSettings.length; i++) {
            for (int j = 0; j < params.searchSpace.nValues(i); j++) {
                if (j == baseSettings[i]) {
                    continue;
                }
                int[] settings = baseSettings.clone();
                settings[i] = j;
                Object player = searchSpace.instantiate(settings);
                // we set the name to indicate the one step deviation
                if (player instanceof AbstractPlayer) {
                    ((AbstractPlayer) player).setName(getAgentName(settings, baseSettings));
                }
                players.add(Pair.of(nextIndex, player));
                playerSettings.add(Arrays.stream(settings).boxed().toList());
                nextIndex++;
            }
        }

        // we track the stats for each player (these need to be lists, as we'll be adding new players as we proceed)
        List<Integer> gamesPlayed = new ArrayList<>(players.size());
        List<Double> totalScore = new ArrayList<>(players.size());
        List<Double> totalScoreSquared = new ArrayList<>(players.size());
        for (int i = 0; i < players.size(); i++) {
            gamesPlayed.add(0);
            totalScore.add(0.0);
            totalScoreSquared.add(0.0);
        }
        List<Integer> agentsMuchBetterThanBaseline = new ArrayList<>();
        boolean finished;
        int iteration = 1;

        do {
            System.out.printf("Running OSD tournament with remaining players: %d%n", players.size());

            // store the results
            int bestIndex = 0;
            double bestScore = -Double.MAX_VALUE;
            double bestStdError = 0.0;
            if (evaluator == null) {
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
                int gamesPerPlayer = params.tournamentGames / players.size();
                for (int loop = 0; loop < players.size(); loop++) {
                    int i = players.get(loop).a;
                    int[] settings = playerSettings.get(i).stream().mapToInt(Integer::intValue).toArray();
                    for (int g = 0; g < gamesPerPlayer; g++) {
                        double fitness = evaluator.evaluate(settings);
                        totalScore.set(i, totalScore.get(i) + fitness);
                        totalScoreSquared.set(i, totalScoreSquared.get(i) + Math.pow(fitness, 2));
                    }
                    gamesPlayed.set(i, gamesPlayed.get(i) + gamesPerPlayer);
                }
            }

            int robustGamesThreshold = iteration * params.tournamentGames / gamesPlayed.size();
            for (int loop = 0; loop < players.size(); loop++) {
                int i = players.get(loop).a;
                // if we have played enough games, then check if best
                if (gamesPlayed.get(i) >= robustGamesThreshold && totalScore.get(i) / gamesPlayed.get(i) > bestScore) {
                    bestIndex = loop;
                    bestScore = totalScore.get(i) / gamesPlayed.get(i);
                    bestStdError = Math.sqrt(totalScoreSquared.get(i) / gamesPlayed.get(i) - Math.pow(bestScore, 2))
                            / Math.sqrt(gamesPlayed.get(i));
                }
            }
            double baseAgentScore = totalScore.get(0) / gamesPlayed.get(0);
            System.out.printf("Iteration %d: Best robust agent is %s with score %.3f +/- %.3f%n",
                    iteration, players.get(bestIndex), bestScore, bestStdError);
            System.out.printf("Baseline agent has score %.3f%n", baseAgentScore);
            // Now run through all players, and discard any that are significantly worse than the best
            List<Pair<Integer, Object>> newPlayers = new ArrayList<>();
            double stdErrorMultiple = Utils.standardZScore(0.01, players.size() - 1);
            double stdBetterMultiple = Utils.standardZScore(0.10, players.size() - 1);
     //       System.out.printf("Discarding agents with score more than %.3f standard errors worse than best%n", stdErrorMultiple);
     //       System.out.printf("Checking for agents significantly better than baseline by more than %.3f standard errors%n", stdBetterMultiple);
            for (Pair<Integer, Object> player : players) {
                int i = player.a;
                if (i == 0) {
                    newPlayers.add(player); // never discard the baseline agent
                    continue;
                }
                double stdError = Utils.meanDiffStandardError(totalScore.get(bestIndex), totalScore.get(i),
                        totalScoreSquared.get(bestIndex), totalScoreSquared.get(i), gamesPlayed.get(bestIndex), gamesPlayed.get(i));
                // We go for a 99% confidence that any given agent is worse than the best (adjusted for multiple comparisons)
                // This technically does not adjust for the fact we compare against the MAX agent, so implicitly there are N(N-1)/2 comparisons
                // instead of N-1, but the full set of comparisons are not independent. We adjust for this by choosing a 1% confidence interval.
                if (totalScore.get(i) / gamesPlayed.get(i) >= bestScore - stdErrorMultiple * stdError) {
                    newPlayers.add(Pair.of(i, player.b));
    //                System.out.printf("Keeping %s (%d) with score %.3f, and mean diff of %.3f +/- %.3f %n",
   //                         player.b, i, totalScore.get(i) / gamesPlayed.get(i), bestScore - totalScore.get(i) / gamesPlayed.get(i), stdError);
                    // and also check for significant improvement over the baseline agent
                    if (!agentsMuchBetterThanBaseline.contains(i)) {
                        double stdErrorToBase = Utils.meanDiffStandardError(totalScore.get(0), totalScore.get(i),
                                totalScoreSquared.get(0), totalScoreSquared.get(i), gamesPlayed.get(0), gamesPlayed.get(i));
                        if (totalScore.get(i) / gamesPlayed.get(i) > baseAgentScore + stdBetterMultiple * stdErrorToBase) {
                            System.out.printf("Agent %s is significantly better than baseline with score %.3f, and mean diff of %.3f +/- %.3f %n",
                                    player.b, totalScore.get(i) / gamesPlayed.get(i), totalScore.get(i) / gamesPlayed.get(i) - baseAgentScore, stdErrorToBase);
                            agentsMuchBetterThanBaseline.add(i);
                        }
                    }
                } else {
                    System.out.printf("Discarding %s (%d) with score %.3f, and mean diff of %.3f +/- %.3f %n",
                            player.b, i, totalScore.get(i) / gamesPlayed.get(i), bestScore - totalScore.get(i) / gamesPlayed.get(i), stdError);
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
                            AbstractPlayer player = (AbstractPlayer) searchSpace.instantiate(combinedSettingsArray);
                            player.setName(getAgentName(combinedSettingsArray, baseSettings));
                            newPlayers.add(Pair.of(nextIndex, player));
                            playerSettings.add(combinedSettings);
                            gamesPlayed.add(0);
                            totalScore.add(0.0);
                            totalScoreSquared.add(0.0);
                            nextIndex++;
                            System.out.printf("Adding new player %s with settings %s%n", player, Arrays.toString(combinedSettingsArray));
                        }
                    }
                }
            }

            players = newPlayers;
            iteration++;
            finished = players.size() <= params.nPlayers + 2 || iteration >= params.repeats;

        } while (!finished);

        // Now we work out the final tweaked 'best' agent
        // We take the single best agent that is better than the baseline
        // As long as this improvement is significant. Keep working through all agents, but now track the best lower bound
        // If there is no significant improvement, then we will consider any settings that are just better than the
        // baseline as long as the changes are all to default settings
        double baseAgentScore = totalScore.get(0) / gamesPlayed.get(0);
        double adjustedZScore = Utils.standardZScore(0.05, players.size() - 1);
        double bestLowerBound = baseAgentScore;
        int[] bestDefaultSettings = null;
        double bestDefaultScore = -Double.MAX_VALUE;
        int selectedPlayer = -1;
        System.out.printf("Base agent score is %.3f, and zScore needed is %.3f%n", baseAgentScore, adjustedZScore);
        for (Pair<Integer, Object> stuff : players) {
            int playerIndex = stuff.a;
            double score = totalScore.get(playerIndex) / gamesPlayed.get(playerIndex);
            double stdError = Utils.meanDiffStandardError(totalScore.get(0), totalScore.get(playerIndex),
                    totalScoreSquared.get(0), totalScoreSquared.get(playerIndex), gamesPlayed.get(0), gamesPlayed.get(playerIndex));
            System.out.printf("Processing Agent %s (%d) with score %.3f, diff to base of %.3f +/- %.3f %n",
                    stuff.b, stuff.a, score, score - baseAgentScore, stdError);
            if (score > baseAgentScore + adjustedZScore * stdError && score - adjustedZScore * stdError > bestLowerBound) {
                // best so far
                bestLowerBound = score - adjustedZScore * stdError;
                selectedPlayer = playerIndex;
            } else if (score > baseAgentScore) {
                // check for default settings
                boolean allDefault = IntStream.range(0, playerSettings.get(playerIndex).size())
                        .allMatch(i ->  playerSettings.get(playerIndex).get(i).equals(baseSettings[i])
                                || playerSettings.get(playerIndex).get(i).equals(defaultSettings[i]));
                if (allDefault && score > bestDefaultScore) {
                    bestDefaultScore = score;
                    bestDefaultSettings = playerSettings.get(playerIndex).stream().mapToInt(Integer::intValue).toArray();
                }
            }
        }
        if (selectedPlayer == -1) {
            System.out.println("No significant improvement found");
            if (bestDefaultScore > -Double.MAX_VALUE) {
                System.out.printf("Best default settings agent is %s with score %.3f%n", getAgentName(bestDefaultSettings, baseSettings), bestDefaultScore);
                return bestDefaultSettings;
            } else {
                return baseSettings;
            }
        } else {
            int finalSelectedPlayer = selectedPlayer;
            Object playerSelected = players.stream().filter(data -> data.a == finalSelectedPlayer).findFirst().get().b;
            System.out.printf("Selected agent is %s with score %.3f%n", playerSelected, totalScore.get(selectedPlayer) / gamesPlayed.get(selectedPlayer));
            return playerSettings.get(selectedPlayer).stream().mapToInt(Integer::intValue).toArray();
        }
    }

    private String getAgentName(int[] settings, int[] baseSettings) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < settings.length; i++) {
            if (settings[i] == baseSettings[i]) {
                continue;
            }
            sb.append(searchSpace.name(i)).append(": ").append(searchSpace.allValues(i).get(settings[i])).append(", ");
        }
        // remove last comma and space
        sb.delete(sb.length() - 2, sb.length());
        return sb.toString();
    }
}
