package games.risk;

import core.Game;
import core.actions.AbstractAction;
import games.risk.actions.*;
import games.risk.components.RiskCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static games.risk.RiskTestUtils.*;
import static games.risk.WorldMap.*;
import static org.junit.Assert.*;

/**
 * Scripted walk-through of a turn with all three expert rules on (linearTradeValues, fortifyAlongPath,
 * maxArmiesPerTerritory 12): a trade worth the linear value with the bonus cut to the room, placing only where there
 * is room, a capture, and a fortify along a held path limited by the room, then the next player's turn.
 */
public class RiskExpertFlowTest {

    static RiskParameters expertParams() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("linearTradeValues", true);
        params.setParameterValue("fortifyAlongPath", true);
        params.setParameterValue("maxArmiesPerTerritory", 12);
        return params;
    }

    @Test
    public void tradeThenPlaceUnderTheLimitThenCaptureThenFortifyAlongAPath() {
        Game game = newGame(3, 23, expertParams());
        RiskGameState s = (RiskGameState) game.getGameState();
        RiskForwardModel fm = (RiskForwardModel) game.getForwardModel();
        // player 1 everything with 1 army but Argentina (player 2) and player 0's
        // South Africa 11, Congo 1, North Africa 1, Brazil 12; 3 sets already traded
        fillBoard(s, 1);
        give(s, 2, 1, ARGENTINA);
        give(s, 0, 11, SOUTH_AFRICA);
        give(s, 0, 1, CONGO, NORTH_AFRICA);
        give(s, 0, 12, BRAZIL);
        startPlay(s, 0, RiskGamePhase.REINFORCE, 3);
        s.setNSetsTraded(3);
        // 3 Artillery; player 0 holds Brazil and South Africa, not Western Europe
        giveCards(s, 0, card(BRAZIL), card(WESTERN_EUROPE), card(SOUTH_AFRICA));
        List<RiskCard> artillery = new ArrayList<>(List.of(card(BRAZIL), card(WESTERN_EUROPE), card(SOUTH_AFRICA)));
        artillery.sort(RiskUtils.CARD_ORDER);

        // Brazil is full (no PlaceArmy) but still a bonus choice; South Africa has room 1
        assertEquals(Set.of(new TradeCards(artillery, BRAZIL), new TradeCards(artillery, SOUTH_AFRICA),
                new PlaceArmy(SOUTH_AFRICA), new PlaceArmy(CONGO), new PlaceArmy(NORTH_AFRICA)), actionSet(fm, s));
        fm.next(s, new TradeCards(artillery, SOUTH_AFRICA));
        // the fourth set, linear: 4 + (4 - 1) = 7 -> 3 + 7 = 10 to place; bonus min(2, 12 - 11) = 1
        assertEquals(10, s.getArmiesToPlace(0));
        assertEquals(12, s.getArmies(SOUTH_AFRICA));
        assertEquals(Set.of(new PlaceArmy(CONGO), new PlaceArmy(NORTH_AFRICA)), actionSet(fm, s));
        for (int i = 0; i < 10; i++) {
            assertEquals(RiskGamePhase.REINFORCE, s.getGamePhase());
            fm.next(s, new PlaceArmy(NORTH_AFRICA));
        }
        assertEquals(11, s.getArmies(NORTH_AFRICA)); // 1 + 10
        assertEquals(RiskGamePhase.ATTACK, s.getGamePhase());

        // North Africa 11 takes Egypt (1 army, 1 defending die): 6 6 6 v 1
        s.setNextRolls(6, 6, 6, 1);
        fm.next(s, new Attack(NORTH_AFRICA, EGYPT, 3));
        assertEquals(0, s.getOwner(EGYPT));
        // 3 .. min(11 - 1, room 12 - 0) = 3 .. 10
        assertEquals(8, actionSet(fm, s).size());
        fm.next(s, new MoveArmies(NORTH_AFRICA, EGYPT, 3));
        assertEquals(8, s.getArmies(NORTH_AFRICA));
        assertEquals(3, s.getArmies(EGYPT));
        fm.next(s, new EndAttack());
        assertEquals(1, s.getHand(0).getSize()); // the card for the capture
        assertEquals(RiskGamePhase.FORTIFY, s.getGamePhase());

        // all five held territories are connected (South Africa - Congo - North Africa - Brazil, North Africa - Egypt);
        // sources: 2+ armies (not Congo); destinations: room left (not South Africa or Brazil, both at 12)
        Set<AbstractAction> expected = Set.of(
                new Fortify(SOUTH_AFRICA, CONGO), new Fortify(SOUTH_AFRICA, NORTH_AFRICA), new Fortify(SOUTH_AFRICA, EGYPT),
                new Fortify(NORTH_AFRICA, CONGO), new Fortify(NORTH_AFRICA, EGYPT),
                new Fortify(BRAZIL, CONGO), new Fortify(BRAZIL, NORTH_AFRICA), new Fortify(BRAZIL, EGYPT),
                new Fortify(EGYPT, CONGO), new Fortify(EGYPT, NORTH_AFRICA),
                new EndTurn());
        assertEquals(expected, actionSet(fm, s));
        fm.next(s, new Fortify(SOUTH_AFRICA, EGYPT));
        // 1 .. min(12 - 1, room 12 - 3 = 9)
        assertEquals(9, actionSet(fm, s).size());
        assertTrue(fm.computeAvailableActions(s).contains(new MoveArmies(SOUTH_AFRICA, EGYPT, 9)));
        fm.next(s, new MoveArmies(SOUTH_AFRICA, EGYPT, 9));
        assertEquals(3, s.getArmies(SOUTH_AFRICA));
        assertEquals(12, s.getArmies(EGYPT));

        assertEquals(1, s.getCurrentPlayer());
        assertEquals(RiskGamePhase.REINFORCE, s.getGamePhase());
        // player 1: 42 - 5 - 1 = 36 territories: 36 / 3 = 12, + North America 5, Europe 5, Asia 7, Australia 2
        assertEquals(31, s.getArmiesToPlace(1));
    }
}
