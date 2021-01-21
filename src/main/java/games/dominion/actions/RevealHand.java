package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.dominion.*;
import games.dominion.cards.DominionCard;

import java.util.Objects;

import static games.dominion.DominionConstants.*;

public class RevealHand extends AbstractAction {

    final int player;

    public RevealHand(int player) {
        this.player = player;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DominionGameState state = (DominionGameState) gs;
        PartialObservableDeck<DominionCard> hand = (PartialObservableDeck<DominionCard>) state.getDeck(DeckType.HAND, player);
        for (int i = 0; i < hand.getSize(); i++) {
            boolean[] allTrue = new boolean[state.getNPlayers()];
            for (int j = 0; j < state.getNPlayers(); j++) allTrue[j] = true;
            hand.setVisibilityOfComponent(i, allTrue);
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        // no mutable state
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RevealHand) {
            RevealHand other = (RevealHand) obj;
            return other.player == player;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Reveals Hand";
    }
}
