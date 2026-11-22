package utilities;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class JsonTest {

    static int counter = 0;

    @SuppressWarnings("unused")
    static class Fixture {
        transient int id = ++counter;
        private int visible = 3;
        protected String name = "n";
        String nothing = null;
        Map<String, Integer> withNull = new HashMap<>(Map.of("a", 1));

        Fixture() {
            withNull.put("b", null);
        }

        public int getId() {
            return id;
        }

        public boolean isSpecial() {
            return true;
        }

        public int getDerived() {
            return visible * 2;
        }
    }

    enum Colour {
        RED, GREEN;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    record Progress(int[] rows, double rate) {
    }

    @Test
    public void transientFieldWithGetterIsNotSerialised() {
        JsonNode node = Json.toTree(new Fixture());
        assertFalse(node.has("id"));
        assertFalse(node.has("special"));
        assertFalse(node.has("derived"));
    }

    @Test
    public void allNonTransientFieldsAreSerialised() {
        JsonNode node = Json.toTree(new Fixture());
        assertEquals(3, node.get("visible").asInt());
        assertEquals("n", node.get("name").asText());
    }

    @Test
    public void nullsAreOmittedIncludingMapValues() {
        JsonNode node = Json.toTree(new Fixture());
        assertFalse(node.has("nothing"));
        assertEquals(1, node.get("withNull").size());
        assertEquals(1, node.get("withNull").get("a").asInt());
    }

    @Test
    public void mapKeysAreSortedAndEnumKeysUseName() throws Exception {
        Map<Colour, Integer> map = new EnumMap<>(Colour.class);
        map.put(Colour.GREEN, 2);
        map.put(Colour.RED, 1);
        // enum keys sort in declaration order, their natural order
        assertEquals("{\"RED\":1,\"GREEN\":2}", Json.mapper().writeValueAsString(map));

        Map<String, Integer> unsorted = new LinkedHashMap<>();
        unsorted.put("b", 1);
        unsorted.put("a", 2);
        assertEquals("{\"a\":2,\"b\":1}", Json.mapper().writeValueAsString(unsorted));
    }

    @Test
    public void enumValuesUseName() throws Exception {
        assertEquals("\"RED\"", Json.mapper().writeValueAsString(Colour.RED));
    }

    @Test
    public void recordsRoundTrip() throws Exception {
        Progress p = new Progress(new int[]{1, 2}, 0.5);
        Progress back = Json.mapper().readValue(Json.mapper().writeValueAsString(p), Progress.class);
        assertArrayEquals(p.rows(), back.rows());
        assertEquals(p.rate(), back.rate(), 0.0);
    }

    @Test
    public void unknownPropertiesAreIgnoredOnRead() throws Exception {
        Progress back = Json.mapper().readValue("{\"rows\":[1],\"rate\":1.0,\"extra\":7}", Progress.class);
        assertEquals(1.0, back.rate(), 0.0);
    }

    static class Holder {
        java.util.function.IntUnaryOperator lambda = x -> x + counter;
        Object anonymous = new Object() {
            final int captured = 5;
        };
        List<Object> list = new ArrayList<>(List.of(1, (Runnable) () -> {
        }));
        Map<Object, Integer> incomparableKeys = new HashMap<>(Map.of(new Object(), 1, new Object(), 2));
    }

    @Test
    public void lambdasAndAnonymousClassesAreNotSerialised() {
        JsonNode node = Json.toTree(new Holder());
        assertFalse(node.has("lambda"));
        assertFalse(node.has("anonymous"));
        assertEquals(2, node.get("list").size());
        assertTrue(node.get("list").get(1).isNull());
    }

    @Test
    public void mapsWithIncomparableKeysAreWrittenUnsorted() {
        assertEquals(2, Json.toTree(new Holder()).get("incomparableKeys").size());
    }

    static class Sets {
        HashSet<Colour> set;
        HashMap<Colour, Integer> map;
    }

    @Test
    public void hashSetsAndMapsAreReadAsLinkedOnes() throws Exception {
        Sets sets = Json.mapper().readValue("{\"set\":[\"GREEN\",\"RED\"],\"map\":{\"GREEN\":1}}", Sets.class);
        assertEquals(LinkedHashSet.class, sets.set.getClass());
        assertEquals(List.of(Colour.GREEN, Colour.RED), new ArrayList<>(sets.set));
        assertEquals(LinkedHashMap.class, sets.map.getClass());
    }

    static class Base {
        int x = 1;
    }

    static class Shadowing extends Base {
        int x = 2;
    }

    @Test
    public void aShadowedFieldIsRefusedNotSilentlyDropped() {
        try {
            Json.mapper().writeValueAsString(new Shadowing());
            fail("expected the duplicate field x to be refused");
        } catch (Exception e) {
            assertTrue(e.getMessage(), e.getMessage().contains("multiple JSON fields named x"));
        }
        try {
            Json.mapper().readValue("{\"x\":3}", Shadowing.class);
            fail("expected the duplicate field x to be refused");
        } catch (Exception e) {
            assertTrue(e.getMessage(), e.getMessage().contains("multiple JSON fields named x"));
        }
    }

    @Test
    public void emptyObjectsSerialise() throws Exception {
        assertEquals("{}", Json.mapper().writeValueAsString(new Object()));
    }
}
