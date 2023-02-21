package games.loveletter.actions;

import core.AbstractGameState;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import core.components.PartialObservableDeck;
import games.loveletter.cards.LoveLetterCard;

import java.util.Objects;

/**
 * The Priest allows a player to see another player's hand cards.
 * This has no effect in case the game is fully observable.
 */
public class PriestAction extends PlayCard implements IPrintable {

    private final int opponentID;
    private LoveLetterCard.CardType opponentCard;

    public PriestAction(int fromIndex, int playerID, int opponentID) {
        super(fromIndex, playerID);
        this.opponentID = opponentID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        LoveLetterGameState llgs = (LoveLetterGameState)gs;
        PartialObservableDeck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(opponentID);

        // Set all cards to be visible by the current player
        for (int i = 0; i < opponentDeck.getComponents().size(); i++)
            opponentDeck.setVisibilityOfComponent(i, playerID, true);

        opponentCard = opponentDeck.get(0).cardType;
        if (llgs.getCoreGameParameters().recordEventHistory) {
            llgs.recordHistory("Priest sees " + opponentCard);
        }

        return super.execute(gs);
    }

    @Override
    public String toString(){
        return "Priest (" + playerID + " sees " + (opponentCard != null? opponentCard : "card") + " of " + opponentID + ")";
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
        if (!(o instanceof PriestAction)) return false;
        if (!super.equals(o)) return false;
        PriestAction that = (PriestAction) o;
        return playerID == that.playerID && opponentID == that.opponentID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerID, opponentID);
    }

    @Override
    public PriestAction copy() {
        PriestAction copy = new PriestAction(fromIndex, playerID, opponentID);
        copy.opponentCard = opponentCard;
        return copy;
    }

    public int getOpponentID() {
        return opponentID;
    }
}
