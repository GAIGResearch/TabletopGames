package games.mastermind;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.BoardNode;
import core.components.PartialObservableDeck;
import core.components.Token;
import games.GameType;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.AssertJUnit.assertTrue;

public class TestMastermind {
    Game mastermind;
    MMForwardModel forwardModel = new MMForwardModel();
    List<AbstractPlayer> players;
    AbstractParameters gameParameters;

    @Before
    public void setUp() {
        players = List.of(
                new RandomPlayer()
        );
        gameParameters = new MMParameters();
        mastermind = GameType.Mastermind.createGameInstance(1,40, gameParameters);
        mastermind.reset(players);
    }

    @Test
    public void testResultGeneratedAfterGuessingEntireRow() {
        MMParameters mmp = (MMParameters) gameParameters;
        MMGameState mmgs = (MMGameState) mastermind.getGameState();

        for (int i=0 ; i<mmp.boardWidth ; i++) {
            List<AbstractAction> actions = forwardModel.computeAvailableActions(mmgs);
            forwardModel.next(mmgs, actions.get(0));
            if (i != mmp.boardWidth-1) {
                assertEquals(mmgs.resultBoard.getElement(0,0).getComponentName(), MMConstants.emptyPeg);
                assertEquals(mmgs.resultBoard.getElement(1,0).getComponentName(), MMConstants.emptyPeg);
                assertEquals(mmgs.resultBoard.getElement(2,0).getComponentName(), MMConstants.emptyPeg);
                assertEquals(mmgs.resultBoard.getElement(3,0).getComponentName(), MMConstants.emptyPeg);
            } else {
                assertNotEquals(mmgs.resultBoard.getElement(0,0).getComponentName(), MMConstants.emptyPeg);
                assertNotEquals(mmgs.resultBoard.getElement(1,0).getComponentName(), MMConstants.emptyPeg);
                assertNotEquals(mmgs.resultBoard.getElement(2,0).getComponentName(), MMConstants.emptyPeg);
                assertNotEquals(mmgs.resultBoard.getElement(3,0).getComponentName(), MMConstants.emptyPeg);
            }
        }
    }

    @Test
    // Assumes boardWidth = 4
    public void testCorrectResult() {
        // For guess and answerString 0-5 correspond to the colours in MMConstants.guessColours
        // For expectedResult 0=correct position, 1=incorrect position, 2=not in code
        assertTrue(_testCorrectResult("0123", "0123", "0000"));
        assertTrue(_testCorrectResult("0123", "1123", "0002"));
        assertTrue(_testCorrectResult("0123", "2103", "0011"));
        assertTrue(_testCorrectResult("0123", "4103", "0012"));
        assertTrue(_testCorrectResult("0123", "0303", "0022"));
        assertTrue(_testCorrectResult("0123", "3021", "0111"));
        assertTrue(_testCorrectResult("0123", "3140", "0112"));
        assertTrue(_testCorrectResult("0123", "3322", "0122"));
        assertTrue(_testCorrectResult("0123", "0000", "0222"));
        assertTrue(_testCorrectResult("0123", "3210", "1111"));
        assertTrue(_testCorrectResult("0123", "1234", "1112"));
        assertTrue(_testCorrectResult("0123", "1000", "1122"));
        assertTrue(_testCorrectResult("0123", "1455", "1222"));
        assertTrue(_testCorrectResult("0123", "4445", "2222"));

        assertTrue(_testCorrectResult("0012", "0304", "0122"));
    }

    private boolean _testCorrectResult(String guess, String answerString, String expectedResult) {
        MMGameState mmgs = (MMGameState) mastermind.getGameState();
        PartialObservableDeck<BoardNode> answerCode = new PartialObservableDeck<>("Answer Code", 0, new boolean[]{false});

        for (int i=0; i<4; i++) {
            forwardModel.next(mmgs, new SetGridValueAction(mmgs.guessBoard.getComponentID(), i, 0, MMConstants.guessColours.get(Character.getNumericValue(guess.charAt(i))).getComponentID()));
            answerCode.add(MMConstants.guessColours.get(Character.getNumericValue(answerString.charAt(3-i))));
        }

        List<Integer> result = mmgs.checkGuessAgainstAnswer(answerCode, 0);
        List<Integer> expectedResultList = expectedResult.chars().map(Character::getNumericValue).boxed().toList();
        return result.equals(expectedResultList);
    }

}