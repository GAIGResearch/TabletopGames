package evaluation;

import core.AbstractPlayer;
import games.GameType;
import players.PlayerConstants;
import players.PlayerFactory;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

import static evaluation.RunArg.*;

/**
 * Runs a single MCTS agent in self-play at each of several time budgets, recording MCTS metrics.
 * <p>
 * RunGames is called once per budget, so all RunGames arguments (game, playerRange, matchups, listener...) apply.
 * Output is written to destDir/Game/Budget/N-players, e.g. metrics/out/Dominion/100ms/3-players.
 * <p>
 * The agent is given by playerDirectory (a JSON file or player name); if absent the default MCTSPlayer is used.
 * If no listener is given, json/listeners/MCTSMetricsListener.json is used.
 * If budget is given, only that budget (in ms) is run instead of BUDGETS_MS.
 * If gameParams is given, its file name is appended to the game directory, e.g. Catan_NoTrading.
 */
public class MCTSBudgetSweep {

    public static final int[] BUDGETS_MS = {10, 100, 1000};
    public static final String DEFAULT_LISTENER = "json/listeners/MCTSSweepListener.json";

    public static void main(String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.contains("--help") || argsList.contains("-h")) {
            RunArg.printHelp(Usage.RunGames);
            return;
        }

        Map<RunArg, Object> config = parseConfig(args, Collections.singletonList(Usage.RunGames));
        if (argsList.stream().noneMatch(a -> a.startsWith(listener.name() + "=")))
            config.put(listener, new ArrayList<>(List.of(DEFAULT_LISTENER)));
        config.put(mode, "random");  // with a single agent, random mode gives self-play in every seat

        String agentSpec = (String) config.get(playerDirectory);
        AbstractPlayer agent = agentSpec.isEmpty() ? new MCTSPlayer() : PlayerFactory.createPlayer(agentSpec);
        if (!(agent instanceof MCTSPlayer mctsPlayer))
            throw new IllegalArgumentException("Agent must be an MCTSPlayer, but was " + agent.getClass().getSimpleName());
        MCTSParams params = mctsPlayer.getParameters();
        params.budgetType = PlayerConstants.BUDGET_TIME;
        params.setParameterValue("budgetType", PlayerConstants.BUDGET_TIME);

        // A game variant (gameParams) is written alongside the standard game, e.g. Catan_NoTrading
        String paramsFile = (String) config.get(gameParams);
        String paramsSuffix = paramsFile.isEmpty() ? "" : "_" + new File(paramsFile).getName().replaceFirst("\\.json$", "");

        int budgetArg = (int) config.get(budget);
        int[] budgets = budgetArg > 0 ? new int[]{budgetArg} : BUDGETS_MS;
        for (int budgetMs : budgets) {
            System.out.printf("Budget: %d ms%n", budgetMs);
            config.put(budget, budgetMs);
            String budgetDir = budgetMs + "ms";
            RunGames runGames = new RunGames(config, List.of(agent)) {
                @Override
                protected List<String> outputDirectories(GameType gameType, int playerCount) {
                    String outputDir = (String) config.get(destDir);
                    List<String> directories = new ArrayList<>(Arrays.asList(outputDir.split(Pattern.quote(File.separator))));
                    directories.add(gameType.name() + paramsSuffix);
                    directories.add(budgetDir);
                    directories.add(playerCount + "-players");
                    return directories;
                }
            };
            runGames.run();
        }
    }
}
