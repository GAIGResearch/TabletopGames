package games.dotsboxes;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;

import java.util.*;

public class DBFeatures implements IStateFeatureVector {

    /*
    Gets the observation vector for Dots and Boxes encoded as a vector of size of the number of edges
    The value 1 means that edge belongs to the current player, -1 belongs to opponent and 0 is empty
     */
    @Override
    public double[] featureVector(AbstractGameState state, int playerID) {
        DBGameState dbState = (DBGameState) state;
//        int no_players = state.getNPlayers();
        int currentPlayer = state.getCurrentPlayer();
        // Create a feature vector for each edge and no. players
        double[] featureVector = new double[dbState.edges.size()]; // * no_players];

        // Order the edges so that the feature vector is consistent
        TreeSet<DBEdge> sortedEdges = new TreeSet<>(dbState.edges);

        // For edge in game, check its owner (if it has one) and add it to the feature vector
        int currentEdge = 0;
        for (DBEdge edge : sortedEdges) {

            // Find owner of edge
            int owner = dbState.edgeToOwnerMap.getOrDefault(edge, -1);

            // If edge is owned by a player, one hot encode it to the feature vector, else leave empty
            if (owner == currentPlayer) {
                featureVector[currentEdge] = 1;
            } else if (owner == -1){
                // empty edge
                featureVector[currentEdge] = 0;
            } else {
                // edge owned by opponent
                featureVector[currentEdge] = -1;
            }
            // Increment to next edge
            currentEdge += 1;
        }
        return featureVector;
    }

    @Override
    public String[] names() {
        return new String[82];
    }
}
