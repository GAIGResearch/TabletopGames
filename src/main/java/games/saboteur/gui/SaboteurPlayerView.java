package games.saboteur.gui;

import games.saboteur.SaboteurGameState;
import games.saboteur.components.SaboteurCard;
import gui.views.DeckView;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class SaboteurPlayerView extends JComponent {
    SaboteurGameState gs;
    int idx;
    boolean human;

    DeckView<SaboteurCard> handCards;

    public SaboteurPlayerView(SaboteurGameState gs, int i, Set<Integer> humanID) {
        this.gs = gs;
        this.idx = i;
        this.human = humanID.contains(i);
        this.handCards = new DeckView<>(i, gs.getPlayerDecks().get(i), false, SaboteurGUIManager.cardWidth, SaboteurGUIManager.cardHeight, new Rectangle(0,0,0,0)) {  // todo rect
            @Override
            public void drawComponent(Graphics2D g, Rectangle rect, SaboteurCard component, boolean front) {
                // todo draw card
            }
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    public void update(boolean front) {
        handCards.setFront(front);
    }
}
