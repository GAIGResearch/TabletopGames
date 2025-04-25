package games.saboteur.actions;

import core.AbstractGameState;
import core.actions.SetGridValueAction;
import core.components.Deck;
import games.saboteur.SaboteurGameState;
import games.saboteur.components.PathCard;
import games.saboteur.components.SaboteurCard;
import utilities.Vector2D;

import java.util.Objects;

public class PlacePathCard extends SetGridValueAction
{
    private final boolean rotated;

    public PlacePathCard(int gridBoard, int x, int y, int pathCardID, boolean rotated) {
        super(gridBoard, x, y, pathCardID);
        this.rotated = rotated;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SaboteurGameState sgs = (SaboteurGameState) gs;
        PathCard pathCard = (PathCard) sgs.getComponentById(getValueID());
        if(rotated)
        {
            pathCard.rotate();
        }
        sgs.getGridBoard().setElement(getX(), getY(), pathCard);
        sgs.getPathCardOptions().remove(new Vector2D(getX(), getY()));

        Deck<SaboteurCard> currentDeck = sgs.getPlayerDecks().get(sgs.getCurrentPlayer());
        currentDeck.remove(pathCard);
        return true;
    }

    @Override
    public PlacePathCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlacePathCard that)) return false;
        if (!super.equals(o)) return false;
        return rotated == that.rotated;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 31 * (rotated ? 1 : 0);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return super.getString(gameState) + (rotated ? " rotated" : "");
    }

    public String toString() {
        return super.toString() + (rotated ? " rotated" : "");
    }
}
