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

/**
 * The Baron lets two players compare their hand card. The player with the lesser valued card is removed from the game.
 */
public class BaronAction extends PlayCard implements IPrintable {
    private transient LoveLetterCard.CardType playerCard;
    private transient LoveLetterCard.CardType opponentCard;

    public BaronAction(int playerID, int opponentID, boolean canExecuteEffect) {
        super(LoveLetterCard.CardType.Baron, playerID, opponentID, null, null, canExecuteEffect);
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
                llgs.killPlayer(playerID, targetPlayer, cardType);
            else if (playerCard.cardType.getValue() < opponentCard.cardType.getValue())
                llgs.killPlayer(playerID, playerID, cardType);
        } else {
            throw new IllegalArgumentException("player with ID " + targetPlayer + " was targeted using a Baron card" +
                    " but one of the players has no cards left.");
        }

        return true;
    }

    @Override
    public String _toString(){
        return "Baron (" + playerID + " compares cards with " + targetPlayer + ")";
    }

    @Override
    public String getString(AbstractGameState gameState) {
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
        return super.equals(o);
    }

    public static List<? extends PlayCard> generateActions(LoveLetterGameState gs, int playerID) {
        List<PlayCard> cardActions = new ArrayList<>();
        for (int targetPlayer = 0; targetPlayer < gs.getNPlayers(); targetPlayer++) {
            if (targetPlayer == playerID || gs.getPlayerResults()[targetPlayer] == CoreConstants.GameResult.LOSE_ROUND || gs.isProtected(targetPlayer))
                continue;
            cardActions.add(new BaronAction(playerID, targetPlayer, true));
        }
        if (cardActions.size() == 0) cardActions.add(new BaronAction(playerID, -1, false));
        return cardActions;
    }

    @Override
    public BaronAction copy() {
        BaronAction copy = new BaronAction(playerID, targetPlayer, canExecuteEffect);
        copy.playerCard = playerCard;
        copy.opponentCard = opponentCard;
        return copy;
    }
}
