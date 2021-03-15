package utilities;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public abstract class Utils {

    public static Color stringToColor(String c) {
        switch (c.toLowerCase()) {
            case "blue":
                return Color.BLUE;
            case "black":
                return Color.BLACK;
            case "yellow":
                return Color.YELLOW;
            case "red":
                return Color.RED;
            case "green":
                return new Color(30, 108, 47);
            case "white":
                return Color.WHITE;
            case "brown":
                return new Color(69, 29, 26);
            case "pink":
                return Color.PINK;
            case "orange":
                return Color.ORANGE;
            case "light green":
                return Color.GREEN;
            case "purple":
                return new Color(143, 77, 175);
            default:
                return null;
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

    /**
     * Finds index in array of String objects.
     *
     * @param array  - array of String
     * @param object - object to look for
     * @return - index of String object, -1 if not found
     */
    public static int indexOf(String[] array, String object) {
        for (int i = 0; i < array.length; i++) {
            if (object.equals(array[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds index of integer in array.
     *
     * @param array  - array of integers
     * @param object - integer to look for
     * @return - index of integer object, -1 if not found
     */
    public static int indexOf(int[] array, int object) {
        for (int i = 0; i < array.length; i++) {
            if (object == array[i]) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Generates all permutations of a given array of integers.
     *
     * @param n        - current index to search up to
     * @param elements - array with elements
     * @param all      - list where all permutations should be added
     */
    public static void generatePermutations(int n, int[] elements, ArrayList<int[]> all) {
        if (n == 1) {
            all.add(elements.clone());
        } else {
            for (int i = 0; i < n - 1; i++) {
                generatePermutations(n - 1, elements, all);
                if (n % 2 == 0) {
                    swap(elements, i, n - 1);
                } else {
                    swap(elements, 0, n - 1);
                }
            }
            generatePermutations(n - 1, elements, all);
        }
    }

    /**
     * Performs a swap of 2 elements in an integer array at given indexes. Modifies original array.
     *
     * @param input - input array
     * @param a     - index of first element
     * @param b     - index of second element
     */
    public static void swap(int[] input, int a, int b) {
        int tmp = input[a];
        input[a] = input[b];
        input[b] = tmp;
    }

    /**
     * Find a list of coordinates for neighbours of point at (x, y) in 2D grid of given width and height, with either
     * 8-way or 4-way connectivity.
     *
     * @param x      - x coordinate of point
     * @param y      - y coordinate of point
     * @param width  - width of grid
     * @param height - height of grid
     * @param way8   - if true, grid has 8-way connectivity, otherwise just 4.
     * @return List of Vector2D, coordinates for valid neighbours
     */
    public static java.util.List<Vector2D> getNeighbourhood(int x, int y, int width, int height, boolean way8) {
        List<Vector2D> neighbours = new ArrayList<>();

        // Add orthogonal neighbours
        if (x > 0) neighbours.add(new Vector2D(x - 1, y));
        if (x < width - 1) neighbours.add(new Vector2D(x + 1, y));
        if (y > 0) neighbours.add(new Vector2D(x, y - 1));
        if (y < height - 1) neighbours.add(new Vector2D(x, y + 1));

        // Add diagonal neighbours
        if (way8) {
            if (x > 0 && y > 0) neighbours.add(new Vector2D(x - 1, y - 1));
            if (x < width - 1 && y < height - 1) neighbours.add(new Vector2D(x + 1, y + 1));
            if (x > 0 && y < height - 1) neighbours.add(new Vector2D(x - 1, y + 1));
            if (x < width - 1 && y > 0) neighbours.add(new Vector2D(x + 1, y - 1));
        }
        return neighbours;
    }

    /**
     * Normalizes a value in range [0, 1] given its minimum and maximum possible.
     *
     * @param a_value - value to normalize
     * @param a_min   - minimum possible
     * @param a_max   - maximum possible
     * @return - normalized value
     */
    public static double normalise(double a_value, double a_min, double a_max) {
        if (a_min < a_max)
            return (a_value - a_min) / (a_max - a_min);
        else    // if bounds are invalid, then return same value
            return a_value;
    }

    /**
     * Applies random noise to input.
     *
     * @param input   - value to apply noise to.
     * @param epsilon - how much should the noise weigh in returned value.
     * @param random  - how much noise should be applied.
     * @return - new value with noise applied.
     */
    public static double noise(double input, double epsilon, double random) {
        return (input + epsilon) * (1.0 + epsilon * (random - 0.5));
    }

    public static double entropyOf(double... data) {
        double sum = Arrays.stream(data).sum();
        double[] normalised = Arrays.stream(data).map(d -> d / sum).toArray();
        return Arrays.stream(normalised).map(d -> -d * Math.log(d)).sum();
    }

    public static <T> Map<T, Double> normaliseMap(Map<T, ? extends Number> input) {
        int lessThanZero = (int) input.values().stream().filter(n -> n.doubleValue() < 0.0).count();
        if (lessThanZero > 0) throw new AssertionError("Probability has negative values!");
        double sum = input.values().stream().mapToDouble(Number::doubleValue).sum();
        if (sum == 0.0) {
            // the sum is zero, with no negative values. Hence all values are zero, and we return a uniform distribution.
            return input.keySet().stream().collect(toMap(key -> key, key -> 1.0 / input.size()));
        }
        return input.keySet().stream().collect(toMap(key -> key, key -> input.get(key).doubleValue() / sum));
    }

    @SuppressWarnings("unchecked")
    public static <T> T getArg(String[] args, String name, T defaultValue) {
        Optional<String> raw = Arrays.stream(args).filter(i -> i.toLowerCase().startsWith(name.toLowerCase() + "=")).findFirst();
        if (raw.isPresent()) {
            String rawString = raw.get().split("=")[1];
            if (defaultValue instanceof Integer) {
                return (T) Integer.valueOf(rawString);
            } else if (defaultValue instanceof Double) {
                return (T) Double.valueOf(rawString);
            } else if (defaultValue instanceof Boolean) {
                return (T) Boolean.valueOf(rawString);
            } else if (defaultValue instanceof String) {
                return (T) rawString;
            } else {
                throw new AssertionError("Unexpected type of defaultValue : " + defaultValue.getClass());
            }
        }
        return defaultValue;
    }

    public static JSONObject loadJSONFile(String fileName) {
        try {
            FileReader reader = new FileReader(fileName);
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(reader);
        } catch (IOException | ParseException e) {
            throw new AssertionError("Error processing file " + fileName + " : " + e.getMessage() + " : " +e.toString());
        }
    }

    public static <T extends Enum<?>> T searchEnum(Class<T> enumeration, String search) {
        for (T each : enumeration.getEnumConstants()) {
            if (each.name().compareToIgnoreCase(search) == 0) {
                return each;
            }
        }
        return null;
    }
}
