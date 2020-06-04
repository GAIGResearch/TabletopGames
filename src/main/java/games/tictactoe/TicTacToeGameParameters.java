package games.tictactoe;

import core.AbstractGameParameters;

public class TicTacToeGameParameters extends AbstractGameParameters {
    public int gridSize = 3;

    public TicTacToeGameParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractGameParameters _copy() {
        TicTacToeGameParameters tttgp = new TicTacToeGameParameters(System.currentTimeMillis());
        tttgp.gridSize = gridSize;
        return tttgp;
    }
}
