package games.seasaltpaper.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.cards.SeaSaltPaperCard;

public class DrawMultiple extends AbstractAction {

//    boolean discard;
//    int playerIdFrom;
//    int  playerIdTo;
//
//    int deckIdFrom;

    int[] drawnCardsId;
    int playerID;

    public DrawMultiple(int count, int playerID) {
       this.drawnCardsId = new int[count];
       this.playerID = playerID;
    }

    public int[] getDrawnCardsId() {
        return drawnCardsId;
    }

    public int getDrawCount() {
        return drawnCardsId.length;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) gs;
        for (int i=0; i < drawnCardsId.length; i++) {
            if (sspgs.getDrawPile().getSize() <= 0) {
                // TODO print or throw exception here?
                System.out.println("NO CARDS LEFT FROM DRAW PILE!!!!");
                return false;
            }
            SeaSaltPaperCard drawnCard = sspgs.getDrawPile().draw();
            sspgs.getPlayerHands().get(playerID).add(drawnCard);
            drawnCardsId[i] = drawnCard.getComponentID();
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Draw " + drawnCardsId.length + " cards from draw pile.";
    }
}
