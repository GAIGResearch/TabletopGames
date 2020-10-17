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


    public static void main(String[] args){

        // create list of players
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        DominionMCTSParameters bigMoneyRollout = new DominionMCTSParameters(8743);
        bigMoneyRollout.rolloutType = "BigMoney";

        agents.add(new MCTSPlayer());
        agents.add(new MCTSPlayer(bigMoneyRollout));
        agents.add(new BigMoney());
        agents.add(new OSLAPlayer());

/*        for (int i=0; i<100; i++) {
            // setup game
            DominionParameters params = new DominionParameters(System.currentTimeMillis());
            Game game = new DominionGame(agents, params);

            // run game
            game.run(null);
        }*/


        // Run!
        AbstractTournament tournament = new RoundRobinTournament(new LinkedList<>(agents), GameType.Dominion, 4, 1, true);
        tournament.runTournament();
    }
}
