package games.loveletter.actions;

import core.AbstractGameState;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;

/**
 * In case the princess is discarded or played the player is immediately removed from the game.
 */
public class PrincessAction extends DrawCard implements IPrintable {

    public PrincessAction(int deckFrom, int deckTo, int fromIndex) {
        super(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // remove the player from the game
        ((LoveLetterGameState)gs).killPlayer(gs.getTurnOrder().getCurrentPlayer(gs));
        return super.execute(gs);
    }

    @Override
    public String toString(){
        return "Princess - discard this card and lose the game";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(toString());
    }
}
