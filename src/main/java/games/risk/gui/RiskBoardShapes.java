package games.risk.gui;

import games.risk.RiskMap;
import games.risk.RiskTerritory;

import java.awt.*;
import java.awt.geom.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The territories' shapes, read from the map's SVG drawing (see {@link RiskMap#svgFile()}), in the drawing's
 * coordinates; and the part of the drawing to show (its viewBox). Each territory is the path whose id is its name in
 * lower case with underscores for spaces, in the group with id "map". A path may have a translate transform, and the
 * group is drawn with the transform of the group that uses it ({@code <use xlink:href="#map">}), a scale or a
 * translate. Path data may use any command but arcs.
 * <p>
 * Each territory also has a label point, where its armies are shown: a point well inside its largest part.
 */
public class RiskBoardShapes {

    private static final Pattern PATH = Pattern.compile("<path\\b([^>]*)>");
    private static final Pattern NUMBER = Pattern.compile("-?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?");
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z]|" + NUMBER.pattern());

    public final Rectangle2D viewBox;
    private final Map<RiskTerritory, Shape> shapes = new HashMap<>();
    private final Map<RiskTerritory, Point2D> labels = new HashMap<>();

    /**
     * Reads the shapes of the map's territories. Throws IllegalArgumentException if a territory has none.
     */
    public RiskBoardShapes(RiskMap map) {
        String svg;
        try {
            svg = Files.readString(Path.of(map.svgFile()));
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read " + map.svgFile(), e);
        }
        viewBox = viewBox(svg);
        AffineTransform group = groupTransform(svg);
        int start = svg.indexOf("id=\"map\"");
        if (start < 0) throw new IllegalArgumentException(map.svgFile() + " has no group with id \"map\"");
        int end = svg.indexOf("</g>", start);
        Map<String, Shape> byId = new HashMap<>();
        Matcher m = PATH.matcher(svg.substring(start, end));
        while (m.find()) {
            String attributes = m.group(1);
            String id = attribute(attributes, "id"), d = attribute(attributes, "d");
            if (id == null || d == null) continue;
            AffineTransform t = new AffineTransform(group);
            t.concatenate(transform(attribute(attributes, "transform")));
            byId.put(id, t.createTransformedShape(parse(d)));
        }
        for (RiskTerritory territory : map.territories()) {
            Shape shape = byId.get(territory.name().toLowerCase().replace(' ', '_'));
            if (shape == null)
                throw new IllegalArgumentException(map.svgFile() + " has no path for " + territory.name());
            shapes.put(territory, shape);
            labels.put(territory, labelPoint(shape));
        }
    }

    /**
     * The territory's shape, in the drawing's coordinates.
     */
    public Shape shape(RiskTerritory territory) {
        return shapes.get(territory);
    }

    /**
     * Where the territory's armies are shown, in the drawing's coordinates.
     */
    public Point2D label(RiskTerritory territory) {
        return labels.get(territory);
    }

    private static String attribute(String attributes, String name) {
        Matcher m = Pattern.compile("(?:^|\\s)" + name + "=\"([^\"]*)\"").matcher(attributes);
        return m.find() ? m.group(1) : null;
    }

    private static Rectangle2D viewBox(String svg) {
        Matcher m = Pattern.compile("<svg\\b[^>]*viewBox=\"([^\"]*)\"").matcher(svg);
        if (!m.find()) throw new IllegalArgumentException("The SVG has no viewBox");
        double[] v = numbers(m.group(1));
        return new Rectangle2D.Double(v[0], v[1], v[2], v[3]);
    }

    /**
     * The transform of the group that draws the map group with a use element, or none.
     */
    private static AffineTransform groupTransform(String svg) {
        int use = svg.indexOf("href=\"#map\"");
        if (use < 0) return new AffineTransform();
        int g = svg.lastIndexOf("<g", use);
        String tag = svg.substring(g, svg.indexOf('>', g));
        return transform(attribute(tag, "transform"));
    }

    /**
     * A transform of translate and scale functions (others are not used by the drawings), or the identity for null.
     */
    static AffineTransform transform(String text) {
        AffineTransform t = new AffineTransform();
        if (text == null) return t;
        Matcher m = Pattern.compile("(translate|scale)\\(([^)]*)\\)").matcher(text);
        while (m.find()) {
            double[] v = numbers(m.group(2));
            if (m.group(1).equals("translate"))
                t.translate(v[0], v.length > 1 ? v[1] : 0);
            else
                t.scale(v[0], v.length > 1 ? v[1] : v[0]);
        }
        return t;
    }

    private static double[] numbers(String text) {
        List<Double> list = new ArrayList<>();
        Matcher m = NUMBER.matcher(text);
        while (m.find()) list.add(Double.parseDouble(m.group()));
        return list.stream().mapToDouble(Double::doubleValue).toArray();
    }

    /**
     * SVG path data as a shape.
     */
    static Path2D parse(String d) {
        List<String> tokens = new ArrayList<>();
        Matcher m = TOKEN.matcher(d);
        while (m.find()) tokens.add(m.group());
        Path2D.Double path = new Path2D.Double();
        char command = 'M';
        // the current point, the start of the subpath, and the last control point (for S and T)
        double x = 0, y = 0, sx = 0, sy = 0, cx = 0, cy = 0;
        char last = ' ';
        int i = 0;
        while (i < tokens.size()) {
            String token = tokens.get(i);
            if (Character.isLetter(token.charAt(0))) {
                command = token.charAt(0);
                i++;
                if (command == 'Z' || command == 'z') {
                    path.closePath();
                    x = sx;
                    y = sy;
                    last = command;
                    continue;
                }
            }
            boolean relative = Character.isLowerCase(command);
            double ox = relative ? x : 0, oy = relative ? y : 0;
            switch (Character.toUpperCase(command)) {
                case 'M' -> {
                    x = ox + num(tokens, i++);
                    y = oy + num(tokens, i++);
                    path.moveTo(x, y);
                    sx = x;
                    sy = y;
                    // further pairs are lines
                    command = relative ? 'l' : 'L';
                }
                case 'L' -> {
                    x = ox + num(tokens, i++);
                    y = oy + num(tokens, i++);
                    path.lineTo(x, y);
                }
                case 'H' -> {
                    x = ox + num(tokens, i++);
                    path.lineTo(x, y);
                }
                case 'V' -> {
                    y = oy + num(tokens, i++);
                    path.lineTo(x, y);
                }
                case 'C' -> {
                    double x1 = ox + num(tokens, i++), y1 = oy + num(tokens, i++);
                    cx = ox + num(tokens, i++);
                    cy = oy + num(tokens, i++);
                    x = ox + num(tokens, i++);
                    y = oy + num(tokens, i++);
                    path.curveTo(x1, y1, cx, cy, x, y);
                }
                case 'S' -> {
                    boolean follows = "CcSs".indexOf(last) >= 0;
                    double x1 = follows ? 2 * x - cx : x, y1 = follows ? 2 * y - cy : y;
                    cx = ox + num(tokens, i++);
                    cy = oy + num(tokens, i++);
                    x = ox + num(tokens, i++);
                    y = oy + num(tokens, i++);
                    path.curveTo(x1, y1, cx, cy, x, y);
                }
                case 'Q' -> {
                    cx = ox + num(tokens, i++);
                    cy = oy + num(tokens, i++);
                    x = ox + num(tokens, i++);
                    y = oy + num(tokens, i++);
                    path.quadTo(cx, cy, x, y);
                }
                case 'T' -> {
                    boolean follows = "QqTt".indexOf(last) >= 0;
                    cx = follows ? 2 * x - cx : x;
                    cy = follows ? 2 * y - cy : y;
                    x = ox + num(tokens, i++);
                    y = oy + num(tokens, i++);
                    path.quadTo(cx, cy, x, y);
                }
                default -> throw new IllegalArgumentException("Unsupported SVG path command " + command);
            }
            last = command;
        }
        return path;
    }

    private static double num(List<String> tokens, int i) {
        return Double.parseDouble(tokens.get(i));
    }

    /**
     * A point well inside the largest part of the shape: of the points on a grid inside it, the one furthest from its
     * edge.
     */
    static Point2D labelPoint(Shape shape) {
        // the parts of the shape, as polygons, and the largest
        List<List<Point2D>> parts = new ArrayList<>();
        List<Point2D> current = null;
        double[] c = new double[6];
        for (PathIterator it = shape.getPathIterator(null, 0.5); !it.isDone(); it.next()) {
            int type = it.currentSegment(c);
            if (type == PathIterator.SEG_MOVETO) {
                current = new ArrayList<>();
                parts.add(current);
            }
            if (type != PathIterator.SEG_CLOSE && current != null)
                current.add(new Point2D.Double(c[0], c[1]));
        }
        List<Point2D> largest = parts.get(0);
        for (List<Point2D> p : parts)
            if (area(p) > area(largest)) largest = p;
        Path2D polygon = new Path2D.Double();
        polygon.moveTo(largest.get(0).getX(), largest.get(0).getY());
        for (Point2D p : largest) polygon.lineTo(p.getX(), p.getY());
        polygon.closePath();
        Rectangle2D b = polygon.getBounds2D();
        double step = Math.max(0.5, Math.min(b.getWidth(), b.getHeight()) / 40);
        Point2D best = new Point2D.Double(b.getCenterX(), b.getCenterY());
        double bestDistance = -1;
        for (double x = b.getMinX(); x <= b.getMaxX(); x += step)
            for (double y = b.getMinY(); y <= b.getMaxY(); y += step) {
                if (!polygon.contains(x, y)) continue;
                double distance = Double.MAX_VALUE;
                for (int i = 0; i < largest.size(); i++) {
                    Point2D p = largest.get(i), q = largest.get((i + 1) % largest.size());
                    distance = Math.min(distance, Line2D.ptSegDist(p.getX(), p.getY(), q.getX(), q.getY(), x, y));
                }
                if (distance > bestDistance) {
                    bestDistance = distance;
                    best = new Point2D.Double(x, y);
                }
            }
        return best;
    }

    private static double area(List<Point2D> polygon) {
        double a = 0;
        for (int i = 0; i < polygon.size(); i++) {
            Point2D p = polygon.get(i), q = polygon.get((i + 1) % polygon.size());
            a += p.getX() * q.getY() - q.getX() * p.getY();
        }
        return Math.abs(a / 2);
    }
}
