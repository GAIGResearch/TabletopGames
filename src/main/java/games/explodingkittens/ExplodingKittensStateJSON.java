package games.explodingkittens;

import core.components.Deck;
import core.components.PartialObservableDeck;
import games.explodingkittens.cards.ExplodingKittensCard;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.JSONUtils;

import java.util.ArrayList;

/**
 * Serialization of {@link ExplodingKittensGameState} to and from JSON, following the same pattern as
 * {@code games.backgammon.BGStateJSON} and {@code games.connect4.Connect4StateJSON}: the shared
 * AbstractGameState fields (parameters, game phase, turn counters and - crucially for this game - the
 * stack of in-progress {@link core.interfaces.IExtendedSequence}s) are handled by
 * {@link core.AbstractGameState#abstractGameStateToJSON()} /
 * {@link core.AbstractGameState#loadAbstractGameStateFromJSON}, and this class adds the Exploding
 * Kittens specific state.
 * <p>
 * The card decks are themselves serializable ({@link Deck} / {@link PartialObservableDeck} implement
 * {@link core.interfaces.IToJSON}), so each is round-tripped via its own {@code toJSON} / {@code loadDeck},
 * preserving componentIDs, card order and (for the partially observable decks) per-player visibility.
 */
public class ExplodingKittensStateJSON {

    @SuppressWarnings("unchecked")
    public static JSONObject toJSON(ExplodingKittensGameState state) {
        JSONObject json = new JSONObject();
        json.put("class", "games.explodingkittens.ExplodingKittensGameState");

        // this includes parameters, game phase and the actionsInProgress stack (the extended sequences)
        json.put("abstractGameState", state.abstractGameStateToJSON());

        json.put("drawPile", state.drawPile.toJSON());
        json.put("discardPile", state.discardPile.toJSON());
        json.put("inPlay", state.inPlay.toJSON());

        JSONArray hands = new JSONArray();
        for (PartialObservableDeck<ExplodingKittensCard> hand : state.playerHandCards)
            hands.add(hand.toJSON());
        json.put("playerHandCards", hands);

        json.put("currentPlayerTurnsLeft", state.currentPlayerTurnsLeft);
        json.put("nextAttackLevel", state.nextAttackLevel);
        json.put("skip", state.skip);
        json.put("orderOfPlayerDeath", JSONUtils.intArrayToJSON(state.orderOfPlayerDeath));

        return json;
    }

    public static void loadFromJSON(ExplodingKittensGameState state, JSONObject json) {
        state.loadAbstractGameStateFromJSON((JSONObject) json.get("abstractGameState"));

        state.drawPile = PartialObservableDeck.loadDeck((JSONObject) json.get("drawPile"));
        state.discardPile = Deck.loadDeck((JSONObject) json.get("discardPile"));
        state.inPlay = Deck.loadDeck((JSONObject) json.get("inPlay"));

        JSONArray hands = (JSONArray) json.get("playerHandCards");
        state.playerHandCards = new ArrayList<>();
        for (Object o : hands)
            state.playerHandCards.add(PartialObservableDeck.loadDeck((JSONObject) o));

        state.currentPlayerTurnsLeft = ((Number) json.get("currentPlayerTurnsLeft")).intValue();
        state.nextAttackLevel = ((Number) json.get("nextAttackLevel")).intValue();
        state.skip = (boolean) json.get("skip");
        state.orderOfPlayerDeath = JSONUtils.intArrayFromJSON((JSONArray) json.get("orderOfPlayerDeath"));
    }
}
