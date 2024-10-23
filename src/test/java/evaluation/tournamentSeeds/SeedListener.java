package evaluation.tournamentSeeds;

import core.Game;
import core.interfaces.IGameEvent;
import evaluation.listeners.IGameListener;
import evaluation.metrics.Event;

import java.util.ArrayList;
import java.util.List;

public class SeedListener implements IGameListener {
    public List<Long> seeds =  new ArrayList<>();
    public List<String> firstPlayerNames = new ArrayList<>();
    private Game game;

    @Override
    public void onEvent(Event event) {
        if (event.type == Event.GameEvent.ABOUT_TO_START) {
            long seed = event.state.getGameParameters().getRandomSeed();
      //      System.out.println("Seed = " + seed);
            seeds.add(seed);
            firstPlayerNames.add(game.getPlayers().get(0).toString());
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
