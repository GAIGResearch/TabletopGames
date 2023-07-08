package evaluation.optimisation;

import evaluation.RunArg;
import games.GameType;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static evaluation.RunArg.game;
import static evaluation.RunArg.parseConfig;
import static utilities.Utils.getArg;

public class ParameterSearch {

    public static void main(String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.isEmpty() || argsList.contains("--help") || argsList.contains("-h")) {
            System.out.println("There are a number of possible arguments:");
            for (RunArg arg : RunArg.values()) {
                System.out.println("\t" + arg.name() + "= " + arg.helpText + "\n");
            }
            return;
        }

        // Config
        Map<RunArg, Object> config;


        String setupFile = getArg(args, "config", "");
        if (!setupFile.equals("")) {
            // Read from file instead
            try {
                FileReader reader = new FileReader(setupFile);
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(reader);
                config = parseConfig(json, RunArg.Usage.ParameterSearch);
            } catch (FileNotFoundException ignored) {
                throw new IllegalArgumentException("Could not find file: " + setupFile);
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        } else {
            config = parseConfig(args, RunArg.Usage.ParameterSearch);
        }

        if (config.get(RunArg.game).equals("all")) {
            System.out.println("No game provided. Please provide a game.");
            return;
        }
        GameType game = GameType.valueOf(config.get(RunArg.game).toString());
        if (game == GameType.GameTemplate) {
            System.out.println("No game provided. Please provide a game.");
            return;
        }
        int nPlayers = (int) config.get(RunArg.nPlayers);
        if (nPlayers < game.getMinPlayers() || nPlayers > game.getMaxPlayers()) {
            System.out.println("Invalid number of players for game " + game + ". Please provide a valid number of players.");
            return;
        }
        String searchSpaceFile = config.get(RunArg.searchSpace).toString();
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
