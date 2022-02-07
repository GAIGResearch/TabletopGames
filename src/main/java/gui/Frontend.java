package gui;

import core.*;
import core.actions.AbstractAction;
import core.interfaces.IGameListener;
import evaluation.TunableParameters;
import games.GameType;
import players.PlayerParameters;
import players.PlayerType;
import players.human.ActionController;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Frontend extends GUI {
    private final int nMaxPlayers = 20;
    private final int defaultNPlayers = 2;
    private final int actionsToSample = 20;
    private final Map<core.actions.AbstractAction, Long> sampledActionsForNextDecision = new HashMap<>();
    Timer guiUpdater;
    private Thread gameThread;
    private Game gameRunning;
    private boolean showAll, paused, started;
    private ActionController humanInputQueue;

    public Frontend() {

        // Number of players selection

        JPanel playerSelect = new JPanel();
        playerSelect.setLayout(new BoxLayout(playerSelect, BoxLayout.Y_AXIS));
        JPanel nPlayers = new JPanel();
        playerSelect.add(nPlayers);
        JLabel nPlayersText = new JLabel("# players (max " + nMaxPlayers + "):");
        nPlayers.add(nPlayersText);
        JTextField nPlayerField = new JTextField("" + defaultNPlayers, 10);  // integer of this is n players
        nPlayers.add(nPlayerField);

        // Game type and parameters selection

        JPanel gameSelect = new JPanel();
        gameSelect.add(new JLabel("Game type:"));
        String[] gameNames = new String[GameType.values().length];
        TunableParameters[] gameParameters = new TunableParameters[GameType.values().length];
        JFrame[] gameParameterEditWindow = new JFrame[GameType.values().length];
        for (int i = 0; i < gameNames.length; i++) {
            gameNames[i] = GameType.values()[i].name();
            AbstractParameters params = ParameterFactory.getDefaultParams(GameType.values()[i], 0);
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
        nPlayersText.setText("# players (min " + firstGameType.getMinPlayers() + ", max " + firstGameType.getMaxPlayers() + "):");

        JComboBox<String> gameOptions = new JComboBox<>(gameNames);  // index of this selection is game
        gameSelect.add(gameOptions);
        JButton gameParameterEdit = new JButton("Edit");
        gameParameterEdit.setVisible(false);
        gameOptions.addActionListener(e -> {
            int idx = gameOptions.getSelectedIndex();
            gameParameterEdit.setVisible(gameParameterEditWindow[idx] != null);

            GameType gameType = GameType.valueOf((String) gameOptions.getSelectedItem());
            nPlayersText.setText("# players (min " + gameType.getMinPlayers() + ", max " + gameType.getMaxPlayers() + "):");
            pack();
        });
        gameSelect.add(gameParameterEdit);
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
        PlayerParameters[] playerParameters = new PlayerParameters[PlayerType.values().length];
        JFrame[] playerParameterEditWindow = new JFrame[PlayerType.values().length];
        for (int i = 0; i < playerOptionsString.length; i++) {
            playerOptionsString[i] = PlayerType.values()[i].name();
            playerParameters[i] = PlayerType.values()[i].createParameterSet(0);
            playerParameterEditWindow[i] = new JFrame();
            playerParameterEditWindow[i].getContentPane().setLayout(new BoxLayout(playerParameterEditWindow[i].getContentPane(), BoxLayout.Y_AXIS));

            if (playerParameters[i] != null) {
                List<String> paramNames = playerParameters[i].getParameterNames();
                HashMap<String, JComboBox<Object>> paramValueOptions = createParameterWindow(paramNames, playerParameters[i], playerParameterEditWindow[i]);

                int idx = i;
                JButton submit = new JButton("Submit");
                submit.addActionListener(e -> {
                    for (String param : paramNames) {
                        playerParameters[idx].setParameterValue(param, paramValueOptions.get(param).getSelectedItem());
                    }
                    playerParameterEditWindow[idx].dispose();
                });
                JButton reset = new JButton("Reset");
                reset.addActionListener(e -> {
                    playerParameters[idx].reset();
                    for (String param : paramNames) {
                        paramValueOptions.get(param).setSelectedItem(playerParameters[idx].getDefaultParameterValue(param));
                    }
                });
                JPanel buttons = new JPanel();
                buttons.add(submit);
                buttons.add(reset);

                playerParameterEditWindow[i].getContentPane().add(buttons);
            }
        }
        for (int i = 0; i < nMaxPlayers; i++) {
            playerOptions[i] = new JPanel();
            if (i >= defaultNPlayers) {
                playerOptions[i].setVisible(false);
            }
            playerOptions[i].add(new JLabel("Player " + i + ":"));
            JButton paramButton = new JButton("Edit");
            paramButton.setVisible(false);
            playerOptionsChoice[i] = new JComboBox<>(playerOptionsString);
            playerOptionsChoice[i].setSelectedItem("Random");
            int playerIdx = i;
            playerOptionsChoice[i].addActionListener(e -> {
                int idx = playerOptionsChoice[playerIdx].getSelectedIndex();
                PlayerParameters pp = playerParameters[idx];
                paramButton.setVisible(pp != null);
                paramButton.removeAll();
                paramButton.addActionListener(f -> {
                    playerParameterEditWindow[idx].setTitle("Edit parameters " + playerOptionsChoice[playerIdx].getSelectedItem());
                    playerParameterEditWindow[idx].pack();
                    playerParameterEditWindow[idx].setVisible(true);
                    playerParameterEditWindow[idx].setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                });
                pack();
            });
            playerOptions[i].add(playerOptionsChoice[i]);
            playerOptions[i].add(paramButton);
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
        nPlayers.add(updateNPlayers);

        // Select visuals on/off

        JPanel visualSelect = new JPanel();
        visualSelect.add(new JLabel("Visuals ON/OFF:"));
        JComboBox<Boolean> visualOptions = new JComboBox<>(new Boolean[]{false, true}); // index here is visuals on/off
        visualOptions.setSelectedItem(true);
        visualSelect.add(visualOptions);

        // Select random seed

        JPanel seedSelect = new JPanel();
        seedSelect.add(new JLabel("Seed:"));
        JTextField seedOption = new JTextField("" + System.currentTimeMillis());  // integer of this is seed
        JButton seedRefresh = new JButton("Refresh");
        seedRefresh.addActionListener(e -> seedOption.setText("" + System.currentTimeMillis()));
        seedSelect.add(seedOption);
        seedSelect.add(seedRefresh);

        // Game run core parameters select
        CoreParameters coreParameters = new CoreParameters();
        JPanel gameRunParamSelect = new JPanel();
        gameRunParamSelect.setLayout(new BoxLayout(gameRunParamSelect, BoxLayout.Y_AXIS));
        HashMap<String, JComboBox<Object>> coreParameterValueOptions = new HashMap<>();
        for (String param : coreParameters.getParameterNames()) {
            JPanel paramPanel = new JPanel();
            paramPanel.add(new JLabel(param));
            List<Object> values = coreParameters.getPossibleValues(param);
            JComboBox<Object> valueOptions = new JComboBox<>(values.toArray());
            valueOptions.setSelectedItem(coreParameters.getDefaultParameterValue(param));
            coreParameterValueOptions.put(param, valueOptions);
            paramPanel.add(valueOptions);
            gameRunParamSelect.add(paramPanel);
        }

        // Empty panel to hold game when play button is pressed

        GamePanel gamePanel = new GamePanel();
        gamePanel.setVisible(false);

        JPanel gameControlButtons = new JPanel();
        // Pause game button
        JButton oneAction = new JButton("Next Action");
        oneAction.setEnabled(paused && started);

        JButton pauseGame = new JButton("Pause");
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

                    // Find core parameters
                    for (String param : coreParameterValueOptions.keySet()) {
                        coreParameters.setParameterValue(param, coreParameterValueOptions.get(param).getSelectedItem());
                    }
                    gameRunning.setCoreParameters(coreParameters);

                    AbstractGUIManager gui = (humanInputQueue != null) ? gameType.createGUIManager(gamePanel, gameRunning, humanInputQueue) : null;
                    // revalidate();
                    // pack();
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
                System.out.printf("Invoking oneAction from FrontEnd for player %d%n", gameRunning.getGameState().getCurrentPlayer());
                synchronized (gameRunning) {
                    gameRunning.oneAction();
                    gameRunning.notifyAll();
                }
            }
        });

        JButton allActions = new JButton("Show All!");
        allActions.addActionListener(e -> {
            showAll = !showAll;
            allActions.setText(showAll ? "Show Self" : "Show All");
        });

        gameControlButtons.add(startGame);
        gameControlButtons.add(pauseGame);
        gameControlButtons.add(oneAction);
        gameControlButtons.add(allActions);

        // todo tournaments, game report, player report etc

        // Put all together

        JPanel gameOptionFullPanel = new JPanel();
        gameOptionFullPanel.setLayout(new BoxLayout(gameOptionFullPanel, BoxLayout.Y_AXIS));
        gameOptionFullPanel.add(gameSelect);
        gameOptionFullPanel.add(playerSelect);
        gameOptionFullPanel.add(visualSelect);
        gameOptionFullPanel.add(seedSelect);
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

        // Frame properties
        setFrameProperties();
    }

    public static void main(String[] args) {
        new Frontend();
    }

    private void listenForDecisions() {
        // add a listener to detect every time an action has been taken
        gameRunning.addListener(new IGameListener() {
            @Override
            public void onGameEvent(CoreConstants.GameEvents type, Game game) {
                // Do nothing
            }

            @Override
            public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
                if (type == CoreConstants.GameEvents.ACTION_TAKEN) {
                    updateSampleActions(state);
                }
            }
        });
        // and then do this at the start of the game
        updateSampleActions(gameRunning.getGameState());
    }

    private void updateSampleActions(AbstractGameState state) {
        if (showAll && state.isNotTerminal() && !gameRunning.isHumanToMove()) {
            int nextPlayerID = state.getCurrentPlayer();
            AbstractPlayer nextPlayer = gameRunning.getPlayers().get(nextPlayerID);
            sampledActionsForNextDecision.clear();
            Map<core.actions.AbstractAction, Long> temp = IntStream.range(0, actionsToSample).mapToObj(i -> {
                AbstractGameState copyState = state.copy(nextPlayerID);
                List<core.actions.AbstractAction> availableActions = gameRunning.getForwardModel().computeAvailableActions(copyState);
                return nextPlayer.getAction(copyState, availableActions);
            }).collect(Collectors.groupingBy(action -> action, Collectors.counting()));
            sampledActionsForNextDecision.putAll(temp);
        }
    }


    /**
     * Performs GUI update.
     *
     * @param gui - gui to update.
     */
    private void updateGUI(AbstractGUIManager gui, JFrame frame) {
        // synchronise on game to avoid updating GUI in middle of action being taken
        synchronized (gameRunning) {
            AbstractGameState gameState = gameRunning.getGameState();
            int currentPlayer = gameState.getCurrentPlayer();
            AbstractPlayer player = gameRunning.getPlayers().get(currentPlayer);
            if (gui != null) {
                gui.update(player, gameState, gameRunning.isHumanToMove() || showAll, sampledActionsForNextDecision);
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
                frame.repaint();
            }
            gameRunning.notifyAll();

        }
    }

    private HashMap<String, JComboBox<Object>> createParameterWindow(List<String> paramNames, TunableParameters pp, JFrame frame) {
        HashMap<String, JComboBox<Object>> paramValueOptions = new HashMap<>();
        frame.getContentPane().removeAll();
        for (String param : paramNames) {
            JPanel paramPanel = new JPanel();
            paramPanel.add(new JLabel(param));
            List<Object> values = pp.getPossibleValues(param);
            JComboBox<Object> valueOptions = new JComboBox<>(values.toArray());
            valueOptions.setSelectedItem(pp.getDefaultParameterValue(param));
            paramValueOptions.put(param, valueOptions);
            paramPanel.add(valueOptions);
            frame.getContentPane().add(paramPanel);
        }
        return paramValueOptions;
    }

}
