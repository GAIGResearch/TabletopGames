package games.perudo;

import core.actions.AbstractAction;
import games.perudo.actions.Calza;
import games.perudo.actions.Dudo;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.perudo.PerudoParameters.PACO;
import static games.perudo.PerudoTestUtils.*;
import static org.junit.Assert.*;

/**
 * The limits on the bids offered, with the default PerudoParameters: at most maxRaise (2) dice above the lowest bid
 * allowed on each face, or up to maxBidPercent (50%) of the dice on the table, and no fewer than minBidPercent (25%)
 * of them; halved for Pacos in a normal round.
 */
public class PerudoBidLimitTest {

    PerudoForwardModel fm;
    PerudoGameState state;

    @Before
    public void setup() {
        fm = new PerudoForwardModel();
        state = newState(new PerudoParameters(), 2);   // 10 dice: at least 2, and up to 5 (Pacos up to 3)
    }

    @Test
    public void openingBidsRunFromAQuarterToHalfTheDice() {
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(bidRange(2, 5, 2, 6), new HashSet<>(actions));
        assertEquals(20, actions.size());
    }

    @Test
    public void raisesRunToTwoAboveTheLowestBidOnEachFace() {
        state.setBid(5, 3, 1);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        Set<Object> expected = new HashSet<>(bidRange(5, 7, 4, 6));   // five of a higher face, up to seven
        expected.addAll(bidRange(6, 8, 2, 3));                         // six of a face no higher, up to eight
        expected.addAll(bidRange(3, 5, PACO, PACO));                   // three Pacos, up to five
        expected.add(new Dudo());
        assertEquals(expected, new HashSet<>(actions));
        assertEquals(expected.size(), actions.size());
    }

    @Test
    public void raisesFromPacosRunToTwoAboveTheLowestBidOnEachFace() {
        state.setBid(3, PACO, 1);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(bidRange(4, 6, PACO, PACO), pacoBids(actions));
        assertEquals(bidRange(7, 9, 2, 6), faceBids(actions));
    }

    @Test
    public void bidsStopAtTheDiceOnTheTable() {
        state.setBid(9, 6, 1);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(bidRange(10, 10, 2, 6), faceBids(actions));
    }

    @Test
    public void withManyDiceTheBidsRunFromAQuarterToHalfTheDice() {
        PerudoParameters params = new PerudoParameters();
        params.setParameterValue("nDicePerPlayer", 6);
        params.setParameterValue("calza", true);
        state = newState(params, 6);   // 36 dice: at least 9, and up to 18 (Pacos up to 9)
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(bidRange(9, 18, 2, 6), new HashSet<>(actions));

        state.setBid(9, 2, 1);
        actions = fm.computeAvailableActions(state);
        assertEquals(bidRange(5, 9, PACO, PACO), pacoBids(actions));   // ceil(9 / 2) = 5 Pacos
        Set<Object> expected = new HashSet<>(bidRange(9, 18, 3, 6));
        expected.addAll(bidRange(10, 18, 2, 2));
        assertEquals(expected, new HashSet<>(faceBids(actions)));
        assertTrue(actions.contains(new Dudo()));
        assertTrue(actions.contains(new Calza()));
    }

    @Test
    public void aPalificoRoundOpensOnEveryFaceWithTheLimitsUnhalved() {
        PerudoParameters params = new PerudoParameters();
        params.setParameterValue("palifico", true);
        state = newState(params, 2);
        state.setPalificoRound(true);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(bidRange(2, 5, PACO, 6), new HashSet<>(actions));
    }

    @Test
    public void everyLegalBidIsOfferedWithNoLimits() {
        state = newState(allBidsParams(), 2);
        assertEquals(bidRange(1, 10, 2, 6), new HashSet<>(fm.computeAvailableActions(state)));
    }
}
