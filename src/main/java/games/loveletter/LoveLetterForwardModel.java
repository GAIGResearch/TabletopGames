package games.loveletter;

import core.AbstractGameState;
import core.ForwardModel;
import core.actions.IAction;
import games.explodingkittens.actions.IsNope;
import games.explodingkittens.actions.IsNopeable;
import utilities.Utils;

import java.util.Stack;

import static games.explodingkittens.ExplodingKittensGameState.GamePhase.NopePhase;
import static games.explodingkittens.ExplodingKittensGameState.GamePhase.PlayerMove;


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
        if (llgs.getGamePhase() == LoveLetterGameState.GamePhase.DrawPhase)
            llgs.setGamePhase(LoveLetterGameState.GamePhase.PlayerMove);
        else{
            llgs.setGamePhase(LoveLetterGameState.GamePhase.DrawPhase);
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
