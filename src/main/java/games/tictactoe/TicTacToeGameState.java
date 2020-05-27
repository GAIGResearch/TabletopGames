package games.tictactoe;

import core.components.GridBoard;
import core.AbstractGameState;
import core.gamestates.GridGameState;
import core.observations.GridObservation;
import core.observations.IObservation;
import core.turnorder.AlternatingTurnOrder;


public class TicTacToeGameState extends AbstractGameState implements GridGameState<Character> {

    GridBoard<Character> gridBoard;

    public TicTacToeGameState(TicTacToeGameParameters gameParameters, int nPlayers){
        super(gameParameters, new AlternatingTurnOrder(nPlayers));
    }

    @Override
    public IObservation getObservation(int player) {
        return new GridObservation<>(gridBoard.getGridValues());
    }

    @Override
    public void addAllComponents() {
        allComponents.putComponent(gridBoard);
    }

    @Override
    public GridBoard<Character> getGridBoard() {
        return gridBoard;
    }
}
