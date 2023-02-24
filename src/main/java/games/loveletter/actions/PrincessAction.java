package games.loveletter.actions;

import core.AbstractGameState;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

/**
 * In case the princess is discarded or played the player is immediately removed from the game.
 */
public class PrincessAction extends PlayCard implements IPrintable {

    public PrincessAction(int playerID) {
        super(LoveLetterCard.CardType.Princess, playerID, -1, null, null, true);
    }

    @Override
    public boolean _execute(LoveLetterGameState gs) {
        // remove the player from the game
        gs.killPlayer(playerID, playerID, cardType);
        return true;
    }

    @Override
    public String _toString(){
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
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PrincessAction && super.equals(o);
    }
}
