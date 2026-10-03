package games.risk;

import core.actions.AbstractAction;
import games.risk.actions.*;
import games.risk.components.RiskCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.risk.RiskTestUtils.*;
import static games.risk.WorldMap.*;
import static org.junit.Assert.*;

/**
 * Trading in sets of RISK cards during REINFORCE: the TradeCards actions offered (one per distinct set, with the
 * territory bonus choices), their effect, the once-per-turn territory bonus and the forced trade at handLimit cards.
 * Base: 3 players; player 1 holds everything with 1 army, player 2 Argentina, player 0 Indonesia (3 armies);
 * player 0 is reinforcing with 3 armies to place and an empty hand.
 * Symbols: Infantry - Alaska, Venezuela, Peru; Cavalry - Greenland, Alberta, Ontario; Artillery - Northwest Territory.
 */
public class RiskCardTradeTest {

    RiskForwardModel fm = new RiskForwardModel();
    RiskGameState state;

    // 3 Infantry, in card order (Alaska 0, Venezuela 9, Peru 10)
    static final List<RiskCard> INFANTRY_SET = List.of(card(ALASKA), card(VENEZUELA), card(PERU));
    // 3 Cavalry (Greenland 2, Alberta 3, Ontario 4)
    static final List<RiskCard> CAVALRY_SET = List.of(card(GREENLAND), card(ALBERTA), card(ONTARIO));

    @Before
    public void setup() {
        state = newState(3, 7, null);
        arrange(state);
    }

    private static void arrange(RiskGameState s) {
        fillBoard(s, 1);
        give(s, 2, 1, ARGENTINA);
        give(s, 0, 3, INDONESIA);
        startPlay(s, 0, RiskGamePhase.REINFORCE, 3);
    }

    private static Set<AbstractAction> set(AbstractAction... actions) {
        return Set.of(actions);
    }

    @Test
    public void tradingMovesTheCardsToTheDiscardPileAndAddsTheFirstSetsValue() {
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        fm.next(state, new TradeCards(INFANTRY_SET, null));
        assertEquals(0, state.getHand(0).getSize());
        assertEquals(new HashSet<>(INFANTRY_SET), new HashSet<>(state.getDiscardPile().getComponents()));
        assertEquals(3, state.getDiscardPile().getSize());
        assertEquals(1, state.getNSetsTraded());
        assertEquals(7, state.getArmiesToPlace(0)); // 3 + 4 (first set)
        assertEquals(RiskGamePhase.REINFORCE, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(1, state.getArmies(ALASKA)); // player 1's territory: no bonus
        assertFalse(state.isTerritoryBonusTaken());
        assertEquals(44, allCards(state).size());
    }

    @Test
    public void theValueCountsTheSetsTradedByAnyoneSoFar() {
        // two sets already traded (by others): this is the third in the game, worth 8 though player 0's first
        state.setNSetsTraded(2);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        fm.next(state, new TradeCards(INFANTRY_SET, null));
        assertEquals(3, state.getNSetsTraded());
        assertEquals(11, state.getArmiesToPlace(0)); // 3 + 8
    }

    @Test
    public void theSeventhSetIsWorthTwenty() {
        state.setNSetsTraded(6);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        fm.next(state, new TradeCards(INFANTRY_SET, null));
        assertEquals(7, state.getNSetsTraded());
        assertEquals(23, state.getArmiesToPlace(0)); // 3 + (15 + 5)
    }

    @Test
    public void theTradeValueParametersChangeTheArmies() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("tradeValues", "3,7");
        params.setParameterValue("tradeValueIncrement", 2);
        state = newState(3, 7, params);
        arrange(state);
        state.setNSetsTraded(2);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        fm.next(state, new TradeCards(INFANTRY_SET, null));
        assertEquals(12, state.getArmiesToPlace(0)); // 3 + (7 + 2), the third set
    }

    @Test
    public void withLinearTradeValuesTheThirdSetIsWorthSix() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("linearTradeValues", true);
        state = newState(3, 7, params);
        arrange(state);
        state.setNSetsTraded(2);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        fm.next(state, new TradeCards(INFANTRY_SET, null));
        assertEquals(3, state.getNSetsTraded());
        assertEquals(9, state.getArmiesToPlace(0)); // 3 + (4 + (3 - 1)) = 3 + 6, not 3 + 8
    }

    @Test
    public void theBonusTerritoryGetsTwoArmiesAtOnce() {
        give(state, 0, 1, ALASKA);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        fm.next(state, new TradeCards(INFANTRY_SET, ALASKA));
        assertEquals(3, state.getArmies(ALASKA)); // 1 + 2, straight onto the territory
        assertEquals(7, state.getArmiesToPlace(0)); // 3 + 4: the bonus is not added to the armies to place
        assertTrue(state.isTerritoryBonusTaken());
        assertEquals(3, state.getArmies(INDONESIA));
    }

    @Test
    public void theTerritoryBonusParameterSetsTheBonusArmies() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("territoryBonus", 3);
        state = newState(3, 7, params);
        arrange(state);
        give(state, 0, 1, ALASKA);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        fm.next(state, new TradeCards(INFANTRY_SET, ALASKA));
        assertEquals(4, state.getArmies(ALASKA)); // 1 + 3
        assertEquals(7, state.getArmiesToPlace(0)); // 3 + 4
    }

    @Test
    public void withNoSetOnlyPlaceArmyIsOffered() {
        giveCards(state, 0, card(ALASKA), card(PERU), card(GREENLAND)); // I I C
        assertEquals(set(new PlaceArmy(INDONESIA)), actionSet(fm, state));
    }

    @Test
    public void aSetShowingNoTerritoryHeldIsOfferedWithoutABonus() {
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        assertEquals(set(new PlaceArmy(INDONESIA), new TradeCards(INFANTRY_SET, null)), actionSet(fm, state));
    }

    @Test
    public void aSetShowingATerritoryHeldIsOfferedOnlyWithThatBonus() {
        give(state, 0, 1, ALASKA);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        assertEquals(set(new PlaceArmy(INDONESIA), new PlaceArmy(ALASKA), new TradeCards(INFANTRY_SET, ALASKA)),
                actionSet(fm, state));
    }

    @Test
    public void twoTerritoriesHeldGiveAChoiceOfBonusTerritory() {
        give(state, 0, 1, ALASKA, PERU);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        assertEquals(set(new PlaceArmy(ALASKA), new PlaceArmy(PERU), new PlaceArmy(INDONESIA),
                        new TradeCards(INFANTRY_SET, ALASKA), new TradeCards(INFANTRY_SET, PERU)),
                actionSet(fm, state));
    }

    @Test
    public void onceTheBonusIsTakenSetsAreOfferedWithoutIt() {
        give(state, 0, 1, ALASKA);
        state.setTerritoryBonusTaken(true);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        assertEquals(set(new PlaceArmy(INDONESIA), new PlaceArmy(ALASKA), new TradeCards(INFANTRY_SET, null)),
                actionSet(fm, state));
    }

    @Test
    public void aWildCardShowsNoTerritory() {
        giveCards(state, 0, card(ALASKA), wild(), card(PERU));
        assertEquals(set(new PlaceArmy(INDONESIA), new TradeCards(List.of(card(ALASKA), card(PERU), wild()), null)),
                actionSet(fm, state));
    }

    @Test
    public void theBonusIsTakenOnlyOncePerTurn() {
        // player 0 holds Alaska and Greenland; 6 cards: the Infantry set (Alaska) and the Cavalry set (Greenland)
        give(state, 0, 1, ALASKA, GREENLAND);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU), card(GREENLAND), card(ALBERTA), card(ONTARIO));
        // 6 >= 5 cards: only trades; no Artillery, so the 2 sets of one symbol, each with its held territory
        assertEquals(set(new TradeCards(INFANTRY_SET, ALASKA), new TradeCards(CAVALRY_SET, GREENLAND)),
                actionSet(fm, state));
        fm.next(state, new TradeCards(INFANTRY_SET, ALASKA));
        assertEquals(3, state.getArmies(ALASKA)); // 1 + 2
        assertEquals(7, state.getArmiesToPlace(0)); // 3 + 4
        // 3 cards left: placing is allowed again; the Cavalry set no longer earns a bonus
        // 7 to place: placementBatch (5) at a time
        assertEquals(set(new PlaceArmy(ALASKA, 5), new PlaceArmy(GREENLAND, 5), new PlaceArmy(INDONESIA, 5),
                new TradeCards(CAVALRY_SET, null)), actionSet(fm, state));
        fm.next(state, new TradeCards(CAVALRY_SET, null));
        assertEquals(1, state.getArmies(GREENLAND));
        assertEquals(13, state.getArmiesToPlace(0)); // 7 + 6 (second set)
        assertEquals(2, state.getNSetsTraded());
        assertEquals(6, state.getDiscardPile().getSize());
    }

    @Test
    public void withFiveCardsOnlyTradesAreOfferedUntilBelowFive() {
        // Alaska, Peru (I), Greenland, Alberta (C), Northwest Territory (A), none held: the 4 one-of-each sets
        giveCards(state, 0, card(PERU), card(ALBERTA), card(ALASKA), card(NORTHWEST_TERRITORY), card(GREENLAND));
        List<RiskCard> agn = List.of(card(ALASKA), card(NORTHWEST_TERRITORY), card(GREENLAND));
        assertEquals(set(
                new TradeCards(agn, null),
                new TradeCards(List.of(card(ALASKA), card(NORTHWEST_TERRITORY), card(ALBERTA)), null),
                new TradeCards(List.of(card(NORTHWEST_TERRITORY), card(GREENLAND), card(PERU)), null),
                new TradeCards(List.of(card(NORTHWEST_TERRITORY), card(ALBERTA), card(PERU)), null)),
                actionSet(fm, state));
        fm.next(state, new TradeCards(agn, null));
        // 2 cards left (Peru, Alberta): no set, and placing again, 5 of the 7 at a time
        assertEquals(set(new PlaceArmy(INDONESIA, 5)), actionSet(fm, state));
        assertEquals(7, state.getArmiesToPlace(0)); // 3 + 4
    }

    @Test
    public void theHandLimitParameterSetsWhenTradingIsForced() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("handLimit", 6);
        state = newState(3, 7, params);
        arrange(state);
        giveCards(state, 0, card(PERU), card(ALBERTA), card(ALASKA), card(NORTHWEST_TERRITORY), card(GREENLAND));
        // 5 < 6: placing is still allowed alongside the 4 sets
        assertEquals(set(new PlaceArmy(INDONESIA),
                new TradeCards(List.of(card(ALASKA), card(NORTHWEST_TERRITORY), card(GREENLAND)), null),
                new TradeCards(List.of(card(ALASKA), card(NORTHWEST_TERRITORY), card(ALBERTA)), null),
                new TradeCards(List.of(card(NORTHWEST_TERRITORY), card(GREENLAND), card(PERU)), null),
                new TradeCards(List.of(card(NORTHWEST_TERRITORY), card(ALBERTA), card(PERU)), null)),
                actionSet(fm, state));
    }

    @Test
    public void tradesAreOfferedAfterSomeArmiesArePlaced() {
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        fm.next(state, new PlaceArmy(INDONESIA));
        fm.next(state, new PlaceArmy(INDONESIA));
        // 1 army left to place: still REINFORCE, the set still on offer
        assertEquals(set(new PlaceArmy(INDONESIA), new TradeCards(INFANTRY_SET, null)), actionSet(fm, state));
        fm.next(state, new TradeCards(INFANTRY_SET, null));
        assertEquals(5, state.getArmiesToPlace(0)); // 1 + 4
        assertEquals(RiskGamePhase.REINFORCE, state.getGamePhase());
    }

    @Test
    public void noTradesAreOfferedWhileAttacking() {
        give(state, 0, 1, ALASKA);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        startPlay(state, 0, RiskGamePhase.ATTACK, 0);
        for (AbstractAction a : fm.computeAvailableActions(state))
            assertFalse(a + " offered in ATTACK", a instanceof TradeCards);
    }
}
