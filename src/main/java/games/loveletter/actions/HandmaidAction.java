package games.loveletter.actions;

import core.AbstractGameState;
import core.observations.IPrintable;
import games.loveletter.LoveLetterGameState;

public class HandmaidAction extends DrawCard implements IPrintable {

    public HandmaidAction(int deckFrom, int deckTo, int fromIndex) {
        super(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);

        ((LoveLetterGameState)gs).setProtection(gs.getTurnOrder().getCurrentPlayer(gs), true);
        return true;
    }

    @Override
    public String toString(){
        return "Handmaid - get protection status";
    }

    @Override
    public void printToConsole() {
        System.out.println(toString());
    }
}
