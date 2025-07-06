package games.diamant;

import core.AbstractGameState;
import core.interfaces.IStateFeatureJSON;
import core.interfaces.IStateFeatureVector;
import org.json.simple.JSONObject;

public class DiamantFeatures implements IStateFeatureVector, IStateFeatureJSON {

    @Override
    public String getObservationJson(AbstractGameState gameState, int playerId) {
        DiamantGameState gs = (DiamantGameState) gameState;
        final JSONObject json = new JSONObject();
        json.put("cave", gs.nCave);
        json.put("playersInCave", gs.playerInCave.size());
        json.put("pathNoGems", gs.gemsOnPath);
        json.put("chestNoGems", gs.getTreasureChests().get(playerId).getValue());
        json.put("hazardScorpionsOnPath", gs.nHazardExplosionsOnPath);
        json.put("hazardSnakesOnPath", gs.nHazardSnakesOnPath);
        json.put("hazardRockfallsOnPath", gs.nHazardRockfallsOnPath);
        json.put("hazardPoisonOnPath", gs.nHazardPoisonGasOnPath);
        json.put("hazardExplosionsOnPath", gs.nHazardExplosionsOnPath);
        return json.toJSONString();
    }

    @Override
    public String[] names(){
        return new String[]{"TreasueChests", "GemsOnPath", "PlayersInCave", "Cave", "Explosions", "Poison", "Rockfalls", "Scorpions", "Snakes"};
    }

    @Override
    public double[] featureVector(AbstractGameState gameState, int playerId) {
        DiamantGameState gs = (DiamantGameState) gameState;
        double[] retVal = new double[getObservationSpace()];
        retVal[0] = gs.getTreasureChests().get(playerId).getValue();
        retVal[1] = gs.path.getComponents().get(gs.path.getSize()-1).getValue(); // nGemsOnPath;
        retVal[2] = gs.playerInCave.size();
        retVal[3] = gs.nCave;
        retVal[4] = gs.nHazardExplosionsOnPath;
        retVal[5] = gs.nHazardPoisonGasOnPath;
        retVal[6] = gs.nHazardRockfallsOnPath;
        retVal[7] = gs.nHazardScorpionsOnPath;
        retVal[8] = gs.nHazardSnakesOnPath;
        return retVal;
    }



    public int getObservationSpace() {
        return names().length;
    }
}
