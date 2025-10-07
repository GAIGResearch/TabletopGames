package games.powergrid;

import games.GameType;
import games.powergrid.PowerGridFeatures;
import games.diamant.*;

import core.Game;
import core.PyTAG;
import core.AbstractGameState;
import core.AbstractPlayer;
import players.python.PythonAgent;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import core.*;
import games.GameType;
import players.python.PythonAgent;   // ensure this exists in your classpath
import players.simple.RandomPlayer;

import java.util.*;



public class TestFeatures {
	

	    private static void printPretty(String[] names, double[] vec) {
	        int width = Arrays.stream(names).mapToInt(String::length).max().orElse(8) + 2;
	        for (int i = 0; i < names.length; i++) {
	            String n = (i < names.length) ? names[i] : "f" + i;
	            String v = (i < vec.length) ? String.valueOf(vec[i]) : "NA";
	            System.out.printf("%-" + width + "s : %s%n", n, v);
	        }
	    }

	    public static void main(String[] args) throws Exception {
	        long seed = 1234;
	        Random rnd = new Random(seed);

	        ArrayList<AbstractPlayer> players = new ArrayList<>();
	        players.add(new PythonAgent()); // learning slot
	        players.add(new RandomPlayer(rnd));
	        players.add(new RandomPlayer(rnd));
	        players.add(new RandomPlayer(rnd));

	        PyTAG env = new PyTAG(GameType.PowerGrid, null, players, seed, false);
	        env.reset();  // runs until PythonAgent must act

	        // Observation that Python will receive
	        double[] obs = env.getObservationVector();

	        // Get names without touching FeatureExtractors
	        String[] names = new PowerGridFeatures().names();

	        System.out.println("=== PyTAG observation ===");
	        printPretty(names, obs);

	        // Step once using a random legal action (masked)
	        int[] mask = env.getActionMask();
	        int randomAction = env.sampleRNDAction(mask, rnd);
	        env.step(randomAction);

	        double[] obs2 = env.getObservationVector();
	        System.out.println("\n=== After one env.step(...) ===");
	        printPretty(names, obs2);
	        
	        
	        env.step(randomAction);

	        double[] obs3 = env.getObservationVector();
	        System.out.println("\n=== After two env.step(...) ===");
	        printPretty(names, obs3);
	        

		     // Observation that Python will receive (JSON)
		     String json = env.getObservationJson();
		     System.out.println("\n=== PyTAG JSON observation ===");
		     System.out.println(json);
	    }
	}


