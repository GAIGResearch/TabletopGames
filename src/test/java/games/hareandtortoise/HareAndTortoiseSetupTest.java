package games.hareandtortoise;

import games.hareandtortoise.components.HareCard;
import org.junit.Test;

import java.util.EnumMap;
import java.util.Map;

import static games.hareandtortoise.components.HareCard.Type.*;
import static org.junit.Assert.*;

public class HareAndTortoiseSetupTest {

    private HareAndTortoiseGameState setUp(HareAndTortoiseParameters params, int nPlayers) {
        HareAndTortoiseGameState state = new HareAndTortoiseGameState(params, nPlayers);
        new HareAndTortoiseForwardModel().setup(state);
        return state;
    }

    @Test
    public void everyRunnerStartsAtStartWith65CarrotsAnd3Lettuces() {
        for (int nPlayers = 3; nPlayers <= 6; nPlayers++) {
            HareAndTortoiseGameState state = setUp(new HareAndTortoiseParameters(), nPlayers);
            assertEquals(0, state.getCurrentPlayer());
            for (int p = 0; p < nPlayers; p++) {
                assertEquals(0, state.getSquare(p));
                assertEquals(65, state.getCarrots(p));
                assertEquals(3, state.getLettuces(p));
                assertFalse(state.hasLettuceToChew(p));
                assertFalse(state.missesNextTurn(p));
                assertEquals(0, state.getFinishPosition(p));
            }
        }
    }

    @Test
    public void hareDeckHasTheTwelveRavensburgerCardsAllUnseen() {
        HareAndTortoiseGameState state = setUp(new HareAndTortoiseParameters(), 4);
        Map<HareCard.Type, Integer> counts = new EnumMap<>(HareCard.Type.class);
        for (HareCard c : state.getHareDeck().getComponents())
            counts.merge(c.type, 1, Integer::sum);
        Map<HareCard.Type, Integer> expected = new EnumMap<>(HareCard.Type.class);
        expected.put(FALL_BACK_ONE_POSITION, 2);
        expected.put(LAST_TURN_FREE, 2);
        expected.put(DRAW_OR_DISCARD, 2);
        expected.put(LEAP_AHEAD_ONE_POSITION, 1);
        expected.put(NEXT_CARROT_SQUARE, 1);
        expected.put(PREVIOUS_CARROT_SQUARE, 1);
        expected.put(ANOTHER_TURN, 1);
        expected.put(MISS_A_TURN, 1);
        expected.put(CHEW_A_LETTUCE, 1);
        assertEquals(expected, counts);
        assertEquals(12, state.getHareDeck().getSize());   // 2 + 2 + 2 + 6 x 1
        assertEquals(12, state.getNUnseenHareCards());
    }

    @Test
    public void startCarrotsAndStartLettucesParametersChangeTheSetup() {
        HareAndTortoiseParameters params = new HareAndTortoiseParameters();
        params.setParameterValue("startCarrots", 95);
        params.setParameterValue("startLettuces", 2);
        HareAndTortoiseGameState state = setUp(params, 3);
        for (int p = 0; p < 3; p++) {
            assertEquals(95, state.getCarrots(p));
            assertEquals(2, state.getLettuces(p));
        }
    }

    @Test
    public void moveCostFollowsTheRaceCard() {
        HareAndTortoiseParameters params = new HareAndTortoiseParameters();
        // values printed on the Race Card (RaceCard.webp)
        assertEquals(1, params.moveCost(1));
        assertEquals(3, params.moveCost(2));
        assertEquals(6, params.moveCost(3));
        assertEquals(10, params.moveCost(4));
        assertEquals(15, params.moveCost(5));
        assertEquals(36, params.moveCost(8));
        assertEquals(136, params.moveCost(16));
        assertEquals(300, params.moveCost(24));
        assertEquals(1176, params.moveCost(48));
    }
}
