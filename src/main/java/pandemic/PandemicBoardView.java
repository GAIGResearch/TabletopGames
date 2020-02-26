package pandemic;

import components.*;
import content.*;
import core.Game;
import core.GameState;
import javafx.util.Pair;
import utilities.Hash;
import utilities.ImageIO;
import utilities.Utils;
import utilities.Vector2D;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

import static pandemic.PandemicCardView.drawCard;
import static pandemic.PandemicForwardModel.playersBNHash;

public class PandemicBoardView extends JComponent {
    //TODO: images for tokens?
    
    private Image background;
    private Board board;
    private Game game;
    private int width;
    private int height;
    int nodeSize = 20;
    int researchStationSize = 10;
    int playerPawnSize = 10;
    int diseaseCubeSize = 10;
    int diseaseCubeDistance = 2;
    int counterWidth = 20, counterHeight = 20;

    Pair<Integer, Integer>[] infectionPositions = new Pair[]{
            new Pair<>(755, 180),
            new Pair<>(795, 180),
            new Pair<>(835, 180),
            new Pair<>(875, 180),
            new Pair<>(915, 180),
            new Pair<>(955, 180),
            new Pair<>(995, 180)
    };
    Pair<Integer, Integer>[] outbreakPositions = new Pair[]{
            new Pair<>(75, 450),
            new Pair<>(120, 495),
            new Pair<>(75, 530),
            new Pair<>(120, 565),
            new Pair<>(75, 600),
            new Pair<>(120, 630),
            new Pair<>(75, 665),
            new Pair<>(120, 700),
            new Pair<>(75, 730)
    };

    Pair<Integer, Integer> infectionDiscardPosition = new Pair<>(915, 50);
    Pair<Integer, Integer> playerDiscardPosition = new Pair<>(880, 625);
    Pair<Integer, Integer>[] diseaseMarkerPositions = new Pair[]{
            new Pair<>(395, 775),
            new Pair<>(450, 775),
            new Pair<>(510, 775),
            new Pair<>(560, 775)
    };

    public PandemicBoardView(Game g, String backgroundPath) {
        this.board = g.findBoard("Cities");
        this.game = g;
        this.background = ImageIO.GetInstance().getImage(backgroundPath);
        width = background.getWidth(null);
        height = background.getHeight(null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawBoard((Graphics2D) g);
    }

    private void drawBoard(Graphics2D g) {
        int nPlayers = game.getPlayers().size();
        GameState gs = game.getGameState();
        int fontSize = g.getFont().getSize();

        // Draw board background
        g.drawImage(background, 0, 0, null, null);

        // Draw nodes
        for (BoardNode b : board.getBoardNodes()) {
            Vector2D pos = ((PropertyVector2D) b.getProperty(Hash.GetInstance().hash("coordinates"))).values;
//            g.setColor(Utils.stringToColor(((PropertyColor) b.getProperty(Hash.GetInstance().hash("color"))).valueStr));
//            g.fillOval(pos.getX() - nodeSize /2, pos.getY() - nodeSize /2, nodeSize, nodeSize);

            // Check if a research stations is here, draw just underneath the node
            PropertyBoolean isStation = (PropertyBoolean) b.getProperty(Hash.GetInstance().hash("researchStation"));
            if (isStation.value) {
                // Draw research station here
                g.setColor(Color.WHITE);
                g.fillRect(pos.getX() - researchStationSize /2, pos.getY() + nodeSize/2, researchStationSize, researchStationSize);
                g.setColor(Color.black);
                g.drawRect(pos.getX() - researchStationSize /2, pos.getY() + nodeSize/2, researchStationSize, researchStationSize);
                g.drawString("R", pos.getX() - researchStationSize /2 + 2, pos.getY() + nodeSize/2 + fontSize/2 + researchStationSize/2);
            }

            // Check if there are players here
            PropertyIntArrayList prop = (PropertyIntArrayList) b.getProperty(playersBNHash);
            ArrayList<Integer> players = prop.getValues();
            for (int p: players) {
                // This player is here, draw them just above the node
                // Find color of player
                Card playerCard = (Card) gs.getAreas().get(p).getComponent(Hash.GetInstance().hash("playerCard"));
                PropertyColor color = (PropertyColor) playerCard.getProperty(Hash.GetInstance().hash("color"));
                g.setColor(Utils.stringToColor(color.valueStr));
                g.fillOval(pos.getX() + nPlayers * playerPawnSize / 2 - p * playerPawnSize - playerPawnSize /2, pos.getY() - nodeSize /2 - playerPawnSize /2, playerPawnSize, playerPawnSize);
                g.setColor(Color.black);
                g.drawOval(pos.getX() + nPlayers * playerPawnSize / 2 - p * playerPawnSize - playerPawnSize /2, pos.getY() - nodeSize /2 - playerPawnSize /2, playerPawnSize, playerPawnSize);
            }

            // Draw disease cubes on top of the node
            int[] array = ((PropertyIntArray) b.getProperty(Hash.GetInstance().hash("infection"))).getValues();
            int total = 0;
            for (int cube: array) {
                total += cube;
            }
            int idx = 0;
            int maxX = pos.getX() + (total + diseaseCubeDistance) * diseaseCubeSize / 2;
            for (int cube: array) {
                Color cubeColor = Utils.stringToColor(PandemicGameState.colors[cube]);
                for (int i = 0; i < cube; i++) {
                    g.setColor(cubeColor);
                    g.fillRect(maxX - idx * (diseaseCubeSize + diseaseCubeDistance) - diseaseCubeSize /2, pos.getY() - diseaseCubeSize /2, diseaseCubeSize, diseaseCubeSize);
                    g.setColor(Color.black);
                    g.drawRect(maxX - idx * (diseaseCubeSize + diseaseCubeDistance) - diseaseCubeSize /2, pos.getY() - diseaseCubeSize /2, diseaseCubeSize, diseaseCubeSize);
                    idx++;
                }
            }
        }

        // Draw infection rate marker
        Counter ir = game.findCounter("Infection Rate");
        Pair<Integer, Integer> pos = infectionPositions[ir.getValue()];
        g.drawImage(ImageIO.GetInstance().getImage("data/infectionRate.png"), pos.getKey(), pos.getValue(), null, null);

        // Draw outbreak marker
        Counter ob = game.findCounter("Outbreaks");
        pos = outbreakPositions[ob.getValue()];
        g.drawImage(ImageIO.GetInstance().getImage("data/outbreakMarker.png"), pos.getKey(), pos.getValue(), null, null);

        // Discard piles
        Deck pDiscard = game.findDeck("Player Deck Discard");
        if (pDiscard != null) {
            Card cP = pDiscard.draw();
            if (cP != null) {
                drawCard(g, 100, 50, cP, null, playerDiscardPosition.getKey(), playerDiscardPosition.getValue());
            }
        }
        Deck iDiscard = game.findDeck("Infection Discard");
        if (iDiscard != null) {
            Card cI = iDiscard.draw();
            if (cI != null) {
                drawCard(g, 100, 50, cI, null, infectionDiscardPosition.getKey(), infectionDiscardPosition.getValue());
            }
        }

        // Disease markers
        int dmy = game.findCounter("Disease yellow").getValue();
        drawCounter(g, dmy, Color.yellow, 0);
        int dmr = game.findCounter("Disease red").getValue();
        drawCounter(g, dmr, Color.red, 1);
        int dmb = game.findCounter("Disease blue").getValue();
        drawCounter(g, dmb, Color.blue, 2);
        int dmk = game.findCounter("Disease black").getValue();
        drawCounter(g, dmk, Color.black, 3);

    }

    private void drawCounter(Graphics2D g, int value, Color color, int idx) {
        if (value > 0) {
            Pair<Integer, Integer> pos = diseaseMarkerPositions[idx];
            g.setColor(color);
            g.fillOval(pos.getKey(), pos.getValue(), counterWidth, counterHeight);
            if (value == 2) {
                g.setColor(Color.white);
                g.drawLine(pos.getKey(), pos.getValue(), pos.getKey() + counterWidth, pos.getValue() + counterHeight);
            }
            g.setColor(Color.black);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
}
