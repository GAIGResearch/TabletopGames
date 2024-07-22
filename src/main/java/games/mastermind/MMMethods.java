package games.mastermind;

import core.AbstractGameState;
import core.components.GridBoard;
import core.components.PartialObservableDeck;
import core.components.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MMMethods {
    static public List<Integer> checkGuessAgainstAnswer(AbstractGameState gameState, PartialObservableDeck<Token> answerCode, int activeRow) {
        MMGameState mmgs = (MMGameState) gameState;
        MMParameters mmp = (MMParameters) mmgs.getGameParameters();

        List<Integer> result = new ArrayList<>(Collections.nCopies(mmp.boardWidth, -1));
        PartialObservableDeck<Token> copyAnswerCode = answerCode.copy();

        for (int i=mmp.boardWidth-1; i>=0; i--) {

            if (mmgs.guessBoard.getElement(i,activeRow) == answerCode.get(i)) {
                result.set(i, 0);
                copyAnswerCode.remove(i);

            }
        }
        for (int i=0; i<mmp.boardWidth; i++) {
            if (result.get(i) != 0){
                if (copyAnswerCode.contains(mmgs.guessBoard.getElement(i,activeRow))) {
                    result.set(i,1);
                    copyAnswerCode.remove(mmgs.guessBoard.getElement(i,activeRow));
                } else {
                    result.set(i,2);
                }

            }
        }

        Collections.sort(result);
        return result;
    }

    static public PartialObservableDeck<Token> generateRandomShuffledAnswerCode(int width, Random rnd) {

        PartialObservableDeck<Token> shuffledAnswerCode = new PartialObservableDeck<Token>("Shuffled Answer Code", 0, new boolean[]{false});
        for (int i=0; i<width; i++) {
            shuffledAnswerCode.add(MMConstants.guessColours.get(rnd.nextInt(MMConstants.guessColours.size())));
        }
        return shuffledAnswerCode;
    }
}
