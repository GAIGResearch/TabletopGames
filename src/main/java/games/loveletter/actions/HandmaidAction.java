package games.loveletter.actions;

import core.AbstractGameState;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;

import java.util.Objects;

/**
 * The handmaid protects the player from any targeted effects until the next turn.
 */
public class HandmaidAction extends PlayCard implements IPrintable {

    public HandmaidAction(int fromIndex, int playerID) {
        super(fromIndex, playerID);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // set the player's protection status
        ((LoveLetterGameState) gs).setProtection(playerID, true);
        return super.execute(gs);
    }

    @Override
    public String toString() {
        return "Handmaid (" + playerID + " is protected until their next turn)";
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
    public HandmaidAction copy() {
        return new HandmaidAction(fromIndex, playerID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HandmaidAction)) return false;
        if (!super.equals(o)) return false;
        HandmaidAction that = (HandmaidAction) o;
        return playerID == that.playerID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerID);
    }
}
