package games.powergrid.actions;


import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.powergrid.PowerGridGameState;
import games.powergrid.PowerGridParameters.Resource;
import games.powergrid.components.PowerGridCard;
import games.powergrid.components.PowerGridCard.PlantInput;

/**
 * Action that discards a power plant from the current player's owned plant deck.
 * <p>
 * This action is typically triggered immediately after a player wins an auction and
 * temporarily exceeds the maximum of three plants. It removes the plant at the given
 * deck index (0-based), then enforces resource storage caps based on the player's
 * remaining plants (e.g., hybrid capacity rules for Gas/Oil, and dedicated caps for
 * Coal/Uranium).
 *
 * <p><b>Side effects:</b>
 * <ul>
 *   <li>Mutates the player's owned plant deck by removing one plant.</li>
 *   <li>May reduce the player's stored fuel to comply with storage capacity.</li>
 *   <li>Returns any excess fuel to the market's discard pile.</li>
 * </ul>
 *
 * @see PowerGridGameState
 * @see PowerGridCard
 * @see PowerGridResourceMarket
 */

public class Discard extends AbstractAction {

	 final int index;  
	    public Discard(int index) { this.index = index; }

	    @Override
	    public boolean execute(AbstractGameState gs) {
	        PowerGridGameState s = (PowerGridGameState) gs;
	        int me = gs.getCurrentPlayer();

	        Deck<PowerGridCard> deck = s.getPlayerPlantDeck(me);
	        if (deck == null || index < 0 || index >= deck.getSize()) return false;

	        PowerGridCard card = deck.peek(index);
	        if (card == null) return false;

	        boolean removed = s.removePlantFromPlayer(me, card.getNumber());
	        if (!removed) return false;
	        enforceStorageCaps(s, me);
	        return true;
	    }
	    
	    public int getIndex() {
	    	return this.index; 
	    }

	    @Override public Discard copy() { return new Discard(index); }
	    
	    @Override
	    public boolean equals(Object o) {
	        return (o instanceof Discard other) && other.index == this.index;
	    }

	    @Override public int hashCode(){ return 0xD15C4 << 3 ^ index; }
	    
	    @Override public String getString(AbstractGameState gs){ return "Discard " + index ; }
	    
	    /**
	     * Ensures that a player's stored fuel does not exceed the total storage capacity
	     * provided by their owned power plants. If a player has more fuel than their plants
	     * can store, this method automatically discards the excess.
	     * <p>
	     * The storage capacity for each resource type is determined as follows:
	     * <ul>
	     *   <li>Each plant can store up to twice the amount of fuel it consumes when powered.</li>
	     *   <li>Hybrid plants (those that can use both GAS and OIL) provide a shared pool equal
	     *       to twice the larger of the two input requirements.</li>
	     * </ul>
	     *
	     * <p><b>Behavior:</b>
	     * <ul>
	     *   <li>Coal and Uranium are capped independently based on their dedicated capacities.</li>
	     *   <li>Gas and Oil may share hybrid storage capacity if applicable.</li>
	     *   <li>If total fuel across Gas/Oil exceeds the combined capacity (dedicated + hybrid),
	     *       the excess is discarded — prioritizing Gas reductions first, then Oil if needed.</li>
	     * </ul>
	     *
	     * <p><b>Side effects:</b> May mutate the {@link PowerGridGameState} by reducing the
	     * player's stored fuel counts through calls to {@code decreaseFuel()}.
	     *
	     * @param s         the {@link PowerGridGameState} containing plant and fuel data
	     * @param playerId  the ID of the player whose fuel storage is being enforced
	     *
	     * @see PowerGridCard
	     * @see PowerGridCard.PlantInput
	     * @see PowerGridParameters.Resource
	     */
	    private void enforceStorageCaps(PowerGridGameState s, int playerId) {
	    	
	        //Compute capacity from remaining plants
	        int coalCap = 0, gasCap = 0, oilCap = 0, urCap = 0, hybridCap = 0;

	        for (PowerGridCard c : s.getOwnedPlantsByPlayer(playerId).getComponents()) {
	            PlantInput in = c.getInput();
	            int cIn = in.get(Resource.COAL);
	            int gIn = in.get(Resource.GAS);
	            int oIn = in.get(Resource.OIL);
	            int uIn = in.get(Resource.URANIUM);

	            boolean isHybrid = (gIn > 0 && oIn > 0 && in.asMap().size() == 2);

	            coalCap += 2 * cIn;
	            urCap   += 2 * uIn;
	            if (isHybrid) {
	                hybridCap += 2 * Math.max(gIn, oIn);   // shared pool for GAS/OIL
	            } else {
	                if (gIn > 0 && oIn == 0) gasCap += 2 * gIn;  // dedicated GAS
	                if (oIn > 0 && gIn == 0) oilCap += 2 * oIn;  // dedicated OIL
	            }
	        }

	        //Current player holdings 
	        int coalHave = s.getFuel(playerId, Resource.COAL);
	        int gasHave  = s.getFuel(playerId, Resource.GAS);
	        int oilHave  = s.getFuel(playerId, Resource.OIL);
	        int urHave   = s.getFuel(playerId, Resource.URANIUM);

	        //Removes Coal & Uranium
	        int coalOver = Math.max(0, coalHave - coalCap);
	        if (coalOver > 0) decreaseFuel(s, playerId, Resource.COAL, coalOver);

	        int urOver = Math.max(0, urHave - urCap);
	        if (urOver > 0) decreaseFuel(s, playerId, Resource.URANIUM, urOver);

	        //Removes Gas/Oil with shared hybrid pool
	        int gasNeedHybrid = Math.max(0, gasHave - gasCap);  // portion that needs hybrid capacity
	        int oilNeedHybrid = Math.max(0, oilHave - oilCap);
	        int needFromHybrid = gasNeedHybrid + oilNeedHybrid;

	        if (needFromHybrid > hybridCap) {
	            int overflow = needFromHybrid - hybridCap; // total units to discard (gas+oil)

	            int removeGas = Math.min(gasNeedHybrid, overflow);
	            if (removeGas > 0) {
	                decreaseFuel(s, playerId, Resource.GAS, removeGas);
	                overflow -= removeGas;
	            }
	            if (overflow > 0) {
	                decreaseFuel(s, playerId, Resource.OIL, overflow);
	            }
	        }
	    }

	    /**
	     * Removes 'amount' of fuel from the player's storage and returns it to the market's discard pile.
	     * Assumes positive 'amount'; clamps to what the player actually has.
	     */
	    private void decreaseFuel(PowerGridGameState s, int playerId, Resource resource, int amount) {
	        if (amount <= 0) return;
	        int have = s.getFuel(playerId, resource);
	        int delta = Math.min(amount, have);
	        if (delta <= 0) return;
	        s.addFuel(playerId, resource, -delta);
	        s.getResourceMarket().returnToDiscardPile(resource, delta);
	    }

	    
	    


	}
