package test.games.sirius;

import core.AbstractPlayer;
import core.Game;
import core.actions.*;
import core.components.PartialObservableDeck;
import games.GameType;
import games.sirius.*;
import games.sirius.actions.*;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;
import utilities.Utils;

import java.util.*;

import static games.sirius.SiriusConstants.SiriusCardType.AMMONIA;
import static games.sirius.SiriusConstants.SiriusCardType.CONTRABAND;
import static games.sirius.SiriusConstants.SiriusPhase.Draw;
import static java.util.stream.Collectors.toList;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;


public class TestGlowingContraband {


    Game game;
    SiriusGameState state;
    SiriusForwardModel fm = new SiriusForwardModel();
    List<AbstractPlayer> players = new ArrayList<>();
    SiriusParameters params = new SiriusParameters();

    @Before
    public void setup() {
        players = Arrays.asList(new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer());
        game = GameType.Sirius.createGameInstance(3, 34, params);
        game.reset(players);
        state = (SiriusGameState) game.getGameState();
    }

    @Test
    public void testInitialContrabandSetup() {
        assertEquals(params.brokenContraband, state.getDeck(CONTRABAND).stream().filter(c -> c.value == 1).count(), 2);
        assertEquals(params.contraband, state.getDeck(CONTRABAND).stream().filter(c -> c.value == 2).count(), 2);
        assertEquals(params.glowingContraband, state.getDeck(CONTRABAND).stream().filter(c -> c.value == 0).count(), 2);
    }

    @Test
    public void testSellCardsWithOneToThreeGlowingContraband() {
        for (int gc = 1; gc <= 4; gc++) {
            // execute on a copy
            SiriusGameState copyState = (SiriusGameState) state.copy();
            System.out.println(gc);
            List<SiriusCard> toSell = new ArrayList<>();
            for (int i = 0; i < gc; i++) {
                SiriusCard glowingContraband = new SiriusCard("Glowing Contraband", CONTRABAND, 0);
                copyState.addCardToHand(0, glowingContraband);
                toSell.add(glowingContraband);
            }
            SellCards action = new SellCards(toSell);
            assertEquals(CONTRABAND, action.salesType);
            assertEquals(gc > 2 ? 10 : 0, action.getTotalValue());

            fm.next(copyState, action);
            assertEquals(gc > 2 ? 10 : 0, copyState.getTrackPosition(CONTRABAND));
            assertEquals(gc > 2 ? 5 : 0, copyState.getGameScore(0), 0.01); // 3 cards, plus first medal
            int expectedCardsSold = gc > 2 ? 3 : 0;
            assertEquals(gc - expectedCardsSold, copyState.getPlayerHand(0).getSize());
        }
    }

}
