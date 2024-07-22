package games.mastermind;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
import core.components.PartialObservableDeck;
import core.components.Token;
import games.GameType;

import java.util.*;

import static games.mastermind.MMMethods.checkGuessAgainstAnswer;
import static games.mastermind.MMMethods.generateRandomShuffledAnswerCode;

public class MMGameState extends AbstractGameState {

    PartialObservableDeck<Token> answerCode;
    GridBoard<Token> guessBoard;
    GridBoard<Token> resultBoard;
    int activeRow;
    int activeCol;

    /**
     * @param gameParameters - game parameters.
     * @param nPlayers
     */
    public MMGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Mastermind;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<>() {{
            add(answerCode);
            add(guessBoard);
            add(resultBoard);
        }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        MMParameters mmp = (MMParameters) getGameParameters();
        MMGameState copy = new MMGameState(gameParameters, playerId);
        copy.guessBoard = guessBoard.copy();
        copy.resultBoard = resultBoard.copy();
        copy.activeRow = activeRow;
        copy.activeCol = activeCol;

        copy.answerCode = answerCode.copy();
        if (playerId != -1) {
            PartialObservableDeck<Token> shuffledAnswerCode = generateRandomShuffledAnswerCode(mmp.boardWidth, redeterminisationRnd);
            while (!satisfiesPreviousHints(shuffledAnswerCode)) {
                shuffledAnswerCode = generateRandomShuffledAnswerCode(mmp.boardWidth, redeterminisationRnd);
            }
            copy.answerCode = shuffledAnswerCode;
        }
        return copy;
    }

    private boolean satisfiesPreviousHints(PartialObservableDeck<Token> answerCode) {
        MMParameters mmp = (MMParameters) getGameParameters();
        for (int i=0; i<activeRow-1; i++) {
            List<Integer> result = checkGuessAgainstAnswer(this, answerCode, i);
            for (int j=0; j<mmp.boardWidth; j++) {
                if (!MMConstants.resultColours.get(result.get(j)).equals(resultBoard.getElement(j,i))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return getGameScore(playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        // Mapping of tokens to scores (Double)
        Map<Token, Double> tokenToScoreMap = new HashMap<>();
        tokenToScoreMap.put(MMConstants.resultColours.get(0), 2.0);
        tokenToScoreMap.put(MMConstants.resultColours.get(1), 1.0);
        tokenToScoreMap.put(MMConstants.resultColours.get(2), 0.0);

        /*
        double score = 1.0;
        for (int i=0; i<mmp.boardWidth; i++) {
            score *= tokenToScoreMap.getOrDefault(resultBoard.getElement(i,activeRow-1),1.0);
        }
        return score - activeRow * 3;
         */

        List<Token> resultsToken = resultBoard.getComponents();

        double score = 0.0;
        for (Token token : resultsToken) {
            score += tokenToScoreMap.getOrDefault(token,0.0);
        }

        return score - activeRow * 3;

    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MMGameState that)) return false;
        return Objects.equals(answerCode, that.answerCode)
                && Objects.equals(guessBoard,that.guessBoard)
                && Objects.equals(resultBoard,that.resultBoard)
                && Objects.equals(activeRow,that.activeRow)
                && Objects.equals(activeCol,that.activeCol);
    }


    @Override
    public int hashCode() {
        return Objects.hash(answerCode, guessBoard, resultBoard, activeRow, activeCol) + 31 * super.hashCode();
    }

    public GridBoard<Token> getGuessBoard() {
        return guessBoard;
    }

    public GridBoard<Token> getResultBoard() {
        return resultBoard;
    }

    public PartialObservableDeck<Token> getAnswerCode() {
        return answerCode;
    }
}

