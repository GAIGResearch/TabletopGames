package evaluation;

import games.GameType;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import players.mcts.MCTSParams;
import players.mcts.MCTSSearchSpace;

import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static utilities.Utils.getArg;

public class ParameterSearch {

    public static void main(String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.contains("--help") || argsList.contains("-h")) System.out.println(
                "The first three arguments must be \n" +
                        "\t<filename for searchSpace definition>\n" +
                        "\t<number of NTBEA iterations>\n" +
                        "\t<game type>" +
                        "Then there are a number of optional arguments:\n" +
                        "\tbaseAgent=     The filename for the baseAgent (from which the searchSpace definition deviates)" +
                        "\topponent=      The filename for the agent used as the opponent" +
                        "\tGameParams=    The filename with game params to use" +
                        "\tuseThreeTuples If specified then we use 3-tuples as well as 1-, 2- and N-tuples" +
                        "\tkExplore=      The k to use in NTBEA - defaults to 100.0" +
                        "\thood=          The size of neighbourhood to look at in NTBEA. Default is min(50, |searchSpace|/100)" +
                        "\trepeat=        The number of times NTBEA should be re-run, to find a single best recommendation"
        );

        if (argsList.size() < 3)
            throw new AssertionError("Must specify at least three parameters: searchSpace, NTBEA iterations, game");
        String searchSpaceFile = args[0];
        int iterationsPerRun = Integer.valueOf(args[1]);
        GameType game = GameType.valueOf(args[2]);
        int repeats = getArg(args, "repeat", 1);
        double kExplore = getArg(args, "kExplore", 100.0);

        //TODO: Convert SearchSpace file to be from JSON (once NTBEA code allows that)
        // TODO: Replace default MCTSPArams with the baseParams from command line
        MCTSSearchSpace searchSpace = new MCTSSearchSpace(new MCTSParams(System.currentTimeMillis()), searchSpaceFile);
        int searchSpaceSize = IntStream.range(0, searchSpace.nDims()).reduce(1, (acc, i) -> acc * searchSpace.nValues(i));
        int twoTupleSize = IntStream.range(0, searchSpace.nDims() - 1)
                .map(i -> searchSpace.nValues(i) *
                        IntStream.range(i + 1, searchSpace.nDims())
                                .map(searchSpace::nValues).sum()
                ).sum();
        int threeTupleSize = IntStream.range(0, searchSpace.nDims() - 2)
                .map(i -> searchSpace.nValues(i) *
                        IntStream.range(i + 1, searchSpace.nDims()).map(j ->
                                searchSpace.nValues(j) * IntStream.range(j + 1, searchSpace.nDims()).map(searchSpace::nValues).sum()
                        ).sum()
                ).sum();

        int hood = getArg(args, "hood", Math.min(50, searchSpaceSize / 100));
        boolean useThreeTuples = Arrays.asList(args).contains("useThreeTuples");

        System.out.println(String.format("Search space consists of %d states and %d possible 2-Tuples%s",
                searchSpaceSize, twoTupleSize, useThreeTuples ? String.format(" and %d 3-Tuples", threeTupleSize) : ""));

        for (int i = 0; i < searchSpace.nDims(); i++) {
            int finalI = i;
            String allValues = IntStream.range(0, searchSpace.nValues(i)).mapToObj(j -> searchSpace.value(finalI, j)).map(Object::toString).collect(Collectors.joining(", "));
            System.out.println(String.format("%20s has %d values %s", searchSpace.name(i), searchSpace.nValues(i), allValues));
        }
    }
}
