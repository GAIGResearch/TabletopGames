package games.santorini;

import core.*;
import games.GameType;
import players.human.HumanConsolePlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;

public class SantoriniGame extends Game {
    public SantoriniGame(List<AbstractPlayer> agents, AbstractParameters gameParameters) {
        super(GameType.Santorini, agents, new SantoriniForwardModel(), new SantoriniGameState(gameParameters, agents.size()));
    }

    public static void main(String[] args){

        // create list of players
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new HumanConsolePlayer());
        agents.add(new HumanConsolePlayer());

        // Play n games and return the pct of wins of each player
        double [] playerWins = new double[agents.size()];
        for (int i=0; i<agents.size(); i++)
            playerWins[i] = 0.0;

        int n = 1;
        for (int i=0; i<n; i++) {
            System.out.println("GAME: " + i + " ");
            AbstractParameters gameParameters = new SantoriniParameters(System.currentTimeMillis());

            Game game = new SantoriniGame(agents, gameParameters);
            game.run(null);

            Utils.GameResult [] results =  game.getGameState().getPlayerResults();
            for (int j=0; j<agents.size(); j++)
                if (results[j] == Utils.GameResult.WIN)
                {
                    playerWins[j] += 1;
                    System.out.print("W");
                }
                else if (results[j] == Utils.GameResult.LOSE)
                    System.out.print("L");
                else
                    System.out.print("D");

            System.out.println();
        }

        for (int i=0; i<agents.size(); i++)
            System.out.println("Player " + i + " [ " + agents.get(i).toString() +"] won: " + (playerWins[i]/n)*100.0 + "%.");
    }

    @Override
    public String toString()
    {
        return "Santorini Game for " + gameState.getNPlayers() + " players.";
    }
}
