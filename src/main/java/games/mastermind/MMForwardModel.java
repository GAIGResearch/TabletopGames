package games.mastermind;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.BoardNode;
import core.components.GridBoard;

import java.util.*;

public class MMForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        MMGameState mmgs = (MMGameState) firstState;
        MMParameters mmp = (MMParameters) mmgs.getGameParameters();

        mmgs.guessBoard = new GridBoard(mmp.boardWidth, mmp.boardHeight, new BoardNode(MMConstants.emptyPeg));
        mmgs.resultBoard = new GridBoard(mmp.boardWidth, mmp.boardHeight, new BoardNode(MMConstants.emptyPeg));
        mmgs.activeRow = 0;
        mmgs.activeCol = 0;
        mmgs.answerCode = mmgs.generateRandomShuffledAnswerCode(mmp.boardWidth, mmgs.getRnd());
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        MMGameState mmgs = (MMGameState) gameState;
        ArrayList<AbstractAction> availableActions = new ArrayList<>();
        for (BoardNode colour : MMConstants.guessColours) {
            availableActions.add(new SetGridValueAction(mmgs.guessBoard.getComponentID(), mmgs.activeCol, mmgs.activeRow, colour.getComponentID()));
        }
        return availableActions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        MMGameState mmgs = (MMGameState) currentState;
        MMParameters mmp = (MMParameters) mmgs.getGameParameters();

        mmgs.activeCol += 1; // Default afterAction is to move into the next column

        boolean[] winAndGameEndResult = checkAndProcessGameEnd(mmgs);
        boolean win = winAndGameEndResult[0];
        boolean gameEnd = winAndGameEndResult[1];

        if (gameEnd) {
            mmgs.setGameStatus(CoreConstants.GameResult.GAME_END);
            if (win) {
                mmgs.setPlayerResult(CoreConstants.GameResult.WIN_GAME, 0);
                // Fill the resultBoard's last row with 'correct'
                for (int i=0; i<mmgs.guessBoard.getWidth(); i++) {
                    mmgs.resultBoard.setElement(i, mmgs.activeRow, MMConstants.resultColours.get(0));
                }
            } else {
                mmgs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, 0);
            }
            return;
        }

        if (mmgs.activeCol == mmp.boardWidth) { // If we have reached the end of the row
            // Check if the current guess is correct
            // 0 = colour in correct position
            // 1 = colour in incorrect position
            // 2 = colour not in code
            List<Integer> rowResult = mmgs.checkGuessAgainstAnswer(mmgs.answerCode, mmgs.activeRow);

            // Update result board with respectively coloured pegs
            for (int i=0; i<rowResult.size(); i++) {
                mmgs.resultBoard.setElement(i, mmgs.activeRow, MMConstants.resultColours.get(rowResult.get(i)));
            }

            // Reset row and column markers, ready for the next row
            mmgs.activeCol = 0;
            mmgs.activeRow += 1;
        }
    }

    private boolean[] checkAndProcessGameEnd(MMGameState gameState) {
        MMParameters mmp = (MMParameters) gameState.getGameParameters();

        boolean win = true;
        boolean gameEnd = true;

        for (int i=0; i<mmp.boardWidth; i++) {
            if (!gameState.guessBoard.getElement(i,gameState.activeRow).getComponentName().equals(gameState.answerCode.get(i).getComponentName())) {
                win = false;
                gameEnd = false;
                break;
            }
        }
        if (gameState.activeRow == mmp.boardHeight-1 && gameState.activeCol == mmp.boardWidth){
            win = false;
            gameEnd = true;
        }

        return new boolean[]{win, gameEnd};
    }

    @Override
    protected void endGame(AbstractGameState gs) {
        // lol nothing needed
    }

}
