package games.mastermind;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.*;
import games.GameType;

import java.util.*;

public class MMGameState extends AbstractGameState {

    PartialObservableDeck<BoardNode> answerCode;
    GridBoard guessBoard;
    GridBoard resultBoard;
    int activeRow;
    int activeCol;

    /**
     * @param gameParameters - game parameters.
     * @param nPlayers - number of players
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
            addAll(MMConstants.resultColours);
            addAll(MMConstants.guessColours);
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
            PartialObservableDeck<BoardNode> shuffledAnswerCode = generateRandomShuffledAnswerCode(mmp.boardWidth, redeterminisationRnd);
            while (!satisfiesPreviousHints(shuffledAnswerCode)) {
                shuffledAnswerCode = generateRandomShuffledAnswerCode(mmp.boardWidth, redeterminisationRnd);
            }
            copy.answerCode = shuffledAnswerCode;
        }
        return copy;
    }

    private boolean satisfiesPreviousHints(PartialObservableDeck<BoardNode> answerCode) {
        MMParameters mmp = (MMParameters) getGameParameters();
        for (int i = 0; i < activeRow - 1; i++) {
            List<Integer> result = checkGuessAgainstAnswer(answerCode, i);
            for (int j = 0; j < mmp.boardWidth; j++) {
                if (!MMConstants.resultColours.get(result.get(j)).getComponentName().equals(resultBoard.getElement(j, i).getComponentName())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        // Mapping of BoardNodes to scores (Double)
        Map<BoardNode, Double> BoardNodeToScoreMap = new HashMap<>();
        BoardNodeToScoreMap.put(MMConstants.resultColours.get(0), 2.0);
        BoardNodeToScoreMap.put(MMConstants.resultColours.get(1), 1.0);
        BoardNodeToScoreMap.put(MMConstants.resultColours.get(2), 0.0);

        List<BoardNode> resultsBoardNode = resultBoard.getComponents();

        double score = 0.0;
        for (BoardNode BoardNode : resultsBoardNode) {
            score += BoardNodeToScoreMap.getOrDefault(BoardNode, 0.0);
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
                && Objects.equals(guessBoard, that.guessBoard)
                && Objects.equals(resultBoard, that.resultBoard)
                && Objects.equals(activeRow, that.activeRow)
                && Objects.equals(activeCol, that.activeCol);
    }


    @Override
    public int hashCode() {
        return Objects.hash(answerCode, guessBoard, resultBoard, activeRow, activeCol) + 31 * super.hashCode();
    }

    public GridBoard getGuessBoard() {
        return guessBoard;
    }

    public GridBoard getResultBoard() {
        return resultBoard;
    }

    public PartialObservableDeck<BoardNode> getAnswerCode() {
        return answerCode;
    }

    // Method that checks if the guess in the activeRow matches the answerCode
    // Returns a list of integers which represent how many black/white pegs should be returned
    // The integer returned corresponds to the index of the desired result colour in MMConstants.resultColours
    public List<Integer> checkGuessAgainstAnswer(PartialObservableDeck<BoardNode> answerCode, int activeRow) {
        MMParameters mmp = (MMParameters) getGameParameters();

        List<Integer> result = new ArrayList<>(Collections.nCopies(mmp.boardWidth, -1));
        PartialObservableDeck<BoardNode> copyAnswerCode = answerCode.copy();

        for (int i = mmp.boardWidth - 1; i >= 0; i--) {
            // If a guess peg is the correct colour in the correct place, add a 0 to the returned list
            if (guessBoard.getElement(i, activeRow).getComponentName().equals(answerCode.get(i).getComponentName())) {
                result.set(i, 0);
                copyAnswerCode.remove(i);
            }
        }
        // If a guess wasn't in the correct position, then either the colour is in the wrong position, or the guess is not in the code at all
        for (int i = 0; i < mmp.boardWidth; i++) {
            if (result.get(i) != 0) {
                // If the guess is in the answer, but in the wrong position, add a 1 to the returned list
                boolean found = false;
                BoardNode toRemove = null;
                for (BoardNode BoardNode : copyAnswerCode) {
                    if (guessBoard.getElement(i, activeRow).getComponentName().equals(BoardNode.getComponentName())) {
                        result.set(i, 1);
                        toRemove = BoardNode;
                        found = true;
                    }
                }
                if (found) {
                    result.set(i, 1);
                    copyAnswerCode.remove(toRemove);
                } else {
                    // If the guess is not in the answer, add a 2 to the returned list
                    result.set(i, 2);
                }
            }
        }

        Collections.sort(result);
        return result;
    }

    // Method returns a random code (used with rnd to create the answerCode, and used with redeterminationRnd to create copies of the answerCode)
    public PartialObservableDeck<BoardNode> generateRandomShuffledAnswerCode(int width, Random rnd) {

        PartialObservableDeck<BoardNode> shuffledAnswerCode = new PartialObservableDeck<>("Shuffled Answer Code", 0, new boolean[]{false});
        for (int i = 0; i < width; i++) {
            shuffledAnswerCode.add(MMConstants.guessColours.get(rnd.nextInt(MMConstants.guessColours.size())));
        }
        return shuffledAnswerCode;
    }
}

