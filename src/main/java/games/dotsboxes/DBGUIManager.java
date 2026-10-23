package games.dotsboxes;

import core.*;
import core.actions.AbstractAction;
import games.GameType;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.views.RulesView;
import players.human.ActionController;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

public class DBGUIManager extends AbstractGUIManager {
    DBGridBoardView view;
    int gapRight = 30;

    public DBGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanId) {
        this(parent, game, ac, humanId, defaultDisplayWidth, defaultDisplayHeight);
    }

    @Override
    public int getMaxActionSpace() {
        return 100;
    }

    public DBGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanId,
                        int displayWidth, int displayHeight) {
        super(parent, game, ac, humanId);

        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);

        this.width = gapRight + displayWidth;
        this.height = displayHeight;
        parent.setBackground(ImageIO.GetInstance().getImage("data/dotsboxes/bg.png"));

        JTabbedPane pane = new JTabbedPane();
        JPanel main = new JPanel();
        main.setOpaque(false);
        main.setLayout(new BorderLayout());

        view = new DBGridBoardView(((DBGameState)game.getGameState()));
        JPanel infoPanel = createGameStateInfoPanel("Dots and Boxes", game.getGameState(), displayWidth, defaultInfoPanelHeight);
        JLabel label = new JLabel("Human player: click on 2 adjacent dots to place your edge.");
        label.setOpaque(false);
        main.add(infoPanel, BorderLayout.NORTH);
        main.add(view, BorderLayout.CENTER);
        main.add(label, BorderLayout.SOUTH);

        pane.add("Main", main);
        RulesView.addTabs(pane, GameType.DotsAndBoxes, game.getGameState().getGameParameters(), displayHeight);
        parent.setLayout(new BorderLayout());

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        wrapper.setOpaque(false);
        wrapper.add(Box.createRigidArea(new Dimension(gapRight,height)));
        wrapper.add(pane);

        parent.add(wrapper, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(Math.max(width,view.getPreferredSize().width), view.getPreferredSize().height + defaultInfoPanelHeight));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    @Override
    protected JPanel createGameStateInfoPanel(String gameTitle, AbstractGameState gameState, int width, int height) {
        JPanel gameInfo = new JPanel();
        gameInfo.setOpaque(false);
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel("<html><h1>" + gameTitle + "</h1></html>"));

        updateGameStateInfo(gameState);

        gameInfo.add(gameStatus);
        gameInfo.add(playerStatus);
        gameInfo.add(playerScores);
        gameInfo.add(gamePhase);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);

        gameInfo.setPreferredSize(new Dimension(width/2 - 10, height));

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new FlowLayout());
        wrapper.add(gameInfo);

        historyInfo.setPreferredSize(new Dimension(width/2 - 10, height));
        historyContainer = new JScrollPane(historyInfo);
        historyContainer.setPreferredSize(new Dimension(width/2 - 25, height));
        wrapper.add(historyContainer);
        historyInfo.setOpaque(false);
        historyContainer.setOpaque(false);
//        historyContainer.getViewport().setOpaque(false);
        historyInfo.setEditable(false);
        return wrapper;
    }

    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        DBEdge db = view.getHighlight();
        if (gameState.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING && db != null) {
            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);
            boolean found = false;
            for (AbstractAction a: actions) {
                AddGridCellEdge aa = (AddGridCellEdge) a;
                if (aa.edge.equals(db)) {
                    ac.addAction(a);
                    found = true;
                    break;
                }
            }
            if (!found) System.out.println("Invalid action, click 2 adjacent dots to select an edge that doesn't already exist.");
            view.highlight = null;
        }
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            view.updateGameState(((DBGameState)gameState));
        }
    }
}
