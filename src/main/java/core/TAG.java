package core;

import evaluation.ExpertIteration;
import evaluation.RunGames;
import evaluation.tournaments.SkillLadder;
import evaluation.optimisation.OneStepDeviations;
import evaluation.optimisation.ParameterSearch;
import gui.Frontend;
import gui.FrontendSimple;

import java.util.Arrays;

/**
 * Main entry point for the TAG framework.
 * This class allows running different sub-modules based on the first argument.
 */
public class TAG {

    public enum Entry {
        RunGames,
        ParameterSearch,
        FrontEnd,
        FrontEndSimple,
        ExpertIteration,
        OneStepDeviations,
        SkillLadder;

        public static Entry find(String name) {
            for (Entry e : Entry.values()) {
                if (e.name().equalsIgnoreCase(name)) return e;
            }
            return null;
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        Entry entry = Entry.find(args[0]);
        if (entry == null) {
            System.err.println("Unknown entry point: " + args[0]);
            printUsage();
            System.exit(1);
        }

        String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (entry) {
            case RunGames:
                RunGames.main(remainingArgs);
                break;
            case ParameterSearch:
                ParameterSearch.main(remainingArgs);
                break;
            case FrontEnd:
                Frontend.main(remainingArgs);
                break;
            case FrontEndSimple:
                FrontendSimple.main(remainingArgs);
                break;
            case ExpertIteration:
                ExpertIteration.main(remainingArgs);
                break;
            case OneStepDeviations:
                OneStepDeviations.main(remainingArgs);
                break;
            case SkillLadder:
                SkillLadder.main(remainingArgs);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + entry);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar TAG.jar <EntryPoint> [args...]");
        System.out.println("Available EntryPoints: " + Arrays.toString(Entry.values()));
    }
}
