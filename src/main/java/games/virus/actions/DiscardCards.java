package games.virus.actions;

import core.AbstractGameState;
import core.actions.IAction;
import core.components.Deck;
import core.observations.IPrintable;
import games.virus.VirusGameState;
import games.virus.cards.VirusCard;

import java.util.ArrayList;
import java.util.List;

public class DiscardCards implements IAction, IPrintable {

    private List<VirusCard> cardsToDiscard;
    private Deck<VirusCard> playerHand;
    private Deck<VirusCard> drawDeck;
    private Deck<VirusCard> discardDeck;

    public DiscardCards(VirusCard card, Deck<VirusCard> playerHand, Deck<VirusCard> drawDeck, Deck<VirusCard> discardDeck) {
        cardsToDiscard = new ArrayList<VirusCard>();
        cardsToDiscard.add(card);
        this.playerHand  = playerHand;
        this.drawDeck    = drawDeck;
        this.discardDeck = discardDeck;
    }

    public DiscardCards(VirusCard card1, VirusCard card2, Deck<VirusCard> playerHand, Deck<VirusCard> drawDeck, Deck<VirusCard> discardDeck) {
        cardsToDiscard = new ArrayList<VirusCard>();
        cardsToDiscard.add(card1);
        cardsToDiscard.add(card2);
        this.playerHand  = playerHand;
        this.drawDeck    = drawDeck;
        this.discardDeck = discardDeck;
    }

    public DiscardCards(VirusCard card1, VirusCard card2, VirusCard card3, Deck<VirusCard> playerHand, Deck<VirusCard> drawDeck, Deck<VirusCard> discardDeck) {
        cardsToDiscard = new ArrayList<VirusCard>();
        cardsToDiscard.add(card1);
        cardsToDiscard.add(card2);
        cardsToDiscard.add(card3);
        this.playerHand  = playerHand;
        this.drawDeck    = drawDeck;
        this.discardDeck = discardDeck;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        for (VirusCard card : cardsToDiscard) {
            playerHand.remove(card);
            discardDeck.add(card);

            System.out.println("###" + drawDeck.getSize());
            if (drawDeck.getSize() == 0)
                ((VirusGameState) gs).discardToDraw();

            VirusCard newCard = drawDeck.draw();
            playerHand.add(newCard);
        }
        return true;
    }

    @Override
    public void printToConsole() {
        String str="";

        for (int i=0; i<cardsToDiscard.size(); i++) {
            str += cardsToDiscard.get(i).toString() + " ";
        }
        System.out.println("Discard cards ( " + str + ")");
    }
}
