package core.interfaces;

import utilities.VectorObservation;

public interface IVectorObservation {

    /**
     * Encode the game state into a vector (fixed length during game).
     * @return - a vector observation.
     */
    VectorObservation getVectorObservation();

}
