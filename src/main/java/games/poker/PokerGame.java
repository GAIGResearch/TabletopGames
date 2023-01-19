package games.poker;

import core.*;
import games.GameType;

import players.human.HumanConsolePlayer;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.AbstractForwardModel;
import core.Game;

import java.util.ArrayList;
import java.util.List;

public class PokerGame extends Game {

    public PokerGame(List<AbstractPlayer> agents, AbstractForwardModel forwardModel, PokerGameState gameState) {
        super(GameType.Poker, agents, forwardModel, gameState);
    }

    public PokerGame(AbstractForwardModel forwardModel, AbstractGameState gameState) {
        super(GameType.Poker, forwardModel, gameState);
    }

    public PokerGame(List<AbstractPlayer> agents, AbstractParameters gameParameters) {
        super(GameType.Poker, agents, new PokerForwardModel(), new PokerGameState(gameParameters, agents.size()));
    }

    public static void main(String[] args) {
        ArrayList<AbstractPlayer> agents = new ArrayList<>();
        //agents.add(new RandomPlayer());
        agents.add(new HumanConsolePlayer());
        agents.add(new HumanConsolePlayer());
        //agents.add(new RandomPlayer());
        //agents.add(new OSLAPlayer());
        //agents.add(new OSLAPlayer());
        //agents.add(new RMHCPlayer());
        //agents.add(new MCTSPlayer());

        //agents.add(new HumanConsolePlayer());
        //agents.add(new MCTSPlayer());

        AbstractParameters gameParameters = new PokerGameParameters(System.currentTimeMillis());
        Game game = new PokerGame(agents, gameParameters);

        game.run();
    }

}
