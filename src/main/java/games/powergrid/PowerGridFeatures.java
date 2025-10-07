package games.powergrid;

import core.AbstractGameState;
import core.interfaces.IStateFeatureJSON;
import core.interfaces.IStateFeatureVector;
import games.powergrid.PowerGridParameters.Resource;

public class PowerGridFeatures implements IStateFeatureVector, IStateFeatureJSON {
	
	

	@Override
	public String getObservationJson(AbstractGameState gameState, int playerId) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        PowerGridGameState pggs = (PowerGridGameState) state;
        double[] retVal = new double[getObservationSpace()];
        //Current Player Observation 
        retVal[0] = pggs.getPlayersMoney(playerID);
        retVal[1] = pggs.getFuel(playerID, Resource.COAL);
        retVal[2] = pggs.getFuel(playerID, Resource.GAS);
        retVal[3] = pggs.getFuel(playerID, Resource.OIL);
        retVal[4] = pggs.getFuel(playerID, Resource.URANIUM);
        retVal[5] = pggs.getCityCountByPlayer(playerID);
        retVal[6] = pggs.getPlayerCapacity(playerID);
        retVal[7] = pggs.getIncome(playerID);

		return retVal;
	}

	@Override
	public String[] names() {
		return new String[]{ "Player Money", "Resource:Coal","Resource:Gas","Resource:Oil","Resource:Uranium", "Generator", "Capacity", "Income"};
	}  
    public int getObservationSpace() {
        return names().length;
    }
}
