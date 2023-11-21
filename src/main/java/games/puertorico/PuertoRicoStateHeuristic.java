package games.puertorico;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

public class PuertoRicoStateHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        PuertoRicoGameState state = (PuertoRicoGameState) gs;

        if (state.isNotTerminal()) {
            double score = state.getGameScore(playerId);
            double plantations = state.getPlayerBoard(playerId).getPlantationSize();
            double buildings = state.getPlayerBoard(playerId).getBuildings().size();
            double doubloons = state.getPlayerBoard(playerId).getDoubloons();
            double stores = state.getPlayerBoard(playerId).getStores().values().stream().mapToInt(Integer::intValue).sum();

            // ugly hardcoded numbers with a comment! (amendable to parameters if needed)
            return score / 100.0 + plantations / 100.0 + buildings / 100.0 + doubloons / 200.0 + stores / 200.0;
        } else {
            return state.getPlayerResults()[playerId].value;
        }

    }
}
