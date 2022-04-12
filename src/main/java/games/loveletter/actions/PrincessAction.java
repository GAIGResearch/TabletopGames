package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;

/**
 * In case the princess is discarded or played the player is immediately removed from the game.
 */
public class PrincessAction extends core.actions.DrawCard implements IPrintable {

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
    public String getString(AbstractGameState gameState) {
        return "Princess (lose game)";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(toString());
    }

    @Override
    public AbstractAction copy() {
        return new PrincessAction(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrincessAction)) return false;
        return super.equals(o);
    }
}
