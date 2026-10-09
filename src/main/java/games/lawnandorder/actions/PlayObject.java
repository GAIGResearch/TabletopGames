package games.lawnandorder.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.lawnandorder.LawnAndOrderGameState;
import games.lawnandorder.components.LawnCard;

import java.util.Objects;

/**
 * A player places a Lawn card from their hand face down (Step 1).
 */
public class PlayObject extends AbstractAction {
    // All active players choose at once, so the action carries the player
    public final int player;
    public final LawnCard card;

    public PlayObject(int player, LawnCard card) {
        this.player = player;
        this.card = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        LawnAndOrderGameState state = (LawnAndOrderGameState) gs;
        state.getHand(player).remove(card);
        state.getChosenCard(player).add(card);
        return true;
    }

    @Override
    public PlayObject copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlayObject other && other.player == player && other.card.equals(card);
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, card) + 618203;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Play " + card;
    }

    @Override
    public String toString() {
        return "P" + player + " plays " + card;
    }
}
