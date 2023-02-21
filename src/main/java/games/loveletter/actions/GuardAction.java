package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.util.Objects;

/**
 * The guard allows to attempt guessing another player's card. If the guess is correct, the targeted opponent
 * is removed from the game.
 */
public class GuardAction extends PlayCard implements IPrintable {

    private final int opponentID;
    private final LoveLetterCard.CardType cardType;

    public GuardAction(int fromIndex, int playerID, int opponentID, LoveLetterCard.CardType cardtype) {
        super(fromIndex, playerID);
        this.opponentID = opponentID;
        this.cardType = cardtype;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        LoveLetterGameState llgs = (LoveLetterGameState)gs;
        Deck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(opponentID);

        // guess the opponent's card and remove the opponent from play if the guess was correct
        LoveLetterCard card = opponentDeck.peek();
        if (card.cardType == this.cardType) {
            llgs.killPlayer(opponentID);
            if (llgs.getCoreGameParameters().recordEventHistory) {
                llgs.recordHistory("Guard guess correct!");
            }
        }
        return super.execute(gs);
    }

    @Override
    public String toString(){
        return "Guard (" + playerID + " guess " + opponentID + " holds card " + cardType.name() + ")";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GuardAction)) return false;
        if (!super.equals(o)) return false;
        GuardAction that = (GuardAction) o;
        return opponentID == that.opponentID && playerID == that.playerID && cardType == that.cardType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), opponentID, playerID, cardType);
    }

    @Override
    public AbstractAction copy() {
        return new GuardAction(fromIndex, playerID, opponentID, cardType);
    }

    public int getOpponentID() {
        return opponentID;
    }
}
