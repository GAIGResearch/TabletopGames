package games.powergrid;

import games.GameType;


import core.PyTAG;
import core.AbstractPlayer;
import players.python.PythonAgent;
import players.simple.RandomPlayer;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;



public class TestFeatures {
	

    private static void printPretty(String[] names, double[] vec) {
        int width = Arrays.stream(names).mapToInt(String::length).max().orElse(8) + 2;
        for (int i = 0; i < names.length; i++) {
            String n = (i < names.length) ? names[i] : "f" + i;
            String v = (i < vec.length) ? String.valueOf(vec[i]) : "NA";
            System.out.printf("%-" + width + "s : %s%n", n, v);
        }
    }

    private static void printMask(String title, int[] mask) {
        int legal = 0;
        for (int v : mask) if (v != 0) legal++;
        System.out.println(title + " legal actions: " + legal);
        System.out.println(title + " mask: " + java.util.Arrays.toString(mask));
    }

    public static void main(String[] args) throws Exception {
    	
        long seed = 5;
        Random rnd = new Random(seed);
        int numberOfSteps = 10;

        ArrayList<AbstractPlayer> players = new ArrayList<>();
        players.add(new PythonAgent()); // learning slot
        players.add(new RandomPlayer(rnd));
        players.add(new RandomPlayer(rnd));


        PyTAG env = new PyTAG(GameType.PowerGrid, null, players, seed, false);
        env.reset();  // runs until PythonAgent must act

        // 🟢 Define feature names for printPretty
        String[] names = new PowerGridFeatures().names();
        System.out.println("Obs (feature) size = " + names.length);
        
        // After env.reset()
        List<String> headNames = env.getLeafNames();           // fixed order of leaves
        System.out.println("Head size: " + headNames.size());
        printMask("INITIAL", env.getActionMask());
        printOnes("INITIAL", env.getActionMask(), headNames);

        // Step loop
        for (int step = 1; step <= numberOfSteps; step++) {
            if (env.isDone()) {
                System.out.println("\nEpisode finished before step " + step);
                break;
            }
            try {
            	double[] obs = env.getObservationVector();
            	checkObsInUnitInterval("after reset", obs);
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("\n=== BEFORE step " + step + " ===");
            int[] maskBefore = env.getActionMask();
            // sanity: head stability
            assertStableHead(headNames, env.getLeafNames());
            printMask("BEFORE", maskBefore);
            printOnes("BEFORE", maskBefore, headNames);

            // Choose a valid action and show what it is
            int actionId = env.sampleRNDAction(maskBefore, rnd);
            String chosenName = env.getActionNameById(actionId);
            System.out.println("Chosen actionId: " + actionId + " -> " + chosenName);

            // Execute
            env.step(actionId);
            
            System.out.println("\n=== AFTER step " + step + " ===");
            //printPretty(names, env.getObservationVector());
            System.out.println("Shape: " + env.getTreeShape());
            System.out.println("Reward:" + env.getReward());
            int[] maskAfter = env.getActionMask();
            assertStableHead(headNames, env.getLeafNames());   // still stable
            printMask("AFTER", maskAfter);
            printOnes("AFTER", maskAfter, headNames);
        }
    }

    
    private static void printOnes(String title, int[] mask, List<String> names) {
        System.out.println(title + " on (index -> name):");
        for (int i = 0; i < mask.length; i++) {
            if (mask[i] == 1) {
                System.out.printf("  [%d] %s%n", i, names.get(i));
            }
        }
    }

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
        }
        if(!ok)System.exit(0);
        return ok;
    }


	}


