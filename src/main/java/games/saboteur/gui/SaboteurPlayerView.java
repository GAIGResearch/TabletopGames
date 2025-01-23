package games.saboteur.gui;

import games.saboteur.SaboteurGameState;
import games.saboteur.components.ActionCard;
import games.saboteur.components.PathCard;
import games.saboteur.components.RoleCard;
import games.saboteur.components.SaboteurCard;
import gui.views.DeckView;

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
                new Rectangle(7,7,SaboteurBoardView.cellWidth * 8,SaboteurBoardView.cellHeight));
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
                /*
                 RockFall,
                 BrokenTools,
                 FixTools,
                 Map
                 */
                g.drawString(actionCard.actionType.shortString(), rect.x + 5, rect.y + 20);
                if (actionCard.actionType == ActionCard.ActionCardType.BrokenTools) {
                    ActionCard.ToolCardType[] toolTypes = actionCard.toolTypes;
                    for (int i = 0; i < toolTypes.length; i++) {
                        g.drawString(toolTypes[i].shortString(), rect.x + 5, rect.y + 35 + i*15);
                    }
                } else if (actionCard.actionType == ActionCard.ActionCardType.FixTools) {
                    ActionCard.ToolCardType[] toolTypes = actionCard.toolTypes;
                    for (int i = 0; i < toolTypes.length; i++) {
                        g.drawString(toolTypes[i].shortString(), rect.x + 5, rect.y + 35 + i*15);
                    }
                }
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

        // Draw player role and number of nugget cards
        g.setColor(Color.black);
        g.drawString("Role: " + gs.getRole(idx).name() + "; nuggetCards: " + gs.getPlayerNuggetDecks().get(idx).getSize(), 7, (int) (SaboteurBoardView.cellHeight * 1.5));

        // Draw tool status
        String tools = "";
        for (ActionCard.ToolCardType tt: ActionCard.ToolCardType.values()) {
            tools += tt.name() + ": " + (gs.isToolFunctional(idx, tt) ? "ok" : "broken") + "   ";
        }
        g.drawString(tools, 7, (int) (SaboteurBoardView.cellHeight * 1.5) + 20);
    }

    public void update(boolean front) {
        setFront(front);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(SaboteurBoardView.cellWidth*8, SaboteurBoardView.cellHeight*3);
    }
}
