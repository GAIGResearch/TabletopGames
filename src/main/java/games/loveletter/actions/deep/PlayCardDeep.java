package games.loveletter.actions.deep;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.PlayCard;
import games.loveletter.cards.LoveLetterCard;

import java.util.Objects;

public abstract class PlayCardDeep extends PlayCard implements IExtendedSequence {
    private boolean executed;

    public PlayCardDeep(LoveLetterCard.CardType cardType, int playerID) {
        super(cardType, playerID, -1, null, null, false, true);
    }

    @Override
    public boolean execute(AbstractGameState llgs) {
        super.execute(llgs);
        llgs.setActionInProgress(this);
        return true;
    }

    public String toString(){
        return cardType + " (" + playerID + ")";
    }

    @Override
    protected boolean _execute(LoveLetterGameState llgs) {
        return true;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        executed = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    public abstract PlayCardDeep copy();

    protected void copyTo(PlayCardDeep copy) {
        copy.executed = executed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayCardDeep)) return false;
        PlayCardDeep that = (PlayCardDeep) o;
        return playerID == that.playerID && executed == that.executed && cardType == that.cardType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, cardType, executed);
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this);
    }
}
