package gui;

import com.formdev.flatlaf.FlatDarculaLaf;
import core.*;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import evaluation.RunArg;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.Event;
import evaluation.optimisation.TunableParameters;
import evaluation.tournaments.AbstractTournament;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import gui.models.AITableModel;
import players.PlayerParameters;
import players.PlayerType;
import players.heuristics.StateHeuristicType;
import players.heuristics.StringHeuristic;
import players.human.ActionController;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;

public class LLMFrontend extends GUI {
    private final int nMaxPlayers = 20;
    private final int defaultNPlayers = 2;
    Timer guiUpdater;
    JFrame[] gameParameterEditWindow, playerParameterEditWindow;
    PlayerParameters[] playerParameters, agentParameters;
    JButton[] editHeuristicButtons;
    private Thread gameThread;
    private Game gameRunning;
    private boolean showAll, paused, started, showAIWindow;
    private ActionController humanInputQueue;

    public LLMFrontend() {

        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

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
//        GameType firstGameType = GameType.valueOf(gameNames[0]);
        GameType firstGameType = GameType.LoveLetter;

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
        editHeuristicButtons = new JButton[nMaxPlayers];
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

            // Add a button to view and edit the String Heuristic text in filePath
            editHeuristicButtons[i] = new JButton("Edit Heuristic");
            editHeuristicButtons[i].setVisible(false);
            int pId = i;
            editHeuristicButtons[i].addActionListener(ex -> {
                if (playerParameters[pId] != null) {
                    IStateHeuristic h = playerParameters[pId].getStateHeuristic();
                    if (h instanceof StringHeuristic heuristic) {
                        String filePath = heuristic.getFileName();
                        JFrame editFrame = new JFrame();
                        editFrame.getContentPane().setLayout(new BoxLayout(editFrame.getContentPane(), BoxLayout.Y_AXIS));
                        editFrame.setTitle("Edit " + filePath);
                        JTextArea textArea = new JTextArea();
                        textArea.setText(heuristic.getHeuristicCode());
                        JScrollPane scrollPane = new JScrollPane(textArea);
                        scrollPane.setPreferredSize(new Dimension(800, 500));
                        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
                        scrollPane.getVerticalScrollBar().setBlockIncrement(100);
                        editFrame.getContentPane().add(scrollPane);
                        JButton saveButton = new JButton("Save");
                        saveButton.setEnabled(false);
                        saveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
                        saveButton.addActionListener(e1 -> {
                            BufferedWriter writer = null;
                            try {
                                writer = new BufferedWriter(new FileWriter(filePath));
                                writer.write(textArea.getText());
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            } finally {
                                try {
                                    if (writer != null) {
                                        writer.close();
                                    }
                                } catch (IOException ioException) {
                                    ioException.printStackTrace();
                                }
                            }
                            heuristic.setHeuristicCode(textArea.getText());
                            saveButton.setEnabled(false);
                        });
                        editFrame.getContentPane().add(saveButton);

                        textArea.getDocument().addDocumentListener(new DocumentListener() {
                            @Override
                            public void insertUpdate(DocumentEvent e) {
                                saveButton.setEnabled(true);
                            }

                            @Override
                            public void removeUpdate(DocumentEvent e) {
                                saveButton.setEnabled(true);
                            }

                            @Override
                            public void changedUpdate(DocumentEvent e) {
                                saveButton.setEnabled(true);
                            }
                        });

                        editFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        editFrame.setSize(800, 600);
                        editFrame.setVisible(true);
                    }
                }
            });
            // todo button should be disabled if changing heuristic or agent

            JPanel paramButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            paramButtonPanel.add(paramButton);
            paramButtonPanel.add(fileButton);
            paramButtonPanel.add(editHeuristicButtons[i]);

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
            if (!nPlayerField.getText().equals("")) {
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

        // Submit final feedback to LLM bot
        JTextArea feedback = new JTextArea("");
        feedback.setLineWrap(true);
        feedback.setWrapStyleWord(true);
//        feedback.setPreferredSize(new Dimension(300, 100));
        JScrollPane spFeedback = new JScrollPane(feedback);
        spFeedback.setPreferredSize(new Dimension(300, 150));
        spFeedback.getVerticalScrollBar().setUnitIncrement(16);  // Default is usually 1-5
        spFeedback.getVerticalScrollBar().setBlockIncrement(100);  // Default is usually larger
        JButton submitFeedback = new JButton("Submit Feedback");
        submitFeedback.addActionListener(e -> {
            System.out.println("Feedback: " + feedback.getText());
            feedback.setText("");
        });
        submitFeedback.setEnabled(false);
        submitFeedback.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Console output logs
        JTextArea output = new JTextArea("");
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
//        output.setPreferredSize(new Dimension(300, 50));
        output.setEditable(false);
        JScrollPane spOutput = new JScrollPane(output);
        spOutput.setPreferredSize(new Dimension(300, 150));
        spOutput.getVerticalScrollBar().setUnitIncrement(16);  // Default is usually 1-5
        spOutput.getVerticalScrollBar().setBlockIncrement(100);  // Default is usually larger
        redirectSystemStreams(output);

        // Select random seed
        JPanel seedSelect = new JPanel(new BorderLayout(5, 5));
        seedSelect.add(BorderLayout.WEST, new JLabel("  Seed:"));
        JTextField seedOption = new JTextField("" + System.currentTimeMillis());  // integer of this is seed
        JButton seedRefresh = new JButton("Refresh");
        seedRefresh.addActionListener(e -> seedOption.setText("" + System.currentTimeMillis()));
        seedSelect.add(BorderLayout.CENTER, seedOption);
        seedSelect.add(BorderLayout.EAST, seedRefresh);

        // Select n games to run
        JPanel nGamesSelect = new JPanel(new BorderLayout(5, 5));
        nGamesSelect.add(BorderLayout.WEST, new JLabel("  N Games:"));
        JTextField nGamesOption = new JTextField("1");
        nGamesSelect.add(BorderLayout.CENTER, nGamesOption);

        // Game run core parameters select
        CoreParameters coreParameters = new CoreParameters();
        JPanel gameRunParamSelect = new JPanel();
        gameRunParamSelect.setLayout(new BoxLayout(gameRunParamSelect, BoxLayout.Y_AXIS));
        HashMap<String, JComboBox<Object>> coreParameterValueOptions = new HashMap<>();
        for (String param : coreParameters.getParameterNames()) {
            JPanel paramPanel = new JPanel();
            paramPanel.setLayout(new BorderLayout(5, 5));
            paramPanel.add(BorderLayout.WEST, new JLabel(String.format("  %-40s", param)));
            paramPanel.add(BorderLayout.CENTER, new JPanel());
            List<Object> values = coreParameters.getPossibleValues(param);
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
        // Analysis button
        JButton AIAnalysis = new JButton("AI Window OFF");
        AIAnalysis.setToolTipText("Click to Toggle. If ON, pop-up window shows AI decision statistics prior to each decision.");
        AIAnalysis.setEnabled(false);
        AIAnalysis.addActionListener(e -> {
            showAIWindow = !showAIWindow;
            AIAnalysis.setText(showAIWindow ? "AI Window ON" : "AI Window OFF");
        });
        AIAnalysis.setEnabled(false);

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
        allActions.setEnabled(false);

        // Pause game button
        JButton oneAction = new JButton("Next Action");
        oneAction.setToolTipText("Use to take the next AI action when the game is Paused.");
        oneAction.setEnabled(paused && started);

        JButton startGame = new JButton("Play!");
        startGame.setToolTipText("Starts a game (if none running), or Stops a running game.");

        JButton pauseGame = new JButton("Pause");
        pauseGame.setEnabled(false);
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

        java.awt.event.ActionListener stopTrigger = e -> {
            if (gameRunning != null) {
                gameRunning.setStopped(true);
                if (guiUpdater != null)
                    guiUpdater.stop();
                gameThread.interrupt();
                guiUpdater.stop();
            }
        };

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

                int nGames = Integer.parseInt(nGamesOption.getText());

                if (nGames > 1 && humanInputQueue == null) {
                    Map<RunArg, Object> config = new HashMap<>();
                    config.put(RunArg.matchups, nGames);
                    config.put(RunArg.budget, 100);  // todo param? or from agent?
                    RoundRobinTournament tournament = new RoundRobinTournament(players, gameType, players.size(), params, config);
                    tournament.run();
                } else {
                    gameRunning = gameType.createGameInstance(players.size(), params);

                    // GUI
                    AbstractGUIManager gui = (humanInputQueue != null) ? gameType.createGUIManager(gamePanel, gameRunning, humanInputQueue) : null;

                    // Find core parameters
                    for (String param : coreParameterValueOptions.keySet()) {
                        coreParameters.setParameterValue(param, coreParameterValueOptions.get(param).getSelectedItem());
                    }
                    gameRunning.setCoreParameters(coreParameters);

                    for (int i = 0; i < nGames; i++) {
                        // Reset game instance, passing the players for this game
                        gameRunning.reset(players);
                        try {
                            gameRunning.setTurnPause(Integer.parseInt(turnPauseValue.getText().trim()));
                        } catch (NumberFormatException notAnInteger) {
                            // just proceed without collapsing in a heap
                        }

                        if (gui != null) {
                            setFrameProperties();
                            pack();
                            guiUpdater = new Timer((int) coreParameters.frameSleepMS, event -> updateGUI(gui, frame));
                            guiUpdater.start();
                        }

                        // if Pause button has been pressed, then pause at the start so we can track all actions
                        gameRunning.setPaused(paused);
                        // set up sample for the first action
                        listenForDecisions();
                        try {
                            gameRunning.run();
                            System.out.println("Game over: " + Arrays.toString(gameRunning.getGameState().getPlayerResults()));
                            if (gui != null) {
                                guiUpdater.stop();
                                // and update GUI to final game state
                                updateGUI(gui, frame);
                            }
                        } catch (Exception ex) {
                            System.out.println("Error: " + ex.getMessage());
                        }
                    }
                }

                submitFeedback.setEnabled(true);
                pauseGame.setEnabled(false);
                started = false;
                oneAction.setEnabled(false);
                AIAnalysis.setEnabled(false);
                allActions.setEnabled(false);
                startGame.setText("Play!");
            };
            gameThread = new Thread(runnable);
            gameThread.start();
        };

        startGame.addActionListener(e -> {
            started = !started;
            if (started) {
                startTrigger.actionPerformed(e);
            } else {
                stopTrigger.actionPerformed(e);
            }
            oneAction.setEnabled(paused && started);
            pauseGame.setEnabled(started);
            AIAnalysis.setEnabled(started);
            allActions.setEnabled(started);
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

        gameControlButtons.add(startGame);
        gameControlButtons.add(pauseGame);
        gameControlButtons.add(oneAction);
        gameControlButtons.add(allActions);
        gameControlButtons.add(AIAnalysis);


        // Put all together

        JPanel gameOptionFullPanel = new JPanel();
        gameOptionFullPanel.setLayout(new BoxLayout(gameOptionFullPanel, BoxLayout.Y_AXIS));
        gameOptionFullPanel.add(leftJustify(gameSelect));
        gameOptionFullPanel.add(leftJustify(playerSelect));
        gameOptionFullPanel.add(leftJustify(visualSelect));
        gameOptionFullPanel.add(leftJustify(turnPause));
        gameOptionFullPanel.add(leftJustify(seedSelect));
        gameOptionFullPanel.add(leftJustify(nGamesSelect));
        gameOptionFullPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        gameOptionFullPanel.add(new JSeparator());
        gameOptionFullPanel.add(Box.createRigidArea(new Dimension(0, 5)));
//        gameOptionFullPanel.add(gameRunParamSelect);
//        gameOptionFullPanel.add(Box.createRigidArea(new Dimension(0, 5)));
//        gameOptionFullPanel.add(new JSeparator());
//        gameOptionFullPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        gameOptionFullPanel.add(gameControlButtons);
        gameOptionFullPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        gameOptionFullPanel.add(new JSeparator());
        gameOptionFullPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        JLabel outputLabel = new JLabel("Console output:");
        outputLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        gameOptionFullPanel.add(outputLabel);
        gameOptionFullPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        gameOptionFullPanel.add(spOutput);
        gameOptionFullPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        gameOptionFullPanel.add(new JSeparator());
        gameOptionFullPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        JLabel feedbackLabel = new JLabel("LLM Feedback Input Post-Game:");
        feedbackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        gameOptionFullPanel.add(feedbackLabel);
        gameOptionFullPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        gameOptionFullPanel.add(spFeedback);
        gameOptionFullPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        gameOptionFullPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        // Align button center
        gameOptionFullPanel.add(submitFeedback);


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

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("TAG:LLM Frontend");
        // Increase title font size
        title.setFont(new Font("Serif", Font.BOLD, 20));
        title.setForeground(new Color(124, 241, 231, 190));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(title);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(wrapper);
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        gamePanel.revalidate();
        gamePanel.setVisible(true);
        gamePanel.repaint();

        // Frame properties
        setFrameProperties();

        pack();
    }

    private void redirectSystemStreams(JTextArea textArea) {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                // Append the single character to the JTextArea
                textArea.append(String.valueOf((char) b));
                textArea.setCaretPosition(textArea.getDocument().getLength());
            }

            @Override
            public void write(byte[] b, int off, int len) {
                // Append the portion of the byte array to the JTextArea
                textArea.append(new String(b, off, len));
                textArea.setCaretPosition(textArea.getDocument().getLength());
            }

            @Override
            public void write(byte[] b) {
                // Append the entire byte array to the JTextArea
                write(b, 0, b.length);
            }
        };

        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    public static void main(String[] args) {
        new LLMFrontend();

        // todo pipeline, frontend-controlled.
        //  - some way of getting the base prompt for a game, and allow edit (maybe in GameType, have a file with the prompt)
        //      - for new games where we don't have this... input game manual (external doc) + API (game-specific) + structure (general)
        //  - select heuristic player, have button for 'attach LLM'. This will call the LLM with the initial prompt and load up the heuristic generated
        //  - we play the game, maybe several times, with human/other player, then submit feedback
        //  - the 'submit feedback' button not only prints feedback, but also iterates on heuristic function with LLM
        //  - print to console the extra fluff text generated maybe?
        //  - repeat!

        // todo
        //  - log interactions to file with timestamp (feedback sent, LLM complete output, game results). get some people to play
        //  - alternative: submit feedback mid-game. Pause game, iterate heuristic, resume game.
        //  - alternative: have the LLM iterate by itself for some generations, using automatic evaluation of heuristic
    }

    private void initialisePlayerParameterWindow(int playerIndex, int agentIndex) {
        if (playerParameters[playerIndex] != null) {
            List<String> paramNames = playerParameters[playerIndex].getParameterNames();
            HashMap<String, JComboBox<Object>> paramValueOptions = createParameterWindow(paramNames, playerParameters[playerIndex], playerParameterEditWindow[playerIndex]);

            JButton submit = new JButton("Submit");
            submit.addActionListener(e -> {
                boolean foundStringHeuristic = false;
                for (String param : paramNames) {
                    Object value = paramValueOptions.get(param).getSelectedItem();
                    playerParameters[playerIndex].setParameterValue(param, value);
                    // we also update the central copy, so this change is inherited by future new players

                    // Add a button to view and edit the String Heuristic text in filePath
                    if (value == StateHeuristicType.StringHeuristic) {
                        editHeuristicButtons[playerIndex].setVisible(true);
                        foundStringHeuristic = true;
                    }
                }
                agentParameters[agentIndex] = (PlayerParameters) playerParameters[playerIndex].copy();
                playerParameterEditWindow[playerIndex].dispose();
                if (!foundStringHeuristic) {
                    editHeuristicButtons[playerIndex].setVisible(false);
                }
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
        } else {
            editHeuristicButtons[playerIndex].setVisible(false);
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
            public void onEvent(Event event)
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

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        int maxParamsPerPanel = 20;
        int paramIdx = 0;

        while (paramIdx < paramNames.size()) {
            JPanel currentPanel = new JPanel();
            currentPanel.setLayout(new BoxLayout(currentPanel, BoxLayout.Y_AXIS));

            for (int i = paramIdx; i < paramIdx + maxParamsPerPanel && i < paramNames.size(); i++) {
                JPanel paramPanel = new JPanel(new BorderLayout());
                paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.X_AXIS));
                String param = paramNames.get(i);
                paramPanel.add(new JLabel("  " + param));
                List<Object> values = pp.getPossibleValues(param);
                JComboBox<Object> valueOptions = new JComboBox<>(values.toArray());
                valueOptions.setSelectedItem(pp.getParameterValue(param));
                paramValueOptions.put(param, valueOptions);
                paramPanel.add(valueOptions);

                currentPanel.add(paramPanel);
            }

            paramIdx += maxParamsPerPanel;
            wrapper.add(currentPanel);
            wrapper.add(Box.createRigidArea(new Dimension(10, 0)));
        }

        frame.getContentPane().add(wrapper);
        frame.pack();

        return paramValueOptions;
    }
}
