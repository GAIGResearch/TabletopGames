package core.actions;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import games.pandemic.PandemicGameState;

import static games.pandemic.Constants.playerDeckDiscardHash;
import static games.pandemic.Constants.playerHandHash;

@SuppressWarnings("unchecked")
public class RemoveCardWithCard implements IAction {
    private Deck<Card> deck;
    private Card card; // card to be discarded from player hand after this action is executed
    private int discardCard;

    public RemoveCardWithCard(Deck<Card> deck, int discardCard, Card card) {
        this.deck = deck;
        this.discardCard = discardCard;
        this.card = card;
    }


    @Override
    public boolean execute(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        deck.discard(discardCard); // card removed from the game

        // Discard other card from player hand
        Deck<Card> discardDeck = (Deck<Card>) pgs.getComponent(playerDeckDiscardHash);  // TODO: if this action is to be general, this could be another deck
        Deck<Card> playerHand = (Deck<Card>) pgs.getComponent(playerHandHash, pgs.getActingPlayerID());
        playerHand.discard(card);
        discardDeck.add(card);
        return true;
    }
}
