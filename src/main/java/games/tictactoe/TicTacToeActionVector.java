package games.tictactoe;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.rl.RLActionVector;

public class TicTacToeActionVector extends RLActionVector {

    TicTacToeActionVector() {
    }

    @Override
    public double[] featureVector(AbstractAction action, AbstractGameState state, int playerID) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'featureVector'");
    }

    @Override
    public String[] names() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'names'");
    }

}
