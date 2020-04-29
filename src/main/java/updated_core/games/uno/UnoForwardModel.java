package updated_core.games.uno;

import updated_core.ForwardModel;
import updated_core.actions.IAction;
import updated_core.games.tictactoe.TicTacToeGameState;
import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.TurnOrder;

public class UnoForwardModel extends ForwardModel {

    @Override
    public void setup(AbstractGameState firstState) {

    }

    @Override
    public void next(AbstractGameState currentState, TurnOrder turnOrder, IAction action) {
        action.Execute(currentState, turnOrder);
        currentState.increaseTurnCounter();
        if (currentState.getTurnCounter() == 9)
            currentState.endGame();

        checkWinCondition((UnoGameState) currentState);
    }

    public void checkWinCondition(UnoGameState gameState) {
        for (int i = 0; i < gameState.getNPlayers(); i++)
        {
            if (gameState.playerDecks.get(i).getCards().size() == 0)
                gameState.registerWinner(i);
        }
    }


}
