package web;

import core.AbstractGameState;
import core.Game;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.BoardNode;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import core.properties.PropertyBoolean;
import core.properties.PropertyIntArray;
import core.properties.PropertyString;
import games.GameType;
import games.pandemic.PandemicForwardModel;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicParameters;
import games.pandemic.PandemicTurnOrder;
import games.pandemic.actions.*;
import games.pandemic.gui.PandemicPlanner;
import org.junit.Test;
import players.human.ActionController;
import players.simple.RandomPlayer;
import utilities.Hash;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static core.CoreConstants.nameHash;
import static core.CoreConstants.playerHandHash;
import static games.pandemic.PandemicConstants.*;
import static org.junit.Assert.*;

/**
 * Pandemic turns planned in the page and carried out by the browser player's seat, one test for each kind of action
 * that may be planned, each from a game set up for it: the browser player is player 0, whose turn it is, with player 1
 * beside them; the hands are emptied and the cards each test needs dealt.
 * <p>
 * Each plan is built a step at a time, as in the page: each step added must leave the steps before it in the plan
 * (the plan is carried out afresh whenever it changes, and some actions record what they did on themselves). The plan
 * is then sent and carried out in the game, and after each step the game must be as the plan showed it.
 */
public class PandemicMovePlanTest {

    static final int BROWSER = 0;
    static final String[] COLOURS = {"yellow", "red", "blue", "black"};

    PandemicGameState state;
    PandemicForwardModel fm;
    PandemicPlanner planner;
    ActionController ac;
    BrowserPlayer browser;
    List<String> stopped = new ArrayList<>();

    /**
     * A 2-player game with the roles given, with a turn of the number of actions given.
     */
    @SuppressWarnings("unchecked")
    void newGame(String role0, String role1, int actionsPerTurn) {
        PandemicParameters params = (PandemicParameters) GameType.Pandemic.createParameters(1);
        params.setParameterValue("player0Role", role0);
        params.setParameterValue("player1Role", role1);
        params.setParameterValue("nActionsPerTurn", actionsPerTurn);
        Game game = GameType.Pandemic.createGameInstance(2, 1, params);
        ac = new ActionController();
        browser = new BrowserPlayer(ac, stopped::add);
        game.reset(List.of(browser, new RandomPlayer(new Random(1))));
        state = (PandemicGameState) game.getGameState();
        fm = (PandemicForwardModel) game.getForwardModel();
        planner = new PandemicPlanner(fm);
        assertEquals(BROWSER, state.getCurrentPlayer());
        Deck<Card> playerDeck = (Deck<Card>) state.getComponent(playerDeckHash);
        for (int p = 0; p < 2; p++) {
            Deck<Card> hand = (Deck<Card>) state.getComponent(playerHandHash, p);
            for (Card c : new ArrayList<>(hand.getComponents())) {
                hand.remove(c);
                playerDeck.add(c);
            }
        }
    }

    void newGame(String role0, String role1) {
        // a turn long enough for every plan here, so that the game is compared with each step before the turn ends
        newGame(role0, role1, 6);
    }

    // ---- setting up the game

    void at(int player, String city) {
        MovePlayer.removePlayer(state, location(state, player), player);
        MovePlayer.placePlayer(state, city, player);
    }

    /**
     * Deals the cards named (city or event cards) from the player deck or its discards to the player's hand.
     */
    @SuppressWarnings("unchecked")
    void holding(int player, String... cards) {
        Deck<Card> hand = (Deck<Card>) state.getComponent(playerHandHash, player);
        for (String name : cards) {
            boolean found = false;
            for (int deckHash : new int[]{playerDeckHash, playerDeckDiscardHash}) {
                Deck<Card> deck = (Deck<Card>) state.getComponent(deckHash);
                Optional<Card> card = deck.getComponents().stream().filter(c -> name(c).equals(name)).findFirst();
                if (card.isPresent()) {
                    deck.remove(card.get());
                    hand.add(card.get());
                    found = true;
                    break;
                }
            }
            assertTrue("no card " + name + " to deal", found);
        }
    }

    @SuppressWarnings("unchecked")
    void discarded(String card) {
        Deck<Card> deck = (Deck<Card>) state.getComponent(playerDeckHash);
        Card c = deck.getComponents().stream().filter(x -> name(x).equals(card)).findFirst().orElseThrow();
        deck.remove(c);
        ((Deck<Card>) state.getComponent(playerDeckDiscardHash)).add(c);
    }

    void station(String city) {
        assertTrue(new AddResearchStation(city).execute(state));
    }

    void cubes(String city, String colour, int n) {
        int[] infection = ((PropertyIntArray) node(state, city).getProperty(infectionHash)).getValues();
        int i = Arrays.asList(COLOURS).indexOf(colour);
        Counter supply = (Counter) state.getComponent(Hash.GetInstance().hash("Disease Cube " + colour));
        supply.decrement(n - infection[i]);
        infection[i] = n;
    }

    void cured(String colour) {
        ((Counter) state.getComponent(Hash.GetInstance().hash("Disease " + colour))).setValue(1);
    }

    // ---- reading the game

    static String name(Card card) {
        return ((PropertyString) card.getProperty(nameHash)).value;
    }

    static BoardNode node(PandemicGameState s, String city) {
        return s.getWorld().getNodeByStringProperty(nameHash, city);
    }

    static String location(AbstractGameState gs, int player) {
        PandemicGameState s = (PandemicGameState) gs;
        return ((PropertyString) s.getComponent(playerCardHash, player).getProperty(playerLocationHash)).value;
    }

    @SuppressWarnings("unchecked")
    static List<String> hand(AbstractGameState gs, int player) {
        PandemicGameState s = (PandemicGameState) gs;
        return ((Deck<Card>) s.getComponent(playerHandHash, player)).getComponents().stream()
                .map(PandemicMovePlanTest::name).toList();
    }

    static boolean hasStation(AbstractGameState gs, String city) {
        return ((PropertyBoolean) node((PandemicGameState) gs, city).getProperty(researchStationHash)).value;
    }

    static int cubesIn(AbstractGameState gs, String city, String colour) {
        int[] infection = ((PropertyIntArray) node((PandemicGameState) gs, city).getProperty(infectionHash)).getValues();
        return infection[Arrays.asList(COLOURS).indexOf(colour)];
    }

    static int cure(AbstractGameState gs, String colour) {
        return ((Counter) ((PandemicGameState) gs).getComponent(Hash.GetInstance().hash("Disease " + colour))).getValue();
    }

    static int turnStep(AbstractGameState gs) {
        return ((PandemicTurnOrder) ((PandemicGameState) gs).getTurnOrder()).getTurnStep();
    }

    /**
     * What a plan may change: where the players are and what they hold, the research stations, the cures, the cubes
     * on the board and in the supply, the cards discarded and set aside, a quiet night, and the step of the turn.
     */
    @SuppressWarnings("unchecked")
    static String summary(AbstractGameState gs) {
        PandemicGameState s = (PandemicGameState) gs;
        StringBuilder sb = new StringBuilder();
        for (int p = 0; p < s.getNPlayers(); p++)
            sb.append("player ").append(p).append(" in ").append(location(s, p)).append(" holding ")
                    .append(hand(s, p)).append('\n');
        sb.append("stations ").append(s.getWorld().getBoardNodes().stream()
                        .map(bn -> ((PropertyString) bn.getProperty(nameHash)).value)
                        .filter(city -> hasStation(s, city)).sorted().toList()).append(", ")
                .append(((Counter) s.getComponent(researchStationHash)).getValue()).append(" left\n");
        for (String colour : COLOURS) {
            sb.append(colour).append(": cure ").append(cure(s, colour)).append(", supply ")
                    .append(((Counter) s.getComponent(Hash.GetInstance().hash("Disease Cube " + colour))).getValue())
                    .append(", in");
            for (BoardNode bn : s.getWorld().getBoardNodes()) {
                String city = ((PropertyString) bn.getProperty(nameHash)).value;
                int n = cubesIn(s, city, colour);
                if (n > 0) sb.append(' ').append(city).append('=').append(n);
            }
            sb.append('\n');
        }
        sb.append("discards ").append(((Deck<Card>) s.getComponent(playerDeckDiscardHash)).getComponents().stream()
                .map(PandemicMovePlanTest::name).toList()).append('\n');
        sb.append("set aside ").append(((Deck<Card>) s.getComponent(plannerDeckHash)).getComponents().stream()
                .map(PandemicMovePlanTest::name).toList()).append('\n');
        sb.append("quiet night ").append(s.isQuietNight()).append(", step ").append(turnStep(s));
        return sb.toString();
    }

    // ---- planning and carrying out

    /**
     * Chooses the step among the plan's options.
     */
    static Function<MovePlan, AbstractAction> step(String description, Predicate<AbstractAction> which) {
        return plan -> plan.options().stream().filter(which).findFirst()
                .orElseThrow(() -> new AssertionError("cannot plan to " + description + " after " + plan.steps()
                        + "; options " + plan.options()));
    }

    static Function<MovePlan, AbstractAction> move(MovePlayer.MoveType type, int player, String city) {
        return step(type + " p" + player + " to " + city, a -> a instanceof MovePlayer m && m.getMoveType() == type
                && m.getPlayerToMove() == player && m.getDestination().equals(city));
    }

    static Function<MovePlan, AbstractAction> drive(String city) {
        return move(MovePlayer.MoveType.DriveFerry, BROWSER, city);
    }

    static Function<MovePlan, AbstractAction> share(int giver, int receiver, String card) {
        return plan -> step("share " + card, a -> a instanceof ShareKnowledge k && k.getGiver() == giver
                && k.getReceiver() == receiver && name(k.getCard(plan.preview())).equals(card)).apply(plan);
    }

    static Function<MovePlan, AbstractAction> treat(String colour, String city) {
        return step("treat " + colour + " in " + city,
                a -> a instanceof TreatDisease t && t.getColor().equals(colour) && t.getCity().equals(city));
    }

    static Function<MovePlan, AbstractAction> cureOf(String colour) {
        return step("cure " + colour, a -> a instanceof CureDisease c && c.getColor().equals(colour));
    }

    /**
     * Plans the steps one by one, then sends the plan and carries it out in the game; returns the plan.
     */
    @SafeVarargs
    final MovePlan planAndCarryOut(Function<MovePlan, AbstractAction>... steps) {
        MovePlan plan = new MovePlan(planner, state.copy(BROWSER), BROWSER);
        List<String> previews = new ArrayList<>();
        for (int i = 0; i < steps.length; i++) {
            assertTrue(plan.add(steps[i].apply(plan)));
            assertEquals("the steps before step " + (i + 1) + " are kept", i + 1, plan.steps().size());
            previews.add(summary(plan.preview()));
        }
        // the page is shown them all
        assertEquals(steps.length, plan.toJson().getAsJsonArray("steps").size());
        // taking out the last step keeps the others, and it may be planned again
        plan.remove(steps.length - 1);
        assertEquals(steps.length - 1, plan.steps().size());
        assertTrue(plan.add(steps[steps.length - 1].apply(plan)));
        assertEquals(steps.length, plan.steps().size());
        assertEquals(previews.get(steps.length - 1), summary(plan.preview()));
        // the game itself is untouched by planning
        String before = summary(state);
        assertEquals(0, turnStep(state));

        browser.send(planner, plan.decision(), plan.steps());
        // the game thread, waiting for the page, would have taken the wake-up
        ac.reset();
        int actionsPerTurn = ((PandemicParameters) state.getGameParameters()).getnActionsPerTurn();
        for (int i = 0; i < steps.length; i++) {
            AbstractGameState observation = state.copy(BROWSER);
            AbstractAction a = browser.fromPlan(observation, fm.computeAvailableActions(observation));
            assertNotNull("no action from the plan for step " + (i + 1), a);
            fm.next(state, a);
            // once the turn's last action is taken, cards are drawn and cities infected, which the plan does not show
            if (i + 1 < actionsPerTurn)
                assertEquals("the game after step " + (i + 1) + " is as planned", previews.get(i), summary(state));
        }
        assertNotEquals(before, summary(state));
        assertTrue(stopped.isEmpty());
        if (steps.length < actionsPerTurn) {
            // the next action was not planned: the player is asked for it
            AbstractGameState observation = state.copy(BROWSER);
            assertNull(browser.fromPlan(observation, fm.computeAvailableActions(observation)));
            assertTrue(stopped.isEmpty());
        }
        return plan;
    }

    // ---- the plans

    /**
     * The Researcher gives a card, drives two cities to a research station and cures there: in a game of 4 actions a
     * turn, so that the cure is the turn's last action.
     */
    @Test
    public void giveACardMoveTwiceAndCureAtTheDestination() {
        newGame("Researcher", "Medic", 4);
        holding(BROWSER, "Delhi", "London", "Paris", "Madrid", "Essen", "Milan");
        station("Montreal");
        MovePlan plan = planAndCarryOut(share(BROWSER, 1, "Delhi"), drive("Chicago"), drive("Montreal"),
                cureOf("blue"));
        assertTrue(plan.steps().get(0) instanceof ShareKnowledge);
        assertTrue(plan.steps().get(3) instanceof CureDisease);
        assertEquals("Montreal", location(state, BROWSER));
        assertTrue(hand(state, 1).contains("Delhi"));
        assertEquals(1, cure(state, "blue"));
        // the turn is over
        assertFalse(planner.plans(state.copy(BROWSER), BROWSER));
    }

    /**
     * Takes the card of the city, builds a research station there and shuttles to another: one the player holds the
     * card of, which a shuttle flight does not use (a direct flight there would).
     */
    @Test
    public void takeTheCityCardBuildThereShuttleAndTreat() {
        newGame("Scientist", "Medic");
        at(BROWSER, "Miami");
        at(1, "Miami");
        holding(1, "Miami");
        holding(BROWSER, "Paris");
        station("Paris");
        cubes("Paris", "blue", 2);
        planAndCarryOut(share(1, BROWSER, "Miami"),
                step("build in Miami", a -> a instanceof AddResearchStationWithCard b && b.getCity().equals("Miami")),
                move(MovePlayer.MoveType.ShuttleFlight, BROWSER, "Paris"),
                treat("blue", "Paris"));
        assertEquals(1, cubesIn(state, "Paris", "blue"));
        assertTrue(hasStation(state, "Miami"));
        assertEquals(List.of("Paris"), hand(state, BROWSER));
    }

    @Test
    public void charterFlightDirectFlightAndDrive() {
        newGame("Scientist", "Medic");
        holding(BROWSER, "Atlanta", "Paris");
        planAndCarryOut(move(MovePlayer.MoveType.CharterFlight, BROWSER, "Tokyo"),
                move(MovePlayer.MoveType.DirectFlight, BROWSER, "Paris"),
                drive("London"));
        assertEquals("London", location(state, BROWSER));
        assertTrue(hand(state, BROWSER).isEmpty());
    }

    @Test
    public void buildByMovingAStationWhenAllAreBuilt() {
        newGame("Scientist", "Medic");
        for (String city : new String[]{"Chicago", "Montreal", "New York", "Washington", "Bogota"}) station(city);
        at(BROWSER, "Miami");
        holding(BROWSER, "Miami");
        planAndCarryOut(step("move the Bogota station to Miami", a -> a instanceof AddResearchStationWithCardFrom b
                        && b.getFromCity().equals("Bogota") && b.getCity().equals("Miami")),
                drive("Atlanta"));
        assertTrue(hasStation(state, "Miami"));
        assertFalse(hasStation(state, "Bogota"));
    }

    @Test
    public void theOperationsExpertBuildsWithoutACardAndFliesFromTheStation() {
        newGame("OperationsExpert", "Medic");
        at(BROWSER, "Chicago");
        holding(BROWSER, "Paris");
        planAndCarryOut(step("build in Chicago", a -> a.getClass() == AddResearchStation.class
                        && ((AddResearchStation) a).getCity().equals("Chicago")),
                move(MovePlayer.MoveType.OperationsExpert, BROWSER, "Lima"),
                drive("Bogota"));
        assertTrue(hasStation(state, "Chicago"));
        assertEquals("Bogota", location(state, BROWSER));
        assertTrue(hand(state, BROWSER).isEmpty());
    }

    /**
     * The Dispatcher moves the Medic to them, then on as the Medic's own move: the Medic treats a cured disease in
     * each city they are moved to.
     */
    @Test
    public void theDispatcherMovesTheMedic() {
        newGame("Dispatcher", "Medic");
        at(1, "Miami");
        cured("blue");
        cubes("Atlanta", "blue", 2);
        cubes("Chicago", "blue", 3);
        planAndCarryOut(move(MovePlayer.MoveType.Dispatcher, 1, "Atlanta"),
                move(MovePlayer.MoveType.DriveFerry, 1, "Chicago"),
                drive("Washington"));
        assertEquals("Chicago", location(state, 1));
        assertEquals(0, cubesIn(state, "Atlanta", "blue"));
        assertEquals(0, cubesIn(state, "Chicago", "blue"));
    }

    @Test
    public void theMedicTreatsAllAndClearsACuredDiseaseOnArrival() {
        newGame("Medic", "Scientist");
        cured("blue");
        cubes("Atlanta", "red", 0);
        cubes("Atlanta", "black", 0);
        cubes("Atlanta", "yellow", 3);
        cubes("Chicago", "blue", 2);
        planAndCarryOut(treat("yellow", "Atlanta"), drive("Chicago"));
        assertEquals(0, cubesIn(state, "Atlanta", "yellow"));
        assertEquals(0, cubesIn(state, "Chicago", "blue"));
    }

    @Test
    public void eventsAirliftGovernmentGrantAndOneQuietNight() {
        newGame("Scientist", "Medic");
        holding(BROWSER, "Airlift", "Government Grant", "One quiet night");
        planAndCarryOut(move(MovePlayer.MoveType.Airlift, 1, "Tokyo"),
                step("build in Lima", a -> a instanceof AddResearchStationWithCard b && b.getCity().equals("Lima")),
                step("have a quiet night", a -> a instanceof QuietNight),
                drive("Miami"));
        assertEquals("Tokyo", location(state, 1));
        assertTrue(hasStation(state, "Lima"));
        assertTrue(state.isQuietNight());
        assertTrue(hand(state, BROWSER).isEmpty());
    }

    @Test
    public void theContingencyPlannerTakesBackAnEvent() {
        newGame("ContingencyPlanner", "Medic");
        discarded("Airlift");
        planAndCarryOut(step("take back Airlift", a -> a.getClass() == DrawCard.class
                        && ((DrawCard) a).getDeckTo() == state.getComponent(plannerDeckHash).getComponentID()),
                drive("Miami"));
        assertEquals("Miami", location(state, BROWSER));
    }
}
