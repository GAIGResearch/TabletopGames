package evaluation.tournaments;

import core.AbstractPlayer;
import core.Game;
import evaluation.RunArg;
import evaluation.listeners.IGameListener;
import evaluation.metrics.Event;
import games.GameType;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class FocusPlayerByTeamTest {

    /**
     * Records the agent in each seat, and the seed, of each game.
     */
    static class SeatingListener implements IGameListener {
        final List<List<String>> seating = new ArrayList<>();
        final List<Long> seeds = new ArrayList<>();
        Game game;

        @Override
        public void onEvent(Event event) {
            if (event.type == Event.GameEvent.ABOUT_TO_START) {
                seating.add(game.getPlayers().stream().map(AbstractPlayer::toString).toList());
                seeds.add(event.state.getGameParameters().getRandomSeed());
            }
        }

        @Override
        public void report() {
        }

        @Override
        public void setGame(Game game) {
            this.game = game;
        }

        @Override
        public Game getGame() {
            return game;
        }
    }

    /**
     * A focus player is run in ONE_VS_ALL mode (RunGames sets this when focusPlayer is given). With byTeam, the focus
     * agent takes both seats of one team, half the games as team 0 and half as team 1, against the other agent in
     * both seats of the other team.
     */
    @Test
    public void focusPlayerTakesBothSeatsOfEachTeamInTurn() {
        AbstractPlayer focus = new RandomPlayer();
        focus.setName("Focus");
        AbstractPlayer other = new RandomPlayer();
        other.setName("Other");
        String[] args = new String[]{
                "mode=onevsall", "byTeam=true", "matchups=20", "seed=35830953", "listener=\"\""
        };
        Map<RunArg, Object> config = RunArg.parseConfig(args, Collections.singletonList(RunArg.Usage.RunGames));
        RoundRobinTournament tournament = new RoundRobinTournament(List.of(focus, other), GameType.Whist, 4, null, config);
        SeatingListener listener = new SeatingListener();
        tournament.addListener(listener);
        tournament.run();

        List<String> focusAsTeam0 = List.of("Focus", "Other", "Focus", "Other");
        List<String> focusAsTeam1 = List.of("Other", "Focus", "Other", "Focus");
        assertEquals(20, listener.seating.size());
        for (int g = 0; g < 10; g++) {
            assertEquals(focusAsTeam0, listener.seating.get(g));
            assertEquals(focusAsTeam1, listener.seating.get(10 + g));
        }
        // the same deals are played from each side
        assertEquals(listener.seeds.subList(0, 10), listener.seeds.subList(10, 20));
    }
}
