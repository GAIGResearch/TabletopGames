package games.toads.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.toads.ToadGameState;
import games.toads.components.ToadCard;

import java.util.Objects;

/**
 * The Defender plays both their cards at once: one face-up to the field, opposite the Attacker's face-up card, and
 * one hidden to the flank. This is chosen at the same time as the Attacker chooses their own hidden card.
 */
public class PlayDefenderCards extends AbstractAction {

    // The player making the play; see PlayFlankCard for why the action carries this.
    public final int playerId;
    public final ToadCard fieldCard;
    public final ToadCard flankCard;

    public PlayDefenderCards(int playerId, ToadCard fieldCard, ToadCard flankCard) {
        this.playerId = playerId;
        this.fieldCard = fieldCard;
        this.flankCard = flankCard;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ToadGameState state = (ToadGameState) gs;
        state.playFieldCard(playerId, fieldCard);
        state.playFlankCard(playerId, flankCard);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PlayDefenderCards other && other.playerId == playerId &&
                other.fieldCard.equals(fieldCard) && other.flankCard.equals(flankCard);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, fieldCard, flankCard) + 101;
    }

    @Override
    public String toString() {
        return "Play " + fieldCard.getComponentName() + " to the field and " + flankCard.getComponentName() + " to the flank";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String getString(AbstractGameState gameState, int perspective) {
        if (perspective == playerId)
            return toString();
        return "Play " + fieldCard.getComponentName() + " to the field and a hidden card to the flank";
    }
}
