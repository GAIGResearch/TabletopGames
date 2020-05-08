package games.pandemic.gui;

import core.components.*;
import core.content.*;
import core.AbstractGameState;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import utilities.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static games.pandemic.PandemicConstants.*;
import static games.pandemic.gui.PandemicCardView.drawCard;
import static games.pandemic.gui.PandemicCardView.drawDeckBack;
import static utilities.CoreConstants.*;

public class PandemicBoardView extends JComponent {
    //TODO: images for tokens?
    
    private Image background;
    private IBoard board;
    private int width;
    private int height;

    PandemicGameState gameState;
    double scale = 0.75;

    private int cardWidth = (int)(scale * 100);
    private int cardHeight = (int)(scale * 50);
    int nodeSize = (int)(scale * 20);
    int researchStationSize = (int)(scale * 10);
    int playerPawnSize = (int)(scale * 10);
    int diseaseCubeSize = (int)(scale * 10);
    int diseaseCubeDistance = (int)(scale * 2);
    int counterWidth = (int)(scale * 20), counterHeight = (int)(scale * 20);
    int strokeWidth = (int)(scale * 2);

    Point2D[] infectionPositions = new Point2D[]{
            new Point2D.Double((int)(scale * 755), (int)(scale * 180)),
            new Point2D.Double((int)(scale * 795), (int)(scale * 180)),
            new Point2D.Double((int)(scale * 835), (int)(scale * 180)),
            new Point2D.Double((int)(scale * 875), (int)(scale * 180)),
            new Point2D.Double((int)(scale * 915), (int)(scale * 180)),
            new Point2D.Double((int)(scale * 955), (int)(scale * 180)),
            new Point2D.Double((int)(scale * 995), (int)(scale * 180))
    };
    Point2D[] outbreakPositions = new Point2D[]{
            new Point2D.Double((int)(scale * 75), (int)(scale *  450)),
            new Point2D.Double((int)(scale * 120), (int)(scale *  495)),
            new Point2D.Double((int)(scale * 75), (int)(scale * 530)),
            new Point2D.Double((int)(scale * 120), (int)(scale *  565)),
            new Point2D.Double((int)(scale * 75), (int)(scale * 600)),
            new Point2D.Double((int)(scale * 120), (int)(scale *  630)),
            new Point2D.Double((int)(scale * 75), (int)(scale *  665)),
            new Point2D.Double((int)(scale * 120), (int)(scale *  700)),
            new Point2D.Double((int)(scale * 75), (int)(scale *  730))
    };
    Point2D[] diseaseMarkerPositions = new Point2D[]{
            new Point2D.Double((int)(scale * 395), (int)(scale * 775)),
            new Point2D.Double((int)(scale * 450), (int)(scale * 775)),
            new Point2D.Double((int)(scale * 510), (int)(scale * 775)),
            new Point2D.Double((int)(scale * 560), (int)(scale * 775))
    };

    // Clickable locations
    Rectangle infectionDeckLocation = new Rectangle((int)(scale * 800), (int)(scale * 50), cardWidth, cardHeight);
    Rectangle infectionDiscardDeckLocation = new Rectangle((int)(scale * 915), (int)(scale * 50), cardWidth, cardHeight);
    Rectangle playerDiscardDeckLocation = new Rectangle((int)(scale * 880), (int)(scale * 625), cardWidth, cardHeight);
    Rectangle plannerDeckLocation = new Rectangle((int)(scale * 1000), (int)(scale * 625), cardWidth, cardHeight);
    HashMap<String, Rectangle> boardNodeLocations;
    Rectangle[] playerLocations;

    HashMap<String, Rectangle> highlights;
    int maxHighlights = 3;

    public PandemicBoardView(AbstractGameState gs, String backgroundPath) {
        gameState = (PandemicGameState) gs;
        this.board = ((PandemicGameState) gs).getData().findBoard("Cities");
        this.background = ImageIO.GetInstance().getImage(backgroundPath);  // todo: scale down
        width = (int)(background.getWidth(null) * scale);
        height = (int)(background.getHeight(null) * scale);

        boardNodeLocations = new HashMap<>();
        playerLocations = new Rectangle[gs.getNPlayers()];
        highlights = new HashMap<>();

        List<BoardNode> bList = board.getBoardNodes();
        for (BoardNode b : bList) {
            Vector2D poss = ((PropertyVector2D) b.getProperty(coordinateHash)).values;
            Vector2D pos = new Vector2D((int)(poss.getX()*scale), (int)(poss.getY()*scale));
            boardNodeLocations.put(((PropertyString) b.getProperty(nameHash)).value,
                    new Rectangle(pos.getX() - nodeSize / 2, pos.getY() - nodeSize / 2, nodeSize, nodeSize));
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1 || highlights.size() >= maxHighlights) {
                    highlights.clear();
                    return;
                }
                if (infectionDeckLocation.contains(e.getPoint())) {
                    highlights.put("infectionDeck", infectionDeckLocation);
                } else if (infectionDiscardDeckLocation.contains(e.getPoint())) {
                    highlights.put("infectionDiscard", infectionDiscardDeckLocation);
                } else if (playerDiscardDeckLocation.contains(e.getPoint())) {
                    highlights.put("playerDiscard", playerDiscardDeckLocation);
                } else if (plannerDeckLocation.contains(e.getPoint())) {
                    highlights.put("plannerDeck", plannerDeckLocation);
                } else {
                    for (int i = 0; i < playerLocations.length; i++) {
                        if (playerLocations[i] != null && playerLocations[i].contains(e.getPoint())) {
                            highlights.put("player " + i, playerLocations[i]);
                            break;
                        }
                    }
                    for (Map.Entry<String, Rectangle> en: boardNodeLocations.entrySet()) {
                        if (en.getValue().contains(e.getPoint())) {
                            highlights.put("BN " + en.getKey(), en.getValue());
                            break;
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.black);
        drawBoard(g2);
        if (highlights.size() > 0) {
            Stroke s = g2.getStroke();
            g2.setStroke(new BasicStroke(strokeWidth));
            g2.setColor(Color.CYAN);
            for (Map.Entry<String, Rectangle> e: highlights.entrySet()) {
                Rectangle highlight = e.getValue();
                g2.drawRect(highlight.x - strokeWidth / 2,
                        highlight.y - strokeWidth / 2,
                        highlight.width + strokeWidth,
                        highlight.height + strokeWidth);
            }
            g2.setStroke(s);
        }
    }

    private void drawBoard(Graphics2D g) {
        int fSize = g.getFont().getSize();
        int fontSize = (int)(scale * fSize);
        g.setFont(new Font(g.getFont().getFontName(), Font.PLAIN, fontSize));
        int nPlayers = gameState.getNPlayers();

        // Draw board background
        drawImage(g, background, 0, 0);

        // Draw nodes
        java.util.List<BoardNode> bList = board.getBoardNodes();
        for (BoardNode b : bList) {
            Vector2D poss = ((PropertyVector2D) b.getProperty(coordinateHash)).values;
            Vector2D pos = new Vector2D((int)(poss.getX()*scale), (int)(poss.getY()*scale));
//            g.setColor(Utils.stringToColor(((PropertyColor) b.getProperty(Hash.GetInstance().hash("color"))).valueStr));
//            g.fillOval(pos.getX() - nodeSize /2, pos.getY() - nodeSize /2, nodeSize, nodeSize);

            // Check if a research stations is here, draw just underneath the node
            PropertyBoolean isStation = (PropertyBoolean) b.getProperty(researchStationHash);
            if (isStation.value) {
                // Draw research station here
                g.setColor(Color.WHITE);
                g.fillRect(pos.getX() - researchStationSize/2, pos.getY() + nodeSize/2, researchStationSize, researchStationSize);
                g.setColor(Color.black);
                g.drawRect(pos.getX() - researchStationSize/2, pos.getY() + nodeSize/2, researchStationSize, researchStationSize);
                g.drawString("R", pos.getX() - researchStationSize/2 + 2, pos.getY() + nodeSize/2 + fontSize/2 + researchStationSize/2);
            }

            // Check if there are players here
            PropertyIntArrayList prop = (PropertyIntArrayList) b.getProperty(playersHash);
            ArrayList<Integer> players = prop.getValues();
            for (int i = 0; i < players.size(); i++) {
                // This player is here, draw them just above the node
                // Find color of player
                int p = players.get(i);
                Card playerCard = (Card) gameState.getComponent(PandemicConstants.playerCardHash, p);
                PropertyColor color = (PropertyColor) playerCard.getProperty(colorHash);
                g.setColor(Utils.stringToColor(color.valueStr));
                int x = pos.getX() + nPlayers * playerPawnSize / 2 - p * playerPawnSize - playerPawnSize /2;
                int y = pos.getY() - nodeSize /2 - playerPawnSize /2;
                g.fillOval(x, y, playerPawnSize, playerPawnSize);
                g.setColor(Color.black);
                g.drawOval(x, y, playerPawnSize, playerPawnSize);
                playerLocations[p] = new Rectangle(x, y, playerPawnSize, playerPawnSize);
            }

            // Draw disease cubes on top of the node
            int[] array = ((PropertyIntArray) b.getProperty(infectionHash)).getValues();
            int total = 0;
            for (int cube: array) {
                total += cube;
            }
            int idx = 0;
            int maxX = pos.getX() + (total + diseaseCubeDistance) * diseaseCubeSize / 4;
            for (int c = 0; c < array.length; c++) {
                int cube = array[c];
                Color cubeColor = Utils.stringToColor(PandemicConstants.colors[c]);
                for (int i = 0; i < cube; i++) {
                    g.setColor(cubeColor);
                    g.fillRect(maxX - idx * (diseaseCubeSize + diseaseCubeDistance) - diseaseCubeSize/2, pos.getY() - diseaseCubeSize /2, diseaseCubeSize, diseaseCubeSize);
                    g.setColor(Color.white);
                    if (cubeColor != null && cubeColor.equals(Color.yellow)) g.setColor(Color.black);  // Contrasting outline
                    g.drawRect(maxX - idx * (diseaseCubeSize + diseaseCubeDistance) - diseaseCubeSize/2, pos.getY() - diseaseCubeSize /2, diseaseCubeSize, diseaseCubeSize);
                    idx++;
                }

            }
        }

        // Draw infection rate marker
        Counter infectionRateCounter = (Counter) gameState.getComponent(PandemicConstants.infectionRateHash);
        Point2D pos = infectionPositions[infectionRateCounter.getValue()];
        drawImage(g, ImageIO.GetInstance().getImage("data/infectionRate.png"), (int)pos.getX(), (int)pos.getY());

        // Draw outbreak marker
        Counter outbreakCounter = (Counter) gameState.getComponent(PandemicConstants.outbreaksHash);
        pos = outbreakPositions[outbreakCounter.getValue()];
        drawImage(g, ImageIO.GetInstance().getImage("data/outbreakMarker.png"), (int)pos.getX(), (int)pos.getY());

        // Decks
        Deck<Card> playerDiscardDeck = (Deck<Card>) gameState.getComponent(PandemicConstants.playerDeckDiscardHash);
        if (playerDiscardDeck != null) {
            Card cP = playerDiscardDeck.peek();
            if (cP != null) {
                drawCard(g, cP, null, playerDiscardDeckLocation);
            }
        }
        Deck<Card> infectionDiscardDeck = (Deck<Card>) gameState.getComponent(PandemicConstants.infectionDiscardHash);
        if (infectionDiscardDeck != null) {
            Card cI = infectionDiscardDeck.peek();
            if (cI != null) {
                drawCard(g, cI, null, infectionDiscardDeckLocation);
            }
        }
        Deck<Card> plannerDeck = (Deck<Card>) gameState.getComponent(plannerDeckHash);
        if (plannerDeck != null) {
            Card cI = plannerDeck.peek();
            if (cI != null) {
                drawCard(g, cI, null, plannerDeckLocation);
            }
        }
        Deck<Card> infectionDeck = (Deck<Card>) gameState.getComponent(infectionHash);
        if (infectionDeck != null) {
            drawDeckBack(g, "Infections", null, infectionDeckLocation);
        }

        // Disease markers
        Counter yC = (Counter) gameState.getComponent(Hash.GetInstance().hash("Disease yellow"));
        drawCounter(g, yC.getValue(), Color.yellow, 0);

        Counter rC = (Counter) gameState.getComponent(Hash.GetInstance().hash("Disease red"));
        drawCounter(g, rC.getValue(), Color.red, 1);

        Counter bC = (Counter) gameState.getComponent(Hash.GetInstance().hash("Disease blue"));
        drawCounter(g, bC.getValue(), Color.blue, 2);

        Counter bkC = (Counter) gameState.getComponent(Hash.GetInstance().hash("Disease black"));
        drawCounter(g, bkC.getValue(), Color.black, 3);

    }

    private void drawImage(Graphics2D g, Image img, int x, int y) {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        g.drawImage(img, x, y, (int) (w*scale), (int) (h*scale), null);
    }

    private void drawCounter(Graphics2D g, int value, Color color, int idx) {
        if (value > 0) {
            Point2D pos = diseaseMarkerPositions[idx];
            g.setColor(color);
            g.fillOval((int)pos.getX(), (int)pos.getY(), counterWidth, counterHeight);
            if (value == 2) {
                g.setColor(Color.white);
                g.drawLine((int)pos.getX(), (int)pos.getY(), (int)pos.getX() + counterWidth, (int)pos.getY() + counterHeight);
            }
            g.setColor(Color.black);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public HashMap<String, Rectangle> getHighlights() {
        return highlights;
    }
}
