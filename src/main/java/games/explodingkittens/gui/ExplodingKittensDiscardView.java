package games.explodingkittens.gui;

import core.components.Component;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.awt.*;
import java.util.ArrayList;
import java.util.Stack;

public class ExplodingKittensDiscardView extends ExplodingKittensDeckView {

    // Images for Action Stack
    ArrayList<String> catImagesBkup;

    /**
     * Constructor initialising information and adding key/mouse listener for card highlight (left click or ALT + hover
     * allows showing the highlighted card on top of all others).
     * @param d - deck to draw
     * @param visible - true if whole deck visible
     * @param dataPath - path to assets
     */
    public ExplodingKittensDiscardView(Deck<ExplodingKittensCard> d, boolean visible, String dataPath) {
        super(-1, d, visible, dataPath);
        catImagesBkup = new ArrayList<>();
        catImagesBkup.addAll(catImages);
    }

    /**
     * In the case of ExplodingKittens Discard we also display the contents of the action stack
     * @param g - Graphics object
     */
    public void drawDeck(Graphics2D g) {
        Component oldComponent = component;
        @SuppressWarnings("unchecked") Deck<ExplodingKittensCard> deckCopy = ((Deck<ExplodingKittensCard>) component).copy();

        // set thie copy to tbe the component we draw
        component = deckCopy;
        // and draw it
        super.drawDeck(g);
        // then reset to the original
        component = oldComponent;
    }


}
