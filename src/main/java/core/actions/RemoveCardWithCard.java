package core.actions;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import games.pandemic.PandemicGameState;

import static games.pandemic.PandemicConstants.playerDeckDiscardHash;
import static games.pandemic.PandemicConstants.playerHandHash;

@SuppressWarnings("unchecked")
public class RemoveCardWithCard implements IAction {
    private Deck<Card> deck;
    private Card card; // card to be discarded from player hand after this action is executed
    private int removeCard;

    public RemoveCardWithCard(Deck<Card> deck, int discardCard, Card card) {
        this.deck = deck;
        this.removeCard = discardCard;
        this.card = card;
    }


    @Override
    public boolean execute(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        deck.discard(removeCard); // card removed from the game

        // Discard other card from player hand
        Deck<Card> discardDeck = (Deck<Card>) pgs.getComponent(playerDeckDiscardHash);  // TODO: if this action is to be general, this could be another deck
        Deck<Card> playerHand = (Deck<Card>) pgs.getComponent(playerHandHash, pgs.getActingPlayerID());
        playerHand.discard(card);
        discardDeck.add(card);
        return true;
    }
}
