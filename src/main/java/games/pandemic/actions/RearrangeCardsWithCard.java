package games.pandemic.actions;

import core.actions.IAction;
import core.components.Card;
import core.components.Deck;
import core.AbstractGameState;
import games.pandemic.PandemicGameState;

import java.util.ArrayList;
import java.util.Arrays;

import static games.pandemic.PandemicConstants.playerDeckDiscardHash;
import static utilities.CoreConstants.playerHandHash;

@SuppressWarnings("unchecked")
public class RearrangeCardsWithCard implements IAction {
    private Deck<Card> deckFrom;
    private int[] newCardOrder;
    private Card card;  // card used to perform this action

    public RearrangeCardsWithCard(Deck<Card> deckFrom, int[] order, Card c) {
        this.deckFrom = deckFrom;
        this.newCardOrder = order;
        this.card = c;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        Card[] cards = new Card[newCardOrder.length];
        for (int value : newCardOrder) {
            cards[value] = deckFrom.draw();
        }
        Deck<Card> draws = new Deck<>();
        draws.setCards(new ArrayList<>(Arrays.asList(cards)));
        boolean result = deckFrom.add(draws);

        if (result) {
            ((Deck<Card>) pgs.getComponentActingPlayer(playerHandHash)).remove(card);
            Deck<Card> discardDeck = (Deck<Card>) pgs.getComponent(playerDeckDiscardHash);
            result = discardDeck.add(card);
        }
        return result;
    }
}
