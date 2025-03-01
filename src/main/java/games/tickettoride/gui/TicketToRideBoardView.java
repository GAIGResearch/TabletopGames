package games.tickettoride.gui;

import core.AbstractGameState;
import core.components.*;
import core.properties.*;
import games.tickettoride.TicketToRideConstants;
import games.tickettoride.TicketToRideGameState;
import games.tickettoride.TicketToRideParameters;
import utilities.Hash;
import utilities.ImageIO;
import utilities.Utils;
import utilities.Vector2D;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

import static core.CoreConstants.*;
import static gui.views.DeckView.drawDeck;
import static games.tickettoride.TicketToRideConstants.*;
import static games.tickettoride.gui.TicketToRideCardView.drawCard;

public class TicketToRideBoardView extends JComponent {
    private final Image background;
    private final Image cardBackPD;
    private final Image cardBackInf;
    private final GraphBoardWithEdges graphBoard;
    private int width;
    private int height;

    TicketToRideGameState gameState;
    double scale = 0.7;

    int cardWidth = (int)(scale * TicketToRideCardView.cardWidth);
    int cardHeight = (int)(scale * TicketToRideCardView.cardHeight);
    int nodeSize = (int)(scale * 20);
    int strokeWidth = (int)(scale * 2);



    // Clickable locations

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

    public TicketToRideBoardView(AbstractGameState gs) {
        gameState = (TicketToRideGameState) gs;
        this.graphBoard = ((TicketToRideGameState) gs).getWorld();
        String dataPath = ((TicketToRideParameters)gs.getGameParameters()).getDataPath() + "img/";

        // Background and card backs
        background = ImageIO.GetInstance().getImage(dataPath + "ticketToRideBg3.png");
        cardBackInf = ImageIO.GetInstance().getImage(dataPath + "trainCardBlueBg.png");
        cardBackPD = ImageIO.GetInstance().getImage(dataPath + "trainCardRedBg.png");


//        System.out.println( background +  " background");

        width = (int)(background.getWidth(null) * scale);
        height = (int)(background.getHeight(null) * scale);

        boardNodeLocations = new HashMap<>();
        playerLocations = new Rectangle[gs.getNPlayers()];
        highlights = new HashMap<>();

        Collection<BoardNodeWithEdges> bList = graphBoard.getBoardNodes();
        for (BoardNodeWithEdges b : bList) {
            Vector2D poss = ((PropertyVector2D) b.getProperty(coordinateHash)).values;
            Vector2D pos = new Vector2D((int)(poss.getX()*scale), (int)(poss.getY()*scale));
            boardNodeLocations.put(((PropertyString) b.getProperty(nameHash)).value,
                    new Rectangle(pos.getX() - nodeSize / 2, pos.getY() - nodeSize / 2, nodeSize, nodeSize));
        }

        addMouseWheelListener(e -> {
            double amount = 0.2 * Math.abs(e.getPreciseWheelRotation());
            if (e.getPreciseWheelRotation() > 0) {
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
                if (playerDiscardDeckLocation.contains(p)) {
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

    }

    private void updateScale(double scale) {
        this.scale = scale;


        cardWidth = (int)(scale * TicketToRideCardView.cardWidth);
        cardHeight = (int)(scale * TicketToRideCardView.cardHeight);
        nodeSize = (int)(scale * 20);


        playerDiscardDeckLocation = new Rectangle((int)(scale * 880), (int)(scale * 50), cardWidth, cardHeight);
        plannerDeckLocation = new Rectangle((int)(scale * 1070), (int)(scale * 50), cardWidth, cardHeight);
        playerDeckLocation = new Rectangle((int)(scale * 760), (int)(scale * 50), cardWidth, cardHeight);

        width = (int)(background.getWidth(null) * scale);
        height = (int)(background.getHeight(null) * scale);

        boardNodeLocations = new HashMap<>();
        Collection<BoardNodeWithEdges> bList = graphBoard.getBoardNodes();
        for (BoardNodeWithEdges b : bList) {
            Vector2D poss = ((PropertyVector2D) b.getProperty(coordinateHash)).values;
            Vector2D pos = new Vector2D((int)(poss.getX()*scale), (int)(poss.getY()*scale));
            boardNodeLocations.put(((PropertyString) b.getProperty(nameHash)).value,
                    new Rectangle(pos.getX() - nodeSize / 2, pos.getY() - nodeSize / 2, nodeSize, nodeSize));
        }

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
        int fontSize = (int) (scale * fSize);
        g.setFont(new Font(g.getFont().getFontName(), Font.PLAIN, fontSize));
        int nPlayers = gameState.getNPlayers();

        // Draw board background
        drawImage(g, background, panX, panY);

        // Draw nodes

        Collection<BoardNodeWithEdges> boardNodeList = graphBoard.getBoardNodes();
        Map<String, BoardNodeWithEdges> locationNameToNode = new HashMap<>();
        for (BoardNodeWithEdges b : boardNodeList) { //location names to acess the actual node
            locationNameToNode.put( String.valueOf(b.getProperty(nameHash)), b);
        }



        Collection<Edge> edgeList = graphBoard.getBoardEdges();

        //draw lines between locations
        for (Edge currentEdge : edgeList) {

            Property claimedByPlayerRoute1Prop = currentEdge.getProperty(claimedByPlayerRoute1Hash);
            Property claimedByPlayerRoute2Prop = currentEdge.getProperty(claimedByPlayerRoute2Hash);

            int claimedByPlayerRoute1 = ((PropertyInt) claimedByPlayerRoute1Prop).value;
            int claimedByPlayerRoute2 = -2;
            if (claimedByPlayerRoute2Prop != null){
                claimedByPlayerRoute2  = ((PropertyInt) claimedByPlayerRoute2Prop).value;
            }


            Property nodeProp = currentEdge.getProperty(nodesHash);
            String[] nodeNames = ((PropertyStringArray) nodeProp).getValues();

            //get nodes
            BoardNodeWithEdges node1 = locationNameToNode.get(nodeNames[0]);
            BoardNodeWithEdges node2 = locationNameToNode.get(nodeNames[1]);

            Vector2D pos1Value = ((PropertyVector2D) node1.getProperty(coordinateHash)).values;
            Vector2D pos2Value = ((PropertyVector2D) node2.getProperty(coordinateHash)).values;

            Vector2D pos1 = new Vector2D((int) (pos1Value.getX() * scale) + panX, (int) (pos1Value.getY() * scale) + panY);
            Vector2D pos2 = new Vector2D((int) (pos2Value.getX() * scale) + panX, (int) (pos2Value.getY() * scale) + panY);


            boolean routeClaimed  = ((PropertyBoolean)currentEdge.getProperty(routeClaimedHash)).value;
            if (routeClaimed){
                g.setColor(Color.RED);
            } else if(claimedByPlayerRoute1 != -1) { //partially taken double route
                g.setColor(Color.ORANGE);

            } else if (claimedByPlayerRoute2 != -1 && claimedByPlayerRoute2Prop != null ) {
                g.setColor(Color.ORANGE);
            } else{
                g.setColor(Color.GREEN);
            }

            g.drawLine(pos1.getX(), pos1.getY(), pos2.getX(), pos2.getY());
            g.setColor(Color.black);
        }

        //draw locations
        for (BoardNodeWithEdges currentNode : boardNodeList) {
            String name = ((PropertyString) currentNode.getProperty(nameHash)).value;
            Vector2D poss = ((PropertyVector2D) currentNode.getProperty(coordinateHash)).values;
            Vector2D pos = new Vector2D((int) (poss.getX() * scale) + panX, (int) (poss.getY() * scale) + panY);


            g.setColor(Utils.stringToColor(((PropertyColor) currentNode.getProperty(colorHash)).valueStr));
            g.fillOval(pos.getX() - nodeSize / 2, pos.getY() - nodeSize / 2, nodeSize, nodeSize);

            Stroke s = g.getStroke();
            g.setStroke(s);
            g.setColor(Color.black);
            g.drawString(name, pos.getX(), pos.getY() - nodeSize/2 );

        }

    }
    private void drawImage(Graphics2D g, Image img, int x, int y) {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        g.drawImage(img, x, y, (int) (w*scale), (int) (h*scale), null);
    }

    public HashMap<String, Rectangle> getHighlights() {
        return highlights;
    }

    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

}
