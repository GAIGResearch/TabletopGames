package core.interfaces;

import core.AbstractGameState;

public interface IStateFeatureJSON {

    // Converts the gameState/observation into JSON format
    String getObservationJson(AbstractGameState gameState, int playerId);
}
