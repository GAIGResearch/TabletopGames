package games.diamant;

import core.AbstractGameState;
import evaluation.features.TunableStateFeatures;
import games.diamant.cards.DiamantCard;
import players.heuristics.LeaderHeuristic;

public class DiamantSimpleFeatures extends TunableStateFeatures {
    LeaderHeuristic heuristic = new LeaderHeuristic();

    static String[] allNames =  new String[]{"TreasureChests", "GemsOnPath", "PlayersInCave", "Cave", "Hazards",
        "PlayerOrdinalPos", "PlayersStillInCaveEncoded", "LeaderHeuristic", "Tiles"};

    public DiamantSimpleFeatures() {
        super(allNames);
    }
    @Override
    public double[] fullFeatureVector(AbstractGameState gameState, int playerId) {
        DiamantGameState gs = (DiamantGameState) gameState;

        double[] retVal = new double[allNames.length];
        retVal[0] = gs.getTreasureChests().get(playerId).getValue();
        retVal[1] = gs.path.getComponents().get(gs.path.getSize()-1).getValue(); // nGemsOnPath;
        retVal[2] = gs.playerInCave.size();
        retVal[3] = gs.nCave;

        // hazards on path - we only care about if we have seen a type or not
        retVal[4] = gs.getPath().stream().
                filter(c -> c.getCardType() == DiamantCard.DiamantCardType.Hazard)
                .count();

        // player's ordinal position
        retVal[5] = gs.getOrdinalPosition(playerId);

        // ordinal position of players still in cave
        int inCaveOrdinalPos = 0;
        for (int i = 0; i < gs.playerInCave.size(); i++){
            if (gs.playerInCave.get(i)){
                inCaveOrdinalPos += (int) Math.pow(2, i);
            }
        }
        retVal[6] = inCaveOrdinalPos;
        retVal[7] = heuristic.evaluateState(gs, playerId);
        retVal[8] = gs.path.getSize();

        return retVal;
    }

    public int getObservationSpace() {
        return names().length;
    }

    @Override
    protected DiamantSimpleFeatures _copy() {
        return new DiamantSimpleFeatures();
    }

    @Override
    public Object getKey(AbstractGameState state) {
        return super.getKey(state);
    }
}
