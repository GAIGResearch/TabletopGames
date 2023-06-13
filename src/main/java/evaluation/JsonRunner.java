package evaluation;

import core.interfaces.IGameRunner;
import utilities.Utils;

/**
 * Receives a JSON config file as argument and runs the evaluation. Can run:
 *  - Game Report {@link evaluation.GameReport} e.g. json/experiments/gamereport.json
 *  - Tournaments {@link evaluation.tournaments.RoundRobinTournament} e.g. json/experiments/tournament.json
 */
public class JsonRunner {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -jar evaluation.jar config=<config.json>");
            System.exit(1);
        }
        String configPath = Utils.getArg(args, "config", "json/experiments/gamereport.json");
        IGameRunner runner = Utils.loadClassFromFile(configPath);
        runner.run();
    }
}
