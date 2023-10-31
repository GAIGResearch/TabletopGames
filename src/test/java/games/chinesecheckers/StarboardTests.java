package games.chinesecheckers;

import core.Game;
import games.GameType;
import games.chinesecheckers.components.Peg;
import org.junit.Test;

import static org.junit.Assert.*;

public class StarboardTests {
    CCForwardModel fm = new CCForwardModel();
    Game game = GameType.ChineseCheckers.createGameInstance(6, new CCParameters());
    CCGameState state = (CCGameState) game.getGameState();

    @Test
    public void baseColoursTest(){
        CCGameState state = (CCGameState) game.getGameState();
        // Check purple nodes
        for (int i = 0; i <= 9; i++) {
            assertEquals("red", state.getStarBoard().getBoardNodes().get(i).getBaseColour().name());
        }
        // Check blue nodes
        int[] blueNodes = {19, 20, 21, 22, 32, 33, 34, 44, 45, 55};
        for (int index : blueNodes) {
            assertEquals("blue", state.getStarBoard().getBoardNodes().get(index).getBaseColour().name());
        }
        // Check yellow nodes
        int[] yellowNodes = {74, 84, 85, 95, 96, 97, 107, 108, 109, 110};
        for (int index : yellowNodes) {
            assertEquals("yellow", state.getStarBoard().getBoardNodes().get(index).getBaseColour().name());
        }
        // Check red nodes
        for (int i = 111; i <= 120; i++) {
            assertEquals("purple", state.getStarBoard().getBoardNodes().get(i).getBaseColour().name());
        }
        // Check orange nodes
        int[] orangeNodes = {65, 75, 76, 86, 87, 88, 98, 99, 100, 101};
        for (int index : orangeNodes) {
            assertEquals("orange", state.getStarBoard().getBoardNodes().get(index).getBaseColour().name());
        }
        // Check green nodes
        int[] greenNodes = {10, 11, 12, 13, 23, 24, 25, 35, 36, 46};
        for (int index : greenNodes) {
            assertEquals("green", state.getStarBoard().getBoardNodes().get(index).getBaseColour().name());
        }
    }

    @Test
    public void pegColoursTest(){
        if (state.getNPlayers() == 2) {
            for (int i = 0; i <= 9; i++) {
                assertEquals("Peg at index " + i + " should be PURPLE for a two-player game",
                        Peg.Colour.purple,
                        state.getStarBoard().getBoardNodes().get(i).getOccupiedPeg().getColour());
            }
            for (int i = 111; i <= 120; i++) {
                assertEquals("Peg at index " + i + " should be RED for a two-player game",
                        Peg.Colour.red,
                        state.getStarBoard().getBoardNodes().get(i).getOccupiedPeg().getColour());
            }
        }
        if (state.getNPlayers() == 3) {
            // Check for PURPLE pegs
            for (int i = 0; i <= 9; i++) {
                assertEquals("Peg at index " + i + " should be PURPLE for a three-player game",
                        Peg.Colour.purple,
                        state.getStarBoard().getBoardNodes().get(i).getOccupiedPeg().getColour());
            }
            // Check for YELLOW pegs
            int[] yellowIndices = {74, 84, 85, 95, 96, 97, 107, 108, 109, 110};
            for (int index : yellowIndices) {
                assertEquals("Peg at index " + index + " should be YELLOW for a three-player game",
                        Peg.Colour.yellow,
                        state.getStarBoard().getBoardNodes().get(index).getOccupiedPeg().getColour());
            }
            // Check for ORANGE pegs
            int[] orangeIndices = {65, 75, 76, 86, 87, 88, 98, 99, 100, 101};
            for (int index : orangeIndices) {
                assertEquals("Peg at index " + index + " should be ORANGE for a three-player game",
                        Peg.Colour.orange,
                        state.getStarBoard().getBoardNodes().get(index).getOccupiedPeg().getColour());
            }
        }
        if (state.getNPlayers() == 4) {
            // Check for PURPLE pegs
            for (int i = 0; i <= 9; i++) {
                assertEquals("Peg at index " + i + " should be PURPLE for a four-player game",
                        Peg.Colour.purple,
                        state.getStarBoard().getBoardNodes().get(i).getOccupiedPeg().getColour());
            }
            // Check for YELLOW pegs
            int[] yellowIndices = {74, 84, 85, 95, 96, 97, 107, 108, 109, 110};
            for (int index : yellowIndices) {
                assertEquals("Peg at index " + index + " should be YELLOW for a four-player game",
                        Peg.Colour.yellow,
                        state.getStarBoard().getBoardNodes().get(index).getOccupiedPeg().getColour());
            }
            // Check for RED pegs
            for (int i = 111; i <= 120; i++) {
                assertEquals("Peg at index " + i + " should be RED for a four-player game",
                        Peg.Colour.red,
                        state.getStarBoard().getBoardNodes().get(i).getOccupiedPeg().getColour());
            }
            // Check for GREEN pegs
            int[] greenIndices = {10, 11, 12, 13, 23, 24, 25, 35, 36, 46};
            for (int index : greenIndices) {
                assertEquals("Peg at index " + index + " should be GREEN for a four-player game",
                        Peg.Colour.green,
                        state.getStarBoard().getBoardNodes().get(index).getOccupiedPeg().getColour());
            }
        }
        if (state.getNPlayers() == 6) {
            // Check for PURPLE pegs
            for (int i = 0; i <= 9; i++) {
                assertEquals("Peg at index " + i + " should be PURPLE for a six-player game",
                        Peg.Colour.purple,
                        state.getStarBoard().getBoardNodes().get(i).getOccupiedPeg().getColour());
            }
            // Check for YELLOW pegs
            int[] yellowIndices = {10, 11, 12, 13, 23, 24, 25, 35, 36, 46};
            for (int index : yellowIndices) {
                assertEquals("Peg at index " + index + " should be YELLOW for a six-player game",
                        Peg.Colour.yellow,
                        state.getStarBoard().getBoardNodes().get(index).getOccupiedPeg().getColour());
            }
            // Check for RED pegs
            for (int i = 111; i <= 120; i++) {
                assertEquals("Peg at index " + i + " should be RED for a six-player game",
                        Peg.Colour.red,
                        state.getStarBoard().getBoardNodes().get(i).getOccupiedPeg().getColour());
            }
            // Check for ORANGE pegs
            int[] orangeIndices = {19, 20, 21, 22, 32, 33, 34, 44, 45, 55};
            for (int index : orangeIndices) {
                assertEquals("Peg at index " + index + " should be ORANGE for a six-player game",
                        Peg.Colour.orange,
                        state.getStarBoard().getBoardNodes().get(index).getOccupiedPeg().getColour());
            }
            // Check for GREEN pegs
            int[] greenIndices = {74, 84, 85, 95, 96, 97, 107, 108, 109, 110};
            for (int index : greenIndices) {
                assertEquals("Peg at index " + index + " should be GREEN for a six-player game",
                        Peg.Colour.green,
                        state.getStarBoard().getBoardNodes().get(index).getOccupiedPeg().getColour());
            }
            // Check for BLUE pegs
            int[] blueIndices = {65, 75, 76, 86, 87, 88, 98, 99, 100, 101};
            for (int index : blueIndices) {
                assertEquals("Peg at index " + index + " should be BLUE for a six-player game",
                        Peg.Colour.blue,
                        state.getStarBoard().getBoardNodes().get(index).getOccupiedPeg().getColour());
            }
        }
    }
}