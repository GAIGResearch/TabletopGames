package games.root;

import core.AbstractGameState;
import core.AbstractParameters;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.root.components.Item;

import java.util.List;

public class RootHeuristic extends TunableParameters implements IStateHeuristic {
    //TODO
    double ScorePlayerWeight = 3.0;
    double ScoreOpponentWeight = 1.0;
    double MapPresencePlayerWeight = 3.0;
    double MapPresenceOpponentWeight = 1.0;
    double HandQualityWeight = 1.0;
    double OpponentHandQualityWeight = 0.33;
    double FactionSpecificPlayerWeight = 1.0;
    double FactionSpecificOpponentWeight = 0.33;


    public RootHeuristic(){
        addTunableParameter("ScorePlayerWeight", 3.0);
        addTunableParameter("ScoreOpponentWeight", 1.0);
        addTunableParameter("MapPresencePlayerWeight", 3.0);
        addTunableParameter("MapPresenceOpponentWeight", 1.0);
        addTunableParameter("HandQualityWeight", 1.0);
        addTunableParameter("OpponentHandQualityWeight", 0.33);
        addTunableParameter("FactionSpecificPlayerWeight", 1.0);
        addTunableParameter("FactionSpecificOpponentWeight", 0.33);
    }
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        RootGameState gameState = (RootGameState) gs;
        double stateValue = 0;
        stateValue += this.ScorePlayerWeight * calculateWinConditionHeuristic(playerId, gameState);
        stateValue += this.MapPresencePlayerWeight * calculateMapPresence(gameState.getPlayerFaction(playerId), gameState);
        stateValue += this.HandQualityWeight * calculateHandQuality(playerId, gameState);
        stateValue += this.FactionSpecificPlayerWeight * calculateFactionSpecific(gameState.getPlayerFaction(playerId), gameState);

        for (int i = 0; i < gs.getNPlayers(); i++){
            if (i != playerId){
                stateValue -= this.ScoreOpponentWeight * calculateWinConditionHeuristic(i, gameState);
                stateValue -= this.MapPresenceOpponentWeight * calculateMapPresence(gameState.getPlayerFaction(i), gameState);
                stateValue -= this.OpponentHandQualityWeight * calculateHandQuality(i, gameState);
                stateValue -= this.FactionSpecificOpponentWeight * calculateFactionSpecific(gameState.getPlayerFaction(i), gameState);
            }
        }
        return stateValue/90;
    }

    private double calculateWinConditionHeuristic(int playerID, RootGameState gameState){
        if (gameState.getPlayerVictoryCondition(playerID) == RootParameters.VictoryCondition.Score) {
            return Math.min(gameState.getGameScore(playerID), ((RootParameters)gameState.getGameParameters()).scoreToEnd);
        } else {
            switch (gameState.getPlayerVictoryCondition(playerID)) {
                case DM -> {
                    return 7.5 * gameState.getGameMap().getNonForrestBoardNodes().stream().filter(node -> node.getClearingType() == RootParameters.ClearingTypes.Mouse && node.rulerID == playerID).count();
                }
                case DF -> {
                    return  7.5 * gameState.getGameMap().getNonForrestBoardNodes().stream().filter(node -> node.getClearingType() == RootParameters.ClearingTypes.Fox && node.rulerID == playerID).count();
                }
                case DR -> {
                    return 7.5 * gameState.getGameMap().getNonForrestBoardNodes().stream().filter(node -> node.getClearingType() == RootParameters.ClearingTypes.Rabbit && node.rulerID == playerID).count();

                }
                case DB -> {
                    return 7.5 * Math.max(gameState.getGameMap().getNonForrestBoardNodes().stream().filter(node -> node.getCorner() && node.rulerID == playerID && (node.identifier.equals("Top Left") || node.identifier.equals("Bottom Right"))).count(), gameState.getGameMap().getNonForrestBoardNodes().stream().filter(node -> node.getCorner() && node.rulerID == playerID && (node.identifier.equals("Top Right") || node.identifier.equals("Bottom Left"))).count());
                }
            }
        }
        return 0;
    }

    private double calculateMapPresence(RootParameters.Factions faction, RootGameState gameState){
        double ret = 0.0;
        switch (faction){
            case MarquiseDeCat:
                ret += 0.5 * (25.0 - gameState.getCatWarriors());
                ret += 0.5 *  gameState.getGameMap().getNonForrestBoardNodes().stream().filter(node -> node.rulerID == 0).count();
                ret += 0.64 * (6.0 - gameState.getBuildingCount(RootParameters.BuildingType.Sawmill));
                ret += 0.64 * (6.0 - gameState.getBuildingCount(RootParameters.BuildingType.Workshop));
                ret += 0.64 * (6.0 - gameState.getBuildingCount(RootParameters.BuildingType.Recruiter));
                break;
            case EyrieDynasties:
                ret += 0.85 * (20.0 - gameState.getBirdWarriors());
                ret += 0.5 * gameState.getGameMap().getNonForrestBoardNodes().stream().filter(node -> node.rulerID == 1).count();
                ret += 7.0 - gameState.getBuildingCount(RootParameters.BuildingType.Roost);
                break;
            case WoodlandAlliance:
                ret += 10.0 - gameState.getWoodlandWarriors();
                ret += 0.5 * gameState.getGameMap().getNonForrestBoardNodes().stream().filter(node -> node.rulerID == 2).count();
                ret += 2* (1.0 - gameState.foxBase);
                ret += 2*(1.0 - gameState.rabbitBase);
                ret += 2*(1.0 - gameState.mouseBase);
                ret += 10.0 - gameState.sympathyTokens;
                break;
            case Vagabond:
                ret += 2* gameState.satchel.stream().mapToDouble(item -> item.damaged ? 0.5 : (!item.refreshed ? 1.0 : 1.5)).sum();
                ret += 2* gameState.coins.stream().mapToDouble(item -> item.damaged ? 0.5 : (!item.refreshed ? 1.0 : 1.5)).sum();
                ret += 2* gameState.bags.stream().mapToDouble(item -> item.damaged ? 0.5 : (!item.refreshed ? 1.0 : 1.5)).sum();
                ret += 2* gameState.teas.stream().mapToDouble(item -> item.damaged ? 0.5 : (!item.refreshed ? 1.0 : 1.5)).sum();
                break;
        }
        return ret;
    }

    private double calculateHandQuality(int playerID, RootGameState gameState){
        //The general rule in root dictates that bird cards have higher value than regular suits -> the agents should care about the size of a hand + number of "better" bird cards
        double ret = 0.0;
        for (int i = 0 ; i < gameState.getPlayerHand(playerID).getSize(); i++){
            if (gameState.getPlayerHand(playerID).get(i).suit == RootParameters.ClearingTypes.Bird){
                ret += 2.;
            }else {
                ret += 1.;
            }
        }
        for (int i = 0; i < gameState.getPlayerCraftedCards(playerID).getSize(); i++){
            ret += 2;
        }
        return Math.min(ret, 30.0);
    }

    private double calculateFactionSpecific(RootParameters.Factions faction, RootGameState gameState){
        double ret = 0;
        switch (faction){
            case MarquiseDeCat -> ret += 3.75 *(8 - gameState.Wood);
            case EyrieDynasties -> ret += Math.min((gameState.eyrieDecree.get(0).getSize() + gameState.eyrieDecree.get(1).getSize() + gameState.eyrieDecree.get(2).getSize() + gameState.eyrieDecree.get(3).getSize()), 30.0);
            case WoodlandAlliance -> ret += Math.min(3.0 * gameState.supporters.getSize(), 30.0);
            case Vagabond -> ret += Math.min(gameState.foxQuests + gameState.rabbitQuests + gameState.mouseQuests + gameState.craftedItems.stream().mapToInt(List<Item>::size).sum(), 30.0);
        }
        return ret;
    }

    public double[] getWeights(){
        return new double[]{ScorePlayerWeight, ScoreOpponentWeight, MapPresencePlayerWeight, MapPresenceOpponentWeight, HandQualityWeight, OpponentHandQualityWeight, FactionSpecificPlayerWeight, FactionSpecificOpponentWeight};
    }
    @Override
    protected AbstractParameters _copy() {
        return new RootHeuristic();
    }

    @Override
    protected boolean _equals(Object o) {
        if (o == this){return true;}
        if (o instanceof RootHeuristic rh){
            return ScorePlayerWeight == rh.ScorePlayerWeight && ScoreOpponentWeight == rh.ScoreOpponentWeight && MapPresencePlayerWeight == rh.MapPresencePlayerWeight && MapPresenceOpponentWeight == rh.MapPresenceOpponentWeight && HandQualityWeight == rh.HandQualityWeight && OpponentHandQualityWeight == rh.OpponentHandQualityWeight && FactionSpecificPlayerWeight == rh.FactionSpecificPlayerWeight && FactionSpecificOpponentWeight == rh.FactionSpecificOpponentWeight;
        }
        return false;
    }

    @Override
    public Object instantiate() {
        return _copy();
    }

    @Override
    public void _reset() {
        ScorePlayerWeight = (double)getParameterValue("ScorePlayerWeight");
        ScoreOpponentWeight = (double) getParameterValue("ScoreOpponentWeight");
        MapPresencePlayerWeight = (double) getParameterValue("MapPresencePlayerWeight");
        MapPresenceOpponentWeight = (double) getParameterValue("MapPresenceOpponentWeight");
        HandQualityWeight = (double) getParameterValue("HandQualityWeight");
        OpponentHandQualityWeight = (double) getParameterValue("OpponentHandQualityWeight");
        FactionSpecificPlayerWeight = (double) getParameterValue("FactionSpecificPlayerWeight");
        FactionSpecificOpponentWeight = (double) getParameterValue("FactionSpecificOpponentWeight");
    }
}
