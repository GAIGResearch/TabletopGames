package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.Objects;


/**
 * The Baron lets two players compare their hand card. The player with the lesser valued card is removed from the game.
 */
public class BaronAction extends core.actions.DrawCard implements IPrintable {
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

        // compares the value of the player's hand card with another player's hand card
        // the player with the lesser valued card will be removed from the game
        if (llgs.isNotProtected(opponentID) && gs.getPlayerResults()[playerID] != Utils.GameResult.LOSE){
            LoveLetterCard opponentCard = opponentDeck.peek();
            LoveLetterCard playerCard = playerDeck.peek();
            if (opponentCard != null && playerCard != null) {
                if (opponentCard.cardType.getValue() < playerCard.cardType.getValue())
                    llgs.killPlayer(opponentID);
                else if (playerCard.cardType.getValue() < opponentCard.cardType.getValue())
                    llgs.killPlayer(playerID);
            } else {
                throw new IllegalArgumentException("player with ID " + opponentID + " was targeted using a Baron card" +
                        " but one of the players has now cards left.");
            }
        }

        return super.execute(gs);
    }

    @Override
    public String toString(){
        return "Baron - compare the cards with player " + opponentID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Baron (compare cards with player " + opponentID + ")";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaronAction)) return false;
        if (!super.equals(o)) return false;
        BaronAction that = (BaronAction) o;
        return opponentID == that.opponentID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), opponentID);
    }

    @Override
    public AbstractAction copy() {
        return new BaronAction(deckFrom, deckTo, fromIndex, opponentID);
    }

    public int getOpponentID() {
        return opponentID;
    }
}
