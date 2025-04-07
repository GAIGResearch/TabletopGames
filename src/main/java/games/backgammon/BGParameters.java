package games.backgammon;

import core.AbstractGameState;
import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;

public class BGParameters extends TunableParameters<BGParameters> {

    // We add parameters for the dimensions of the backgammon board
    // and the number of pieces for each player.

    // This currently assumes that the board has two legs, with players
    // moving in opposite directions in a u-shaped board.
    public int boardSize = 24; // Number of points on the board
    public int piecesPerPlayer = 15; // Number of pieces for each player
    public int barSize = 2; // Number of points on the bar
    public int homeBoardSize = 6; // Number of points in the home board
    public int diceNumber = 2; // Number of dice used in the game
    public int diceSides = 6; // Number of sides on each die

    // starting set up of pieces in backgammon
    // 2 on the bar, 5 in the home board, 3 on the 8 point, and 5 on the 24 point.

    public int startingAtBar = 0;
    public int startingAt6 = 5;
    public int startingAt8 = 3;
    public int startingAt13 = 5;
    public int startingAt24 = 2;

    public BGParameters() {
        addTunableParameter("boardSize", 24);
        addTunableParameter("piecesPerPlayer", 15);
        addTunableParameter("barSize", 2);
        addTunableParameter("homeBoardSize", 6);
        addTunableParameter("startingAtBar", 0);
        addTunableParameter("startingAt6", 5);
        addTunableParameter("startingAt8", 3);
        addTunableParameter("startingAt13", 5);
        addTunableParameter("startingAt24", 2);
        addTunableParameter("diceNumber", 2);
        addTunableParameter("diceSides", 6);
        _reset();
    }


    @Override
    protected BGParameters _copy() {
        return new BGParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof BGParameters;
    }

    @Override
    public int hashCode() {
        // TODO: include the hashcode of all variables.
        return super.hashCode();
    }

    @Override
    public BGParameters instantiate() {
        return this;
    }

    @Override
    public void _reset() {
        boardSize = (int) getParameterValue("boardSize");
        piecesPerPlayer = (int) getParameterValue("piecesPerPlayer");
        barSize = (int) getParameterValue("barSize");
        homeBoardSize = (int) getParameterValue("homeBoardSize");
        startingAt24 = (int) getParameterValue("startingAt24");
        startingAt8 = (int) getParameterValue("startingAt8");
        startingAt6 = (int) getParameterValue("startingAt6");
        startingAt13 = (int) getParameterValue("startingAt13");
        startingAtBar = (int) getParameterValue("startingAtBar");
        diceNumber = (int) getParameterValue("diceNumber");
        diceSides = (int) getParameterValue("diceSides");
    }
}
