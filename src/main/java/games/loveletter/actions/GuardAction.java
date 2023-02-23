package games.loveletter.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.util.ArrayList;
import java.util.List;

/**
 * The guard allows to attempt guessing another player's card. If the guess is correct, the targeted opponent
 * is removed from the game.
 */
public class GuardAction extends PlayCard implements IPrintable {

    public GuardAction(int playerID, int opponentID, LoveLetterCard.CardType cardtype) {
        super(LoveLetterCard.CardType.Guard, playerID, opponentID, cardtype, null);
    }

    @Override
    protected boolean _execute(LoveLetterGameState llgs) {
        Deck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(targetPlayer);

        // guess the opponent's card and remove the opponent from play if the guess was correct
        LoveLetterCard card = opponentDeck.peek();
        if (card.cardType == this.targetCardType) {
            llgs.killPlayer(playerID, targetPlayer, cardType);
            if (llgs.getCoreGameParameters().recordEventHistory) {
                llgs.recordHistory("Guard guess correct!");
            }
        }
        return true;
    }

    @Override
    public String toString(){
        return "Guard (" + playerID + " guess " + targetPlayer + " holds card " + targetCardType.name() + ")";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GuardAction && super.equals(o);
    }

    public static List<? extends PlayCard> generateActions(LoveLetterGameState gs, int playerID) {
        List<PlayCard> cardActions = new ArrayList<>();
        for (int targetPlayer = 0; targetPlayer < gs.getNPlayers(); targetPlayer++) {
            if (targetPlayer == playerID || gs.getPlayerResults()[targetPlayer] == CoreConstants.GameResult.LOSE_ROUND || gs.isProtected(targetPlayer))
                continue;
            for (LoveLetterCard.CardType type : LoveLetterCard.CardType.values())
                if (type != LoveLetterCard.CardType.Guard) {
                    cardActions.add(new GuardAction(playerID, targetPlayer, type));
                }
        }
        return cardActions;
    }

    @Override
    public GuardAction copy() {
        return this;
    }
}
