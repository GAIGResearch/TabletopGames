package web;

import com.google.gson.JsonObject;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.diplomacy.DiplomacyForwardModel;
import games.diplomacy.DiplomacyGameState;
import games.diplomacy.gui.DiplomacyPlanner;
import gui.IMovePlanner;
import org.junit.Before;
import org.junit.Test;
import players.human.ActionController;
import players.simple.RandomPlayer;

import java.util.*;

import static org.junit.Assert.*;

/**
 * A plan made in the page (MovePlan) and carried out by the browser player's seat (BrowserPlayer): Austria's orders in
 * Spring 1901 (A Bud, F Tri, A Vie), with the browser player as Austria. The game is moved on by hand, as the game
 * loop would, rather than run.
 */
public class MovePlanTest {

    static final int AUSTRIA = 0;

    DiplomacyGameState state;
    DiplomacyForwardModel fm;
    DiplomacyPlanner planner;
    ActionController ac;
    BrowserPlayer browser;
    List<String> stopped = new ArrayList<>();

    @Before
    public void setup() {
        Game game = GameType.Diplomacy.createGameInstance(7, 1);
        ac = new ActionController();
        browser = new BrowserPlayer(ac, stopped::add);
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(browser);
        for (int i = 1; i < 7; i++) players.add(new RandomPlayer(new Random(i)));
        game.reset(players);
        state = (DiplomacyGameState) game.getGameState();
        fm = (DiplomacyForwardModel) game.getForwardModel();
        planner = new DiplomacyPlanner(fm);
        assertEquals(AUSTRIA, state.getCurrentPlayer());
    }

    static AbstractAction option(MovePlan plan, String text) {
        return plan.options().stream().filter(a -> a.toString().equals(text)).findFirst()
                .orElseThrow(() -> new AssertionError("not an option: " + text));
    }

    static List<String> texts(List<AbstractAction> actions) {
        return actions.stream().map(AbstractAction::toString).toList();
    }

    MovePlan plan(String... orders) {
        MovePlan plan = new MovePlan(planner, state.copy(AUSTRIA), AUSTRIA);
        for (String o : orders)
            assertTrue(plan.add(option(plan, o)));
        return plan;
    }

    @Test
    public void anOrderForAUnitAlreadyPlannedReplacesItsOrder() {
        MovePlan plan = plan("Tri-Alb", "Bud-Ser", "Bud-Rum");
        assertEquals(List.of("Tri-Alb", "Bud-Rum"), texts(plan.steps()));
        // the preview has the orders; the decision planned from does not
        assertEquals(List.of("Tri-Alb", "Bud-Rum"), texts(new ArrayList<>(((DiplomacyGameState) plan.preview()).getOrders(AUSTRIA))));
        assertTrue(((DiplomacyGameState) plan.decision()).getOrders(AUSTRIA).isEmpty());
    }

    @Test
    public void stepsMayBeTakenOutOfThePlan() {
        MovePlan plan = plan("Tri-Alb", "Bud-Rum", "Vie-Gal");
        plan.remove(1);
        assertEquals(List.of("Tri-Alb", "Vie-Gal"), texts(plan.steps()));
        assertEquals(2, ((DiplomacyGameState) plan.preview()).getOrders(AUSTRIA).size());
        plan.clear();
        assertTrue(plan.steps().isEmpty());
        assertTrue(((DiplomacyGameState) plan.preview()).getOrders(AUSTRIA).isEmpty());
    }

    @Test
    public void onlyTheOptionsMayBeAdded() {
        MovePlan plan = plan();
        // an English order
        AbstractAction english = fm.ordersFor(state, state.getMap().province("Lon")).get(0);
        assertFalse(plan.add(english));
        assertTrue(plan.steps().isEmpty());
    }

    @Test
    public void thePageIsSentTheStepsWarningsAndTheSendButtonsLabel() {
        JsonObject json = plan("Tri-Alb").toJson();
        assertEquals("plan", json.get("type").getAsString());
        assertEquals(1, json.getAsJsonArray("steps").size());
        assertEquals("F Tri-Alb", json.getAsJsonArray("steps").get(0).getAsString());
        // A Bud and A Vie hold
        assertEquals(1, json.getAsJsonArray("warnings").size());
        assertEquals("Send orders", json.get("send").getAsString());
    }

    /**
     * Answers the browser player's decisions from the plan, as the game loop would ask for them, until it is no longer
     * the player's turn. Returns the actions sent.
     */
    List<String> carryOut() {
        List<String> sent = new ArrayList<>();
        while (state.getCurrentPlayer() == AUSTRIA) {
            AbstractGameState observation = state.copy(AUSTRIA);
            AbstractAction a = browser.fromPlan(observation, fm.computeAvailableActions(observation));
            assertNotNull("no action from the plan for " + state.unitsToOrder(AUSTRIA).get(0), a);
            fm.next(state, a);
            sent.add(a.toString());
        }
        return sent;
    }

    @Test
    public void aPlanIsSentInTheGamesOrderWithHoldsForUnitsGivenNoOrder() {
        MovePlan plan = plan("Vie-Gal", "Tri-Alb");
        browser.send(planner, plan.decision(), plan.steps());
        // the game thread, waiting for the page, would have taken the wake-up
        ac.reset();
        // in the order the forward model asks about the units: by province index
        List<String> expected = new ArrayList<>();
        for (var p : state.unitsToOrder(AUSTRIA))
            expected.add(Map.of("Vie", "Vie-Gal", "Tri", "Tri-Alb").getOrDefault(p.name(), p.name() + " Holds"));
        assertEquals(expected, carryOut());
        assertTrue(stopped.isEmpty());
    }

    @Test
    public void thePlanEndsWithThePlayersTurn() {
        MovePlan plan = plan("Vie-Gal");
        browser.send(planner, plan.decision(), plan.steps());
        ac.reset();
        carryOut();
        assertTrue(browser.isSending());
        // England's turn now: the next decision of the browser player is not in the run
        AbstractGameState later = state.copy(AUSTRIA);
        assertNull(browser.fromPlan(later, fm.computeAvailableActions(later)));
        assertFalse(browser.isSending());
        assertTrue(stopped.isEmpty());
    }

    @Test
    public void aPlanWhoseNextStepIsNotOfferedStopsAndThePlayerIsTold() {
        // a planner with no fallback, whose plan has only Vie's order: the game asks first about another unit
        IMovePlanner noFallback = new IMovePlanner() {
            @Override
            public boolean plans(AbstractGameState state, int player) {
                return true;
            }

            @Override
            public List<AbstractAction> options(AbstractGameState planned, int player) {
                return planner.options(planned, player);
            }

            @Override
            public void apply(AbstractGameState planned, AbstractAction action) {
                planner.apply(planned, action);
            }
        };
        MovePlan plan = plan("Vie-Gal");
        assertNotEquals("Vie", state.unitsToOrder(AUSTRIA).get(0).name());
        browser.send(noFallback, plan.decision(), plan.steps());
        ac.reset();
        AbstractGameState observation = state.copy(AUSTRIA);
        assertNull(browser.fromPlan(observation, fm.computeAvailableActions(observation)));
        assertFalse(browser.isSending());
        assertEquals(1, stopped.size());
        assertTrue(stopped.get(0), stopped.get(0).contains("Vie-Gal is no longer possible"));
    }
}
