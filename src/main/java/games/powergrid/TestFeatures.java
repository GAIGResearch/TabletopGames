package games.powergrid;

import games.GameType;


import core.PyTAG;
import core.AbstractPlayer;
import core.CoreConstants;
import players.python.PythonAgent;
import players.simple.RandomPlayer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * TestFeatures is a standalone diagnostic and validation utility for the Power Grid environment 
 * within the TAG/PyTAG framework. 
 * <p>
 * It provides an executable test that interacts with the {@link PyTAG} wrapper to:
 * <ul>
 *   <li>Initialize and reset a Power Grid game environment with a configurable set of players.</li>
 *   <li>Iteratively step through a fixed number of game turns, sampling random legal actions 
 *       and executing them to ensure the environment behaves deterministically.</li>
 *   <li>Verify that the observation vector remains normalized within the [0, 1] interval, 
 *       throwing a fatal error if invalid feature values (NaN, infinite, or out-of-range) 
 *       are detected.</li>
 *   <li>Confirm that the action tree (leaf ordering and count) remains stable throughout gameplay, 
 *       ensuring consistency of the action mask structure.</li>
 *   <li>Print diagnostic information such as legal action masks, action names, chosen actions, 
 *       and final player results for inspection.</li>
 * </ul>
 * This class is typically used for debugging, regression testing, or environment validation 
 * before training reinforcement learning agents on the Power Grid domain.
 * <p>
 * This will run a full episode of Power Grid with user defined python vs mcts vs random agents.
 */

public class TestFeatures {


    private static void printMask(String title, int[] mask) {
        int legal = 0;
        for (int v : mask) if (v != 0) legal++;
        System.out.println(title + " legal actions: " + legal);
        System.out.println(title + " mask: " + java.util.Arrays.toString(mask));
    }


    private static void printWinners(PyTAG env, int nPlayers) {
        CoreConstants.GameResult[] results = env.getPlayerResults();
        System.out.println("Final player results:");
        for (int i = 0; i < results.length; i++) {
            System.out.printf("  Player %d: %s%n", i, results[i]);
        }
    }

    public static void main(String[] args) throws Exception {
        long seed = 10;
        Random rnd = new Random(seed);
        int numberOfSteps = 10;

        ArrayList<AbstractPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer(rnd));
        players.add(new PythonAgent()); 
        players.add(new RandomPlayer(rnd));
   
        final int N_PLAYERS = players.size();

        PyTAG env = new PyTAG(GameType.PowerGrid, null, players, seed, false);
        env.reset();  

        String[] names = new PowerGridFeatures().names();
        System.out.println("Obs (feature) size = " + names.length);

        List<String> headNames = env.getLeafNames();           
        System.out.println("Head size: " + headNames.size());
        printMask("INITIAL", env.getActionMask());
        printOnes("INITIAL", env.getActionMask(), headNames);

       
        for (int step = 1; step <= numberOfSteps; step++) {
            if (env.isDone()) {
                System.out.println("\nEpisode finished before step " + step);
                break;
            }
            try {
                double[] obs = env.getObservationVector();
                checkObsInUnitInterval("after reset/step " + (step - 1), obs);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            //displays the list of actions possible
            System.out.println("\n=== BEFORE step " + step + " ===");
            int[] maskBefore = env.getActionMask();
            assertStableHead(headNames, env.getLeafNames());
            printMask("BEFORE", maskBefore);
            printOnes("BEFORE", maskBefore, headNames);

            // Choose a valid action and show what it is
            int actionId = env.sampleRNDAction(maskBefore, rnd);
            String chosenName = env.getActionNameById(actionId);
            System.out.println("Chosen actionId: " + actionId + " -> " + chosenName);

            // Execute the chosen action
            env.step(actionId);
            
            //prints the tree showing the different levels of the tree
            System.out.println("\n=== AFTER step " + step + " ===");
            System.out.println("Shape: " + env.getTreeShape());

            // This is usually only the acting agent's reward; keep if you still want it
            try {
                System.out.println("env.getReward(): " + env.getReward());
            } catch (Throwable ignored) {}

            int[] maskAfter = env.getActionMask();
            assertStableHead(headNames, env.getLeafNames());
            printMask("AFTER", maskAfter);
            printOnes("AFTER", maskAfter, headNames);
        }

        System.out.println("\n=== FINAL RESULTS ===");
        printWinners(env, N_PLAYERS);
    }

    private static void printOnes(String title, int[] mask, List<String> names) {
        System.out.println(title + " on (index -> name):");
        for (int i = 0; i < mask.length; i++) {
            if (mask[i] == 1) {
                System.out.printf("  [%d] %s%n", i, names.get(i));
            }
        }
    }
    
    //makes sure the tree does not change shape or nodes move if it does it throws an exception 
    private static void assertStableHead(List<String> baseline, List<String> current) {
        if (baseline.size() != current.size()) {
            throw new AssertionError("Leaf count changed! " + baseline.size() + " -> " + current.size());
        }
        for (int i = 0; i < baseline.size(); i++) {
            if (!baseline.get(i).equals(current.get(i))) {
                throw new AssertionError("Leaf order/name changed at " + i + ": '"
                        + baseline.get(i) + "' -> '" + current.get(i) + "'");
            }
        }
    }

    //makes sure the observation is normalized between 0 and 1 
    public static boolean checkObsInUnitInterval(String label, double[] obs) {
        boolean ok = true;
        for (int i = 0; i < obs.length; i++) {
            double v = obs[i];
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                System.err.printf("OBS NUMERIC ERROR (%s): idx=%d value=%s%n", label, i, String.valueOf(v));
                ok = false;
                continue;
            }
            if (v < 0.0 || v > 1.0) {
                System.err.printf("OBS RANGE ERROR   (%s): idx=%d value=%.6f (expected in [0,1])%n", label, i, v);
                ok = false;
            }
        }
        if (!ok) {
            System.err.printf("Observation check FAILED (%s): %d features checked%n", label, obs.length);
            System.exit(0);
        }
        return ok;
    }
}