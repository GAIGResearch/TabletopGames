package games.virus;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import games.virus.components.VirusOrgan;
import utilities.Utils;

public class VirusHeuristic implements IStateHeuristic {

    // Simple count of healthy organs

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        VirusGameState vgs = (VirusGameState) gs;
        Utils.GameResult playerResult = gs.getPlayerResults()[playerId];

        if (!vgs.isNotTerminal())
            return playerResult.value;
        int nHealthyOrgans = 0;
        for (VirusOrgan o : vgs.playerBodies.get(playerId).organs.values()) {
            if (o.isHealthy()) {
                nHealthyOrgans++;
            }
        }
        return nHealthyOrgans * 1.0 / vgs.playerBodies.get(playerId).organs.size();
    }
}