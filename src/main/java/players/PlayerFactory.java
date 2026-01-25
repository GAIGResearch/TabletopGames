package players;

import core.AbstractPlayer;
import evaluation.optimisation.TunableParameters;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import players.basicMCTS.BasicMCTSPlayer;
import players.rhea.RHEAParams;
import players.rhea.RHEAPlayer;
import players.rmhc.RMHCParams;
import players.rmhc.RMHCPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;
import utilities.JSONUtils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static utilities.JSONUtils.parser;

/**
 * Factory class for creating AbstractPlayers from JSON configuration file.
 * All three methods return an AbstractPlayer, with the configuration defined in a JSON file, a JSONObject
 * or a JSON-format String respectively.
 * <p>
 * The crucial property in the JSON file is class:
 * "class" : "players.mcts.MCTSParams"
 * "class" : "players.simple.RandomPlayer"
 * "class" : "players.simple.OSLAPlayer"
 * <p>
 * This must either be the class name of an AbstractPlayer implementation which has a no-argument constructor
 * (OSLAPlayer and RandomPlayer in the example above)
 * or the class name of a TunableParameters implementation which returns an AbstractPlayer from instantiate()
 * <p>
 * "random" and "osla" require no further properties.
 * "heuristic" requires a further property of:
 * "class" : "<fullNameOfClassThatImplementsAbstractPlayerWithANoArgumentConstructor>"
 */
public class PlayerFactory {

    /**
     * This provides the main access point for generating a new AbstractPlayer
     * The input can be one of a few things:
     * 1) A JSON file - in which case this is used to generate a player using fromJSONFile()
     * 2) A simple String with any of:
     * "mcts", "rmhc", "osla", "random", "className"
     * The first four of these will return the appropriate player with default parameters
     * Anything else is interpreted as a class name that implements AbstractPlayer with a no-argument constructor
     */
    public static AbstractPlayer createPlayer(String data) {
        File f = new File(data);
        if (f.exists()) {
            Object instantiatedObject = JSONUtils.loadClassFromFile(data);
            if (instantiatedObject instanceof TunableParameters<?> parameters) {
                instantiatedObject = parameters.instantiate();
            }
            if (!(instantiatedObject instanceof AbstractPlayer)) {
                throw new AssertionError("The file " + data + " does not contain a valid AbstractPlayer or TunableParameters class.");
            }
            AbstractPlayer retValue = (AbstractPlayer) instantiatedObject;
            retValue.setName(data.substring(0, data.indexOf(".")));
            return retValue;
        }

        // if we get here then the file does not exist, and check some hard-coded strings
        String input = data.toLowerCase();
        return switch (input) {
            case "random" -> new RandomPlayer();
            case "osla" -> new OSLAPlayer();
            case "mcts" -> new BasicMCTSPlayer();
            case "rmhc" -> new RMHCPlayer(new RMHCParams());
            case "rhea" -> new RHEAPlayer(new RHEAParams());
            default -> throw new AssertionError("Unknown file or player key : " + input);
        };
    }

    public static List<AbstractPlayer> createPlayers(String opponentDescriptor) {
        List<AbstractPlayer> retValue = new ArrayList<>();
        File od = new File(opponentDescriptor);
        if (od.exists() && od.isDirectory()) {
            for (String fileName : Objects.requireNonNull(od.list())) {
                if (!fileName.endsWith(".json"))
                    continue;
                AbstractPlayer player = PlayerFactory.createPlayer(od.getAbsolutePath() + File.separator + fileName);
                retValue.add(player);
                player.setName(fileName.substring(0, fileName.indexOf(".")));
            }
        } else {
            return Collections.singletonList(createPlayer(opponentDescriptor));
        }
        return retValue;
    }
}
