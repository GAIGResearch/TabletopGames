package games.explodingkittens.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard.CardType;

import java.util.Objects;

public class PlayEKCard extends AbstractAction {

    public final CardType cardType;
    public final int target;

    public PlayEKCard(CardType cardType) {
        this.cardType = cardType;
        target = -1;
    }

    public PlayEKCard(CardType cardType, int target) {
        this.cardType = cardType;
        this.target = target;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ExplodingKittensGameState state = (ExplodingKittensGameState) gs;
        state.setInPlay(cardType, state.getCurrentPlayer());
        if (cardType.catCard)
            state.setInPlay(cardType, state.getCurrentPlayer());  // add an extra one

        if (cardType.nopeable) {
            state.setActionInProgress(new NopeableAction(state.getCurrentPlayer(), this, state));
        } else {
            // if not Nopeable, execute the card
            cardType.execute(state, target);
        }

        return true;
    }

    @Override
    public PlayEKCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PlayEKCard pek && pek.cardType == cardType && pek.target == target;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardType, target) + 2901;
    }

    @Override
    public String toString() {
        if (target == -1)
            return "Play " + cardType;
        return "Play " + cardType + " on player " + target;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
