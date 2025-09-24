package games.diamant;

import core.AbstractGameState;
import core.interfaces.IStateFeatureJSON;
import core.interfaces.IStateFeatureVector;
import games.diamant.cards.DiamantCard;
import org.json.simple.JSONObject;

import java.util.Map;
import java.util.stream.Collectors;

public class DiamantFeatures implements IStateFeatureVector, IStateFeatureJSON {

    @Override
    public String getObservationJson(AbstractGameState gameState, int playerId) {
        DiamantGameState gs = (DiamantGameState) gameState;
        final JSONObject json = new JSONObject();
        Map< DiamantCard.HazardType, Long> hazardsOnPath = gs.getHazardsOnPath();
        json.put("cave", gs.nCave);
        json.put("playersInCave", gs.playerInCave.size());
        json.put("pathNoGems", gs.gemsOnPath);
        json.put("chestNoGems", gs.getTreasureChests().get(playerId).getValue());
        json.put("hazardScorpionsOnPath", hazardsOnPath.get(DiamantCard.HazardType.Scorpions));
        json.put("hazardSnakesOnPath", hazardsOnPath.get(DiamantCard.HazardType.Snakes));
        json.put("hazardPoisonGasOnPath", hazardsOnPath.get(DiamantCard.HazardType.PoisonGas));
        json.put("hazardExplosionsOnPath", hazardsOnPath.get(DiamantCard.HazardType.Explosions));
        json.put("hazardRockfallsOnPath", hazardsOnPath.get(DiamantCard.HazardType.Rockfalls));
        return json.toJSONString();
    }

    @Override
    public String[] names(){
        return new String[]{"TreasureChests", "GemsOnPath", "PlayersInCave", "Cave", "Explosions", "Poison", "Rockfalls", "Scorpions", "Snakes"};
    }

    @Override
    public double[] doubleVector(AbstractGameState gameState, int playerId) {
        DiamantGameState gs = (DiamantGameState) gameState;
        double[] retVal = new double[getObservationSpace()];
        retVal[0] = gs.getTreasureChests().get(playerId).getValue();
        retVal[1] = gs.path.getComponents().get(gs.path.getSize()-1).getValue(); // nGemsOnPath;
        retVal[2] = gs.playerInCave.stream().filter(b -> b).count();
        retVal[3] = gs.nCave;
        Map< DiamantCard.HazardType, Long> hazardsOnPath = gs.getHazardsOnPath();
        retVal[4] = hazardsOnPath.get(DiamantCard.HazardType.Explosions) != null ? hazardsOnPath.get(DiamantCard.HazardType.Explosions) : 0;
        retVal[5] = hazardsOnPath.get(DiamantCard.HazardType.PoisonGas) != null ? hazardsOnPath.get(DiamantCard.HazardType.PoisonGas) : 0;
        retVal[6] = hazardsOnPath.get(DiamantCard.HazardType.Rockfalls) != null ? hazardsOnPath.get(DiamantCard.HazardType.Rockfalls) : 0;
        retVal[7] = hazardsOnPath.get(DiamantCard.HazardType.Scorpions) != null ? hazardsOnPath.get(DiamantCard.HazardType.Scorpions) : 0;
        retVal[8] = hazardsOnPath.get(DiamantCard.HazardType.Snakes) != null ? hazardsOnPath.get(DiamantCard.HazardType.Snakes) : 0;
        return retVal;
    }

    public int getObservationSpace() {
        return names().length;
    }
}
