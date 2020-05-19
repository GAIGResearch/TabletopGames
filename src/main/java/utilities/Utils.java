package utilities;

import java.awt.*;
import java.util.ArrayList;

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
        BOARD,
        DECK,
        CARD,
        COUNTER,
        DICE,
        TOKEN
    }

    public enum GameResult {
        GAME_WIN(2),
        GAME_DRAW(1),
        GAME_ONGOING(0),
        GAME_LOSE(-1),
        GAME_END(3);

        public final int value;

        GameResult(int value) {
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

    private static void swap(int[] input, int a, int b) {
        int tmp = input[a];
        input[a] = input[b];
        input[b] = tmp;
    }
}
