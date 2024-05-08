package utilities;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;

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
        else if (a_min == a_max)
            return 0.0; // special case
        throw new IllegalArgumentException(String.format("Invalid args in Utils.normalise() - %.3f is not in range [%.3f, %.3f]", a_value, a_min, a_max));
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

    /**
     * we sample a uniform variable in [0, 1] and ascend the cdf to find the selection
     * exploreEpsilon is the percentage chance of taking a random action
     *
     * @param itemsAndValues A map keyed by the things to select (e.g. Actions or Integers), and their unnormalised values
     * @param rnd
     * @param <T>
     * @return
     */
    public static <T> T sampleFrom(Map<T, Double> itemsAndValues, double exploreEpsilon, Random rnd) {
        Map<T, Double> normalisedMap = Utils.normaliseMap(itemsAndValues);
        // we then add on the exploration bonus
        if (exploreEpsilon > 0.0) {
            double exploreBonus = exploreEpsilon / normalisedMap.size();
            normalisedMap = normalisedMap.entrySet().stream().collect(
                    toMap(Map.Entry::getKey, e -> e.getValue() * (1.0 - exploreEpsilon) + exploreBonus));
        }
        double cdfSample = rnd.nextDouble();
        double cdf = 0.0;
        for (T item : normalisedMap.keySet()) {
            cdf += normalisedMap.get(item);
            if (cdf >= cdfSample)
                return item;
        }
        throw new AssertionError("Should never get here!");
    }

    public static <T> T sampleFrom(Map<T, Double> itemsAndValues, double temperature, double exploreEpsilon, Random rnd) {
        double temp = Math.max(temperature, 0.001);
        // first we find the largest value, and subtract that from all values
        double maxValue = itemsAndValues.values().stream().mapToDouble(d -> d).max().orElse(0.0);
        Map<T, Double> tempModified = itemsAndValues.entrySet().stream().collect(
                toMap(Map.Entry::getKey, e -> Math.exp((e.getValue() - maxValue) / temp)));
        return sampleFrom(tempModified, exploreEpsilon, rnd);
    }

    public static double entropyOf(double... data) {
        double sum = Arrays.stream(data).sum();
        double[] normalised = Arrays.stream(data).map(d -> d / sum).toArray();
        return Arrays.stream(normalised).map(d -> -d * Math.log(d)).sum();
    }

    public static <T> Map<T, Double> normaliseMap(Map<T, ? extends Number> input) {
        int lessThanZero = (int) input.values().stream().filter(n -> n.doubleValue() < 0.0).count();
        if (lessThanZero > 0)
            throw new AssertionError("Probability has negative values!");
        double sum = input.values().stream().mapToDouble(Number::doubleValue).sum();
        if (sum == 0.0) {
            // the sum is zero, with no negative values. Hence all values are zero, and we return a uniform distribution.
            return input.keySet().stream().collect(toMap(key -> key, key -> 1.0 / input.size()));
        }
        return input.keySet().stream().collect(toMap(key -> key, key -> input.get(key).doubleValue() / sum));
    }

    public static double clamp(double value, double min, double max) {
        if (value > max) return max;
        if (value < min) return min;
        return value;
    }

    /**
     * This decays statistics by gamma
     *
     * @param pair
     * @param gamma
     * @return
     */
    public static Pair<Integer, Double> decay(Pair<Integer, Double> pair, double gamma) {
        if (gamma < 1.0 && gamma >= 0.0) {
            if (pair.a == 0) return new Pair<>(0, 0.0);
            double oldCount = pair.a;
            int newCount = (int) (oldCount * gamma);
            double newValue = pair.b * newCount / oldCount;
            return new Pair<>(newCount, newValue);
        }
        return pair;
    }

    public static <T> Map<T, Pair<Integer, Double>> decay(Map<T, Pair<Integer, Double>> map, double gamma) {
        return map.keySet().stream()
                .collect(toMap(key -> key, key -> decay(map.get(key), gamma)));
    }

    public static <T> T getArg(Object args, String name, T defaultValue) {
        if (args instanceof JSONObject) return getArg((JSONObject) args, name, defaultValue);
        else if (args instanceof String[]) return getArg((String[]) args, name, defaultValue);
        else throw new IllegalArgumentException("Unknown args type " + args.getClass());
    }

    @SuppressWarnings("unchecked")
    public static <T> T getArg(String[] args, String name, T defaultValue) {
        Optional<String> raw = Arrays.stream(args).filter(i -> i.toLowerCase().startsWith(name.toLowerCase() + "=")).findFirst();
        if (raw.isPresent()) {
            String[] temp = raw.get().split("=");
            if (temp.length > 1) { // if no value is specified, we just return the default value
                String rawString = temp[1];
                if (defaultValue instanceof Enum) {
                    T[] constants = (T[]) defaultValue.getClass().getEnumConstants();
                    for (T o : constants) {
                        if (o.toString().equals(rawString))
                            return o;
                    }
                } else if (defaultValue instanceof Integer) {
                    return (T) Integer.valueOf(rawString);
                } else if (defaultValue instanceof Double) {
                    return (T) Double.valueOf(rawString);
                } else if (defaultValue instanceof Boolean) {
                    return (T) Boolean.valueOf(rawString);
                } else if (defaultValue instanceof String) {
                    return (T) rawString;
                } else if (defaultValue instanceof Long) {
                    return (T) Long.valueOf(rawString);
                } else {
                    throw new AssertionError("Unexpected type of defaultValue : " + defaultValue.getClass());
                }
            } else {
                System.out.println("No value specified for " + name + ", using default value of " + defaultValue);
            }
        }
        return defaultValue;
    }

    public static <T> T getArg(JSONObject args, String name, T defaultValue) {
        if (args.containsKey(name)) {
            Object rawObject = args.get(name);
            if (defaultValue instanceof Enum) {
                T[] constants = (T[]) defaultValue.getClass().getEnumConstants();
                for (T o : constants) {
                    if (o.toString().equals(rawObject))
                        return o;
                }
            } else if (defaultValue instanceof Integer) {
                Integer number = (int) (long) rawObject;
                return (T) number;
            } else if (defaultValue instanceof Double || defaultValue instanceof Boolean || defaultValue instanceof String) {
                return (T) rawObject;
            } else {
                throw new AssertionError("Unexpected type of defaultValue : " + defaultValue.getClass());
            }
        }
        return defaultValue;
    }

    public static String createDirectory(String[] nestedDirectories) {
        String folder = "";
        boolean success = true;
        for (String nestedDir : nestedDirectories) {
            folder = folder + nestedDir + File.separator;
            File outFolder = new File(folder);
            if (!outFolder.exists()) {
                success = outFolder.mkdir();
            }
            if (!success)
                throw new AssertionError("Unable to create output directory" + outFolder.getAbsolutePath());
        }
        return folder;
    }

    public static String createDirectory(String fullDirectoryPath) {
        String[] nestedDirectories = fullDirectoryPath.split(Pattern.quote(File.separator));
        return createDirectory(nestedDirectories);
    }

    /**
     * Recursively computes combinations of numbers in an array, taken {r} at a time. Each combination is added into the
     * allData list.
     *
     * @param arr   - Input Array
     * @param data  - Temporary array to store current combination
     * @param start - Staring index in arr for current iteration
     * @param end   - Ending index in arr for current iteration
     * @param index - Current index in data
     * @param r     ---> Size of a combination
     */
    public static void combinationUtil(int[] arr, int[] data, int start, int end, int index, int r, ArrayList<int[]> allData) {
        if (index == r) {
            allData.add(data.clone());
            return;
        }
        if (allData.size() > 1000) return; // don't let the list get too big (1 million combinations is a lot!)

        for (int i = start; i <= end && end - i + 1 >= r - index; i++) {
            data[index] = arr[i];
            combinationUtil(arr, data, i + 1, end, index + 1, r, allData);
        }
    }
    public static void combinationUtil(Object[] arr, Object[] data, int start, int end, int index, int r, HashSet<Object[]> allData) {
        if (index == r) {
            allData.add(data.clone());
            return;
        }

        for (int i = start; i <= end && end - i + 1 >= r - index; i++) {
            data[index] = arr[i];
            combinationUtil(arr, data, i + 1, end, index + 1, r, allData);
        }
    }

    /**
     * Auxiliary function shortcut to generate combinations of numbers in an array, each of size r.
     *
     * @param arr - input array
     * @param r   - size of one combination
     * @return a list of all possible combinations of values (!not indexes)
     */
    public static ArrayList<int[]> generateCombinations(int[] arr, int r) {
        int[] data = new int[r];
        ArrayList<int[]> allData = new ArrayList<>();
        combinationUtil(arr, data, 0, arr.length - 1, 0, r, allData);
        return allData;
    }

    /**
     * Generate all combinations of objects in the given array, in sizes from min to max (capped 1 - array length)
     *
     * @param arr           - input array of objects, e.g. (Apple, Pear, Apple)
     * @param minSizeOutput - minimum size of output array, e.g. 1
     * @param maxSizeOutput - maximum size of output array, e.g. 2
     * @return - All combinations of objects in arrays of different sizes, e.g. (Apple), (Pear), (Apple, Pear), (Pear, Apple)
     */
    public static HashSet<Object[]> generateCombinations(Object[] arr, int minSizeOutput, int maxSizeOutput) {
        HashSet<Object[]> allData = new HashSet<>();
        if (minSizeOutput < 1) minSizeOutput = 1;
        if (maxSizeOutput > arr.length) maxSizeOutput = arr.length;
        for (int r = minSizeOutput; r <= maxSizeOutput; r++) {
            Object[] data = new Object[r];
            combinationUtil(arr, data, 0, arr.length - 1, 0, r, allData);
        }
        return allData;
    }

    /**
     * Returns a list of objects arrays, each one a combination of elements from the param
     * Example: input [[1, 2] [3] [4, 5]] ===> output [[1, 3, 4], [2, 3, 4], [1, 3, 5], [2, 3, 5]]
     * Algorithm from <a href="https://www.geeksforgeeks.org/combinations-from-n-arrays-picking-one-element-from-each-array/">here</a>
     *
     * @param arr A list of array objects to combine/
     * @return the combination of elements.
     */
    public static List<Object[]> generateCombinations(List<Object[]> arr) {
        ArrayList<Object[]> combinations = new ArrayList<>();

        // Number of arrays
        int n = arr.size();

        // To keep track of next element in each of the n arrays
        int[] indices = new int[n];

        // Initialize with first element's index
        for (int i = 0; i < n; i++) indices[i] = 0;

        while (true) {
            // Add current combination
            Object[] objs = new Object[n];
            for (int i = 0; i < n; i++) objs[i] = arr.get(i)[indices[i]];
            combinations.add(objs);

            // Find the rightmost array that has more elements left after the current element in that array
            int next = n - 1;
            while (next >= 0 && (indices[next] + 1 >= arr.get(next).length))
                next--;

            // No such array is found so no more combinations left
            if (next < 0)
                return combinations;

            // If found move to next element in that array
            indices[next]++;

            // For all arrays to the right of this array current index again points to first element
            for (int i = next + 1; i < n; i++)
                indices[i] = 0;
        }
    }


    public static Object searchEnum(Object[] enumConstants, String search) {
        for (Object obj : enumConstants) {
            if (obj.toString().compareToIgnoreCase(search) == 0) {
                return obj;
            }
        }
        return null;
    }

    public static <T extends Enum<?>> T searchEnum(Class<T> enumeration, String search) {
        for (T each : enumeration.getEnumConstants()) {
            if (each.name().compareToIgnoreCase(search) == 0) {
                return each;
            }
        }
        return null;
    }


    public static BufferedImage convertToType(BufferedImage sourceImage, int targetType) {
        BufferedImage image;

        // if the source image is already the target type, return the source image
        if (sourceImage.getType() == targetType)
            image = sourceImage;

            // otherwise create a new image of the target type and draw the new image
        else {
            image = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), targetType);
            image.getGraphics().drawImage(sourceImage, 0, 0, null);
        }

        return image;
    }

    public static BufferedImage componentToImage(Component component, int type) {
        BufferedImage img = new BufferedImage(component.getWidth(), component.getHeight(), type);
        Graphics2D g2d = img.createGraphics();
        component.printAll(g2d);
        g2d.dispose();
        return img;
    }

    /**
     * Accept a string, like aCamelString
     *
     * @param s - input string in camel case
     * @return string with each word separated by a space
     */
    public static String splitCamelCaseString(String s) {
        StringBuilder r = new StringBuilder();
        for (String w : s.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
            r.append(w).append(" ");
        }
        return r.toString().trim();
    }

    public static String getNumberSuffix(final int n) {
        if (n >= 11 && n <= 13) {
            return "th";
        }
        switch (n % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }


    public static double[] enumToOneHot(Enum<?> e) {
        return enumToOneHot(e, 1.0);
    }

    public static double[] enumToOneHot(Enum<?> e, double value) {
        double[] retValue = new double[e.getClass().getEnumConstants().length];
        retValue[e.ordinal()] = value;
        return retValue;
    }

    public static List<String> enumNames(Class<? extends Enum<?>> e) {
        return Arrays.stream(e.getEnumConstants()).map(Enum::name).collect(toList());
    }

    public static List<String> enumNames(Enum<?> e) {
        return enumNames((Class<? extends Enum<?>>) e.getClass());
    }



}
