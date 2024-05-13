package games.terraformingmars.gui;

import core.*;
import core.actions.AbstractAction;
import core.components.Deck;
import games.terraformingmars.TMForwardModel;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTurnOrder;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.PayForAction;
import games.terraformingmars.actions.PlaceTile;
import games.terraformingmars.actions.TMAction;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.components.TMMapTile;
import games.terraformingmars.rules.requirements.Requirement;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import utilities.ImageIO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;

public class TMGUI extends AbstractGUIManager {

    TMBoardView view;
    TMPlayerView playerView;
    TMDeckDisplay playerHand, playerCardChoice;
    TMCardView playerCorporation, lastCardPlayed;
    TMDeckDisplay playerCardsPlayed;
    JScrollPane paneHand, paneCardChoice, paneCardsPlayed;
    JLabel generationCount;

    static int fontSize = 16;
    static Font defaultFont = new Font("Prototype", Font.BOLD, fontSize);
    static Color fontColor = Color.white;
    static Color bgColor = Color.black;
    static Color grayColor = Color.gray;
    static Color lightGrayColor = Color.lightGray;
    static Color darkGrayColor = Color.darkGray;
    static int focusPlayer = 0;

    int currentPlayerIdx = 0;
    boolean focusCurrentPlayer;
    JButton focusPlayerButton;

    boolean updateButtons = false;
    HashMap<TMTypes.ActionType, JMenu> actionMenus;

    TMAction lastAction;
    TMTurnOrder turnOrder;

    public TMGUI(GamePanel parent, Game game, ActionController ac, Set<Integer> humanId) {
        super(parent, game, ac, humanId);
        if (game == null) return;

        // Make backgroundImage the content pane.
//        BufferedImage bg = (BufferedImage) ImageIO.GetInstance().getImage("data/terraformingmars/images/stars.jpg");
//        TexturePaint space = new TexturePaint(bg, new Rectangle2D.Float(0,0, bg.getWidth(), bg.getHeight()));
//        TiledImage backgroundImage = new TiledImage(space);
//        setContentPane(backgroundImage);
        parent.setBackground(bgColor);

        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);

        TMGameState gameState = (TMGameState) game.getGameState();
        view = new TMBoardView(this, gameState);
        view.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                stateChange = true;
            }
        });

        actionMenus = new HashMap<>();
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(bgColor);
        int mnemonicStart = KeyEvent.VK_A;
        for (TMTypes.ActionType t : TMTypes.ActionType.values()) {
            if (t == TMTypes.ActionType.BuyProject) continue;
            JMenu menu = new JMenu(t.name());
            menu.setMnemonic(mnemonicStart++);
            menu.getAccessibleContext().setAccessibleDescription("Choose an action of type " + t.name());
            menu.setForeground(fontColor);
            menu.setFont(defaultFont);
            actionMenus.put(t, menu);
            menuBar.add(menu);
        }

//        parent.setJMenuBar(menuBar);

        try {
            GraphicsEnvironment ge =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("data/terraformingmars/images/fonts/Prototype.ttf")));
        } catch (IOException |FontFormatException e) {
            //Handle exception
        }

        createActionHistoryPanel(defaultDisplayWidth, defaultInfoPanelHeight/2, new HashSet<>());
        historyInfo.setFont(defaultFont);
        historyInfo.setForeground(fontColor);
        JPanel historyWrapper = new JPanel();
        JLabel historyText = new JLabel("Action history:");
        historyText.setFont(defaultFont);
        historyText.setForeground(fontColor);
        historyWrapper.add(historyText);
        historyWrapper.add(historyContainer);
        historyContainer.setBackground(bgColor);

        playerView = new TMPlayerView(gameState, focusPlayer);

        playerHand = new TMDeckDisplay(this, gameState, gameState.getPlayerHands()[focusPlayer], true);
        paneHand = new JScrollPane(playerHand);
        paneHand.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        paneHand.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        paneHand.setPreferredSize(new Dimension(TMDeckDisplay.cardWidth * 4, TMDeckDisplay.cardHeight + 20));
        playerCorporation = new TMCardView(gameState, null, -1, TMDeckDisplay.cardWidth, TMDeckDisplay.cardHeight);

        playerCardChoice = new TMDeckDisplay(this, gameState, gameState.getPlayerCardChoice()[focusPlayer], true);
        paneCardChoice = new JScrollPane(playerCardChoice);
        paneCardChoice.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        paneCardChoice.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        paneCardChoice.setPreferredSize(new Dimension(TMDeckDisplay.cardWidth * 4, TMDeckDisplay.cardHeight + 20));

        lastCardPlayed = new TMCardView(gameState, null, -1, TMDeckDisplay.cardWidth, TMDeckDisplay.cardHeight);
        JLabel label = new JLabel("Last card played:");
        label.setFont(defaultFont);
        label.setForeground(fontColor);
        label.setOpaque(false);
        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setOpaque(false);
        wrap.add(label);
        wrap.add(lastCardPlayed);
        wrap.setPreferredSize(new Dimension(TMDeckDisplay.cardWidth, TMDeckDisplay.cardHeight + 50));

        playerCardsPlayed = new TMDeckDisplay(this, gameState, gameState.getPlayerComplicatedPointCards()[focusPlayer], false);
        paneCardsPlayed = new JScrollPane(playerCardsPlayed);
        paneCardsPlayed.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        paneCardsPlayed.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        paneCardsPlayed.setPreferredSize(new Dimension(TMDeckDisplay.cardWidth + 20, TMDeckDisplay.cardHeight * 3));

        JPanel wrap2 = new JPanel();
        wrap2.setOpaque(false);
        wrap2.setLayout(new BoxLayout(wrap2, BoxLayout.Y_AXIS));
        wrap2.add(wrap);
        wrap2.add(paneCardsPlayed);

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
        gamePhase.setForeground(fontColor);
        generationCount.setForeground(fontColor);
        JPanel infoWrapper = new JPanel();
        infoWrapper.setLayout(new BoxLayout(infoWrapper, BoxLayout.Y_AXIS));
        infoWrapper.add(generationCount);
        infoWrapper.add(gamePhase);

        JPanel playerFlipButtons = new JPanel();
        JLabel jLabel1 = new JLabel("Change player display:");
        jLabel1.setFont(defaultFont);
        jLabel1.setForeground(fontColor);
        playerFlipButtons.add(jLabel1);
        for (int i = 0; i < gameState.getNPlayers(); i++) {
            String text = "p" + i;
            JButton jb = new JButton(text);
            jb.setFont(defaultFont);
            jb.setForeground(fontColor);
            jb.setBackground(darkGrayColor);
            jb.addActionListener(e -> {
                focusPlayer = Integer.parseInt(jb.getText().replace("p",""));
                focusCurrentPlayer = false;
            });
            playerFlipButtons.add(jb);
        }
        focusPlayerButton = new JButton("Current player: " + currentPlayerIdx);
        focusPlayerButton.setFont(defaultFont);
        focusPlayerButton.setForeground(fontColor);
        focusPlayerButton.setBackground(darkGrayColor);
        focusPlayerButton.addActionListener(e -> focusCurrentPlayer = true);
        playerFlipButtons.add(focusPlayerButton);

        JButton jb2 = new JButton("Pause/Resume");
        jb2.setFont(defaultFont);
        jb2.setForeground(fontColor);
        jb2.setBackground(grayColor);
        jb2.addActionListener(e -> game.flipPaused());
        playerFlipButtons.add(jb2);

        JPanel top = new JPanel();
        top.add(historyWrapper);
        top.add(playerFlipButtons);
        top.add(infoWrapper);

        JLabel actionLabel = new JLabel("Actions: ");
        actionLabel.setFont(defaultFont);
        actionLabel.setForeground(fontColor);
        actionLabel.setOpaque(false);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[]{view, playerHand, playerCardChoice}, defaultDisplayWidth*2, defaultActionPanelHeight/2, false,false, null, null, null);
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

        JTabbedPane tabs = new SeeThroughTabbedPane();
        tabs.setForeground(fontColor);
        tabs.setFont(defaultFont);
        tabs.setBackground(bgColor);
        tabs.setOpaque(false);

        JPanel gameWrap = new JPanel();
        gameWrap.setOpaque(false);
        JPanel gamePanel = new JPanel();
        gamePanel.setOpaque(false);
        gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.Y_AXIS));
        gamePanel.add(top);
        gamePanel.add(main);
        gamePanel.add(actionWrapper);
        gameWrap.add(gamePanel);
        gameWrap.add(wrap2);
        tabs.add("Game", gameWrap);

        JPanel instructionsPanel = new JPanel();
        instructionsPanel.setBackground(bgColor);
        instructionsPanel.setLayout(new BoxLayout(instructionsPanel, BoxLayout.Y_AXIS));
        JTextPane textPane = new JTextPane();
        textPane.setOpaque(false);
        textPane.setBorder(new EmptyBorder(20, 20, 20, 20));
        textPane.setFont(new Font("Prototype", Font.BOLD, 22));
        textPane.setForeground(lightGrayColor);
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

//        parent.setExtendedState(JFrame.MAXIMIZED_BOTH);
//        tabs.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize());

        parent.setLayout(new BorderLayout());

        JPanel scrolling = new JPanel();
        scrolling.setLayout(new BorderLayout());

        scrolling.add(tabs, BorderLayout.CENTER);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        JScrollPane pane = new JScrollPane(scrolling);
        pane.setPreferredSize(new Dimension((int)(screenSize.width*0.95), (int)(screenSize.height*0.9)));
        parent.add(menuBar, BorderLayout.NORTH);
        parent.add(pane);

        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();

        // TODO: display end of game scoring and winner (separate window?)
    }

    @Override
    public int getMaxActionSpace() {
        return 500;
    }

    private void createActionMenu(AbstractPlayer player, TMGameState gs) {
        if (gs.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING) {
            TMForwardModel fm = (TMForwardModel) player.getForwardModel();
            List<AbstractAction> actions = fm.getAllActions(gs);
            List<AbstractAction> legalActions = fm.computeAvailableActions(gs);

            int mnemonicStart = KeyEvent.VK_A;
            for (TMTypes.ActionType t : TMTypes.ActionType.values()) {
                if (t != TMTypes.ActionType.PlayCard && t != TMTypes.ActionType.BuyProject) {
                    JMenu menu = actionMenus.get(t);
                    menu.removeAll();

                    for (AbstractAction aa: actions) {
                        TMAction a = (TMAction) aa;
                        TMAction fullLegalAction = getFullLegalAction(a, legalActions);

                        if (a.actionType != null && a.actionType == t) {
                            JMenuItem menuItem;
                            if (fullLegalAction != null) {
                                menuItem = new JMenuItem(a.getString(gs));
                                menuItem.setForeground(fontColor);
                                menuItem.addActionListener(e -> ac.addAction(fullLegalAction));
                            } else {
                                menuItem = new JMenuItem("<html><strike>" + a.getString(gs) + "</strike><html>");
                                menuItem.setForeground(darkGrayColor);
                                menuItem.setToolTipText(getInvalidActionReason(a, gs));
                            }
                            menuItem.setFont(defaultFont);
                            menuItem.setBackground(bgColor);
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
        if (gameState.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING) {

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
                    if (a instanceof PlaceTile && ((PlaceTile) a).mapTileID != -1 && ((PlaceTile) a).tile != null) {
                        placeActions.add(a);
                    } else {
                        if (a.pass) {
                            actionButtons[i].setVisible(true);
                            actionButtons[i].setButtonAction(a, "Pass");
                            i++;
                        } else {
                            TMAction fullLegalAction = getFullLegalAction(a, legalActions);
                            actionButtons[i].setVisible(true);
                            if (fullLegalAction != null) {
                                actionButtons[i].setButtonAction(fullLegalAction, gs);
                            } else {
                                actionButtons[i].setText(a.getString(gs));
                                actionButtons[i].setEnabled(false);
                                actionButtons[i].setToolTipText(getInvalidActionReason(a, gs));
                            }
                            i++;
                        }
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
                        TMAction fullLegalAction = getFullLegalAction(action, legalActions);
                        actionButtons[i].setVisible(true);
                        if (fullLegalAction != null) {
                            actionButtons[i].setButtonAction(fullLegalAction, "Play");
                        } else {
                            actionButtons[i].setText("Play");
                            actionButtons[i].setEnabled(false);
                            actionButtons[i].setToolTipText(getInvalidActionReason(action, gs));
                        }
                        i++;
                        break;
                    }
                }
            }
            if (view.highlight.size() > 0) {
                for (Rectangle r: view.highlight) {
                    String code = view.rects.get(r);
                    for (TMAction a: placeActions) {
                        if (a instanceof PlaceTile) {
                            TMAction fullLegalAction = getFullLegalAction(a, legalActions);

                            TMMapTile mt = (TMMapTile) gs.getComponentById(((PlaceTile) a).mapTileID);
                            if (mt != null && code.contains("grid")) {
                                // a grid location, trim actions to place tile here
                                int x = Integer.parseInt(code.split("-")[1]);
                                int y = Integer.parseInt(code.split("-")[2]);
                                if (mt.getX() == x && mt.getY() == y) {
                                    actionButtons[i].setVisible(true);
                                    if (fullLegalAction != null) {
                                        actionButtons[i].setButtonAction(fullLegalAction, gs);
                                    } else {
                                        actionButtons[i].setButtonAction(a, gs);
                                    }
                                    i++;
                                }
                            } else if (code.contains("+") || code.contains("-")) {
                                // ?
                            } else if (mt != null) {
                                // An extra tile
                                for (TMMapTile mt2: gs.getExtraTiles()) {
                                    if (mt2 != null && mt2.getComponentName().equalsIgnoreCase(code) && mt2.getComponentName().equalsIgnoreCase(mt.getComponentName())) {
                                        actionButtons[i].setVisible(true);
                                        if (fullLegalAction != null) {
                                            actionButtons[i].setButtonAction(fullLegalAction, gs);
                                        } else {
                                            actionButtons[i].setButtonAction(a, gs);
                                        }
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

    private TMAction getFullLegalAction(TMAction a, List<AbstractAction> legalActions) {
        for (AbstractAction aa: legalActions) {
            if (aa instanceof PayForAction && a.equals(((PayForAction) aa).action)) return (TMAction) aa;
            if (a.equals(aa)) return (TMAction) aa;
        }
        return null;
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

    boolean stateChange;

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {

            TMGameState gs = ((TMGameState) gameState);

            if (gameState.getGameStatus() == CoreConstants.GameResult.GAME_END) {
                int win = -1;
                String displayText = "<html><table><tr><td>Player</td><td>TR</td><td>Milestones</td><td>Awards</td><td>Board</td><td>Cards</td><td>Total</td></tr>";
                for (int i = 0; i < gameState.getNPlayers(); i++) {
                    if (gameState.getPlayerResults()[i] == CoreConstants.GameResult.WIN_GAME) win = i;

                    int tr = gs.getPlayerResources()[i].get(TMTypes.Resource.TR).getValue();
                    int milestones = gs.countPointsMilestones(i);
                    int awards = gs.countPointsAwards(i);
                    int board = gs.countPointsBoard(i);
                    int cards = gs.countPointsCards(i);
                    int total = tr + milestones + awards + board + cards;

                    displayText += "<tr><td>" + i + "</td><td>" + tr + "</td><td>" + milestones +"</td><td>" + awards
                            + "</td><td>" + board + "</td><td>" + cards + "</td><td>" + total + "</td></tr>";
                }
                displayText += "</table><hr>Winner(s): " + win + "</html>";
                JOptionPane.showConfirmDialog(parent, displayText, "Terraforming Mars: Game Over", OK_CANCEL_OPTION, INFORMATION_MESSAGE);
            }

            if (player instanceof HumanGUIPlayer) {
                TMAction action = (TMAction) gameState.getHistory().get(gameState.getHistory().size()-1).b;
                TMTurnOrder turnOrder = (TMTurnOrder) gs.getTurnOrder();
                if (!action.equals(lastAction) || !turnOrder.equals(this.turnOrder)) {
                    createActionMenu(player, (TMGameState) gameState);
                    this.lastAction = action.copy();
                    this.turnOrder = (TMTurnOrder) turnOrder.copy();
                }
            } else {
                resetActionButtons();
            }

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
            playerCardChoice.clearHighlights();
            playerCardChoice.update(deck, gs.allCorpChosen() && deck.getSize() > 0);

            // Display points and resource cards, + most recent card played
            if (gs.getPlayedCards()[focusPlayer].getSize() > 0) {
                lastCardPlayed.update(gs, gs.getPlayedCards()[focusPlayer].get(0).copy(), -1);
            } else {
                lastCardPlayed.update(gs, null, -1);
            }
            playerCardsPlayed.update(gs.getPlayerComplicatedPointCards()[focusPlayer].copy(), false);

            TMCard corp = gs.getPlayerCorporations()[focusPlayer];
            playerCorporation.update(gs, corp, -1);  // TODO do it once

        }
        parent.repaint();
    }

    private Image getScaledImage(Image srcImg, int w, int h){
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, w, h, null);
        g2.dispose();

        return resizedImg;
    }

    public static class SeeThroughTabbedPane extends JTabbedPane {

        private float alpha;

        public SeeThroughTabbedPane() {
            setOpaque(false);
            setAlpha(1f);
        }

        public void setAlpha(float value) {
            if (alpha != value) {
                float old = alpha;
                this.alpha = value;
                firePropertyChange("alpha", old, alpha);
                repaint();
            }
        }

        public float getAlpha() {
            return alpha;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(getBackground());
            g2d.setComposite(AlphaComposite.SrcOver.derive(getAlpha()));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.dispose();
            super.paintComponent(g);
        }

    }

}
