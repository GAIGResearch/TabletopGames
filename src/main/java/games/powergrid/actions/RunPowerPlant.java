package games.powergrid.actions;

import java.util.EnumMap;
import java.util.Map;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.powergrid.PowerGridGameState;
import games.powergrid.PowerGridParameters.Resource;
import games.powergrid.components.PowerGridCard;
import games.powergrid.components.PowerGridCard.PlantInput;
import games.powergrid.components.PowerGridResourceMarket;

/**
 * Action that operates a specific power plant owned by the current player,
 * consuming the specified fuel and producing electricity to power cities
 * equal to the plant's capacity.
 * <p>
 * Execution rules:
 * <ul>
 *   <li>The current player must own {@code plantId}.</li>
 *   <li>The plant must not have been run already this Bureaucracy phase.</li>
 *   <li>The action spends the provided {@code spend} amounts from the player's fuel stores,
 *       returning them to the market's discard pile.</li>
 *   <li>On success, the plant is marked as run and the player's powered city count
 *       increases by the plant's capacity (capped elsewhere by city ownership).</li>
 * </ul>
 *
 * <p><b>Side effects:</b> Mutates {@link PowerGridGameState} by:
 * <ul>
 *   <li>Reducing the player's stored fuel,</li>
 *   <li>Returning consumed fuel to discard pile kept in {@link PowerGridResourceMarket},</li>
 *   <li>Marking the plant as run for this phase,</li>
 *   <li>Increasing powered cities via {@link PowerGridGameState#addPoweredCities(int, int)}.</li>
 * </ul>
 *
 * @see PowerGridGameState
 * @see PowerGridCard
 * @see PowerGridResourceMarket
 * @see games.powergrid.PowerGridParameters.Resource
 */

public class RunPowerPlant extends AbstractAction {

	    private final int plantId;  
	    private final EnumMap<Resource, Integer> spend;   

	    public RunPowerPlant(int plantId, Map<Resource, Integer> spend) {
	        this.plantId = plantId;
	        this.spend = new EnumMap<>(spend); 
	    }

	    @Override
	    public boolean execute(AbstractGameState gs) {
	        PowerGridGameState s = (PowerGridGameState) gs;
	        PowerGridResourceMarket resourceMarket = s.getResourceMarket();
	        int player = gs.getCurrentPlayer();
	      

	        if (!s.playerOwnsPlant(gs.getCurrentPlayer(), plantId)) return false;
	        if (s.hasPlantRun(plantId)) return false;

	        Deck<PowerGridCard> owned_plants = s.getPlayerPlantDeck(player);
	        PowerGridCard plant_card = null;
	        for (PowerGridCard card : owned_plants) {
	        	if (card.getNumber() == plantId) {
	        		plant_card = card; }
	        	}
	        int capacity = plant_card.getCapacity();
	        
	        s.markPlantRun(plantId);
	        for (Map.Entry<Resource, Integer> entry : spend.entrySet()) {
	            Resource r = entry.getKey();
	            int amount = entry.getValue();
	            resourceMarket.returnToDiscardPile(r, amount);
	            s.removeFuel(player, r, amount);              
	            
	        }
	        s.addPoweredCities(player, capacity);
	        return true;
	    }
	    
	    public int getPlantId() {
	    	return this.plantId; 
	    }
	    
	    public EnumMap<Resource, Integer> getSpend(){
	    	return this.spend; 
	    }

	    @Override
	    public String toString() {
	        return "RunPowerPlant(plantId=" + plantId + ", spend=" + spend + ")";
	    }

	    @Override
	    public AbstractAction copy() {
	        return new RunPowerPlant(plantId, new EnumMap<>(spend));
	    }

	    @Override
	    public boolean equals(Object obj) {
	        if (this == obj) return true;
	        if (!(obj instanceof RunPowerPlant)) return false;
	        RunPowerPlant other = (RunPowerPlant) obj;
	        return this.plantId == other.plantId && this.spend.equals(other.spend);
	    }

	    @Override
	    public int hashCode() {
	        return java.util.Objects.hash(plantId, spend);
	    }

	    @Override
	    public String getString(AbstractGameState gameState) {
	        return "Run plant #" + plantId + " using " + spend;
	    }



}
