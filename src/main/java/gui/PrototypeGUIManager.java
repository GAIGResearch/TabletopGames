package gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.components.Component;
import core.components.Deck;
import games.GameType;
import gui.views.AreaView;
import gui.views.CardView;
import gui.views.ComponentView;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import javax.swing.*;
import java.awt.*;

public class PrototypeGUIManager extends AbstractGUIManager {
    JComponent view;
    int width, height;

    protected ComponentView[] componentViews;
    protected int maxComponentsInDeck = 100;

    public PrototypeGUIManager(GamePanel parent, GameType game, AbstractGameState gameState, ActionController ac, int maxActionSpace) {
        this(parent, game, gameState, ac, maxActionSpace, defaultDisplayWidth, defaultDisplayHeight);
    }

    public PrototypeGUIManager(GamePanel parent, GameType game, AbstractGameState gameState, ActionController ac, int maxActionSpace,
                               int displayWidth, int displayHeight) {
        super(parent, ac, maxActionSpace);
        this.width = displayWidth;
        this.height = displayHeight;

        if (gameState != null) {
            view = new AreaView(gameState.getAllComponents(), width, height);
        } else {
            view = new JPanel();
        }
        JPanel infoPanel = new JPanel();
        if (game != null && gameState != null) {
            infoPanel = createGameStateInfoPanel(game.name(), gameState, width, defaultInfoPanelHeight);
        }
        JComponent actionPanel = createActionPanel(new ScreenHighlight[0], width, defaultActionPanelHeight, true);

        JPanel deckView = new JPanel();
        componentViews = new ComponentView[maxComponentsInDeck];
        for (int i = 0; i < maxComponentsInDeck; i++) {
            CardView cw = new CardView(null);
            cw.setVisible(false);
            deckView.add(cw);
            componentViews[i] = cw;  // TODO: may be other component type
        }
        JScrollPane deckScroll = new JScrollPane(deckView);
        deckScroll.setPreferredSize(new Dimension(width, defaultCardHeight + 20));
        deckScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        JButton expandDeckButton = new JButton(new ImageIcon("data/javagraphics/toolbarButtonGraphics/General/ZoomIn16.gif"));
        expandDeckButton.setOpaque(true);
        expandDeckButton.setBackground(Color.white);
        expandDeckButton.setToolTipText("Expand deck");
        expandDeckButton.addActionListener(e -> {
            for (ComponentView componentView : componentViews) {
                componentView.setVisible(false);
            }
            Deck<? extends Component> deck = ((AreaView)view).getDeckHighlight();
            if (deck != null) {
                for (int i = 0; i < deck.getSize(); i++) {
                    componentViews[i].setVisible(true);
                    componentViews[i].updateComponent(deck.getComponents().get(i));
                }
            }
        });

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(infoPanel);
        north.add(deckScroll);
        north.add(expandDeckButton);

        parent.setLayout(new BorderLayout());
        parent.add(view, BorderLayout.CENTER);
        parent.add(north, BorderLayout.NORTH);
        parent.add(actionPanel, BorderLayout.SOUTH);
        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + defaultCardHeight + 20));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            if (view instanceof AreaView) {
                ((AreaView) view).updateComponent(gameState.getAllComponents());
            } else {
                view = new AreaView(gameState.getAllComponents(), width, height);
            }
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
        }
        parent.repaint();
    }

}
