package games.terraformingmars.gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.Token;
import games.terraformingmars.TMGameState;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import utilities.ImageIO;
import utilities.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TMGUI extends AbstractGUI {

    TMBoardView view;
    TMPlayerView playerView;
    TMDeckDisplay playerHand;
    JPanel infoPanel;
    private boolean adjustedSize;

    public TMGUI(Game game, ActionController ac) {
        super(ac, 500);
        if (game == null) return;

        BufferedImage bg = (BufferedImage) ImageIO.GetInstance().getImage("data/terraformingmars/images/stars.jpg");
        TexturePaint space = new TexturePaint(bg, new Rectangle2D.Float(0,0, bg.getWidth(), bg.getHeight()));
        TiledImage backgroundImage = new TiledImage(space);
        // Make backgroundImage the content pane.
        setContentPane(backgroundImage);

        TMGameState gameState = (TMGameState) game.getGameState();
        view = new TMBoardView(gameState);

        try {
            GraphicsEnvironment ge =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("data/terraformingmars/images/fonts/Prototype.ttf")));
        } catch (IOException |FontFormatException e) {
            //Handle exception
        }

        infoPanel = createGameStateInfoPanel("Terraforming Mars", gameState, defaultDisplayWidth, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new Collection[]{view.getHighlight()}, defaultDisplayWidth, defaultActionPanelHeight);

        JPanel playerViewWrapper = new JPanel();
        playerViewWrapper.setLayout(new BoxLayout(playerViewWrapper, BoxLayout.Y_AXIS));
        playerView = new TMPlayerView(gameState, 0);
        playerHand = new TMDeckDisplay(gameState, gameState.getPlayerHands()[0]);
        JScrollPane pane = new JScrollPane(playerHand);
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        pane.setPreferredSize(new Dimension(playerView.getPreferredSize().width, playerHand.getPreferredSize().height + 20));
        playerViewWrapper.add(playerView);
        playerViewWrapper.add(pane);

        JPanel main = new JPanel();
        main.add(view);
        main.add(playerViewWrapper);

        pane.setOpaque(false);
        pane.getViewport().setOpaque(false);
        playerHand.setOpaque(false);
        playerView.setOpaque(false);
        playerViewWrapper.setOpaque(false);
        view.setOpaque(false);
        infoPanel.setOpaque(false);
        actionPanel.setOpaque(false);
        main.setOpaque(false);

        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        getContentPane().add(infoPanel);
        getContentPane().add(main);
        getContentPane().add(actionPanel);

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setFrameProperties();
    }

    /**
     * Only shows actions for highlighted cell.
     * @param player - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState.getGameStatus() == Utils.GameResult.GAME_ONGOING) {
            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);
            ArrayList<Rectangle> highlight = view.getHighlight();

            int start = actions.size();
            if (highlight.size() > 0) {
                Rectangle r = highlight.get(0);
                for (AbstractAction abstractAction : actions) {
                    SetGridValueAction<Token> action = (SetGridValueAction<Token>) abstractAction;
                    if (action.getX() == r.x/defaultItemSize && action.getY() == r.y/defaultItemSize) {
                        actionButtons[0].setVisible(true);
                        actionButtons[0].setButtonAction(action, "Play ");
                        break;
                    }
                }
            } else {
                actionButtons[0].setVisible(false);
                actionButtons[0].setButtonAction(null, "");
            }
        }
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            view.update((TMGameState)gameState);
            playerView.update((TMGameState) gameState);  // TODO: interface buttons for changing players, or automatic for current player
            playerHand.update((TMGameState) gameState);  // TODO: interface buttons for changing players, or automatic for current player

            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
            if (!adjustedSize && view.getPreferredSize().height != 20) {
//                pack();
//                revalidate();
                adjustedSize = true;
            }
        }
        repaint();
    }
}
