package games.dominion;

import core.*;
import evaluation.AbstractTournament;
import evaluation.RoundRobinTournament;
import games.GameType;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.rmhc.RMHCPlayer;
import players.simple.*;

import java.util.*;

public class DominionGame extends Game {

    public DominionGame(List<AbstractPlayer> players, DominionParameters params) {
        super(GameType.Dominion, players, new DominionForwardModel(), new DominionGameState(params, players.size()));
    }
    public DominionGame(DominionParameters params, int nPlayers) {
        super(GameType.Dominion, new DominionForwardModel(), new DominionGameState(params, nPlayers));
    }


    public static void main(String[] args) {

        // create list of players
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        MCTSParams bigMoneyRollout = new MCTSParams(8743);
        bigMoneyRollout.rolloutType = "BigMoney";
        bigMoneyRollout.rolloutsEnabled = true;
        bigMoneyRollout.rolloutLength = 100;

        agents.add(new MCTSPlayer());
        agents.add(new MCTSPlayer(bigMoneyRollout, "MCTS_BMRollout"));
        agents.add(new BigMoney());
        agents.add(new RandomPlayer());

        int nGames = 1;
/*        for (int i = 0; i < nGames; i++) {
            // setup game
            DominionParameters params = new DominionParameters(System.currentTimeMillis());
            Game game = new DominionGame(agents, params);

            // run game
            game.run(null);
        }*/


        // Run!
        AbstractTournament tournament = new RoundRobinTournament(new LinkedList<>(agents), GameType.Dominion, 4, 10, false);
        tournament.runTournament();
    }
}
