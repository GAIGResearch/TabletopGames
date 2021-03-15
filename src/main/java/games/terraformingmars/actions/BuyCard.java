package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;

import java.util.Objects;

public class BuyCard extends AbstractAction {
    final int cardIdx;

    public BuyCard(int cardIdx) {
        this.cardIdx = cardIdx;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        TMGameState gs = (TMGameState) gameState;
        TMGameParameters gp = (TMGameParameters) gameState.getGameParameters();
        TMCard card = gs.getPlayerCardChoice()[gs.getCurrentPlayer()].pick(cardIdx);
        if (card.cardType == TMTypes.CardType.Corporation) {
            gs.getPlayerCorporations()[gs.getCurrentPlayer()] = card;
            gs.getPlayerCardChoice()[gs.getCurrentPlayer()].clear();
            return true;
        } else {
            if (gs.canPlayerPay(gp.getProjectPurchaseCost())) {
                gs.getPlayerHands()[gs.getCurrentPlayer()].add(card);
                gs.getPlayerResources()[gs.getCurrentPlayer()].get(TMTypes.Resource.MegaCredit).decrement(gp.getProjectPurchaseCost());
                // TODO: pay with other resources
                return true;
            } else {
                // Can't pay for it, discard instead
                gs.getDiscardCards().add(card);
                return false;
            }
        }
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuyCard)) return false;
        BuyCard buyCard = (BuyCard) o;
        return cardIdx == buyCard.cardIdx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardIdx);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Buy card idx " + cardIdx;
    }
}
