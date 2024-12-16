package games.saboteur.gui;

import games.saboteur.SaboteurGameState;
import games.saboteur.components.ActionCard;
import games.saboteur.components.PathCard;
import games.saboteur.components.RoleCard;
import games.saboteur.components.SaboteurCard;
import gui.views.DeckView;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

import static games.saboteur.gui.SaboteurBoardView.drawPathCard;

public class SaboteurPlayerView extends DeckView<SaboteurCard> {
    SaboteurGameState gs;
    SaboteurGUIManager gui;
    int idx;
    boolean human;

    public SaboteurPlayerView(SaboteurGUIManager gui, SaboteurGameState gs, int i, Set<Integer> humanID) {
        super(i, gs.getPlayerDecks().get(i), false, SaboteurBoardView.cellWidth, SaboteurBoardView.cellHeight,
                new Rectangle(5,5,SaboteurBoardView.cellWidth * 8,SaboteurBoardView.cellHeight));
        this.gs = gs;
        this.gui = gui;
        this.idx = i;
        this.human = humanID.contains(i);
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, SaboteurCard component, boolean front) {
        // Draw card background and outline
        g.setColor(Color.white);
        g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 2, 2);
        g.setColor(Color.black);
        // todo idx highlight
        if (gui.componentIDHighlight == component.getComponentID() || component instanceof ActionCard ac && gui.actionCardHighlight == ac.actionType)
            g.setColor(Color.green);
        g.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 2, 2);

        if (front) {
            if (component instanceof ActionCard actionCard) {
                g.drawString(""+actionCard.actionType.name().charAt(0), rect.x + 5, rect.y + 20);
            } else if (component instanceof PathCard) {
                drawPathCard(g, (PathCard) component, rect.x, rect.y);
            } else if (component instanceof RoleCard roleCard) {
                g.drawString(""+roleCard.type.name().charAt(0), rect.x + 5, rect.y + 20);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    public void update(boolean front) {
        setFront(front);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(SaboteurBoardView.cellWidth*8, SaboteurBoardView.cellHeight*2);
    }
}
