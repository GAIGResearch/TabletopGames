package games.dotsboxes;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import gui.ScreenHighlight;
import core.actions.AbstractAction;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import utilities.ImageIO;
import utilities.Utils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DBGUI extends AbstractGUI {
    DBGridBoardView view;
    int gapRight = 30;

    public DBGUI(AbstractGameState gameState, ActionController ac) {
        this(gameState, ac, defaultDisplayWidth, defaultDisplayHeight);
    }

    public DBGUI(AbstractGameState gameState, ActionController ac,
                 int displayWidth, int displayHeight) {
        super(ac, 100);

        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);

        this.width = gapRight + displayWidth;
        this.height = displayHeight;
        setContentPane(new ScaledImage(ImageIO.GetInstance().getImage("data/dotsboxes/bg.png"), width, height, this));

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

        view = new DBGridBoardView(((DBGameState)gameState));
        JPanel infoPanel = createGameStateInfoPanel("Dots and Boxes", gameState, displayWidth, defaultInfoPanelHeight);
        JLabel label = new JLabel("Human player: click on 2 adjacent dots to place your edge.");
        label.setOpaque(false);
        main.add(infoPanel, BorderLayout.NORTH);
        main.add(view, BorderLayout.CENTER);
        main.add(label, BorderLayout.SOUTH);

        pane.add("Main", main);
        pane.add("Rules", rules);
        setLayout(new BorderLayout());

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        wrapper.setOpaque(false);
        wrapper.add(Box.createRigidArea(new Dimension(gapRight,height)));
        wrapper.add(pane);

        getContentPane().add(wrapper, BorderLayout.CENTER);

        setFrameProperties();
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
        gameInfo.add(turnOwner);
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
        historyContainer.getViewport().setOpaque(false);
        historyInfo.setEditable(false);
        return wrapper;
    }

    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        DBEdge db = view.getHighlight();
        if (gameState.getGameStatus() == Utils.GameResult.GAME_ONGOING && db != null) {
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
    protected void _update(AbstractPlayer player, AbstractGameState gameState, boolean actionTaken) {
        if (gameState != null) {
            view.updateGameState(((DBGameState)gameState));
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(Math.max(width,view.getPreferredSize().width), view.getPreferredSize().height + defaultInfoPanelHeight);
    }

    public static class ScaledImage extends JPanel {
        Image img;
        int w, h;
        JFrame frame;

        public ScaledImage(Image img, int w, int h, JFrame frame) {
            this.img = img;
            this.w = w;
            this.h = h;
            this.frame = frame;
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setComposite(AlphaComposite.SrcOver.derive(0.3f));

            Rectangle r = frame.getBounds();
            h = r.height;
            w = r.width;

            int picW = img.getWidth(null);
            int picH = img.getHeight(null);
            double scale = w*1.0/picW;
            double s2 = h*1.0/picH;
            if (s2 > scale) scale = s2;
            g2d.drawImage(img, 0, 0, (int)(picW * scale), (int)(picH * scale), null);
            g2d.dispose();
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
