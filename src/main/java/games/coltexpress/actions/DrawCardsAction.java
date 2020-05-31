package games.coltexpress.actions;

import core.AbstractGameState;
import core.actions.DrawCard;
import core.interfaces.IPrintable;


public class DrawCardsAction extends DrawCard implements IPrintable {

    private final int nCards;

    public DrawCardsAction(int from, int to, int nCards){
        super(from, to);
        this.nCards = nCards;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        boolean success = true;
        for (int i = 0; i < nCards; i++) {
            success &= super.execute(gs);
        }
        return success;
    }

    @Override
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException();
        //return false;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString(){
        return "Draw " + nCards + " cards";

    }
}
