package games.saboteur.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.Deck;
import games.saboteur.SaboteurGameState;
import games.saboteur.components.PathCard;
import games.saboteur.components.SaboteurCard;
import utilities.Vector2D;

public class PlacePathCard extends SetGridValueAction
{
    private final boolean rotated;
    private final PathCard pathCard;

    public PlacePathCard(int gridBoard, int x, int y, PathCard pathCard, boolean rotated) {
        super(gridBoard, x, y, pathCard);
        this.rotated = rotated;
        this.pathCard = pathCard;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SaboteurGameState sgs = (SaboteurGameState) gs;
        if(rotated)
        {
            String value = "Rotating Card from " + pathCard.getString() + " to ";
            pathCard.Rotate();
            value += pathCard.getString();
            System.out.println(value);
        }
        sgs.gridBoard.setElement(getX(), getY(), pathCard);
        sgs.pathCardOptions.remove(new Vector2D(getX(), getY()));

        Deck<SaboteurCard> currentDeck = sgs.playerDecks.get(sgs.getCurrentPlayer());
        currentDeck.remove(pathCard);
        System.out.println(this);
        System.out.println(sgs.gridBoard.toString());
        return true;
    }

    @Override
    public AbstractAction copy() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    public String toString() {
        return pathCard.getString() + " at (" + getX() + ", " + getY() + ") " +  (rotated ? " rotated" : "");
    }
}
