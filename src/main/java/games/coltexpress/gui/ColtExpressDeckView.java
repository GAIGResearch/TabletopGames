package games.coltexpress.gui;

import core.actions.AbstractAction;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressTurnOrder;
import games.coltexpress.ColtExpressTypes;
import games.coltexpress.actions.roundcardevents.*;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.cards.RoundCard;
import games.coltexpress.components.Loot;
import gui.views.CardView;
import gui.views.ComponentView;
import utilities.ImageIO;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import static games.coltexpress.gui.ColtExpressGUIManager.*;

public class ColtExpressDeckView<T extends Component> extends ComponentView {

    // Is deck fully visible?
    protected boolean front;
    // Is first component in deck drawn on top?
    boolean firstOnTop;
    // Back of card image
    Image backOfCard;
    // Path to assets
    String dataPath;
    // Minimum distance between components drawn
    int minCardOffset = 5;

    // Rectangles where components are drawn, used for highlighting
    Rectangle[] rects;
    // Index of component highlighted
    int cardHighlight = -1;
    // If currently highlighting a component (on ALT press)
    boolean highlighting;

    // ID of currently active player
    int activePlayer = -1;
    // Color of marshal
    Color marshalColor = new Color(242, 189, 24);
    // List of player characters, corresponding to player IDs
    HashMap<Integer, ColtExpressTypes.CharacterType> characters;
    // Current game state
    ColtExpressGameState cegs;

    /**
     * Constructor for a deck view, adding a mouse/key listener to allow highlight of component (highlighted component is drawn
     * on top of all others).
     * @param d - deck to draw
     * @param visible - if deck is visible
     * @param dataPath - path to assets for the deck
     * @param characters - list of player characters
     */
    public ColtExpressDeckView(Deck<T> d, boolean visible, String dataPath,
                               HashMap<Integer, ColtExpressTypes.CharacterType> characters) {
        super(d, playerAreaWidth, ceCardHeight);
        this.front = visible;
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "CardBack.png");
        this.dataPath = dataPath;
        this.characters = characters;

        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ALT) {
                    highlighting = true;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ALT) {
                    highlighting = false;
                    cardHighlight = -1;
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (highlighting) {
                    for (int i = 0; i < rects.length; i++) {
                        if (rects[i].contains(e.getPoint())) {
                            cardHighlight = i;
                            break;
                        }
                    }
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == 1) {
                    // Left click, highlight
                    for (int i = 0; i < rects.length; i++) {
                        if (rects[i].contains(e.getPoint())) {
                            cardHighlight = i;
                            break;
                        }
                    }
                } else {
                    // Other click, reset highlight
                    cardHighlight = -1;
                }
            }
        });
    }

    double scale = 1.0;

    @Override
    protected void paintComponent(Graphics g) {
        drawDeck((Graphics2D) g, new Rectangle(0, 0, width, height), firstOnTop, scale);
    }

    /**
     * Draws all components in a deck, spaced out so they fill the given area.
     * @param g - Graphics object
     * @param rect - rectangle to draw deck in.
     * @param firstOnTop - if true, first component is drawn on top, otherwise on bottom.
     */
    public void drawDeck(Graphics2D g, Rectangle rect, boolean firstOnTop, double scale) {
        this.scale = scale;

        int size = (int)(g.getFont().getSize() * scale);
        Deck<T> deck = (Deck<T>) component;

        if (deck != null && deck.getSize() > 0) {
            // Draw cards, 0 index on top
            rects = new Rectangle[deck.getSize()];
            int i = deck.getSize()-1;
            if (firstOnTop) i = 0;
            while (i >= 0 && !firstOnTop || i < deck.getSize() && firstOnTop) {
                if (deck.get(0) instanceof ColtExpressCard) {
                    // Player card
                    int offset = (rect.width-ceCardWidth) / deck.getSize();
                    Rectangle r = new Rectangle(rect.x + offset * i, rect.y, (int)(ceCardWidth * scale), (int)(ceCardHeight * scale));
                    rects[i] = r;
                    drawCard(g, (ColtExpressCard) deck.get(i), r, deck instanceof PartialObservableDeck ?
                            activePlayer != -1 && ((PartialObservableDeck) deck).isComponentVisible(i, activePlayer) : front);
                } else if (deck.get(0) instanceof Loot) {
                    // Loot
                    int offset = (rect.width-(int)(defaultItemSize * scale)) / deck.getSize();
                    Rectangle r = new Rectangle(rect.x + offset * i, rect.y, (int)(defaultItemSize * scale), (int)(defaultItemSize * scale));
                    rects[i] = r;
                    drawLoot(g, (Loot) deck.get(i), r, front);
                } else {
                    // Round card
                    int offset = (rect.width-(int)(roundCardWidth*scale)) / deck.getSize();
                    Rectangle r = new Rectangle(rect.x + offset * i, rect.y, (int)(roundCardWidth*scale), (int)(roundCardHeight*scale));
                    rects[i] = r;
                    boolean visible = cegs.getTurnOrder().getRoundCounter() >= i;
                    boolean current = cegs.getTurnOrder().getRoundCounter() == i;
                    drawRoundCard(g, (RoundCard) deck.get(i), r, visible, current);
                }

                if (firstOnTop) {
                    i++;
                } else {
                    i--;
                }
            }
            if (cardHighlight != -1) {
                // Draw this one on top
                if (deck.get(0) instanceof ColtExpressCard) {
                    // Player card
                    int offset = (rect.width-ceCardWidth) / deck.getSize();
                    Rectangle r = new Rectangle(rect.x + offset * cardHighlight, rect.y, ceCardWidth, ceCardHeight);
                    drawCard(g, (ColtExpressCard) deck.get(cardHighlight), r, (deck instanceof PartialObservableDeck ?
                            (activePlayer != -1 && cardHighlight != -1 && ((PartialObservableDeck) deck).isComponentVisible(cardHighlight, activePlayer)) : front));
                } else if (deck.get(0) instanceof Loot) {
                    // Loot
                    int offset = (rect.width-defaultItemSize) / deck.getSize();
                    Rectangle r = new Rectangle(rect.x + offset * cardHighlight, rect.y, defaultItemSize, defaultItemSize);
                    drawLoot(g, (Loot) deck.get(cardHighlight), r, front);
                } else {
                    // Round card
                    int offset = (rect.width-(int)(roundCardWidth*scale)) / deck.getSize();
                    Rectangle r = new Rectangle(rect.x + offset * cardHighlight, rect.y, (int)(roundCardWidth*scale), (int)(roundCardHeight*scale));
                    boolean visible = cegs.getTurnOrder().getRoundCounter() >= cardHighlight;
                    boolean current = cegs.getTurnOrder().getRoundCounter() == cardHighlight;
                    drawRoundCard(g, (RoundCard) deck.get(cardHighlight), r, visible, current);
                }
            }
            if (deck.get(0) instanceof ColtExpressCard) {
                g.setColor(Color.black);
                g.drawString("" + deck.getSize(), rect.x + 10, rect.y + rect.height - size);
            }
        }
    }

    /**
     * Draws a player card, with player's color border.
     * @param g - Graphics object
     * @param card - card to draw
     * @param r - rectangle to draw card in
     * @param visible - true if visible, false otherwise (shows back of card instead and no color)
     */
    private void drawCard(Graphics2D g, ColtExpressCard card, Rectangle r, boolean visible) {
        Image cardFace = ImageIO.GetInstance().getImage(dataPath + "characters/deck/" + card.cardType.name() + ".png");
        CardView.drawCard(g, r.x, r.y, r.width, r.height, card, cardFace, backOfCard, visible);
        // Draw border around card in player owner's color
        if (visible) {
            Stroke s = g.getStroke();
            int width = 6;
            int arc = 20;
            g.setStroke(new BasicStroke(width));
            if (card.playerID == -1) {
                // Marshal/neutral bullet
                g.setColor(marshalColor);
            } else {
                // Player card/bullet from other player
                g.setColor(characters.get(card.playerID).getColor());
            }
            g.drawRoundRect(r.x + width / 2, r.y + width / 2, r.width - width, r.height - width, arc, arc);
            g.setStroke(s);
        }
    }

    /**
     * Draws loot, with value information only if visible.
     * @param g - Graphics object
     * @param loot - loot to draw
     * @param r - rectangle to draw loot in
     * @param visible - true if visible, false otherwise
     */
    private void drawLoot(Graphics2D g, Loot loot, Rectangle r, boolean visible) {
        Image lootFace;
        if (visible) {
            lootFace = ImageIO.GetInstance().getImage(dataPath + loot.getLootType().name() + "_" + loot.getValue() + ".png");
            if (lootFace == null) lootFace = ImageIO.GetInstance().getImage(dataPath + loot.getLootType().name() + "_front.png");
        } else {
            lootFace = ImageIO.GetInstance().getImage(dataPath + loot.getLootType().name() + "_behind.png");
        }
        g.drawImage(lootFace, r.x, r.y, r.width, r.height, null);
    }

    /**
     * Draws a round card
     * @param g - Graphics object
     * @param card - card to draw
     * @param r - rectangle to draw card in
     * @param visible - true if card is visible, false otherwise (details will not be drawn until revealed)
     * @param currentRound - true if this is the current round
     */
    private void drawRoundCard(Graphics2D g, RoundCard card, Rectangle r, boolean visible, boolean currentRound) {
        Image cardFace = ImageIO.GetInstance().getImage(dataPath + "roundcards/roundCard.png");
        g.drawImage(cardFace, r.x, r.y, r.width, r.height, null);

        if (visible) {
            // Draw title
            Font f = g.getFont();
            g.setFont(new Font(f.getName(), Font.PLAIN, (int)(9 * scale)));
            g.setColor(Color.black);
            g.drawString(card.getComponentName(), r.x + (int)(5 * scale), r.y + (int)(17 * scale));
            g.setFont(f);

            double scaleW = 1.0 * r.width / cardFace.getWidth(null);
            double scaleH = 1.0 * r.height / cardFace.getHeight(null);

            // Draw round types
            int nDouble = 0;
            int nRounds = card.getTurnTypes().length;
            for (int i = 0; i < nRounds; i++) {
                if (card.getTurnTypes()[i] == RoundCard.TurnType.DoubleTurn) {
                    nDouble++;
                }
            }

            Image sImg = ImageIO.GetInstance().getImage(dataPath + "roundcards/NormalTurn.png");
            Image dImg = ImageIO.GetInstance().getImage(dataPath + "roundcards/DoubleTurn.png");
            int singleWidth = (int) (sImg.getWidth(null) * scaleW * 1.2);
            int roundHeight = (int) (sImg.getHeight(null) * scaleH);
            int doubleWidth = (int) (dImg.getWidth(null) * scaleW * 1.2);

            int x = r.x + r.width/2 - ((nRounds-nDouble) * singleWidth + nDouble * doubleWidth)/2;
            for (int i = 0; i < card.getTurnTypes().length; i++) {
                Image roundImg = ImageIO.GetInstance().getImage(dataPath + "roundcards/" + card.getTurnTypes()[i].name() + ".png");
                int roundWidth = (int) (roundImg.getWidth(null) * scaleW * 1.2);
                g.drawImage(roundImg, x, r.y + r.height / 3, roundWidth, roundHeight, null);
                // Highlight current turn in current round
                if (currentRound && ((ColtExpressTurnOrder)cegs.getTurnOrder()).getFullPlayerTurnCounter() == i) {
                    g.setColor(Color.green);
                    Stroke s = g.getStroke();
                    g.setStroke(new BasicStroke(3));
                    g.drawRect(x, r.y + r.height/3, roundWidth, roundHeight);
                    g.setStroke(s);
                }
                x += roundWidth;
            }

            // Draw end event
            AbstractAction event = card.getEndRoundCardEvent();
            if (event != null) {
                Image endImg = getEndImage(event);
                if (endImg != null) {
                    int roundWidth = (int) (endImg.getWidth(null) * scaleW);
                    int rHeight = (int) (endImg.getHeight(null) * scaleH);
                    g.drawImage(endImg, r.x + r.width / 2 - roundWidth / 2, (int)(r.y + r.height * 2 / 3 - 5*scale), roundWidth, rHeight, null);
                }
            }
        }
    }

    /**
     * Get the image corresponding to the end of round event.
     * @param event - end of round event
     * @return - image.
     */
    private Image getEndImage(AbstractAction event) {
        String path = null;
        if (event instanceof EndCardHostage) {
            path = dataPath + "roundcards/hostage.png";
        } else if (event instanceof EndCardMarshallsRevenge) {
            path = dataPath + "roundcards/marshalRevenge.png";
        } else if (event instanceof EndCardPickPocket) {
            path = dataPath + "roundcards/pickpocket.png";
        } else if (event instanceof RoundCardAngryMarshall) {
            path = dataPath + "roundcards/angryMarshal.png";
        } else if (event instanceof RoundCardBraking) {
            path = dataPath + "roundcards/braking.png";
        } else if (event instanceof RoundCardPassengerRebellion) {
            path = dataPath + "roundcards/rebellion.png";
        } else if (event instanceof RoundCardSwivelArm) {
            path = dataPath + "roundcards/swivelArm.png";
        } else if (event instanceof RoundCardTakeItAll) {
            path = dataPath + "roundcards/takeItAll.png";
        }
        if (path != null) return ImageIO.GetInstance().getImage(path);
        else return null;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    // Getters, setters
    public int getCardHighlight() {
        return cardHighlight;
    }
    public void setCardHighlight(int cardHighlight) {
        this.cardHighlight = cardHighlight;
    }
    public Rectangle[] getRects() {
        return rects;
    }
    public void informActivePlayer(int player) {
        this.activePlayer = player;
    }
    public void updateGameState(ColtExpressGameState cegs) {
        this.cegs = cegs;
    }
    public void setFirstOnTop(boolean firstOnTop) {
        this.firstOnTop = firstOnTop;
    }
    public void setFront(boolean visible) {
        this.front = visible;
    }
    public void flip() {
        front = !front;
    }
}
