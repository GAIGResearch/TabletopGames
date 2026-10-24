package games.saboteur.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.grid.SquareCoord;
import games.saboteur.SaboteurGameState;
import games.saboteur.components.PathCard;
import games.saboteur.components.SaboteurCard;

import java.util.Objects;

public class PlacePathCard extends AbstractAction
{
    private final SquareCoord position;
    private final int pathCardID;
    private final boolean rotated;

    public PlacePathCard(SquareCoord position, int pathCardID, boolean rotated) {
        this.position = position;
        this.pathCardID = pathCardID;
        this.rotated = rotated;
    }

    public SquareCoord getPosition() {
        return position;
    }

    public int getPathCardID() {
        return pathCardID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SaboteurGameState sgs = (SaboteurGameState) gs;
        PathCard pathCard = cardInHand(sgs);
        if (pathCard == null)
            throw new IllegalStateException("Player " + sgs.getCurrentPlayer() + " does not hold path card " + pathCardID);
        sgs.getGridBoard().set(position, rotated ? pathCard.rotated() : pathCard);
        sgs.getPathCardOptions().remove(position);
        sgs.getPlayerDecks().get(sgs.getCurrentPlayer()).remove(pathCard);
        return true;
    }

    // the card in the current player's hand, or null; not looked up by id in the state, as a card placed turned round
    // is a new object with the same id
    private PathCard cardInHand(SaboteurGameState sgs) {
        for (SaboteurCard c : sgs.getPlayerDecks().get(sgs.getCurrentPlayer()).getComponents())
            if (c.getComponentID() == pathCardID)
                return (PathCard) c;
        return null;
    }

    @Override
    public PlacePathCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlacePathCard that)) return false;
        return pathCardID == that.pathCardID && rotated == that.rotated && position.equals(that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, pathCardID, rotated) + 5821;
    }

    @Override
    public String getString(AbstractGameState state) {
        PathCard pathCard = cardInHand((SaboteurGameState) state);
        if (pathCard == null)
            return toString();
        return String.format("Place %s at %s %s.", pathCard.getString(), position, rotated ? " rotated" : "");
    }

    public String toString() {
        return String.format("Place %d at %s %s.", pathCardID, position, rotated ? " rotated" : "");
    }
}
