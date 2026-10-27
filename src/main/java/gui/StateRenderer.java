package gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.GameType;
import players.human.ActionController;
import players.simple.RandomPlayer;
import utilities.JSONUtils;
import utilities.Utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility that loads a saved game-state JSON file (as produced by the JSON serialisation used by the
 * Advisers feature) and renders it to a PNG image, going through the game's real GUI components.
 * <p>
 * It loads the state (see {@code RoundRobinTournament} for the same pattern), builds the game's
 * {@link AbstractGUIManager} via {@link GameType#createGUIManager}, updates the view to reflect the
 * loaded state, and then paints the relevant Swing component to an offscreen {@link BufferedImage}.
 * No window is ever shown and no screenshot is taken — painting is done via {@code printAll}, so the
 * output is deterministic and does not require focus or an unoccluded display.
 * <p>
 * Arguments (key=value):
 * <ul>
 *   <li>{@code input}     - path to a saved-state JSON file, or a directory of {@code *.json} files (batch mode).</li>
 *   <li>{@code output}    - output PNG path (single file), or output directory (batch mode). Defaults to the
 *                           input path with its extension replaced by {@code .png}.</li>
 *   <li>{@code game}      - {@link GameType} name (default {@code XIIScripta}).</li>
 *   <li>{@code fullPanel} - {@code true} to render the whole game panel (board + info panel); default {@code false}
 *                           renders the board view only.</li>
 * </ul>
 */
public class StateRenderer {

    public static void main(String[] args) {
        String input = Utils.getArg(args, "input", "");
        String output = Utils.getArg(args, "output", "");
        String game = Utils.getArg(args, "game", "XIIScripta");
        boolean fullPanel = Utils.getArg(args, "fullPanel", false);

        if (input.isEmpty()) {
            System.out.println("Usage: input=<file-or-dir> [output=<file-or-dir>] [game=XIIScripta] [fullPanel=false]");
            return;
        }

        GameType gameType = GameType.valueOf(game);
        File inputFile = new File(input);
        if (!inputFile.exists())
            throw new AssertionError("Input does not exist: " + input);

        if (inputFile.isDirectory()) {
            File[] jsonFiles = inputFile.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
            if (jsonFiles == null || jsonFiles.length == 0) {
                System.out.println("No .json files found in directory " + input);
                return;
            }
            File outDir = output.isEmpty() ? inputFile : new File(output);
            if (!outDir.exists() && !outDir.mkdirs())
                throw new AssertionError("Could not create output directory: " + outDir);

            int success = 0;
            for (File json : jsonFiles) {
                File png = new File(outDir, stripExtension(json.getName()) + ".png");
                try {
                    render(gameType, json, png, fullPanel);
                    success++;
                } catch (Exception e) {
                    System.err.println("Failed to render " + json.getName() + " : " + e.getMessage());
                }
            }
            System.out.printf("Rendered %d of %d state files to %s%n", success, jsonFiles.length, outDir);
        } else {
            File png = output.isEmpty()
                    ? new File(inputFile.getParentFile(), stripExtension(inputFile.getName()) + ".png")
                    : new File(output);
            render(gameType, inputFile, png, fullPanel);
            System.out.println("Rendered " + inputFile + " to " + png);
        }
    }

    /**
     * Loads the state in {@code stateFile} and writes a PNG of it to {@code outputFile}.
     *
     * @param gameType   the game the state belongs to (determines which GUI manager / view is used).
     * @param stateFile  the saved-state JSON file.
     * @param outputFile the PNG to write.
     * @param fullPanel  if true render the whole game panel; otherwise the board view only.
     */
    public static void render(GameType gameType, File stateFile, File outputFile, boolean fullPanel) {
        BufferedImage img = renderToImage(gameType, stateFile, fullPanel);
        try {
            ImageIO.write(img, "png", outputFile);
        } catch (Exception e) {
            throw new AssertionError("Failed to write PNG " + outputFile + " : " + e);
        }
    }

    /**
     * Loads the state in {@code stateFile} and returns a rendered image of it.
     *
     * @param fullPanel if true, render the whole game panel (board + info panel); otherwise render only
     *                  the board view (the component placed at {@link BorderLayout#CENTER} by the GUI manager).
     */
    public static BufferedImage renderToImage(GameType gameType, File stateFile, boolean fullPanel) {
        // 1. Load the saved state
        AbstractGameState state = JSONUtils.loadClassFromFile(stateFile.getAbsolutePath());
        return renderToImage(gameType, state, fullPanel);
    }

    /**
     * Renders a live (in-memory) {@link AbstractGameState} to an image, going through the game's real
     * GUI components. This is the painting half of {@link #renderToImage(GameType, File, boolean)} and
     * is used by observers that want to render a state they already hold (rather than load from file).
     *
     * @param fullPanel if true, render the whole game panel (board + info panel); otherwise render only
     *                  the board view (the component placed at {@link BorderLayout#CENTER} by the GUI manager).
     */
    public static BufferedImage renderToImage(GameType gameType, AbstractGameState state, boolean fullPanel) {
        // 2. Wrap it in a Game for the matching forward model, then install the loaded state.
        List<AbstractPlayer> players = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++)
            players.add(new RandomPlayer());
        Game gameInstance = gameType.createGameInstance(state.getNPlayers());
        gameInstance.reset(players);
        gameInstance.reset(state);

        // 3. Build the real GUI components for this game.
        GamePanel gamePanel = new GamePanel();
        ActionController ac = new ActionController();
        AbstractGUIManager gui = gameType.createGUIManager(gamePanel, gameInstance, ac);

        // 4. Populate the view with the loaded state
        int currentPlayer = state.getCurrentPlayer();
        gui.update(players.get(currentPlayer), state, true);

        // 5. Choose what to capture: the board view only (default) or the whole panel.
        Component target = gamePanel;
        if (!fullPanel) {
            Component centre = boardView(gamePanel);
            if (centre != null) target = centre;
        }

        // 6. Lay the chosen component out offscreen at its own preferred size (no window is shown) so it
        //    has valid bounds. Sizing the target directly avoids the BorderLayout CENTER component being
        //    stretched to fill the parent panel's leftover space.
        target.setSize(target.getPreferredSize());
        layoutRecursively(target);

        int w = Math.max(1, target.getWidth());
        int h = Math.max(1, target.getHeight());
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        // A TYPE_INT_RGB image initialises to black; fill it white first so any region of the panel that
        // is not covered by an opaque component (e.g. empty action/history areas in full-panel mode) shows
        // as white rather than a black band.
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        target.printAll(g);
        g.dispose();
        return img;
    }

    /**
     * Returns a new image consisting of {@code image} with the given lines of text drawn in a white strip
     * <b>below</b> it. Used to annotate rendered states with extra information (e.g. the chosen action, or
     * the top actions an agent considered). If {@code colors} is non-null it supplies a colour per line
     * (shorter lists fall back to a dark default).
     */
    public static BufferedImage withTextBelow(BufferedImage image, List<String> lines, List<Color> colors) {
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 16);
        int pad = 8, lineGap = 4;
        Color defaultColor = new Color(0x10, 0x10, 0x10);

        // Measure with a scratch graphics context.
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D sg = scratch.createGraphics();
        sg.setFont(font);
        FontMetrics fm = sg.getFontMetrics();
        int lineH = fm.getHeight();
        int textW = lines.stream().mapToInt(fm::stringWidth).max().orElse(0);
        sg.dispose();

        int stripH = pad * 2 + lineH * lines.size() + lineGap * Math.max(0, lines.size() - 1);
        int w = Math.max(image.getWidth(), textW + pad * 2);
        int h = image.getHeight() + stripH;

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font);

        // The image first, then a separator, then the text below it.
        g.drawImage(image, 0, 0, null);
        g.setColor(Color.LIGHT_GRAY);
        g.drawLine(0, image.getHeight(), w, image.getHeight());

        int baseline = image.getHeight() + pad + fm.getAscent();
        for (int i = 0; i < lines.size(); i++) {
            Color c = (colors != null && i < colors.size() && colors.get(i) != null) ? colors.get(i) : defaultColor;
            g.setColor(c);
            g.drawString(lines.get(i), pad, baseline + i * (lineH + lineGap));
        }
        g.dispose();
        return out;
    }

    // The board view is placed at BorderLayout.CENTER by the backgammon-family GUI managers.
    private static Component boardView(GamePanel gamePanel) {
        if (gamePanel.getLayout() instanceof BorderLayout layout)
            return layout.getLayoutComponent(BorderLayout.CENTER);
        return null;
    }

    // Recursively force layout of a component tree without realising a window.
    private static void layoutRecursively(Component c) {
        c.doLayout();
        if (c instanceof Container container) {
            for (Component child : container.getComponents())
                layoutRecursively(child);
        }
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot == -1 ? name : name.substring(0, dot);
    }
}
