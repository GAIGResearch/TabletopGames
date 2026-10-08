package evaluation.listeners;

import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStatisticLogger;
import evaluation.metrics.Event;
import games.spades.SpadesForwardModel;
import games.spades.SpadesGameState;
import games.spades.SpadesParameters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * The score advantage (lead) recorded by the listeners should be relative to the best player on another team,
 * not the best other player (who may be a team-mate with the same score).
 */
public class TeamScoreAdvantageTest {

    // Spades: 4 players in 2 teams (players 0 & 2 are team 0; players 1 & 3 are team 1)
    SpadesGameState state = new SpadesGameState(new SpadesParameters(), 4);
    SpadesForwardModel fm = new SpadesForwardModel();

    @Before
    public void setup() {
        fm.setup(state);
        state.setTeamScore(0, 100);
        state.setTeamScore(1, 40);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void featureListenerScoreAdvIsRelativeToOtherTeam() {
        StateFeatureListener listener = new StateFeatureListener(mock(IStateFeatureVector.class), Event.GameEvent.ACTION_CHOSEN, false);
        IStatisticLogger logger = mock(IStatisticLogger.class);
        listener.setLogger(logger);
        Game game = mock(Game.class);
        when(game.getPlayers()).thenReturn(Arrays.asList(new AbstractPlayer[4]));
        listener.game = game;
        for (int p = 0; p < 4; p++)
            listener.currentData.add(new FeatureListener.LocalDataWrapper(p, new HashMap<>(), state, null));

        listener.writeDataWithStandardHeaders(state);

        ArgumentCaptor<Map<String, ?>> captor = ArgumentCaptor.forClass(Map.class);
        verify(logger, times(4)).record(captor.capture());
        double[] expected = {60.0, -60.0, 60.0, -60.0};
        for (Map<String, ?> data : captor.getAllValues()) {
            int player = (int) data.get("Player");
            assertEquals(expected[player], (double) data.get("ActualScoreAdv"), 1e-6);
            assertEquals(expected[player], (double) data.get("FinalScoreAdv"), 1e-6);
        }
    }

    @Test
    public void rolloutListenerLeadIsRelativeToOtherTeam() {
        // a terminal state, so the rollout ends immediately with the current scores
        state.setGameStatus(CoreConstants.GameResult.GAME_END);
        AbstractPlayer[] players = new AbstractPlayer[4];
        for (int p = 0; p < 4; p++)
            players[p] = new players.simple.RandomPlayer();
        RolloutStateFeatureListener listener = new RolloutStateFeatureListener(mock(IStateFeatureVector.class), players, fm);

        double[] lead = listener.rolloutFrom(state)[3];

        assertEquals(60.0, lead[0], 1e-6);
        assertEquals(-60.0, lead[1], 1e-6);
        assertEquals(60.0, lead[2], 1e-6);
        assertEquals(-60.0, lead[3], 1e-6);
    }

    @Test
    public void rolloutListenerColumnsMatchNames() {
        state.setGameStatus(CoreConstants.GameResult.GAME_END);
        AbstractPlayer[] players = new AbstractPlayer[4];
        for (int p = 0; p < 4; p++)
            players[p] = new players.simple.RandomPlayer();
        IStateFeatureVector phi = mock(IStateFeatureVector.class);
        when(phi.names()).thenReturn(new String[0]);
        when(phi.doubleVector(any(), anyInt())).thenReturn(new double[0]);
        when(phi.featureVector(any(), anyInt())).thenReturn(new Object[0]);
        RolloutStateFeatureListener listener = new RolloutStateFeatureListener(phi, players, fm);

        listener.preProcessing(state, null);
        List<String> names = Arrays.asList(listener.names());
        double[] doubles = listener.extractDoubleVector(null, state, 0);
        Object[] features = listener.extractFeatureVector(null, state, 0);

        assertEquals(100.0, doubles[names.indexOf("FinalScore")], 1e-6);
        assertEquals(60.0, doubles[names.indexOf("FinalScoreAdv")], 1e-6);
        assertEquals(100.0, (double) features[names.indexOf("FinalScore")], 1e-6);
        assertEquals(60.0, (double) features[names.indexOf("FinalScoreAdv")], 1e-6);
    }
}
