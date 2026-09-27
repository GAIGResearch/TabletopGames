package games.toads.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.toads.components.ToadCard;
import games.toads.ToadGameState;

public class PlayFlankCard extends AbstractAction {

    // The player making the play. The Attacker's hidden card is chosen at the same time as the Defender's cards, so
    // the action has to carry this itself rather than read it from the turn owner: inside a SimultaneousAction the
    // turn owner is whoever happened to hold the turn when the joint action was applied.
    public final int playerId;
    public final ToadCard card;

    public PlayFlankCard(int playerId, ToadCard card) {
        this.playerId = playerId;
        this.card = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ToadGameState state = (ToadGameState) gs;
        state.playFlankCard(playerId, card);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PlayFlankCard other && other.playerId == playerId && other.card.equals(card);
    }

    @Override
    public int hashCode() {
        return card.hashCode() + 97 + 1013 * playerId;
    }

    @Override public String toString() {
        return "Play " + card.getComponentName() + " to the flank";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
