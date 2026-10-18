package gui;

import games.GameType;
import games.explodingkittens.ExplodingKittensForwardModel;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.ExplodingKittensParameters;
import org.json.simple.JSONObject;
import org.junit.Test;
import utilities.JSONUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class StateRendererTest {

    static final File XII_Sample_state = new File("src/test/resources/P1_Tick659.json");

    @Test
    public void rendersBoardOnlyPng() throws Exception {
        assumeStateFileExists();
        BufferedImage img = StateRenderer.renderToImage(GameType.XIIScripta, XII_Sample_state, false);
        // XIIBoardView is 900x500.
        assertEquals(900, img.getWidth());
        assertEquals(500, img.getHeight());
        assertTrue("Rendered board should not be a single flat colour", hasMultipleColours(img));
    }

    @Test
    public void rendersFullPanelPng() throws Exception {
        assumeStateFileExists();
        BufferedImage img = StateRenderer.renderToImage(GameType.XIIScripta, XII_Sample_state, true);
        // Full panel is at least as large as the board view, and taller (info + action panels).
        assertTrue(img.getWidth() >= 900);
        assertTrue(img.getHeight() > 500);
        assertTrue(hasMultipleColours(img));
    }

    @Test
    public void writesPngFileToDisk() throws Exception {
        assumeStateFileExists();
        Path tmpDir = Files.createTempDirectory("staterender");
        File out = new File(tmpDir.toFile(), "state.png");
        StateRenderer.render(GameType.XIIScripta, XII_Sample_state, out, false);
        assertTrue(out.exists());
        assertTrue("PNG should be non-trivial in size", out.length() > 1000);
        BufferedImage reloaded = ImageIO.read(out);
        assertNotNull("Output should be a decodable PNG", reloaded);
        assertEquals(900, reloaded.getWidth());
    }

    /**
     * Renders a second, structurally different game (Exploding Kittens) to confirm StateRenderer is
     * game-agnostic. EK uses a completely different GUI layout (player hands on the four edges, a
     * BoxLayout of deck/discard piles in the centre) and loads card-image assets from {@code data/},
     * so a successful render exercises a different code path from the backgammon-family board view.
     * The state is generated and serialised here rather than relying on a checked-in JSON file.
     */
    @Test
    public void rendersExplodingKittensFromGeneratedState() throws Exception {
        org.junit.Assume.assumeTrue("EK card assets not present", new File("data/explodingkittens").isDirectory());

        ExplodingKittensGameState state =
                new ExplodingKittensGameState(new ExplodingKittensParameters(), 4);
        new ExplodingKittensForwardModel().setup(state);

        Path tmpDir = Files.createTempDirectory("staterender-ek");
        File stateFile = new File(tmpDir.toFile(), "ek_state.json");
        JSONObject json = state.toJSON();
        JSONUtils.writeJSON(json, stateFile.getAbsolutePath());

        // Board-only (the central deck/discard area) and the full panel (info + board + actions).
        BufferedImage board = StateRenderer.renderToImage(GameType.ExplodingKittens, stateFile, false);
        assertTrue("EK board render should have positive dimensions", board.getWidth() > 0 && board.getHeight() > 0);
        assertTrue("EK board should not be a single flat colour", hasMultipleColours(board));

        BufferedImage full = StateRenderer.renderToImage(GameType.ExplodingKittens, stateFile, true);
        assertTrue("Full panel should be at least as wide as the board", full.getWidth() >= board.getWidth());
        assertTrue("Full panel should be taller than the board (info + action panels)", full.getHeight() > board.getHeight());
        assertTrue(hasMultipleColours(full));

        File out = new File(tmpDir.toFile(), "ek_state.png");
        StateRenderer.render(GameType.ExplodingKittens, stateFile, out, true);
        assertTrue("EK PNG should be written and non-trivial", out.exists() && out.length() > 1000);
        assertNotNull("EK output should be a decodable PNG", ImageIO.read(out));
    }

    private void assumeStateFileExists() {
        org.junit.Assume.assumeTrue("Test state file " + XII_Sample_state + " not present", XII_Sample_state.exists());
    }

    private static boolean hasMultipleColours(BufferedImage img) {
        int first = img.getRGB(0, 0);
        for (int x = 0; x < img.getWidth(); x += 13)
            for (int y = 0; y < img.getHeight(); y += 13)
                if (img.getRGB(x, y) != first) return true;
        return false;
    }
}
