package games.perudo;

import evaluation.optimisation.TunableParameters;

import java.util.Arrays;

/**
 * Parameters for Perudo (the Asmodée rules, as on Board Game Arena: https://en.doc.boardgamearena.com/Gamehelpdudo).
 */
public class PerudoParameters extends TunableParameters<PerudoParameters> {

    // The face that shows a Paco (the bird on the "1" face), wild in a normal round
    public static final int PACO = 1;

    // Dice each player starts with
    public int nDicePerPlayer = 5;
    // Faces on each die, numbered 1 (Paco) to nFaces
    public int nFaces = 6;
    // Asmodée: the round after a player first drops to one die is a Palifico round (Pacos not wild, face fixed)
    public boolean palifico = false;
    // Calza: the player to act may call the bid exactly right, gaining a die if so and losing one if not
    public boolean calza = false;

    public PerudoParameters() {
        addTunableParameter("nDicePerPlayer", 5, Arrays.asList(3, 4, 5, 6));
        addTunableParameter("nFaces", 6);
        addTunableParameter("palifico", false, Arrays.asList(false, true));
        addTunableParameter("calza", false, Arrays.asList(false, true));
        _reset();
    }

    @Override
    public void _reset() {
        nDicePerPlayer = (int) getParameterValue("nDicePerPlayer");
        nFaces = (int) getParameterValue("nFaces");
        palifico = (boolean) getParameterValue("palifico");
        calza = (boolean) getParameterValue("calza");
    }

    @Override
    protected PerudoParameters _copy() {
        return new PerudoParameters();  // TunableParameters.copy() copies the parameter values
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof PerudoParameters;  // TunableParameters.equals() compares the parameter values
    }

    @Override
    public PerudoParameters instantiate() {
        return this;
    }
}
