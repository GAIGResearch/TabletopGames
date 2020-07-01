package utilities;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class Utils {

    public static Color stringToColor(String c) {
        switch(c.toLowerCase()) {
            case "blue": return Color.BLUE;
            case "black": return Color.BLACK;
            case "yellow": return Color.YELLOW;
            case "red": return Color.RED;
            case "green": return new Color(30, 108, 47);
            case "white": return Color.WHITE;
            case "brown": return new Color(69, 29, 26);
            case "pink": return Color.PINK;
            case "orange": return Color.ORANGE;
            case "light green": return Color.GREEN;
            default: return null;
        }
    }

    public enum ComponentType {
        DECK,
        AREA,
        BOARD,
        BOARD_NODE,
        CARD,
        COUNTER,
        DICE,
        TOKEN
    }

    public enum GameResult {
        WIN(1),
        DRAW(0),
        LOSE(0),
        DISQUALIFY(-2),
        GAME_ONGOING(2),
        GAME_END(3);

        public final double value;

        GameResult(double value) {
            this.value = value;
        }
    }

    public static int indexOf (String[] array, String object) {
        for (int i = 0; i < array.length; i++) {
            if (object.equals(array[i])) {
                return i;
            }
        }
        return -1;
    }
    public static int indexOf (int[] array, int object) {
        for (int i = 0; i < array.length; i++) {
            if (object == array[i]) {
                return i;
            }
        }
        return -1;
    }

    public static void generatePermutations(int n, int[] elements, ArrayList<int[]> all) {
        if (n == 1) {
            all.add(elements.clone());
        } else {
            for(int i = 0; i < n-1; i++) {
                generatePermutations(n - 1, elements, all);
                if(n % 2 == 0) {
                    swap(elements, i, n-1);
                } else {
                    swap(elements, 0, n-1);
                }
            }
            generatePermutations(n - 1, elements, all);
        }
    }

    public static void swap(int[] input, int a, int b) {
        int tmp = input[a];
        input[a] = input[b];
        input[b] = tmp;
    }

    /**
     * Find a list of coordinates for neighbours of point at (x, y) in 2D grid of given width and height, with either
     * 8-way or 4-way connectivity.
     * @param x - x coordinate of point
     * @param y - y coordinate of point
     * @param width - width of grid
     * @param height - height of grid
     * @param way8 - if true, grid has 8-way connectivity, otherwise just 4.
     * @return List of Vector2D, coordinates for valid neighbours
     */
    public static java.util.List<Vector2D> getNeighbourhood(int x, int y, int width, int height, boolean way8) {
        List<Vector2D> neighbours = new ArrayList<>();

        // Add orthogonal neighbours
        if (x > 0) neighbours.add(new Vector2D(x-1, y));
        if (x < width-1) neighbours.add(new Vector2D(x+1, y));
        if (y > 0) neighbours.add(new Vector2D(x, y-1));
        if (y < height-1) neighbours.add(new Vector2D(x, y+1));

        // Add diagonal neighbours
        if (way8) {
            if (x > 0 && y > 0) neighbours.add(new Vector2D(x-1, y-1));
            if (x < width-1 && y < height-1) neighbours.add(new Vector2D(x+1, y+1));
            if (x > 0 && y < height-1) neighbours.add(new Vector2D(x-1, y+1));
            if (x < width-1 && y > 0) neighbours.add(new Vector2D(x+1, y-1));
        }
        return neighbours;
    }
}
