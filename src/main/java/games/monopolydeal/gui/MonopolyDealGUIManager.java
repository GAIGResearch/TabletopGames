package games.monopolydeal.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.components.Deck;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.MonopolyDealParameters;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.SetType;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Set;

/**
 * <p>This class allows the visualisation of the game. The game components (accessible through {@link Game#getGameState()}
 * should be added into {@link javax.swing.JComponent} subclasses (e.g. {@link javax.swing.JLabel},
 * {@link javax.swing.JPanel}, {@link javax.swing.JScrollPane}; or custom subclasses such as those in {@link gui} package).
 * These JComponents should then be added to the <code>`parent`</code> object received in the class constructor.</p>
 *
 * <p>An appropriate layout should be set for the parent GamePanel as well, e.g. {@link javax.swing.BoxLayout} or
 * {@link java.awt.BorderLayout} or {@link java.awt.GridBagLayout}.</p>
 *
 * <p>Check the super class for methods that can be overwritten for a more custom look, or
 * {@link games.terraformingmars.gui.TMGUI} for an advanced game visualisation example.</p>
 *
 * <p>A simple implementation example can be found in {@link games.tictactoe.gui.TicTacToeGUIManager}.</p>
 */
public class MonopolyDealGUIManager extends AbstractGUIManager {

    // Display Settings
    final static int playerAreaWidth = 800;
    final static int MonopolyDealCardWidth = 55;
    final static int MonopolyDealCardHeight = 80;

    int activePlayer = -1;
    MonopolyDealDeckView discardPile;
    MonopolyDealDeckView drawPile;
    MDealPlayerView[] playerViews;

    Border[] playerViewBorders;
    Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public MonopolyDealGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanID) {
        super(parent, game, ac, humanID);
        if (game != null) {
            parent.setBgColor(Color.white);
            AbstractGameState gameState = game.getGameState();
            // Initialise active player
            activePlayer = gameState.getCurrentPlayer();

            // Find required size of window
            int nPlayers = gameState.getNPlayers();
            this.width = playerAreaWidth + 20;
            this.height = defaultInfoPanelHeight + defaultActionPanelHeight + MonopolyDealCardHeight * 4;

            MonopolyDealGameState mdgs = (MonopolyDealGameState) gameState;
            MonopolyDealParameters mdp = (MonopolyDealParameters) gameState.getGameParameters();

            playerViews = new MDealPlayerView[nPlayers];
            JPanel playerViewWrapper = new JPanel();
            playerViewWrapper.setOpaque(false);
            playerViewWrapper.setLayout(new BoxLayout(playerViewWrapper, BoxLayout.Y_AXIS));
            JScrollPane playerViewScroll = new JScrollPane(playerViewWrapper);
            playerViewScroll.setPreferredSize(new Dimension(width + 20, MonopolyDealCardHeight * 4));
            playerViewScroll.setOpaque(false);
            playerViewScroll.getViewport().setOpaque(false);
            playerViewBorders = new Border[nPlayers];
            for (int i = 0; i < nPlayers; i++){
                // Adding player properties
                playerViews[i] = new MDealPlayerView(i, humanID, mdp, mdgs);
                playerViews[i].setOpaque(false);

                // Get agent name
                String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
                String agentName = split[split.length - 1];

                // Create border, layouts and keep track of this view
                TitledBorder title = BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + (i+1) + " [" + agentName + "]",
                        TitledBorder.CENTER, TitledBorder.ABOVE_TOP);
                playerViewBorders[i] = title;

                // Add border and view to panel
                playerViews[i].setBorder(title);
                playerViewWrapper.add(playerViews[i]);
            }

            // Discard and draw piles go to the top
            JPanel topArea = new JPanel();
            topArea.setOpaque(false);
            topArea.setLayout(new BoxLayout(topArea, BoxLayout.X_AXIS));
            discardPile = new MonopolyDealDeckView(-1, mdgs.getDiscardPile(), true, mdp.getDataPath(), new Rectangle(0, 0, MonopolyDealCardWidth, MonopolyDealCardHeight), MonopolyDealCardWidth, MonopolyDealCardHeight);
            drawPile = new MonopolyDealDeckView(-1, mdgs.getDrawPile(), gameState.getCoreGameParameters().alwaysDisplayFullObservable, mdp.getDataPath(), new Rectangle(0, 0, MonopolyDealCardWidth*5, MonopolyDealCardHeight), MonopolyDealCardWidth, MonopolyDealCardHeight);
            discardPile.setOpaque(false);
            drawPile.setOpaque(false);
            topArea.add(Box.createRigidArea(new Dimension(20, 0)));//spacer
            topArea.add(drawPile);
            topArea.add(Box.createRigidArea(new Dimension(20, 0)));//spacer
            topArea.add(discardPile);


            // Top area will show state information
            JPanel infoPanel = createGameStateInfoPanel("MonopolyDeal", gameState, width, defaultInfoPanelHeight);

            // Bottom area will show actions available
            JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false, false, null, null, null);

            // Add all views to frame
            parent.setLayout(new BoxLayout(parent, BoxLayout.Y_AXIS));
            parent.add(infoPanel);
            parent.add(Box.createRigidArea(new Dimension(0, 10)));//spacer
            parent.add(topArea);
            parent.add(Box.createRigidArea(new Dimension(0, 10)));//spacer
            parent.add(playerViewScroll);
            parent.add(Box.createRigidArea(new Dimension(0, 5)));//spacer
            parent.add(actionPanel);
            parent.revalidate();
            parent.setVisible(true);
            parent.repaint();
        }
    }

    /**
     * Defines how many action button objects will be created and cached for usage if needed. Less is better, but
     * should not be smaller than the number of actions available to players in any game state.
     *
     * @return maximum size of the action space (maximum actions available to a player for any decision point in the game)
     */
    @Override
    public int getMaxActionSpace() {
        return 100;
    }

    /**
     * Updates all GUI elements given current game state and player that is currently acting.
     *
     * @param player    - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null){
            if (gameState.getCurrentPlayer() != activePlayer) {
                playerViews[activePlayer].hand.setCardHighlight(-1);
                activePlayer = gameState.getCurrentPlayer();
            }
            // Clearing all properties
            for(int i=0;i<gameState.getNPlayers();i++){
                for(int j=0;j<10;j++){
                    playerViews[i].properties[j].updateComponent(new Deck<MonopolyDealCard>("PropertySet", CoreConstants.VisibilityMode.VISIBLE_TO_ALL));
                }
            }

            // Update decks and visibility
            MonopolyDealGameState mdgs = (MonopolyDealGameState) gameState;

            for (int i = 0; i < gameState.getNPlayers(); i++){
                playerViews[i].hand.updateComponent(mdgs.getPlayerHand(i));
                playerViews[i].hand.setFront(false);
                mdgs.getPlayerBank(i).setComponentName("Bank: " + mdgs.getBankValue(i) + "M");
                playerViews[i].bank.updateComponent(mdgs.getPlayerBank(i));
                for(int j = 0; j < SetType.values().length; j++){
                    if (mdgs.getPropertySets(i)[j].getSize() > 0) {
                        playerViews[i].properties[j].setVisible(true);
                        playerViews[i].properties[j].updateComponent(mdgs.getPropertySets(i)[j]);
                    } else {
                        playerViews[i].properties[j].setVisible(false);
                        playerViews[i].properties[j].updateComponent(null);
                    }
                }
                if (i == gameState.getCurrentPlayer() && gameState.getCoreGameParameters().alwaysDisplayCurrentPlayer
                        || humanPlayerIds.contains(i)
                        || gameState.getCoreGameParameters().alwaysDisplayFullObservable) {
                    playerViews[i].hand.setFront(true);
                    playerViews[i].hand.setFocusable(true);
                }

                // Highlight active player
                if (i == gameState.getCurrentPlayer()) {
                    Border compound = BorderFactory.createCompoundBorder(
                            highlightActive, playerViewBorders[i]);
                    playerViews[i].setBorder(compound);
                } else {
                    playerViews[i].setBorder(playerViewBorders[i]);
                }
            }

            discardPile.updateComponent(mdgs.getDiscardPile());
            drawPile.setFocusable(true);
            drawPile.updateComponent(mdgs.getDrawPile());
            if (gameState.getCoreGameParameters().alwaysDisplayFullObservable) {
                drawPile.setFront(true);
            }
        }
    }
}