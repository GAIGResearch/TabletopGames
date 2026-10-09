package games.pitch.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.pitch.PitchGameState;
import games.pitch.PitchParameters;
import games.tricktaking.Trick;

public class PlayCard extends AbstractAction {

    // FrenchCard is immutable, so the action can hold it
    public final FrenchCard card;

    public PlayCard(FrenchCard card) {
        this.card = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PitchGameState state = (PitchGameState) gs;
        int player = state.getCurrentPlayer();
        Trick<FrenchCard, FrenchCard.Suite> trick = state.getCurrentTrick();
        state.getPlayerHand(player).remove(card);
        // the pitcher's first lead sets trumps
        if (state.getTrumpSuit() == null)
            state.setTrumpSuit(card.suite);
        if (((PitchParameters) state.getGameParameters()).rememberVoids)
            state.getKnownVoids().record(player, trick, card, state.getTrumpSuit());
        trick.play(card);
        return true;
    }

    @Override
    public PlayCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PlayCard other && other.card.equals(card);
    }

    @Override
    public int hashCode() {
        return card.hashCode() + 830219;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Play " + card;
    }
}
