package evaluation.optimisation;

import games.GameType;
import java.util.*;
import static utilities.Utils.getArg;

public class ParameterSearch {

    public static void main(String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.isEmpty() || argsList.contains("--help") || argsList.contains("-h")) System.out.println(
                "The first two arguments must be supplied\n" +
                        "\tsearchSpace=   The json-format file of the search space to use. No default.\n" +
                        "\tgame=          The game to be used for tuning. No default.\n" +
                        "\tnPlayers=      The total number of players in each game (the default is game.Min#players) \n " +
                        "\tnGames=        The number of iterations of NTBEA to run (default is 1000) \n" +
                        "\tevalGames=     The number of games to run with the best predicted setting to estimate its true value (default is 20% of NTBEA iterations) \n" +
                        "\trepeat=        The number of times NTBEA should be re-run, to find a single best recommendation \n" +
                        "\ttournament=    The number of games to run as a tournament with the winners of all NTBEA iterations (default is 0) \n" +
                        "\topponent=      The agent used as opponent. Default is random. \n" +
                        "\t               This can any of: \n" +
                        "\t               \ta json-format file detailing the parameters, or\n" +
                        "\t               \tone of coop|mcts|rmhc|random|osla|<className>, or\n" +
                        "\t               \ta directory that contains one or more json-format files from which opponents will be sampled.\n" +
                        "\t               If tuneGame is set, then the opponent argument must be provided, and will be used for all players.\n" +
                        "\tgameParam=     The json-format file of game parameters to use. Defaults to standard rules and options.\n" +
                        "\ttuneGame=      If true, then we will tune the game instead of tuning the agent.\n" +
                        "\t               In this case the searchSpace file must be relevant for the game.\n" +
                        "\teval=          Score|Ordinal|Heuristic|Win specifies what we are optimising (if not tuneGame). Defaults to Win.\n" +
                        "\t               If tuneGame, then instead the name of a IGameHeuristic class in the evaluation.heuristics package\n" +
                        "\t               must be provided, or the a json-format file that provides the requisite details. \n" +
                        "\t               The json-format file is needed if non-default settings for the IGameHeuristic are used.\n" +
                        "\tuseThreeTuples= If true then we use 3-tuples as well as 1-, 2- and N-tuples \n" +
                        "\tkExplore=      The k to use in NTBEA - defaults to 1.0 - this makes sense for win/lose games with a score in {0, 1}\n" +
                        "\t               For scores with larger ranges, we recommend scaling kExplore appropriately.\n" +
                        "\thood=          The size of neighbourhood to look at in NTBEA. Default is min(50, |searchSpace|/100) \n" +
                        "\tverbose=       Will log the results marginalised to each dimension, and the Top 10 best tuples for each run \n" +
                        "\tseed=          Random seed for Game use (not used by NTBEA itself). Defaults to System.currentTimeMillis()\n" +
                        "\tlogFile=       Output file with results of each run for easier statistical analysis\n" +
                        "\tmode=          Defaults to NTBEA. The other options are MultiNTBEA and CoopNTBEA. This last uses the same agent for all players.\n" +
                        "\tlistener=      A JSON file that defines an IGameListener to be used. A pipe-delimited list of such files can be provided.\n"
        );

        GameType game = GameType.valueOf(getArg(args, "game", "GameTemplate"));
        if (game == GameType.GameTemplate) {
            System.out.println("No game provided. Please provide a game.");
            return;
        }
        int nPlayers = getArg(args, "nPlayers", game.getMinPlayers());
        String searchSpaceFile = getArg(args, "searchSpace", "");
        if (searchSpaceFile.equals("")) {
            System.out.println("No search space file provided. Please provide a search space file.");
            return;
        }

        NTBEAParameters params = new NTBEAParameters(args);
        params.printSearchSpaceDetails();

        if (params.mode == NTBEAParameters.Mode.MultiNTBEA) {
            MultiNTBEA multiNTBEA = new MultiNTBEA(params, game, nPlayers);
            multiNTBEA.run();
        } else {
            NTBEA singleNTBEA = new NTBEA(params, game, nPlayers);
            singleNTBEA.run();
        }
    }


}
