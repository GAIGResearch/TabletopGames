package games.descent2e.pcg_clean.ui;

import games.descent2e.pcg_clean.data.TileCatalog;
import games.descent2e.pcg_clean.mapelites.*;
import games.descent2e.pcg_clean.spatial.SpatialCandidate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Live clickable heatmap. Each click captures the elite occupying that niche at click time. */
public final class MapElitesHeatmapPanel extends JPanel {
    private static final int LEFT = 92, RIGHT = 35, TOP = 45, BOTTOM = 82;
    private static final Color EMPTY = new Color(42, 45, 51);
    private final BehaviorDescriptor descriptor;
    private final TileCatalog catalog;
    private final TileArtworkProvider artwork;
    private final AtomicReference<MapElitesSnapshot> pending = new AtomicReference<>();
    private MapElitesSnapshot displayed = new MapElitesSnapshot(0, java.util.Map.of());

    public MapElitesHeatmapPanel(BehaviorDescriptor descriptor, TileCatalog catalog,
                                 TileArtworkProvider artwork) {
        this.descriptor = descriptor;
        this.catalog = catalog;
        this.artwork = artwork;
        setBackground(new Color(30, 33, 38));
        setForeground(new Color(235, 237, 240));
        setPreferredSize(new Dimension(920, 690));
        setToolTipText("");
        new Timer(100, ignored -> consumeLatest()).start();
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) { openEliteAt(event.getPoint()); }
        });
    }

    /** May be called from the evolution thread; only the latest update is retained. */
    public void submit(MapElitesSnapshot snapshot) { pending.set(snapshot); }

    private void consumeLatest() {
        MapElitesSnapshot latest = pending.getAndSet(null);
        if (latest != null) { displayed = latest; repaint(); }
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle grid = gridBounds();
            int cellWidth = grid.width / descriptor.branchingBins();
            int cellHeight = grid.height / descriptor.cycleBins();
            g.setFont(getFont().deriveFont(Font.BOLD, 14f));
            for (int branch = 0; branch < descriptor.branchingBins(); branch++)
                for (int cycle = 0; cycle < descriptor.cycleBins(); cycle++)
                    drawCell(g, grid, cellWidth, cellHeight, branch, cycle);
            drawAxes(g, grid, cellWidth, cellHeight);
            drawLegend(g, grid);
        } finally { g.dispose(); }
    }

    private void drawCell(Graphics2D g, Rectangle grid, int width, int height, int branch, int cycle) {
        int x = grid.x + branch * width;
        int y = grid.y + (descriptor.cycleBins() - 1 - cycle) * height;
        SpatialCandidate elite = displayed.elites().get(new EliteCell(branch, cycle));
        g.setColor(elite == null ? EMPTY : ViridisColourMap.colour(colourValue(elite)));
        g.fillRect(x + 2, y + 2, width - 4, height - 4);
        if (elite == null) return;
        boolean bright = colourValue(elite) > 0.55;
        g.setColor(bright ? new Color(25, 27, 30) : Color.WHITE);
        String first = "v=%d".formatted(elite.evaluation().violationCount());
        String second = "f=%.3f".formatted(elite.evaluation().quality().fitness());
        drawCentred(g, first, x, y + height / 2 - 4, width);
        drawCentred(g, second, x, y + height / 2 + 17, width);
    }

    private double colourValue(SpatialCandidate elite) {
        return elite.evaluation().quality().fitness() / (1.0 + elite.evaluation().violationCount());
    }

    private void drawAxes(Graphics2D g, Rectangle grid, int width, int height) {
        g.setColor(getForeground());
        for (int branch = 0; branch < descriptor.branchingBins(); branch++)
            drawCentred(g, descriptor.branchingLabel(branch), grid.x + branch * width,
                    grid.y + grid.height + 24, width);
        for (int cycle = 0; cycle < descriptor.cycleBins(); cycle++) {
            int y = grid.y + (descriptor.cycleBins() - 1 - cycle) * height + height / 2 + 5;
            String label = descriptor.cycleLabel(cycle);
            g.drawString(label, grid.x - 18 - g.getFontMetrics().stringWidth(label), y);
        }
        g.setFont(getFont().deriveFont(Font.BOLD, 16f));
        drawCentred(g, "Branching pieces (degree ≥ 3)", grid.x, grid.y + grid.height + 52, grid.width);
        g.rotate(-Math.PI / 2);
        drawCentred(g, "Independent graph cycles", -grid.y - grid.height, grid.x - 55, grid.height);
        g.rotate(Math.PI / 2);
        g.setFont(getFont().deriveFont(Font.BOLD, 18f));
        g.drawString("Live MAP-Elites — %,d evaluations — %d / %d niches occupied".formatted(
                displayed.evaluations(), displayed.elites().size(),
                descriptor.branchingBins() * descriptor.cycleBins()), grid.x, 28);
    }

    private void drawLegend(Graphics2D g, Rectangle grid) {
        int x = grid.x + grid.width - 190, y = grid.y + grid.height + 63, width = 190, height = 12;
        for (int i = 0; i < width; i++) {
            g.setColor(ViridisColourMap.colour(i / (double) (width - 1)));
            g.drawLine(x + i, y, x + i, y + height);
        }
        g.setColor(getForeground());
        g.setFont(getFont().deriveFont(11f));
        g.drawString("fitness / (1 + violations)", x, y - 3);
    }

    private void openEliteAt(Point point) {
        EliteCell cell = cellAt(point);
        if (cell == null) return;
        // Read exactly once: this is the point-in-time elite the user requested.
        SpatialCandidate candidate = displayed.elites().get(cell);
        if (candidate != null) BoardViewer.show(List.of(BoardViewModel.from(candidate, catalog)), artwork);
    }

    @Override public String getToolTipText(MouseEvent event) {
        EliteCell cell = cellAt(event.getPoint());
        if (cell == null) return null;
        SpatialCandidate candidate = displayed.elites().get(cell);
        return candidate == null ? "Empty niche" : candidate.evaluation().violationCount()
                + " violations; fitness " + "%.6f".formatted(candidate.evaluation().quality().fitness())
                + "; click to open candidate " + candidate.id();
    }

    private EliteCell cellAt(Point point) {
        Rectangle grid = gridBounds();
        if (!grid.contains(point)) return null;
        int width = grid.width / descriptor.branchingBins();
        int height = grid.height / descriptor.cycleBins();
        int branch = Math.min((point.x - grid.x) / width, descriptor.branchingBins() - 1);
        int row = Math.min((point.y - grid.y) / height, descriptor.cycleBins() - 1);
        return new EliteCell(branch, descriptor.cycleBins() - 1 - row);
    }

    private Rectangle gridBounds() {
        int width = Math.max(descriptor.branchingBins(), getWidth() - LEFT - RIGHT);
        int height = Math.max(descriptor.cycleBins(), getHeight() - TOP - BOTTOM);
        width -= width % descriptor.branchingBins();
        height -= height % descriptor.cycleBins();
        return new Rectangle(LEFT, TOP, width, height);
    }

    private void drawCentred(Graphics2D g, String text, int x, int baseline, int width) {
        g.drawString(text, x + (width - g.getFontMetrics().stringWidth(text)) / 2, baseline);
    }
}
