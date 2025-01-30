package games.seasaltpaper.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.cards.SeaSaltPaperCard;

import java.util.Arrays;
import java.util.Objects;

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
            if (sspgs.getDrawPile().getSize() == 0) {
                // TODO print or throw exception here?
//                System.out.println("NO CARDS LEFT FROM DRAW PILE!!!!");
                throw new RuntimeException("NO CARDS LEFT FROM DRAW PILE!!!! SHOULD NEVER REACH HERE");
//                return false;
            }
            SeaSaltPaperCard drawnCard = sspgs.getDrawPile().draw();
            drawnCard.setVisible(playerID, true);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DrawMultiple that = (DrawMultiple) o;
        return playerID == that.playerID && Arrays.equals(drawnCardsId, that.drawnCardsId);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(playerID);
        result = 31 * result + Arrays.hashCode(drawnCardsId);
        return result;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        StringBuilder cardsStr = new StringBuilder();
        for (int id : drawnCardsId) {
            cardsStr.append(gameState.getComponentById(id).toString()).append(" ");
        }
        return "Draw " + cardsStr + " cards from draw pile.";
    }
}
