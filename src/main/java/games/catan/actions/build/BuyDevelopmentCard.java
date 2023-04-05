package games.catan.actions.build;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.components.CatanCard;

import java.util.Objects;

public class BuyDevelopmentCard extends AbstractAction {
    public final int player;

    public BuyDevelopmentCard(int player) {
        this.player = player;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        CatanParameters cp = (CatanParameters) gs.getGameParameters();
        if (!cgs.spendResourcesIfPossible(cp.costMapping.get(BuyAction.BuyType.DevCard), player)) return false;
        // give a dev card to the player
        Deck<CatanCard> playerDevDeck = cgs.getPlayerDevCards(player);
        Deck<CatanCard> devDeck = cgs.getDevCards();
        CatanCard card = devDeck.draw();
        if (card != null) {
            card.roundCardWasBought = cgs.getRoundCounter();
            playerDevDeck.add(card);
            if (card.cardType == CatanCard.CardType.VICTORY_POINT_CARD){
                cgs.addVictoryPoint(cgs.getCurrentPlayer());
            }
            return true;
        }

        throw new AssertionError("Player cannot afford a development card");
    }

    @Override
    public BuyDevelopmentCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuyDevelopmentCard)) return false;
        BuyDevelopmentCard that = (BuyDevelopmentCard) o;
        return player == that.player;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player);
    }

    @Override
    public String toString() {
        return "p" + player + " Buy:DevelopmentCard";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
