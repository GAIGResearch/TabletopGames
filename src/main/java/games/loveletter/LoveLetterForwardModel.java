package games.loveletter;

import core.AbstractGameState;
import core.gamephase.DefaultGamePhase;
import core.ForwardModel;
import core.actions.IAction;
import utilities.Utils;

import java.util.Stack;

import static games.loveletter.LoveLetterGameState.LoveLetterGamePhase.Draw;

public class LoveLetterForwardModel extends ForwardModel {

    private Stack<IAction> actionStack = new Stack<>();

    @Override
    public void setup(AbstractGameState firstState) {

    }

    @Override
    public void next(AbstractGameState gameState, IAction action) {
        System.out.println(action.toString());
        LoveLetterTurnOrder llTurnOrder = (LoveLetterTurnOrder) gameState.getTurnOrder();
        LoveLetterGameState llgs = (LoveLetterGameState) gameState;
        action.execute(gameState);
        if (llgs.getGamePhase() == Draw)
            llgs.setGamePhase(DefaultGamePhase.Main);
        else{
            llgs.setGamePhase(Draw);
            checkWinningCondition(llgs);
            if (llgs.getGameStatus() != Utils.GameResult.GAME_END)
                llgs.getTurnOrder().endPlayerTurn(gameState);
        }
    }

    private void checkWinningCondition(LoveLetterGameState llgs) {
        int playersAlive = 0;
        for (Utils.GameResult result : llgs.getPlayerResults())
            if (result != Utils.GameResult.GAME_LOSE)
                playersAlive += 1;

        if (playersAlive == 1) {
            // game ends because only a single player is left
            llgs.endGame();
        }
        else if (llgs.getRemainingCards() == 0){
            // game needs to end because their are no cards left
            llgs.endGame();
        }
    }
}
