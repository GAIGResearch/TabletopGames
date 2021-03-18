package games.terraformingmars.gui;

import core.*;
import core.components.Deck;
import games.terraformingmars.TMGameState;
import games.terraformingmars.components.TMCard;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class TMGUI extends AbstractGUI {

    TMBoardView view;
    TMPlayerView playerView;
    TMDeckDisplay playerHand, playerCardChoice, playerCorporation;
    JScrollPane paneHand, paneCardChoice;
    JPanel infoPanel;
    JLabel generationCount;

    static int fontSize = 16;
    static Font defaultFont = new Font("Prototype", Font.BOLD, fontSize);
    static int focusPlayer = 0;

    int currentPlayerIdx = 0;
    boolean focusCurrentPlayer;
    JButton focusPlayerButton;

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

        createActionHistoryPanel(defaultDisplayWidth, defaultInfoPanelHeight);
        historyInfo.setFont(defaultFont);
        historyInfo.setForeground(Color.white);
        JPanel historyWrapper = new JPanel();
        JLabel historyText = new JLabel("Action history:");
        historyText.setFont(defaultFont);
        historyText.setForeground(Color.white);
        historyWrapper.add(historyText);
        historyWrapper.add(historyContainer);
        historyContainer.setBackground(Color.black);

        JLabel actionLabel = new JLabel("Actions: ");
        actionLabel.setFont(defaultFont);
        actionLabel.setForeground(Color.white);
        actionLabel.setOpaque(false);
        JComponent actionPanel = createActionPanel(new Collection[]{view.getHighlight()}, defaultDisplayWidth*2, defaultActionPanelHeight/2, false,false);
        JPanel actionWrapper = new JPanel();
        actionWrapper.add(actionLabel);
        actionWrapper.add(actionPanel);
        actionWrapper.setOpaque(false);

        JPanel playerViewWrapper = new JPanel();
        playerViewWrapper.setLayout(new BoxLayout(playerViewWrapper, BoxLayout.Y_AXIS));
        playerView = new TMPlayerView(gameState, focusPlayer);

        playerHand = new TMDeckDisplay(gameState, gameState.getPlayerHands()[focusPlayer]);
        paneHand = new JScrollPane(playerHand);
        paneHand.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        paneHand.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        paneHand.setPreferredSize(new Dimension(playerView.getPreferredSize().width, TMDeckDisplay.cardHeight + 20));

        playerCorporation = new TMDeckDisplay(gameState, null);
        playerCorporation.setPreferredSize(new Dimension(TMDeckDisplay.cardWidth + TMDeckDisplay.offsetX * 2, TMDeckDisplay.cardHeight + TMDeckDisplay.offsetX * 2));

        playerCardChoice = new TMDeckDisplay(gameState, gameState.getPlayerCardChoice()[focusPlayer]);
        paneCardChoice = new JScrollPane(playerCardChoice);
        paneCardChoice.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        paneCardChoice.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        paneCardChoice.setPreferredSize(new Dimension(playerView.getPreferredSize().width, TMDeckDisplay.cardHeight + 20));

        JPanel playerMainWrap = new JPanel();
        playerMainWrap.add(playerView);
        playerMainWrap.add(playerCorporation);
        playerViewWrapper.add(playerMainWrap);
        playerViewWrapper.add(paneHand);
        playerViewWrapper.add(paneCardChoice);

        JPanel main = new JPanel();
        main.add(view);
        main.add(playerViewWrapper);

        generationCount = new JLabel("Generation: 1");
        gamePhase.setFont(defaultFont);
        generationCount.setFont(defaultFont);
        gamePhase.setForeground(Color.white);
        generationCount.setForeground(Color.white);
        JPanel infoWrapper = new JPanel();
        infoWrapper.setLayout(new BoxLayout(infoWrapper, BoxLayout.Y_AXIS));
        infoWrapper.add(generationCount);
        infoWrapper.add(gamePhase);

        JPanel playerFlipButtons = new JPanel();
        JLabel jLabel1 = new JLabel("Change player display:");
        jLabel1.setFont(defaultFont);
        jLabel1.setForeground(Color.white);
        playerFlipButtons.add(jLabel1);
        for (int i = 0; i < gameState.getNPlayers(); i++) {
            String text = "p" + i;
            JButton jb = new JButton(text);
            jb.setFont(defaultFont);
            jb.setForeground(Color.white);
            jb.setBackground(Color.darkGray);
            jb.addActionListener(e -> {
                focusPlayer = Integer.parseInt(jb.getText().replace("p",""));
                focusCurrentPlayer = false;
            });
            playerFlipButtons.add(jb);
        }
        focusPlayerButton = new JButton("Current player: " + currentPlayerIdx);
        focusPlayerButton.setFont(defaultFont);
        focusPlayerButton.setForeground(Color.white);
        focusPlayerButton.setBackground(Color.darkGray);
        focusPlayerButton.addActionListener(e -> focusCurrentPlayer = true);
        playerFlipButtons.add(focusPlayerButton);

        JButton jb2 = new JButton("Pause/Resume");
        jb2.setFont(defaultFont);
        jb2.setForeground(Color.white);
        jb2.setBackground(Color.gray);
        jb2.addActionListener(e -> game.flipPaused());
        playerFlipButtons.add(jb2);

        JPanel top = new JPanel();
        top.add(historyWrapper);
        top.add(playerFlipButtons);
        top.add(infoWrapper);

        top.setOpaque(false);
        playerMainWrap.setOpaque(false);
        playerCorporation.setOpaque(false);
        playerFlipButtons.setOpaque(false);
        paneHand.setOpaque(false);
        paneHand.getViewport().setOpaque(false);
        playerCardChoice.setOpaque(false);
        paneCardChoice.setOpaque(false);
        paneCardChoice.getViewport().setOpaque(false);
        playerHand.setOpaque(false);
        playerView.setOpaque(false);
        playerViewWrapper.setOpaque(false);
        view.setOpaque(false);
        main.setOpaque(false);
        historyInfo.setOpaque(false);
        historyContainer.setOpaque(false);
        historyContainer.getViewport().setOpaque(false);
        historyWrapper.setOpaque(false);
        infoWrapper.setOpaque(false);
        gamePhase.setOpaque(false);
        generationCount.setOpaque(false);

        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        getContentPane().add(top);
        getContentPane().add(main);
        getContentPane().add(actionWrapper);

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setFrameProperties();
    }

//    /**
//     * Only shows actions for highlighted cell.
//     * @param player - current player acting.
//     * @param gameState - current game state to be used in updating visuals.
//     */
//    @Override
//    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
//        if (gameState.getGameStatus() == Utils.GameResult.GAME_ONGOING) {
//            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);
//            ArrayList<Rectangle> highlight = view.getHighlight();
//
//            int start = actions.size();
//            if (highlight.size() > 0) {
//                Rectangle r = highlight.get(0);
//                for (AbstractAction abstractAction : actions) {
//                    SetGridValueAction<Token> action = (SetGridValueAction<Token>) abstractAction;
//                    if (action.getX() == r.x/defaultItemSize && action.getY() == r.y/defaultItemSize) {
//                        actionButtons[0].setVisible(true);
//                        actionButtons[0].setButtonAction(action, "Play ");
//                        break;
//                    }
//                }
//            } else {
//                actionButtons[0].setVisible(false);
//                actionButtons[0].setButtonAction(null, "");
//            }
//        }
//    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            TMGameState gs = ((TMGameState) gameState);
            currentPlayerIdx = gs.getCurrentPlayer();
            focusPlayerButton.setText("Current player: " + currentPlayerIdx);
            if (focusCurrentPlayer) {
                focusPlayer = currentPlayerIdx;
            }
            generationCount.setText("Generation: " + gs.getGeneration());

            view.update(gs);
            playerView.update(gs);
            playerHand.update(gs.getPlayerHands()[focusPlayer]);
            playerCardChoice.update(gs.getPlayerCardChoice()[focusPlayer]);
            Deck<TMCard> temp = new Deck<>("Temp", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
            TMCard corp = gs.getPlayerCorporations()[focusPlayer];
            if (corp != null) {
                temp.add(corp);
            }
            playerCorporation.update(temp);

            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
        }
        repaint();
    }
}
