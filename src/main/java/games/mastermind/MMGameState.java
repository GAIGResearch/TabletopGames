package games.mastermind;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
import core.components.PartialObservableDeck;
import core.components.Token;
import games.GameType;

import java.util.*;

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
            List<Integer> result = checkGuessAgainstAnswer(answerCode, i);
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
        // Mapping of tokens to scores (Double)
        Map<Token, Double> tokenToScoreMap = new HashMap<>();
        tokenToScoreMap.put(MMConstants.resultColours.get(0), 2.0);
        tokenToScoreMap.put(MMConstants.resultColours.get(1), 1.0);
        tokenToScoreMap.put(MMConstants.resultColours.get(2), 0.0);

        List<Token> resultsToken = resultBoard.getComponents();

        double score = 0.0;
        for (Token token : resultsToken) {
            score += tokenToScoreMap.getOrDefault(token,0.0);
        }

        return score - activeRow * 3;
    }

    @Override
    public double getGameScore(int playerId) {
        return ((MMParameters) getGameParameters()).boardHeight - activeRow;

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

    // Method that checks if the guess in the activeRow matches the answerCode
    // Returns a list of integers which represent how many black/white pegs should be returned
    // The integer returned corresponds to the index of the desired result colour in MMConstants.resultColours
    public List<Integer> checkGuessAgainstAnswer(PartialObservableDeck<Token> answerCode, int activeRow) {
        MMParameters mmp = (MMParameters) getGameParameters();

        List<Integer> result = new ArrayList<>(Collections.nCopies(mmp.boardWidth, -1));
        PartialObservableDeck<Token> copyAnswerCode = answerCode.copy();

        for (int i=mmp.boardWidth-1; i>=0; i--) {
            // If a guess peg is the correct colour in the correct place, add a 0 to the returned list
            if (guessBoard.getElement(i,activeRow) == answerCode.get(i)) {
                result.set(i, 0);
                copyAnswerCode.remove(i);
            }
        }
        // If a guess wasn't in the correct position, then either the colour is in the wrong position, or the guess is not in the code at all
        for (int i=0; i<mmp.boardWidth; i++) {
            if (result.get(i) != 0){
                // If the guess is in the answer, but in the wrong position, add a 1 to the returned list
                if (copyAnswerCode.contains(guessBoard.getElement(i,activeRow))) {
                    result.set(i,1);
                    copyAnswerCode.remove(guessBoard.getElement(i,activeRow));
                } else {
                // If the guess is not in the answer, add a 2 to the returned list
                    result.set(i,2);
                }
            }
        }

        Collections.sort(result);
        return result;
    }

    // Method returns a random code (used with rnd to create the answerCode, and used with redeterminationRnd to create copies of the answerCode)
    public PartialObservableDeck<Token> generateRandomShuffledAnswerCode(int width, Random rnd) {

        PartialObservableDeck<Token> shuffledAnswerCode = new PartialObservableDeck<Token>("Shuffled Answer Code", 0, new boolean[]{false});
        for (int i=0; i<width; i++) {
            shuffledAnswerCode.add(MMConstants.guessColours.get(rnd.nextInt(MMConstants.guessColours.size())));
        }
        return shuffledAnswerCode;
    }
}

