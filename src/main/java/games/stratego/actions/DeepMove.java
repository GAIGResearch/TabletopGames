package games.stratego.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.interfaces.IExtendedSequence;
import games.stratego.StrategoGameState;
import games.stratego.components.Piece;
import utilities.Vector2D;

import java.util.List;
import java.util.Objects;

public class DeepMove extends AbstractAction implements IExtendedSequence {
    // Choose, dependent:
    final Vector2D position;

    // Choose, independent:
    final int pieceID;

    final ActionSpace actionSpace;
    final int playerID;
    boolean executed;

    public DeepMove(int playerID, Vector2D position, ActionSpace actionSpace) {
        this.position = position;
        this.actionSpace = actionSpace;
        this.playerID = playerID;
        this.pieceID = -1;
    }
    public DeepMove(int playerID, int pieceID, ActionSpace actionSpace) {
        this.pieceID = pieceID;
        this.actionSpace = actionSpace;
        this.playerID = playerID;
        this.position = null;
    }
    private DeepMove(Vector2D position, int pieceID, ActionSpace actionSpace, int playerID) {
        this.pieceID = pieceID;
        this.actionSpace = actionSpace;
        this.playerID = playerID;
        this.position = position;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        gs.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return getPiece((StrategoGameState) state).calculateMoves((StrategoGameState) state, actionSpace);
    }

    public Piece getPiece(StrategoGameState gs) {
        if (position == null) {
            return (Piece) gs.getComponentById(pieceID);
        } else {
            return (Piece) gs.getGridBoard().getElement(position);
        }
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        executed = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    public DeepMove copy() {
        DeepMove copy = new DeepMove(position, pieceID, actionSpace, playerID);
        copy.executed = executed;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeepMove)) return false;
        DeepMove deepMove = (DeepMove) o;
        return pieceID == deepMove.pieceID && playerID == deepMove.playerID && executed == deepMove.executed && Objects.equals(position, deepMove.position) && Objects.equals(actionSpace, deepMove.actionSpace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, pieceID, actionSpace, playerID, executed);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        if (pieceID != -1)
            return "DeepMove (" + pieceID + ")";
        else return "DeepMove from (" + position + ")";
    }
}
