package games.battlelore;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.GridBoard;
import core.components.Token;

import games.battlelore.components.Unit;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static core.CoreConstants.VERBOSE;

public class BattleloreForwardModel extends AbstractForwardModel
{
    @Override
    protected void _setup(AbstractGameState initialState)
    {
        BattleloreGameParameters gameParams = (BattleloreGameParameters) initialState.getGameParameters();
        int hexHeight = gameParams.hexHeight;
        int hexWidth = gameParams.hexWidth;
        BattleloreGameState gameState = (BattleloreGameState)initialState;
        gameState.hexBoard = new GridBoard<Unit>(hexWidth, hexHeight, new Unit());
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState)
    {
        BattleloreGameState state = (BattleloreGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = gameState.getTurnOrder().getCurrentPlayer(gameState);//TODO_Ertugrul might be used later

        for (int x = 0; x < state.hexBoard.getWidth(); x++)
        {
            for (int y = 0; y < state.hexBoard.getHeight(); y++)
            {
                actions.add(new SetGridValueAction(state.hexBoard.getComponentID(), x, y, BattleloreConstants.itemList.get(0)));
            }
        }
        return actions;
    }

    @Override
    protected AbstractForwardModel _copy()
    {
        return new BattleloreForwardModel();
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action)
    {
        action.execute(currentState);
        BattleloreGameParameters gameParams = (BattleloreGameParameters) currentState.getGameParameters();
        int gridSize = gameParams.hexHeight * gameParams.hexWidth;

        /*if (currentState.getTurnOrder().getRoundCounter() == (gridSize * gridSize)) {
            currentState.setGameStatus(Utils.GameResult.GAME_END);
            return;
        }

        if (checkGameEnd((BattleloreGameState) currentState)) {
            return;
        }*/

        currentState.getTurnOrder().endPlayerTurn(currentState);
    }


    /**
     * Checks if the game ended.
     * @param gameState - game state to check game end.
     */
    private boolean checkGameEnd(BattleloreGameState gameState)
    {
        //TODO_Ertugrul
        return false;
    }

    @Override
    protected void endGame(AbstractGameState gameState)
    {
        if (VERBOSE)
        {
            System.out.println(Arrays.toString(gameState.getPlayerResults()));
        }
    }

    /**
     * Inform the game this player has won.
     * @param winnerSymbol - which player won.
     */
    private void registerWinner(BattleloreGameState gameState, Token winnerSymbol)
    {
        gameState.setGameStatus(Utils.GameResult.GAME_END);
        //int winningPlayer = BattleloreConstants //.playerMapping.indexOf(winnerSymbol);
        //gameState.setPlayerResult(Utils.GameResult.WIN, winningPlayer);
        //gameState.setPlayerResult(Utils.GameResult.LOSE, 1-winningPlayer);
    }
}
