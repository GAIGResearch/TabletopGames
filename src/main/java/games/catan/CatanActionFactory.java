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
                            if (checkDistanceRule(settlement, gs)){
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
    static List<AbstractAction> getPlayerActions(CatanGameState pgs) {
        // todo
        CatanParameters catanParameters = (CatanParameters) pgs.getGameParameters();
        int activePlayer = pgs.getTurnOrder().getCurrentPlayer(pgs);
        ArrayList<AbstractAction> actions = new ArrayList();
        actions.add(new DoNothing());

        return actions;
    }

    /**
     * Calculates player's actions when robber is active
     * @return - ArrayList, various action types (unique).
     */
    static List<AbstractAction> getRobberActions(CatanGameState pgs) {
        // todo
        CatanParameters catanParameters = (CatanParameters) pgs.getGameParameters();
        int activePlayer = pgs.getTurnOrder().getCurrentPlayer(pgs);
        ArrayList<AbstractAction> actions = new ArrayList();

        return actions;
    }

    static boolean checkDistanceRule(Settlement settlement, CatanGameState gs){
        // checks if any of the neighbouring settlements are already taken (distance rule)
        // if yes returns false otherwise true
        Graph<Settlement, Road> graph = gs.getGraph();
        List<Settlement> settlements = graph.getNeighbourNodes(settlement);
        for (Settlement settl: settlements){
            if (settl.getOwner() != -1){
                return false;
            }
        }
        return true;
    }
}
