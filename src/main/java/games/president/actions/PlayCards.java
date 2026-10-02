package games.president.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.president.PresidentGameState;
import games.president.PresidentUtils;

/**
 * Plays a set of count cards of one rank, identified by its FrenchCard number (2..14, Ace 14).
 */
public class PlayCards extends AbstractAction {

    public final int number;
    public final int count;

    public PlayCards(int number, int count) {
        this.number = number;
        this.count = count;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PresidentGameState state = (PresidentGameState) gs;
        int player = state.getCurrentPlayer();
        Deck<FrenchCard> hand = state.getPlayerHand(player);
        // the suits do not matter, so the cards are taken in a fixed suit order
        int moved = 0;
        for (FrenchCard.Suite suit : FrenchCard.Suite.values()) {
            if (moved == count) break;
            for (FrenchCard card : hand.getComponents()) {
                if (card.number == number && card.suite == suit) {
                    hand.remove(card);
                    state.getPlayPile().add(card);
                    moved++;
                    break;
                }
            }
        }
        state.setLastPlayer(player);
        state.setSetSize(count);
        state.setPassesInRow(0);
        return true;
    }

    @Override
    public PlayCards copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PlayCards other && other.number == number && other.count == count;
    }

    @Override
    public int hashCode() {
        return 610337 + 31 * number + 997 * count;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Play " + count + " x " + PresidentUtils.rankName(number);
    }
}
