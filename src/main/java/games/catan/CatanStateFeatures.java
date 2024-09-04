package games.catan;

import core.AbstractGameState;
import core.components.BoardNodeWithEdges;
import core.interfaces.IStateFeatureVector;
import games.catan.actions.build.BuyAction;
import games.catan.components.Building;
import games.catan.components.CatanTile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static games.catan.CatanParameters.Resource.*;

public class CatanStateFeatures implements IStateFeatureVector {

    String[] localNames = new String[]{
            "TURN",
            "ROUND",
            "SCORE",
            "OTHER_SCORE",
            "SCORE_DELTA",
            "SETTLEMENTS",
            "CITIES",
            "ROADS",
            "CARDS_IN_HAND",
            "CARDS_OVER_SEVEN",
            "GRAIN_INCOME", // can express as Expected gain per roll
            "WOOL_INCOME",
            "BRICK_INCOME",
            "ORE_INCOME",
            "WOOD_INCOME",
            "MIN_INCOME",
            "MAX_INCOME",
            "TOTAL_INCOME",
            "ROBBER_VALUE_OWN",
            "ROBBER_VALUE_OTHER",
            "GRAIN_IN_HAND",
            "WOOL_IN_HAND",
            "BRICK_IN_HAND",
            "ORE_IN_HAND",
            "WOOD_IN_HAND",
            "DUPLICATE_HEXES",
            "LONGEST_ROAD_US",
            "LARGEST_ARMY_US",
            "LONGEST_ROAD_OTHER",
            "LARGEST_ARMY_OTHER",
            "DEV_CARDS",
            "KNIGHTS",
            "KNIGHTS_DIFF"
    };


    @Override
    public double[] featureVector(AbstractGameState state, int playerID) {
        double[] retValue = new double[localNames.length];
        CatanGameState catanState = (CatanGameState) state;
        retValue[0] = catanState.getTurnCounter();
        retValue[1] = catanState.getRoundCounter();
        retValue[2] = catanState.getGameScore(playerID);
        double otherScore = 0.0;
        for (int i = 0; i < state.getNPlayers(); i++) {
            if (i != playerID && catanState.getGameScore(i) > otherScore) {
                otherScore = catanState.getGameScore(i);
                break;
            }
        }
        retValue[3] = otherScore;
        retValue[4] = retValue[2] - retValue[3];
        List<BoardNodeWithEdges> playersSettlements = catanState.getPlayersSettlements(playerID);
        retValue[5] = playersSettlements.stream()
                .filter(node -> node instanceof Building b && b.getBuildingType() == Building.Type.Settlement).count();
        retValue[6] = playersSettlements.stream()
                .filter(node -> node instanceof Building b && b.getBuildingType() == Building.Type.City).count();
        retValue[7] = catanState.getPlayerTokens().get(playerID).get(BuyAction.BuyType.Road).getValue();
        retValue[8] = catanState.getNResourcesInHand(playerID);
        retValue[9] = Math.max(0, retValue[8] - 7);
        // income for each resource is the expected value of that resource per roll
        // there is no access to CatanTile from BoardNode, so we have to iterate over all tiles
        List<CatanTile> allTiles = new ArrayList<>(50);
        for (int i = 0; i < catanState.getBoard().length; i++) {
            allTiles.addAll(Arrays.asList(catanState.getBoard()[i]));
        }
        for (CatanTile tile : allTiles) {
            if (tile.getNumber() == 0) continue; // DESERT, SEA
            double income = switch (tile.getNumber()) {
                case 2, 12 -> 1.0;
                case 3, 11 -> 2.0;
                case 4, 10 -> 3.0;
                case 5, 9 -> 4.0;
                case 6, 8 -> 5.0;
                default -> 0;
            };

            int incomeIndex = switch (tile.getTileType()) {
                case FIELDS -> 0;
                case PASTURE -> 1;
                case HILLS -> 2;
                case MOUNTAINS -> 3;
                case FOREST -> 4;
                default -> throw new IllegalStateException("Unexpected value: " + tile.getTileType());
            };

            int count = 0;
            for (Building b : catanState.getBuildings(tile)) {
                if (b.getOwnerId() == -1 ) continue; // unowned
                if (b.getOwnerId() != playerID && !tile.hasRobber()) continue; // other player's building (and robber is not there)
                switch (b.getBuildingType()) {
                    case Settlement -> income *= 1.0;
                    case City -> income *= 2.0;
                }

                if (b.getOwnerId() == playerID) {
                    count++;
                    if (count > 1) {
                        // add one duplicate hexes
                        retValue[25] += 1.0;
                    }
                }

                if (tile.hasRobber()) {
                    if (b.getOwnerId() == playerID) {
                        // our loss
                        retValue[18] += income;
                    } else {
                        // other player's loss
                        retValue[19] += income;
                    }
                } else {
                    // the usual case, our income
                    retValue[10 + incomeIndex] += income;
                }
            }

            retValue[15] = 10.0; // min income
            retValue[16] = -10.0; // max income
            for (int i = 0; i < 5; i++) {
                retValue[15] = Math.min(retValue[15], retValue[10 + i]);
                retValue[16] = Math.max(retValue[16], retValue[10 + i]);
                retValue[17] += retValue[10 + i];  // total income
            }
        }

        retValue[20] = catanState.getPlayerResources(playerID).get(GRAIN).getValue();
        retValue[21] = catanState.getPlayerResources(playerID).get(WOOL).getValue();
        retValue[22] = catanState.getPlayerResources(playerID).get(BRICK).getValue();
        retValue[23] = catanState.getPlayerResources(playerID).get(ORE).getValue();
        retValue[24] = catanState.getPlayerResources(playerID).get(LUMBER).getValue();

        int longestRoad = catanState.getLongestRoadOwner();
        int largestArmy = catanState.getLargestArmyOwner();
        retValue[26] = longestRoad == playerID ? 1.0 : 0.0;
        retValue[27] = largestArmy == playerID ? 1.0 : 0.0;
        retValue[28] = longestRoad != playerID && longestRoad > -1 ? 1.0 : 0.0;
        retValue[29] = largestArmy != playerID && largestArmy > -1 ? 1.0 : 0.0;

        retValue[30] = catanState.getPlayerDevCards(playerID).getSize();
        retValue[31] = catanState.getKnights()[playerID];
        int otherKnights = 0;
        for (int i = 0; i < state.getNPlayers(); i++) {
            if (i != playerID) {
                otherKnights = Math.max(otherKnights, catanState.getKnights()[i]);
            }
        }
        retValue[32] = retValue[31] - otherKnights;
        return retValue;

    }

    @Override
    public String[] names() {
        return localNames;
    }
}
