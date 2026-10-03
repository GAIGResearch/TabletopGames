package games.risk;

import core.actions.AbstractAction;
import games.risk.actions.*;
import games.risk.components.RiskCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static games.risk.RiskTestUtils.*;
import static games.risk.WorldMap.*;
import static org.junit.Assert.*;

/**
 * The expert rule maxArmiesPerTerritory (pdf p.15) in initial placement, REINFORCE, the territory bonus, FORTIFY and
 * the elimination trade. The move-in after a capture is left to the random games: the captured territory is empty,
 * so its room is the whole limit and the limit never binds there.
 * Base arrangement: 3 players, limit 12; player 1 holds everything with 1 army but Argentina (player 2, 1 army).
 */
public class RiskArmyLimitTest {

    RiskForwardModel fm = new RiskForwardModel();
    RiskGameState state;

    // 3 Infantry, in card order (Alaska 0, Venezuela 9, Peru 10)
    static final List<RiskCard> INFANTRY_SET = List.of(card(ALASKA), card(VENEZUELA), card(PERU));
    // 3 Cavalry (Greenland 2, Alberta 3, Ontario 4)
    static final List<RiskCard> CAVALRY_SET = List.of(card(GREENLAND), card(ALBERTA), card(ONTARIO));

    @Before
    public void setup() {
        state = newState(3, 7, params(12));
        arrangeBase(state);
    }

    private static RiskParameters params(int limit) {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("maxArmiesPerTerritory", limit);
        return params;
    }

    private static void arrangeBase(RiskGameState s) {
        fillBoard(s, 1);
        give(s, 2, 1, ARGENTINA);
    }

    @Test
    public void roomIsTheLimitLessTheArmies() {
        give(state, 0, 9, INDONESIA);
        give(state, 0, 12, NEW_GUINEA);
        assertEquals(3, state.getRoom(INDONESIA));   // 12 - 9
        assertEquals(0, state.getRoom(NEW_GUINEA));  // 12 - 12
        assertEquals(11, state.getRoom(ALASKA));     // 12 - 1

        RiskGameState noLimit = newState(3, 7, params(0));
        arrangeBase(noLimit);
        give(noLimit, 0, 40, INDONESIA);
        assertEquals(Integer.MAX_VALUE, noLimit.getRoom(INDONESIA));
    }

    // ---- REINFORCE ----

    @Test
    public void reinforceOffersPlaceArmyOnlyWhereThereIsRoom() {
        give(state, 0, 12, INDONESIA);
        give(state, 0, 11, NEW_GUINEA);
        give(state, 0, 5, WESTERN_AUSTRALIA);
        startPlay(state, 0, RiskGamePhase.REINFORCE, 3);
        // Indonesia is at the limit
        assertEquals(Set.of(new PlaceArmy(NEW_GUINEA), new PlaceArmy(WESTERN_AUSTRALIA)), actionSet(fm, state));
        fm.next(state, new PlaceArmy(NEW_GUINEA));
        assertEquals(12, state.getArmies(NEW_GUINEA)); // 11 + 1: now full too
        assertEquals(2, state.getArmiesToPlace(0));
        assertEquals(RiskGamePhase.REINFORCE, state.getGamePhase());
        assertEquals(Set.of(new PlaceArmy(WESTERN_AUSTRALIA)), actionSet(fm, state));
    }

    @Test
    public void reinforcementsThatFitNowhereAreLostAndAttackingBegins() {
        give(state, 0, 12, INDONESIA);
        give(state, 0, 11, NEW_GUINEA);
        startPlay(state, 0, RiskGamePhase.REINFORCE, 3);
        assertEquals(Set.of(new PlaceArmy(NEW_GUINEA)), actionSet(fm, state));
        fm.next(state, new PlaceArmy(NEW_GUINEA));
        // 3 - 1 placed = 2 left, but both territories hold 12: the 2 are lost
        assertEquals(12, state.getArmies(NEW_GUINEA));
        assertEquals(0, state.getArmiesToPlace(0));
        assertEquals(24, state.getTotalArmies(0)); // 12 + 12
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void reinforcementsAreLostAtTheStartOfATurnWhenNoTerritoryHasRoom() {
        give(state, 2, 12, ARGENTINA);
        startPlay(state, 1, RiskGamePhase.FORTIFY, 0);
        fm.next(state, new EndTurn());
        // player 2: 1 territory -> 1 / 3 = 0 -> minimum 3 reinforcements, Argentina full: all 3 lost
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(0, state.getArmiesToPlace(2));
        assertEquals(12, state.getArmies(ARGENTINA));
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        // Argentina borders Peru and Brazil: 1..3 dice each, a Blitz each, EndAttack
        Set<AbstractAction> expected = Set.of(
                new Attack(ARGENTINA, PERU, 1), new Attack(ARGENTINA, PERU, 2), new Attack(ARGENTINA, PERU, 3),
                new Attack(ARGENTINA, BRAZIL, 1), new Attack(ARGENTINA, BRAZIL, 2), new Attack(ARGENTINA, BRAZIL, 3),
                new Blitz(ARGENTINA, PERU), new Blitz(ARGENTINA, BRAZIL), new EndAttack());
        assertEquals(expected, actionSet(fm, state));
    }

    @Test
    public void aForcedTradeComesBeforeReinforcementsThatFitNowhereAreLost() {
        // player 2 holds only Argentina, full, and 5 Infantry cards: the trade is forced first
        give(state, 2, 12, ARGENTINA);
        giveCards(state, 2, card(ALASKA), card(VENEZUELA), card(PERU), card(ICELAND), card(EGYPT));
        startPlay(state, 1, RiskGamePhase.FORTIFY, 0);
        fm.next(state, new EndTurn());
        assertEquals(RiskGamePhase.REINFORCE, state.getGamePhase());
        assertEquals(3, state.getArmiesToPlace(2)); // 1 territory -> the minimum 3, not yet lost
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(10, actions.size()); // 5 Infantry: C(5, 3) = 10 sets, no territory held -> no bonus
        for (AbstractAction a : actions)
            assertTrue(a instanceof TradeCards);
        fm.next(state, new TradeCards(List.of(card(ALASKA), card(VENEZUELA), card(PERU)), null));
        // 3 + 4 (the first set in the game) = 7, nowhere to put them once the hand is below 5: all lost
        assertEquals(1, state.getNSetsTraded());
        assertEquals(2, state.getHand(2).getSize());
        assertEquals(0, state.getArmiesToPlace(2));
        assertEquals(12, state.getArmies(ARGENTINA));
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
    }

    @Test
    public void aTurnWithRoomForOnlySomeArmiesPlacesThoseAndLosesTheRest() {
        give(state, 2, 11, ARGENTINA);
        startPlay(state, 1, RiskGamePhase.FORTIFY, 0);
        fm.next(state, new EndTurn());
        // 3 reinforcements, room 12 - 11 = 1
        assertEquals(RiskGamePhase.REINFORCE, state.getGamePhase());
        assertEquals(3, state.getArmiesToPlace(2));
        assertEquals(Set.of(new PlaceArmy(ARGENTINA)), actionSet(fm, state));
        fm.next(state, new PlaceArmy(ARGENTINA));
        assertEquals(12, state.getArmies(ARGENTINA));
        assertEquals(0, state.getArmiesToPlace(2)); // 3 - 1 placed, 2 lost
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        assertEquals(2, state.getCurrentPlayer());
    }

    // ---- the territory bonus of a trade ----

    @Test
    public void theTerritoryBonusIsCutToTheRoom() {
        give(state, 0, 3, INDONESIA);
        give(state, 0, 11, ALASKA);
        startPlay(state, 0, RiskGamePhase.REINFORCE, 3);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        assertEquals(Set.of(new TradeCards(INFANTRY_SET, ALASKA), new PlaceArmy(INDONESIA), new PlaceArmy(ALASKA)),
                actionSet(fm, state));
        fm.next(state, new TradeCards(INFANTRY_SET, ALASKA));
        // bonus min(2, 12 - 11) = 1 onto Alaska, the other 1 lost; the set's 4 armies to place: 3 + 4
        assertEquals(12, state.getArmies(ALASKA));
        assertEquals(7, state.getArmiesToPlace(0));
        assertTrue(state.isTerritoryBonusTaken());
        assertEquals(RiskGamePhase.REINFORCE, state.getGamePhase());
        assertEquals(Set.of(new PlaceArmy(INDONESIA)), actionSet(fm, state));
    }

    @Test
    public void aTradeIsOfferedWithTheBonusOnAFullTerritoryAndTheWholeBonusIsLost() {
        give(state, 0, 3, INDONESIA);
        give(state, 0, 12, ALASKA);
        startPlay(state, 0, RiskGamePhase.REINFORCE, 3);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        // Alaska is full: no PlaceArmy there, but the trade is still offered, with Alaska as the only bonus choice
        assertEquals(Set.of(new TradeCards(INFANTRY_SET, ALASKA), new PlaceArmy(INDONESIA)), actionSet(fm, state));
        fm.next(state, new TradeCards(INFANTRY_SET, ALASKA));
        assertEquals(12, state.getArmies(ALASKA));  // + min(2, 0)
        assertEquals(7, state.getArmiesToPlace(0)); // 3 + 4
    }

    // ---- FORTIFY ----

    @Test
    public void fortifyOnlyToATerritoryWithRoomAndMovesAtMostTheRoom() {
        // Australia: Indonesia 10, New Guinea 12, Eastern Australia 9, Western Australia 1
        give(state, 0, 10, INDONESIA);
        give(state, 0, 12, NEW_GUINEA);
        give(state, 0, 9, EASTERN_AUSTRALIA);
        give(state, 0, 1, WESTERN_AUSTRALIA);
        startPlay(state, 0, RiskGamePhase.FORTIFY, 0);
        // New Guinea (room 0) is no destination; Western Australia (1 army) is no source
        Set<AbstractAction> expected = Set.of(
                new Fortify(INDONESIA, WESTERN_AUSTRALIA),
                new Fortify(NEW_GUINEA, INDONESIA), new Fortify(NEW_GUINEA, WESTERN_AUSTRALIA),
                new Fortify(NEW_GUINEA, EASTERN_AUSTRALIA),
                new Fortify(EASTERN_AUSTRALIA, WESTERN_AUSTRALIA),
                new EndTurn());
        assertEquals(expected, actionSet(fm, state));

        fm.next(state, new Fortify(NEW_GUINEA, INDONESIA));
        // 1 .. min(12 - 1, room 12 - 10 = 2)
        assertEquals(Set.of(new MoveArmies(NEW_GUINEA, INDONESIA, 1), new MoveArmies(NEW_GUINEA, INDONESIA, 2)),
                actionSet(fm, state));
        fm.next(state, new MoveArmies(NEW_GUINEA, INDONESIA, 2));
        assertEquals(12, state.getArmies(INDONESIA));
        assertEquals(10, state.getArmies(NEW_GUINEA));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.REINFORCE, state.getGamePhase());
    }

    // ---- PLACE_INITIAL ----

    @Test
    public void initialPlacementOffersOnlyTerritoriesWithRoom() {
        state = newState(3, 7, params(4));
        arrangeBase(state);
        give(state, 0, 4, INDONESIA);
        give(state, 0, 3, NEW_GUINEA);
        give(state, 0, 1, WESTERN_AUSTRALIA);
        state.setGamePhase(RiskGamePhase.PLACE_INITIAL);
        for (int p = 0; p < 3; p++) state.setArmiesToPlace(p, 5);
        state.setTurnOwner(0);
        assertEquals(Set.of(new PlaceArmy(NEW_GUINEA), new PlaceArmy(WESTERN_AUSTRALIA)), actionSet(fm, state));
    }

    @Test
    public void initialArmiesThatFitNowhereAreLostAndTheTurnPasses() {
        state = newState(3, 7, params(4));
        arrangeBase(state);
        give(state, 0, 4, INDONESIA);
        give(state, 0, 3, NEW_GUINEA);
        state.setGamePhase(RiskGamePhase.PLACE_INITIAL);
        state.setArmiesToPlace(0, 3);
        state.setArmiesToPlace(1, 5);
        state.setArmiesToPlace(2, 5);
        state.setTurnOwner(0);
        fm.next(state, new PlaceArmy(NEW_GUINEA));
        // New Guinea 3 + 1 = 4: both full, the other 3 - 1 = 2 lost
        assertEquals(4, state.getArmies(NEW_GUINEA));
        assertEquals(0, state.getArmiesToPlace(0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.PLACE_INITIAL, state.getGamePhase());
        assertEquals(5, state.getArmiesToPlace(1));
    }

    @Test
    public void aPlayerWithNoRoomIsSkippedInTheInitialPlacementAndPlayStartsWithArmiesLost() {
        // limit 4: player 1 holds everything at 4 but Indonesia (player 0, 2) and Argentina (player 2, 1)
        state = newState(3, 7, params(4));
        give(state, 1, 4, WorldMap.ALL);
        give(state, 0, 2, INDONESIA);
        give(state, 2, 1, ARGENTINA);
        state.setFirstPlayer(1);
        state.setGamePhase(RiskGamePhase.PLACE_INITIAL);
        state.setArmiesToPlace(0, 1);
        state.setArmiesToPlace(1, 5);
        state.setArmiesToPlace(2, 5);
        state.setTurnOwner(0);

        fm.next(state, new PlaceArmy(INDONESIA));
        assertEquals(3, state.getArmies(INDONESIA));
        // player 1 has 5 to place but nowhere with room: skipped, the 5 lost
        assertEquals(0, state.getArmiesToPlace(1));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.PLACE_INITIAL, state.getGamePhase());
        assertEquals(Set.of(new PlaceArmy(ARGENTINA)), actionSet(fm, state));

        for (int i = 0; i < 3; i++)
            fm.next(state, new PlaceArmy(ARGENTINA));
        // Argentina 1 + 3 = 4: full, the other 5 - 3 = 2 lost; nobody has armies left: play starts with player 1
        assertEquals(4, state.getArmies(ARGENTINA));
        assertEquals(0, state.getArmiesToPlace(2));
        assertEquals(1, state.getCurrentPlayer());
        // player 1: 40 territories -> 40 / 3 = 13, + North America 5, Europe 5, Africa 3, Asia 7 = 33 reinforcements,
        // but every territory of theirs holds 4: all 33 lost, straight to ATTACK
        assertEquals(0, state.getArmiesToPlace(1));
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        assertEquals(160, state.getTotalArmies(1)); // 40 x 4, unchanged
        assertEquals(0, state.getRoundCounter());
    }

    // ---- the elimination trade ----

    @Test
    public void eliminationTradeArmiesThatFitNowhereAreLostAndAttackingResumes() {
        // 4 players, limit 4: player 0 Indonesia 4 attacks Siam, player 2's only territory (1 army);
        // player 3 Argentina, player 1 the rest with 1 army; two sets already traded, so the next is the third (8)
        state = newState(4, 7, params(4));
        fillBoard(state, 1);
        give(state, 0, 4, INDONESIA);
        give(state, 2, 1, SIAM);
        give(state, 3, 1, ARGENTINA);
        startPlay(state, 0, RiskGamePhase.ATTACK, 0);
        state.setNSetsTraded(2);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        giveCards(state, 2, card(GREENLAND), card(ALBERTA), card(ONTARIO));

        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertTrue(state.isEliminated(2));
        // move 3 (the dice) .. min(4 - 1, room of Siam 4) = 3
        assertEquals(Set.of(new MoveArmies(INDONESIA, SIAM, 3)), actionSet(fm, state));
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 3)); // Indonesia 1, Siam 3

        // 6 cards: the trade is forced; no card shows a territory of player 0's, so no bonus
        assertEquals(Set.of(new TradeCards(INFANTRY_SET, null), new TradeCards(CAVALRY_SET, null)),
                actionSet(fm, state));
        fm.next(state, new TradeCards(INFANTRY_SET, null));
        assertEquals(8, state.getArmiesToPlace(0)); // the third set: 8
        // room: Indonesia 4 - 1 = 3, Siam 4 - 3 = 1
        assertEquals(Set.of(new PlaceArmy(INDONESIA), new PlaceArmy(SIAM)), actionSet(fm, state));
        fm.next(state, new PlaceArmy(SIAM));
        assertEquals(Set.of(new PlaceArmy(INDONESIA)), actionSet(fm, state));
        for (int i = 0; i < 3; i++) {
            assertTrue(state.isActionInProgress());
            fm.next(state, new PlaceArmy(INDONESIA));
        }
        // 8 - 4 placed = 4 left with both territories at 4: lost, and attacking resumes
        assertEquals(4, state.getArmies(INDONESIA));
        assertEquals(4, state.getArmies(SIAM));
        assertEquals(0, state.getArmiesToPlace(0));
        assertFalse(state.isActionInProgress());
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(3, state.getHand(0).getSize());
        assertTrue(fm.computeAvailableActions(state).contains(new EndAttack()));
    }
}
