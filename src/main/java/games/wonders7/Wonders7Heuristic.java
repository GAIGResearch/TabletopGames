package games.wonders7;

import core.AbstractGameState;

import core.interfaces.IStateHeuristic;
import evaluation.TunableParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

        List<HashMap<Wonders7Constants.resources, Integer>> playerResourcesCopy = new ArrayList<>();
        for (HashMap<Wonders7Constants.resources, Integer> map : wgs.playerResources) {
            playerResourcesCopy.add(new HashMap<>(map));  // Will be used to calculate everybody's VP scores
        }

        for (int i=0;i<wgs.getNPlayers();i++){
            // Evaluate military conflicts
            int nextplayer = (i+1)% wgs.getNPlayers();
            if(playerResourcesCopy.get(i).get(Wonders7Constants.resources.shield) > playerResourcesCopy.get(nextplayer).get(Wonders7Constants.resources.shield)){ // IF PLAYER i WINS
                playerResourcesCopy.get(i).put(Wonders7Constants.resources.victory,  playerResourcesCopy.get(i).get(Wonders7Constants.resources.victory)+(2*wgs.currentAge-1)); // 2N-1 POINTS FOR PLAYER i
                playerResourcesCopy.get(nextplayer).put(Wonders7Constants.resources.victory,  playerResourcesCopy.get(nextplayer).get(Wonders7Constants.resources.victory)-1); // -1 FOR THE PLAYER i+1
            }
            else if (playerResourcesCopy.get(i).get(Wonders7Constants.resources.shield) < playerResourcesCopy.get(nextplayer).get(Wonders7Constants.resources.shield)){ // IF PLAYER i+1 WINS
                playerResourcesCopy.get(i).put(Wonders7Constants.resources.victory,  playerResourcesCopy.get(i).get(Wonders7Constants.resources.victory)-1);// -1 POINT FOR THE PLAYER i
                playerResourcesCopy.get(nextplayer).put(Wonders7Constants.resources.victory,  playerResourcesCopy.get(nextplayer).get(Wonders7Constants.resources.victory)+(2*wgs.currentAge-1));// 2N-1 POINTS FOR PLAYER i+1
            }

            // Treasury
            playerResourcesCopy.get(i).put(Wonders7Constants.resources.victory, playerResourcesCopy.get(i).get(Wonders7Constants.resources.victory)+playerResourcesCopy.get(i).get(Wonders7Constants.resources.coin)/3);
            // Scientific
            playerResourcesCopy.get(i).put(Wonders7Constants.resources.victory, playerResourcesCopy.get(i).get(Wonders7Constants.resources.victory)+(int)Math.pow(playerResourcesCopy.get(i).get(Wonders7Constants.resources.cog),2));
            playerResourcesCopy.get(i).put(Wonders7Constants.resources.victory, playerResourcesCopy.get(i).get(Wonders7Constants.resources.victory)+(int)Math.pow(playerResourcesCopy.get(i).get(Wonders7Constants.resources.compass),2));
            playerResourcesCopy.get(i).put(Wonders7Constants.resources.victory, playerResourcesCopy.get(i).get(Wonders7Constants.resources.victory)+(int)Math.pow(playerResourcesCopy.get(i).get(Wonders7Constants.resources.tablet),2));
            playerResourcesCopy.get(i).put(Wonders7Constants.resources.victory, playerResourcesCopy.get(i).get(Wonders7Constants.resources.victory)+7*Math.min(Math.min(playerResourcesCopy.get(i).get(Wonders7Constants.resources.cog),playerResourcesCopy.get(i).get(Wonders7Constants.resources.compass)),playerResourcesCopy.get(i).get(Wonders7Constants.resources.tablet))); // Sets of different science symbols
        }
        if (wgs.currentAge == 4) playerResourcesCopy.get(playerId).put(Wonders7Constants.resources.victory, wgs.playerResources.get(playerId).get(Wonders7Constants.resources.victory)); // If Game is completed and VP have already been calculated for players, use already calculated scores

        // Counts the accumulated total of each player in the game
        for (int i=0;i<wgs.getNPlayers();i++){
            totalVP += playerResourcesCopy.get(i).get(Wonders7Constants.resources.victory);
            if ((playerResourcesCopy.get(i).get(Wonders7Constants.resources.victory) > highestVP) && (i!=playerId)) highestVP = playerResourcesCopy.get(i).get(Wonders7Constants.resources.victory);
        }

        if (totalVP==0) return 0;
        return playerResourcesCopy.get(playerId).get(Wonders7Constants.resources.victory)/totalVP;
    }

    @Override
    public Wonders7Heuristic instantiate() {
        return this._copy();
    }
}
