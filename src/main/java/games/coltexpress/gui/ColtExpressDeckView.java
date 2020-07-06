package games.coltexpress.gui;

import core.actions.AbstractAction;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressTypes;
import games.coltexpress.actions.roundcardevents.*;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.cards.RoundCard;
import games.coltexpress.components.Loot;
import gui.views.CardView;
import gui.views.ComponentView;
import utilities.ImageIO;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import static games.coltexpress.gui.ColtExpressGUI.*;

public class ColtExpressDeckView<T extends Component> extends ComponentView {
    protected boolean front;
    Image backOfCard;
    String dataPath;
    int minCardOffset = 5;

    Rectangle[] rects;
    int cardHighlight = -1;
    int activePlayer = -1;
    Color marshalColor = new Color(242, 189, 24);

    HashMap<Integer, ColtExpressTypes.CharacterType> characters;
    ColtExpressGameState cegs;

    public ColtExpressDeckView(Deck<T> d, boolean visible, String dataPath,
                               HashMap<Integer, ColtExpressTypes.CharacterType> characters) {
        super(d, playerAreaWidth, ceCardHeight);
        this.front = visible;
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "CardBack.png");
        this.dataPath = dataPath;
        this.characters = characters;

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

    @Override
    protected void paintComponent(Graphics g) {
        drawDeck((Graphics2D) g, new Rectangle(0, 0, width, height));
    }

    public void setFront(boolean visible) {
        this.front = visible;
    }

    public void flip() {
        front = !front;
    }

    public void drawDeck(Graphics2D g, Rectangle rect) {
        int size = g.getFont().getSize();
        Deck<T> deck = (Deck<T>) component;

        if (deck != null && deck.getSize() > 0) {
            // Draw cards, 0 index on top
            rects = new Rectangle[deck.getSize()];
            for (int i = deck.getSize()-1; i >= 0; i--) {
                if (deck.get(0) instanceof ColtExpressCard) {
                    // Player card
                    int offset = (rect.width-ceCardWidth) / deck.getSize();
                    Rectangle r = new Rectangle(rect.x + offset * i, rect.y, ceCardWidth, ceCardHeight);
                    rects[i] = r;
                    drawCard(g, (ColtExpressCard) deck.get(i), r, deck instanceof PartialObservableDeck ?
                            activePlayer != -1 && ((PartialObservableDeck) deck).isComponentVisible(i, activePlayer) : front);
                } else if (deck.get(0) instanceof Loot) {
                    // Loot
                    int offset = (rect.width-defaultItemSize) / deck.getSize();
                    Rectangle r = new Rectangle(rect.x + offset * i, rect.y, defaultItemSize, defaultItemSize);
                    rects[i] = r;
                    drawLoot(g, (Loot) deck.get(i), r, front);
                } else {
                    // Round card
                    int offset = (rect.width-roundCardWidth) / deck.getSize();
                    Rectangle r = new Rectangle(rect.x + offset * i, rect.y, roundCardWidth, roundCardHeight);
                    rects[i] = r;
                    boolean visible = (cegs.getTurnOrder().getRoundCounter() >= i);
                    drawRoundCard(g, (RoundCard) deck.get(i), r, visible);
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
                    int offset = (rect.width-roundCardWidth) / deck.getSize();
                    Rectangle r = new Rectangle(rect.x + offset * cardHighlight, rect.y, roundCardWidth, roundCardHeight);
                    boolean visible = (cegs.getTurnOrder().getRoundCounter() >= cardHighlight);
                    drawRoundCard(g, (RoundCard) deck.get(cardHighlight), r, visible);
                }
            }
            if (!(deck.get(0) instanceof RoundCard)) {
                g.setColor(Color.black);
                g.drawString("" + deck.getSize(), rect.x + 10, rect.y + rect.height - size);
            }
        }
    }

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

    private void drawLoot(Graphics2D g, Loot loot, Rectangle r, boolean visible) {
        Image lootFace = ImageIO.GetInstance().getImage(dataPath + loot.getLootType().name() + "_behind.png");
        g.drawImage(lootFace, r.x, r.y, r.width, r.height, null);
    }

    private void drawRoundCard(Graphics2D g, RoundCard card, Rectangle r, boolean visible) {
        Image cardFace = ImageIO.GetInstance().getImage(dataPath + "roundcards/roundCard.png");
        g.drawImage(cardFace, r.x, r.y, r.width, r.height, null);

        if (visible) {
            // Draw title
            Font f = g.getFont();
            g.setFont(new Font(f.getName(), Font.PLAIN, 8));
            g.setColor(Color.black);
            g.drawString(card.getComponentName(), r.x + 5, r.y + 13);
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
            int singleWidth = (int) (sImg.getWidth(null) * scaleW);
            int roundHeight = (int) (sImg.getHeight(null) * scaleH);
            int doubleWidth = (int) (dImg.getWidth(null) * scaleW);

            int x = r.x + r.width/2 - ((nRounds-nDouble) * singleWidth + nDouble * doubleWidth)/2;
            for (int i = 0; i < card.getTurnTypes().length; i++) {
                Image roundImg = ImageIO.GetInstance().getImage(dataPath + "roundcards/" + card.getTurnTypes()[i].name() + ".png");
                int roundWidth = (int) (roundImg.getWidth(null) * scaleW);
                g.drawImage(roundImg, x, r.y + r.height / 3, roundWidth, roundHeight, null);
                x += roundWidth;
            }

            // Draw end event
            AbstractAction event = card.getEndRoundCardEvent();
            if (event != null) {
                Image endImg = getEndImage(event);
                if (endImg != null) {
                    int roundWidth = (int) (endImg.getWidth(null) * scaleW);
                    int rHeight = (int) (endImg.getHeight(null) * scaleH);
                    g.drawImage(endImg, r.x + r.width / 2 - roundWidth / 2, r.y + r.height * 2 / 3, roundWidth, rHeight, null);
                }
            }
        }
    }

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
}
