package games.pandemic.gui;

import core.components.*;
import core.properties.*;
import core.AbstractGameState;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicParameters;
import utilities.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

import static gui.views.DeckView.drawDeck;
import static games.pandemic.PandemicConstants.*;
import static games.pandemic.gui.PandemicCardView.drawCard;
import static core.CoreConstants.*;

public class PandemicBoardView extends JComponent {
    private final Image background;
    private final Image cardBackPD;
    private final Image cardBackInf;
    private final GraphBoard graphBoard;
    private int width;
    private int height;

    PandemicGameState gameState;
    double scale = 0.85;

    int cardWidth = (int)(scale * PandemicCardView.cardWidth);
    int cardHeight = (int)(scale * PandemicCardView.cardHeight);
    int nodeSize = (int)(scale * 20);
    int researchStationSize = (int)(scale * 10);
    int playerPawnSize = (int)(scale * 10);
    int diseaseCubeSize = (int)(scale * 10);
    int diseaseCubeDistance = (int)(scale * 2);
    int counterWidth = (int)(scale * 20), counterHeight = (int)(scale * 20);
    int strokeWidth = (int)(scale * 2);

    private final Image outbreakCounterImg, infectionRateCounterImg, outbreakCounterBG, infectionRateCounterBG;
    private final Image outbreakImgLast;

    Vector2D infectionMarkerPositionStart = new Vector2D((int)(scale * 760), (int)(scale * 750));
    int infectionMarkerSize;

    Vector2D outbreakMarkerPositionStart = new Vector2D((int)(scale * 80), (int)(scale *  465));
    int outbreakMarkerGap = (int)(45 * scale);
    int outbreakMarkerSize;

    Vector2D[] diseaseMarkerPositions = new Vector2D[]{
            new Vector2D((int)(scale * 395), (int)(scale * 775)),
            new Vector2D((int)(scale * 450), (int)(scale * 775)),
            new Vector2D((int)(scale * 510), (int)(scale * 775)),
            new Vector2D((int)(scale * 560), (int)(scale * 775))
    };

    // Clickable locations
    Rectangle infectionDeckLocation = new Rectangle((int)(scale * 100), (int)(scale * 50), cardWidth, cardHeight);
    Rectangle infectionDiscardDeckLocation = new Rectangle((int)(scale * 220), (int)(scale * 50), cardWidth, cardHeight);
    Rectangle playerDiscardDeckLocation = new Rectangle((int)(scale * 880), (int)(scale * 50), cardWidth, cardHeight);
    Rectangle plannerDeckLocation = new Rectangle((int)(scale * 1070), (int)(scale * 50), cardWidth, cardHeight);
    Rectangle playerDeckLocation = new Rectangle((int)(scale * 760), (int)(scale * 50), cardWidth, cardHeight);
    HashMap<String, Rectangle> boardNodeLocations;
    Rectangle[] playerLocations;

    HashMap<String, Rectangle> highlights;
    int maxHighlights = 3;

    ArrayList<Integer> bufferHighlights;
    HashSet<Integer> playerHighlights;
    ArrayList<Integer>[] handCardHighlights;

    int panX, panY;

    public PandemicBoardView(AbstractGameState gs) {
        gameState = (PandemicGameState) gs;
        this.graphBoard = ((PandemicGameState) gs).getWorld();
        String dataPath = ((PandemicParameters)gs.getGameParameters()).getDataPath() + "/img/";

        // Background and card backs
        this.background = ImageIO.GetInstance().getImage(dataPath + ((PropertyString) graphBoard.getProperty(imgHash)).value);
        cardBackInf = ImageIO.GetInstance().getImage(dataPath + "CardBackInfections.png");
        cardBackPD = ImageIO.GetInstance().getImage(dataPath + "CardBackPD.png");

        // infection rate marker
        Counter infectionRateCounter = (Counter) gameState.getComponent(PandemicConstants.infectionRateHash);
        this.infectionRateCounterImg = ImageIO.GetInstance().getImage(dataPath +
                ((PropertyString)infectionRateCounter.getProperty(imgHash)).value);
        this.infectionRateCounterBG = ImageIO.GetInstance().getImage(dataPath +
                ((PropertyString)infectionRateCounter.getProperty(backgroundImgHash)).value);
        this.infectionMarkerSize = (int) (infectionRateCounterBG.getWidth(null) * scale);

        // outbreak marker
        Counter outbreakCounter = (Counter) gameState.getComponent(PandemicConstants.outbreaksHash);
        this.outbreakCounterImg = ImageIO.GetInstance().getImage(dataPath +
                ((PropertyString)outbreakCounter.getProperty(imgHash)).value);
        this.outbreakCounterBG = ImageIO.GetInstance().getImage(dataPath +
                ((PropertyString)outbreakCounter.getProperty(Hash.GetInstance().hash("backgroundImg"))).value);
        this.outbreakImgLast = ImageIO.GetInstance().getImage(dataPath +
                ((PropertyString)outbreakCounter.getProperty(Hash.GetInstance().hash("imgMax"))).value);
        this.outbreakMarkerSize = (int) (outbreakCounterBG.getWidth(null) * scale);

        width = (int)(background.getWidth(null) * scale);
        height = (int)(background.getHeight(null) * scale);

        boardNodeLocations = new HashMap<>();
        playerLocations = new Rectangle[gs.getNPlayers()];
        highlights = new HashMap<>();

        Collection<BoardNode> bList = graphBoard.getBoardNodes();
        for (BoardNode b : bList) {
            Vector2D poss = ((PropertyVector2D) b.getProperty(coordinateHash)).values;
            Vector2D pos = new Vector2D((int)(poss.getX()*scale), (int)(poss.getY()*scale));
            boardNodeLocations.put(((PropertyString) b.getProperty(nameHash)).value,
                    new Rectangle(pos.getX() - nodeSize / 2, pos.getY() - nodeSize / 2, nodeSize, nodeSize));
        }

        addMouseWheelListener(e -> {
            double amount = 0.2 * Math.abs(e.getPreciseWheelRotation());
            if (e.getPreciseWheelRotation() > 0) {
                // Rotated down, zoom out
                updateScale(scale - amount);
            } else {
                updateScale(scale + amount);
            }
            highlights.clear();
        });
        addMouseListener(new MouseAdapter() {
            Point start;

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    // Middle (wheel) click, pan around
                    start = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2 && start != null) {
                    // Middle (wheel) click, pan around
                    Point end = e.getPoint();
                    panX += (int)(scale * (end.x - start.x));
                    panY += (int)(scale * (end.y - start.y));
                    start = null;
                    highlights.clear();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3 || highlights.size() >= maxHighlights) {
                    highlights.clear();
                    return;
                }
                Point p = new Point(e.getX() - panX, e.getY() - panY);
                if (infectionDeckLocation.contains(p)) {
                    highlights.put("infectionDeck", infectionDeckLocation);
                } else if (infectionDiscardDeckLocation.contains(p)) {
                    highlights.put("infectionDiscard", infectionDiscardDeckLocation);
                } else if (playerDiscardDeckLocation.contains(p)) {
                    highlights.put("playerDiscard", playerDiscardDeckLocation);
                } else if (plannerDeckLocation.contains(p)) {
                    highlights.put("plannerDeck", plannerDeckLocation);
                } else {
                    for (int i = 0; i < playerLocations.length; i++) {
                        if (playerLocations[i] != null && playerLocations[i].contains(p)) {
                            highlights.put("player " + i, playerLocations[i]);
                            break;
                        }
                    }
                    for (Map.Entry<String, Rectangle> en: boardNodeLocations.entrySet()) {
                        if (en.getValue().contains(p)) {
                            highlights.put("BN " + en.getKey(), en.getValue());
                            break;
                        }
                    }
                }
            }
        });

//        System.out.println(Arrays.toString(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
    }

    private void updateScale(double scale) {
        this.scale = scale;

        diseaseMarkerPositions = new Vector2D[]{
                new Vector2D((int)(scale * 395), (int)(scale * 775)),
                new Vector2D((int)(scale * 450), (int)(scale * 775)),
                new Vector2D((int)(scale * 510), (int)(scale * 775)),
                new Vector2D((int)(scale * 560), (int)(scale * 775))
        };
        cardWidth = (int)(scale * PandemicCardView.cardWidth);
        cardHeight = (int)(scale * PandemicCardView.cardHeight);
        nodeSize = (int)(scale * 20);
        researchStationSize = (int)(scale * 10);
        playerPawnSize = (int)(scale * 10);
        diseaseCubeSize = (int)(scale * 10);
        diseaseCubeDistance = (int)(scale * 2);
        counterWidth = (int)(scale * 20);
        counterHeight = (int)(scale * 20);
        strokeWidth = (int)(scale * 2);
        infectionMarkerPositionStart = new Vector2D((int)(scale * 760), (int)(scale * 750));
        outbreakMarkerPositionStart = new Vector2D((int)(scale * 80), (int)(scale *  465));
        infectionDeckLocation = new Rectangle((int)(scale * 100), (int)(scale * 50), cardWidth, cardHeight);
        infectionDiscardDeckLocation = new Rectangle((int)(scale * 220), (int)(scale * 50), cardWidth, cardHeight);
        playerDiscardDeckLocation = new Rectangle((int)(scale * 880), (int)(scale * 50), cardWidth, cardHeight);
        plannerDeckLocation = new Rectangle((int)(scale * 1070), (int)(scale * 50), cardWidth, cardHeight);
        playerDeckLocation = new Rectangle((int)(scale * 760), (int)(scale * 50), cardWidth, cardHeight);

        width = (int)(background.getWidth(null) * scale);
        height = (int)(background.getHeight(null) * scale);

        boardNodeLocations = new HashMap<>();
        Collection<BoardNode> bList = graphBoard.getBoardNodes();
        for (BoardNode b : bList) {
            Vector2D poss = ((PropertyVector2D) b.getProperty(coordinateHash)).values;
            Vector2D pos = new Vector2D((int)(poss.getX()*scale), (int)(poss.getY()*scale));
            boardNodeLocations.put(((PropertyString) b.getProperty(nameHash)).value,
                    new Rectangle(pos.getX() - nodeSize / 2, pos.getY() - nodeSize / 2, nodeSize, nodeSize));
        }
        outbreakMarkerGap = (int)(45 * scale);
        this.infectionMarkerSize = (int) (infectionRateCounterBG.getWidth(null) * scale);
        this.outbreakMarkerSize = (int) (outbreakCounterBG.getWidth(null) * scale);
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
                g2.drawRect(panX + highlight.x - strokeWidth / 2,
                        panY + highlight.y - strokeWidth / 2,
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
        drawImage(g, background, panX, panY);

        // Draw nodes
        Collection<BoardNode> bList = graphBoard.getBoardNodes();
        for (BoardNode b: bList) {
            Vector2D poss = ((PropertyVector2D) b.getProperty(coordinateHash)).values;
            Vector2D pos = new Vector2D((int)(poss.getX()*scale) + panX, (int)(poss.getY()*scale) + panY);
            PropertyBoolean edge = ((PropertyBoolean)b.getProperty(edgeHash));

            Set<BoardNode> bns = b.getNeighbours().keySet();
            for (BoardNode b2: bns) {
                Vector2D poss2 = ((PropertyVector2D) b2.getProperty(coordinateHash)).values;
                Vector2D pos2 = new Vector2D((int)(poss2.getX()*scale) + panX, (int)(poss2.getY()*scale) + panY);
                PropertyBoolean edge2 = ((PropertyBoolean)b2.getProperty(edgeHash));

                if (edge != null && edge.value && edge2 != null && edge2.value) {
                    // Two edge nodes connected check if on opposite sides, draw connection as if b2 on the other side of map
                    if (pos2.getX() < width/2 && pos.getX() > width/2 || pos2.getX() > width/2 && pos.getX() < width/2) {
                        if (pos2.getX() > pos.getX()) pos2.setX(pos2.getX() - width);
                        else pos2.setX(width + pos2.getX());
                    }
                }
                g.setColor(Color.white);
                g.drawLine(pos.getX(), pos.getY(), pos2.getX(), pos2.getY());
            }

            g.setColor(Color.black);
            g.drawString(((PropertyString)b.getProperty(nameHash)).value, pos.getX(), pos.getY() - nodeSize/2 - playerPawnSize);
        }

        HashSet<String> locationsHighlights = getLocationsHighlighted();
        for (BoardNode b : bList) {
            String name = ((PropertyString)b.getProperty(nameHash)).value;
            Vector2D poss = ((PropertyVector2D) b.getProperty(coordinateHash)).values;
            Vector2D pos = new Vector2D((int)(poss.getX()*scale) + panX, (int)(poss.getY()*scale) + panY);

            Stroke s = g.getStroke();
            if (locationsHighlights.contains(name)) {
                g.setStroke(new BasicStroke(10));
                g.setColor(new Color(211, 252, 209));
                g.drawOval(pos.getX() - nodeSize /2, pos.getY() - nodeSize /2, nodeSize, nodeSize);
            }

            g.setColor(Utils.stringToColor(((PropertyColor) b.getProperty(colorHash)).valueStr));
            g.fillOval(pos.getX() - nodeSize /2, pos.getY() - nodeSize /2, nodeSize, nodeSize);

            if (!locationsHighlights.contains(name)) {
                g.setColor(new Color(30, 108, 47));
                g.drawOval(pos.getX() - nodeSize /2, pos.getY() - nodeSize /2, nodeSize, nodeSize);
            }

            g.setStroke(s);
            g.setColor(Color.black);

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
            for (int p: players) {
                // This player is here, draw them just above the node

                // Position
                int x = pos.getX() + nPlayers * playerPawnSize / 2 - p * playerPawnSize - playerPawnSize /2;
                int y = pos.getY() - nodeSize /2 - playerPawnSize /2;

                // Highlight
                Stroke s2 = g.getStroke();
                if (playerHighlights.contains(p)) {
                    g.setStroke(new BasicStroke(10));
                    g.setColor(new Color(211, 252, 209));
                    g.drawOval(x, y, playerPawnSize, playerPawnSize);
                }
                g.setStroke(s2);

                // Find color of player
                Card playerCard = (Card) gameState.getComponent(PandemicConstants.playerCardHash, p);
                PropertyColor color = (PropertyColor) playerCard.getProperty(colorHash);
                g.setColor(Utils.stringToColor(color.valueStr));
                g.fillOval(x, y, playerPawnSize, playerPawnSize);
                g.setColor(Color.black);
                g.drawOval(x, y, playerPawnSize, playerPawnSize);

                int rawX = (int)(poss.getX()*scale) + nPlayers * playerPawnSize / 2 - p * playerPawnSize - playerPawnSize /2;
                int rawY = (int)(poss.getY()*scale) - nodeSize /2 - playerPawnSize /2;
                playerLocations[p] = new Rectangle(rawX, rawY, playerPawnSize, playerPawnSize);
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
                    g.fillRect(maxX - idx * (diseaseCubeSize + diseaseCubeDistance) - diseaseCubeSize/2,
                            pos.getY() - diseaseCubeSize /2, diseaseCubeSize, diseaseCubeSize);
                    g.setColor(Color.white);
                    if (cubeColor != null && cubeColor.equals(Color.yellow)) g.setColor(Color.black);  // Contrasting outline
                    g.drawRect(maxX - idx * (diseaseCubeSize + diseaseCubeDistance) - diseaseCubeSize/2,
                            pos.getY() - diseaseCubeSize /2, diseaseCubeSize, diseaseCubeSize);
                    idx++;
                }

            }
        }

        Font f = g.getFont();
        Font labelFont = new Font("Agency FB", Font.BOLD, fontSize+10);
        Font labelFontS = new Font("Agency FB", Font.BOLD, fontSize+4);

        // Draw infection rate marker
        Counter infectionRateCounter = (Counter) gameState.getComponent(PandemicConstants.infectionRateHash);
        int idx = infectionRateCounter.getValue();
        int[] infectionArray = ((PandemicParameters)gameState.getGameParameters()).getInfectionRate();

        g.setFont(labelFontS);
        for (int i = 0; i < infectionArray.length; i++) {
            int x = infectionMarkerPositionStart.getX() + infectionMarkerSize/2 + infectionMarkerSize*i;
            Vector2D pos = new Vector2D(x + panX, infectionMarkerPositionStart.getY() + panY);
            drawCenteredImage(g, infectionRateCounterBG, pos.getX(), pos.getY());
            g.drawString(""+infectionArray[i], pos.getX() - infectionMarkerSize/4, pos.getY() + infectionMarkerSize);
        }
        g.setFont(f);

        int x = infectionMarkerPositionStart.getX() + infectionMarkerSize/2 + infectionMarkerSize*idx;
        Vector2D pos = new Vector2D(x + panX, infectionMarkerPositionStart.getY() + panY);
        drawCenteredImage(g, infectionRateCounterImg, pos.getX(), pos.getY());

        g.setFont(labelFont);
        g.drawString("Infection Rate", panX + infectionMarkerPositionStart.getX() - infectionMarkerSize/2,
                panY + infectionMarkerPositionStart.getY() - infectionMarkerSize*2/3);

        // Draw outbreak marker
        Counter outbreakCounter = (Counter) gameState.getComponent(PandemicConstants.outbreaksHash);
        idx = outbreakCounter.getValue();

        g.setFont(labelFontS);
        for (int i = 0; i < outbreakCounter.getMaximum() + 1; i++) {
            pos = getOutbreakPos(i);
            drawCenteredImage(g, outbreakCounterBG, pos.getX(), pos.getY());
            if (i == outbreakCounter.getMaximum()) {
                drawCenteredImage(g, outbreakImgLast, pos.getX(), pos.getY());
            } else {
                g.drawString("" + i, (int)(pos.getX()), (int)(pos.getY() + 4*scale));
            }
        }
        g.setFont(f);

        pos = getOutbreakPos(idx);
        drawCenteredImage(g, outbreakCounterImg, pos.getX(), pos.getY());

        g.setFont(labelFont);
        g.drawString("Outbreaks", panX + outbreakMarkerPositionStart.getX() - outbreakMarkerGap/4,
                panY + outbreakMarkerPositionStart.getY() - outbreakMarkerSize*2/3);

        // Decks
        Deck<Card> playerDiscardDeck = (Deck<Card>) gameState.getComponent(PandemicConstants.playerDeckDiscardHash);
        if (playerDiscardDeck != null) {
            Card cP = playerDiscardDeck.peek();
            g.setFont(f);
            drawCard(g, cP, null, new Rectangle(playerDiscardDeckLocation.x + panX, playerDiscardDeckLocation.y + panY, playerDiscardDeckLocation.width, playerDiscardDeckLocation.height));
        }
        g.setFont(labelFontS);
        g.drawString("Player Discard Deck", (int)playerDiscardDeckLocation.getX() + panX, (int)playerDiscardDeckLocation.getY() - fontSize + panY);

        Deck<Card> infectionDiscardDeck = (Deck<Card>) gameState.getComponent(PandemicConstants.infectionDiscardHash);
        if (infectionDiscardDeck != null) {
            Card cI = infectionDiscardDeck.peek();
            g.setFont(f);
            drawCard(g, cI, null, new Rectangle(infectionDiscardDeckLocation.x + panX, infectionDiscardDeckLocation.y + panY, infectionDiscardDeckLocation.width, infectionDiscardDeckLocation.height));
        }
        g.setFont(labelFontS);
        g.drawString("Infection Discard Deck", (int)infectionDiscardDeckLocation.getX() + panX, (int)infectionDiscardDeckLocation.getY() - fontSize + panY);

        Deck<Card> plannerDeck = (Deck<Card>) gameState.getComponent(plannerDeckHash);
        if (plannerDeck != null) {
            Card cI = plannerDeck.peek();
            g.setFont(f);
            drawCard(g, cI, null, new Rectangle(plannerDeckLocation.x + panX, plannerDeckLocation.y + panY, plannerDeckLocation.width, plannerDeckLocation.height));
        }
        g.setFont(labelFontS);
        g.drawString("Planner Deck", (int)plannerDeckLocation.getX() + panX, (int)plannerDeckLocation.getY() - fontSize + panY);

        drawDeck(g, (Deck<Card>) gameState.getComponent(infectionHash), null, cardBackInf, new Rectangle(infectionDeckLocation.x + panX, infectionDeckLocation.y + panY, infectionDeckLocation.width, infectionDeckLocation.height), false);
        g.drawString("Infection Deck", (int)infectionDeckLocation.getX() + panX, panY + (int)infectionDeckLocation.getY() - fontSize);
        drawDeck(g, (Deck<Card>) gameState.getComponent(playerDeckHash), null, cardBackPD, new Rectangle(playerDeckLocation.x + panX, playerDeckLocation.y + panY, playerDeckLocation.width, playerDeckLocation.height), false);
        g.drawString("Player Deck", (int)playerDeckLocation.getX() + panX, panY + (int)playerDeckLocation.getY() - fontSize);
        g.setFont(f);

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

    private Vector2D getOutbreakPos(int i) {
        int x = outbreakMarkerPositionStart.getX() + outbreakMarkerGap*(i%2) + panX;
        int y = outbreakMarkerPositionStart.getY() + (int)(outbreakMarkerSize*0.5*i) + panY;
        return new Vector2D(x, y);
    }

    private void drawImage(Graphics2D g, Image img, int x, int y) {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        g.drawImage(img, x, y, (int) (w*scale), (int) (h*scale), null);
    }

    private void drawCenteredImage(Graphics2D g, Image img, int x, int y) {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        g.drawImage(img, x-w/2, y-h/2, (int) (w*scale), (int) (h*scale), null);
    }

    private void drawCounter(Graphics2D g, int value, Color color, int idx) {
        g.setColor(color);
        Vector2D pos = diseaseMarkerPositions[idx];
        if (value > 0) {
            g.fillOval(pos.getX() + panX, pos.getY() + panY, counterWidth, counterHeight);
            if (value == 2) {
                g.setColor(Color.white);
                g.drawLine(pos.getX() + panX, pos.getY() + panY, pos.getX() + counterWidth + panX, pos.getY() + counterHeight + panY);
            }
        } else {
            g.drawOval(pos.getX() + panX, pos.getY() + panY, counterWidth, counterHeight);
        }
        g.setColor(Color.black);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public HashMap<String, Rectangle> getHighlights() {
        return highlights;
    }

    public void setBufferHighlights(ArrayList<Integer> bufferHighlights) {
        this.bufferHighlights = bufferHighlights;
    }

    public void setPlayerHighlights(HashSet<Integer> playerHighlights) {
        this.playerHighlights = playerHighlights;
    }

    public void setCardHandHighlights(ArrayList<Integer>[] handCardHighlights) {
        this.handCardHighlights = handCardHighlights;
    }

    public HashSet<String> getLocationsHighlighted() {
        HashSet<String> highlights = new HashSet<>();
        for (int i = 0; i < handCardHighlights.length; i++) {
            if (handCardHighlights[i].size() > 0) {
                Deck<Card> handCards = (Deck<Card>) gameState.getComponent(playerHandHash, i);
                for (int j : handCardHighlights[i]) {
                    if (j < handCards.getSize())
                        highlights.add(((PropertyString) handCards.get(j).getProperty(nameHash)).value);
                }
            }
        }
        return highlights;
    }
}
