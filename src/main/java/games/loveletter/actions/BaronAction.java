package games.loveletter.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The Baron lets two players compare their hand card. The player with the lesser valued card is removed from the game.
 */
public class BaronAction extends PlayCard implements IPrintable {
    private LoveLetterCard.CardType playerCard, opponentCard;

    public BaronAction(int playerID, int opponentID) {
        super(LoveLetterCard.CardType.Baron, playerID, opponentID, null, null);
    }

    @Override
    protected boolean _execute(LoveLetterGameState llgs) {
        Deck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(targetPlayer);
        PartialObservableDeck<LoveLetterCard> playerDeck = llgs.getPlayerHandCards().get(playerID);

        // compares the value of the player's hand card with another player's hand card
        // the player with the lesser valued card will be removed from the game
        LoveLetterCard opponentCard = opponentDeck.peek();
        LoveLetterCard playerCard = playerDeck.peek();
        if (opponentCard != null && playerCard != null) {
            this.playerCard = playerCard.cardType;
            this.opponentCard = opponentCard.cardType;
            if (opponentCard.cardType.getValue() < playerCard.cardType.getValue())
                llgs.killPlayer(targetPlayer);
            else if (playerCard.cardType.getValue() < opponentCard.cardType.getValue())
                llgs.killPlayer(playerID);
        } else {
            throw new IllegalArgumentException("player with ID " + targetPlayer + " was targeted using a Baron card" +
                    " but one of the players has no cards left.");
        }

        return true;
    }

    @Override
    public String toString(){
        return "Baron (" + playerID + " compares cards with " + targetPlayer + ")";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        if (playerCard == null) {
            LoveLetterGameState llgs = (LoveLetterGameState)gameState;
            Deck<LoveLetterCard> playerDeck = llgs.getPlayerHandCards().get(playerID);
            Deck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(targetPlayer);

            LoveLetterCard opponentCard = opponentDeck.peek();
            LoveLetterCard playerCard = playerDeck.peek();
            if (opponentCard != null && playerCard != null) {
                this.playerCard = playerCard.cardType;
                this.opponentCard = opponentCard.cardType;
            }
        }
        return "Baron (" + playerID + " " + playerCard + " vs " + targetPlayer + " " + opponentCard + ")";
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
        return playerCard == that.playerCard && opponentCard == that.opponentCard;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerCard, opponentCard);
    }

    public static List<? extends PlayCard> generateActions(LoveLetterGameState gs, int playerID) {
        List<PlayCard> cardActions = new ArrayList<>();
        for (int targetPlayer = 0; targetPlayer < gs.getNPlayers(); targetPlayer++) {
            if (targetPlayer == playerID || gs.getPlayerResults()[targetPlayer] == CoreConstants.GameResult.LOSE_ROUND || gs.isProtected(targetPlayer))
                continue;
            cardActions.add(new BaronAction(playerID, targetPlayer));
        }
        return cardActions;
    }

    @Override
    public BaronAction copy() {
        BaronAction copy = new BaronAction(playerID, targetPlayer);
        copy.playerCard = playerCard;
        copy.opponentCard = opponentCard;
        return copy;
    }
}
