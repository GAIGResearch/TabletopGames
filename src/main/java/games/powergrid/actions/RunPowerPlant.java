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

public class RunPowerPlant extends AbstractAction {

	    private final int plantId;  
	    private final EnumMap<Resource, Integer> spend;   

	    public RunPowerPlant(int plantId, Map<Resource, Integer> spend) {
	        this.plantId = plantId;
	        this.spend = new EnumMap<>(spend); // defensive copy
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

	    @Override
	    public String toString() {
	        return "RunPowerPlant(plantId=" + plantId + ", spend=" + spend + ")";
	    }

	    @Override
	    public AbstractAction copy() {
	        // deep copy the EnumMap so the action is immutable
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
	        // concise, UI-friendly label
	        return "Run plant #" + plantId + " using " + spend;
	    }



}
