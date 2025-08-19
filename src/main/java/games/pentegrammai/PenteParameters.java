package games.pentegrammai;

import core.AbstractGameState;
import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;

import java.util.Arrays;

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
public class PenteParameters extends AbstractParameters {

    public int boardSize = 10;
    public int dieSides = 6;
    public int[] sacredPoints = {2, 7};

    public PenteParameters() {
        // If using TunableParameters, addTunableParameter(...) here
    }

    @Override
    protected AbstractParameters _copy() {
        PenteParameters copy = new PenteParameters();
        copy.boardSize = this.boardSize;
        copy.dieSides = this.dieSides;
        copy.sacredPoints = Arrays.copyOf(this.sacredPoints, this.sacredPoints.length);
        return copy;
    }

    @Override
    protected boolean _equals(Object o) {
        if (!(o instanceof PenteParameters other)) return false;
        return boardSize == other.boardSize &&
                dieSides == other.dieSides &&
                Arrays.equals(sacredPoints, other.sacredPoints);
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(boardSize);
        result = 31 * result + Integer.hashCode(dieSides);
        result = 31 * result + Arrays.hashCode(sacredPoints);
        return result;
    }
}
