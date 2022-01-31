package games.diamant;

import core.*;
import games.GameType;
import players.human.ActionController;
import players.human.HumanConsolePlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;

public class DiamantGame extends Game {
    public DiamantGame(List<AbstractPlayer> players, AbstractParameters gameParameters) {
        super(GameType.Diamant, players, new DiamantForwardModel(), new DiamantGameState(gameParameters, players.size()));
    }

    public static void main(String[] args) {
        ActionController ac = new ActionController();

        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        agents.add(new RandomPlayer());
        agents.add(new RandomPlayer());
        agents.add(new OSLAPlayer());
        agents.add(new HumanConsolePlayer());

        // Play n games and return the pct of wins of each player
        double [] playerWins = new double[agents.size()];
        for (int i=0; i<agents.size(); i++)
            playerWins[i] = 0.0;

        int n = 1000;
        for (int i=0; i<n; i++) {
            System.out.print("GAME: " + i + " ");
            AbstractParameters gameParameters = new DiamantParameters(System.currentTimeMillis());

            Game game = new DiamantGame(agents, gameParameters);
            game.run();

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
        return "Diamant Game for " + gameState.getNPlayers() + " players.";
    }
}
