package games.monopoly;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static games.monopoly.MonopolyTestUtils.*;
import static org.junit.Assert.*;

public class MonopolySetupTest {

    @Test
    public void theUKBoardHasFortySquaresWithTheRulebookPropertyCounts() {
        MonopolyBoard board = newState(4, 7, null).getBoard();
        assertEquals(40, board.nSquares());
        assertEquals(22, board.squares(SquareType.STREET).size());
        assertEquals(8, board.groups().size());
        // the two end groups have 2 streets, the other six have 3
        for (MonopolyGroup g : board.groups()) {
            int expected = g.name().equals("Brown") || g.name().equals("Dark Blue") ? 2 : 3;
            assertEquals(g.name(), expected, board.streets(g).size());
        }
        assertEquals(4, board.squares(SquareType.STATION).size());
        assertEquals(2, board.squares(SquareType.UTILITY).size());
        assertEquals(10, board.jail().index());
        assertEquals("Old Kent Road", board.square(1).name());
        assertEquals("King's Cross Station", board.square(5).name());
        assertEquals("Mayfair", board.square(39).name());
        assertEquals(200, board.goSalary());
    }

    @Test
    public void everyPlayerStartsOnGoWithStartingCashAndTheBankOwnsEverything() {
        MonopolyGameState state = newState(4, 7, null);
        for (int p = 0; p < 4; p++) {
            assertEquals(1500, state.getCash(p));
            assertEquals(0, state.getPosition(p).index());
            assertFalse(state.isInJail(p));
            assertEquals(0, state.getJailRolls(p));
            assertFalse(state.isBankrupt(p));
            assertEquals(0, state.getJailCards(p).getSize());
        }
        for (MonopolySquare s : state.getBoard().squares()) {
            assertEquals(s.name(), -1, state.getOwner(s));
            assertEquals(0, state.getBuildings(s));
            assertFalse(state.isMortgaged(s));
        }
        assertEquals(16, state.getChanceDeck().getSize());
        assertEquals(16, state.getCommunityChestDeck().getSize());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
        assertEquals(0, state.getNDoubles());
        assertFalse(state.hasAnotherRoll());
    }

    @Test
    public void startingCashComesFromTheParameter() {
        MonopolyParameters params = new MonopolyParameters();
        params.setParameterValue("startingCash", 2000);
        MonopolyGameState state = newState(3, 7, params);
        for (int p = 0; p < 3; p++)
            assertEquals(2000, state.getCash(p));
    }

    @Test
    public void theFirstPlayerIsRandom() {
        Set<Integer> first = new HashSet<>();
        for (long seed = 0; seed < 40; seed++)
            first.add(newState(4, seed, null).getCurrentPlayer());
        assertEquals(Set.of(0, 1, 2, 3), first);
    }
}
