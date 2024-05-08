package games.dicemonastery;


import core.Game;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.GameType;
import games.dicemonastery.*;
import games.dicemonastery.DiceMonasteryConstants.ActionArea;
import games.dicemonastery.DiceMonasteryConstants.Resource;
import games.dicemonastery.DiceMonasteryConstants.Season;
import games.dicemonastery.actions.*;
import games.dicemonastery.components.*;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.*;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.BONUS_TOKEN.*;
import static games.dicemonastery.DiceMonasteryConstants.Phase.PLACE_MONKS;
import static games.dicemonastery.DiceMonasteryConstants.Phase.USE_MONKS;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
import static games.dicemonastery.DiceMonasteryConstants.Season.*;
import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;

public class ActionTests {
    DiceMonasteryForwardModel fm = new DiceMonasteryForwardModel();
    Game game = GameType.DiceMonastery.createGameInstance(4, new DiceMonasteryParams(3));
    DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
    RandomPlayer rnd = new RandomPlayer();
    List<Treasure> allTreasures = state.availableTreasures();
    Treasure cape = allTreasures.stream().filter(t -> t.getComponentName().equals("Cape"))
            .findFirst().orElseThrow( () -> new AssertionError("Cape not found"));
    Treasure robe = allTreasures.stream().filter(t -> t.getComponentName().equals("Robe"))
            .findFirst().orElseThrow( () -> new AssertionError("Robe not found"));

    private void startOfUseMonkPhaseForArea(ActionArea region, Season season, Map<Integer, ActionArea> overrides) {
        do {
            // first take random action until we get to the point required
            while (state.getGamePhase() == USE_MONKS)
                fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));

            // then place all monks randomly
            do {
                int player = state.getCurrentPlayer();
                List<AbstractAction> availableActions = fm.computeAvailableActions(state);
                AbstractAction chosen = rnd._getAction(state, fm.computeAvailableActions(state));
                if (overrides.containsKey(player) && availableActions.contains(new PlaceMonk(player, overrides.get(player)))) {
                    chosen = new PlaceMonk(player, overrides.get(player));
                }
                fm.next(state, chosen);
            } while (state.getGamePhase() == PLACE_MONKS);

            // then act randomly until we get to the point required
            while (state.getCurrentArea() != region) {
                fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
            }
        } while (state.getSeason() != season);
    }

    private void startOfUseMonkPhaseForAreaAfterBonusToken(ActionArea region, Season season) {
        startOfUseMonkPhaseForAreaAfterBonusToken(region, season, new HashMap<>());
    }

    private void startOfUseMonkPhaseForAreaAfterBonusToken(ActionArea region, Season season, Map<Integer, ActionArea> overrides) {
        startOfUseMonkPhaseForArea(region, season, overrides);

        // finally we take BONUS_TOKEN and possible PROMOTION
        assertTrue(fm.computeAvailableActions(state).get(0) instanceof TakeToken);
        fm.next(state, fm.computeAvailableActions(state).get(0)); // take one of the tokens
        if (state.isActionInProgress())
            fm.next(state, fm.computeAvailableActions(state).get(0)); // and promote a monk

        // then we decide not to Pray (if we have the option)
        if (fm.computeAvailableActions(state).stream().anyMatch(a -> a instanceof Pray))
            fm.next(state, new Pray(0)); // decline to Pray
    }

    @Test
    public void meadowActionsCorrectSpring() {
        startOfUseMonkPhaseForAreaAfterBonusToken(MEADOW, SPRING);

        int pigmentCount = 0;
        if (state.getCurrentForage().blue > 0) pigmentCount++;
        if (state.getCurrentForage().red > 0) pigmentCount++;
        if (state.getCurrentForage().green > 0) pigmentCount++;

        int expectedActions = 3 + pigmentCount;
        assertEquals(expectedActions, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
        assertTrue(fm.computeAvailableActions(state).contains(new SowWheat()));
        assertTrue(fm.computeAvailableActions(state).contains(new PlaceSkep()));

        if (state.getCurrentForage().blue > 0)
            assertTrue(fm.computeAvailableActions(state).contains(new TakePigment(PALE_BLUE_PIGMENT, 1)));
        if (state.getCurrentForage().red > 0)
            assertTrue(fm.computeAvailableActions(state).contains(new TakePigment(PALE_RED_PIGMENT, 1)));
        if (state.getCurrentForage().green > 0)
            assertTrue(fm.computeAvailableActions(state).contains(new TakePigment(PALE_GREEN_PIGMENT, 1)));
    }

    @Test
    public void noPrayerOpportunityIfNoPrayerTokens() {
        for (int p = 0; p < 4; p++)
            state.addResource(p, PRAYER, -1); // remove starting Prayers
        startOfUseMonkPhaseForArea(MEADOW, SPRING, Collections.emptyMap());
        // finally we take BONUS_TOKEN and possible PROMOTION
        state.putToken(MEADOW, PROMOTION, 0);
        state.putToken(MEADOW, PROMOTION, 1);
        assertTrue(fm.computeAvailableActions(state).get(0) instanceof TakeToken);
        fm.next(state, fm.computeAvailableActions(state).get(0)); // take one of the tokens
        if (state.isActionInProgress())
            fm.next(state, fm.computeAvailableActions(state).get(0)); // and promote a monk


        int pigmentCount = 0;
        if (state.getCurrentForage().blue > 0) pigmentCount++;
        if (state.getCurrentForage().red > 0) pigmentCount++;
        if (state.getCurrentForage().green > 0) pigmentCount++;

        // Now check we move straight on to actions
        assertEquals(0, state.getResource(state.getCurrentPlayer(), PRAYER, STOREROOM));
        assertEquals(3 + pigmentCount, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
        assertTrue(fm.computeAvailableActions(state).contains(new SowWheat()));
        assertTrue(fm.computeAvailableActions(state).contains(new PlaceSkep()));
    }

    @Test
    public void prayerOptionsIfWeHaveDevotionTokens() {
        startOfUseMonkPhaseForArea(MEADOW, SPRING, Collections.emptyMap());
        state.addResource(state.getCurrentPlayer(), PRAYER, 1); // add Prayer token

        // finally we take BONUS_TOKEN and possible PROMOTION
        state.putToken(MEADOW, PROMOTION, 0);
        state.putToken(MEADOW, PROMOTION, 1);
        assertTrue(fm.computeAvailableActions(state).get(0) instanceof TakeToken);
        fm.next(state, fm.computeAvailableActions(state).get(0)); // take one of the tokens
        if (state.isActionInProgress())
            fm.next(state, fm.computeAvailableActions(state).get(0)); // and promote a monk

        // Now check that we have an option to Pray
        assertEquals(3, fm.computeAvailableActions(state).size());
        for (int i = 0; i <= 2; i++)
            assertTrue(fm.computeAvailableActions(state).contains(new Pray(i)));
    }


    @Test
    public void prayerOptionIsSkippedInTheChapel() {
        startOfUseMonkPhaseForArea(CHAPEL, SPRING, Collections.emptyMap());
        for (int p = 0; p < 4; p++)
            state.addResource(p, PRAYER, 1); // add Prayer token, as they might have ben used already

        for (int i = 0; i < 4; i++) {
            // finally we take BONUS_TOKEN and possible PROMOTION
            if (i < 2) {
                assertTrue(fm.computeAvailableActions(state).get(0) instanceof TakeToken);
                fm.next(state, fm.computeAvailableActions(state).get(0)); // take one of the tokens
                if (state.isActionInProgress())
                    fm.next(state, fm.computeAvailableActions(state).get(0)); // and promote a monk
            }

            // Now check that we have no option to Pray
       //     System.out.println(fm.computeAvailableActions(state).stream().map(Objects::toString).collect(joining("\n")));
            assertTrue(fm.computeAvailableActions(state).stream().noneMatch(a -> a instanceof Pray));

            fm.next(state, fm.computeAvailableActions(state).get(0)); // and promote all monks
        }
    }

    @Test
    public void pray() {
        startOfUseMonkPhaseForArea(MEADOW, SPRING, Collections.emptyMap());

        // finally we take BONUS_TOKEN and possible PROMOTION
        state.putToken(MEADOW, PROMOTION, 0);
        state.putToken(MEADOW, PROMOTION, 1);
        assertTrue(fm.computeAvailableActions(state).get(0) instanceof TakeToken);
        fm.next(state, fm.computeAvailableActions(state).get(0)); // take one of the tokens
        if (state.isActionInProgress())
            fm.next(state, fm.computeAvailableActions(state).get(0)); // and promote a monk
        state.addResource(state.getCurrentPlayer(), PRAYER, 1); // add Prayer token

        assertEquals(2, state.getResource(state.getCurrentPlayer(), PRAYER, STOREROOM));
        int startingAP = state.getActionPointsLeft();
        fm.next(state, new Pray(2));
        assertEquals(0, state.getResource(state.getCurrentPlayer(), PRAYER, STOREROOM));
        assertEquals(4 + startingAP, state.getActionPointsLeft());
    }

    @Test
    public void meadowActionsCorrectAutumn() {
        startOfUseMonkPhaseForAreaAfterBonusToken(MEADOW, AUTUMN);
        while (state.getResource(state.getCurrentPlayer(), GRAIN, MEADOW) > 0)
            state.moveCube(state.getCurrentPlayer(), GRAIN, MEADOW, SUPPLY);
        while (state.getResource(state.getCurrentPlayer(), SKEP, MEADOW) > 0)
            state.moveCube(state.getCurrentPlayer(), SKEP, MEADOW, SUPPLY);


        int pigmentCount = 0;
        if (state.getCurrentForage().blue > 0) pigmentCount++;
        if (state.getCurrentForage().red > 0) pigmentCount++;
        if (state.getCurrentForage().green > 0) pigmentCount++;

        int expectedActions = 1 + pigmentCount;

        assertEquals(expectedActions, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));

        int ap = state.getActionPointsLeft();

        state.moveCube(state.getCurrentPlayer(), GRAIN, SUPPLY, MEADOW);

        assertEquals(2 + pigmentCount, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
        assertTrue(fm.computeAvailableActions(state).contains(new HarvestWheat(1)));

        state.moveCube(state.getCurrentPlayer(), SKEP, SUPPLY, MEADOW);
        assertEquals(3 + pigmentCount, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
        assertTrue(fm.computeAvailableActions(state).contains(new HarvestWheat(1)));
        assertTrue(fm.computeAvailableActions(state).contains(new CollectSkep(1)));

        state.moveCube(state.getCurrentPlayer(), GRAIN, SUPPLY, MEADOW);
        for (int i = 0; i < 10; i++) {
            state.moveCube(state.getCurrentPlayer(), SKEP, SUPPLY, MEADOW);
            assertEquals(5 + pigmentCount, fm.computeAvailableActions(state).size());
            assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
            assertTrue(fm.computeAvailableActions(state).contains(new HarvestWheat(1)));
            assertTrue(fm.computeAvailableActions(state).contains(new CollectSkep(1)));
            assertTrue(fm.computeAvailableActions(state).contains(new HarvestWheat(2)));
            assertTrue(fm.computeAvailableActions(state).contains(new CollectSkep(Math.min(ap, i + 2))));
        }

        if (state.getCurrentForage().blue > 0)
            assertTrue(fm.computeAvailableActions(state).contains(new TakePigment(PALE_BLUE_PIGMENT, 1)));
        if (state.getCurrentForage().red > 0)
            assertTrue(fm.computeAvailableActions(state).contains(new TakePigment(PALE_RED_PIGMENT, 1)));
        if (state.getCurrentForage().green > 0)
            assertTrue(fm.computeAvailableActions(state).contains(new TakePigment(PALE_GREEN_PIGMENT, 1)));
    }

    @Test
    public void takePigment() {
        startOfUseMonkPhaseForAreaAfterBonusToken(MEADOW, SPRING);

        ForageCard card = state.getCurrentForage();

        TakePigment takeAction = card.blue > 0 ? new TakePigment(PALE_BLUE_PIGMENT, 1) : new TakePigment(PALE_GREEN_PIGMENT, 1);
        // with 4 players, we must have at least one of these
        assertEquals(card.blue, state.getResource(-1, PALE_BLUE_PIGMENT, MEADOW));
        assertEquals(card.red, state.getResource(-1, PALE_RED_PIGMENT, MEADOW));
        assertEquals(card.green, state.getResource(-1, PALE_GREEN_PIGMENT, MEADOW));

        assertEquals(0, state.getResource(state.getCurrentPlayer(), takeAction.pigment, STOREROOM));
        fm.next(state, takeAction);
        assertEquals(1, state.getResource(state.getCurrentPlayer(), takeAction.pigment, STOREROOM));
        if (fm.computeAvailableActions(state).contains(takeAction))
            fm.next(state, takeAction); // and we will have one or two of these

        assertFalse(fm.computeAvailableActions(state).contains(takeAction));
        assertEquals(0, state.getResource(-1, takeAction.pigment, MEADOW));

    }


    @Test
    public void sowWheat() {
        assertEquals(0, state.getResource(state.getCurrentPlayer(), GRAIN, MEADOW));
        assertEquals(2, state.getResource(state.getCurrentPlayer(), GRAIN, STOREROOM));
        state.useAP(-1);
        (new SowWheat()).execute(state);
        assertEquals(1, state.getResource(state.getCurrentPlayer(), GRAIN, MEADOW));
        assertEquals(2, state.getResource(state.getCurrentPlayer(), GRAIN, STOREROOM));
    }

    @Test
    public void harvestGrain() {
        state.moveCube(state.getCurrentPlayer(), GRAIN, SUPPLY, MEADOW);
        assertEquals(1, state.getResource(state.getCurrentPlayer(), GRAIN, MEADOW));
        assertEquals(2, state.getResource(state.getCurrentPlayer(), GRAIN, STOREROOM));
        state.useAP(-1);
        (new HarvestWheat(1)).execute(state);
        assertEquals(0, state.getResource(state.getCurrentPlayer(), GRAIN, MEADOW));
        assertEquals(4, state.getResource(state.getCurrentPlayer(), GRAIN, STOREROOM));
    }

    @Test
    public void harvestAllGrain() {
        state.moveCube(state.getCurrentPlayer(), GRAIN, SUPPLY, MEADOW);
        state.moveCube(state.getCurrentPlayer(), GRAIN, SUPPLY, MEADOW);
        state.moveCube(state.getCurrentPlayer(), GRAIN, SUPPLY, MEADOW);
        assertEquals(3, state.getResource(state.getCurrentPlayer(), GRAIN, MEADOW));
        assertEquals(2, state.getResource(state.getCurrentPlayer(), GRAIN, STOREROOM));
        state.useAP(-3);
        (new HarvestWheat(3)).execute(state);
        assertEquals(0, state.getResource(state.getCurrentPlayer(), GRAIN, MEADOW));
        assertEquals(8, state.getResource(state.getCurrentPlayer(), GRAIN, STOREROOM));
    }


    @Test
    public void placeSkep() {
        assertEquals(0, state.getResource(state.getCurrentPlayer(), SKEP, MEADOW));
        assertEquals(2, state.getResource(state.getCurrentPlayer(), SKEP, STOREROOM));
        state.useAP(-1);
        (new PlaceSkep()).execute(state);
        assertEquals(1, state.getResource(state.getCurrentPlayer(), SKEP, MEADOW));
        assertEquals(1, state.getResource(state.getCurrentPlayer(), SKEP, STOREROOM));
    }

    @Test
    public void collectSkep() {
        state.useAP(-4);
        (new PlaceSkep()).execute(state);
        (new PlaceSkep()).execute(state);
        assertEquals(2, state.getResource(state.getCurrentPlayer(), SKEP, MEADOW));
        assertEquals(0, state.getResource(state.getCurrentPlayer(), SKEP, STOREROOM));
        assertEquals(2, state.getResource(state.getCurrentPlayer(), HONEY, STOREROOM));
        assertEquals(2, state.getResource(state.getCurrentPlayer(), WAX, STOREROOM));
        (new CollectSkep(2)).execute(state);
        assertEquals(0, state.getResource(state.getCurrentPlayer(), SKEP, MEADOW));
        assertEquals(0, state.getResource(state.getCurrentPlayer(), SKEP, STOREROOM));
        assertEquals(4, state.getResource(state.getCurrentPlayer(), HONEY, STOREROOM));
        assertEquals(4, state.getResource(state.getCurrentPlayer(), WAX, STOREROOM));
    }


    @Test
    public void kitchenActionsCorrect() {
        startOfUseMonkPhaseForAreaAfterBonusToken(KITCHEN, SPRING);

        while (state.getResource(state.getCurrentPlayer(), GRAIN, STOREROOM) > 0)
            state.moveCube(state.getCurrentPlayer(), GRAIN, STOREROOM, SUPPLY);
        while (state.getResource(state.getCurrentPlayer(), HONEY, STOREROOM) > 0)
            state.moveCube(state.getCurrentPlayer(), HONEY, STOREROOM, SUPPLY);
        Set<Resource> allPigments = state.getStores(state.getCurrentPlayer(), r -> r.isPigment).keySet();
        for (Resource r : allPigments)
            state.addResource(state.getCurrentPlayer(), r, -state.getResource(state.getCurrentPlayer(), r, STOREROOM));
        assertEquals(1, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));

        state.useAP(state.getActionPointsLeft() - 1);
        state.moveCube(state.getCurrentPlayer(), GRAIN, SUPPLY, STOREROOM);
        assertEquals(2, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
        assertTrue(fm.computeAvailableActions(state).contains(new BakeBread(1)));

        state.useAP(-1);
        assertEquals(3, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
        assertTrue(fm.computeAvailableActions(state).contains(new BakeBread(1)));
        assertTrue(fm.computeAvailableActions(state).contains(new BrewBeer(2)));

        state.moveCube(state.getCurrentPlayer(), HONEY, SUPPLY, STOREROOM);
        assertEquals(4, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new BrewMead(2)));

        state.moveCube(state.getCurrentPlayer(), PALE_BLUE_PIGMENT, SUPPLY, STOREROOM);
        assertEquals(5, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new PrepareInk(PALE_BLUE_PIGMENT, 1)));

        state.moveCube(state.getCurrentPlayer(), PALE_RED_PIGMENT, SUPPLY, STOREROOM);
        state.moveCube(state.getCurrentPlayer(), PALE_BLUE_PIGMENT, SUPPLY, STOREROOM);
        state.moveCube(state.getCurrentPlayer(), PALE_GREEN_PIGMENT, SUPPLY, STOREROOM);
        state.moveCube(state.getCurrentPlayer(), VIVID_BLUE_PIGMENT, SUPPLY, STOREROOM);
        state.moveCube(state.getCurrentPlayer(), VIVID_GREEN_PIGMENT, SUPPLY, STOREROOM);
        state.moveCube(state.getCurrentPlayer(), VIVID_PURPLE_PIGMENT, SUPPLY, STOREROOM);
        assertEquals(7, fm.computeAvailableActions(state).size());

        state.useAP(1);
        assertEquals(2, fm.computeAvailableActions(state).size());
    }

    @Test
    public void promotingAMonkViaABonusIncreasesAP() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        state.putToken(MEADOW, PROMOTION, 0);
        startOfUseMonkPhaseForArea(MEADOW, SPRING, Collections.emptyMap());
        int startingPiety = state.monksIn(MEADOW, state.getCurrentPlayer()).stream().mapToInt(Monk::getPiety).sum();
        fm.next(state, new TakeToken(PROMOTION, MEADOW, state.getCurrentPlayer()));
        fm.next(state, fm.computeAvailableActions(state).get(0)); // promote a random monk

        assertEquals(1 + startingPiety, state.monksIn(MEADOW, state.getCurrentPlayer()).stream().mapToInt(Monk::getPiety).sum());
        assertEquals(1 + startingPiety, state.getActionPointsLeft());
    }

    @Test
    public void retiringAMonkViaABonusStillGivesYouTheirActionPoints() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        state.putToken(MEADOW, PROMOTION, 0);
        Monk newMonk = state.createMonk(6, 0);
        state.moveMonk(newMonk.getComponentID(), DORMITORY, MEADOW);

        Map<Integer, ActionArea> override = new HashMap<>();
        override.put(0, MEADOW);
        startOfUseMonkPhaseForArea(MEADOW, SPRING, override);
        int startingPiety = state.monksIn(MEADOW, state.getCurrentPlayer()).stream().mapToInt(Monk::getPiety).sum();
        fm.next(state, new TakeToken(PROMOTION, MEADOW, state.getCurrentPlayer()));
        fm.next(state, new PromoteMonk(6, MEADOW)); // promote the 6er

        assertEquals(startingPiety - 6, state.monksIn(MEADOW, state.getCurrentPlayer()).stream().mapToInt(Monk::getPiety).sum());
        assertEquals( startingPiety, state.getActionPointsLeft());
    }

    @Test
    public void bakeBread() {
        state.useAP(-1);
        // Has 2 Grain in STOREROOM at setup
        (new BakeBread(1)).execute(state);
        assertEquals(1, state.getResource(state.getCurrentPlayer(), GRAIN, STOREROOM));
        assertEquals(4, state.getResource(state.getCurrentPlayer(), BREAD, STOREROOM));
    }

    @Test
    public void prepareInk() {
        state.useAP(-1);
        try {
            (new PrepareInk(PALE_GREEN_PIGMENT, 2)).execute(state);
            fail("Should throw exception");
        } catch (IllegalArgumentException error) {
           // expected!
        }
        state.useAP(-1);
        state.moveCube(state.getCurrentPlayer(), PALE_GREEN_PIGMENT, SUPPLY, STOREROOM);
        (new PrepareInk(PALE_GREEN_PIGMENT, 2)).execute(state);
        assertEquals(0, state.getResource(state.getCurrentPlayer(), PALE_GREEN_PIGMENT, STOREROOM));
        assertEquals(1, state.getResource(state.getCurrentPlayer(), PALE_GREEN_INK, STOREROOM));
    }

    @Test
    public void brewBeer() {
        state.useAP(-2);
        // Has 2 Grain in STOREROOM at setup
        (new BrewBeer(2)).execute(state);
        assertEquals(1, state.getResource(state.getCurrentPlayer(), GRAIN, STOREROOM));
        assertEquals(1, state.getResource(state.getCurrentPlayer(), PROTO_BEER_1, STOREROOM));
        assertEquals(0, state.getResource(state.getCurrentPlayer(), PROTO_BEER_2, STOREROOM));
        assertEquals(0, state.getResource(state.getCurrentPlayer(), BEER, STOREROOM));

    }

    @Test
    public void brewMead() {
        state.useAP(-2);
        // Has 2 Honey in STOREROOM at setup
        (new BrewMead(2)).execute(state);
        assertEquals(1, state.getResource(state.getCurrentPlayer(), HONEY, STOREROOM));
        assertEquals(1, state.getResource(state.getCurrentPlayer(), PROTO_MEAD_1, STOREROOM));
        assertEquals(0, state.getResource(state.getCurrentPlayer(), PROTO_MEAD_2, STOREROOM));
        assertEquals(0, state.getResource(state.getCurrentPlayer(), MEAD, STOREROOM));
    }


    @Test
    public void workshopActionsCorrect() {
        startOfUseMonkPhaseForAreaAfterBonusToken(WORKSHOP, SPRING);

        state.useAP(state.getActionPointsLeft() - 1);

        Set<Resource> allPigments = state.getStores(state.getCurrentPlayer(), r -> r.isPigment).keySet();
        for (Resource r : allPigments)
            state.addResource(state.getCurrentPlayer(), r, -state.getResource(state.getCurrentPlayer(), r, STOREROOM));
        while (state.getResource(state.getCurrentPlayer(), WAX, STOREROOM) > 0)
            state.moveCube(state.getCurrentPlayer(), WAX, STOREROOM, SUPPLY);
        assertEquals(2, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
        assertTrue(fm.computeAvailableActions(state).contains(new WeaveSkep()));

        state.moveCube(state.getCurrentPlayer(), PALE_RED_PIGMENT, SUPPLY, STOREROOM);
        assertEquals(2, fm.computeAvailableActions(state).size());

        state.useAP(-1);
        assertEquals(2, fm.computeAvailableActions(state).size());

        state.moveCube(state.getCurrentPlayer(), VIVID_BLUE_PIGMENT, SUPPLY, STOREROOM);
        assertEquals(3, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new PrepareInk(VIVID_BLUE_PIGMENT, 2)));

        state.moveCube(state.getCurrentPlayer(), WAX, SUPPLY, STOREROOM);
        assertEquals(4, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new MakeCandle(2)));

        state.moveCube(state.getCurrentPlayer(), CALF_SKIN, SUPPLY, STOREROOM);
        assertEquals(5, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new PrepareVellum(2)));

        state.moveCube(state.getCurrentPlayer(), VIVID_RED_PIGMENT, SUPPLY, STOREROOM);
        state.moveCube(state.getCurrentPlayer(), VIVID_PURPLE_PIGMENT, SUPPLY, STOREROOM);
        state.moveCube(state.getCurrentPlayer(), VIVID_GREEN_PIGMENT, SUPPLY, STOREROOM);
        assertEquals(8, fm.computeAvailableActions(state).size());

        state.useAP(1);
        assertEquals(2, fm.computeAvailableActions(state).size());
    }

    @Test
    public void weaveSkep() {
        state.useAP(-1);
        assertEquals(2, state.getResource(state.getCurrentPlayer(), SKEP, STOREROOM));
        (new WeaveSkep()).execute(state);
        assertEquals(3, state.getResource(state.getCurrentPlayer(), SKEP, STOREROOM));
    }

    @Test
    public void prepareVellum() {
        state.useAP(-2);
        assertEquals(0, state.getResource(state.getCurrentPlayer(), CALF_SKIN, STOREROOM));
        assertEquals(0, state.getResource(state.getCurrentPlayer(), VELLUM, STOREROOM));
        try {
            (new PrepareVellum(2)).execute(state);
            fail("Should not succeed");
        } catch (IllegalArgumentException e) {
            // expected
        }
        state.useAP(-2);
        state.addResource(state.getCurrentPlayer(), CALF_SKIN, 1);
        (new PrepareVellum(2)).execute(state);
        assertEquals(0, state.getResource(state.getCurrentPlayer(), CALF_SKIN, STOREROOM));
        assertEquals(1, state.getResource(state.getCurrentPlayer(), VELLUM, STOREROOM));
    }

    @Test
    public void makeCandle() {
        int player = state.getCurrentPlayer();
        state.useAP(-1);
        assertEquals(2, state.getResource(player, WAX, STOREROOM));
        assertEquals(0, state.getResource(player, CANDLE, STOREROOM));
        try {
            (new MakeCandle(2)).execute(state);
            fail("Should not succeed");
        } catch (IllegalArgumentException e) {
            // expected
        }
        assertEquals(1, state.getActionPointsLeft());
        state.useAP(-1);
        fm.next(state, (new MakeCandle(2)));
        assertEquals(1, state.getResource(player, WAX, STOREROOM));
        assertEquals(1, state.getResource(player, CANDLE, STOREROOM));
    }

    @Test
    public void gatehouseActionsCorrectWithoutPilgrimages() {
        startOfUseMonkPhaseForAreaAfterBonusToken(GATEHOUSE, SPRING);

        state.useAP(state.getActionPointsLeft() - 1);
        state.monksIn(GATEHOUSE, -1).stream()  // Move monks eligible for pilgrimage out of Gatehouse
                .filter(m -> m.getPiety() >=3 )
                .forEach( m-> state.moveMonk(m.getComponentID(), GATEHOUSE, DORMITORY));
        Monk p1 = state.createMonk(1, state.getCurrentPlayer());
        state.moveMonk(p1.getComponentID(), DORMITORY, GATEHOUSE);

        assertEquals(3, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Pass()));
        assertTrue(fm.computeAvailableActions(state).contains(new BegForAlms(1)));
        assertTrue(fm.computeAvailableActions(state).contains(new VisitMarket()));

        state.useAP(-1);
        state.addResource(state.getCurrentPlayer(), SHILLINGS, 6 - state.getResource(state.getCurrentPlayer(), SHILLINGS, STOREROOM));
        assertEquals(6, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new BuyTreasure(cape)));
        assertTrue(fm.computeAvailableActions(state).contains(new BuyTreasure(robe)));
        assertTrue(fm.computeAvailableActions(state).contains(new BegForAlms(2)));

        state.acquireTreasure(cape, state.getCurrentPlayer());
        assertEquals(5, fm.computeAvailableActions(state).size());
        assertFalse(fm.computeAvailableActions(state).contains(new BuyTreasure(cape)));

        state.addResource(state.getCurrentPlayer(), SHILLINGS, 8);
        assertEquals(7, fm.computeAvailableActions(state).size());  // One more Treasures in price range

        state.useAP(-1);
        assertEquals(8, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new HireNovice(3)));
    }

    @Test
    public void gatehousePilgrimageActionsCorrect() {
        startOfUseMonkPhaseForAreaAfterBonusToken(GATEHOUSE, SPRING);
        int player = state.getCurrentPlayer();

        state.monksIn(GATEHOUSE, -1).stream()  // Move monks eligible for pilgrimage out of Gatehouse
                .filter(m -> m.getPiety() >=3 )
                .forEach( m-> state.moveMonk(m.getComponentID(), GATEHOUSE, DORMITORY));

        // set AP to three, and remove all money
        state.useAP(state.getActionPointsLeft() - 3);
        state.addResource(player, SHILLINGS, -state.getResource(player, SHILLINGS, STOREROOM));

        assertTrue(fm.computeAvailableActions(state).stream().noneMatch(a -> a instanceof GoOnPilgrimage));

        Monk p4 = state.createMonk(4, player);
        state.moveMonk(p4.getComponentID(), DORMITORY, GATEHOUSE);
        state.addActionPoints(4);
        assertTrue(fm.computeAvailableActions(state).stream().noneMatch(a -> a instanceof GoOnPilgrimage));

        state.addResource(player, SHILLINGS, 3);
        assertEquals(1, fm.computeAvailableActions(state).stream().filter(a -> a instanceof GoOnPilgrimage).count());
        // ROME or SANTIAGO
        assertEquals(1, fm.computeAvailableActions(state).stream().filter(a -> a instanceof GoOnPilgrimage && ((GoOnPilgrimage) a).getActionPoints() == 4).count());

        state.addResource(player, SHILLINGS, 3); // can now go to JERUSALEM or ALEXANDRIA (but piety limits)
        assertEquals(1, fm.computeAvailableActions(state).stream().filter(a -> a instanceof GoOnPilgrimage).count());

        Monk p5 = state.createMonk(5, player);
        state.moveMonk(p5.getComponentID(), DORMITORY, GATEHOUSE);
        state.addActionPoints(5);
        assertEquals(3, fm.computeAvailableActions(state).stream().filter(a -> a instanceof GoOnPilgrimage).count());
        // Either can go on a short pilgrimage, but only 1 on a long one
        assertEquals(2, fm.computeAvailableActions(state).stream()
                .filter(a -> a instanceof GoOnPilgrimage && (((GoOnPilgrimage) a).destination.cost == 3)).count());
    }

    @Test
    public void pilgrimage() {
        startOfUseMonkPhaseForAreaAfterBonusToken(GATEHOUSE, SPRING);
        int player = state.getCurrentPlayer();
        Monk p5 = state.createMonk(5, player); // should be only piety 5 monk in Gatehouse
        state.moveMonk(p5.getComponentID(), DORMITORY, GATEHOUSE);
        state.addActionPoints(5);
        int ap = state.getActionPointsLeft();
        assertEquals(8, state.pilgrimagesLeft(false));
        assertEquals(8, state.pilgrimagesLeft(true));
        assertEquals(0, state.getPilgrimagesStarted().size());
        Pilgrimage next = state.peekAtNextShortPilgrimage();

        fm.next(state, new GoOnPilgrimage(next, 5));

        assertEquals(7, state.pilgrimagesLeft(false));
        assertEquals(8, state.pilgrimagesLeft(true));
        assertNotSame(next, state.peekAtNextShortPilgrimage());
        assertEquals(1, state.getPilgrimagesStarted().size());
        assertEquals(ap - 5, state.getActionPointsLeft()); // uses all AP
    }

    @Test
    public void cannotChoosePilgrimageIfNoneLeft() {
        startOfUseMonkPhaseForAreaAfterBonusToken(GATEHOUSE, SPRING);
        int player = state.getCurrentPlayer();

        state.addResource(player, SHILLINGS, 10);
        Monk p5 = state.createMonk(5, player); // should be only piety 5 monk in Gatehouse
        state.moveMonk(p5.getComponentID(), DORMITORY, GATEHOUSE);
        state.addActionPoints(5);

        for (int i = 0; i < 8; i++) {
            assertTrue(fm.computeAvailableActions(state).stream()
                    .anyMatch(a -> a instanceof GoOnPilgrimage && ((GoOnPilgrimage) a).destination.cost == 3));
            Monk pilgrim = state.createMonk(3, 0);
            state.moveMonk(pilgrim.getComponentID(), DORMITORY, GATEHOUSE);
            state.addResource(0, SHILLINGS, 3);
            state.startPilgrimage(state.peekAtNextShortPilgrimage(), pilgrim);
        }

        assertTrue(fm.computeAvailableActions(state).stream()
                .noneMatch(a -> a instanceof GoOnPilgrimage && ((GoOnPilgrimage) a).destination.cost == 3));
    }

    @Test
    public void buyTreasure() {
        int player = state.getCurrentPlayer();
        state.addActionPoints(3);
        state.addResource(player, SHILLINGS, 4);

        fm.next(state, new BuyTreasure(robe));
        assertEquals(1, state.getActionPointsLeft());
        assertEquals(5, state.getResource(player, SHILLINGS, STOREROOM));
        assertEquals(2, state.getVictoryPoints(player));
        assertEquals(1, state.getNumberCommissioned(robe));
        assertEquals(0, state.getNumberCommissioned(cape));
    }

    @Test
    public void begForAlms() {
        int player = state.getCurrentPlayer();
        state.useAP(-2);
        assertEquals(6, state.getResource(player, SHILLINGS, STOREROOM));
        fm.next(state, (new BegForAlms(2)));
        assertEquals(8, state.getResource(player, SHILLINGS, STOREROOM));
        assertEquals(0, state.getActionPointsLeft());
    }

    @Test
    public void visitMarketToBuy() {
        state.addResource(state.getCurrentPlayer(), BREAD, -2);
        state.addResource(state.getCurrentPlayer(), SHILLINGS, -3); // should leave 3 over
        VisitMarket visit = new VisitMarket();
        MarketCard market = state.getCurrentMarket();

        visit._execute(state);
        assertEquals(visit, state.currentActionInProgress());
        assertEquals(2, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Buy(GRAIN, market.grain)));
        assertTrue(fm.computeAvailableActions(state).contains(new Buy(CALF_SKIN, market.calf_skin)));

        int player = state.getCurrentPlayer();
        state.addResource(state.getCurrentPlayer(), SHILLINGS, 3);
        if (market.inkType != null) {
            assertTrue(fm.computeAvailableActions(state).contains(new Buy(market.inkType, market.inkPrice)));
            assertEquals(3, fm.computeAvailableActions(state).size());
        }
        fm.next(state, (new Buy(CALF_SKIN, market.calf_skin)));
        assertTrue(visit.executionComplete(state));
        assertEquals(1, state.getResource(player, CALF_SKIN, STOREROOM));
        assertEquals(6 - market.calf_skin, state.getResource(player, SHILLINGS, STOREROOM));
        assertFalse(state.isActionInProgress());
    }

    @Test
    public void visitMarketToSell() {
        state.addResource(state.getCurrentPlayer(), SHILLINGS, -5); // 1 left - not enough to buy anything
        state.useAP(-1);
        VisitMarket visit = new VisitMarket();
        MarketCard market = state.getCurrentMarket();
        int player = state.getCurrentPlayer();
        fm.next(state, visit);
        assertEquals(1, fm.computeAvailableActions(state).size());
        assertEquals(new DoNothing(), fm.computeAvailableActions(state).get(0));
        assertEquals(player, state.getCurrentPlayer());
        state.addResource(player, BEER, 1);
        state.addResource(player, MEAD, 1);
        assertEquals(2, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Sell(BEER, market.beer)));
        assertTrue(fm.computeAvailableActions(state).contains(new Sell(MEAD, market.mead)));
        Sell action = (Sell) fm.computeAvailableActions(state).get(1);

        state.addResource(player, CANDLE, 1);
        assertEquals(3, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new Sell(CANDLE, market.candle)));

        fm.next(state, action);
        assertTrue(visit.executionComplete(state));
        assertEquals(0, state.getResource(player, action.resource, STOREROOM));
        assertEquals(1 + action.price, state.getResource(player, SHILLINGS, STOREROOM));
        assertFalse(state.isActionInProgress());
    }

    @Test
    public void visitMarketToDoNothing() {
        state.addResource(state.getCurrentPlayer(), SHILLINGS, -5); // 1 left - not enough to buy anything
        state.addResource(state.getCurrentPlayer(), BREAD, -2);
        state.useAP(-1);

        VisitMarket visit = new VisitMarket();
        fm.next(state, visit);
        assertEquals(1, fm.computeAvailableActions(state).size());
        assertEquals(new DoNothing(), fm.computeAvailableActions(state).get(0));

        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertTrue(visit.executionComplete(state));
        assertFalse(state.isActionInProgress());
    }

    @Test
    public void hireNovice() {
        state.useAP(-1);
        HireNovice action = new HireNovice(3);
        try {
            fm.next(state, action);
            fail("Should throw exception as not enough AP");
        } catch (IllegalArgumentException e) {
            // expected
        }
        state.useAP(-2);
        fm.next(state, action);
        assertEquals(7, state.monksIn(null, 0).size());
        assertEquals(7, state.monksIn(DORMITORY, 0).size());
        assertEquals(3, state.monksIn(DORMITORY, 0).stream().filter(m -> m.getPiety() == 1).count());
        assertEquals(0, state.getResource(0, SHILLINGS, STOREROOM));
        assertEquals(0, state.getActionPointsLeft());
    }

    @Test
    public void libraryActionsCorrect() {
        Map<Integer, ActionArea> override = new HashMap<>();
        override.put(1, LIBRARY);
        startOfUseMonkPhaseForAreaAfterBonusToken(LIBRARY, SPRING, override);

        IlluminatedText psalm = state.getAvailableTexts().stream().filter(t -> t.getComponentName().equals("Psalm")).findFirst().orElseThrow(
                () -> new AssertionError("Psalm not found")
        );
        IlluminatedText epistle = state.getAvailableTexts().stream().filter(t -> t.getComponentName().equals("Epistle")).findFirst().orElseThrow(
                () -> new AssertionError("Epistle not found")
        );
        IlluminatedText liturgy = state.getAvailableTexts().stream().filter(t -> t.getComponentName().equals("Liturgy")).findFirst().orElseThrow(
                () -> new AssertionError("Liturgy not found")
        );
        IlluminatedText lukesGospel = state.getAvailableTexts().stream().filter(t -> t.getComponentName().equals("Gospel of Luke")).findFirst().orElseThrow(
                () -> new AssertionError("Liturgy not found")
        );

        state.useAP(state.getActionPointsLeft() - 1);
        assertEquals(1, state.getActionPointsLeft());
        int player = state.getCurrentPlayer();
        assertEquals(1, fm.computeAvailableActions(state).size());
        assertEquals(new Pass(), fm.computeAvailableActions(state).get(0));

        // first we clear out any items the player may have acquired to date
        state.addResource(player, VELLUM, -state.getResource(player, VELLUM, STOREROOM));
        state.addResource(player, CANDLE, -state.getResource(player, CANDLE, STOREROOM));
        for (Resource ink : Resource.values()) {
            if (ink.isInk || ink.isPigment)
                state.addResource(player, ink, -state.getResource(player, ink, STOREROOM));
        }

        state.useAP( -3);
        Monk m4 = state.createMonk(4, state.getCurrentPlayer());
        state.moveMonk(m4.getComponentID(), DORMITORY, LIBRARY);
        assertEquals(4, state.getActionPointsLeft());
        assertEquals(1, fm.computeAvailableActions(state).size());

        state.addResource(player, VELLUM, 2);
        state.addResource(player, CANDLE, 2);
        state.addResource(player, PALE_RED_INK, 3);

        assertEquals(3, fm.computeAvailableActions(state).size());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.contains(new WriteText(psalm, 4)));
        assertTrue(actions.contains(new WriteText(psalm, 3)));

        state.addResource(player, VIVID_RED_INK, 1);
        assertEquals(4, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new WriteText(epistle, 4)));

        state.addResource(player, PALE_GREEN_INK, 1);
        assertEquals(4, fm.computeAvailableActions(state).size());

        Monk m5 = state.createMonk(5, state.getCurrentPlayer());
        state.moveMonk(m5.getComponentID(), DORMITORY, LIBRARY);
        assertEquals(4, fm.computeAvailableActions(state).size());

        state.useAP(-5);
        assertEquals(7, fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).contains(new WriteText(liturgy, 5)));
        assertTrue(fm.computeAvailableActions(state).contains(new WriteText(psalm, 5)));
        assertTrue(fm.computeAvailableActions(state).contains(new WriteText(epistle, 5)));

        state.addResource(player, VIVID_BLUE_INK, 2);
        assertEquals(7, fm.computeAvailableActions(state).size());

        state.useAP(-1);
        Monk m6 = state.createMonk(6, state.getCurrentPlayer());
        state.moveMonk(m6.getComponentID(), DORMITORY, LIBRARY);
        assertTrue(fm.computeAvailableActions(state).contains(new WriteText(lukesGospel, 6)));
    }

    @Test
    public void chooseInkSequence() {
        // we will have a monk of at least level 4 in the library
        Map<Integer, ActionArea> override = new HashMap<>();
        override.put(1, LIBRARY);
        startOfUseMonkPhaseForAreaAfterBonusToken(LIBRARY, SPRING, override);

        IlluminatedText epistle = state.getAvailableTexts().stream().filter(t -> t.getComponentName().equals("Epistle")).findFirst().orElseThrow(
                () -> new AssertionError("Epistle not found")
        );

        int player = state.getCurrentPlayer();

        // we reset ink inventory (as a player may have bought one in the Market at random)
        state.addResource(player, PALE_GREEN_INK, -state.getResource(player, PALE_GREEN_INK, STOREROOM));
        state.addResource(player, PALE_GREEN_INK, 1);
        state.addResource(player, PALE_BLUE_INK, -state.getResource(player, PALE_BLUE_INK, STOREROOM));
        state.addResource(player, PALE_BLUE_INK, 2);
        state.addResource(player, PALE_RED_INK, -state.getResource(player, PALE_RED_INK, STOREROOM));
        state.addResource(player, PALE_RED_INK, 3);
        state.addResource(player, VELLUM, 3);
        state.addResource(player, CANDLE, -state.getResource(player, CANDLE, STOREROOM));
        state.addResource(player, CANDLE, 3);

        WriteText writeText = new WriteText(epistle, 4);
        fm.next(state, writeText);
        assertEquals(writeText, state.getActionsInProgress().peek());

        List<AbstractAction> availableActions = fm.computeAvailableActions(state);
        assertEquals(3, availableActions.size());
        assertTrue(availableActions.contains(new ChooseInk(PALE_BLUE_INK)));
        assertTrue(availableActions.contains(new ChooseInk(PALE_RED_INK)));
        assertTrue(availableActions.contains(new ChooseInk(PALE_GREEN_INK)));

        assertEquals(3, state.getResource(player, PALE_RED_INK, STOREROOM));

        fm.next(state, new ChooseInk(PALE_RED_INK));
        availableActions = fm.computeAvailableActions(state);
        assertEquals(2, availableActions.size());
        assertTrue(availableActions.contains(new ChooseInk(PALE_BLUE_INK)));
        assertTrue(availableActions.contains(new ChooseInk(PALE_GREEN_INK)));

        fm.next(state, new ChooseInk(PALE_GREEN_INK));
        assertFalse(state.isActionInProgress());
        assertEquals(0, state.getResource(player, PALE_GREEN_INK, STOREROOM));
        assertEquals(2, state.getResource(player, PALE_RED_INK, STOREROOM));
        assertEquals(2, state.getResource(player, PALE_BLUE_INK, STOREROOM));
        assertEquals(2, state.getResource(player, VELLUM, STOREROOM));
        assertEquals(1, state.getResource(player, CANDLE, STOREROOM));

        assertEquals(1, state.getNumberWritten(epistle));
    }

    @Test
    public void copyDuringChooseInkSequence() {
        Map<Integer, ActionArea> override = new HashMap<>();
        override.put(1, LIBRARY);
        startOfUseMonkPhaseForAreaAfterBonusToken(LIBRARY, SPRING, override);

        IlluminatedText epistle = state.getAvailableTexts().stream().filter(t -> t.getComponentName().equals("Epistle")).findFirst().orElseThrow(
                () -> new AssertionError("Epistle not found")
        );

        int player = state.getCurrentPlayer();

        state.addResource(player, PALE_GREEN_INK, 1);
        state.addResource(player, PALE_BLUE_INK, 2);
        state.addResource(player, PALE_RED_INK, 3);
        state.addResource(player, VELLUM, 3);
        state.addResource(player, CANDLE, 3);

        WriteText writeText = new WriteText(epistle, 4);

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, writeText);

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new ChooseInk(PALE_RED_INK));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());

    }

    @Test
    public void chapelActionsCorrect() {
        startOfUseMonkPhaseForAreaAfterBonusToken(CHAPEL, SPRING);

        Set<Integer> pietyOfMonks = state.monksIn(CHAPEL, state.getCurrentPlayer()).stream()
                .map(Monk::getPiety)
                .collect(toSet());

        assertTrue(pietyOfMonks.size() > 0);
        int player = state.getCurrentPlayer();

        int retired = state.monksIn(RETIRED, player).size();
        int pietyCount = state.monksIn(null, player).stream().mapToInt(Monk::getPiety).sum();

        Map<Integer, List<Monk>> startingPieties = state.monksIn(CHAPEL, player).stream()
                .collect(groupingBy(Monk::getPiety));
        assertEquals(startingPieties.keySet().size(), fm.computeAvailableActions(state).size());
        assertTrue(fm.computeAvailableActions(state).stream().allMatch(a -> a instanceof PromoteMonk));
        PromoteMonk action = (PromoteMonk) fm.computeAvailableActions(state).get(0);
        fm.next(state, action); // Promote monk
        if (action.pietyLevelToPromote == 6) {
            // retired
            assertEquals(retired + 1, state.monksIn(RETIRED, player).size());
        } else {
            assertEquals(1 + pietyCount, state.monksIn(null, player).stream().mapToInt(Monk::getPiety).sum());
        }
    }

    @Test
    public void promoteMonk() {
        startOfUseMonkPhaseForAreaAfterBonusToken(CHAPEL, SPRING);
        int player = state.getCurrentPlayer();

        int startingTotalPiety = state.monksIn(null, player).stream().mapToInt(Monk::getPiety).sum();
        List<Integer> pietyOfMonks = state.monksIn(CHAPEL, player).stream()
                .map(Monk::getPiety)
                .collect(toList());
        PromoteMonk promotion = new PromoteMonk(pietyOfMonks.get(0), CHAPEL);

        fm.next(state, promotion);
        assertEquals(1 + startingTotalPiety,
                state.monksIn(null, player).stream().mapToInt(Monk::getPiety).sum());

        fm.next(state, new Pass());
    }


    @Test
    public void bonusTokensGotToFirstTwoPlayersOnly() {
        Map<Integer, ActionArea> override = new HashMap<>();
        override.put(2, KITCHEN);
        override.put(0, KITCHEN);
        // players 0 and 2 will put all their monks in the Kitchen, and hence will go 1st and 2nd respectively (0 is abbot)
        // Their first actions will be to TakeToken - but not for players 1 and 3
        startOfUseMonkPhaseForArea(KITCHEN, SPRING, override);
        assertEquals(0, state.getCurrentPlayer());

        int player = -1;
        while (state.getCurrentArea() == KITCHEN) {
            List<AbstractAction> availableActions = fm.computeAvailableActions(state);
            if (state.getCurrentPlayer() != player) {
                player = state.getCurrentPlayer();
                // Check first action is to take a token (or not)
                if (player == 0 || player == 2) {
                    assertTrue(availableActions.stream().allMatch(a -> a instanceof TakeToken));
                } else if (state.getCurrentArea() == KITCHEN) {
                    assertTrue(availableActions.stream().noneMatch(a -> a instanceof TakeToken));
                }
            } else {
                assertTrue(availableActions.stream().noneMatch(a -> a instanceof TakeToken));
            }
            // and take action at random
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        }
    }

    @Test
    public void retiringMonksGivesVPsAndRemovesThemFromGame() {
        startOfUseMonkPhaseForAreaAfterBonusToken(KITCHEN, AUTUMN);

        int startingVP = state.getVictoryPoints(1);
        int startingMonks = state.monksIn(null, 1).size();
        List<Monk> monks = state.monksIn(null, 1).stream().filter(m -> m.getPiety() <= 6).collect(toList());
        int retiredMonks = state.monksIn(RETIRED, -1).size();
        int expectedReward = DiceMonasteryConstants.RETIREMENT_REWARDS[retiredMonks];
        promoteToRetirement(monks.get(0));
        assertEquals(startingVP + expectedReward, state.getVictoryPoints(1));
        assertEquals(startingMonks - 1, state.monksIn(null, 1).size());

        int nextExpectedReward = DiceMonasteryConstants.RETIREMENT_REWARDS[retiredMonks + 1];
        promoteToRetirement(monks.get(1));
        assertEquals(startingVP + expectedReward + nextExpectedReward, state.getVictoryPoints(1));
        assertEquals(startingMonks - 2, state.monksIn(null, 1).size());
    }

    private void promoteToRetirement(Monk m) {
        while (m.getPiety() < 6) {
            m.promote(state);
        }
        m.promote(state);
    }

    @Test
    public void endOfYearPromotion() {
        state.setFirstPlayer(1);
        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (state.getSeason() != WINTER);

        // We have now moved to Winter
        assertEquals(1, state.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.stream().allMatch(a -> a instanceof PromoteMonk));
        Set<Integer> pietyLevels = state.monksIn(null, 1).stream().mapToInt(Monk::getPiety).boxed().collect(toSet());
        assertEquals(pietyLevels.size(), actions.size());
    }

    @Test
    public void takeToken() {
        assertEquals(0, state.getCurrentPlayer());
        state.putToken(DORMITORY, DONATION, 0);
        assertEquals(6, state.getResource(1, SHILLINGS, STOREROOM));
        fm.next(state, new TakeToken(DONATION, DORMITORY, 1));
        assertEquals(9, state.getResource(1, SHILLINGS, STOREROOM));

        assertEquals(1, state.getCurrentPlayer());
        state.putToken(DORMITORY, DEVOTION, 0);
        assertEquals(1, state.getResource(1, PRAYER, STOREROOM));
        fm.next(state, new TakeToken(DEVOTION, DORMITORY, 1));
        assertEquals(2, state.getResource(1, PRAYER, STOREROOM));

        assertEquals(2, state.getCurrentPlayer());
        state.putToken(DORMITORY, PRESTIGE, 0);
        assertEquals(0, state.getVictoryPoints(2));
        fm.next(state, new TakeToken(PRESTIGE, DORMITORY, 2));
        assertEquals(1, state.getVictoryPoints(2));

        assertEquals(3, state.getCurrentPlayer());
        state.putToken(DORMITORY, PROMOTION, 0);
        assertEquals(13, state.monksIn(DORMITORY, 3).stream().mapToInt(Monk::getPiety).sum());
        fm.next(state, new TakeToken(PROMOTION, DORMITORY, 3));
        assertEquals(13, state.monksIn(DORMITORY, 3).stream().mapToInt(Monk::getPiety).sum());
        assertTrue(state.isActionInProgress());
        fm.next(state, new PromoteMonk(1, DORMITORY));
        assertFalse(state.isActionInProgress());
        assertEquals(14, state.monksIn(DORMITORY, 3).stream().mapToInt(Monk::getPiety).sum());
    }

    @Test
    public void writePsalm() {
        state.addActionPoints(10);
        IlluminatedText psalm = state.getAvailableTexts().stream().filter(t -> t.getComponentName().equals("Psalm")).findFirst().orElseThrow(
                () -> new AssertionError("Psalm not found")
        );
        WriteText action = new WriteText(psalm, 3);
        try {
            fm.next(state, action);
            fail("Should throw exception as not enough materials");
        } catch (IllegalArgumentException e) {
            // expected
        }

        int player = state.getCurrentPlayer();
        state.addResource(player, VELLUM, 2);
        state.addResource(player, CANDLE, 2);
        state.addResource(player, PALE_RED_INK, 2);

        assertEquals(0, state.getNumberWritten(psalm));
        fm.next(state, action);
        while (!action.executionComplete(state)) {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertEquals(1, state.getResource(player, VELLUM, STOREROOM));
        assertEquals(1, state.getResource(player, CANDLE, STOREROOM));
        assertEquals(1, state.getResource(player, PALE_RED_INK, STOREROOM));
        assertEquals(psalm.rewards[0], state.getVictoryPoints(player));
        assertEquals(1, state.getNumberWritten(psalm));
    }

    @Test
    public void writeGospel() {
        state.addActionPoints(10);
        IlluminatedText matthew = state.getAvailableTexts().stream().filter(t -> t.getComponentName().equals("Gospel of Matthew")).findFirst().orElseThrow(
                () -> new AssertionError("Matthew not found")
        );
        WriteText action = new WriteText(matthew, 6);

        int player = state.getCurrentPlayer();
        state.addResource(player, VELLUM, 2);
        state.addResource(player, CANDLE, 2);
        state.addResource(player, PALE_RED_INK, 2);

        assertFalse(WriteText.meetsRequirements(matthew, state.getStores(player, r -> true)));
        state.addResource(player, VIVID_PURPLE_INK, 1);
        state.addResource(player, VIVID_GREEN_INK, 1);
        assertFalse(WriteText.meetsRequirements(matthew, state.getStores(player, r -> true)));
        state.addResource(player, VIVID_PURPLE_INK, 1);
        assertTrue(WriteText.meetsRequirements(matthew, state.getStores(player, r -> true)));

        assertEquals(0, state.getNumberWritten(matthew));
        fm.next(state, action);
        while (!action.executionComplete(state)) {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertEquals(0, state.getResource(player, VELLUM, STOREROOM));
        assertEquals(0, state.getResource(player, CANDLE, STOREROOM));
        assertEquals(1, state.getResource(player, PALE_RED_INK, STOREROOM));
        assertEquals(0, state.getResource(player, VIVID_PURPLE_INK, STOREROOM));
        assertEquals(0, state.getResource(player, VIVID_GREEN_INK, STOREROOM));
        assertEquals(matthew.rewards[0] + 5, state.getVictoryPoints(player));
        assertEquals(1, state.getNumberWritten(matthew));
    }

    @Test
    public void cannotWriteTextIfAllWritten() {
        state.addActionPoints(12);
        IlluminatedText matthew = state.getAvailableTexts().stream().filter(t -> t.getComponentName().equals("Gospel of Matthew")).findFirst().orElseThrow(
                () -> new AssertionError("Matthew not found")
        );
        WriteText action = new WriteText(matthew, 6);
        int player = state.getCurrentPlayer();
        state.addResource(player, VELLUM, 4);
        state.addResource(player, CANDLE, 4);
        state.addResource(player, PALE_RED_INK, 4);
        state.addResource(player, VIVID_PURPLE_INK, 4);
        state.addResource(player, VIVID_GREEN_INK, 4);
        fm.next(state, action);
        while (!action.executionComplete(state)) {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertEquals(1, state.getNumberWritten(matthew));
        assertTrue(WriteText.meetsRequirements(matthew, state.getStores(player, r -> true)));

        try {
            fm.next(state, action);
            fail("Should throw exception as already written");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
