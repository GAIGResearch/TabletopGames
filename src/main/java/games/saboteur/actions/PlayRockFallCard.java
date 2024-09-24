package games.saboteur.actions;

import core.AbstractGameState;
import core.actions.SetGridValueAction;
import core.components.Deck;
import games.saboteur.SaboteurGameState;
import games.saboteur.components.ActionCard;
import games.saboteur.components.PathCard;
import games.saboteur.components.SaboteurCard;


public class PlayRockFallCard extends SetGridValueAction<PathCard>
{
    public PlayRockFallCard(int gridBoard, int x, int y) {
        super(gridBoard, x, y, -1);
    }

    public boolean execute(AbstractGameState gs) {
        SaboteurGameState sgs = (SaboteurGameState) gs;
        sgs.getGridBoard().setElement(getX(), getY(), null);

        Deck<SaboteurCard> currentDeck = sgs.getPlayerDecks().get(sgs.getCurrentPlayer());
        SaboteurCard cardToPlay = null;
        for (SaboteurCard card : currentDeck.getComponents()) {
            if (card instanceof ActionCard && ((ActionCard)card).actionType == ActionCard.ActionCardType.RockFall) {
                cardToPlay = card;
                break;
            }
        }
        currentDeck.remove(cardToPlay);
        return true;
    }
    @Override
    public PlayRockFallCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PlayRockFallCard;
    }

    @Override
    public int hashCode() {
        return 128942839;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    public String toString() {
        return "RockFall at (" + super.getX() + ", " + super.getY() + ")";
    }
}

