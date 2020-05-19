package games.pandemic.gui;

import core.components.*;
import core.content.*;
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
import java.util.List;

import static games.pandemic.PandemicConstants.*;
import static games.pandemic.gui.PandemicCardView.drawCard;
import static games.pandemic.gui.PandemicCardView.drawDeckBack;
import static utilities.CoreConstants.*;

public class PandemicBoardView extends JComponent {
    private Image background;
    private Board board;
    private int width;
    private int height;

    PandemicGameState gameState;
    double scale = 0.75;

    private int cardWidth = (int)(scale * PandemicCardView.cardWidth);
    private int cardHeight = (int)(scale * PandemicCardView.cardHeight);
    int nodeSize = (int)(scale * 20);
    int researchStationSize = (int)(scale * 10);
    int playerPawnSize = (int)(scale * 10);
    int diseaseCubeSize = (int)(scale * 10);
    int diseaseCubeDistance = (int)(scale * 2);
    int counterWidth = (int)(scale * 20), counterHeight = (int)(scale * 20);
    int strokeWidth = (int)(scale * 2);

    private Image outbreakCounterImg, infectionRateCounterImg, outbreakCounterBG, infectionRateCounterBG;
    private Image outbreakImgLast;

    Vector2D infectionMarkerPositionStart = new Vector2D((int)(scale * 760), (int)(scale * 750));
    int infectionMarkerSize;

    Vector2D outbreakMarkerPositionStart = new Vector2D((int)(scale * 80), (int)(scale *  465));
    int outbreakMarkerGap = 45;
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

    public PandemicBoardView(AbstractGameState gs) {
        gameState = (PandemicGameState) gs;
        this.board = ((PandemicGameState) gs).getData().findBoard("Cities");
        String dataPath = ((PandemicParameters)gs.getGameParameters()).getDataPath() + "img/";

        this.background = ImageIO.GetInstance().getImage(dataPath + ((PropertyString)board.getProperty(imgHash)).value);
        // infection rate marker
        Counter infectionRateCounter = (Counter) gameState.getComponent(PandemicConstants.infectionRateHash);
        this.infectionRateCounterImg = ImageIO.GetInstance().getImage(dataPath +
                ((PropertyString)infectionRateCounter.getProperty(imgHash)).value);
        this.infectionRateCounterBG = ImageIO.GetInstance().getImage(dataPath +
                ((PropertyString)infectionRateCounter.getProperty(backgroundImgHash)).value);
        this.infectionMarkerSize = infectionRateCounterBG.getWidth(null);
        this.infectionMarkerPositionStart.setX(infectionMarkerPositionStart.getX()+infectionMarkerSize/2);

        // outbreak marker
        Counter outbreakCounter = (Counter) gameState.getComponent(PandemicConstants.outbreaksHash);
        this.outbreakCounterImg = ImageIO.GetInstance().getImage(dataPath +
                ((PropertyString)outbreakCounter.getProperty(imgHash)).value);
        this.outbreakCounterBG = ImageIO.GetInstance().getImage(dataPath +
                ((PropertyString)outbreakCounter.getProperty(Hash.GetInstance().hash("backgroundImg"))).value);
        this.outbreakImgLast = ImageIO.GetInstance().getImage(dataPath +
                ((PropertyString)outbreakCounter.getProperty(Hash.GetInstance().hash("imgMax"))).value);
        this.outbreakMarkerSize = outbreakCounterBG.getWidth(null);

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

//        System.out.println(Arrays.toString(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
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
        for (BoardNode b: bList) {
            Vector2D poss = ((PropertyVector2D) b.getProperty(coordinateHash)).values;
            Vector2D pos = new Vector2D((int)(poss.getX()*scale), (int)(poss.getY()*scale));
            PropertyBoolean edge = ((PropertyBoolean)b.getProperty(edgeHash));

            HashSet<BoardNode> neighbours = b.getNeighbours();
            for (BoardNode b2: neighbours) {
                Vector2D poss2 = ((PropertyVector2D) b2.getProperty(coordinateHash)).values;
                Vector2D pos2 = new Vector2D((int)(poss2.getX()*scale), (int)(poss2.getY()*scale));
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

        for (BoardNode b : bList) {
            Vector2D poss = ((PropertyVector2D) b.getProperty(coordinateHash)).values;
            Vector2D pos = new Vector2D((int)(poss.getX()*scale), (int)(poss.getY()*scale));

            g.setColor(Utils.stringToColor(((PropertyColor) b.getProperty(Hash.GetInstance().hash("color"))).valueStr));
            g.fillOval(pos.getX() - nodeSize /2, pos.getY() - nodeSize /2, nodeSize, nodeSize);
            g.setColor(new Color(30, 108, 47));
            g.drawOval(pos.getX() - nodeSize /2, pos.getY() - nodeSize /2, nodeSize, nodeSize);
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
                // Find color of player
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
        int[] infectionArray = ((PandemicParameters)gameState.getGameParameters()).getInfection_rate();

        g.setFont(labelFontS);
        for (int i = 0; i < infectionArray.length; i++) {
            int x = infectionMarkerPositionStart.getX() + infectionMarkerSize*i;
            Vector2D pos = new Vector2D(x, infectionMarkerPositionStart.getY());
            drawCenteredImage(g, infectionRateCounterBG, pos.getX(), pos.getY());
            g.drawString(""+infectionArray[i], pos.getX() - infectionMarkerSize/4, pos.getY() + infectionMarkerSize*2/3 + (int)(4*scale));
        }
        g.setFont(f);

        int x = infectionMarkerPositionStart.getX() + infectionMarkerSize*idx;
        Vector2D pos = new Vector2D(x, infectionMarkerPositionStart.getY());
        drawCenteredImage(g, infectionRateCounterImg, pos.getX(), pos.getY());

        g.setFont(labelFont);
        g.drawString("Infection Rate", infectionMarkerPositionStart.getX() - infectionMarkerSize/2,
                infectionMarkerPositionStart.getY() - infectionMarkerSize*2/3);

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
                g.drawString("" + i, pos.getX() - outbreakMarkerSize/5, pos.getY());
            }
        }
        g.setFont(f);

        pos = getOutbreakPos(idx);
        drawCenteredImage(g, outbreakCounterImg, pos.getX(), pos.getY());

        g.setFont(labelFont);
        g.drawString("Outbreaks", outbreakMarkerPositionStart.getX() - outbreakMarkerGap/4,
                outbreakMarkerPositionStart.getY() - outbreakMarkerSize*2/3);

        // Decks
        Deck<Card> playerDiscardDeck = (Deck<Card>) gameState.getComponent(PandemicConstants.playerDeckDiscardHash);
        if (playerDiscardDeck != null) {
            Card cP = playerDiscardDeck.peek();
            g.setFont(f);
            drawCard(g, cP, null, playerDiscardDeckLocation);
        }
        g.setFont(labelFontS);
        g.drawString("Player Discard Deck", (int)playerDiscardDeckLocation.getX(), (int)playerDiscardDeckLocation.getY() - fontSize);

        Deck<Card> infectionDiscardDeck = (Deck<Card>) gameState.getComponent(PandemicConstants.infectionDiscardHash);
        if (infectionDiscardDeck != null) {
            Card cI = infectionDiscardDeck.peek();
            g.setFont(f);
            drawCard(g, cI, null, infectionDiscardDeckLocation);
        }
        g.setFont(labelFontS);
        g.drawString("Infection Discard Deck", (int)infectionDiscardDeckLocation.getX(), (int)infectionDiscardDeckLocation.getY() - fontSize);

        Deck<Card> plannerDeck = (Deck<Card>) gameState.getComponent(plannerDeckHash);
        if (plannerDeck != null) {
            Card cI = plannerDeck.peek();
            g.setFont(f);
            drawCard(g, cI, null, plannerDeckLocation);
        }
        g.setFont(labelFontS);
        g.drawString("Planner Deck", (int)plannerDeckLocation.getX(), (int)plannerDeckLocation.getY() - fontSize);

        drawDeckBack(g, null, null, infectionDeckLocation);
        g.drawString("Infection Deck", (int)infectionDeckLocation.getX(), (int)infectionDeckLocation.getY() - fontSize);
        drawDeckBack(g, null, null, playerDeckLocation);
        g.drawString("Player Deck", (int)playerDeckLocation.getX(), (int)playerDeckLocation.getY() - fontSize);
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
        int x = outbreakMarkerPositionStart.getX() + outbreakMarkerGap*(i%2);
        int y = outbreakMarkerPositionStart.getY() + (int)(outbreakMarkerSize*0.5*i);
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
        if (value > 0) {
            Vector2D pos = diseaseMarkerPositions[idx];
            g.setColor(color);
            g.fillOval(pos.getX(), pos.getY(), counterWidth, counterHeight);
            if (value == 2) {
                g.setColor(Color.white);
                g.drawLine(pos.getX(), pos.getY(), pos.getX() + counterWidth, pos.getY() + counterHeight);
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
