package games.chess;



import core.AbstractGameState;
import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;

/**
 * <p>This class should hold a series of variables representing game parameters (e.g. number of cards dealt to players,
 * maximum number of rounds in the game etc.). These parameters should be used everywhere in the code instead of
 * local variables or hard-coded numbers, by accessing these parameters from the game state via {@link AbstractGameState#getGameParameters()}.</p>
 *
 * <p>It should then implement appropriate {@link #_copy()}, {@link #_equals(Object)} and {@link #hashCode()} functions.</p>
 *
 * <p>The class can optionally extend from {@link TunableParameters} instead, which allows to use
 * automatic game parameter optimisation tools in the framework.</p>
 */
public class ChessParameters extends TunableParameters {

    public int maxRounds = 300; // Maximum number of moves in the game before it ends in a draw
    public int drawByRepetition = 3; // Number of times a position must be repeated for a draw by repetition to be declared, 0 for no limit
    public String dataPathString = "data/chess/"; // Path to the data folder for the game, used for loading images and other resources

    

    public ChessParameters() {
        addTunableParameter("maxRounds", 100);
        addTunableParameter("drawByRepetition", 3);
        _reset();
    }
    
    public Game instantiate() {
        return new Game(GameType.Chess, new ChessForwardModel(), new ChessGameState(this, GameType.Chess.getMinPlayers()));
    }

    public void _reset() {
        maxRounds = (int) getParameterValue("maxRounds");
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
        return maxRounds == that.maxRounds && drawByRepetition == that.drawByRepetition;
    }

    @Override
    public int hashCode() {
        return 31 * maxRounds + drawByRepetition;
    }

}
