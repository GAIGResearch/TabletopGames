package games.chess;



import java.util.Arrays;

import core.AbstractGameState;
import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;


public class ChessParameters extends TunableParameters {

<<<<<<< HEAD
    public int maxChessRounds = 300; // Maximum number of moves in the game before it ends in a draw
=======
    public int maxRounds = 300; // Maximum number of moves in the game before it ends in a draw
>>>>>>> 9d59845f675b7ddaa58e9adb5ed3781d501f1f5c
    public int drawByRepetition = 3; // Number of times a position must be repeated for a draw by repetition to be declared, 0 for no limit
    public String dataPathString = "data/chess/"; // Path to the data folder for the game, used for loading images and other resources

    

    public ChessParameters() {
<<<<<<< HEAD
        addTunableParameter("maxChessRounds", 100, Arrays.asList(50, 100, 200, 300));
=======
        addTunableParameter("maxRounds", 100, Arrays.asList(50, 100, 200, 300));
>>>>>>> 9d59845f675b7ddaa58e9adb5ed3781d501f1f5c
        addTunableParameter("drawByRepetition", 3, Arrays.asList(0, 1, 2, 3));
        _reset();
    }
    
    public Game instantiate() {
        return new Game(GameType.Chess, new ChessForwardModel(), new ChessGameState(this, GameType.Chess.getMinPlayers()));
    }

    public void _reset() {
<<<<<<< HEAD
        maxChessRounds = (int) getParameterValue("maxChessRounds");
=======
        maxRounds = (int) getParameterValue("maxRounds");
>>>>>>> 9d59845f675b7ddaa58e9adb5ed3781d501f1f5c
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
<<<<<<< HEAD
        return maxChessRounds == that.maxChessRounds && drawByRepetition == that.drawByRepetition;
=======
        return maxRounds == that.maxRounds && drawByRepetition == that.drawByRepetition;
>>>>>>> 9d59845f675b7ddaa58e9adb5ed3781d501f1f5c
    }

    @Override
    public int hashCode() {
<<<<<<< HEAD
        return 31 * maxChessRounds + drawByRepetition;
=======
        return 31 * maxRounds + drawByRepetition;
>>>>>>> 9d59845f675b7ddaa58e9adb5ed3781d501f1f5c
    }

}
