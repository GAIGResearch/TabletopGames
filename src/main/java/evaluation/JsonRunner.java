package evaluation;

import core.interfaces.IGameRunner;
import utilities.Utils;

/**
 * Receives a JSON config file as argument and runs the evaluation. Can run:
 *  - Game Report
 *  - Round Robin Tournament
 *  - Parameter search
 */
public class JsonRunner {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -jar evaluation.jar <config.json>");
            System.exit(1);
        }
        String configPath = args[0];
        IGameRunner runner = Utils.loadClassFromFile(configPath);
        runner.run();
    }
}
