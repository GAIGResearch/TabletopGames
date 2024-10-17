package games.connect4;

import core.AbstractGameState;
import core.components.GridBoard;
import core.components.Token;
import players.heuristics.AbstractStateFeature;

import java.util.Objects;

public class Connect4StateFeatures extends AbstractStateFeature {

    String[] localNames = new String[]{"One_Token", "Opponent_One_Token", "Two_Token", "Opponent_Two_Token",
            "Three_Token", "Opponent_Three_Token", "Four_Token", "Opponent_Four_Token"};

    @Override
    protected double maxScore() {
        return 1.0;
    }

    @Override
    protected double maxRounds() {
        return 100.0;
    }

    @Override
    protected String[] localNames() {
        return localNames;
    }

    @Override
    protected double[] localFeatureVector(AbstractGameState gs, int playerID) {
        Connect4GameState state = (Connect4GameState) gs;
        GridBoard<Token> gridBoard = state.gridBoard;
        int width = gridBoard.getWidth();
        int height = gridBoard.getHeight();
        double[] retValue = new double[localNames.length];

        String playerChar = Connect4Constants.playerMapping.get(playerID).getTokenType();

        // Count 1 token
        int[][] directions = {{0, 1}, {0, -1}, {-1, 0}, {1, 0}};
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                String ownerChar = gridBoard.getElement(x, y).getTokenType();
                if (Objects.equals(ownerChar, Connect4Constants.emptyCell)) {
                    continue;
                }

                int ownerCount = 0;
                for (int[] direction : directions) {
                    if (isInBound((x + direction[0]), y + direction[1], width, height)) {
                        if (Objects.equals(gridBoard.getElement(x + direction[0], y + direction[1]).getTokenType(), ownerChar)) {
                            ownerCount++;
                        }
                    }
                }

                if (ownerCount == 0) {
                    if (Objects.equals(ownerChar, playerChar)) {
                        retValue[0]++;
                    } else {
                        retValue[1]++;
                    }
                }
            }
        }

        // Pattern Matching
        // down
        int x_direction = 0;
        int y_direction = 1;
        pattern_match(gridBoard, width, height, retValue, playerChar, x_direction, y_direction);

        // right
        x_direction = 1;
        y_direction = 0;
        pattern_match(gridBoard, width, height, retValue, playerChar, x_direction, y_direction);

        // left-down
        x_direction = -1;
        y_direction = 1;
        pattern_match(gridBoard, width, height, retValue, playerChar, x_direction, y_direction);

        // right-down
        x_direction = 1;
        y_direction = 1;
        pattern_match(gridBoard, width, height, retValue, playerChar, x_direction, y_direction);

        return retValue;
    }

    private void pattern_match(GridBoard<Token> gridBoard, int width, int height, double[] retValue, String playerChar, int x_direction, int y_direction) {
        int[][] visitedMap = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Skip the visited position
                if (visitedMap[x][y] == 1) {
                    continue;
                }

                // Set current position as visited
                int cur_x = x;
                int cur_y = y;
                visitedMap[cur_x][cur_y] = 1;

                // Skip the empty Cell
                String ownerChar = gridBoard.getElement(cur_x, cur_y).getTokenType();
                if (Objects.equals(ownerChar, Connect4Constants.emptyCell)) {
                    continue;
                }

                // Get the number of token continuous link together from current position
                int link = 1;
                int new_x = cur_x + x_direction;
                int new_y = cur_y + y_direction;
                while (isInBound(new_x, new_y, width, height) && Objects.equals(gridBoard.getElement(new_x, new_y).getTokenType(), ownerChar)) {
                    cur_x = new_x;
                    cur_y = new_y;
                    visitedMap[cur_x][cur_y] = 1;

                    link++;

                    new_x = cur_x + x_direction;
                    new_y = cur_y + y_direction;
                }

                if (link == 1) {
                    // right location (Oxox)
                    int x_next = x + 2 * x_direction;
                    int y_next = y + 2 * y_direction;
                    if (isInBound(x_next, y_next, width, height) && Objects.equals(gridBoard.getElement(x_next, y_next).getTokenType(), ownerChar)) {
                        if (!isInBound(x_next + x_direction, y_next + y_direction, width, height)) {
                            retValue[getRetValueIdx(link + 1, ownerChar, playerChar)]++;
                        } else if (!Objects.equals(gridBoard.getElement(x_next + x_direction, y_next + y_direction).getTokenType(), ownerChar)) {
                            retValue[getRetValueIdx(link + 1, ownerChar, playerChar)]++;
                        }

                    }
                    continue;
                }

                // Check the special case for 3 token: OoXo or oXOo or oXOoXo
                if (link == 2) {
                    // left location (oXOo)
                    int x_prev = x - 2 * x_direction;
                    int y_prev = y - 2 * y_direction;
                    // right location (OoXo)
                    int x_next = x + 3 * x_direction;
                    int y_next = y + 3 * y_direction;
                    if (isInBound(x_prev, y_prev, width, height) && Objects.equals(gridBoard.getElement(x_prev, y_prev).getTokenType(), ownerChar)) {
                        retValue[getRetValueIdx(link + 1, ownerChar, playerChar)]++;
                    } else if (isInBound(x_next, y_next, width, height) && Objects.equals(gridBoard.getElement(x_next, y_next).getTokenType(), ownerChar)) {
                        retValue[getRetValueIdx(link + 1, ownerChar, playerChar)]++;
                    }
                }

                retValue[getRetValueIdx(link, ownerChar, playerChar)]++;
            }
        }
    }


    private boolean isInBound(int x, int y, int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    private int getRetValueIdx(int link, String ownerChar, String playerChar) {
        if (link == 2) {
            if (Objects.equals(ownerChar, playerChar)) {
                return 2;
            } else {
                return 3;
            }
        } else if (link == 3) {
            if (Objects.equals(ownerChar, playerChar)) {
                return 4;
            } else {
                return 5;
            }
        } else {
            if (Objects.equals(ownerChar, playerChar)) {
                return 6;
            } else {
                return 7;
            }
        }
    }

}
