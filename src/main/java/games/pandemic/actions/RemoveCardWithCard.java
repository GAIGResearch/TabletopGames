package games.pandemic.actions;

import core.AbstractGameState;
import core.actions.IAction;
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
        deck.remove(removeCard); // card removed from the game

        // Discard other card from player hand
        Deck<Card> discardDeck = (Deck<Card>) pgs.getComponent(playerDeckDiscardHash);
        Deck<Card> playerHand = (Deck<Card>) pgs.getComponentActingPlayer(playerHandHash);
        playerHand.remove(card);
        discardDeck.add(card);
        return true;
    }
}
