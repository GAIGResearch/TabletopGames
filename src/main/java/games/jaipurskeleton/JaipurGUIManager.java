package games.jaipurskeleton;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.jaipurskeleton.components.JaipurCard;
import games.jaipurskeleton.gui.JaipurBonusTokenView;
import games.jaipurskeleton.gui.JaipurMarketView;
import games.jaipurskeleton.gui.JaipurPlayerArea;
import games.jaipurskeleton.gui.JaipurGoodTokenView;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Set;

import static games.jaipurskeleton.components.JaipurCard.GoodType.*;

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
public class JaipurGUIManager extends AbstractGUIManager {

    public static HashMap<JaipurCard.GoodType, Color> goodColorMapping = new HashMap<JaipurCard.GoodType, Color>() {{
        put(Diamonds, (new Color(217, 31, 31)));
        put(Gold, (new Color(232, 173, 57)));
        put(Silver, (new Color(128, 149, 159)));
        put(Cloth, (new Color(185, 93, 231)));
        put(Spice, (new Color(106, 203, 69)));
        put(Leather, (new Color(133, 70, 22)));
        put(Camel, new Color(132, 171, 224));
    }};
    public static HashMap<JaipurCard.GoodType, Color> soldGoodColorMapping = new HashMap<JaipurCard.GoodType, Color>() {{
        put(Diamonds, (new Color(246, 198, 198)));
        put(Gold, (new Color(245, 232, 206)));
        put(Silver, (new Color(216, 235, 243)));
        put(Cloth, (new Color(234, 216, 245)));
        put(Spice, (new Color(224, 246, 216)));
        put(Leather, (new Color(248, 228, 213)));
        put(Camel, new Color(223, 233, 246));
    }};
    public static int viewWidth, viewHeight;
    public static int border = 30;

    public JaipurGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;

        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);

        this.height = 500 + defaultActionPanelHeight + defaultInfoPanelHeight;

        JTabbedPane pane = new JTabbedPane();

        JPanel main = new JPanel();
        main.setLayout(new BorderLayout());
        JPanel infoPanel = createGameStateInfoPanel("Jaipur", game.getGameState(), defaultDisplayWidth, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false, true, null, null, null);

        GamePanel playPanel = new GamePanel();
        playPanel.setBackground(ImageIO.GetInstance().getImage("data/games/jaipurbg.png"));
        playPanel.setKeepBackgroundRatio(false);
        playPanel.setAlpha(1f);

        playPanel.setLayout(new BoxLayout(playPanel, BoxLayout.Y_AXIS));
        playPanel.add(Box.createRigidArea(new Dimension(0, border)));

        JPanel wrapperTop = new JPanel();
        wrapperTop.setOpaque(false);
        wrapperTop.setLayout(new BoxLayout(wrapperTop, BoxLayout.X_AXIS));
        wrapperTop.add(Box.createRigidArea(new Dimension(border, 0)));

        // Good token view
        JaipurGoodTokenView jtv = new JaipurGoodTokenView((JaipurGameState) game.getGameState());
        viewWidth = jtv.getPreferredSize().width;
        viewHeight = jtv.getPreferredSize().height;
        wrapperTop.add(jtv);

        // Market view
        wrapperTop.add(new JaipurMarketView((JaipurGameState) game.getGameState()));

        // Player views
        for (int i = 0; i < game.getGameState().getNPlayers(); i++) {
            wrapperTop.add(new JaipurPlayerArea((JaipurGameState) game.getGameState(), game.getPlayers().get(i), i));
        }
        this.width = border*2 + viewWidth*(2 + game.getGameState().getNPlayers());
        playPanel.add(wrapperTop);

        // Bonus token view
        JPanel wrapperBottom = new JPanel();
        wrapperBottom.setOpaque(false);
        wrapperBottom.setLayout(new BoxLayout(wrapperBottom, BoxLayout.X_AXIS));
        wrapperBottom.add(Box.createRigidArea(new Dimension(border, 0)));
        wrapperBottom.add(new JaipurBonusTokenView((JaipurGameState) game.getGameState()));
        playPanel.add(wrapperBottom);

        main.add(infoPanel, BorderLayout.NORTH);
        main.add(playPanel, BorderLayout.CENTER);
        main.add(actionPanel, BorderLayout.SOUTH);

        GamePanel rules = new GamePanel();
        rules.setBackground(ImageIO.GetInstance().getImage("data/games/jaipurbg.png"));
        rules.setKeepBackgroundRatio(false);
        rules.setAlpha(1f);
        rules.setLayout(new FlowLayout());
        JLabel ruleText = new JLabel(getRuleText((JaipurParameters) game.getGameState().getGameParameters()));
        ruleText.setOpaque(false);
        ruleText.setPreferredSize(new Dimension((int)(2/3.*defaultDisplayWidth),height));
        rules.add(ruleText);

        pane.add("Main", main);
        pane.add("Rules", rules);
        parent.setLayout(new BorderLayout());

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        wrapper.setOpaque(false);
        wrapper.add(Box.createRigidArea(new Dimension(border,height)));
        wrapper.add(pane);

        parent.add(wrapper, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(width, height));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    /**
     * Defines how many action button objects will be created and cached for usage if needed. Less is better, but
     * should not be smaller than the number of actions available to players in any game state.
     *
     * @return maximum size of the action space (maximum actions available to a player for any decision point in the game)
     */
    @Override
    public int getMaxActionSpace() {
        return 10;   // TODO: increase this number if doing optional exercise week 1
    }

    /**
     * Updates all GUI elements given current game state and player that is currently acting.
     *
     * @param player    - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
    }

    private String getRuleText(JaipurParameters params) {
        String rules = "<html><center><h1>Jaipur</h1></center><br/><hr><br/>";
        rules += "<p>Players sell good cards from their hands for good tokens, or take cards from the market, alternating turns.</p><br/>";
        rules += "<p>Selling several cards at the same time can earn the players bonus tokens.</p><br/>";
        rules += "<p>Players have a hand limit of " + 7 + " cards. Camel cards go in a separate pile and don't count towards this limit.</p><br/>";
        rules += "<p>If taking Camels from the market, all camels must be taken. If exchanging cards in the market, camel cards from the player's herd can be used.</p><br/>";
        rules += "<p>A round ends when " + params.nGoodTokensEmptyRoundEnd + " stacks of good tokens are empty, or no more cards are left in the draw pile when needing to replenish the market.</p><br/>";
        rules += "<p>At the end of the round, the player with the most camel cards wins the camel token and " + params.nPointsMostCamels + " bonus points.</p><br/>";
        rules += "<p>ROUND WIN: The player with the most points (from good tokens, bonus tokens and camel token) wins the round.</p><br/>";
        rules += "<p>WIN: The player who wins majority of rounds played (" + params.nRoundsWinForGameWin + ") wins.</p>";
        rules += "<hr><p><b>INTERFACE: </b> Click action buttons.</p>";
        rules += "</html>";
        return rules;
    }
}
