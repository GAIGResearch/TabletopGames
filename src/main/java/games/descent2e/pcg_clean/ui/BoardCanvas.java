package games.descent2e.pcg_clean.ui;

import games.descent2e.pcg_clean.domain.*;

import javax.swing.JPanel;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.HashMap;
import java.util.Map;

/** Pure board renderer. Navigation and window lifecycle are handled by BoardViewer. */
public final class BoardCanvas extends JPanel {
    private static final int MARGIN = 28;
    private static final int MAX_CELL_SIZE = 38;

    private final BoardPalette palette;
    private final TileArtworkProvider artworkProvider;
    private BoardViewModel model;
    private boolean showPieceGraph = true;
    private boolean showArtwork = true;

    public BoardCanvas(BoardPalette palette) {
        this(palette, TileArtworkProvider.none());
    }

    public BoardCanvas(BoardPalette palette, TileArtworkProvider artworkProvider) {
        this.palette = palette;
        this.artworkProvider = artworkProvider;
        setBackground(new Color(34, 37, 42));
        setPreferredSize(new Dimension(900, 700));
    }

    public void display(BoardViewModel model) {
        this.model = model;
        repaint();
    }

    public void setShowPieceGraph(boolean showPieceGraph) {
        this.showPieceGraph = showPieceGraph;
        repaint();
    }

    public void setShowArtwork(boolean showArtwork) {
        this.showArtwork = showArtwork;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (model == null || model.layout().cells().isEmpty()) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Viewport viewport = viewport();
            drawCells(g, viewport);
            if (showArtwork) drawArtwork(g, viewport);
            drawPieces(g, viewport);
            if (showPieceGraph) drawPieceGraph(g, viewport);
        } finally {
            g.dispose();
        }
    }

    private void drawArtwork(Graphics2D g, Viewport viewport) {
        Object interpolation = viewport.cellSize >= 8
                ? RenderingHints.VALUE_INTERPOLATION_BICUBIC
                : RenderingHints.VALUE_INTERPOLATION_BILINEAR;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
        for (PlacedTile placement : model.genome().tiles()) {
            GridPoint origin = model.layout().origins().get(placement.instanceId());
            if (origin == null) continue;
            RotatedTile tile = model.tile(placement);
            Rectangle content = occupiedTerrainBounds(tile, origin, viewport);
            artworkProvider.artwork(placement.tileId(), placement.quarterTurns()).ifPresent(image ->
                    g.drawImage(image, content.x, content.y, content.width, content.height, null));
        }
    }

    /** Artwork excludes the VOID/OPEN connector border, matching the legacy image dimensions. */
    private Rectangle occupiedTerrainBounds(RotatedTile tile, GridPoint origin, Viewport viewport) {
        int minX = tile.width();
        int minY = tile.height();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < tile.height(); y++) for (int x = 0; x < tile.width(); x++) {
            Cell cell = tile.cellAt(x, y);
            if (cell == Cell.VOID || cell == Cell.OPEN) continue;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        if (maxX < minX) return viewport.bounds(origin, tile.width(), tile.height());
        return viewport.bounds(origin.plus(new GridPoint(minX, minY)), maxX - minX + 1, maxY - minY + 1);
    }

    private void drawCells(Graphics2D g, Viewport viewport) {
        model.layout().cells().forEach((point, cell) -> {
            Rectangle rectangle = viewport.cell(point);
            g.setColor(palette.colour(cell));
            g.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            g.setColor(new Color(0, 0, 0, 45));
            g.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        });
    }

    private void drawPieces(Graphics2D g, Viewport viewport) {
        g.setStroke(new BasicStroke(Math.max(2f, viewport.cellSize / 10f)));
        for (PlacedTile placement : model.genome().tiles()) {
            GridPoint origin = model.layout().origins().get(placement.instanceId());
            if (origin == null) continue;
            RotatedTile tile = model.tile(placement);
            Rectangle bounds = viewport.bounds(origin, tile.width(), tile.height());
            g.setColor(pieceColour(placement.instanceId()));
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
            if (viewport.cellSize >= 10) drawPieceLabel(g, placement, bounds);
        }
    }

    private void drawPieceLabel(Graphics2D g, PlacedTile placement, Rectangle bounds) {
        String label = placement.instanceId() + ":" + placement.tileId();
        g.setFont(getFont().deriveFont(Font.BOLD, Math.max(10f, Math.min(15f, bounds.height / 4f))));
        FontMetrics metrics = g.getFontMetrics();
        int padding = 3;
        int width = metrics.stringWidth(label) + padding * 2;
        int height = metrics.getHeight();
        g.setColor(new Color(20, 20, 20, 190));
        g.fillRoundRect(bounds.x + 2, bounds.y + 2, width, height, 5, 5);
        g.setColor(Color.WHITE);
        g.drawString(label, bounds.x + 2 + padding, bounds.y + 2 + metrics.getAscent());
    }

    private void drawPieceGraph(Graphics2D g, Viewport viewport) {
        Map<Integer, Point> centres = new HashMap<>();
        for (PlacedTile placement : model.genome().tiles()) {
            GridPoint origin = model.layout().origins().get(placement.instanceId());
            if (origin == null) continue;
            RotatedTile tile = model.tile(placement);
            Rectangle bounds = viewport.bounds(origin, tile.width(), tile.height());
            centres.put(placement.instanceId(), new Point((int) bounds.getCenterX(), (int) bounds.getCenterY()));
        }
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(225, 72, 72, 190));
        for (TileConnection edge : model.genome().connections()) {
            Point a = centres.get(edge.firstTile());
            Point b = centres.get(edge.secondTile());
            if (a != null && b != null) g.draw(new Line2D.Double(a, b));
        }
        centres.forEach((id, centre) -> {
            int radius = 4;
            g.fillOval(centre.x - radius, centre.y - radius, radius * 2, radius * 2);
        });
    }

    private Color pieceColour(int id) {
        float hue = (float) ((id * 0.173) % 1.0);
        return Color.getHSBColor(hue, 0.72f, 0.95f);
    }

    private Viewport viewport() {
        int minX = model.layout().cells().keySet().stream().mapToInt(GridPoint::x).min().orElse(0);
        int maxX = model.layout().cells().keySet().stream().mapToInt(GridPoint::x).max().orElse(0);
        int minY = model.layout().cells().keySet().stream().mapToInt(GridPoint::y).min().orElse(0);
        int maxY = model.layout().cells().keySet().stream().mapToInt(GridPoint::y).max().orElse(0);
        int boardWidth = maxX - minX + 1;
        int boardHeight = maxY - minY + 1;
        int cellSize = Math.max(2, Math.min(MAX_CELL_SIZE,
                Math.min((getWidth() - MARGIN * 2) / boardWidth, (getHeight() - MARGIN * 2) / boardHeight)));
        int renderedWidth = boardWidth * cellSize;
        int renderedHeight = boardHeight * cellSize;
        int offsetX = (getWidth() - renderedWidth) / 2 - minX * cellSize;
        int offsetY = (getHeight() - renderedHeight) / 2 - minY * cellSize;
        return new Viewport(cellSize, offsetX, offsetY);
    }

    private record Viewport(int cellSize, int offsetX, int offsetY) {
        Rectangle cell(GridPoint point) {
            return new Rectangle(offsetX + point.x() * cellSize, offsetY + point.y() * cellSize, cellSize, cellSize);
        }

        Rectangle bounds(GridPoint origin, int width, int height) {
            return new Rectangle(offsetX + origin.x() * cellSize, offsetY + origin.y() * cellSize,
                    width * cellSize, height * cellSize);
        }
    }
}
