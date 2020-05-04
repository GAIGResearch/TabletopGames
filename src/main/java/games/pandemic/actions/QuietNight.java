package games.pandemic.actions;

import core.actions.IAction;
import core.components.Card;
import core.components.Deck;
import core.AbstractGameState;
import games.pandemic.PandemicGameState;

import static games.pandemic.PandemicConstants.playerHandHash;

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
    public boolean equals(Object other)
    {
        if (this == other) return true;
        return other instanceof QuietNight;
    }
}
