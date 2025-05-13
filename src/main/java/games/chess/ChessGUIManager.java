package games.chess;

import core.*;
import core.actions.AbstractAction;
import gui.AbstractGUIManager;
import gui.GamePanel;
import players.human.ActionController;

import javax.swing.*;

import java.awt.*;
import java.util.List;
import java.util.Set;
import games.chess.components.*;
import games.chess.actions.*;
import games.chess.actions.Castle.CastleType;

public class ChessGUIManager extends AbstractGUIManager {
    ChessBoardView view;
    int gapRight = 30;

    public ChessGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanId) {
        this(parent, game, ac, humanId, defaultDisplayWidth, defaultDisplayHeight);
    }

    @Override
    public int getMaxActionSpace() {
        return 100;
    }

    public ChessGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanId,
                        int displayWidth, int displayHeight) {
        super(parent, game, ac, humanId);

        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);

        this.width = gapRight + displayWidth;
        this.height = displayHeight;

        JTabbedPane pane = new JTabbedPane();
        JPanel main = new JPanel();
        main.setOpaque(false);
        main.setLayout(new BorderLayout());

        view = new ChessBoardView(((ChessGameState)game.getGameState()), game);
        JPanel infoPanel = createGameStateInfoPanel("Chess", game.getGameState(), displayWidth, defaultInfoPanelHeight);
        JLabel label = new JLabel("Human player: drag and drop pieces to move them");
        label.setOpaque(false);
        main.add(infoPanel, BorderLayout.NORTH);
        main.add(view, BorderLayout.CENTER);
        main.add(label, BorderLayout.SOUTH);

        pane.add("Main", main);
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
        ChessPiece selectedPiece = view.getSelectedPiece();
        int[] selectedPos = view.getSelectedPos();
        int[] targetPos = view.getTargetPos();
        int direction = player.getPlayerID() == 0 ? 1 : -1;
        if (gameState.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING && selectedPiece != null && targetPos != null && !view.promoting) {
            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);
            boolean found = false;
            for (AbstractAction a: actions) {

                if (a.equals(new MovePiece(selectedPos[0], selectedPos[1], targetPos[0], targetPos[1]))) {
                    ac.addAction(a);
                    found = true;
                    break;
                }
                if (selectedPiece.getChessPieceType() == ChessPiece.ChessPieceType.KING && targetPos[0] == 6 && a.equals(new Castle(CastleType.KING_SIDE))) {
                    ac.addAction(a);
                    found = true;
                }
                if (selectedPiece.getChessPieceType() == ChessPiece.ChessPieceType.KING && targetPos[0] == 2 && a.equals(new Castle(CastleType.QUEEN_SIDE))) {
                    ac.addAction(a);
                    found = true;
                }
                // En passant
                if (selectedPiece.getChessPieceType() == ChessPiece.ChessPieceType.PAWN && targetPos[0] == selectedPos[0] + 1 && targetPos[1] == selectedPos[1] + direction && a.equals(new EnPassant(selectedPos[0], selectedPos[1], targetPos[0]))) {
                    ac.addAction(a);
                    found = true;
                }
                if (selectedPiece.getChessPieceType() == ChessPiece.ChessPieceType.PAWN && targetPos[0] == selectedPos[0] - 1 && targetPos[1] == selectedPos[1] + direction && a.equals(new EnPassant(selectedPos[0], selectedPos[1], targetPos[0]))) {
                    ac.addAction(a);
                    found = true;
                }
                // Promotion
                if (selectedPiece.getChessPieceType() == ChessPiece.ChessPieceType.PAWN && (targetPos[1] == 7 || targetPos[1] == 0 && view.selectedPromotion != null)) {
                    if (a.equals(new Promotion(selectedPos[0], selectedPos[1], targetPos[0], targetPos[1], view.selectedPromotion))) {
                        ac.addAction(a);
                        found = true;
                        view.selectedPromotion = null;
                    }
                }


            }
            if (!found) System.out.println("Invalid action, " + "selected: " + selectedPos[0] + ", " + selectedPos[1] + " target: " + targetPos[0] + ", " + targetPos[1]);
            view.selectedPos = null;
            view.targetPos = null;
            view.selectedPiece = null;
            view.mouseOverPos = null;
            view.repaint();
        }
        
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            view.updateGameState(((ChessGameState)gameState));
        }
    }
}