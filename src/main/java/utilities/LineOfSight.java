package utilities;

import java.util.ArrayList;

public class LineOfSight {

    /**
     * Code taken from: http://rosettacode.org/wiki/Bitmap/Bresenham%27s_line_algorithm#Java
     *
     * Bresenham's line algorithm is a line drawing algorithm that determines the points of an n-dimensional raster
     * that should be selected in order to form a close approximation to a straight line between two points.
     *
     * Input:
     *  Vector2D - startPoint of the line
     *  Vector2D - endPoint of the line
     * Output:
     *  ArrayList<Vector2D> - list of the x,y coordinates contained in the line
     */
    public static ArrayList<Vector2D> bresenhamsLineAlgorithm(Vector2D startPoint, Vector2D endPoint) {
        // delta of exact value and rounded value of the dependent variable
        int x1 = startPoint.getX();
        int y1 = startPoint.getY();

        int x2 = endPoint.getX();
        int y2 = endPoint.getY();

        ArrayList<Vector2D> containedPoints = new ArrayList<>();

        int d = 0;

        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);

        int dx2 = 2 * dx; // slope scaling factors to
        int dy2 = 2 * dy; // avoid floating point

        int ix = x1 < x2 ? 1 : -1; // increment direction
        int iy = y1 < y2 ? 1 : -1;

        int x = x1;
        int y = y1;

        if (dx > dy) {
            while (true) {
                containedPoints.add(new Vector2D(x, y));
                if (x == x2)
                    break;

                d += dy2;
                if (d > dx) {
                    y += iy;
                    containedPoints.add(new Vector2D(x, y));
                    d -= dx2;
                }

                x += ix;
            }
        } else if (dy > dx) {
            while (true) {
                containedPoints.add(new Vector2D(x, y));
                if (y == y2)
                    break;

                d += dx2;
                if (d > dy) {
                    x += ix;
                    containedPoints.add(new Vector2D(x, y));
                    d -= dy2;
                }

                y += iy;
            }
        } else if (dx == dy) {

            while (true) {
                d += dx2;
                d += dy2;

                containedPoints.add(new Vector2D(x, y));
                if (y == y2 || x == x2)
                    break;

                //This increases the line size -- made for with descent_2e line of sight
                //containedPoints.add(new Vector2D(x, y + iy));
                //containedPoints.add(new Vector2D(x + ix, y));

                y += iy;
                x += ix;
            }
        }

        return containedPoints;
    }
}

