package games.battleship;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.forwardModels.SequentialActionForwardModel;
import scala.Int;

import java.util.ArrayList;
import java.util.List;

public class BattleshipForwardModel extends SequentialActionForwardModel {

    public Ship[] playerOneShips;
    public Ship[] playerTwoShips;
    @Override
    protected void _setup(AbstractGameState firstState) {
        BattleshipGameParameters bgp = (BattleshipGameParameters) firstState.getGameParameters();
        int gridSize = bgp.gridSize;
        int nShips = bgp.nShips;
        ArrayList<Integer> shipSizes = bgp.shipSizes;

        if (nShips != shipSizes.length)
        {
            if (nShips < shipSizes.length)
            {
                int[] newShipSizes = new int[nShips];
                for (int i = 0; i < nShips - 1; i++)
                {
                    newShipSizes[i] = shipSizes[i];
                }
                newShipSizes[nShips - 1] = shipSizes[shipSizes.length - 1];
                shipSizes = newShipSizes;
            }
            if (nShips > shipSizes.length)
            {
                for (int i = shipSizes.length; i < nShips; i++)
                {
                    shipSizes.add(1);
                }
        }


        BattleshipGameState state = (BattleshipGameState) firstState;
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        return null;
    }
}
