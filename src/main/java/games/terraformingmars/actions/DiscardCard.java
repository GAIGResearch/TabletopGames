package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;

import java.util.Objects;

public class DiscardCard extends AbstractAction {
    final int cardIdx;

    public DiscardCard(int cardIdx) {
        this.cardIdx = cardIdx;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        TMGameState gs = (TMGameState) gameState;
        TMGameParameters gp = (TMGameParameters) gameState.getGameParameters();
        TMCard card = gs.getPlayerCardChoice()[gs.getCurrentPlayer()].pick(cardIdx);
        if (card.cardType != TMTypes.CardType.Corporation) {
            gs.getDiscardCards().add(card);
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiscardCard)) return false;
        DiscardCard buyCard = (DiscardCard) o;
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
