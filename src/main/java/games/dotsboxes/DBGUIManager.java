package games.dotsboxes;

import core.*;
import core.actions.AbstractAction;
import gui.AbstractGUIManager;
import gui.GamePanel;
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
        JPanel rules = new JPanel();
        rules.setOpaque(false);
        JLabel ruleText = new JLabel(getRuleText());
        ruleText.setOpaque(false);
        ruleText.setPreferredSize(new Dimension((int)(2/3.*displayWidth),displayHeight));
        rules.add(ruleText);

        view = new DBGridBoardView(((DBGameState)game.getGameState()));
        JPanel infoPanel = createGameStateInfoPanel("Dots and Boxes", game.getGameState(), displayWidth, defaultInfoPanelHeight);
        JLabel label = new JLabel("Human player: click on 2 adjacent dots to place your edge.");
        label.setOpaque(false);
        main.add(infoPanel, BorderLayout.NORTH);
        main.add(view, BorderLayout.CENTER);
        main.add(label, BorderLayout.SOUTH);

        pane.add("Main", main);
        pane.add("Rules", rules);
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

    private String getRuleText() {
        String rules = "<html><center><h1>Dots & Boxes</h1></center><br/><hr><br/>";
        rules += "<p>Play happens on a grid with dots in the corners of all cells. Players alternate drawing lines between the dots.</p><br/>";
        rules += "<p>Whenever a grid cell is completely enclosed, the player who drew the last edge gets 1 point. If a line drawn closes X cells at the same time, then the player gets X points. If any cell is closed, the same player gets to draw another line.</p><br/>";
        rules += "<p>WIN: The player with most boxes completed (points) wins.</p>";
        rules += "<hr><p><b>INTERFACE: </b> Place edges by clicking on 2 adjacent dots where an edge doesn't already exist. Invalid actions will be ignored.</p>";
        rules += "</html>";
        return rules;
    }
}
