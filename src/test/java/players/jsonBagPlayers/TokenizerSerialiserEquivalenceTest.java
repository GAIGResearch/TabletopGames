package players.jsonBagPlayers;

import com.google.gson.Gson;
import core.AbstractGameState;
import core.AbstractGameStateContainer;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import evaluation.containers.GameStateContainerFactory;
import games.GameType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import players.simple.RandomPlayer;
import utilities.Json;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

/**
 * JSON-Bag tokens must not depend on the JSON library. For each game with a container, a mid-game
 * container is serialised by Gson (the library the bags were built with) and by {@link Json}, in the
 * same process, and the two token maps must be identical. Comparing two serialisations of one object
 * avoids cross-run noise: component IDs and some game content vary between separately launched JVMs.
 * <p>
 * This test is the one place Gson may still be imported (see {@code JsonLibraryRulesTest}).
 */
@RunWith(Parameterized.class)
public class TokenizerSerialiserEquivalenceTest {

    /** Identity and derived values that must never become tokens; see {@link Json}. */
    static final Pattern IDENTITY_TOKEN = Pattern.compile(
            "(^|[.\\[])(componentID|ownerId|numProperties|visibilityMode|gridValues|@id)([.\\[]|$)");

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> games() {
        return Arrays.stream(GameType.values())
                .filter(GameStateContainerFactory::hasContainer)
                .map(g -> new Object[]{g})
                .toList();
    }

    private final GameType gameType;

    public TokenizerSerialiserEquivalenceTest(GameType gameType) {
        this.gameType = gameType;
    }

    private AbstractGameStateContainer midGameContainer() {
        int nPlayers = Math.max(2, gameType.getMinPlayers());
        Game game = gameType.createGameInstance(nPlayers, 42L);
        List<AbstractPlayer> players = IntStream.range(0, nPlayers).mapToObj(i -> (AbstractPlayer) new RandomPlayer()).toList();
        game.reset(players);
        AbstractGameState state = game.getGameState();
        Random rnd = new Random(7);
        for (int i = 0; i < 40 && state.isNotTerminal(); i++) {
            List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(state);
            game.getForwardModel().next(state, actions.get(rnd.nextInt(actions.size())));
        }
        return GameStateContainerFactory.createContainer(state);
    }

    @Test
    public void gsonAndJacksonGiveIdenticalTokens() throws Exception {
        AbstractGameStateContainer container = midGameContainer();
        Map<String, Integer> viaGson = Tokenizer.tokenize(new Gson().toJson(container));
        Map<String, Integer> viaJackson = Tokenizer.tokenize(Json.mapper().writeValueAsString(container));
        assertFalse(viaGson.isEmpty());

        Set<String> onlyGson = new TreeSet<>(viaGson.keySet());
        onlyGson.removeAll(viaJackson.keySet());
        Set<String> onlyJackson = new TreeSet<>(viaJackson.keySet());
        onlyJackson.removeAll(viaGson.keySet());
        assertTrue("only in Gson: " + first(onlyGson) + "\nonly in Jackson: " + first(onlyJackson),
                onlyGson.isEmpty() && onlyJackson.isEmpty());
        assertEquals(viaGson, viaJackson);
    }

    @Test
    public void objectAndStringEntryPointsAgree() throws Exception {
        AbstractGameStateContainer container = midGameContainer();
        assertEquals(Tokenizer.tokenize(Json.mapper().writeValueAsString(container), "", true, "both", true,
                        false, false, false, null, false),
                Tokenizer.tokenize(container));
    }

    @Test
    public void noIdentityTokens() {
        Map<String, Integer> bag = Tokenizer.tokenize(midGameContainer());
        List<String> identity = bag.keySet().stream().filter(t -> IDENTITY_TOKEN.matcher(t).find()).sorted().toList();
        assertTrue("identity tokens: " + first(identity), identity.isEmpty());
    }

    private static String first(Collection<String> tokens) {
        return tokens.stream().limit(10).toList() + (tokens.size() > 10 ? " ... (" + tokens.size() + ")" : "");
    }
}
