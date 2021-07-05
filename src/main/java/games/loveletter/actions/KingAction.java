package games.loveletter.actions;

import core.AbstractGameState;
import core.CoreConstants.VisibilityMode;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.util.Objects;

/**
 * The King lets two players swap their hand cards.
 */
public class KingAction extends core.actions.DrawCard implements IPrintable {

    private final int opponentID;

    public KingAction(int deckFrom, int deckTo, int fromIndex, int opponentID) {
        super(deckFrom, deckTo, fromIndex);
        this.opponentID = opponentID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);

        LoveLetterGameState llgs = (LoveLetterGameState)gs;
        int playerID = gs.getTurnOrder().getCurrentPlayer(gs);
        Deck<LoveLetterCard> playerDeck = llgs.getPlayerHandCards().get(playerID);
        Deck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(opponentID);

        // create a temporary deck to store cards in and then swap cards accordingly
        if (((LoveLetterGameState) gs).isNotProtected(opponentID)){
            Deck<LoveLetterCard> tmpDeck = new Deck<>("tmp", VisibilityMode.HIDDEN_TO_ALL);
            while (opponentDeck.getSize() > 0)
                tmpDeck.add(opponentDeck.draw());
            while (playerDeck.getSize() > 0)
                opponentDeck.add(playerDeck.draw());
            while (tmpDeck.getSize() > 0)
                playerDeck.add(tmpDeck.draw());
        }

        return true;
    }

    @Override
    public String toString(){
        return "King - trade hands with player "+ opponentID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "King (trade hands with player " + opponentID + ")";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KingAction)) return false;
        if (!super.equals(o)) return false;
        KingAction that = (KingAction) o;
        return opponentID == that.opponentID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), opponentID);
    }

    @Override
    public AbstractAction copy() {
        return new KingAction(deckFrom, deckTo, fromIndex, opponentID);
    }

    public int getOpponentID() {
        return opponentID;
    }
}
