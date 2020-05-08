package games.tictactoe;

import core.ForwardModel;
import core.actions.IAction;
import core.actions.SetGridValueAction;
import core.components.Grid;
import core.AbstractGameState;
import core.gamestates.GridGameState;
import core.observations.GridObservation;
import core.observations.IObservation;
import core.turnorder.AlternatingTurnOrder;
import utilities.Utils;

import java.util.*;
import java.util.List;


public class TicTacToeGameState extends AbstractGameState implements GridGameState<Character> {

    private Grid<Character> grid = new Grid<>(3, 3, ' ');

    //HashMap<AbstractPlayer, Character> playerSymbols = new HashMap<>();

    public TicTacToeGameState(TicTacToeGameParameters gameParameters, ForwardModel model, int nPlayers){
        super(gameParameters, model, nPlayers, new AlternatingTurnOrder(nPlayers));
    }

    @Override
    public IObservation getObservation(int player) {
        return new GridObservation<>(grid.getGridValues());
    }

    @Override
    public void endGame() {
        gameStatus = Utils.GameResult.GAME_DRAW;
        Arrays.fill(playerResults, Utils.GameResult.GAME_DRAW);
    }

    @Override
    public List<IAction> computeAvailableActions() {
        ArrayList<IAction> actions = new ArrayList<>();
        int player = turnOrder.getCurrentPlayer(this);

        for (int x = 0; x < grid.getWidth(); x++){
            for (int y = 0; y < grid.getHeight(); y++) {
                if (grid.getElement(x, y) == ' ')
                    actions.add(new SetGridValueAction<>(grid, x, y, player == 0 ? 'x' : 'o'));
            }
        }
        return actions;
    }

    @Override
    public void setComponents() {

    }

    @Override
    public Grid<Character> getGrid() {
        return grid;
    }

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
