package games.risk;

import core.AbstractForwardModel;
import core.Game;
import core.actions.AbstractAction;
import games.risk.actions.*;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static games.risk.WorldMap.*;
import static games.risk.RiskTestUtils.*;
import static org.junit.Assert.*;

/**
 * Reinforcements, the steps of a turn (REINFORCE, ATTACK, FORTIFY) and passing the turn.
 * Base arrangement: player 1 holds everything with 1 army, player 2 Argentina; each test gives player 0 its part.
 */
public class RiskTurnTest {

    RiskForwardModel fm = new RiskForwardModel();
    RiskGameState state;

    // 11 of Asia's 12 (not Siam), and Europe's first 3 / 6 (not Southern Europe)
    static final List<RiskTerritory> ASIA_BUT_SIAM = List.of(URAL, SIBERIA, YAKUTSK, KAMCHATKA, IRKUTSK, MONGOLIA,
            JAPAN, AFGHANISTAN, CHINA, MIDDLE_EAST, INDIA);
    static final List<RiskTerritory> EUROPE_3 = List.of(ICELAND, SCANDINAVIA, UKRAINE);
    static final List<RiskTerritory> EUROPE_6 = List.of(ICELAND, SCANDINAVIA, UKRAINE, GREAT_BRITAIN,
            NORTHERN_EUROPE, WESTERN_EUROPE);

    @Before
    public void setup() {
        state = newState(3, 7, null);
        arrangeBase(state);
    }

    private static void arrangeBase(RiskGameState s) {
        fillBoard(s, 1);
        give(s, 2, 1, ARGENTINA);
    }

    @SafeVarargs
    private static int reinforcementsFor(RiskGameState s, List<RiskTerritory>... held) {
        arrangeBase(s);
        for (List<RiskTerritory> list : held)
            give(s, 0, 1, list);
        return s.getReinforcements(0);
    }

    @Test
    public void reinforcementsAreTerritoriesDividedByThreeWithAMinimumOfThree() {
        assertEquals(3, reinforcementsFor(state, List.of(SIAM, INDONESIA)));      // 2 / 3 = 0 -> minimum 3
        assertEquals(3, reinforcementsFor(state, ASIA_BUT_SIAM.subList(0, 8)));   // 8 / 3 = 2 -> 3
        assertEquals(3, reinforcementsFor(state, ASIA_BUT_SIAM));                 // pdf: 11 -> 3
        assertEquals(4, reinforcementsFor(state, ASIA_BUT_SIAM, EUROPE_3));       // pdf: 14 -> 4
        assertEquals(5, reinforcementsFor(state, ASIA_BUT_SIAM, EUROPE_6));       // pdf: 17 -> 5
    }

    @Test
    public void reinforcementsIncludeTheBonusForEachWholeContinent() {
        // Australia + 8 others: 12 / 3 = 4, + 2
        assertEquals(6, reinforcementsFor(state, AUSTRALIA, ASIA_BUT_SIAM.subList(0, 8)));
        // South America + Africa: 10 / 3 = 3, + 2 + 3
        assertEquals(8, reinforcementsFor(state, continent(WorldContinents.SOUTH_AMERICA), continent(WorldContinents.AFRICA)));
        // Asia: 12 / 3 = 4, + 7
        assertEquals(11, reinforcementsFor(state, continent(WorldContinents.ASIA)));
        // North America: 9 / 3 = 3, + 5
        assertEquals(8, reinforcementsFor(state, continent(WorldContinents.NORTH_AMERICA)));
        // Europe: 7 / 3 = 2 -> 3, + 5
        assertEquals(8, reinforcementsFor(state, continent(WorldContinents.EUROPE)));
    }

    @Test
    public void reinforcementParametersChangeTheCount() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("minReinforcements", 5);
        assertEquals(5, reinforcementsFor(newState(3, 7, params), List.of(SIAM, INDONESIA))); // 2 / 3 = 0 -> 5

        params = new RiskParameters();
        params.setParameterValue("territoriesPerArmy", 2);
        assertEquals(5, reinforcementsFor(newState(3, 7, params), ASIA_BUT_SIAM)); // 11 / 2 = 5
        // the continent bonuses are in the map file: RiskMapTest.aGameOnAVariantMapUsesThatMap
    }

    @Test
    public void reinforceOffersPlaceArmyOnTerritoriesHeldUntilNoneAreLeftThenAttack() {
        give(state, 0, 1, AUSTRALIA);
        startPlay(state, 0, RiskGamePhase.REINFORCE, 3);
        Set<AbstractAction> expected = new HashSet<>();
        for (RiskTerritory t : AUSTRALIA) expected.add(new PlaceArmy(t));
        assertEquals(expected, actionSet(fm, state));

        fm.next(state, new PlaceArmy(INDONESIA));
        assertEquals(2, state.getArmies(INDONESIA));
        assertEquals(2, state.getArmiesToPlace(0));
        assertEquals(RiskGamePhase.REINFORCE, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());

        fm.next(state, new PlaceArmy(NEW_GUINEA));
        fm.next(state, new PlaceArmy(INDONESIA));
        assertEquals(3, state.getArmies(INDONESIA));
        assertEquals(2, state.getArmies(NEW_GUINEA));
        assertEquals(0, state.getArmiesToPlace(0));
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
    }

    private void arrangeAttackers(RiskGameState s) {
        // player 0: Indonesia 6, New Guinea 3, Western Australia 1; player 2: Eastern Australia; Siam is player 1's
        give(s, 0, 6, INDONESIA);
        give(s, 0, 3, NEW_GUINEA);
        give(s, 0, 1, WESTERN_AUSTRALIA);
        give(s, 2, 1, EASTERN_AUSTRALIA);
        startPlay(s, 0, RiskGamePhase.ATTACK, 0);
    }

    @Test
    public void attacksAreFromTerritoriesWithTwoOrMoreArmiesToAdjacentEnemiesWithUpToThreeDiceAndABlitzEach() {
        arrangeAttackers(state);
        Set<AbstractAction> expected = Set.of(
                new Attack(INDONESIA, SIAM, 1), new Attack(INDONESIA, SIAM, 2), new Attack(INDONESIA, SIAM, 3), // min(3, 6 - 1)
                new Attack(NEW_GUINEA, EASTERN_AUSTRALIA, 1), new Attack(NEW_GUINEA, EASTERN_AUSTRALIA, 2), // 3 - 1
                new Blitz(INDONESIA, SIAM), new Blitz(NEW_GUINEA, EASTERN_AUSTRALIA), // allowBlitz (default): one per pair
                new EndAttack());
        assertEquals(expected, attackOptions(fm, state));
    }

    @Test
    public void maxAttackDiceLimitsTheDice() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("maxAttackDice", 2);
        RiskGameState s = newState(3, 7, params);
        arrangeBase(s);
        arrangeAttackers(s);
        Set<AbstractAction> expected = Set.of(
                new Attack(INDONESIA, SIAM, 1), new Attack(INDONESIA, SIAM, 2),
                new Attack(NEW_GUINEA, EASTERN_AUSTRALIA, 1), new Attack(NEW_GUINEA, EASTERN_AUSTRALIA, 2),
                new Blitz(INDONESIA, SIAM), new Blitz(NEW_GUINEA, EASTERN_AUSTRALIA), // one per pair whatever the dice
                new EndAttack());
        assertEquals(expected, attackOptions(fm, s));
    }

    @Test
    public void withNoTerritoryOfTwoArmiesOnlyEndAttackIsOffered() {
        give(state, 0, 1, AUSTRALIA);
        startPlay(state, 0, RiskGamePhase.ATTACK, 0);
        assertEquals(Set.of(new EndAttack()), actionSet(fm, state));
    }

    @Test
    public void endAttackMovesToFortify() {
        arrangeAttackers(state);
        fm.next(state, new EndAttack());
        assertEquals(RiskGamePhase.FORTIFY, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void fortifyIsBetweenAdjacentTerritoriesHeldFromOneWithTwoOrMoreArmies() {
        give(state, 0, 1, AUSTRALIA);
        give(state, 0, 3, INDONESIA);
        give(state, 0, 2, NEW_GUINEA);
        give(state, 0, 5, BRAZIL); // no neighbour held
        startPlay(state, 0, RiskGamePhase.FORTIFY, 0);
        Set<AbstractAction> expected = Set.of(
                new Fortify(INDONESIA, NEW_GUINEA), new Fortify(INDONESIA, WESTERN_AUSTRALIA),
                new Fortify(NEW_GUINEA, INDONESIA), new Fortify(NEW_GUINEA, WESTERN_AUSTRALIA),
                new Fortify(NEW_GUINEA, EASTERN_AUSTRALIA),
                new EndTurn());
        assertEquals(expected, actionSet(fm, state));
    }

    // player 1 then holds 42 - 4 - 1 = 37: 37 / 3 = 12, + North America 5, Europe 5, Africa 3, Asia 7 = 32
    private static final int PLAYER_1_REINFORCEMENTS = 32;

    @Test
    public void fortifyingMovesOneOrMoreArmiesAndEndsTheTurn() {
        give(state, 0, 1, AUSTRALIA);
        give(state, 0, 4, NEW_GUINEA);
        startPlay(state, 0, RiskGamePhase.FORTIFY, 0);
        fm.next(state, new Fortify(NEW_GUINEA, EASTERN_AUSTRALIA));
        assertTrue(state.currentActionInProgress() instanceof MoveArmiesChoice);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Set.of(new MoveArmies(NEW_GUINEA, EASTERN_AUSTRALIA, 1), new MoveArmies(NEW_GUINEA, EASTERN_AUSTRALIA, 2),
                new MoveArmies(NEW_GUINEA, EASTERN_AUSTRALIA, 3)), actionSet(fm, state)); // 1 .. 4 - 1

        fm.next(state, new MoveArmies(NEW_GUINEA, EASTERN_AUSTRALIA, 1));
        assertEquals(3, state.getArmies(NEW_GUINEA));
        assertEquals(2, state.getArmies(EASTERN_AUSTRALIA));
        assertFalse(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.REINFORCE, state.getGamePhase());
        assertEquals(PLAYER_1_REINFORCEMENTS, state.getArmiesToPlace(1));
        assertEquals(0, state.getRoundCounter());
    }

    @Test
    public void fortifyingWithOneArmyThatMayMoveOffersOnlyThatOne() {
        give(state, 0, 1, AUSTRALIA);
        give(state, 0, 2, NEW_GUINEA);
        startPlay(state, 0, RiskGamePhase.FORTIFY, 0);
        fm.next(state, new Fortify(NEW_GUINEA, EASTERN_AUSTRALIA));
        assertEquals(Set.of(new MoveArmies(NEW_GUINEA, EASTERN_AUSTRALIA, 1)), actionSet(fm, state));
    }

    @Test
    public void fortifyingWithManyArmiesOffersFiveEvenlySpacedNumbers() {
        give(state, 0, 1, AUSTRALIA);
        give(state, 0, 91, NEW_GUINEA);
        startPlay(state, 0, RiskGamePhase.FORTIFY, 0);
        fm.next(state, new Fortify(NEW_GUINEA, EASTERN_AUSTRALIA));
        // 1 .. 90: 1, 1 + 89 / 4 = 23.25, 45.5, 67.75, 90, rounded
        Set<AbstractAction> expected = new HashSet<>();
        for (int n : List.of(1, 23, 46, 68, 90))
            expected.add(new MoveArmies(NEW_GUINEA, EASTERN_AUSTRALIA, n));
        assertEquals(expected, actionSet(fm, state));
    }

    @Test
    public void maxMoveChoicesSetsHowManyNumbersAreOffered() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("maxMoveChoices", 3);
        state = newState(3, 7, params);
        arrangeBase(state);
        give(state, 0, 1, AUSTRALIA);
        give(state, 0, 11, NEW_GUINEA);
        startPlay(state, 0, RiskGamePhase.FORTIFY, 0);
        fm.next(state, new Fortify(NEW_GUINEA, EASTERN_AUSTRALIA));
        assertEquals(Set.of(new MoveArmies(NEW_GUINEA, EASTERN_AUSTRALIA, 1), new MoveArmies(NEW_GUINEA, EASTERN_AUSTRALIA, 6),
                new MoveArmies(NEW_GUINEA, EASTERN_AUSTRALIA, 10)), actionSet(fm, state)); // 1 .. 10
    }

    @Test
    public void endTurnPassesToTheNextPlayerToReinforce() {
        give(state, 0, 1, AUSTRALIA);
        give(state, 0, 4, NEW_GUINEA);
        startPlay(state, 0, RiskGamePhase.FORTIFY, 0);
        fm.next(state, new EndTurn());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.REINFORCE, state.getGamePhase());
        assertEquals(PLAYER_1_REINFORCEMENTS, state.getArmiesToPlace(1));
        assertEquals(4, state.getArmies(NEW_GUINEA));
        assertEquals(0, state.getRoundCounter());
    }

    @Test
    public void roundEndsWhenTheTurnPassesBackRoundTheTable() {
        give(state, 0, 1, AUSTRALIA);
        startPlay(state, 2, RiskGamePhase.FORTIFY, 0);
        fm.next(state, new EndTurn());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(1, state.getRoundCounter());
        assertEquals(RiskGamePhase.REINFORCE, state.getGamePhase());
        assertEquals(5, state.getArmiesToPlace(0)); // Australia: 4 / 3 = 1 -> 3, + 2
    }

    @Test
    public void roundEndsWhenTheTurnPassesBackEvenIfTheFirstPlayerIsOut() {
        state.setFirstPlayer(0);
        eliminate(state, 0, 3);
        startPlay(state, 2, RiskGamePhase.FORTIFY, 0);
        fm.next(state, new EndTurn());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, state.getRoundCounter());
        // player 1 holds all but Argentina, 41: 41 / 3 = 13, + 5 + 5 + 3 + 7 + 2 (all but South America) = 35
        assertEquals(35, state.getArmiesToPlace(1));
    }

    @Test
    public void wholeTurnWalkThrough() {
        Game game = newGame(3, 21);
        RiskGameState s = (RiskGameState) game.getGameState();
        AbstractForwardModel gfm = game.getForwardModel();
        arrangeBase(s);
        give(s, 0, 1, AUSTRALIA);
        give(s, 0, 3, INDONESIA);
        startPlay(s, 0, RiskGamePhase.REINFORCE, 5);

        for (int i = 0; i < 5; i++) {
            assertEquals(RiskGamePhase.REINFORCE, s.getGamePhase());
            gfm.next(s, new PlaceArmy(INDONESIA));
        }
        assertEquals(8, s.getArmies(INDONESIA)); // 3 + 5
        assertEquals(RiskGamePhase.ATTACK, s.getGamePhase());

        // Siam has 1 army, so 1 defending die: 6 beats 3 and Siam is captured
        s.setNextRolls(6, 5, 4, 3);
        gfm.next(s, new Attack(INDONESIA, SIAM, 3));
        assertEquals(0, s.getOwner(SIAM));
        assertEquals(0, s.getCurrentPlayer());
        List<AbstractAction> moves = gfm.computeAvailableActions(s);
        assertEquals(5, moves.size()); // 3 .. 8 - 1
        gfm.next(s, new MoveArmies(INDONESIA, SIAM, 5));
        assertEquals(3, s.getArmies(INDONESIA));
        assertEquals(5, s.getArmies(SIAM));
        assertEquals(RiskGamePhase.ATTACK, s.getGamePhase());

        // China has 1 army: 1 vs 6, the attacker loses one
        s.setNextRolls(1, 1, 6);
        gfm.next(s, new Attack(SIAM, CHINA, 2));
        assertEquals(4, s.getArmies(SIAM));
        assertEquals(1, s.getOwner(CHINA));
        assertEquals(1, s.getArmies(CHINA));
        assertEquals(RiskGamePhase.ATTACK, s.getGamePhase());

        gfm.next(s, new EndAttack());
        assertEquals(RiskGamePhase.FORTIFY, s.getGamePhase());
        gfm.next(s, new Fortify(INDONESIA, NEW_GUINEA));
        gfm.next(s, new MoveArmies(INDONESIA, NEW_GUINEA, 2));
        assertEquals(1, s.getArmies(INDONESIA));
        assertEquals(3, s.getArmies(NEW_GUINEA));

        assertEquals(1, s.getCurrentPlayer());
        assertEquals(RiskGamePhase.REINFORCE, s.getGamePhase());
        // player 1: 42 - 5 (player 0) - 1 (Argentina) = 36: 36 / 3 = 12, + North America 5, Europe 5, Africa 3
        assertEquals(25, s.getArmiesToPlace(1));
    }
}
