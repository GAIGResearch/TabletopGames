package actions;

import components.Card;
import components.Deck;
import core.GameState;

import static pandemic.Constants.playerHandHash;

public class RemoveCardWithCard implements Action {
    private Deck deck;
    private Card card; // card to be discarded from player hand after this action is executed
    private int discardCard;

    public RemoveCardWithCard(Deck deck, int discardCard, Card card) {
        this.deck = deck;
        this.discardCard = discardCard;
        this.card = card;
    }


    @Override
    public boolean execute(GameState gs) {
        deck.discard(discardCard); // card removed from the game

        // Discard other card from player hand
        Deck discardDeck = gs.findDeck("Player Deck Discard");  // TODO: if this action is to be general, this could be another deck
        Deck playerHand = (Deck) gs.getAreas().get(gs.getActingPlayer()).getComponent(playerHandHash);
        playerHand.discard(card);
        discardDeck.add(card);
        return true;
    }
}
