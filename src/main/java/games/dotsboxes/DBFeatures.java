package games.dotsboxes;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;

public class DBFeatures implements IStateFeatureVector {

    /*
    Gets the observation vector for Dots and Boxes
    Vector length = no. edges * no. players
    Each edge will be one hot encoded for the player that owns it
    (e.g. if player 1 owns the edge, the vector will be [1, 0, 0, 0, 0, 0]) for a 6 player game with one edge)
    example vector for 2 players and 3 edges: [1, 0, 0, 1, 0, 1] (player 1 owns edge 1 and player 2 owns edge 2 and 3)
     */
    @Override
    public double[] featureVector(AbstractGameState state, int playerID) {
        DBGameState dbState = (DBGameState) state;
        int no_players = state.getNPlayers();

        // Create a feature vector for each edge and no. players
        double[] featureVector = new double[dbState.edges.size() * no_players];

        // For edge in game, check its owner (if it has one) and add it to the feature vector
        int currentEdge = 0;
        for (DBEdge edge : dbState.edges) {

            // Find owner of edge
            int owner = dbState.edgeToOwnerMap.getOrDefault(edge, -1);

            // If edge is owned by a player, one hot encode it to the feature vector, else leave empty
            if (owner != -1) {
                featureVector[currentEdge + owner] = 1;
            }
            // Increment to next edge
            currentEdge += no_players;
        }
        return featureVector;
    }

    @Override
    public String[] names() {
        return new String[0];
    }
}
