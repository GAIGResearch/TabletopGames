package games.loveletter.actions;

import core.AbstractGameState;
import core.observations.IPrintable;
import games.loveletter.LoveLetterGameState;

public class PrincessAction extends DrawCard implements IPrintable {

    public PrincessAction(int deckFrom, int deckTo, int fromIndex) {
        super(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((LoveLetterGameState)gs).killPlayer(gs.getTurnOrder().getCurrentPlayer(gs));
        return super.execute(gs);
    }

    @Override
    public String toString(){
        return "Princess - discard this card and lose the game";
    }

    @Override
    public void printToConsole() {
        System.out.println(toString());
    }
}
