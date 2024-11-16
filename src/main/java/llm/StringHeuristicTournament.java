package llm;

import core.AbstractPlayer;
import evaluation.RunArg;
import evaluation.listeners.IGameListener;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import players.PlayerFactory;
import players.heuristics.StringHeuristic;
import players.search.MaxNSearchPlayer;
import utilities.Utils;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import static evaluation.RunArg.*;

public class StringHeuristicTournament {

    public static void main(String[] args) {

        // We take in all the configuration parameters suitable for RunGames
        // and then we also need a config file with the StringHeuristic code details
        // Being a list of .java file, each of which needs to be read in, compiled, and
        // used in initialise a StringHeuristic player.

        String agentConfigFile = Utils.getArg(args, "rawJavaDetails", "RawJavaDetails.txt");
        // Then read this file, with each row having two fields (comma separated)
        // i) the name of the agent
        // ii) the path to the .java file

        File file = new File(agentConfigFile);
        if (!file.exists()) {
            System.out.println("File not found: " + agentConfigFile);
            return;
        }
        String agentConfig = Utils.getArg(args, "playerDirectory", "osla");

        GameType gameType = GameType.valueOf(Utils.getArg(args, "game", "TicTacToe"));
        int nPlayers = Integer.parseInt(Utils.getArg(args, "nPlayers", "2"));


        List<AbstractPlayer> players = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 2) {
                    System.out.println("Invalid line: " + line);
                    continue;
                }
                String agentName = parts[0].trim();
                String className = agentName + "_" + gameType.name();
                String fileName = parts[1].trim();
         //       String className = fileName.replaceAll(".*(/|\\\\)(.*?)\\.java", "$2");
                StringHeuristic heuristic = new StringHeuristic(fileName, className);

                // To get the player from the StringHeuristic we rely on implementation of IHasStateHeuristic
                AbstractPlayer player = PlayerFactory.createPlayer(agentConfig);
                if (player instanceof IHasStateHeuristic playerWithHeuristic) {
                    playerWithHeuristic.setStateHeuristic(heuristic);
                } else {
                    throw new IllegalArgumentException("Player does not support IHasStateHeuristic: " + player.getClass());
                }
                player.setName(agentName);
                players.add(player);
            }

            Map<RunArg, Object> config = parseConfig(args, Collections.singletonList(RunArg.Usage.RunGames));

            RoundRobinTournament tournament = new RoundRobinTournament(players, gameType, nPlayers, null, config);
            //noinspection unchecked
             for (String listenerClass : ((List<String>) config.get(listener))) {
                IGameListener gameTracker = IGameListener.createListener(listenerClass);
                tournament.addListener(gameTracker);
                String outputDir = (String) config.get(destDir);
                gameTracker.setOutputDirectory(outputDir);
            }

            tournament.run();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
