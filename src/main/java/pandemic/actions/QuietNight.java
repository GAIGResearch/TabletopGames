package pandemic.actions;

import actions.Action;
import components.Card;
import components.Deck;
import core.GameState;
import pandemic.PandemicGameState;

import static pandemic.Constants.playerHandHash;

public class QuietNight implements Action {
    Card card;
    public QuietNight(Card c) {
        this.card = c;
    }

    @Override
    public boolean execute(GameState gs) {
        // Discards the card
        ((Deck)gs.getAreas().get(gs.getActivePlayer()).getComponent(playerHandHash)).discard(card);
        return true;
   }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        return other instanceof QuietNight;
    }
}
