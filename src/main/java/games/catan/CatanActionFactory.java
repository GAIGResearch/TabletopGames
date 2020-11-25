package games.catan;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.catan.actions.BuildRoad;
import games.catan.actions.BuildSettlement;
import games.catan.components.Road;
import games.catan.components.Settlement;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static games.catan.CatanConstants.HEX_SIDES;

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

        // todo find possible settlement locations
        CatanTile[][] board = gs.getBoard();
        if (turnStep == 0) {
            ArrayList<Settlement> settlements = new ArrayList<>();
            for (int x = 0; x < board.length; x++) {
                for (int y = 0; y < board[x].length; y++) {
                    CatanTile tile = board[x][y];
                    for (int i = 0; i < 6; i++) {
                        Settlement settlement = tile.getSettlements()[i];
                        if (settlement.getOwner() == -1) {
                            actions.add(new BuildSettlement(x, y, i, activePlayer));
                        }
                    }
                }
            }
        }

        // find previous settlement and provide roads around the vertex
        else if (turnStep == 1){
            ArrayList<Settlement> settlements = new ArrayList<>();
            for (int x = 0; x < board.length; x++) {
                for (int y = 0; y < board[x].length; y++) {
                    CatanTile tile = board[x][y];
                    Settlement[] settlement = tile.getSettlements();
                    for (int i = 0; i < 6; i++) {
                        // Road has already been set
                        if (settlement[i].getOwner() == activePlayer) {
                            System.out.println("Current Player's settlement found");
                            // todo check if road is valid
                            actions.add(new BuildRoad(x, y, i, activePlayer));
                            actions.add(new BuildRoad(x, y, (i+5)%HEX_SIDES, activePlayer));
                            // todo last one is along a neighbour that needs to be retrieved

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
}
