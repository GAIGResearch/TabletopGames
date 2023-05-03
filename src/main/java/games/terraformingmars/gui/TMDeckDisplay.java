package games.terraformingmars.gui;

import core.components.Deck;
import games.terraformingmars.TMGameState;
import games.terraformingmars.components.TMCard;
import gui.IScreenHighlight;
import utilities.ImageIO;
import utilities.Vector2D;

import javax.swing.*;
import java.awt.*;

import static utilities.GUIUtils.*;

public class TMDeckDisplay extends JComponent implements IScreenHighlight {

    private Deck<TMCard> deck;
    TMGameState gs;

    int width, height;

    int maxCards = 200;
    static int cardHeight = 200;
    static int cardWidth;

    TMCardView[] cardViews;
    boolean horizontal;

    public TMDeckDisplay(TMGUI gui, TMGameState gs, Deck<TMCard> deck, boolean horizontal) {
        this.gs = gs;
        this.deck = deck;
        this.horizontal = horizontal;

        Image projCardBg = ImageIO.GetInstance().getImage("data/terraformingmars/images/cards/proj-card-bg.png");
        Vector2D dim = scaleLargestDimImg(projCardBg, cardHeight);
        cardWidth = dim.getX();
        if (deck != null) {
            if (horizontal) {
                width = (deck.getSize() + 1) * cardWidth;
                height = cardHeight;
            } else {
                width = cardWidth;
                height = (deck.getSize() + 1) * cardHeight;
            }
        } else {
            width = cardWidth;
            height = cardHeight;
        }

        setLayout(new FlowLayout());
//        setLayout(new BoxLayout(this, horizontal? BoxLayout.X_AXIS : BoxLayout.Y_AXIS));

        cardViews = new TMCardView[maxCards];
        for (int i = 0; i < maxCards; i++) {
            cardViews[i] = new TMCardView(gs, null, i, cardWidth, cardHeight);
            cardViews[i].informGUI(gui);
            add(cardViews[i]);
        }
        for (int i = 0; i < maxCards; i++) {
            cardViews[i].informOtherViews(cardViews);
        }
    }

    public int getHighlightIndex() {
        for (int i = 0; i < deck.getSize(); i++) {
            if (cardViews[i].clicked) return i;
        }
        return -1;
    }

    public void clearHighlights() {
        for (int i = 0; i < maxCards; i++) {
            cardViews[i].clicked = false;
            cardViews[i].repaint();
        }
    }

    public void update(Deck<TMCard> deck, boolean highlightFirst) {

        if (horizontal) {
            width = (deck.getSize() + 1) * cardWidth;
        } else {
            height = (deck.getSize() + 1) * cardHeight;
        }

        if (deck.getSize() > 0) {
            for (int i = 0; i < deck.getSize(); i++) {
//            if (playerHand.isComponentVisible(i, gs.getCurrentPlayer())) {
                if (deck.get(i) != null) {
                    cardViews[i].update(gs, deck.get(i), i);
                    cardViews[i].repaint();
                }
//            }
            }
        }

        for (int i = deck.getSize(); i < maxCards; i++) {
            cardViews[i].update(gs, null, -1);
            cardViews[i].repaint();
        }

        if (highlightFirst && !cardViews[0].clicked) {
            cardViews[0].clicked = true;
            cardViews[0].repaint();
        }

        this.deck = deck;
        revalidate();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
}
