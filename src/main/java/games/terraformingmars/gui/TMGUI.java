package games.terraformingmars.gui;

import core.*;
import core.actions.AbstractAction;
import core.components.Deck;
import games.terraformingmars.TMForwardModel;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.PayForAction;
import games.terraformingmars.actions.PlaceTile;
import games.terraformingmars.actions.TMAction;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.components.TMMapTile;
import games.terraformingmars.rules.requirements.Requirement;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import utilities.ImageIO;
import utilities.Utils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class TMGUI extends AbstractGUI {

    TMBoardView view;
    TMPlayerView playerView;
    TMDeckDisplay playerHand, playerCardChoice;
    TMCardView playerCorporation;
    TMDeckDisplay2 playerCardsPlayed;
    JScrollPane paneHand, paneCardChoice, paneCardsPlayed;
    JPanel infoPanel;
    JLabel generationCount;

    static int fontSize = 16;
    static Font defaultFont = new Font("Prototype", Font.BOLD, fontSize);
    static int focusPlayer = 0;

    int currentPlayerIdx = 0;
    boolean focusCurrentPlayer;
    JButton focusPlayerButton;

    boolean firstUpdate = true;
    boolean updateButtons = false;
    HashMap<TMTypes.ActionType, JMenu> actionMenus;

    public TMGUI(Game game, ActionController ac) {
        super(ac, 500);
        if (game == null) return;

        BufferedImage bg = (BufferedImage) ImageIO.GetInstance().getImage("data/terraformingmars/images/stars.jpg");
        TexturePaint space = new TexturePaint(bg, new Rectangle2D.Float(0,0, bg.getWidth(), bg.getHeight()));
        TiledImage backgroundImage = new TiledImage(space);
        // Make backgroundImage the content pane.
        setContentPane(backgroundImage);
        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);

        TMGameState gameState = (TMGameState) game.getGameState();
        view = new TMBoardView(this, gameState);

        actionMenus = new HashMap<>();
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(Color.black);
        int mnemonicStart = KeyEvent.VK_A;
        for (TMTypes.ActionType t : TMTypes.ActionType.values()) {
            JMenu menu = new JMenu(t.name());
            menu.setMnemonic(mnemonicStart++);
            menu.getAccessibleContext().setAccessibleDescription("Choose an action of type " + t.name());
            menu.setForeground(Color.white);
            menu.setFont(defaultFont);
            actionMenus.put(t, menu);
            menuBar.add(menu);
        }
        this.setJMenuBar(menuBar);

        try {
            GraphicsEnvironment ge =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("data/terraformingmars/images/fonts/Prototype.ttf")));
        } catch (IOException |FontFormatException e) {
            //Handle exception
        }

        createActionHistoryPanel(defaultDisplayWidth, defaultInfoPanelHeight/2);
        historyInfo.setFont(defaultFont);
        historyInfo.setForeground(Color.white);
        JPanel historyWrapper = new JPanel();
        JLabel historyText = new JLabel("Action history:");
        historyText.setFont(defaultFont);
        historyText.setForeground(Color.white);
        historyWrapper.add(historyText);
        historyWrapper.add(historyContainer);
        historyContainer.setBackground(Color.black);

        playerView = new TMPlayerView(gameState, focusPlayer);

        playerHand = new TMDeckDisplay(this, gameState, gameState.getPlayerHands()[focusPlayer]);
        paneHand = new JScrollPane(playerHand);
        paneHand.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        paneHand.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        paneHand.setPreferredSize(new Dimension(TMDeckDisplay.cardWidth * 4, TMDeckDisplay.cardHeight + 20));

        playerCorporation = new TMCardView(gameState, null, -1, TMDeckDisplay.cardWidth, TMDeckDisplay.cardHeight);

        playerCardChoice = new TMDeckDisplay(this, gameState, gameState.getPlayerCardChoice()[focusPlayer]);
        paneCardChoice = new JScrollPane(playerCardChoice);
        paneCardChoice.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        paneCardChoice.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        paneCardChoice.setPreferredSize(new Dimension(TMDeckDisplay.cardWidth * 4, TMDeckDisplay.cardHeight + 20));

        playerCardsPlayed = new TMDeckDisplay2(this, gameState, gameState.getPlayerComplicatedPointCards()[focusPlayer]);
        paneCardsPlayed = new JScrollPane(playerCardsPlayed);
        paneCardsPlayed.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        paneCardsPlayed.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        paneCardsPlayed.setPreferredSize(new Dimension(TMDeckDisplay.cardWidth + 20, TMDeckDisplay.cardHeight * 4));

        JPanel playerMainWrap = new JPanel();
        playerMainWrap.add(playerView);
        playerMainWrap.add(playerCorporation);

        JPanel playerViewWrapper = new JPanel();
        playerViewWrapper.setLayout(new BoxLayout(playerViewWrapper, BoxLayout.Y_AXIS));
        playerViewWrapper.add(playerMainWrap);
        playerViewWrapper.add(paneHand);
        playerViewWrapper.add(Box.createRigidArea(new Dimension(1, 10)));
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

        JLabel actionLabel = new JLabel("Actions: ");
        actionLabel.setFont(defaultFont);
        actionLabel.setForeground(Color.white);
        actionLabel.setOpaque(false);
        JComponent actionPanel = createActionPanel(new Collection[]{view.getHighlight()}, defaultDisplayWidth*2, defaultActionPanelHeight/2, false,false);
        JPanel actionWrapper = new JPanel();
        actionWrapper.add(actionLabel);
        actionWrapper.add(actionPanel);
        actionWrapper.setOpaque(false);

        top.setOpaque(false);
        playerMainWrap.setOpaque(false);
        playerCorporation.setOpaque(false);
        playerFlipButtons.setOpaque(false);
        paneHand.setOpaque(false);
        paneHand.getViewport().setOpaque(false);
        playerCardChoice.setOpaque(false);
        paneCardChoice.setOpaque(false);
        paneCardsPlayed.setOpaque(false);
        paneCardChoice.getViewport().setOpaque(false);
        paneCardsPlayed.getViewport().setOpaque(false);
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

        JTabbedPane tabs = new JTabbedPane();
        tabs.setForeground(Color.white);
        tabs.setFont(defaultFont);

        JPanel gameWrap = new JPanel();
        gameWrap.setOpaque(false);
        JPanel gamePanel = new JPanel();
        gamePanel.setOpaque(false);
        gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.Y_AXIS));
        gamePanel.add(top);
        gamePanel.add(main);
        gamePanel.add(actionWrapper);
        gameWrap.add(gamePanel);
        gameWrap.add(paneCardsPlayed);
        tabs.add("Game", gameWrap);

        JPanel instructionsPanel = new JPanel();
        instructionsPanel.setBackground(Color.black);
        instructionsPanel.setLayout(new BoxLayout(instructionsPanel, BoxLayout.Y_AXIS));
        JTextPane textPane = new JTextPane();
        textPane.setOpaque(false);
        textPane.setBorder(new EmptyBorder(20, 20, 20, 20));
        textPane.setFont(new Font("Prototype", Font.BOLD, 22));
        textPane.setForeground(Color.lightGray);
        textPane.setEditable(false);
        textPane.setFocusable(false);
        textPane.setPreferredSize(new Dimension(defaultDisplayWidth*2,defaultDisplayHeight*2));
        textPane.setText("Hi there! This is Terraforming Mars: increase temperature and oxygen to max, and place all the ocean tiles to " +
                "end the game. Earn the most points to win! \n\n\n" +
                "We distinguish 3 phases here: Corporation Select (only once in the beginning), and Research and Actions (+ production) repeating every generation.\n\n" +
                "\t- In Corporation Select: you'll be given X corporation cards to choose from. Select the button in the actions list at the bottom corresponding to the card you want.\n\n" +
                "\t- In Research: you'll be given X project cards to potentially buy in your hand (for Y MegaCredits). You'll make this decision one card at a time, choosing for the first card in the list whether you want to buy it or discard it.\n\n" +
                "\t- In Actions: perform actions by either selecting a card in hand to play it (if legal), or choosing another possible action from the menu at the top.\n\n\n" +
                "At the end of the actions phase, all resource production will be turned into resources. Then a new generation begins, with Research + Actions phases.\n\n\n" +
                "Good luck! :)");
        instructionsPanel.add(textPane);

        tabs.add("Help", instructionsPanel);
        Image qmark = ImageIO.GetInstance().getImage("data/terraformingmars/images/resources/wild.png");
        qmark = getScaledImage(qmark, 20, 20);
        tabs.setIconAt(1, new ImageIcon(qmark));
//        menuBar.add(Box.createHorizontalGlue());

        getContentPane().add(tabs);

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setFrameProperties();

        // TODO: display end of game scoring and winner (separate window?)
    }

    private void createActionMenu(AbstractPlayer player, TMGameState gs) {
        if (gs.getGameStatus() == Utils.GameResult.GAME_ONGOING) {
            TMForwardModel fm = (TMForwardModel) player.getForwardModel();
            List<AbstractAction> actions = fm.getAllActions(gs);

            int mnemonicStart = KeyEvent.VK_A;
            for (TMTypes.ActionType t : TMTypes.ActionType.values()) {
                if (t != TMTypes.ActionType.PlayCard) {
                    JMenu menu = actionMenus.get(t);
                    menu.removeAll();

                    for (AbstractAction aa: actions) {
                        TMAction a = (TMAction) aa;
                        if (a.actionType != null && a.actionType == t) {
                            JMenuItem menuItem;
                            if (a.canBePlayed(gs)) {
                                menuItem = new JMenuItem(a.getString(gs));
                                menuItem.setForeground(Color.white);
                                menuItem.addActionListener(e -> {
                                    if (a.getCost() != 0) ac.addAction(new PayForAction(player.getPlayerID(), a));
                                    else ac.addAction(a);
                                });
                            } else {
                                menuItem = new JMenuItem("<html><strike>" + a.getString(gs) + "</strike><html>");
                                menuItem.setForeground(Color.darkGray);
                                menuItem.setToolTipText(getInvalidActionReason(a, gs));
                            }
                            menuItem.setFont(defaultFont);
                            menuItem.setBackground(Color.black);
                            menu.add(menuItem);
                        }
                    }

                    menu.revalidate();
                }
            }
        }
    }

    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState.getGameStatus() == Utils.GameResult.GAME_ONGOING) {

            // Reset buttons
            for (ActionButton actionButton : actionButtons) {
                actionButton.setVisible(false);
                actionButton.setButtonAction(null, "");
                actionButton.setEnabled(true);
                actionButton.setToolTipText("");
            }

            TMGameState gs = (TMGameState) gameState;
            TMForwardModel fm = (TMForwardModel) player.getForwardModel();

            List<AbstractAction> actions = fm.getAllActions(gs);
            List<AbstractAction> legalActions = fm.computeAvailableActions(gs);

            int i = 0;
            TMAction passAction = null;
            ArrayList<TMAction> playCardActions = new ArrayList<>();
            ArrayList<TMAction> placeActions = new ArrayList<>();

            for (AbstractAction aa: actions) {
                TMAction a = (TMAction) aa;
                if (a.actionType == null) {
                    if (a.pass) passAction = a;
                    else if (a instanceof PlaceTile) {
                        placeActions.add(a);
                    } else {
                        actionButtons[i].setVisible(true);
                        if (a.canBePlayed(gs)) {
                            setButtonAction(actionButtons[i], player.getPlayerID(), a, gs);
                        } else {
                            actionButtons[i].setText(a.getString(gs));
                            actionButtons[i].setEnabled(false);
                            actionButtons[i].setToolTipText(getInvalidActionReason(a, gs));
                        }
                        i++;
                    }
                } else if (a.actionType == TMTypes.ActionType.PlayCard) {
                    playCardActions.add(a);
                }
            }

            int highlightIdx = playerHand.getHighlightIndex();
            if (highlightIdx >= 0) {
                // A card to choose
                for (TMAction action: playCardActions) {
                    if (action.getCardID() == gs.getPlayerHands()[focusPlayer].get(highlightIdx).getComponentID()) {
                        actionButtons[i].setVisible(true);
                        if (action.canBePlayed(gs)) {
                            setButtonAction(actionButtons[i], player.getPlayerID(), action, "Play");
                        } else {
                            actionButtons[i].setText(action.getString(gs));
                            actionButtons[i].setEnabled(false);
                            actionButtons[i].setToolTipText(getInvalidActionReason(action, gs));
                        }
                        i++;
                        break;
                    }
                }
            } else {
                if (passAction != null) {
                    actionButtons[i].setVisible(true);
                    actionButtons[i].setButtonAction(passAction, "Pass");
                    i++;
                }
            }
            if (view.highlight.size() > 0) {
                for (Rectangle r: view.highlight) {
                    String code = view.rects.get(r);
                    for (TMAction a: placeActions) {
                        if (a instanceof PlaceTile) {
                            TMMapTile mt = (TMMapTile) gs.getComponentById(((PlaceTile) a).mapTileID);
                            if (mt != null && code.contains("grid")) {
                                // a grid location, trim actions to place tile here
                                int x = Integer.parseInt(code.split("-")[1]);
                                int y = Integer.parseInt(code.split("-")[2]);
                                if (mt.getX() == x && mt.getY() == y) {
                                    actionButtons[i].setVisible(true);
                                    setButtonAction(actionButtons[i], player.getPlayerID(), a, "Place " + ((PlaceTile) a).tile);
                                    i++;
                                }
                            } else if (code.contains("+") || code.contains("-")) {
                                // ?
                            } else {
                                // An extra tile
                                for (TMMapTile mt2: gs.getExtraTiles()) {
                                    if (mt2 != null && mt2.getComponentName().equalsIgnoreCase(code)) {
                                        actionButtons[i].setVisible(true);
                                        setButtonAction(actionButtons[i], player.getPlayerID(), a, "Place " + ((PlaceTile) a).tile);
                                        i++;
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (i == 0) {
                actionButtons[i].setVisible(true);
                actionButtons[i].setEnabled(false);
                actionButtons[i].setButtonAction(null, "Choose a location to place tile");
            }
        }
    }

    private void setButtonAction(ActionButton ab, int player, TMAction action, String text) {
        if (action.getCost() != 0) {
            ab.setButtonAction(new PayForAction(player, action), text);
        } else {
            ab.setButtonAction(action, text);
        }
    }

    private void setButtonAction(ActionButton ab, int player, TMAction action, TMGameState gs) {
        if (action.getCost() != 0) {
            ab.setButtonAction(new PayForAction(player, action), gs);
        } else {
            ab.setButtonAction(action, gs);
        }
    }

    private String getInvalidActionReason(TMAction action, TMGameState gs) {
        String reason = "<html>Reasons:<br/>";
        for (Requirement<TMGameState> req: action.requirements) {
            if (req.testCondition(gs)) reason += "OK: " + req.toString() + "<br/>";
            else reason += "FAIL: " + req.getReasonForFailure(gs).replace("\n", "<br/>") + "<br/>";
        }
        reason += "</html>";
        return reason;
    }

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
            playerHand.update(gs.getPlayerHands()[focusPlayer], false);
            Deck<TMCard> deck = gs.getPlayerCardChoice()[focusPlayer];
            playerCardChoice.update(deck, gs.allCorpChosen() && deck.getSize() > 0);
            playerCardsPlayed.update(gs.getPlayerComplicatedPointCards()[focusPlayer]);

            TMCard corp = gs.getPlayerCorporations()[focusPlayer];
            playerCorporation.update(gs, corp, -1);

            if (player instanceof HumanGUIPlayer) {
                if (actionChosen) {
                    resetActionButtons();
                    firstUpdate = true;
                } else {
                    if (firstUpdate) {
                        createActionMenu(player, gs);
                        updateActionButtons(player, gameState);
                        firstUpdate = false;
                    }
                    if (updateButtons) {
                        updateActionButtons(player, gameState);
                        updateButtons = false;
                    }
                }
            }
        }
        repaint();
    }

    private Image getScaledImage(Image srcImg, int w, int h){
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, w, h, null);
        g2.dispose();

        return resizedImg;
    }
}