package games.saboteur.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.saboteur.SaboteurGameState;
import games.saboteur.components.ActionCard;
import games.saboteur.components.SaboteurCard;
import utilities.Vector2D;

import java.util.Objects;

public class PlayMapCard extends AbstractAction {

    final Vector2D position;

    public Vector2D getPosition() {
        return position;
    }

    public PlayMapCard(int x, int y)
    {
        this.position = new Vector2D(x, y);
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        SaboteurGameState sgs = (SaboteurGameState) gs;
        int currentPlayer = sgs.getCurrentPlayer();

        Deck<SaboteurCard> currentPlayerDeck = sgs.getPlayerDecks().get(currentPlayer);
        int idx = -1;
        for (int i = 0; i < currentPlayerDeck.getSize(); i++) {
            SaboteurCard card = currentPlayerDeck.getComponents().get(i);
            if (card instanceof ActionCard && ((ActionCard)card).actionType == ActionCard.ActionCardType.Map) {
                idx = i;
                break;
            }
        }
        sgs.getDiscardDeck().add(currentPlayerDeck.pick(idx));
        sgs.getGridBoard().setElementVisibility(position.getX(), position.getY(), currentPlayer, true);
        return true;
    }

    @Override
    public PlayMapCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayMapCard that)) return false;
        return Objects.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Reveals card at (" + position.getX() + ", " + position.getY() + ")";
    }

    public String toString() {
        return "Reveals card at (" + position.getX() + ", " + position.getY() + ")";
    }
}
