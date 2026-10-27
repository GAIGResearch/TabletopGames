package core.components;

import core.CoreConstants;
import core.CoreConstants.VisibilityMode;
import core.interfaces.IToJSON;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Round-trip tests for {@link Deck} / {@link PartialObservableDeck} serialization. Both implement
 * {@link IToJSON}, which implicitly requires every component they hold to be serializable: it must
 * implement {@link IToJSON} <em>and</em> provide a {@link JSONObject} constructor, since loading
 * recurses into each component via {@link utilities.JSONUtils#loadClassFromJSON}. The {@link TestCard}
 * below is the minimal component that satisfies that contract.
 */
public class DeckSerializationTest {

    /** A minimal serializable component: implements IToJSON and has a JSONObject constructor that
     * restores its componentID, so deck round-trips preserve identity. */
    public static class TestCard extends Component implements IToJSON {
        TestCard(String name) {
            super(CoreConstants.ComponentType.CARD, name);
        }

        public TestCard(JSONObject json) {
            super(CoreConstants.ComponentType.CARD, (String) json.get("name"), ((Number) json.get("id")).intValue());
        }

        @Override
        public Component copy() {
            return new TestCard(toJSON());
        }

        @Override
        @SuppressWarnings("unchecked")
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("name", componentName);
            json.put("id", componentID);
            return json;
        }
    }

    /** Re-parses through text, as a real save-to-file/load-from-file round-trip would (json-simple
     * yields Long for integers, so this also guards the {@code (Number)} casts). */
    private static JSONObject reparse(JSONObject json) throws Exception {
        return (JSONObject) new JSONParser().parse(json.toJSONString());
    }

    @Test
    public void deckRoundTrips() throws Exception {
        Deck<TestCard> deck = new Deck<>("My Deck", 2, VisibilityMode.VISIBLE_TO_ALL);
        deck.setCapacity(10);
        deck.add(new TestCard("Ace"));
        deck.add(new TestCard("King"));
        deck.add(new TestCard("Queen"));

        Deck<TestCard> loaded = Deck.loadDeck(reparse(deck.toJSON()));

        // Deck.equals covers componentID, capacity and the ordered component list (by componentID)
        assertEquals(deck, loaded);
        assertEquals(deck.getComponentID(), loaded.getComponentID());
        assertEquals(deck.getOwnerId(), loaded.getOwnerId());
        assertEquals(deck.getVisibilityMode(), loaded.getVisibilityMode());
        assertEquals(deck.getSize(), loaded.getSize());
        for (int i = 0; i < deck.getSize(); i++) {
            assertEquals(deck.get(i).getComponentID(), loaded.get(i).getComponentID());
            assertEquals(deck.get(i).getComponentName(), loaded.get(i).getComponentName());
        }
    }

    @Test
    public void emptyDeckRoundTrips() throws Exception {
        Deck<TestCard> deck = new Deck<>("Empty", VisibilityMode.HIDDEN_TO_ALL);
        Deck<TestCard> loaded = Deck.loadDeck(reparse(deck.toJSON()));
        assertEquals(deck, loaded);
        assertEquals(0, loaded.getSize());
    }

    @Test
    public void partialObservableDeckRoundTripsIncludingVisibility() throws Exception {
        int nPlayers = 3;
        PartialObservableDeck<TestCard> deck = new PartialObservableDeck<>("Hand", 1, nPlayers, VisibilityMode.MIXED_VISIBILITY);
        // add cards with distinct per-player visibility patterns
        deck.add(new TestCard("c0"), new boolean[]{true, false, false});
        deck.add(new TestCard("c1"), new boolean[]{false, true, true});
        deck.add(new TestCard("c2"), new boolean[]{true, true, true});

        PartialObservableDeck<TestCard> loaded = PartialObservableDeck.loadDeck(reparse(deck.toJSON()));

        // Deck.equals ignores visibility, so this only confirms identity/order/contents...
        assertEquals(deck, loaded);
        assertEquals(deck.getComponentID(), loaded.getComponentID());
        assertEquals(deck.getVisibilityMode(), loaded.getVisibilityMode());

        // ...the visibility itself must be asserted explicitly, as it is the whole point of a
        // PartialObservableDeck and would otherwise be silently lost.
        assertArrayEquals(deck.getDeckVisibility(), loaded.getDeckVisibility());
        assertEquals(deck.getSize(), loaded.getSize());
        for (int i = 0; i < deck.getSize(); i++) {
            assertArrayEquals("element " + i + " visibility",
                    deck.getVisibilityOfComponent(i), loaded.getVisibilityOfComponent(i));
            for (int p = 0; p < nPlayers; p++) {
                assertEquals("card " + i + " visible to player " + p,
                        deck.isComponentVisible(i, p), loaded.isComponentVisible(i, p));
            }
        }
    }

    @Test
    public void serializingDeckWithNonSerializableComponentThrows() {
        // Card does not implement IToJSON, so it cannot live in a serializable deck.
        Deck<Card> deck = new Deck<>("Bad", VisibilityMode.VISIBLE_TO_ALL);
        deck.add(new Card("plain"));
        assertThrows(IllegalStateException.class, deck::toJSON);
    }
}
