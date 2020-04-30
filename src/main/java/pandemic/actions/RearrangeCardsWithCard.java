package pandemic.actions;

import actions.Action;
import components.Card;
import components.Deck;
import components.IDeck;
import core.GameState;

import java.util.ArrayList;
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
        IDeck deckFrom = gs.findDeck(deckIdFrom);
        Card[] cards = new Card[newCardOrder.length];
        for (int value : newCardOrder) {
            cards[value] = deckFrom.draw();
        }
        IDeck draws = new Deck();
        draws.setCards(new ArrayList<>(Arrays.asList(cards)));
        boolean result = deckFrom.add(draws);

        if (result) {
            ((Deck) gs.getAreas().get(gs.getActivePlayer()).getComponent(playerHandHash)).discard(card);
            IDeck discardDeck = gs.findDeck("Player Deck Discard");
            result = discardDeck.add(card);
        }
        return result;
    }
}
