package games.XIIScripta;

import games.backgammon.BGParameters;

// This just overrides the default parameters for Backgammon
public class XIIParameters extends BGParameters {

    public XIIParameters() {
        addTunableParameter("boardSize", 36);
        addTunableParameter("piecesPerPlayer", 15);
        addTunableParameter("startingAtBar", 15);
        addTunableParameter("homeBoardSize", 6);
        addTunableParameter("entryBoardSize", 12);
        addTunableParameter("diceNumber", 2);
        addTunableParameter("diceSides", 6);
        addTunableParameter("startingAt6", 0);
        addTunableParameter("startingAt8", 0);
        addTunableParameter("startingAt13", 0);
        addTunableParameter("startingAt24", 0);
        addTunableParameter("doubleActions", true);
        addTunableParameter("route", Route.Common);
        addTunableParameter("entryRule", EntryRule.Entry);
        _reset();
    }
}
