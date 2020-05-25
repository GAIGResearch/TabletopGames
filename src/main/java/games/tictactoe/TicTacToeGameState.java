package games.tictactoe;

import core.ForwardModel;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.GridBoard;
import core.AbstractGameState;
import core.gamestates.GridGameState;
import core.observations.GridObservation;
import core.observations.IObservation;
import core.turnorder.AlternatingTurnOrder;
import utilities.Utils;

import java.util.*;
import java.util.List;


public class TicTacToeGameState extends AbstractGameState implements GridGameState<Character> {

    GridBoard<Character> gridBoard;

    public TicTacToeGameState(TicTacToeGameParameters gameParameters, ForwardModel model, int nPlayers){
        super(gameParameters, model, new AlternatingTurnOrder(nPlayers));
    }

    @Override
    public IObservation getObservation(int player) {
        return new GridObservation<>(gridBoard.getGridValues());
    }

    @Override
    public void endGame() {
        gameStatus = Utils.GameResult.GAME_DRAW;
        Arrays.fill(playerResults, Utils.GameResult.GAME_DRAW);
    }

    @Override
    public List<AbstractAction> computeAvailableActions() {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = turnOrder.getCurrentPlayer(this);

        for (int x = 0; x < gridBoard.getWidth(); x++){
            for (int y = 0; y < gridBoard.getHeight(); y++) {
                if (gridBoard.getElement(x, y) == ' ')
                    actions.add(new SetGridValueAction(gridBoard.getComponentID(), x, y, player == 0 ? 'x' : 'o'));
            }
        }
        return actions;
    }

    @Override
    public void addAllComponents() {
        allComponents.putComponent(gridBoard);
    }

    @Override
    public GridBoard<Character> getGridBoard() {
        return gridBoard;
    }

    /**
     * Inform the game this player has won.
     * @param winnerSymbol - which player won.
     */
    public void registerWinner(char winnerSymbol){
        gameStatus = Utils.GameResult.GAME_END;
        if (winnerSymbol == 'o'){
            playerResults[1] = Utils.GameResult.GAME_WIN;
            playerResults[0] = Utils.GameResult.GAME_LOSE;
        } else {
            playerResults[0] = Utils.GameResult.GAME_WIN;
            playerResults[1] = Utils.GameResult.GAME_LOSE;
        }
    }
}
