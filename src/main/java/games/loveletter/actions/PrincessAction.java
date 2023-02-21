package games.loveletter.actions;

import core.AbstractGameState;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;

import java.util.Objects;

/**
 * In case the princess is discarded or played the player is immediately removed from the game.
 */
public class PrincessAction extends PlayCard implements IPrintable {

    public PrincessAction(int fromIndex, int playerID) {
        super(fromIndex, playerID);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // remove the player from the game
        ((LoveLetterGameState)gs).killPlayer(gs.getCurrentPlayer());
        return super.execute(gs);
    }

    @Override
    public String toString(){
        return "Princess (" + playerID + " loses the game)";
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
    public PrincessAction copy() {
        return new PrincessAction(fromIndex, playerID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrincessAction)) return false;
        if (!super.equals(o)) return false;
        PrincessAction that = (PrincessAction) o;
        return playerID == that.playerID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerID);
    }
}
