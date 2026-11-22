package games.catan;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import games.catan.actions.dev.PlayKnightCard;
import games.catan.actions.dev.PlayKnightCardDeep;
import games.catan.actions.robber.MoveRobber;
import games.catan.actions.robber.MoveRobberAndSteal;
import org.junit.Test;
import players.rhea.RHEAParams;
import players.rhea.RHEAPlayer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Playing a knight uses up the knight card, and moving the robber with a steal also steals, so none of these
 * actions may equal another of a different class, or an agent can execute a stored knight where only a plain
 * robber move is legal.
 */
public class RobberActionEqualityTest {

    private static void assertUnequalBothWays(Object a, Object b) {
        assertNotEquals(a, b);
        assertNotEquals(b, a);
    }

    @Test
    public void playKnightCardIsNotEqualToMoveRobberAndSteal() {
        assertUnequalBothWays(new PlayKnightCard(2, 3, 0, 1), new MoveRobberAndSteal(2, 3, 0, 1));
    }

    @Test
    public void playKnightCardDeepIsNotEqualToMoveRobber() {
        assertUnequalBothWays(new PlayKnightCardDeep(2, 3, 0), new MoveRobber(2, 3, 0));
    }

    @Test
    public void moveRobberIsNotEqualToMoveRobberAndSteal() {
        assertUnequalBothWays(new MoveRobber(2, 3, 0), new MoveRobberAndSteal(2, 3, 0, 1));
    }

    @Test
    public void sameClassAndFieldsAreStillEqual() {
        assertEquals(new PlayKnightCard(2, 3, 0, 1), new PlayKnightCard(2, 3, 0, 1));
        assertEquals(new PlayKnightCardDeep(2, 3, 0), new PlayKnightCardDeep(2, 3, 0));
        assertEquals(new MoveRobberAndSteal(2, 3, 0, 1), new MoveRobberAndSteal(2, 3, 0, 1));
        assertEquals(new MoveRobber(2, 3, 0), new MoveRobber(2, 3, 0));
        assertEquals(new MoveRobber(2, 3, 0), new MoveRobber(2, 3, 0).copy());
        assertNotEquals(new MoveRobberAndSteal(2, 3, 0, 1), new MoveRobberAndSteal(2, 3, 0, 2));
    }

    @Test
    public void rheaCompletesCatanWithoutTrading() {
        for (long seed = 1; seed <= 3; seed++) {
            CatanParameters params = new CatanParameters();
            params.setParameterValue("tradingAllowed", false);
            params.tradingAllowed = false;
            params.setRandomSeed(seed);
            Game game = GameType.Catan.createGameInstance(3, params);
            List<AbstractPlayer> players = new ArrayList<>();
            for (int p = 0; p < 3; p++) {
                RHEAParams rheaParams = new RHEAParams();
                rheaParams.budget = 200;
                rheaParams.setRandomSeed(seed * 10 + p);
                players.add(new RHEAPlayer(rheaParams));
            }
            game.reset(players);
            game.run();
            assertFalse(game.getGameState().isNotTerminal());
        }
    }
}
