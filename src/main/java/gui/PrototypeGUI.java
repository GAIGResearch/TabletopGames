package gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.components.Component;
import core.components.Deck;
import games.GameType;
import gui.views.AreaView;
import gui.views.CardView;
import gui.views.ComponentView;
import players.ActionController;
import players.HumanGUIPlayer;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class PrototypeGUI extends AbstractGUI {
    JComponent view;
    int width, height;

    protected ComponentView[] componentViews;
    protected int maxCardsInDeck = 100;

    public PrototypeGUI(GameType game, AbstractGameState gameState, ActionController ac, int maxActionSpace) {
        this(game, gameState, ac, maxActionSpace, defaultDisplayWidth, defaultDisplayHeight);
    }

    public PrototypeGUI(GameType game, AbstractGameState gameState, ActionController ac, int maxActionSpace,
                        int displayWidth, int displayHeight) {
        super(ac, maxActionSpace);
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
        JComponent actionPanel = createActionPanel(new Collection[0], width, defaultActionPanelHeight);

        JPanel deckView = new JPanel();
        componentViews = new ComponentView[maxCardsInDeck];
        for (int i = 0; i < maxCardsInDeck; i++) {
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

        getContentPane().add(view, BorderLayout.CENTER);
        getContentPane().add(north, BorderLayout.NORTH);
        getContentPane().add(actionPanel, BorderLayout.SOUTH);

        setFrameProperties();
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
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + defaultCardHeight + 20);
    }
}
