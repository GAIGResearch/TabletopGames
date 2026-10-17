package games.hareandtortoise.gui;

import games.hareandtortoise.HareAndTortoiseGameState;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static games.hareandtortoise.HareAndTortoiseParameters.HOME_SQUARE;

/**
 * The 1978 Ravensburger board, with each player's runner drawn on its square. Runners at START or HOME, which
 * several may share, are drawn side by side.
 */
public class HareAndTortoiseBoardView extends JComponent {

    static final String BOARD_IMAGE = "data/hareandtortoise/board.jpg";
    static final double SCALE = 1.0;
    static final int IMAGE_WIDTH = 800, IMAGE_HEIGHT = 600;
    static final int RUNNER_RADIUS = 12;

    // the centre of each square on board.jpg: index 0 is START, 64 HOME
    static final int[][] SQUARE_CENTRES = {
            {380, 300}, {355, 331}, {307, 331}, {283, 378}, {283, 427}, {331, 450}, {331, 500}, {283, 500},
            {235, 523}, {187, 523}, {140, 523}, {93, 498}, {72, 450}, {72, 403}, {73, 355}, {118, 355},
            {166, 355}, {166, 304}, {167, 262}, {122, 262}, {78, 240}, {80, 195}, {82, 150}, {105, 105},
            {129, 62}, {173, 60}, {218, 55}, {240, 102}, {285, 123}, {331, 123}, {332, 78}, {377, 78},
            {420, 77}, {468, 77}, {470, 122}, {517, 122}, {560, 100}, {580, 57}, {625, 57}, {668, 57},
            {693, 102}, {718, 147}, {720, 190}, {723, 235}, {680, 258}, {635, 258}, {637, 303}, {640, 352},
            {685, 350}, {728, 350}, {732, 397}, {733, 443}, {712, 492}, {667, 517}, {643, 470}, {618, 423},
            {570, 423}, {548, 472}, {550, 522}, {502, 522}, {453, 498}, {452, 450}, {452, 400}, {452, 353},
            {443, 318}
    };

    static final Color[] RUNNER_COLOURS = {
            new Color(220, 40, 40), new Color(40, 90, 220), new Color(245, 210, 40),
            new Color(40, 160, 60), Color.WHITE, new Color(240, 130, 30)
    };

    private final Image board;
    private HareAndTortoiseGameState state;

    public HareAndTortoiseBoardView() {
        board = ImageIO.GetInstance().getImage(BOARD_IMAGE);
        Dimension size = new Dimension((int) (IMAGE_WIDTH * SCALE), (int) (IMAGE_HEIGHT * SCALE));
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
    }

    public void update(HareAndTortoiseGameState state) {
        this.state = state;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(board, 0, 0, getPreferredSize().width, getPreferredSize().height, null);
        if (state == null) return;

        int current = state.isNotTerminal() ? state.getCurrentPlayer() : -1;
        List<Integer> atStart = new ArrayList<>(), atHome = new ArrayList<>();
        for (int p = 0; p < state.getNPlayers(); p++) {
            int square = state.getSquare(p);
            if (square == 0) atStart.add(p);
            else if (square == HOME_SQUARE) atHome.add(p);
            else drawRunner(g2, p, SQUARE_CENTRES[square][0], SQUARE_CENTRES[square][1], p == current);
        }
        drawRow(g2, atStart, SQUARE_CENTRES[0], current);
        drawRow(g2, atHome, SQUARE_CENTRES[HOME_SQUARE], current);
    }

    private void drawRow(Graphics2D g, List<Integer> players, int[] centre, int current) {
        int step = 2 * RUNNER_RADIUS - 4;
        int x0 = centre[0] - (players.size() - 1) * step / 2;
        for (int i = 0; i < players.size(); i++)
            drawRunner(g, players.get(i), x0 + i * step, centre[1], players.get(i) == current);
    }

    private void drawRunner(Graphics2D g, int player, int imageX, int imageY, boolean current) {
        int x = (int) (imageX * SCALE), y = (int) (imageY * SCALE), r = RUNNER_RADIUS;
        if (current) {
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(4));
            g.drawOval(x - r - 4, y - r - 4, 2 * r + 8, 2 * r + 8);
        }
        g.setColor(RUNNER_COLOURS[player % RUNNER_COLOURS.length]);
        g.fillOval(x - r, y - r, 2 * r, 2 * r);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2));
        g.drawOval(x - r, y - r, 2 * r, 2 * r);
        g.setFont(getFont().deriveFont(Font.BOLD, 13f));
        String label = String.valueOf(player);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, x - fm.stringWidth(label) / 2, y + fm.getAscent() / 2 - 1);
    }
}
