package games.loveletter.actions;

import core.AbstractGameState;
import core.components.Deck;
import core.observations.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.Objects;

import static utilities.CoreConstants.VERBOSE;

public class BaronAction extends DrawCard implements IPrintable {
    private final int opponentID;

    public BaronAction(int deckFrom, int deckTo, int fromIndex, int opponentID) {
        super(deckFrom, deckTo, fromIndex);
        this.opponentID = opponentID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        LoveLetterGameState llgs = (LoveLetterGameState)gs;
        int playerID = gs.getTurnOrder().getCurrentPlayer(gs);
        Deck<LoveLetterCard> playerDeck = llgs.getPlayerHandCards().get(playerID);
        Deck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(opponentID);

        if (llgs.isNotProtected(opponentID) && gs.getPlayerResults()[playerID] != Utils.GameResult.GAME_LOSE){
            LoveLetterCard opponentCard = opponentDeck.peek();
            LoveLetterCard playerCard = playerDeck.peek();
            if (opponentCard != null && playerCard != null) {
                if (opponentCard.cardType.getValue() < playerCard.cardType.getValue())
                    llgs.killPlayer(opponentID);
                else if (playerCard.cardType.getValue() < opponentCard.cardType.getValue())
                    llgs.killPlayer(playerID);
            } else {
                if (VERBOSE) {
                    System.out.println();
                }
            }
        }

        return super.execute(gs);
    }

    @Override
    public String toString(){
        return "Baron - compare the cards with player " + opponentID;
    }

    @Override
    public void printToConsole() {
        System.out.println(this.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BaronAction that = (BaronAction) o;
        return opponentID == that.opponentID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), opponentID);
    }
}
