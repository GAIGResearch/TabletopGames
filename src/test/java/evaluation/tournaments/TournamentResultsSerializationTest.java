package evaluation.tournaments;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;

import static org.junit.Assert.*;

public class TournamentResultsSerializationTest {

    @Test
    public void testSerializationRoundTrip() throws Exception {
        TournamentResults tr = new TournamentResults();
        RandomPlayer p1 = new RandomPlayer();
        p1.setName("P1");
        RandomPlayer p2 = new RandomPlayer();
        p2.setName("P2");

        // Use the three update methods
        tr.updateGamePlayed(p1, p2);
        tr.updateOrdinalResults(p1, p2, -2);
        tr.updateWins(p1, p2, 3);

        // Add some results for both players
        tr.addResult(p1, 1.0, 1, 10.0, 1);
        tr.addResult(p1, 0.5, 2, 7.0, 0);
        tr.addResult(p2, 0.0, 3, 4.0, 0);

        ObjectMapper om = new ObjectMapper();
        om.enable(SerializationFeature.INDENT_OUTPUT);

        // Serialize and deserialize using Jackson
        String json = om.writeValueAsString(tr);
        TournamentResults tr2 = om.readValue(json, TournamentResults.class);

        // Check the maps
        assertEquals(1, tr2.getGamesPlayed("P1", "P2"));
        assertEquals(3, tr2.getWins("P1", "P2"));
        assertEquals(-2, tr2.getOrdinalDelta("P1", "P2"));

        // Check player results for P1
        List<Double> pointsP1 = tr2.getPlayerPoints("P1");
        assertEquals(2, pointsP1.size());
        assertEquals(1.0, pointsP1.get(0), 1e-9);
        assertEquals(0.5, pointsP1.get(1), 1e-9);

        List<Integer> ordinalsP1 = tr2.getPlayerOrdinals("P1");
        assertEquals(2, ordinalsP1.size());
        assertEquals(1, (int) ordinalsP1.get(0));
        assertEquals(2, (int) ordinalsP1.get(1));

        // Check player results for P2
        List<Double> pointsP2 = tr2.getPlayerPoints("P2");
        assertEquals(1, pointsP2.size());
        assertEquals(0.0, pointsP2.get(0), 1e-9);

        // Agents' names should be present
        List<String> names = tr2.getAllAgentNames();
        assertTrue(names.contains("P1"));
        assertTrue(names.contains("P2"));

        // Agents themselves are placeholders (null) after deserialization using DTO-based reconstruction
        assertNull(tr2.getAgent("P1"));
        assertNull(tr2.getAgent("P2"));
    }

    @Test
    public void gameRecordsRoundTrip() throws Exception {
        TournamentResults tr = new TournamentResults();
        tr.gameRecords.add(new TournamentResults.GameRecord(42L, new String[]{"P1", "P2"},
                new double[]{1.0, 0.0}, new int[]{1, 2}));

        ObjectMapper om = new ObjectMapper();
        TournamentResults tr2 = om.readValue(om.writeValueAsString(tr), TournamentResults.class);

        assertEquals(1, tr2.getGameRecords().size());
        TournamentResults.GameRecord record = tr2.getGameRecords().get(0);
        assertEquals(42L, record.seed);
        assertArrayEquals(new String[]{"P1", "P2"}, record.agents);
        assertArrayEquals(new double[]{1.0, 0.0}, record.points, 1e-9);
        assertArrayEquals(new int[]{1, 2}, record.ordinals);
    }

    @Test
    public void jsonWithoutGameRecordsStillLoads() throws Exception {
        // files written before game records were added
        String json = "{\"nGamesPlayedPerOpponent\":{},\"winsPerPlayerPerOpponent\":{},\"ordinalDeltaPerOpponent\":{}," +
                "\"playerResults\":{\"P1\":[{\"points\":1.0,\"ordinal\":1,\"score\":3.0,\"win\":1}]}}";
        TournamentResults tr = new ObjectMapper().readValue(json, TournamentResults.class);
        assertEquals(1, tr.getPlayerPoints("P1").size());
        assertTrue(tr.getGameRecords().isEmpty());
    }
}

