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
}
