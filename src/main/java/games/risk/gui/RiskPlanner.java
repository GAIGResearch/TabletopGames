package games.risk.gui;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.risk.RiskForwardModel;
import games.risk.RiskGamePhase;
import games.risk.RiskGameState;
import gui.IMovePlanner;

import java.util.List;

/**
 * Plans a player's reinforcements: the card trades and placements of their turn's reinforce phase, each undoable,
 * sent together. Nothing random happens in that phase, so the plan is carried out exactly; the attacks that follow
 * are rolled one at a time and not planned.
 */
public class RiskPlanner implements IMovePlanner {

    private final RiskForwardModel fm;

    public RiskPlanner(RiskForwardModel fm) {
        this.fm = fm;
    }

    @Override
    public boolean plans(AbstractGameState state, int player) {
        return reinforcing((RiskGameState) state, player);
    }

    private static boolean reinforcing(RiskGameState s, int player) {
        return s.isNotTerminal() && s.getCurrentPlayer() == player && s.getGamePhase() == RiskGamePhase.REINFORCE
                && s.currentActionInProgress() == null;
    }

    @Override
    public List<AbstractAction> options(AbstractGameState planned, int player) {
        RiskGameState s = (RiskGameState) planned;
        return reinforcing(s, player) ? fm.computeAvailableActions(s) : List.of();
    }

    @Override
    public void apply(AbstractGameState planned, AbstractAction action) {
        fm.next(planned, action);
    }

    @Override
    public List<String> warnings(AbstractGameState planned, int player) {
        RiskGameState s = (RiskGameState) planned;
        int left = s.getGamePhase() == RiskGamePhase.REINFORCE ? s.getArmiesToPlace(player) : 0;
        if (left == 0) return List.of();
        return List.of(left + (left == 1 ? " army" : " armies") + " still to place: you will be asked for "
                + (left == 1 ? "it" : "them") + " after sending.");
    }

    @Override
    public String sendLabel() {
        return "Place armies";
    }
}
