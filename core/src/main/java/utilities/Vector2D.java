package utilities;

public class Vector2D {
    private int x;
    private int y;

    public Vector2D() {
        this.x = 0;
        this.y = 0;
    }

    public Vector2D(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int hashCode() {
        return x*5999 + y;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Vector2D)) return false;
        return x == ((Vector2D) obj).x && y == ((Vector2D) obj).y;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

    public Vector2D copy() {
        return new Vector2D(x, y);
    }

    public void add(int x, int y) {
        this.x += x;
        this.y += y;
    }

    public void subtract(int x, int y) {
        this.x -= x;
        this.y -= y;
    }

    public Vector2D add(Vector2D b) {
        return new Vector2D(x + b.x, y + b.y);
    }

    public Vector2D mult(int j) {
        return new Vector2D(x*j, y*j);
    }

    public Vector2D divide(int j) {
        return new Vector2D(x/j, y/j);
    }

    public enum Direction {
        NONE(new Vector2D()),
        UP(new Vector2D(0, -1)),
        DOWN(new Vector2D(0, 1)),
        LEFT(new Vector2D(-1, 0)),
        RIGHT(new Vector2D(1, 0)),
        UPLEFT(new Vector2D(-1, -1)),
        UPRIGHT(new Vector2D(1, -1)),
        DOWNLEFT(new Vector2D(-1, 1)),
        DOWNRIGHT(new Vector2D(1, 1));
        public final Vector2D vector2D;
        Direction(Vector2D vector2D) {
            this.vector2D = vector2D;
        }
        public static Direction[] values4() {
            return new Direction[]{UP, DOWN, LEFT, RIGHT};
        }
        public static Direction vecToDir(Vector2D vec) {
            for (Direction d: values()) {
                if (d.vector2D.equals(vec)) return d;
            }
            return null;
        }
        public static Pair<Direction, Integer> approxVecToDir(Vector2D vec) {
            int mult = Math.max(Math.abs(vec.x), Math.abs(vec.y));
            if (mult == 0) {
                return new Pair<>(NONE, 0);
            }
            Direction d = vecToDir(vec.divide(mult));
            if (d != null) return new Pair<>(d, mult);
            return null;
        }
    }
}
