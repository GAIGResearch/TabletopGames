package evaluation.listeners;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.AbstractGameState;
import core.AbstractGameStateContainer;
import core.Game;
import evaluation.containers.GameStateContainerFactory;
import evaluation.metrics.Event;
import core.interfaces.IGameEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Writes a JSON snapshot of the game state at chosen events, using the same {@link AbstractGameStateContainer}
 * form that JSON-Bag tokenises.
 * <p>
 * The directory comes from {@link #setOutputDirectory(String...)}, which the runners ({@code RunGames},
 * {@code ExpertIteration}, tournaments) call with their {@code destDir}, so a configuration only needs the
 * listener class:
 *
 * <pre>{@code
 * { "class": "evaluation.listeners.SaveStateListener" }
 * }</pre>
 *
 * Files land in {@code <destDir>/rawGameStates/} as
 * {@code <GameType>-<gameID>-<roundCounter>-<turnCounter>.json}. The folder is created when the listener is
 * given its directory, and a run refuses to start if it already exists, so a second run cannot overwrite a
 * set of states by accident; move or delete the folder to run again.
 * <p>
 * Events default to {@code TURN_OVER} plus {@code GAME_OVER}. To choose others, pass an array of event names:
 *
 * <pre>{@code
 * { "class": "evaluation.listeners.SaveStateListener", "args": [ ["ROUND_OVER", "TURN_OVER"] ] }
 * }</pre>
 *
 * {@code GAME_OVER} is always included. A game with no container registered in
 * {@link GameStateContainerFactory} fails on the first event with a message naming the game, rather than
 * silently writing nothing.
 */
public class SaveStateListener implements IGameListener {

    private static final Set<IGameEvent> DEFAULT_EVENTS =
            Collections.unmodifiableSet(EnumSet.of(Event.GameEvent.TURN_OVER, Event.GameEvent.GAME_OVER));

    private final Set<IGameEvent> events;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private Game game;
    private File outputDir;

    public SaveStateListener() {
        this.events = new LinkedHashSet<>(DEFAULT_EVENTS);
    }

    public SaveStateListener(String[] eventNames) {
        Set<IGameEvent> chosen = new LinkedHashSet<>();
        for (String name : eventNames) {
            chosen.add(Event.GameEvent.valueOf(name.trim().toUpperCase()));
        }
        chosen.add(Event.GameEvent.GAME_OVER);
        this.events = chosen;
    }

    @Override
    public boolean setOutputDirectory(String... nestedDirectories) {
        outputDir = new File(String.join(File.separator, nestedDirectories), "rawGameStates");
        if (outputDir.exists()) {
            // Deliberately fatal: a second run must not scatter new snapshots over an existing set.
            throw new RuntimeException(outputDir.getPath()
                    + " ALREADY EXIST, DELETE OR MOVE THE DIRECTORY SOMEWHERE ELSE");
        }
        if (!outputDir.mkdirs()) {
            throw new RuntimeException("CANNOT CREATE DIRECTORY FOR " + outputDir.getPath());
        }
        return true;
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public Game getGame() {
        return game;
    }

    @Override
    public void onEvent(Event event) {
        if (event == null || event.state == null || !events.contains(event.type)) {
            return;
        }
        write(event.state);
    }

    private void write(AbstractGameState state) {
        if (outputDir == null) {
            throw new IllegalStateException("SaveStateListener has no output directory. Run it through "
                    + "RunGames (which sets one from destDir), or call setOutputDirectory on it first.");
        }
        if (!GameStateContainerFactory.hasContainer(state)) {
            throw new IllegalStateException("SaveStateListener cannot save " + state.getGameType()
                    + ": no AbstractGameStateContainer is registered for it in GameStateContainerFactory.");
        }

        AbstractGameStateContainer container = GameStateContainerFactory.createContainer(state);
        String fileName = state.getGameType().name() + "-" + state.getGameID() + "-"
                + state.getRoundCounter() + "-" + state.getTurnCounter() + ".json";
        File target = new File(outputDir, fileName);
        try (FileWriter writer = new FileWriter(target)) {
            gson.toJson(container, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write game state to " + target, e);
        }
    }

    @Override
    public void report() {
        // nothing to aggregate: each snapshot is written as the game runs
    }

    @Override
    public String toString() {
        return "SaveStateListener" + Arrays.toString(events.toArray());
    }
}
