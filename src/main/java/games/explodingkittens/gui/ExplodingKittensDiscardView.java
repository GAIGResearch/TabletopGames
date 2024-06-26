package games.explodingkittens.gui;

import core.actions.AbstractAction;
import core.components.Component;
import core.components.Deck;
import games.explodingkittens.actions.*;
import games.explodingkittens.actions.reactions.ChooseSeeTheFutureOrder;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.awt.*;
import java.util.*;

public class ExplodingKittensDiscardView extends ExplodingKittensDeckView {

    // Images for Action Stack
    ArrayList<String> catImagesBkup;

    // This view adds action stack as cards in the discard pile for display
    Stack<AbstractAction> actionStack;

    /**
     * Constructor initialising information and adding key/mouse listener for card highlight (left click or ALT + hover
     * allows showing the highlighted card on top of all others).
     * @param d - deck to draw
     * @param visible - true if whole deck visible
     * @param dataPath - path to assets
     */
    public ExplodingKittensDiscardView(Deck<ExplodingKittensCard> d, Stack<AbstractAction> actionStack, boolean visible, String dataPath) {
        super(-1, d, visible, dataPath);
        this.actionStack = actionStack;
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

        // Add cards played from action stack into the copy of the deck
        for (AbstractAction aa: actionStack) {
            ExplodingKittensCard card = getStackCard(aa);
            if (card != null) {
                deckCopy.add(card);
            }
        }
        // set thie copy to tbe the component we draw
        component = deckCopy;
        // and draw it
        super.drawDeck(g);
        // then reset to the original
        component = oldComponent;
    }

    /**
     * Turns an action into a card
     * @param aa - action to turn to card
     * @return - Exploding kittens card
     */
    private ExplodingKittensCard getStackCard(AbstractAction aa) {
        if (aa instanceof AttackAction) {
            return new ExplodingKittensCard(ExplodingKittensCard.CardType.ATTACK, 0);
        } else if (aa instanceof ChooseSeeTheFutureOrder) {
            return new ExplodingKittensCard(ExplodingKittensCard.CardType.FAVOR, 1);
        } else if (aa instanceof NopeAction) {
            return new ExplodingKittensCard(ExplodingKittensCard.CardType.SEETHEFUTURE, 3);
        } else if (aa instanceof ShuffleAction) {
            return new ExplodingKittensCard(ExplodingKittensCard.CardType.SHUFFLE, 4);
        } else if (aa instanceof SkipAction) {
            return new ExplodingKittensCard(ExplodingKittensCard.CardType.SKIP, 5);
        }
        return null;
    }

}
