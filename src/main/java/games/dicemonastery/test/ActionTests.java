package games.dicemonastery.test;


import core.actions.*;
import games.dicemonastery.*;
import games.dicemonastery.actions.*;
import org.junit.*;
import players.simple.RandomPlayer;

import java.util.*;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Phase.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
import static games.dicemonastery.DiceMonasteryConstants.Season.*;
import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;

public class ActionTests {


    DiceMonasteryForwardModel fm = new DiceMonasteryForwardModel();
    DiceMonasteryGame game = new DiceMonasteryGame(fm, new DiceMonasteryGameState(new DiceMonasteryParams(3), 4));
    DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
    DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) game.getGameState().getTurnOrder();
    RandomPlayer rnd = new RandomPlayer();

    private void startOfUseMonkPhaseForArea(ActionArea region) {
        // first place all monks randomly
        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        } while (state.getGamePhase() == PLACE_MONKS);

        // then Pass until we get to the point required
        do {
            fm.next(state, new Pass());
        } while (turnOrder.getCurrentArea() != region);
    }

    @Test
    public void meadowActionsCorrectSpring() {
        startOfUseMonkPhaseForArea(MEADOW);
        assertEquals(4, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
        assertTrue(fm.computeAvailableActions(state).contains(new SowWheat()));
        assertTrue(fm.computeAvailableActions(state).contains(new Forage()));
        assertTrue(fm.computeAvailableActions(state).contains(new PlaceSkep()));
    }

    @Test
    public void meadowActionsCorrectAutumn() {
        fail("Not yet implemented");
    }

    @Test
    public void sowWheat() {
        fail("Not yet implemented");
    }

    @Test
    public void harvestGrain() {
        fail("Not yet implemented");
    }

    @Test
    public void placeSkep() {
        fail("Not yet implemented");
    }

    @Test
    public void collectSkep() {
        fail("Not yet implemented");
    }

    @Test
    public void forage() {
        fail("Not yet implemented");
    }
}
