package games.cantstop.gui;

import games.cantstop.CantStopGameState;
import games.cantstop.CantStopParameters;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class CantStopBoardView extends JComponent {

    // We need one large panel for the board
    CantStopParameters params;
    int spaceHeight = 35;
    int spaceWidth = 35;
    int discDiam = 20;
    int spaceInterval = 10;
    int margin = 20;
    int labelHeight = 30;
    int boardWidth = 500;
    int boardHeight = 500;
    int discOffset;
    Color[] playerColours = new Color[]{Color.GREEN, Color.RED, Color.BLUE, Color.YELLOW};

    int[][] markerPositions;
    int[] tempPositions;
    int[] completedColumns;
    int[] dice;
    int currentPlayer;

    public CantStopBoardView(CantStopGameState state) {
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(boardWidth, boardHeight));
        params = (CantStopParameters) state.getGameParameters();
        markerPositions = new int[state.getNPlayers()][13];
        tempPositions = new int[13];
        completedColumns = new int[13];
        Arrays.fill(completedColumns, -1);
        discOffset = 25 / (state.getNPlayers() + 1);
        dice = new int[params.DICE_NUMBER];
    }

    public synchronized void update(CantStopGameState state) {
        // here we update the details that paintComponent() will actually draw
        // we synchronize to ensure that we do not try to draw the board (in paintComponent)
        // while we are updating this data
        dice = state.getDice();
        currentPlayer = state.getCurrentPlayer();
        markerPositions = new int[state.getNPlayers()][13];
        completedColumns = new int[13];
        Arrays.fill(completedColumns, -1);
        for (int p = 0; p < state.getNPlayers(); p++) {
            for (int n = 2; n <= 12; n++) {
                markerPositions[p][n] = state.getMarkerPosition(n, p);
                if (markerPositions[p][n] == params.maxValue(n)) {
                    // completed!
                    completedColumns[n] = p;
                }
            }
        }
        tempPositions = new int[13];
        for (int n = 2; n <= 12; n++) {
            tempPositions[n] = state.getTemporaryMarkerPosition(n);
        }
    }


    @Override
    public synchronized void paintComponent(Graphics graphics) {
        // first draw the board
        Graphics2D g = (Graphics2D) graphics;
        g.setStroke(new BasicStroke(5));
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Dice Values : " + Arrays.toString(dice), margin, margin);
        for (int n = 2; n <= 12; n++) {
            g.setColor(Color.BLACK);
            int labelX = margin + n * spaceWidth + n * spaceInterval;
            int labelY = boardHeight - margin;
            g.drawString(String.format(" %2d", n), labelX, labelY);
            int maxValue = params.maxValue(n);
            if (completedColumns[n] > -1) {
                g.setColor(playerColours[completedColumns[n]]);
                int x = margin + n * spaceWidth + n * spaceInterval;
                int y = boardHeight - margin - labelHeight - maxValue * spaceHeight;
                g.fillRect(x, y, spaceWidth, spaceHeight * maxValue);
                continue;
            }
            for (int i = 1; i <= maxValue; i++) {
                int x = margin + n * spaceWidth + n * spaceInterval;
                int y = boardHeight - margin - i * spaceHeight - labelHeight;
                g.drawRect(x, y, spaceWidth, spaceHeight);
            }
            // now indicate temp marker position (if relevant)
            g.setColor(playerColours[currentPlayer]);
            if (tempPositions[n] > 0) {
                int x = margin + n * spaceWidth + n * spaceInterval;
                int y = boardHeight - margin - tempPositions[n] * spaceHeight - labelHeight;
                g.drawRect(x, y, spaceWidth, spaceHeight);
            }
            // now draw in the player permanent markers
            for (int p = 0; p < markerPositions.length; p++) {
                g.setColor(playerColours[p]);
                int pos = markerPositions[p][n];
                if (pos > 0) {
                    int x = margin + n * spaceWidth + n * spaceInterval + (p + 1) * discOffset;
                    int y = boardHeight - margin - (pos - 1) * spaceHeight - labelHeight - (p + 1) * discOffset - spaceHeight / 2;
                    g.fillOval(x, y, discDiam, discDiam);
                }
            }
        }

    }
}
