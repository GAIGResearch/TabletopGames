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
        json.put("pathNoGems", gs.nGemsOnPath);
        json.put("chestNoGems", gs.getTreasureChests().get(playerId).getValue());
        json.put("hazardScorpionsOnPath", gs.nHazardExplosionsOnPath);
        json.put("hazardSnakesOnPath", gs.nHazardSnakesOnPath);
        json.put("hazardRockfallsOnPath", gs.nHazardRockfallsOnPath);
        json.put("hazardPoisonOnPath", gs.nHazardPoissonGasOnPath);
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
        retVal[1] = gs.path.getComponents().get(gs.path.getSize()-1).getNumberOfGems(); // nGemsOnPath;
        retVal[2] = gs.playerInCave.size();
        retVal[3] = gs.nCave;
        retVal[4] = gs.nHazardExplosionsOnPath;
        retVal[5] = gs.nHazardPoissonGasOnPath;
        retVal[6] = gs.nHazardRockfallsOnPath;
        retVal[7] = gs.nHazardScorpionsOnPath;
        retVal[8] = gs.nHazardSnakesOnPath;
        return retVal;
    }

//    public double[] normFeatureVector() {
//        double[] retVal = new double[getObservationSpace()];
//        retVal[0] = getTreasureChests().get(getCurrentPlayer()).getValue() / 100d;
//        retVal[1] = path.getComponents().get(path.getSize()-1).getNumberOfGems() / 17d;
//        retVal[2] = playerInCave.size() / (double) getNPlayers();
//        retVal[3] = nCave / 5d;
//        retVal[4] = nHazardExplosionsOnPath / 3d;
//        retVal[5] = nHazardPoissonGasOnPath/ 3d;
//        retVal[6] = nHazardRockfallsOnPath /3d;
//        retVal[7] = nHazardScorpionsOnPath /3d;
//        retVal[8] = nHazardSnakesOnPath/ 3d;
//        return retVal;
//    }



    public int getObservationSpace() {
        return names().length;
    }
}
