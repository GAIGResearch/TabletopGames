package games.gofish;

import evaluation.optimisation.TunableParameters;

import java.util.List;

/**
 * Parameters for Go Fish (as described at https://www.pagat.com/quartet/gofish.html).
 */
public class GoFishParameters extends TunableParameters<GoFishParameters> {

    // Cards dealt to each player: startingHandSize with 3 or more players, twoPlayerHandSize with 2
    public int startingHandSize = 5;
    public int twoPlayerHandSize = 7;

    // Whether the asker takes another turn after being handed cards, and after drawing the rank they asked for
    public boolean continueOnSuccess = true;
    public boolean continueOnDrawingSameRank = true;

    // Whether play goes on past an empty hand or draw deck until nobody has anyone to ask. A player about to take a
    // turn with an empty hand draws a card, or is skipped if the draw deck is empty
    public boolean playUntilAllBooks = false;

    public GoFishParameters() {
        setMaxRounds(500);
        addTunableParameter("startingHandSize", 5);
        addTunableParameter("twoPlayerHandSize", 7);
        addTunableParameter("continueOnSuccess", true, List.of(false, true));
        addTunableParameter("continueOnDrawingSameRank", true, List.of(false, true));
        addTunableParameter("playUntilAllBooks", false, List.of(false, true));
    }

    @Override
    public void _reset() {
        startingHandSize = (int) getParameterValue("startingHandSize");
        twoPlayerHandSize = (int) getParameterValue("twoPlayerHandSize");
        continueOnSuccess = (boolean) getParameterValue("continueOnSuccess");
        continueOnDrawingSameRank = (boolean) getParameterValue("continueOnDrawingSameRank");
        playUntilAllBooks = (boolean) getParameterValue("playUntilAllBooks");
    }

    public int handSize(int nPlayers) {
        return nPlayers == 2 ? twoPlayerHandSize : startingHandSize;
    }

    @Override
    protected GoFishParameters _copy() {
        return new GoFishParameters();  // TunableParameters.copy() copies the parameter values
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof GoFishParameters;  // TunableParameters.equals() compares the parameter values
    }

    @Override
    public GoFishParameters instantiate() {
        return this;
    }
}
