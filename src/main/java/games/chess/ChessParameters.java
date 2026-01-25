package games.chess;



import java.util.Arrays;

import core.AbstractGameState;
import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;


public class ChessParameters extends TunableParameters {

    public int drawByRepetition = 3; // Number of times a position must be repeated for a draw by repetition to be declared, 0 for no limit
    public String dataPathString = "data/chess/"; // Path to the data folder for the game, used for loading images and other resources

    

    public ChessParameters() {
        addTunableParameter("maxRounds", 100, Arrays.asList(50, 100, 200, 300)); //Note: current forward model checks for this before checkmate e.g. if the last move ends in checkmate, the game will end in a draw
        addTunableParameter("drawByRepetition", 3, Arrays.asList(0, 1, 2, 3));
        _reset();
    }
    
    public Game instantiate() {
        return new Game(GameType.Chess, new ChessForwardModel(), new ChessGameState(this, GameType.Chess.getMinPlayers()));
    }

    public void _reset() {
        setMaxRounds((int) getParameterValue("maxRounds"));
        drawByRepetition = (int) getParameterValue("drawByRepetition");
    }

    @Override
    protected AbstractParameters _copy() {
        return this;
    }

    @Override
    protected boolean _equals(Object o) {
        if (!(o instanceof ChessParameters)) return false;
        ChessParameters that = (ChessParameters) o;
        return getMaxRounds() == that.getMaxRounds() && drawByRepetition == that.drawByRepetition;
    }

    @Override
    public int hashCode() {
        return 31 * getMaxRounds() + drawByRepetition;
    }

}
