package games.connect4;

import core.AbstractParameters;
import games.GameType;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * The Connect4 experiment configs predate the split of the square {@code gridSize} parameter into
 * {@code width}/{@code height}. They are checked here because the parameter loader only warns about an
 * unknown key and then runs with defaults — a stale {@code gridSize} silently changes the board the
 * experiment runs on.
 *
 * <p>The configs are experiment data and are not tracked, so the test only runs where they are present.</p>
 */
public class Connect4ParameterFileTest {

    private static final String[][] EXPECTED = {
            {"param1", "10", "3"},
            {"param2", "8", "6"},
            {"param3", "6", "4"},
            {"param4", "12", "5"},
    };

    @Test
    public void experimentParameterFilesConfigureTheBoardTheyDescribe() {
        Assume.assumeTrue("Experiments/ is local data and is not checked in",
                new File("Experiments/Connect4/gameParams").isDirectory());
        for (String[] row : EXPECTED) {
            String path = "Experiments/Connect4/gameParams/" + row[0] + "/connect4Param.json";
            AbstractParameters params = AbstractParameters.createFromFile(GameType.Connect4, path);
            Connect4GameParameters c4 = (Connect4GameParameters) params;

            int side = Integer.parseInt(row[1]);
            assertEquals(path + " width", side, c4.width);
            assertEquals(path + " height (the old gridSize was square)", side, c4.height);
            assertEquals(path + " winCount", Integer.parseInt(row[2]), c4.winCount);
        }
    }
}
