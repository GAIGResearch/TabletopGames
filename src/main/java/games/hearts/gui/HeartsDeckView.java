package games.hearts.gui;

import core.components.Deck;
import core.components.FrenchCard;
import gui.views.CardView;
import gui.views.ComponentView;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static games.hearts.gui.HeartsGUIManager.*;

public class HeartsDeckView extends ComponentView {

    Image backOfCard;
    String dataPath;

    Deck<FrenchCard> deck;

    protected boolean isVisible;
    int minimumCardOffset = 5;
    Rectangle[] rects;
    int cardHighlight = -1;   // position in the cards as drawn, left to right
    boolean highlighting;
    // order in which to lay out the cards when face-up (null: deck order); display only
    Comparator<FrenchCard> displayOrder;
    // the cards as last drawn, left to right (matching rects)
    List<FrenchCard> drawnCards = new ArrayList<>();
    // for a clickable GUI: the cards to outline, and the tooltip for a mouse position
    Map<FrenchCard, Color> outlines = new HashMap<>();
    Function<MouseEvent, String> toolTips;

    public HeartsDeckView(Deck<FrenchCard> d, String dataPath, boolean visible){
        super(d, playerWidth, cardHeight);
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "gray_back.png");
        this.dataPath = dataPath;
        this.isVisible = visible;
        this.deck = d;

        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ALT){
                    highlighting = true;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ALT){
                    highlighting = false;
                    cardHighlight = -1;
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (highlighting && positionAt(e.getPoint()) >= 0)
                    cardHighlight = positionAt(e.getPoint());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() ==1){
                    if (positionAt(e.getPoint()) >= 0)
                        cardHighlight = positionAt(e.getPoint());
                }
                else{
                    cardHighlight = -1;
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawDeck((Graphics2D)g, new Rectangle(0, 0, width, cardHeight));
    }

    public void drawDeck(Graphics2D g, Rectangle rect){
        int size = g.getFont().getSize();
        @SuppressWarnings("Unchecked") Deck<FrenchCard> deck = (Deck<FrenchCard>) component;

        if (deck != null){
            int offset = deck.getSize() > 0 ? Math.max((rect.width-cardWidth) / deck.getSize(), minimumCardOffset) : minimumCardOffset;
            rects = new Rectangle[deck.getSize()];
            List<FrenchCard> cards = inDisplayOrder(deck);
            drawnCards = cards;
            for (int i = 0; i < cards.size(); i++){
                FrenchCard card = cards.get(i);
                Image cardFace = getCardImage(card);
                Rectangle r = new Rectangle(rect.x + offset * i, rect.y, cardWidth, cardHeight);
                rects[i] = r;
                CardView.drawCard(g, r.x, r.y, r.width, r.height, card, cardFace, backOfCard, isVisible);
                g.drawRoundRect(r.x, r.y, r.width, r.height, 15, 15);
            }
            if (cardHighlight != -1 && cardHighlight < cards.size()){
                FrenchCard card = cards.get(cardHighlight);
                Image cardFace = getCardImage(card);
                Rectangle r = rects[cardHighlight];
                CardView.drawCard(g, r.x, r.y, r.width, r.height, card, cardFace, backOfCard, isVisible);
                g.drawRoundRect(r.x, r.y, r.width, r.height, 15, 15);
            }
            g.setStroke(new BasicStroke(3));
            for (int i = 0; i < cards.size(); i++) {
                Color outline = outlines.get(cards.get(i));
                if (outline != null) {
                    g.setColor(outline);
                    g.draw(visibleShape(i));
                }
            }
            g.setStroke(new BasicStroke(1));
            g.setColor(Color.black);
        }
    }

    /**
     * The part of the card drawn at this position (left to right) that is showing: its rectangle less the cards
     * drawn over it - those to its right, and the highlighted card.
     */
    private Shape visibleShape(int position) {
        Area shape = new Area(rects[position]);
        for (int j = 0; j < rects.length; j++)
            if (j > position && j != cardHighlight || j == cardHighlight && cardHighlight != position)
                shape.subtract(new Area(rects[j]));
        return shape;
    }

    /**
     * The part of the card that was showing when the deck was last drawn, or null if it was not drawn.
     */
    public Shape cardShape(FrenchCard card) {
        if (rects == null || drawnCards == null) return null;
        int position = drawnCards.indexOf(card);
        return position < 0 || position >= rects.length ? null : visibleShape(position);
    }

    /**
     * The card showing at point p (the topmost there), or null for none.
     */
    public FrenchCard cardAt(Point p) {
        int position = positionAt(p);
        return position < 0 ? null : drawnCards.get(position);
    }

    /**
     * The position (left to right) of the card showing at point p - the highlighted card, or else the rightmost
     * there, as each card is drawn over the one to its left - or -1 for none.
     */
    private int positionAt(Point p) {
        if (rects == null) return -1;
        if (cardHighlight >= 0 && cardHighlight < rects.length && rects[cardHighlight].contains(p))
            return cardHighlight;
        for (int i = rects.length - 1; i >= 0; i--)
            if (rects[i].contains(p))
                return i;
        return -1;
    }

    /**
     * Outlines the showing part of each card given, in its colour; the rest are not outlined.
     */
    public void setOutlines(Map<FrenchCard, Color> outlines) {
        this.outlines = new HashMap<>(outlines);
        repaint();
    }

    /**
     * Shows a tooltip computed from the mouse position (null for none).
     */
    public void setToolTips(Function<MouseEvent, String> toolTips) {
        this.toolTips = toolTips;
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        return toolTips == null ? null : toolTips.apply(e);
    }

    /** The cards in the order they are laid out: sorted by displayOrder only when face-up. */
    private List<FrenchCard> inDisplayOrder(Deck<FrenchCard> deck) {
        List<FrenchCard> cards = new ArrayList<>(deck.getComponents());
        if (displayOrder != null && isVisible)
            cards.sort(displayOrder);
        return cards;
    }

    /** Lay out the cards in this order when face-up (e.g. FrenchCard.HAND_DISPLAY_ORDER for a hand). */
    public void setDisplayOrder(Comparator<FrenchCard> displayOrder) {
        this.displayOrder = displayOrder;
    }

    @Override
    public Dimension getPreferredSize(){
        return new Dimension(width, height);
    }

    private Image getCardImage(FrenchCard card){
        Image img = null;
        //String coloName = card.
        switch(card.type){
            case Number:
                img = ImageIO.GetInstance().getImage(dataPath + card.number + card.suite + ".png");
                break;
            case Jack:
            case Queen:
            case King:
            case Ace:
                img = ImageIO.GetInstance().getImage(dataPath + card.type + card.suite + ".png");
                break;

        }
        return img;
    }

    public void setDeck(Deck<FrenchCard> newDeck) {
        this.deck = newDeck;
        this.component = newDeck;
    }


    public int getCardHighlight(){return cardHighlight;}
    public void setCardHighlight(int cardHighlight) {
        this.cardHighlight = cardHighlight;
    }
    public void setFront(boolean visible) {
        this.isVisible = visible;
    }
    public Rectangle[] getRects() {
        return rects;
    }
}

