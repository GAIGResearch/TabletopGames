package games.virus.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.virus.VirusGameParameters;
import games.virus.VirusGameState;
import games.virus.cards.VirusCard;

import java.util.Objects;

public class DrawNewPlayerHand extends AbstractAction implements IPrintable {

    private int playerHandId;

    public DrawNewPlayerHand(int playerHandId) {
        this.playerHandId  = playerHandId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        VirusGameState vgs = (VirusGameState) gs;

        Deck<VirusCard> playerHand    =  (Deck<VirusCard>) vgs.getComponentById(playerHandId);
        Deck<VirusCard> drawDeck      =  vgs.getDrawDeck();

        int nCards = ((VirusGameParameters)gs.getGameParameters()).nCardsPlayerHand;
        // Draw three cards
        for (int i=0; i<nCards; i++) {
            // After discarding a card, the player must draw a card from the draw deck.
            // It is is empty, move all cards from discard deck to draw one and shuffle.
            // After, draw a card and add it to the player hand.
            if (drawDeck.getSize() == 0)
                discardToDraw((VirusGameState) gs);
            playerHand.add(drawDeck.draw());
        }

       return true;
    }

    private void discardToDraw(VirusGameState vgs) {
        while (vgs.getDiscardDeck().getSize()>0) {
            VirusCard card = vgs.getDiscardDeck().draw();
            vgs.getDrawDeck().add(card);
        }
        vgs.getDrawDeck().shuffle(vgs.getRnd());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Draw a new player hand";
    }

    @Override
    public void printToConsole() {
        System.out.println("Draw a new player hand");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DrawNewPlayerHand)) return false;
        DrawNewPlayerHand that = (DrawNewPlayerHand) o;
        return playerHandId == that.playerHandId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerHandId);
    }

    @Override
    public AbstractAction copy() {
        return new DrawNewPlayerHand(playerHandId);
    }
}
