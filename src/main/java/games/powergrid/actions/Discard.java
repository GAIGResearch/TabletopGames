package games.powergrid.actions;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.powergrid.PowerGridGameState;
import games.powergrid.components.PowerGridCard;

public class Discard extends AbstractAction {

	 final int index;  
	    Discard(int index) { this.index = index; }

	    @Override
	    public boolean execute(AbstractGameState gs) {

	        PowerGridGameState s = (PowerGridGameState) gs;
	        int me = gs.getCurrentPlayer();
	        Deck<PowerGridCard> deck  = s.getPlayerPlantDeck(me);
	        PowerGridCard A = deck.peek(index);         
	        return s.removePlantFromPlayer(me,A.getNumber());
	    }

	    @Override public Discard copy() { return this; }
	    @Override
	    public boolean equals(Object o) {
	        return (o instanceof Discard other) && other.index == this.index;
	    }

	    @Override public int hashCode(){ return 0xD15C4 << 3 ^ index; }
	    @Override public String getString(AbstractGameState gs){ return "DiscardSlot(" + index + ")"; }
	}
