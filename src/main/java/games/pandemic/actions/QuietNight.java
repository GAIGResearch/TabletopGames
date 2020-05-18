package games.pandemic.actions;

import core.actions.IAction;
import core.components.Card;
import core.components.Deck;
import core.AbstractGameState;
import games.pandemic.PandemicGameState;

import java.util.Objects;

import static utilities.CoreConstants.playerHandHash;

@SuppressWarnings("unchecked")
public class QuietNight implements IAction {
    Card card;
    public QuietNight(Card c) {
        this.card = c;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // Discards the card
        PandemicGameState pgs = (PandemicGameState) gs;
        ((Deck<Card>) pgs.getComponentActingPlayer(playerHandHash)).remove(card);
        return true;
   }

    @Override
    public Card getCard() {
        return null;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        return other instanceof QuietNight;
    }

    @Override
    public String toString() {
        return "QuietNight";
    }

    @Override
    public int hashCode() {
        return Objects.hash(card);
    }
}
