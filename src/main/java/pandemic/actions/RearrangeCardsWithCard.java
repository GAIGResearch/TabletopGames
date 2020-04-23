package pandemic.actions;

import actions.Action;
import components.Card;
import components.Deck;
import core.GameState;

import java.util.Arrays;

import static pandemic.Constants.playerHandHash;

public class RearrangeCardsWithCard implements Action {
    private String deckIdFrom;
    private int[] newCardOrder;
    private Card card;  // card used to perform this action

    public RearrangeCardsWithCard(String deckFrom, int[] order, Card c) {
        this.deckIdFrom = deckFrom;
        this.newCardOrder = order;
        this.card = c;
    }

    @Override
    public boolean execute(GameState gs) {
        Deck deckFrom = gs.findDeck(deckIdFrom);
        Card[] cards = new Card[newCardOrder.length];
        for (int value : newCardOrder) {
            cards[value] = deckFrom.draw();
        }
        Deck draws = new Deck();
        draws.setCards(Arrays.asList(cards));
        boolean result = deckFrom.add(draws);

        if (result) {
            ((Deck) gs.getAreas().get(gs.getActivePlayer()).getComponent(playerHandHash)).discard(card);
            Deck discardDeck = gs.findDeck("Player Deck Discard");
            result = discardDeck.add(card);
        }
        return result;
    }
}
