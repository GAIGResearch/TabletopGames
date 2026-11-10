package games.connect4;

import core.AbstractGameState;
import core.AbstractGameStateContainer;
import core.components.GridBoard;
import utilities.Pair;

import java.util.LinkedList;

public class Connect4GameStateContainer extends AbstractGameStateContainer {
    GridBoard gridBoard;
    LinkedList<Pair<Integer, Integer>> winnerCells;
    public Connect4GameStateContainer(Connect4GameState gs) {
        super(gs);
        gridBoard = gs.gridBoard;
        winnerCells = gs.winnerCells;
    }
}
