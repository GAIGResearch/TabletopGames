package evaluation;

import org.apache.hadoop.yarn.webapp.hamlet.HamletSpec;
import org.json.simple.JSONObject;

import java.io.File;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static utilities.Utils.getArg;

public enum RunArg {
    NTBEAMode("Defaults to NTBEA. The other options are StableNTBEA, MultiNTBEA and CoopNTBEA.\n" +
            "The default runs a single game for each iteration. CoopNTBEA uses the same agent for all players.\n" +
            "StableNTBEA runs P (number of players) for a given random seed, with the tuned agent in each position.\n" +
            "This is useful for games with strong positional or random seed effects to reduce variance.\n" +
            "MultiNTBEA is deprecated and should not be used.",
            "NTBEA",
            new Usage[]{Usage.ParameterSearch}),
    addTimeStamp("(Optional) If true (default is false), then the results will be written to a subdirectory of destDir.\n" +
            "\t This may be useful if you want to use the same destDir for multiple experiments.",
            false,
            new Usage[]{Usage.RunGames}),
    budget("The budget to be used by all agent (if they support the IAnyTime interface). \n" +
            "\t If non-zero then this will override the value in any JSON definitions.\n",
            0,
            new Usage[]{Usage.RunGames, Usage.ParameterSearch}),
    byTeam("If true (the default) and the game supports teams, then one player type will be assigned to all players on a team.\n" +
            "\t If false, then each player will be assigned a player type independently.",
            true,
            new Usage[]{Usage.RunGames, Usage.ParameterSearch}),
    config("The location of a JSON file from which to read the configuration. \n" +
            "\t If this is specified, then all other arguments are ignored.",
            "",
            new Usage[]{Usage.RunGames, Usage.ParameterSearch}),
    destDir("The directory to which the results will be written. Defaults to 'metrics/out'.\n" +
            "\t If (and only if) this is being run for multiple games/player counts, then a subdirectory\n" +
            "\t will be created for each game, and then within that for  each player count combination.",
            "metrics" + File.separator + "out",
            new Usage[]{Usage.RunGames, Usage.ParameterSearch}),
    distinctRandomSeeds("If non-zero, then this defines the number of distinct random seeds to use for each game.\n" +
            "\t For tournament will be run for each individual random seed individually, using the other specified parameters.\n" +
            "\t If a seedFile is specified, then this is ignored.",
            0,
            new Usage[]{Usage.RunGames}),
    evalGames("The number of games to run with the best predicted setting to estimate its true value (default is 20% of NTBEA iterations)",
            -1,
            new Usage[]{Usage.ParameterSearch}),
    evalMethod("Score|Ordinal|Heuristic|Win specifies what we are optimising (if not tuneGame). Defaults to Win.\n" +
            "\tIf tuneGame, then instead the name of a IGameHeuristic class in the evaluation.heuristics package\n" +
            "\tmust be provided, or the a json-format file that provides the requisite details. \n" +
            "\tThe json-format file is needed if non-default settings for the IGameHeuristic are used.",
            "Win",
            new Usage[]{Usage.ParameterSearch}),
    focusPlayer("(Optional) A JSON file that defines the 'focus' of the tournament.\n" +
            "\t The 'focus' player will be present in every single game.\n" +
            "\t In this case an equal number of games will be run with the focusPlayer\n" +
            "\t in each position. The other positions will be filled randomly from players.",
            "",
            new Usage[]{Usage.RunGames}),
    game("A list of the games to be played. If there is more than one, then use a \n" +
            "\t pipe-delimited list, for example game=Uno|ColtExpress|Pandemic.\n" +
            "\t The default is 'all' to indicate that all games should be analysed.\n" +
            "\t Specifying all|-name1|-name2... will run all games except for name1, name2...",
            "all",
            new Usage[]{Usage.RunGames, Usage.ParameterSearch}),
    gameParams("(Optional) A JSON file from which the game parameters will be initialised.",
            "",
            new Usage[]{Usage.RunGames, Usage.ParameterSearch}),
    grid("If true, then we compare the current best agent against all previous budget levels. \n" +
            "\t If false, then we just compare against the previous budget level.\n",
            false,
            new Usage[]{Usage.SkillLadder}),
    gridMinorStart("""
            Only relevant if grid is true, and gridStart != 0
            \tThis is the minor grid budget start level.\s
            """,
            0,
            new Usage[]{Usage.SkillLadder}),
    gridStart("If provided we start calculating from the specified budget level. \n" +
            "\tThe default is to start from the startBudget, which is always where\n" +
            "\t the number of iterations are calculated from.\n",
            0,
            new Usage[]{Usage.SkillLadder}),
    iterations("The number of iterations of NTBEA to run (default is 1000)",
            1000,
            new Usage[]{Usage.ParameterSearch, Usage.SkillLadder}),
    kExplore("The k to use in NTBEA - defaults to 1.0 - this makes sense for win/lose games with a score in {0, 1}\n" +
            "\tFor scores with larger ranges, we recommend scaling kExplore appropriately.",
            1.0,
            new Usage[]{Usage.ParameterSearch}),
    listener("The full class name of an IGameListener implementation. Or, better, the location\n" +
            "\t of a json file from which a listener can be instantiated.\n" +
            "\t Defaults to evaluation.metrics.MetricsGameListener. \n" +
            "\t A pipe-delimited string can be provided to gather many types of statistics \n" +
            "\t from the same set of games.",
            "metrics/MetricsGameListener.json",
            new Usage[]{Usage.RunGames, Usage.ParameterSearch}),
    matchups("The total number of matchups to run in a tournament.\n" +
            "\tIf the mode is 'exhaustive', then this will be the maximum number of games run. TAG will divide\n" +
            "\tthis by the total number of permutations, and run an equal number of games for each permutation.\n" +
            "\tFor NTBEA this will be used as a final tournament between the recommended agents from each run.",
            1,
            new Usage[]{Usage.RunGames, Usage.ParameterSearch, Usage.SkillLadder}),
    mode("exhaustive|exhaustiveSP|random|sequential\n" +
            "\t 'exhaustive' will iterate exhaustively through every possible permutation: \n" +
            "\t every possible player in every possible position, and run an equal number of games'\n" +
            "\t for each. This can be unworkable for a given matchup budget for a large number of players.\n" +
            "\t 'exhaustiveSP' is the same as exhaustive, but allows for self-play; 'exhaustive' will not duplicate agents in any game.\n"+
            "\t 'random' will have a random matchup, while ensuring no duplicates, and that all players get the\n" +
            "\t the same number of games in total. (Unless the number of agents is less than the number of players, \n" +
            "\t in which case self-play will be allowed.)\n" +
            "\t 'sequential' will run tournaments on a ONE_VS_ALL basis between each pair of agents.\n" +
            "\t If a focusPlayer is provided, then 'mode' is ignored.",
            "random",
            new Usage[]{Usage.RunGames}),
    multiplier("The multiplier for budget at each iteration of the SkillLadder process. \n",
            2,
            new Usage[]{Usage.SkillLadder}),
    nPlayers("The number of players in each game. Overrides playerRange.",
            -1,
            new Usage[]{Usage.ParameterSearch, Usage.RunGames}),
    neighbourhood("The size of neighbourhood to look at in NTBEA. Default is min(50, |searchSpace|/100) ",
            50,
            new Usage[]{Usage.ParameterSearch}),
    opponent("The json specification of the opponent to be used. \n" +
            "\t If not specified, then a random player will be used.",
            "random",
            new Usage[]{Usage.ParameterSearch}),
    playerDirectory("The directory containing agent JSON files for the competing Players\n" +
            "\t If not specified, this defaults to very basic OSLA, RND, RHEA and MCTS players.",
            "",
            new Usage[]{Usage.RunGames}),
    playerRange("The total number of players in each game (the default is 'all') \n " +
            "\t A range can also be specified, for example 3-5. \n " +
            "\t Different player counts can be specified for each game in pipe-delimited format.\n" +
            "\t If 'all' is specified, then every possible playerCount for the game will be analysed.\n" +
            "\t Exception: if no player directory is set, then maximum number of players per game will be 5.",
            "all",
            new Usage[]{Usage.RunGames}),
    randomGameParams("(Optional) If specified, parameters for the game will be randomized for each game, and printed before the run.",
            false,
            new Usage[]{Usage.RunGames}),
    repeats("The number of times the whole process should be re-run, to find a single best recommendation ",
            1,
            new Usage[]{Usage.ParameterSearch}),
    reportPeriod("(Optional) For random mode execution only, after how many games played results are reported.\n" +
            "\t Defaults to the end of the tournament (-1)",
            -1,
            new Usage[]{Usage.RunGames}),
    searchSpace("The json-format file of the search space to use. No default.",
            "",
            new Usage[]{Usage.ParameterSearch}),
    seed("(Optional) Random seed to use for process. This is not the seed used for games, but the seed of \n" +
            "\t the random number generator used to generate these.",
            System.currentTimeMillis(),
            new Usage[]{Usage.RunGames, Usage.ParameterSearch}),
    seedFile("(Optional) A file containing a list of random seeds to use for individual games. \n" +
            "\t If this is specified, then the 'seed' and `distinctRandomSeed` arguments are ignored. \n" +
            "\t Each seed will be used in turn for a full tournament run, as defined by the other parameters.",
            "",
            new Usage[]{Usage.RunGames}),
    startBudget("The starting budget for the SkillLadder process. \n",
            8,
            new Usage[]{Usage.SkillLadder}),
    tuneGame("If true, then we will tune the game instead of tuning the agent.\n" +
            "\tIn this case the searchSpace file must be relevant for the game.",
            false,
            new Usage[]{Usage.ParameterSearch}),
    tuningBudget("The number of games to be played in total for each tuning process. \n" +
            "\t One tuning process is run for each iteration of SkillLadder.\n",
            1000,
            new Usage[]{Usage.SkillLadder}),
    finalPercent("The proportion of the tuningBudget used to determine the best agent across each iteration. \n" +
            "\t The remainder is divided amongst the iterations.\n",
            0.5,
            new Usage[]{Usage.SkillLadder}),
    useThreeTuples("If true then we use 3-tuples as well as 1-, 2- and N-tuples",
            false,
            new Usage[]{Usage.ParameterSearch}),
    verbose("If true, then the result of each game is reported. Default is false.",
            false,
            new Usage[]{Usage.RunGames, Usage.ParameterSearch});

    public final String helpText;
    public final Object defaultValue;
    public Object value;
    private final Usage[] when;


    public enum Usage {
        RunGames, ParameterSearch, SkillLadder
    }

    RunArg(String helpText, Object defaultValue, Usage[] when) {
        this.helpText = helpText;
        this.defaultValue = defaultValue;
        this.when = when;
    }

    public boolean isUsedIn(Usage usage) {
        return Arrays.asList(when).contains(usage);
    }

    public Object parse(String[] args) {
        value = getArg(args, name(), defaultValue);
        if (this == listener) {
            value = new ArrayList<>(Arrays.asList(((String) value).split("\\|")));
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public Object parse(JSONObject json) {
        value = json.getOrDefault(name(), defaultValue);
        if (value instanceof Long) {
            value = ((Long) value).intValue();
        }
        if (this == listener) {
            value = new ArrayList<>(Arrays.asList(((String) value).split("\\|")));
        }
        return value;
    }

    public static Map<RunArg, Object> parseConfig(String[] args, List<Usage> usages) {
        return RunArg.parseConfig(args, usages, true);
    }

    public static Map<RunArg, Object> parseConfig(String[] args, List<Usage> usages, boolean checkUnknownArgs) {
        if (checkUnknownArgs)
            checkUnknownArgs(args, usages);
        return Arrays.stream(RunArg.values())
                .filter(arg -> usages.stream().anyMatch(arg::isUsedIn))
                .collect(toMap(arg -> arg, arg -> arg.parse(args)));
    }

    public static void checkUnknownArgs(String[] args, List<Usage> usages) {
        List<String> possibleArgs = Arrays.stream(RunArg.values())
                .filter(arg -> usages.stream().anyMatch(arg::isUsedIn))
                .map(RunArg::name)
                .toList();
        List<String> keys = Arrays.stream(args).map(s -> s.split("=")[0]).toList();
        keys.stream().filter(arg -> !possibleArgs.contains(arg))
                .forEach(arg -> System.out.println("Unknown argument: " + arg));
    }

    @SuppressWarnings("unchecked")
    public static Map<RunArg, Object> parseConfig(JSONObject json, Usage usage) {
        String[] keyNames = (String[]) json.keySet().stream().map(Object::toString).toArray(String[]::new);
        checkUnknownArgs(keyNames, Collections.singletonList(usage));
        return Arrays.stream(RunArg.values())
                .filter(arg -> arg.isUsedIn(usage))
                .collect(toMap(arg -> arg, arg -> arg.parse(json)));
    }

    @SuppressWarnings("unchecked")
    public static Map<RunArg, Object> parseConfig(JSONObject json, List<Usage> usages) {
        String[] keyNames = (String[]) json.keySet().stream().map(Object::toString).toArray(String[]::new);
        checkUnknownArgs(keyNames, usages);
        return Arrays.stream(RunArg.values())
                .filter(arg -> usages.stream().anyMatch(arg::isUsedIn))
                .collect(toMap(arg -> arg, arg -> arg.parse(json)));
    }

    public static void printHelp(Usage in) {
        System.out.println("There are a number of possible arguments:");
        for (RunArg arg : RunArg.values()) {
            if (arg.isUsedIn(in))
                System.out.println("\t" + arg.name() + "= " + arg.helpText + "\n");
        }
    }
}
