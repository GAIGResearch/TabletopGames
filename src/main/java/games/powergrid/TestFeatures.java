package games.powergrid;

import games.GameType;


import core.PyTAG;
import core.AbstractPlayer;
import players.python.PythonAgent;
import players.simple.RandomPlayer;


import java.util.ArrayList;
import java.util.Arrays;
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
        long seed = 1234;
        Random rnd = new Random(seed);
        int numberOfSteps = 100;

        ArrayList<AbstractPlayer> players = new ArrayList<>();
        players.add(new PythonAgent()); // learning slot
        players.add(new RandomPlayer(rnd));
        players.add(new RandomPlayer(rnd));
        players.add(new RandomPlayer(rnd));
        players.add(new RandomPlayer(rnd));
        players.add(new RandomPlayer(rnd));

        PyTAG env = new PyTAG(GameType.PowerGrid, null, players, seed, false);
        env.reset();  // runs until PythonAgent must act

        // Feature names (avoid FeatureExtractors visibility)
        String[] names = new PowerGridFeatures().names();

        System.out.println("=== Initial PyTAG observation ===");
        printPretty(names, env.getObservationVector());
        System.out.println("Shape: " + env.getTreeShape());
        //System.out.println("Tree mask: " + java.util.Arrays.toString(env.getActionTree()));
        printMask("INITIAL", env.getActionMask());   // <-- show initial action mask

        for (int step = 1; step <= numberOfSteps; step++) {
            if (env.isDone()) {
                System.out.println("\nEpisode finished before step " + step);
                break;
            }

            // BEFORE step: show current mask
            System.out.println("\n=== BEFORE step " + step + " ===");
            printMask("BEFORE", env.getActionMask());

            // Sample a valid action from the current mask
            int[] mask = env.getActionMask();
            int actionId = env.sampleRNDAction(mask, rnd);

            // Take the step
            env.step(actionId);

            // AFTER step: observation + tree + action mask
            System.out.println("\n=== AFTER step " + step + " ===");
            printPretty(names, env.getObservationVector());
            System.out.println("Shape: " + env.getTreeShape());
            System.out.println("Tree mask: " + java.util.Arrays.toString(env.getActionTree()));
            System.out.println("Tree mask Size: " + (env.getActionTree().length));
            printMask("AFTER", env.getActionMask());  // <-- show mask after state update
        }
    }
	}


