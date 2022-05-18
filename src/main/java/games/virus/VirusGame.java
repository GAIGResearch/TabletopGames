package games.virus;

import core.*;
import games.GameType;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;

// Official Rules
// https://tranjisgames.com/wp-content/uploads/2017/02/VIRUS-RULES-eng.pdf
public class VirusGame extends Game {
    public VirusGame(List<AbstractPlayer> agents, AbstractParameters gameParameters) {
        super(GameType.Virus, agents, new VirusForwardModel(), new VirusGameState(gameParameters, agents.size()));
    }

    public static void main(String[] args) {
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        //agents.add(new OSLA());
        //agents.add(new HumanConsolePlayer());
        //agents.add(new RandomPlayer());

        agents.add(new RandomPlayer());
        agents.add(new OSLAPlayer());
        agents.add(new OSLAPlayer());
        agents.add(new RandomPlayer());

        // Play n games and return the pct of wins of each player
        double [] playerWins = new double[4];
        for (int i=0; i<agents.size(); i++)
            playerWins[i] = 0.0;

        int n = 1000;
        for (int i=0; i<n; i++) {
            System.out.println(i);
            AbstractParameters gameParameters = new VirusGameParameters(System.currentTimeMillis());
            Game game = new VirusGame(agents, gameParameters);
            game.run();

            Utils.GameResult [] results =  game.getGameState().getPlayerResults();
            for (int j=0; j<agents.size(); j++)
                if (results[j] == Utils.GameResult.WIN)
                {
                    playerWins[j] += 1;
                    break;
                }
         }

        for (int i=0; i<agents.size(); i++)
            System.out.println("PLayer " + i + " [ " + agents.get(i).toString() +"] won: " + (playerWins[i] / n)*100.0 + "% of games.");
    }
}
