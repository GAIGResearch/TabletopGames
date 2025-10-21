package games.backgammon;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Dice;
import evaluation.optimisation.TunableParameters;
import utilities.JSONUtils;

import java.util.Arrays;
import java.util.List;

public class BGParameters extends TunableParameters<BGParameters> {

    public enum Route {
        Common, Counter, CommonHalfA
    }

    public enum EntryRule {
        None, Bar, Entry
        // None means that there are no rules - any piece can be moved even if there are some pieces on the bar.
        // Bar means that pieces on the bar must be moved onto the board before any other pieces can be moved.
        // Entry means that pieces must be moved onto the board before any other pieces can be moved;
        // but pieces moved on this turn can be moved with other dice as long as they stay on the entry board.
    }

    // We add parameters for the dimensions of the backgammon board
    // and the number of pieces for each player.

    // This currently assumes that the board has two legs, with players
    // moving in opposite directions in a u-shaped board.
    public int boardSize = 24; // Number of points on the board
    public int piecesPerPlayer = 15; // Number of pieces for each player
    public int homeBoardSize = 6; // Number of points in the home board
    public int entryBoardSize = 0; // Number of points in the entry board (0 if not used)
    public int diceNumber = 2; // Number of dice used in the game
    public int diceSides = 6; // Number of sides on each die

    public Dice customDie = null;

    public boolean doubleActions = true; // Whether doubles allow double actions
    public Route route = Route.Counter; // Route type for the game
    public EntryRule entryRule = EntryRule.Bar; // Entry rule for pieces

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
        addTunableParameter("homeBoardSize", 6);
        addTunableParameter("entryBoardSize", 0);
        addTunableParameter("startingAtBar", 0);
        addTunableParameter("startingAt6", 5);
        addTunableParameter("startingAt8", 3);
        addTunableParameter("startingAt13", 5);
        addTunableParameter("startingAt24", 2);
        addTunableParameter("diceNumber", 2);
        addTunableParameter("diceSides", 6);
        addTunableParameter("diceJSON", "");
        addTunableParameter("doubleActions", true, List.of(false, true));
        addTunableParameter("route", Route.Counter);
        addTunableParameter("entryRule", EntryRule.Bar);

        _reset();
    }


    @Override
    protected BGParameters _copy() {
        BGParameters params = new BGParameters();
        if (this.customDie != null)
            params.customDie = this.customDie.copy();
        return params;
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof BGParameters && super.equals(o)
                && ((customDie == null && ((BGParameters) o).customDie == null)
                || (customDie != null && customDie.equals(((BGParameters) o).customDie)));
    }

    @Override
    public int hashCode() {
        return super.hashCode() + (customDie == null ? 0 : customDie.hashCode());
    }

    @Override
    public BGParameters instantiate() {
        return this;
    }

    @Override
    public void _reset() {
        boardSize = (int) getParameterValue("boardSize");
        piecesPerPlayer = (int) getParameterValue("piecesPerPlayer");
        homeBoardSize = (int) getParameterValue("homeBoardSize");
        entryBoardSize = (int) getParameterValue("entryBoardSize");
        startingAt24 = (int) getParameterValue("startingAt24");
        startingAt8 = (int) getParameterValue("startingAt8");
        startingAt6 = (int) getParameterValue("startingAt6");
        startingAt13 = (int) getParameterValue("startingAt13");
        startingAtBar = (int) getParameterValue("startingAtBar");
        diceNumber = (int) getParameterValue("diceNumber");
        diceSides = (int) getParameterValue("diceSides");
        String diceJSON = (String) getParameterValue("diceJSON");
        if (!diceJSON.isBlank()) {
            customDie = new Dice(diceJSON);
        }
        doubleActions = (boolean) getParameterValue("doubleActions");
        route = (Route) getParameterValue("route");
        entryRule = (EntryRule) getParameterValue("entryRule");
    }
}
