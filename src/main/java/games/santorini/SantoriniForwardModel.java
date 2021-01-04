package games.santorini;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.GridBoard;
import games.santorini.actions.MoveAndBuild;
import games.santorini.components.PlayerPosition;
import games.santorini.components.SantoriniCell;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SantoriniForwardModel extends AbstractForwardModel {
    @Override
    protected void _setup(AbstractGameState firstState) {
        SantoriniGameState sgs = (SantoriniGameState) firstState;
        SantoriniParameters sp = (SantoriniParameters) firstState.getGameParameters();

        Random r = new Random(sgs.getGameParameters().getRandomSeed());


        sgs.grid = new GridBoard<>(sp.gridWidth, sp.gridHeight, SantoriniCell.class);

        for(int row = 0; row < sp.gridHeight; row++){
            for(int col = 0; col < sp.gridWidth; col++){
                SantoriniCell c = new SantoriniCell();
                sgs.grid.setElement(row, col, c);
            }
        }

        // Randomly initialization of players positions
        for(int playerID = 0; playerID<sgs.getNPlayers(); playerID++) {
            boolean empty = false;
            while (!empty) {
                int initialPlayerCol = (int) Math.floor(r.nextDouble() * sp.gridWidth);
                int initialPlayerRow = (int) Math.floor(r.nextDouble() * sp.gridHeight);

                SantoriniCell sc = sgs.grid.getElement(initialPlayerRow, initialPlayerCol);
                if (!sc.isPlayer()) {
                    empty = true;
                    sc.setPlayerIn(playerID);

                    sgs.playerPositions.add(new PlayerPosition(initialPlayerRow, initialPlayerCol));
                }
            }
        }
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        SantoriniGameState sgs = (SantoriniGameState) currentState;
        currentState.getTurnOrder().endPlayerTurn(currentState);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        MoveAndBuild a1 = new MoveAndBuild(1,1, 1,1, 1, 2);
        MoveAndBuild a2 = new MoveAndBuild(1,1, 2, 1, 1,3);
        actions.add(a1);
        actions.add(a2);
        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return this;
    }
}
