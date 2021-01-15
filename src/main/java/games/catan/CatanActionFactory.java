package games.catan;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.catan.actions.BuildRoad;
import games.catan.actions.BuildSettlement;
import games.catan.actions.PlaceSettlementWithRoad;
import games.catan.components.Graph;
import games.catan.components.Road;
import games.catan.components.Settlement;

import java.util.ArrayList;
import java.util.List;

public class CatanActionFactory {
    /**
     * Calculates setup actions
     * @return - ArrayList, various action types (unique).
     */
    static List<AbstractAction> getSetupActions(CatanGameState gs) {
        CatanParameters catanParameters = (CatanParameters) gs.getGameParameters();
        int turnStep = ((CatanTurnOrder) gs.getTurnOrder()).turnStep;
        int activePlayer = gs.getTurnOrder().getCurrentPlayer(gs);
        ArrayList<AbstractAction> actions = new ArrayList();

        // find possible settlement locations and propose them as actions
        CatanTile[][] board = gs.getBoard();
        if (turnStep == 0) {
            for (int x = 0; x < board.length; x++) {
                for (int y = 0; y < board[x].length; y++) {
                    CatanTile tile = board[x][y];
                    for (int i = 0; i < 6; i++) {
                        Settlement settlement = tile.getSettlements()[i];

                        // where it is legal to place tile then it can be placed from there
                        if (settlement.getOwner() == -1 &&
                                !(tile.getType().equals(CatanParameters.TileType.SEA) || tile.getType().equals(CatanParameters.TileType.DESERT))) {
//                            actions.add(new BuildSettlement_v2(settlement, activePlayer));
//                            actions.add(new BuildSettlement(x, y, i, activePlayer));
                            if (checkSettlementPlacement(settlement, gs, true)){
                                actions.add(new PlaceSettlementWithRoad(new BuildSettlement(x, y, i, activePlayer), new BuildRoad(x, y, i, activePlayer)));

                            }
                        }
                    }
                }
            }
        }

        return actions;
    }

    /**
     * Calculates regular Player actions
     * @return - ArrayList, various action types (unique).
     */
    static List<AbstractAction> getPlayerActions(CatanGameState gs) {
        // todo
        CatanParameters catanParameters = (CatanParameters) gs.getGameParameters();
        int activePlayer = gs.getTurnOrder().getCurrentPlayer(gs);
        ArrayList<AbstractAction> actions = new ArrayList();
        actions.add(new DoNothing());
        actions.addAll(getBuildActions(gs));

        return actions;
    }

    /**
     * Calculates player's actions when robber is active
     * @return - ArrayList, various action types (unique).
     */
    static List<AbstractAction> getRobberActions(CatanGameState gs) {
        // todo
        CatanParameters catanParameters = (CatanParameters) gs.getGameParameters();
        int activePlayer = gs.getTurnOrder().getCurrentPlayer(gs);
        ArrayList<AbstractAction> actions = new ArrayList();

        return actions;
    }

    static boolean checkSettlementPlacement(Settlement settlement, CatanGameState gs, boolean setup){
        // checks if any of the neighbouring settlements are already taken (distance rule)
        // if yes returns false otherwise true
        Graph<Settlement, Road> graph = gs.getGraph();
        List<Road> roads = graph.getEdges(settlement);
        // check first if we have a road next to the settlement owned by the player
        if (!setup) {
            boolean hasRoadNeighbour = false;
            for (Road road : roads) {
                if (road.getOwner() == gs.getCurrentPlayer()) {
                    hasRoadNeighbour = true;
                }
            }
            if (!hasRoadNeighbour) {
                return false;
            }
        }

        List<Settlement> settlements = graph.getNeighbourNodes(settlement);
        for (Settlement settl: settlements){
            if (settl.getOwner() != -1){
                return false;
            }
        }
        return true;
    }

    static boolean checkRoadPlacement(int roadId, CatanTile tile, CatanGameState gs){
        // todo get neighbours of a road
        // rule is : , check if there is our settlement along edge,
        Graph<Settlement, Road> graph = gs.getGraph();
        Road road = tile.getRoads()[roadId];
        // check if road is already taken
        if (road.getOwner() != -1){
            return false;
        }
        // check if there is our settlement along edge
        int playerId = gs.getCurrentPlayer();
        Settlement settl1 = tile.getSettlements()[roadId];
        Settlement settl2 = tile.getSettlements()[(roadId+1)%6];
        if (settl1.getOwner() == playerId || settl2.getOwner() == playerId){
            return true;
        }

        // check if there is a road on a neighbouring edge
        List<Road> roads = graph.getEdges(settl1);
        roads.addAll(graph.getEdges(settl2));
        for (Road rd :roads){
            if (rd.getOwner() == playerId){
                return true;
            }
        }
        return false;
    }

    // ############## Helper functions to keep main functions simpler

    public static List<AbstractAction> getBuildActions(CatanGameState gs){
        // todo check if player has enough resources

        CatanParameters catanParameters = (CatanParameters) gs.getGameParameters();
        int turnStep = ((CatanTurnOrder) gs.getTurnOrder()).turnStep;
        int activePlayer = gs.getTurnOrder().getCurrentPlayer(gs);
        ArrayList<AbstractAction> actions = new ArrayList();

        // find possible roads, settlements and city upgrades and propose them as actions
        CatanTile[][] board = gs.getBoard();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                for (int i = 0; i < CatanConstants.HEX_SIDES; i++) {
                    Settlement settlement = tile.getSettlements()[i];
                    Road road = tile.getRoads()[i];

                    // where it is legal to place tile then it can be placed from there
                    if (settlement.getOwner() == -1 &&
                            !(tile.getType().equals(CatanParameters.TileType.SEA) || tile.getType().equals(CatanParameters.TileType.DESERT))) {
//                            actions.add(new BuildSettlement_v2(settlement, activePlayer));
//                            actions.add(new BuildSettlement(x, y, i, activePlayer));
                        if (checkSettlementPlacement(settlement, gs, false)){
                            actions.add(new BuildSettlement(x, y, i, activePlayer));
                        }
                    }

                    if (road.getOwner() == -1 && !(tile.getType().equals(CatanParameters.TileType.SEA) || tile.getType().equals(CatanParameters.TileType.DESERT))
                            && checkRoadPlacement(i, tile, gs)){
                        actions.add(new BuildRoad(x, y, i, activePlayer));
                    }
                }
            }
        }
    return actions;
    }
}
