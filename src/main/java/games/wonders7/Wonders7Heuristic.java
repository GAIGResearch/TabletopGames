package games.wonders7;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;

import java.util.*;

public class Wonders7Heuristic extends TunableParameters implements IStateHeuristic {

    public Wonders7Heuristic(){}

    @Override
    public void _reset(){}

    @Override
    protected Wonders7Heuristic _copy() {
        return new Wonders7Heuristic();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof Wonders7Heuristic;
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        Wonders7GameState wgs = (Wonders7GameState) gs;

        double totalVP = 0.0; // Total Victory points
        double highestVP = 0.0; // Highest VP
        double lowestVP = 0.0; // Highest VP

        List<Map<Wonders7Constants.Resource, Integer>> playerResourcesCopy = new ArrayList<>();
        for (Map<Wonders7Constants.Resource, Integer> map : wgs.playerResources) {
            playerResourcesCopy.add(new EnumMap<>(map));  // Will be used to calculate everybody's VP scores
        }

        for (int i=0;i<wgs.getNPlayers();i++){
            // Evaluate military conflicts
            int nextplayer = (i+1)% wgs.getNPlayers();
            if(playerResourcesCopy.get(i).get(Wonders7Constants.Resource.Shield) > playerResourcesCopy.get(nextplayer).get(Wonders7Constants.Resource.Shield)){ // IF PLAYER i WINS
                playerResourcesCopy.get(i).put(Wonders7Constants.Resource.Victory,  playerResourcesCopy.get(i).get(Wonders7Constants.Resource.Victory)+(2*wgs.currentAge-1)); // 2N-1 POINTS FOR PLAYER i
                playerResourcesCopy.get(nextplayer).put(Wonders7Constants.Resource.Victory,  playerResourcesCopy.get(nextplayer).get(Wonders7Constants.Resource.Victory)-1); // -1 FOR THE PLAYER i+1
            }
            else if (playerResourcesCopy.get(i).get(Wonders7Constants.Resource.Shield) < playerResourcesCopy.get(nextplayer).get(Wonders7Constants.Resource.Shield)){ // IF PLAYER i+1 WINS
                playerResourcesCopy.get(i).put(Wonders7Constants.Resource.Victory,  playerResourcesCopy.get(i).get(Wonders7Constants.Resource.Victory)-1);// -1 POINT FOR THE PLAYER i
                playerResourcesCopy.get(nextplayer).put(Wonders7Constants.Resource.Victory,  playerResourcesCopy.get(nextplayer).get(Wonders7Constants.Resource.Victory)+(2*wgs.currentAge-1));// 2N-1 POINTS FOR PLAYER i+1
            }

            int vp = playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Victory);
            // Treasury
            vp += playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Coin)/3;
            // Scientific
            vp += (int)Math.pow(playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Cog),2);
            vp += (int)Math.pow(playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Compass),2);
            vp += (int)Math.pow(playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Tablet),2);
            // Sets of different science symbols
            vp += 7*Math.min(Math.min(playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Cog),playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Compass)),playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Tablet));
            playerResourcesCopy.get(i).put(Wonders7Constants.Resource.Victory, vp);
        }
        if (wgs.currentAge == 4) playerResourcesCopy.get(playerId).put(Wonders7Constants.Resource.Victory, wgs.playerResources.get(playerId).get(Wonders7Constants.Resource.Victory)); // If Game is completed and VP have already been calculated for players, use already calculated scores

        // Counts the accumulated total of each player in the game
        for (int i=0;i<wgs.getNPlayers();i++){
            totalVP += playerResourcesCopy.get(i).get(Wonders7Constants.Resource.Victory);
            if ((playerResourcesCopy.get(i).get(Wonders7Constants.Resource.Victory) > highestVP) && (i!=playerId)) highestVP = playerResourcesCopy.get(i).get(Wonders7Constants.Resource.Victory);
        }

        if (totalVP==0) return 0;
        return playerResourcesCopy.get(playerId).get(Wonders7Constants.Resource.Victory)/totalVP;
    }

    @Override
    public Wonders7Heuristic instantiate() {
        return this._copy();
    }
}
