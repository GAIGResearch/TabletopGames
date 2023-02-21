package games.loveletter.actions;

import core.AbstractGameState;
import core.CoreConstants.VisibilityMode;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.util.Objects;

/**
 * The King lets two players swap their hand cards.
 */
public class KingAction extends PlayCard implements IPrintable {

    private final int opponentID;

    public KingAction(int fromIndex, int playerID, int opponentID) {
        super(fromIndex, playerID);
        this.opponentID = opponentID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);

        LoveLetterGameState llgs = (LoveLetterGameState)gs;
        Deck<LoveLetterCard> playerDeck = llgs.getPlayerHandCards().get(playerID);
        Deck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(opponentID);

        // create a temporary deck to store cards in and then swap cards accordingly
        Deck<LoveLetterCard> tmpDeck = new Deck<>("tmp", VisibilityMode.HIDDEN_TO_ALL);
        while (opponentDeck.getSize() > 0)
            tmpDeck.add(opponentDeck.draw());
        while (playerDeck.getSize() > 0)
            opponentDeck.add(playerDeck.draw());
        while (tmpDeck.getSize() > 0)
            playerDeck.add(tmpDeck.draw());

        return true;
    }

    @Override
    public String toString(){
        return "King (" + playerID + " trades hands with " + opponentID + ")";
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
        if (this == o) return true;
        if (!(o instanceof KingAction)) return false;
        if (!super.equals(o)) return false;
        KingAction that = (KingAction) o;
        return playerID == that.playerID && opponentID == that.opponentID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerID, opponentID);
    }

    @Override
    public KingAction copy() {
        return new KingAction(fromIndex, playerID, opponentID);
    }

    public int getOpponentID() {
        return opponentID;
    }
}
