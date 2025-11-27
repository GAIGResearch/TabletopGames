package games.seasaltpaper.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.cards.SeaSaltPaperCard;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class SSPGUIManager extends AbstractGUIManager {
    final static int cardWidth = 100;
    final static int cardHeight = 100;
    final static int playerAreaWidth = 600;
    final static int playerAreaHeight = cardHeight;

    SSPPlayerView[] playerViews;
    SSPDeckView discardPile1, discardPile2; //TODO make this generic for any number of discard piles?
    SSPDeckView drawPile;

    public SSPGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);

        this.width = 1000;
        this.height = 1000;

        AbstractGameState gameState = game.getGameState();
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) game.getGameState();
        String dataPath = "data/seasaltpaper/"; // TODO get this from game parameter

        JPanel mainGameArea = new JPanel();
        mainGameArea.setLayout(new BoxLayout(mainGameArea, BoxLayout.Y_AXIS));

        JTabbedPane playerArea = new JTabbedPane();
        playerViews = new SSPPlayerView[sspgs.getNPlayers()];

        for (int i=0; i<gameState.getNPlayers(); i++) {
            PartialObservableDeck<SeaSaltPaperCard> playerHand = sspgs.getPlayerHands().get(i);
            Deck<SeaSaltPaperCard> playerDiscard = sspgs.getPlayerDiscards().get(i);
            SSPPlayerView playerView = new SSPPlayerView(sspgs, playerHand, playerDiscard, i, dataPath);
            playerViews[i] = playerView;

            playerArea.addTab("Player " + i, playerView);
        }

        JPanel drawDiscardPanel = new JPanel();
        drawDiscardPanel.setLayout(new BoxLayout(drawDiscardPanel, BoxLayout.X_AXIS));
        // TODO: make this generic for multiple number of discard piles (?)
        drawPile = new SSPDeckView(-1, sspgs.getDrawPile(), gameState.getCoreGameParameters().alwaysDisplayFullObservable, dataPath, new Rectangle(0, 0, cardWidth, cardHeight));
        discardPile1 = new SSPDeckView(-1, sspgs.getDiscardPiles()[0], true, dataPath, new Rectangle(0, 0, cardWidth, cardHeight));
        discardPile2 = new SSPDeckView(-1, sspgs.getDiscardPiles()[1], true, dataPath, new Rectangle(0, 0, cardWidth, cardHeight));
        drawDiscardPanel.add(drawPile);
        drawDiscardPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        drawDiscardPanel.add(discardPile1);
        drawDiscardPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        drawDiscardPanel.add(discardPile2);
        discardPile1.setFocusable(true);
        discardPile2.setFocusable(true);

        // Set up game area
        mainGameArea.add(playerArea);
        mainGameArea.add(drawDiscardPanel, BorderLayout.CENTER);

        JPanel infoPanel = createGameStateInfoPanel("Sea Salt and Paper", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false, true, null, null, null);

//        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + 20));
        parent.setLayout(new BorderLayout());
        parent.add(mainGameArea, BorderLayout.CENTER);
        parent.add(infoPanel, BorderLayout.NORTH);
        parent.add(actionPanel, BorderLayout.SOUTH);
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();

    }

    @Override
    public int getMaxActionSpace() {
        return 10;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
    }
}
