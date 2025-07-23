package games.XIIScripta;

import games.backgammon.BGParameters;

import java.util.List;

// This just overrides the default parameters for Backgammon
public class XIIParameters extends BGParameters {

    public XIIParameters() {
        addTunableParameter("boardSize", 36);
        addTunableParameter("piecesPerPlayer", 15);
        addTunableParameter("startingAtBar", 15);
        addTunableParameter("startingAt6", 0);
        addTunableParameter("startingAt8", 0);
        addTunableParameter("startingAt13", 0);
        addTunableParameter("startingAt24", 0);
        addTunableParameter("homeBoardSize", 6);
        addTunableParameter("entryBoardSize", 6, List.of(0, 6));
        addTunableParameter("diceNumber", 2, List.of(2, 3));
        addTunableParameter("diceSides", 6);
        addTunableParameter("doubleActions", true, List.of(false, true));
        addTunableParameter("route", Route.Common, List.of(Route.values()));
        addTunableParameter("entryRule", EntryRule.Entry, List.of(EntryRule.values()));
        _reset();
    }
}
