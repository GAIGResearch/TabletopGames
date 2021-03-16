package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;

import java.util.Objects;

public class DiscardCard extends TMAction {
    final int cardIdx;

    public DiscardCard(int cardIdx, boolean free) {
        super(free);
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
        return super.execute(gs);
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiscardCard)) return false;
        if (!super.equals(o)) return false;
        DiscardCard that = (DiscardCard) o;
        return cardIdx == that.cardIdx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardIdx);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Discard card idx " + cardIdx;
    }

    @Override
    public String toString() {
        return "Discard card idx " + cardIdx;
    }
}
