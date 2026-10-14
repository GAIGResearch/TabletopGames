package games.stratego;

import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.components.grid.SquareCoord;
import games.GameType;
import games.stratego.actions.AttackMove;
import games.stratego.actions.Move;
import games.stratego.actions.NormalMove;
import games.stratego.components.Piece;
import org.junit.Before;
import org.junit.Test;
import utilities.Vector2D;

import java.util.*;

import static games.stratego.components.Piece.Alliance.BLUE;
import static games.stratego.components.Piece.Alliance.RED;
import static games.stratego.components.Piece.PieceType.*;
import static org.junit.Assert.*;

/**
 * Red (player 0) moves first. The lakes are at x = 2, 3, 6, 7 and y = 4, 5.
 */
public class StrategoBoardTest {

    Game game = GameType.Stratego.createGameInstance(2, 1, new StrategoParams());
    StrategoGameState state = (StrategoGameState) game.getGameState();
    StrategoForwardModel fm = new StrategoForwardModel();

    @Before
    public void emptyTheBoard() {
        for (SquareCoord pos : new ArrayList<>(state.getGridBoard().positions()))
            state.getGridBoard().remove(pos);
        // a flag each, out of the way, so that the game goes on
        place(FLAG, RED, 0, 0);
        place(FLAG, BLUE, 9, 9);
    }

    Piece place(Piece.PieceType type, Piece.Alliance alliance, int x, int y) {
        Piece p = new Piece(type, alliance, new Vector2D(x, y));
        state.setPiece(new Vector2D(x, y), p);
        return p;
    }

    Piece at(int x, int y) {
        return state.getPiece(new Vector2D(x, y));
    }

    void attack(Piece attacker, Piece defender) {
        fm.next(state, new AttackMove(attacker.getComponentID(), defender.getComponentID()));
    }

    @Test
    public void theHigherRankWinsAndTakesTheSquare() {
        Piece major = place(MAJOR, RED, 1, 1);
        Piece captain = place(CAPTAIN, BLUE, 1, 2);
        attack(major, captain);
        assertNull(at(1, 1));
        assertEquals(major.getComponentID(), at(1, 2).getComponentID());
        assertEquals(new Vector2D(1, 2), at(1, 2).getPiecePosition());
        assertTrue(at(1, 2).isPieceKnown());
    }

    @Test
    public void theLowerRankLosesAndTheDefenderIsRevealed() {
        place(CAPTAIN, RED, 1, 1);
        Piece major = place(MAJOR, BLUE, 1, 2);
        attack(at(1, 1), major);
        assertNull(at(1, 1));
        assertEquals(MAJOR, at(1, 2).getPieceType());
        assertTrue(at(1, 2).isPieceKnown());
    }

    @Test
    public void equalRanksBothGo() {
        place(MAJOR, RED, 1, 1);
        place(MAJOR, BLUE, 1, 2);
        attack(at(1, 1), at(1, 2));
        assertNull(at(1, 1));
        assertNull(at(1, 2));
    }

    @Test
    public void aSpyAttackingTheMarshalWins() {
        place(SPY, RED, 1, 1);
        place(MARSHAL, BLUE, 1, 2);
        attack(at(1, 1), at(1, 2));
        assertEquals(SPY, at(1, 2).getPieceType());
    }

    @Test
    public void onlyAMinerDefusesABomb() {
        place(MINER, RED, 1, 1);
        place(BOMB, BLUE, 1, 2);
        place(SERGEANT, RED, 5, 1);
        place(BOMB, BLUE, 5, 2);
        place(SERGEANT, BLUE, 8, 8);
        attack(at(1, 1), at(1, 2));
        assertEquals(MINER, at(1, 2).getPieceType());
        fm.next(state, fm.computeAvailableActions(state).get(0));
        attack(at(5, 1), at(5, 2));
        assertNull(at(5, 1));
        assertEquals(BOMB, at(5, 2).getPieceType());
    }

    @Test
    public void takingTheFlagWins() {
        place(SCOUT, RED, 9, 8);
        place(SERGEANT, BLUE, 1, 1);  // so that blue could move
        attack(at(9, 8), at(9, 9));
        assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[1]);
    }

    @Test
    public void aScoutMovingFarRevealsItself() {
        place(SCOUT, RED, 1, 1);
        place(SCOUT, RED, 8, 1);
        place(SERGEANT, BLUE, 1, 9);
        fm.next(state, new NormalMove(at(1, 1).getComponentID(), new Vector2D(1, 2)));
        assertFalse(at(1, 2).isPieceKnown());
        fm.next(state, fm.computeAvailableActions(state).get(0));
        fm.next(state, new NormalMove(at(8, 1).getComponentID(), new Vector2D(8, 3)));
        assertTrue(at(8, 3).isPieceKnown());
    }

    @Test
    public void aMovedPieceIsFoundByItsIdWhereItNowIs() {
        // getComponentById would give the piece where it was first put on the board
        Piece sergeant = place(SERGEANT, RED, 1, 1);
        place(SERGEANT, BLUE, 8, 8);
        state.getComponentById(sergeant.getComponentID());
        fm.next(state, new NormalMove(sergeant.getComponentID(), new Vector2D(1, 2)));
        fm.next(state, fm.computeAvailableActions(state).get(0));
        Set<Vector2D> destinations = new HashSet<>();
        for (AbstractAction a : fm.computeAvailableActions(state))
            if (((Move) a).getMovedPieceID() == sergeant.getComponentID())
                destinations.add(((Move) a).to(state));
        assertEquals(Set.of(new Vector2D(1, 3), new Vector2D(1, 1), new Vector2D(0, 2), new Vector2D(2, 2)), destinations);
        // and the move takes it from where it now is
        fm.next(state, new NormalMove(sergeant.getComponentID(), new Vector2D(1, 3)));
        assertNull(at(1, 2));
        assertEquals(sergeant.getComponentID(), at(1, 3).getComponentID());
        assertEquals(4, state.getGridBoard().size());
    }

    @Test
    public void aDependentMoveIsUnchangedByBeingPlayed() {
        place(SERGEANT, RED, 1, 1);
        place(SERGEANT, BLUE, 8, 8);
        NormalMove move = new NormalMove(new Vector2D(1, 1), new Vector2D(0, 1));
        NormalMove twin = new NormalMove(new Vector2D(1, 1), new Vector2D(0, 1));
        fm.next(state, move);
        assertEquals(SERGEANT, at(1, 2).getPieceType());
        assertEquals(twin, move);
        assertEquals(twin.hashCode(), move.hashCode());
    }

    @Test
    public void aScoutStopsAtTheFirstEnemy() {
        place(SCOUT, RED, 1, 1);
        place(SERGEANT, BLUE, 1, 3);
        place(SERGEANT, BLUE, 1, 6);
        Set<Vector2D> targets = new HashSet<>();
        for (AbstractAction a : at(1, 1).calculateMoves(state, ActionSpace.Default))
            if (a instanceof AttackMove m)
                targets.add(m.to(state));
        assertEquals(Set.of(new Vector2D(1, 3)), targets);
    }

    @Test
    public void copiesAreEqualAndIndependent() {
        place(SERGEANT, RED, 1, 1);
        place(SERGEANT, BLUE, 8, 8);
        StrategoGameState copy = (StrategoGameState) state.copy();
        assertEquals(state.getGridBoard(), copy.getGridBoard());
        fm.next(copy, new NormalMove(at(1, 1).getComponentID(), new Vector2D(1, 2)));
        assertNotNull(at(1, 1));
        assertNull(copy.getPiece(new Vector2D(1, 1)));
    }

    @Test
    public void aPlayersCopyDealsTheOpponentsUnknownTypesAgain() {
        place(SERGEANT, RED, 1, 1);
        Piece known = place(MAJOR, BLUE, 4, 8).revealed();
        state.setPiece(new Vector2D(4, 8), known);
        place(MARSHAL, BLUE, 5, 8);
        place(SPY, BLUE, 6, 8);
        place(SCOUT, BLUE, 7, 8);
        boolean anyDealtElsewhere = false;
        for (int i = 0; i < 20; i++) {
            StrategoGameState copy = (StrategoGameState) state.copy(0);
            assertEquals(MAJOR, copy.getPiece(new Vector2D(4, 8)).getPieceType());
            assertEquals(SERGEANT, copy.getPiece(new Vector2D(1, 1)).getPieceType());
            List<Piece.PieceType> hidden = new ArrayList<>();
            for (int x : new int[]{5, 6, 7, 9}) {
                Piece p = copy.getPiece(new Vector2D(x, x == 9 ? 9 : 8));
                hidden.add(p.getPieceType());
                anyDealtElsewhere |= p.getPieceType() != at(x, x == 9 ? 9 : 8).getPieceType();
                assertFalse(p.isPieceKnown());
                // nothing on the piece gives away what it really is
                assertEquals(p.getPieceType().name(), p.getComponentName());
            }
            Collections.sort(hidden);
            assertEquals(List.of(SPY, SCOUT, MARSHAL, FLAG), hidden);
        }
        assertTrue(anyDealtElsewhere);
    }
}
