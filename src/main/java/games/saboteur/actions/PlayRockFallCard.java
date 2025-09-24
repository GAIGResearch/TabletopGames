package games.saboteur.actions;

import core.AbstractGameState;
import core.actions.SetGridValueAction;
import core.components.Deck;
import games.saboteur.SaboteurGameState;
import games.saboteur.components.ActionCard;
import games.saboteur.components.SaboteurCard;


public class PlayRockFallCard extends SetGridValueAction
{
    public PlayRockFallCard(int gridBoard, int x, int y) {
        super(gridBoard, x, y, -1);
    }

    public boolean execute(AbstractGameState gs) {
        SaboteurGameState sgs = (SaboteurGameState) gs;
        sgs.getGridBoard().setElement(getX(), getY(), null);

        Deck<SaboteurCard> currentDeck = sgs.getPlayerDecks().get(sgs.getCurrentPlayer());
        int idx = -1;
        for (int i = 0; i < currentDeck.getSize(); i++) {
            SaboteurCard card = currentDeck.getComponents().get(i);
            if (card instanceof ActionCard && ((ActionCard)card).actionType == ActionCard.ActionCardType.RockFall) {
                idx = i;
                break;
            }
        }
        sgs.getDiscardDeck().add(currentDeck.pick(idx));
        return true;
    }
    @Override
    public PlayRockFallCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PlayRockFallCard && super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    public String toString() {
        return "RockFall at (" + super.getX() + ", " + super.getY() + ")";
    }
}

