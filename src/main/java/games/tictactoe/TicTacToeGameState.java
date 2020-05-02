package games.tictactoe;

import core.actions.IAction;
import core.actions.SetGridValueAction;
import core.components.Grid;
import core.AbstractGameState;
import core.gamestates.GridGameState;
import core.observations.GridObservation;
import core.observations.IObservation;
import utilities.Utils;

import java.util.*;
import java.util.List;


public class TicTacToeGameState extends AbstractGameState implements GridGameState<Character> {

    private Grid<Character> grid = new Grid<>(3, 3, ' ');

    //HashMap<AbstractPlayer, Character> playerSymbols = new HashMap<>();

    public TicTacToeGameState(TicTacToeGameParameters gameParameters, int nPlayers){
        super(gameParameters, nPlayers);
    }

    @Override
    public IObservation getObservation(int player) {
        return new GridObservation<>(grid.getGridValues());
    }

    @Override
    public void endGame() {
        terminalState = true;
        Arrays.fill(playerResults, Utils.GameResult.GAME_DRAW);
    }

    @Override
    public List<IAction> computeAvailableActions(int player) {
        ArrayList<IAction> actions = new ArrayList<>();

        for (int x = 0; x < grid.getWidth(); x++){
            for (int y = 0; y < grid.getHeight(); y++) {
                if (grid.getElement(x, y) == ' ')
                    actions.add(new SetGridValueAction<>(grid, x, y, player == 0 ? 'x' : 'o'));
            }
        }
        return actions;
    }

    @Override
    public Grid<Character> getGrid() {
        return grid;
    }

    public void registerWinner(char winnerSymbol){
        terminalState = true;
        if (winnerSymbol == 'o'){
            playerResults[1] = Utils.GameResult.GAME_WIN;
            playerResults[0] = Utils.GameResult.GAME_LOSE;
        } else {
            playerResults[0] = Utils.GameResult.GAME_WIN;
            playerResults[1] = Utils.GameResult.GAME_LOSE;
        }
    }
}
