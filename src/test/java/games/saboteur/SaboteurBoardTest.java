package games.saboteur;

import core.actions.AbstractAction;
import core.components.Deck;
import core.components.grid.SquareCoord;
import games.saboteur.actions.PlacePathCard;
import games.saboteur.actions.PlayMapCard;
import games.saboteur.actions.PlayRockFallCard;
import games.saboteur.components.ActionCard;
import games.saboteur.components.PathCard;
import games.saboteur.components.SaboteurCard;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * With the default parameters the board is 14 wide and 12 high, the start is at (2,6), and the goals are at (10,4),
 * (10,6) and (10,8). Path cards are written with their sides in the order E, S, W, N.
 */
public class SaboteurBoardTest {

    static final boolean T = true, F = false;
    static final SquareCoord START = sq(2, 6);
    static final List<SquareCoord> GOALS = List.of(sq(10, 4), sq(10, 6), sq(10, 8));

    SaboteurGameState state;
    SaboteurForwardModel fm;

    @Before
    public void setup() {
        SaboteurGameParameters params = new SaboteurGameParameters();
        params.setRandomSeed(3);
        state = new SaboteurGameState(params, 4);
        fm = new SaboteurForwardModel();
        fm.setup(state);
    }

    static SquareCoord sq(int x, int y) {
        return new SquareCoord(x, y);
    }

    static PathCard path(boolean e, boolean s, boolean w, boolean n) {
        return new PathCard(PathCard.PathCardType.Path, new boolean[]{e, s, w, n});
    }

    /**
     * Replaces the current player's hand with the cards.
     */
    void giveHand(SaboteurCard... cards) {
        Deck<SaboteurCard> hand = state.playerDecks.get(state.getCurrentPlayer());
        hand.clear();
        for (SaboteurCard c : cards)
            hand.add(c);
    }

    Set<AbstractAction> placements() {
        Set<AbstractAction> retValue = new HashSet<>();
        for (AbstractAction a : fm.computeAvailableActions(state))
            if (a instanceof PlacePathCard)
                retValue.add(a);
        return retValue;
    }

    /**
     * Swaps goal cards so that the treasure is at the position.
     */
    void putTreasureAt(SquareCoord pos) {
        for (SquareCoord g : GOALS)
            if (state.gridBoard.get(g).hasTreasure()) {
                PathCard treasure = state.gridBoard.get(g);
                state.gridBoard.set(g, state.gridBoard.get(pos));
                state.gridBoard.set(pos, treasure);
                return;
            }
        fail("no treasure");
    }

    @Test
    public void setupPutsTheStartAndThreeHiddenGoalsOnTheBoard() {
        assertEquals(START, state.startingSquare);
        assertEquals(PathCard.PathCardType.Start, state.gridBoard.get(START).type);
        assertEquals(4, state.gridBoard.size());
        int treasures = 0;
        for (SquareCoord g : GOALS) {
            assertEquals(PathCard.PathCardType.Goal, state.gridBoard.get(g).type);
            if (state.gridBoard.get(g).hasTreasure()) treasures++;
            for (int p = 0; p < 4; p++)
                assertFalse(state.gridBoard.isVisible(g, p));
        }
        assertEquals(1, treasures);
        assertEquals(Set.of(sq(3, 6), sq(2, 7), sq(1, 6), sq(2, 5)), state.pathCardOptions);
    }

    @Test
    public void pathSymbolsMatchTheirSides() {
        assertEquals("─", path(T, F, T, F).getString());
        assertEquals("│", path(F, T, F, T).getString());
        assertEquals("┌", path(T, T, F, F).getString());
        assertEquals("┐", path(F, T, T, F).getString());
        assertEquals("└", path(T, F, F, T).getString());
        assertEquals("┘", path(F, F, T, T).getString());
        assertEquals("┬", path(T, T, T, F).getString());
        assertEquals("├", path(T, T, F, T).getString());
        assertEquals("┴", path(T, F, T, T).getString());
    }

    @Test
    public void rotatingTurnsTheCardRound() {
        PathCard corner = path(T, T, F, F);
        assertFalse(corner.isSymmetric());
        PathCard turned = corner.rotated();
        assertEquals(List.of(F, F, T, T), List.of(turned.opensTo(0), turned.opensTo(1), turned.opensTo(2), turned.opensTo(3)));
        assertEquals(corner.getComponentID(), turned.getComponentID());
        // the card itself is unchanged
        assertTrue(corner.opensTo(SquareCoord.E));
        assertTrue(path(T, F, T, F).isSymmetric());
        assertTrue(path(T, T, T, T).isSymmetric());
    }

    @Test
    public void pathsMustMeetAcrossTheSidesTheyShare() {
        // the start is open on every side, so an east-west path fits east and west of it, but not north or south
        PathCard straight = path(T, F, T, F);
        giveHand(straight);
        int id = straight.getComponentID();
        assertEquals(Set.of(new PlacePathCard(sq(3, 6), id, false), new PlacePathCard(sq(1, 6), id, false)),
                placements());
    }

    @Test
    public void aCardMayBePlacedTurnedRound() {
        // a corner open east and south fits west of the start (meeting it with its east side) and north of it; turned
        // round, open west and north, it fits east and south of the start
        PathCard corner = path(T, T, F, F);
        giveHand(corner);
        int id = corner.getComponentID();
        assertEquals(Set.of(new PlacePathCard(sq(1, 6), id, false), new PlacePathCard(sq(2, 5), id, false),
                        new PlacePathCard(sq(3, 6), id, true), new PlacePathCard(sq(2, 7), id, true)),
                placements());
        // placed turned round, east of the start, it opens west and north
        fm.next(state, new PlacePathCard(sq(3, 6), id, true));
        PathCard placed = state.gridBoard.get(sq(3, 6));
        assertEquals(List.of(F, F, T, T), List.of(placed.opensTo(0), placed.opensTo(1), placed.opensTo(2), placed.opensTo(3)));
    }

    @Test
    public void aCardPlacedTurnedRoundCanBePlayedAgainLater() {
        PathCard corner = path(T, T, F, F);
        giveHand(corner);
        // the state's lookup of components by id now holds the card as it was dealt
        state.getAllComponents();
        fm.next(state, new PlacePathCard(sq(3, 6), corner.getComponentID(), true));
        // a later round returns the card from the board to a hand; here, straight to the next player's
        PathCard placed = state.gridBoard.remove(sq(3, 6));
        giveHand(placed);
        // open west and north as it lies, it fits west of the start turned back again
        fm.next(state, new PlacePathCard(sq(1, 6), corner.getComponentID(), true));
        PathCard again = state.gridBoard.get(sq(1, 6));
        assertTrue(again.opensTo(SquareCoord.E) && again.opensTo(SquareCoord.S));
    }

    @Test
    public void aPathMustMeetANeighbourOnTheSideTheyShare() {
        // a corner east of the start, open west (to the start) and north, leaves an opening north of it at (3,5)
        PathCard corner = path(F, F, T, T);
        giveHand(corner);
        fm.next(state, new PlacePathCard(sq(3, 6), corner.getComponentID(), false));
        assertTrue(state.pathCardOptions.contains(sq(3, 5)));
        // a north-south path fits there, meeting the corner's open north side with its south side; it also fits north
        // and south of the start, but not west of it
        PathCard straight = path(F, T, F, T);
        giveHand(straight);
        int id = straight.getComponentID();
        assertEquals(Set.of(new PlacePathCard(sq(3, 5), id, false), new PlacePathCard(sq(2, 5), id, false),
                new PlacePathCard(sq(2, 7), id, false)), placements());
    }

    @Test
    public void placementsStayOnTheBoard() {
        // a north-south path from the start to the top row leaves an opening above the board
        for (int y = 0; y < 5; y++)
            state.gridBoard.set(sq(2, y), path(F, T, F, T));
        PathCard last = path(F, T, F, T);
        giveHand(last);
        fm.next(state, new PlacePathCard(sq(2, 5), last.getComponentID(), false));
        assertTrue(state.pathCardOptions.contains(sq(2, -1)));
        // the next player's north-south path fits only south of the start
        PathCard straight = path(F, T, F, T);
        giveHand(straight);
        assertEquals(Set.of(new PlacePathCard(sq(2, 7), straight.getComponentID(), false)), placements());
    }

    @Test
    public void reachingAGoalRevealsItToEveryone() {
        // a path east from the start to the coal at (10,6), finished by a card placed next to the start
        putTreasureAt(sq(10, 8));
        for (int x = 4; x < 10; x++)
            state.gridBoard.set(sq(x, 6), path(T, F, T, F));
        PathCard last = path(T, F, T, F);
        giveHand(last);
        fm.next(state, new PlacePathCard(sq(3, 6), last.getComponentID(), false));
        for (int p = 0; p < 4; p++) {
            assertTrue(state.gridBoard.isVisible(sq(10, 6), p));
            assertFalse(state.gridBoard.isVisible(sq(10, 4), p));
        }
        assertEquals(Set.of(sq(10, 6)), state.goalLocationsFound);
        // the coal is open on every side, so the path can go on round it
        assertTrue(state.pathCardOptions.containsAll(List.of(sq(10, 5), sq(11, 6), sq(10, 7))));
    }

    @Test
    public void aMapCardRevealsAGoalToItsPlayerOnly() {
        int player = state.getCurrentPlayer();
        giveHand(new ActionCard(ActionCard.ActionCardType.Map, new ActionCard.ToolCardType[]{}));
        Set<AbstractAction> maps = new HashSet<>();
        for (AbstractAction a : fm.computeAvailableActions(state))
            if (a instanceof PlayMapCard) maps.add(a);
        assertEquals(Set.of(new PlayMapCard(sq(10, 4)), new PlayMapCard(sq(10, 6)), new PlayMapCard(sq(10, 8))), maps);
        fm.next(state, new PlayMapCard(sq(10, 8)));
        for (int p = 0; p < 4; p++)
            assertEquals(p == player, state.gridBoard.isVisible(sq(10, 8), p));
    }

    @Test
    public void aRockfallReopensThePosition() {
        // a dead end east of the start, then the rockfall that clears it
        PathCard deadEnd = new PathCard(PathCard.PathCardType.Edge, new boolean[]{T, F, T, F});
        state.gridBoard.set(sq(3, 6), deadEnd);
        ActionCard rockfall = new ActionCard(ActionCard.ActionCardType.RockFall, new ActionCard.ToolCardType[]{});
        giveHand(rockfall);
        int discards = state.discardDeck.getSize();
        assertTrue(fm.computeAvailableActions(state).contains(new PlayRockFallCard(sq(3, 6))));
        fm.next(state, new PlayRockFallCard(sq(3, 6)));
        assertFalse(state.gridBoard.contains(sq(3, 6)));
        // both the Rockfall card and the card it removed are discarded; the removed card face up
        assertEquals(discards + 2, state.discardDeck.getSize());
        assertTrue(state.discardDeck.getComponents().contains(rockfall));
        int idx = state.discardDeck.getComponents().indexOf(deadEnd);
        assertTrue(idx >= 0);
        for (int p = 0; p < 4; p++)
            assertTrue(state.discardDeck.getVisibilityForPlayer(idx, p));
        assertEquals(Set.of(sq(3, 6), sq(2, 7), sq(1, 6), sq(2, 5)), state.pathCardOptions);
    }

    @Test
    public void redeterminisationShufflesOnlyTheGoalsHiddenFromThePlayer() {
        // player 0 has seen the coal at (10,4); the treasure is at (10,6)
        putTreasureAt(sq(10, 6));
        state.gridBoard.setVisible(sq(10, 4), 0, true);
        Map<SquareCoord, Integer> treasureFor0 = new HashMap<>(), treasureFor1 = new HashMap<>();
        // each hidden goal holds the treasure with chance 1/2 for player 0 and 1/3 for player 1, so a position is
        // missed in 100 copies with chance at most (2/3)^100
        for (int i = 0; i < 100; i++) {
            SaboteurGameState copy0 = (SaboteurGameState) state.copy(0);
            SaboteurGameState copy1 = (SaboteurGameState) state.copy(1);
            assertEquals(1, countTreasure(copy0, treasureFor0));
            assertEquals(1, countTreasure(copy1, treasureFor1));
            assertFalse(copy0.gridBoard.get(sq(10, 4)).hasTreasure());
            assertTrue(copy0.gridBoard.isVisible(sq(10, 4), 0));
        }
        assertEquals(Set.of(sq(10, 6), sq(10, 8)), treasureFor0.keySet());
        assertEquals(new HashSet<>(GOALS), treasureFor1.keySet());
    }

    private static int countTreasure(SaboteurGameState copy, Map<SquareCoord, Integer> counts) {
        int n = 0;
        for (SquareCoord g : GOALS) {
            assertEquals(PathCard.PathCardType.Goal, copy.gridBoard.get(g).type);
            if (copy.gridBoard.get(g).hasTreasure()) {
                counts.merge(g, 1, Integer::sum);
                n++;
            }
        }
        return n;
    }
}
