package games.tictactoe;

import core.AbstractGameParameters;

public class TicTacToeGameParameters extends AbstractGameParameters {
    public int gridSize = 3;

    public TicTacToeGameParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractGameParameters _copy() {
        return new TicTacToeGameParameters(System.currentTimeMillis());
    }
}
