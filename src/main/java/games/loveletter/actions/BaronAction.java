package games.loveletter.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.util.Objects;


/**
 * The Baron lets two players compare their hand card. The player with the lesser valued card is removed from the game.
 */
public class BaronAction extends PlayCard implements IPrintable {
    private final int opponentID;
    private LoveLetterCard.CardType playerCard, opponentCard;

    public BaronAction(int fromIndex, int playerID, int opponentID) {
        super(fromIndex, playerID);
        this.opponentID = opponentID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        LoveLetterGameState llgs = (LoveLetterGameState)gs;
        Deck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(opponentID);
        PartialObservableDeck<LoveLetterCard> playerDeck = llgs.getPlayerHandCards().get(playerID);

        // compares the value of the player's hand card with another player's hand card
        // the player with the lesser valued card will be removed from the game
        if (gs.getPlayerResults()[playerID] != CoreConstants.GameResult.LOSE){
            LoveLetterCard opponentCard = opponentDeck.peek();
            LoveLetterCard playerCard = playerDeck.peek();
            if (opponentCard != null && playerCard != null) {
                this.playerCard = playerCard.cardType;
                this.opponentCard = opponentCard.cardType;
                if (opponentCard.cardType.getValue() < playerCard.cardType.getValue())
                    llgs.killPlayer(opponentID);
                else if (playerCard.cardType.getValue() < opponentCard.cardType.getValue())
                    llgs.killPlayer(playerID);
            } else {
                throw new IllegalArgumentException("player with ID " + opponentID + " was targeted using a Baron card" +
                        " but one of the players has no cards left.");
            }
        }

        return super.execute(gs);
    }

    @Override
    public String toString(){
        return "Baron (" + playerID + " compares cards with " + opponentID + ")";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        if (playerCard == null) {
            LoveLetterGameState llgs = (LoveLetterGameState)gameState;
            Deck<LoveLetterCard> playerDeck = llgs.getPlayerHandCards().get(playerID);
            Deck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(opponentID);

            LoveLetterCard opponentCard = opponentDeck.peek();
            LoveLetterCard playerCard = playerDeck.peek();
            if (opponentCard != null && playerCard != null) {
                this.playerCard = playerCard.cardType;
                this.opponentCard = opponentCard.cardType;
            }
        }
        return "Baron (" + playerID + " " + playerCard + " vs " + opponentID + " " + opponentCard + ")";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaronAction)) return false;
        if (!super.equals(o)) return false;
        BaronAction that = (BaronAction) o;
        return playerID == that.playerID && opponentID == that.opponentID && playerCard == that.playerCard && opponentCard == that.opponentCard;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerID, opponentID, playerCard, opponentCard);
    }

    @Override
    public BaronAction copy() {
        BaronAction copy = new BaronAction(fromIndex, playerID, opponentID);
        copy.playerCard = playerCard;
        copy.opponentCard = opponentCard;
        return copy;
    }

    public int getOpponentID() {
        return opponentID;
    }
}
