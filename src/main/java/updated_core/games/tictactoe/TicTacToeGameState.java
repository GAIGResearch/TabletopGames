package updated_core.games.tictactoe;

import actions.Action;
import updated_core.actions.IAction;
import updated_core.actions.SetGridValueAction;
import updated_core.components.Grid;
import updated_core.gamestates.GameState;
import updated_core.observations.Observation;
import updated_core.players.AbstractPlayer;

import java.awt.*;
import java.util.*;
import java.util.List;


public class TicTacToeGameState extends GameState {

    Grid<Character> grid = new Grid<>(3, 3, ' ');
    ArrayList<Point> openPositions = new ArrayList<>();
    //HashMap<AbstractPlayer, Character> playerSymbols = new HashMap<>();

    public TicTacToeGameState(TicTacToeGameParameters gameParameters, TicTacToeForwardModel fm){
        super(gameParameters, fm);
        /*
        for (int x = 0; x < grid.getWidth(); x++){

            for (int y = 0; y < grid.getHeight(); y++){
                openPositions.add(new Point(x, y));
            }
        }
        */
    }

    @Override
    public Observation getObservation(AbstractPlayer player) {
        return null;
    }

    @Override
    public List<IAction> getActions(AbstractPlayer player) {
        ArrayList<IAction> actions = new ArrayList<>();

        for (int x = 0; x < grid.getWidth(); x++){
            for (int y = 0; y < grid.getHeight(); y++) {
                if (grid.getElement(x, y) == ' ')
                    actions.add(new SetGridValueAction<Character>(grid, x, y, Integer.toString(player.playerID).charAt(0)));
            }
        }
        return actions;
    }

    @Override
    public void getWinner() {

    }

    @Override
    public int getNPlayers() {
        return 0;
    }

    @Override
    public int getActivePlayer() {
        return 0;
    }
}
