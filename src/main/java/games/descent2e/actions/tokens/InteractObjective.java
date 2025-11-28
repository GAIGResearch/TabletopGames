package games.descent2e.actions.tokens;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.BoardNode;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;
import games.descent2e.components.tokens.DToken;
import utilities.Vector2D;

import java.util.*;

/**
 * General interact action for Figures interacting with Objective tokens
 */
public class InteractObjective extends TokenAction<InteractObjective> implements IExtendedSequence {
    boolean complete;
    int figureID;

    public InteractObjective() {
        super(-1, Triggers.ACTION_POINT_SPEND);
    }

    public InteractObjective(int objectiveID) {
        super(objectiveID, Triggers.ACTION_POINT_SPEND);
    }

    public InteractObjective(int objectiveID, int figureID) {
        super(objectiveID, Triggers.ACTION_POINT_SPEND);
        this.figureID = figureID;
    }



    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        // This will be overridden where necessary
        List<AbstractAction> actions = new ArrayList<>();
        if (actions.isEmpty()) actions.add(new DoNothing());
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return state.getComponentById(figureID).getOwnerId();
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        complete = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }

    @Override
    public InteractObjective _copy() {
        return new InteractObjective(tokenID, figureID);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(figureID);
        if (!dgs.getActingFigure().equals(f)) return false;
        if (f.getNActionsExecuted().isMaximum()) return false;

        // Can only execute if player adjacent to another hero
        DToken objective = (DToken) dgs.getComponentById(tokenID);
        Vector2D pos = objective.getPosition();
        if (pos == null) return false;
        if (f.getPosition() == null) return false;
        // Immediately return true if the Figure is standing on top of the token
        if (pos.equals(f.getPosition())) return true;
        // Else, check that they are adjacent
        BoardNode loc = dgs.getMasterBoard().getElement(f.getPosition());
        BoardNode tokenLoc = dgs.getMasterBoard().getElement(pos);
        Set<BoardNode> neighbours = loc.getNeighbours().keySet();
        return neighbours.contains(tokenLoc);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof InteractObjective that) {
            return super.equals(that) && complete == that.complete && figureID == that.figureID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), complete, figureID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        DToken token = (DToken) gameState.getComponentById(tokenID);
        return "Interact with Objective at (" + token.getPosition().getX() + ", " + token.getPosition().getY() + ")";
    }

    @Override
    public String toString() {
        return "Interact with Objective " + tokenID;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        dgs.setActionInProgress(this);
        interact(dgs);
        Figure f = (Figure) dgs.getComponentById(figureID);
        f.getNActionsExecuted().increment();
        f.addActionTaken(toString());
        return true;
    }

    public void setFigureID(int figureID) {
        this.figureID = figureID;
    }

    protected void interact(DescentGameState dgs) {
        // This will be overridden where necessary
        complete = true;
    }
}
