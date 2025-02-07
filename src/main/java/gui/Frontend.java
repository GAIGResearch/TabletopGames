package gui;

import core.*;
import core.actions.AbstractAction;
import evaluation.listeners.MetricsGameListener;
import evaluation.optimisation.TunableParameters;
import evaluation.metrics.Event;
import games.GameType;
import gui.models.AITableModel;
import players.PlayerParameters;
import players.PlayerType;
import players.human.ActionController;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.util.List;
import java.util.*;

public class Frontend extends GUI {
    private final int nMaxPlayers = 20;
    private final int defaultNPlayers = 2;
    Timer guiUpdater;
    JFrame[] gameParameterEditWindow, playerParameterEditWindow;
    PlayerParameters[] playerParameters, agentParameters;
    private Thread gameThread;
    private Game gameRunning;
    private boolean showAll, paused, started, showAIWindow;
    private ActionController humanInputQueue;

    public Frontend() {

        // Number of players selection

        JPanel playerSelect = new JPanel();
        playerSelect.setLayout(new BoxLayout(playerSelect, BoxLayout.Y_AXIS));
        JPanel nPlayers = new JPanel(new BorderLayout(5, 5));
        playerSelect.add(nPlayers);
        JLabel nPlayersText = new JLabel("  # players (max " + nMaxPlayers + "):");
        nPlayers.add(BorderLayout.WEST, nPlayersText);
        JTextField nPlayerField = new JTextField("" + defaultNPlayers, 10);  // integer of this is n players
        nPlayers.add(BorderLayout.CENTER, nPlayerField);

        // Game type and parameters selection

        JPanel gameSelect = new JPanel(new BorderLayout(5, 5));
        gameSelect.add(BorderLayout.WEST, new JLabel("  Game type:"));
        String[] gameNames = new String[GameType.values().length];
        TunableParameters[] gameParameters = new TunableParameters[GameType.values().length];
        gameParameterEditWindow = new JFrame[GameType.values().length];
        for (int i = 0; i < gameNames.length; i++) {
            gameNames[i] = GameType.values()[i].name();
            AbstractParameters params = GameType.values()[i].createParameters(0);
            if (params instanceof TunableParameters) {
                gameParameters[i] = (TunableParameters) params;
                gameParameterEditWindow[i] = new JFrame();
                gameParameterEditWindow[i].getContentPane().setLayout(new BoxLayout(gameParameterEditWindow[i].getContentPane(), BoxLayout.Y_AXIS));

                List<String> paramNames = gameParameters[i].getParameterNames();
                HashMap<String, JComboBox<Object>> paramValueOptions = createParameterWindow(paramNames, gameParameters[i], gameParameterEditWindow[i]);

                int idx = i;
                JButton submit = new JButton("Submit");
                submit.addActionListener(e -> {
                    for (String param : paramNames) {
                        gameParameters[idx].setParameterValue(param, paramValueOptions.get(param).getSelectedItem());
                    }
                    gameParameterEditWindow[idx].dispose();
                });
                JButton reset = new JButton("Reset");
                reset.addActionListener(e -> {
                    gameParameters[idx].reset();
                    for (String param : paramNames) {
                        paramValueOptions.get(param).setSelectedItem(gameParameters[idx].getDefaultParameterValue(param));
                    }
                });
                JPanel buttons = new JPanel();
                buttons.add(submit);
                buttons.add(reset);

                gameParameterEditWindow[i].getContentPane().add(buttons);
            }
        }
        GameType firstGameType = GameType.valueOf(gameNames[0]);
        nPlayersText.setText("  # players (min " + firstGameType.getMinPlayers() + ", max " + firstGameType.getMaxPlayers() + "):");

        JComboBox<String> gameOptions = new JComboBox<>(gameNames);  // index of this selection is game
        gameSelect.add(BorderLayout.CENTER, gameOptions);
        JButton gameParameterEdit = new JButton("Edit");
        gameOptions.addActionListener(e -> {
            int idx = gameOptions.getSelectedIndex();
            gameParameterEdit.setVisible(gameParameterEditWindow[idx] != null);

            GameType gameType = GameType.valueOf((String) gameOptions.getSelectedItem());
            nPlayersText.setText("  # players (min " + gameType.getMinPlayers() + ", max " + gameType.getMaxPlayers() + "):");
            pack();
        });
        gameSelect.add(BorderLayout.EAST, gameParameterEdit);
        gameParameterEdit.addActionListener(e -> {
            int idx = gameOptions.getSelectedIndex();
            if (gameParameterEditWindow[idx] != null) {
                gameParameterEditWindow[idx].pack();
                gameParameterEditWindow[idx].setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                gameParameterEditWindow[idx].setTitle("Edit parameters " + gameOptions.getSelectedItem());
                gameParameterEditWindow[idx].setVisible(true);
            }
        });

        // For each player, select type and parameters

        JPanel[] playerOptions = new JPanel[nMaxPlayers];
        JComboBox<String>[] playerOptionsChoice = new JComboBox[nMaxPlayers];  // player is index of this selection
        String[] playerOptionsString = new String[PlayerType.values().length];
        // agentParameters contains the defaults (last edited set) of parameters for each agent type
        agentParameters = new PlayerParameters[PlayerType.values().length];
        for (int i = 0; i < playerOptionsString.length; i++) {
            playerOptionsString[i] = PlayerType.values()[i].name();
            agentParameters[i] = PlayerType.values()[i].createParameterSet();
        }
        // We have one JFrame per player, as different players may use the same agent type with different parameters
        playerParameters = new PlayerParameters[nMaxPlayers];
        playerParameterEditWindow = new JFrame[nMaxPlayers];
        for (int i = 0; i < nMaxPlayers; i++) {
            int playerIdx = i;
            playerOptions[i] = new JPanel(new BorderLayout(5, 5));
            if (i >= defaultNPlayers) {
                playerOptions[i].setVisible(false);
            }
            playerOptions[i].add(BorderLayout.WEST, new JLabel("  Player " + i + ":"));
            JButton paramButton = new JButton("Edit");
            paramButton.setVisible(false);

            JButton fileButton = new JButton("Load JSON");
            fileButton.setVisible(false);

            JPanel paramButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            paramButtonPanel.add(paramButton);
            paramButtonPanel.add(fileButton);

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.addChoosableFileFilter(
                    new FileNameExtensionFilter("JSON files only", "json")
            );

            fileButton.addActionListener(e -> {
                        int retVal = fileChooser.showOpenDialog(this);
                        if (retVal == JFileChooser.APPROVE_OPTION) {
                            try {
                                PlayerParameters.loadFromJSONFile(playerParameters[playerIdx], fileChooser.getSelectedFile().getPath());
                            } catch (Exception loadingException) {
                                System.out.println("File not loadable : " + loadingException.getMessage());
                                int agentIdx = playerOptionsChoice[playerIdx].getSelectedIndex();
                                playerParameters[playerIdx] = (PlayerParameters) agentParameters[agentIdx].copy();
                            }
                        }
                    }
            );

            playerOptionsChoice[i] = new JComboBox<>(playerOptionsString);
            playerOptionsChoice[i].setSelectedItem("Random");

            playerParameterEditWindow[i] = new JFrame();
            playerParameterEditWindow[i].getContentPane().setLayout(new BoxLayout(playerParameterEditWindow[i].getContentPane(), BoxLayout.Y_AXIS));

            playerOptionsChoice[i].addActionListener(e -> {
                int agentIndex = playerOptionsChoice[playerIdx].getSelectedIndex();
                // set Edit button visible if we have parameters to edit; else make it invisible
                paramButton.setVisible(agentParameters[agentIndex] != null);
                fileButton.setVisible(agentParameters[agentIndex] != null);
                // set up the player parameters with the current saved default for that agent type

                paramButton.removeAll();
                try {
                    playerParameters[playerIdx] = (PlayerParameters) agentParameters[agentIndex].copy();
                    paramButton.addActionListener(f -> {
                        initialisePlayerParameterWindow(playerIdx, agentIndex);
                        playerParameterEditWindow[playerIdx].setTitle("Edit parameters " + playerOptionsChoice[playerIdx].getSelectedItem());
                        playerParameterEditWindow[playerIdx].pack();
                        playerParameterEditWindow[playerIdx].setVisible(true);
                        playerParameterEditWindow[playerIdx].setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    });
                } catch (Exception ignored) {}
                pack();
            });
            playerOptions[i].add(BorderLayout.CENTER, playerOptionsChoice[i]);
            playerOptions[i].add(BorderLayout.EAST, paramButtonPanel);
            playerSelect.add(playerOptions[i]);
        }
        JButton updateNPlayers = new JButton("Update");
        updateNPlayers.addActionListener(e -> {
            if (!nPlayerField.getText().isEmpty()) {
                int nP = Integer.parseInt(nPlayerField.getText());
                if (nP > 0 && nP < nMaxPlayers) {
                    for (int i = 0; i < nP; i++) {
                        playerOptions[i].setVisible(true);
                    }
                    for (int i = nP; i < nMaxPlayers; i++) {
                        playerOptions[i].setVisible(false);
                    }
                    pack();
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Error: Please enter number bigger than 0 and less than " + nMaxPlayers, "Error Message",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        nPlayers.add(BorderLayout.EAST, updateNPlayers);

        // Select visuals on/off
        JPanel visualSelect = new JPanel(new BorderLayout(5, 5));
        visualSelect.add(BorderLayout.WEST, new JLabel("  Visuals ON/OFF:"));
        JComboBox<Boolean> visualOptions = new JComboBox<>(new Boolean[]{false, true}); // index here is visuals on/off
        visualOptions.setSelectedItem(true);
        visualSelect.add(BorderLayout.EAST, visualOptions);

        JPanel turnPause = new JPanel(new BorderLayout(5, 5));
        turnPause.add(BorderLayout.WEST, new JLabel("  Pause between turns (ms):"));
        JTextField turnPauseValue = new JTextField("    200");
        turnPause.add(BorderLayout.EAST, turnPauseValue);

        // Select random seed
        JPanel seedSelect = new JPanel(new BorderLayout(5, 5));
        seedSelect.add(BorderLayout.WEST, new JLabel("  Seed:"));
        JTextField seedOption = new JTextField("" + System.currentTimeMillis());  // integer of this is seed
        JButton seedRefresh = new JButton("Refresh");
        seedRefresh.addActionListener(e -> seedOption.setText("" + System.currentTimeMillis()));
        seedSelect.add(BorderLayout.CENTER, seedOption);
        seedSelect.add(BorderLayout.EAST, seedRefresh);

        // Game run core parameters select
        CoreParameters coreParameters = new CoreParameters();
        JPanel gameRunParamSelect = new JPanel();
        gameRunParamSelect.setLayout(new BoxLayout(gameRunParamSelect, BoxLayout.Y_AXIS));
        Map<String, JComboBox<Object>> coreParameterValueOptions = new HashMap<>();
        for (String param : coreParameters.getParameterNames()) {
            JPanel paramPanel = new JPanel();
            paramPanel.setLayout(new BorderLayout(5, 5));
            paramPanel.add(BorderLayout.WEST, new JLabel(String.format("  %-40s", param)));
            paramPanel.add(BorderLayout.CENTER, new JPanel());
            List<?> values = coreParameters.getPossibleValues(param);
            JComboBox<Object> valueOptions = new JComboBox<>(values.toArray());
            valueOptions.setSelectedItem(coreParameters.getDefaultParameterValue(param));
            coreParameterValueOptions.put(param, valueOptions);
            paramPanel.add(BorderLayout.EAST, valueOptions);
            gameRunParamSelect.add(leftJustify(paramPanel));
        }

        // Empty panel to hold game when play button is pressed

        GamePanel gamePanel = new GamePanel();
        gamePanel.setVisible(false);

        JPanel gameControlButtons = new JPanel();
        // Pause game button
        JButton oneAction = new JButton("Next Action");
        oneAction.setToolTipText("Use to take the next AI action when the game is Paused.");
        oneAction.setEnabled(paused && started);

        JButton pauseGame = new JButton("Pause");
        pauseGame.setToolTipText("Toggles pause on and off. When Paused you can use NextAction to move through AI turns.");
        pauseGame.addActionListener(e -> {
            paused = !paused;
            pauseGame.setText(paused ? "Resume" : "Pause");
            if (gameRunning != null) {
                gameRunning.setPaused(paused);
                if (!paused && !gameRunning.isHumanToMove()) {
                    // in this case we need to notify the game loop to get going again
                    synchronized (gameRunning) {
                        gameRunning.notifyAll();
                    }
                }
            }
            oneAction.setEnabled(paused && started);
        });

        // Play button, runs game in separate thread to allow for proper updates
        java.awt.event.ActionListener startTrigger = e -> {
            GUI frame = this;
            Runnable runnable = () -> {
                humanInputQueue = (visualOptions.getSelectedIndex() == 0) ? null : new ActionController();
                long seed = Long.parseLong(seedOption.getText());
                ArrayList<AbstractPlayer> players = new ArrayList<>();
                int nP = Integer.parseInt(nPlayerField.getText());
                String[] playerNames = new String[nP];
                for (int i = 0; i < nP; i++) {
                    AbstractPlayer player = PlayerType.valueOf(playerOptionsChoice[i].getItemAt(playerOptionsChoice[i].getSelectedIndex()))
                            .createPlayerInstance(seed, humanInputQueue, playerParameters[i]);
                    playerNames[i] = player.toString();
                    players.add(player);
                }
                GameType gameType = GameType.valueOf(gameOptions.getItemAt(gameOptions.getSelectedIndex()));
                System.out.println("Playing `" + gameType.name() + "` with players: " + Arrays.toString(playerNames));

                gamePanel.removeAll();
                TunableParameters params = gameParameters[gameOptions.getSelectedIndex()];
                if (params != null) {
                    params.setRandomSeed(seed);
                }
                gameRunning = gameType.createGameInstance(players.size(), params);
                if (gameRunning != null) {
                    // Reset game instance, passing the players for this game
                    gameRunning.reset(players);
                    try {
                        gameRunning.setTurnPause(Integer.parseInt(turnPauseValue.getText().trim()));
                    } catch (NumberFormatException notAnInteger) {
                        // just proceed without collapsing in a heap
                    }
                    // Find core parameters
                    for (String param : coreParameterValueOptions.keySet()) {
                        coreParameters.setParameterValue(param, coreParameterValueOptions.get(param).getSelectedItem());
                    }
                    gameRunning.setCoreParameters(coreParameters);

                    AbstractGUIManager gui = (humanInputQueue != null) ? gameType.createGUIManager(gamePanel, gameRunning, humanInputQueue) : null;
                    setFrameProperties();

                    guiUpdater = new Timer((int) coreParameters.frameSleepMS, event -> updateGUI(gui, frame));
                    guiUpdater.start();
                    // if Pause button has been pressed, then pause at the start so we can track all actions
                    gameRunning.setPaused(paused);
                    // set up sample for the first action
                    listenForDecisions();
                    gameRunning.run();
                    System.out.println("Game over: " + Arrays.toString(gameRunning.getGameState().getPlayerResults()));
                    guiUpdater.stop();
                    // and update GUI to final game state
                    updateGUI(gui, frame);
                }
            };
            gameThread = new Thread(runnable);
            gameThread.start();
        };

        java.awt.event.ActionListener stopTrigger = e -> {
            if (gameRunning != null) {
                gameRunning.setStopped(true);
                if (guiUpdater != null)
                    guiUpdater.stop();
                gameThread.interrupt();
                guiUpdater.stop();
            }
        };

        JButton startGame = new JButton("Play!");
        startGame.setToolTipText("Starts a game (if none running), or Stops a running game.");
        startGame.addActionListener(e -> {
            started = !started;
            if (started) {
                startTrigger.actionPerformed(e);
            } else {
                stopTrigger.actionPerformed(e);
            }
            oneAction.setEnabled(paused && started);
            startGame.setText(started ? "Stop!" : "Play!");
        });


        oneAction.addActionListener(e -> {
            if (paused && gameRunning != null && !gameRunning.isHumanToMove()) {
                // if the thread is running and paused (or human to move)
                // and then take a single action
                // (as long as it is not a human to move...as in this case the GUI is already in control)
                synchronized (gameRunning) {
                    gameRunning.oneAction();
                    gameRunning.notifyAll();
                }
            }
        });

        JButton AIAnalysis = new JButton("AI Window OFF");
        AIAnalysis.setToolTipText("Click to Toggle. If ON, pop-up window shows AI decision statistics prior to each decision.");
        AIAnalysis.setEnabled(false);
        AIAnalysis.addActionListener(e -> {
            showAIWindow = !showAIWindow;
            AIAnalysis.setText(showAIWindow ? "AI Window ON" : "AI Window OFF");
        });

        JButton allActions = new JButton("Showing Self");
        allActions.setToolTipText("Click to Toggle. Either show actions for all players (All), or just those of a human player (Self).");
        allActions.addActionListener(e -> {
            showAll = !showAll;
            AIAnalysis.setEnabled(showAll);
            if (!showAll) {
                showAIWindow = false;
                AIAnalysis.setText("AI Window OFF");
            }
            allActions.setText(showAll ? "Showing All" : "Showing Self");
        });


        gameControlButtons.add(startGame);
        gameControlButtons.add(pauseGame);
        gameControlButtons.add(oneAction);
        gameControlButtons.add(allActions);
        gameControlButtons.add(AIAnalysis);

        // todo tournaments, game report, player report etc

        // Put all together

        JPanel gameOptionFullPanel = new JPanel();
        gameOptionFullPanel.setLayout(new BoxLayout(gameOptionFullPanel, BoxLayout.Y_AXIS));
        gameOptionFullPanel.add(leftJustify(gameSelect));
        gameOptionFullPanel.add(leftJustify(playerSelect));
        gameOptionFullPanel.add(leftJustify(visualSelect));
        gameOptionFullPanel.add(leftJustify(turnPause));
        gameOptionFullPanel.add(leftJustify(seedSelect));
        gameOptionFullPanel.add(new JSeparator());
        gameOptionFullPanel.add(gameRunParamSelect);
        gameOptionFullPanel.add(new JSeparator());
        gameOptionFullPanel.add(gameControlButtons);

        // Collapse run settings panel

        JButton toggleButton = new JButton("<<");
        toggleButton.addActionListener(e -> {
            boolean visible = gameOptionFullPanel.isVisible();
            if (visible) {
                gameOptionFullPanel.setVisible(false);
                toggleButton.setText(">>");
            } else {
                gameOptionFullPanel.setVisible(true);
                toggleButton.setText("<<");
            }
            pack();
        });

        // Wrap all together

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        wrapper.add(gameOptionFullPanel);
        wrapper.add(toggleButton);
        wrapper.add(gamePanel);

        getContentPane().add(wrapper, BorderLayout.CENTER);
        gamePanel.revalidate();
        gamePanel.setVisible(true);
        gamePanel.repaint();

        // Frame properties
        setFrameProperties();
    }

    public static void main(String[] args) {
        new Frontend();
    }

    private void initialisePlayerParameterWindow(int playerIndex, int agentIndex) {
        if (playerParameters[playerIndex] != null) {
            List<String> paramNames = playerParameters[playerIndex].getParameterNames();
            HashMap<String, JComboBox<Object>> paramValueOptions = createParameterWindow(paramNames, playerParameters[playerIndex], playerParameterEditWindow[playerIndex]);

            JButton submit = new JButton("Submit");
            submit.addActionListener(e -> {
                for (String param : paramNames) {
                    playerParameters[playerIndex].setParameterValue(param, paramValueOptions.get(param).getSelectedItem());
                    agentParameters[agentIndex].setParameterValue(param, paramValueOptions.get(param).getSelectedItem());
                    // we also update the central copy, so this change is inherited by future new players
                }
                playerParameterEditWindow[playerIndex].dispose();
            });
            JButton reset = new JButton("Reset");
            reset.addActionListener(e -> {
                playerParameters[playerIndex].reset();
                PlayerParameters defaultParams = PlayerType.values()[agentIndex].createParameterSet();
                if (defaultParams != null)
                    for (String param : paramNames) {
                        paramValueOptions.get(param).setSelectedItem(defaultParams.getDefaultParameterValue(param));
                    }
            });
            JPanel buttons = new JPanel();
            buttons.add(submit);
            buttons.add(reset);

            playerParameterEditWindow[playerIndex].getContentPane().add(buttons);
        }
    }

    private Component leftJustify(JPanel panel) {
        Box b = Box.createHorizontalBox();
        b.add(panel);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.add(Box.createHorizontalGlue());
        return b;
    }

    private void listenForDecisions() {
        // add a listener to detect every time an action has been taken
        gameRunning.addListener(new MetricsGameListener() {
            @Override
            public void onEvent(evaluation.metrics.Event event)
            {
                if(event.type == Event.GameEvent.ACTION_TAKEN)
                    updateSampleActions(event.state.copy());
            }
        });

        // and then do this at the start of the game
        updateSampleActions(gameRunning.getGameState());
    }

    private void updateSampleActions(AbstractGameState state) {
        if (showAIWindow && state.isNotTerminal() && !gameRunning.isHumanToMove()) {
            int nextPlayerID = state.getCurrentPlayer();
            AbstractPlayer nextPlayer = gameRunning.getPlayers().get(nextPlayerID);
            nextPlayer.getAction(state, nextPlayer.getForwardModel().computeAvailableActions(state, nextPlayer.getParameters().actionSpace));

            JFrame AI_debug = new JFrame();
            AI_debug.setTitle(String.format("Player %d, Tick %d, Round %d, Turn %d",
                    nextPlayerID,
                    gameRunning.getTick(),
                    state.getRoundCounter(),
                    state.getTurnCounter()));
            Map<AbstractAction, Map<String, Object>> decisionStats = nextPlayer.getDecisionStats();
            if (decisionStats.size() > 1) {
                AITableModel AIDecisions = new AITableModel(nextPlayer.getDecisionStats());
                JTable table = new JTable(AIDecisions);
                table.setAutoCreateRowSorter(true);
                table.setDefaultRenderer(Double.class, (table1, value, isSelected, hasFocus, row, column) -> new JLabel(String.format("%.2f", (Double) value)));
                JScrollPane scrollPane = new JScrollPane(table);
                table.setFillsViewportHeight(true);
                AI_debug.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                AI_debug.add(scrollPane);
                AI_debug.revalidate();
                AI_debug.pack();
                AI_debug.setVisible(true);
            }
        }
    }


    /**
     * Performs GUI update.
     *
     * @param gui - gui to update.
     */
    private void updateGUI(AbstractGUIManager gui, JFrame frame) {
        AbstractGameState gameState = gameRunning.getGameState().copy();
        int currentPlayer = gameState.getCurrentPlayer();
        AbstractPlayer player = gameRunning.getPlayers().get(currentPlayer);
        if (gui != null) {
            gui.update(player, gameState, gameRunning.isHumanToMove() || showAll);
            if (!gameRunning.isHumanToMove() && paused && showAll) {
                // in this case we allow a human to override an AI decision
                try {
                    if (humanInputQueue.hasAction()) {
                        gameRunning.getForwardModel().next(gameState, humanInputQueue.getAction());
                    }
                } catch (InterruptedException e) {
                    // Really shouldn't happen as we checked first
                    e.printStackTrace();
                }
            }
            if (!gameRunning.isHumanToMove())
                humanInputQueue.reset(); // clear out any actions clicked before their turn
            frame.revalidate();
            frame.repaint();
        }
    }

    private HashMap<String, JComboBox<Object>> createParameterWindow(List<String> paramNames, TunableParameters pp, JFrame frame) {
        HashMap<String, JComboBox<Object>> paramValueOptions = new HashMap<>();
        frame.getContentPane().removeAll();
        for (String param : paramNames) {
            JPanel paramPanel = new JPanel(new BorderLayout());
            paramPanel.add(BorderLayout.WEST, new JLabel("  " + param));
            List<Object> values = pp.getPossibleValues(param);
            JComboBox<Object> valueOptions = new JComboBox<>(values.toArray());
            valueOptions.setSelectedItem(pp.getParameterValue(param));
            paramValueOptions.put(param, valueOptions);
            paramPanel.add(BorderLayout.EAST, valueOptions);
            frame.getContentPane().add(paramPanel);
        }
        return paramValueOptions;
    }

}
