package games.coltexpress.gui;

import core.components.Deck;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressTypes;
import games.coltexpress.actions.roundcardevents.RoundEvent;
import games.coltexpress.cards.RoundCard;
import games.coltexpress.components.Compartment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;

import static games.coltexpress.gui.ColtExpressGUIManager.*;

public class ColtExpressRoundView extends JComponent {

    double scale = 1.0;
    int panX, panY;

    // Width and height of view
    int width, height;
    // View for round cards deck
    ColtExpressDeckView<RoundCard> roundView;

    public ColtExpressRoundView(List<Compartment> train, int nRounds, String dataPath,
                                HashMap<Integer, ColtExpressTypes.CharacterType> characters) {
        int nCars = train.size();
        this.width = Math.min(trainCarWidth*3/2*nCars, nRounds*roundCardWidth);
        this.height = (int)((roundCardHeight + 20) * 1.5);
        roundView = new ColtExpressDeckView<>(null, true, dataPath, characters);

        addMouseWheelListener(e -> {
            double amount = 0.2 * Math.abs(e.getPreciseWheelRotation());
            if (e.getPreciseWheelRotation() > 0) {
                // Rotated down, zoom out
                updateScale(scale - amount);
            } else {
                updateScale(scale + amount);
            }
        });
        addMouseListener(new MouseAdapter() {
            Point start;

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    // Middle (wheel) click, pan around
                    start = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2 && start != null) {
                    // Middle (wheel) click, pan around
                    Point end = e.getPoint();
                    panX += (int) (scale * (end.x - start.x));
                    panY += (int) (scale * (end.y - start.y));
                    start = null;
                }
            }
        });
    }

    private void updateScale(double scale) {
        this.scale = scale;
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Draw round cards
        roundView.drawDeck((Graphics2D) g, new Rectangle(panX, panY, (int)(width*scale), (int)(roundCardHeight*scale)), false, scale);
        Deck<RoundCard> deck = (Deck<RoundCard>) roundView.getComponent();
        if (deck != null && deck.getSize() > 0) {
            String text = "<html>";
            for (int i = 0; i < deck.getSize(); i++) {
                boolean visible = roundView.cegs.getTurnOrder().getRoundCounter() >= i;
                boolean current = roundView.cegs.getTurnOrder().getRoundCounter() == i;
                if (visible) {
                    if (current) text += "[CURRENT] ";
                    RoundEvent re = roundView.cegs.getRounds().get(i).getEndRoundCardEvent();
                    if (re != null) {
                        text += "<b>Round " + i + "</b>: " + re.getEventText() + "<br/>";
                    } else {
                        text += "<b>Round " + i + "</b>: No event.<br/>";
                    }
                } else {
                    text += "Round " + i + ": unknown<br/>";
                }
            }
            text += "</html>";
            setToolTipText(text);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    /**
     * Updates information based on current game state.
     * @param cegs - current game state.
     */
    public void update(ColtExpressGameState cegs) {
        this.roundView.updateComponent(cegs.getRounds());
        this.roundView.updateGameState(cegs);
    }
}
