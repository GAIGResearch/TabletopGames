package games.saboteur.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.saboteur.SaboteurGameState;
import games.saboteur.components.ActionCard;
import games.saboteur.components.SaboteurCard;
import utilities.Vector2D;

public class PlayMapCard extends AbstractAction {

    Vector2D position;
    ActionCard mapCard;
    public PlayMapCard(int x, int y, ActionCard mapCard)
    {
        this.position = new Vector2D(x, y);
        this.mapCard = mapCard;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        SaboteurGameState sgs = (SaboteurGameState) gs;
        int currentPlayer = sgs.getCurrentPlayer();

        Deck<SaboteurCard> currentPlayerDeck = sgs.playerDecks.get(currentPlayer);
        currentPlayerDeck.getComponents().remove(mapCard);
        sgs.gridBoard.setElementVisibility(position.getX(), position.getY(), currentPlayer, true);

        System.out.println(this);
        System.out.println(sgs.gridBoard.toString(position.getX(), position.getY()));
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
        return "Reveals card at (" + position.getX() + ", " + position.getY() + ")";
    }

    public String toString() {
        return "Reveals card at (" + position.getX() + ", " + position.getY() + ")";
    }
}
