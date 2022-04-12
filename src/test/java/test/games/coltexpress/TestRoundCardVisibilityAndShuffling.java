package test.games.coltexpress;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameListener;
import games.coltexpress.ColtExpressForwardModel;
import games.coltexpress.ColtExpressGame;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressParameters;
import games.coltexpress.cards.RoundCard;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static core.CoreConstants.GameEvents.ROUND_OVER;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

public class TestRoundCardVisibilityAndShuffling {

    List<AbstractPlayer> players = Arrays.asList(new RandomPlayer(),
            new RandomPlayer(),
            new RandomPlayer());

    @Test
    public void testVisibilityAndShuffleEachRound() {
        ColtExpressGame game = new ColtExpressGame(players,
                new ColtExpressForwardModel(),
                new ColtExpressGameState(new ColtExpressParameters(6), 3));

        // We now run through Game making random decision. Each time the round changes, we copy the state x10 and
        // check shuffling. The work is done in TestRoundEndListener()

        // This checks the counts of
        game.addListener(new TestRoundEndListener());
        game.run();
    }

    static class TestRoundEndListener implements IGameListener {
        @Override
        public void onGameEvent(CoreConstants.GameEvents type, Game game) {

        }

        @Override
        public void onEvent(CoreConstants.GameEvents type, AbstractGameState gameState, AbstractAction action) {
            if (type == ROUND_OVER) {
                ColtExpressGameState state = (ColtExpressGameState) gameState;
                long visibleRoundCards = state.getRounds().getVisibleComponents(0).stream().filter(Objects::nonNull).count();
                System.out.printf("End of Round: %d, Visible Cards: %d%n", state.getTurnOrder().getRoundCounter(), visibleRoundCards);
                for (int i = 0; i < state.getTurnOrder().getRoundCounter(); i++)
                    assertTrue(state.getRounds().getVisibilityForPlayer(i, 0));
                assertEquals(visibleRoundCards, state.getTurnOrder().getRoundCounter() + 1);

                // 1 card visible at end of Round 0, and so on.

                int matches = 0, nonMatches = 0;
                List<String> visibleCards = state.getRounds().getVisibleComponents(0).stream()
                        .filter(Objects::nonNull)
                        .map(RoundCard::getComponentName)
                        .collect(toList());

                for (int loop = 0; loop < 30; loop++) {
                    ColtExpressGameState copyState = (ColtExpressGameState) state.copy(0);
                    assertEquals(visibleRoundCards, copyState.getTurnOrder().getRoundCounter() + 1);

                    // this relies on the fact that the visible cards occur first, followed by the invisible ones
                    for (int i = 0; i < copyState.getRounds().getSize(); i++) {
                        for (int j = 0; j < 3; j++)
                            assertEquals(state.getRounds().getVisibilityOfComponent(i)[j], copyState.getRounds().getVisibilityOfComponent(i)[j]);
                        RoundCard roundCard = state.getRounds().get(i);
                        if (state.getRounds().isComponentVisible(i, 0)) {
                            assertEquals(roundCard, copyState.getRounds().get(i));
                        } else {
                            // We need to check Component Name, as equality also checks componentID, which will not match
                            if (roundCard.getComponentName().equals(copyState.getRounds().get(i).getComponentName()))
                                matches++; // it is possible for the card to be identical, but should be rare...we check this in aggregate later
                            else
                                nonMatches++; // the most likely outcome
                            // we can be confident that none of the visible cards should be shuffled into an invisible space
                            assertFalse(visibleCards.contains(copyState.getRounds().get(i).getComponentName()));
                        }
                    }
                }
                System.out.printf("Matches: %d of %d%n", matches, matches + nonMatches);
                if (visibleRoundCards < 5) {// on the last round we know everything
                    if (visibleRoundCards == 4) {
                        // in this case we have 1 of 3 possible cards, so expect a mean of 10/30 matches
                        assertEquals((double) matches / (matches + nonMatches), 0.333, 0.333);
                        assertTrue(matches > 0);
                    } else {
                        assertTrue(nonMatches > matches);
                        assertTrue(matches > 0); // we should have something match by chance!
                    }
                }
            }

        }
    }
}
