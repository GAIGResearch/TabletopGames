package gui;

import core.components.grid.HexCoord;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.Collection;

/**
 * Where pointy-topped hexes are drawn on screen: size is the distance from a hex's centre to each corner, and the
 * origin is the centre of hex (0,0). A hex is sqrt(3) * size wide and 2 * size high.
 */
public record HexLayout(double size, double originX, double originY) {

    private static final double SQRT3 = Math.sqrt(3);

    /**
     * The largest layout, up to maxSize, that fits the hexes into a width by height area with a margin all round,
     * centred in it.
     */
    public static HexLayout fitting(Collection<HexCoord> hexes, double width, double height, double margin,
                                    double maxSize) {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (HexCoord h : hexes) {
            double x = SQRT3 * (h.q() + h.r() / 2.0), y = 1.5 * h.r();
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        if (hexes.isEmpty())
            minX = maxX = minY = maxY = 0;
        // the centres span (maxX - minX) by (maxY - minY) at size 1, and the hexes reach half a hex beyond them
        double size = Math.min(maxSize, Math.min((width - 2 * margin) / (maxX - minX + SQRT3),
                (height - 2 * margin) / (maxY - minY + 2)));
        return new HexLayout(size, width / 2 - size * (minX + maxX) / 2, height / 2 - size * (minY + maxY) / 2);
    }

    public Point2D centre(HexCoord h) {
        return new Point2D.Double(originX + size * SQRT3 * (h.q() + h.r() / 2.0), originY + size * 1.5 * h.r());
    }

    /**
     * The centre of the hex, rounded to the nearest pixel.
     */
    public Point pixel(HexCoord h) {
        Point2D c = centre(h);
        return new Point((int) Math.round(c.getX()), (int) Math.round(c.getY()));
    }

    /**
     * The corner of the hex, if it were radius from its centre to each corner.
     */
    public Point2D corner(HexCoord h, int corner, double radius) {
        Point2D c = centre(h);
        double angle = Math.toRadians(60 * corner - 30);
        return new Point2D.Double(c.getX() + radius * Math.cos(angle), c.getY() + radius * Math.sin(angle));
    }

    public Point2D corner(HexCoord h, int corner) {
        return corner(h, corner, size);
    }

    /**
     * The outline of the hex, drawn radius from its centre to each corner (size for the full hex, less to inset it).
     */
    public Path2D outline(HexCoord h, double radius) {
        Path2D path = new Path2D.Double();
        for (int i = 0; i < 6; i++) {
            Point2D p = corner(h, i, radius);
            if (i == 0) path.moveTo(p.getX(), p.getY());
            else path.lineTo(p.getX(), p.getY());
        }
        path.closePath();
        return path;
    }

    /**
     * The hex under the point.
     */
    public HexCoord hexAt(double px, double py) {
        double x = (px - originX) / size, y = (py - originY) / size;
        double q = SQRT3 / 3 * x - y / 3, r = 2.0 / 3 * y;
        // round in cube coordinates (q, r, s with q + r + s = 0), then correct the coordinate that moved furthest
        double s = -q - r;
        long rq = Math.round(q), rr = Math.round(r), rs = Math.round(s);
        double dq = Math.abs(rq - q), dr = Math.abs(rr - r), ds = Math.abs(rs - s);
        if (dq > dr && dq > ds)
            rq = -rr - rs;
        else if (dr > ds)
            rr = -rq - rs;
        return new HexCoord((int) rq, (int) rr);
    }
}
