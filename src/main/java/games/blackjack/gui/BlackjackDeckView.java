package games.blackjack.gui;

import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import gui.views.CardView;
import gui.views.ComponentView;
import utilities.ImageIO;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static games.blackjack.gui.BlackjackGUIManager.*;

public class BlackjackDeckView extends ComponentView {

    Image backOfCard;
    String dataPath;
    int minimumCardOffset = 5;
    Rectangle[] rects;
    int cardHighlight = -1;
    boolean highlighting;

    public BlackjackDeckView(Deck<FrenchCard> d, String dataPath){
        super(d, playerWidth, cardHeight);
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "gray_back.png");
        this.dataPath = dataPath;

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
                if (highlighting){
                    for (int i = 0; i< rects.length; i++){
                        if (rects[i].contains(e.getPoint())){
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
                if (e.getButton() ==1){
                    for (int i = 0; i< rects.length; i++){
                        if (rects[i].contains(e.getPoint())){
                            cardHighlight = i;
                            break;
                        }
                    }
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
        @SuppressWarnings("Unchecked") PartialObservableDeck<FrenchCard> deck = (PartialObservableDeck<FrenchCard>) component;

        if (deck != null){
            int offset = Math.max((rect.width-cardWidth) / deck.getSize(), minimumCardOffset);
            rects = new Rectangle[deck.getSize()];
            for (int i = 0; i < deck.getSize(); i++){
                FrenchCard card = deck.get(i);
                Image cardFace = getCardImage(card);
                Rectangle r = new Rectangle(rect.x + offset * i, rect.y, cardWidth, cardHeight);
                rects[i] = r;
                CardView.drawCard(g, r.x, r.y, r.width, r.height, card, cardFace, backOfCard, deck.isComponentVisible(i, 0));
                g.drawRoundRect(r.x, r.y, r.width, r.height, 15, 15);
            }
            if (cardHighlight != -1){
                FrenchCard card = deck.get(cardHighlight);
                Image cardFace = getCardImage(card);
                Rectangle r = rects[cardHighlight];
                CardView.drawCard(g, r.x, r.y, r.width, r.height, card, cardFace, backOfCard, deck.isComponentVisible(cardHighlight, 0));
                g.drawRoundRect(r.x, r.y, r.width, r.height, 15, 15);
            }
            g.drawString(""+deck.getSize(), rect.x+10, rect.y+cardHeight - size);
        }
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

    public int getCardHighlight(){return cardHighlight;}
    public void setCardHighlight(int cardHighlight) {
        this.cardHighlight = cardHighlight;
    }
    public Rectangle[] getRects() {
        return rects;
    }
}
