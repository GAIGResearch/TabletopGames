package gui;

import core.AbstractPlayer;
import core.GUI;
import core.Game;
import games.GameType;
import players.PlayerType;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Frontend extends GUI {
    private final int nMaxPlayers = 20;
    private final int defaultNPlayers = 2;

    public Frontend() {

        JPanel gameSelect = new JPanel();
        gameSelect.add(new JLabel("Game type:"));
        String[] gameNames = new String[GameType.values().length];
        for (int i = 0; i < gameNames.length; i++) {
            gameNames[i] = GameType.values()[i].name();
        }
        JComboBox<String> gameOptions = new JComboBox<>(gameNames);  // index of this selection is game
        gameSelect.add(gameOptions);

        JPanel playerSelect = new JPanel();
        playerSelect.setLayout(new BoxLayout(playerSelect, BoxLayout.Y_AXIS));
        JPanel nPlayers = new JPanel();
        playerSelect.add(nPlayers);
        nPlayers.add(new JLabel("# players (max " + nMaxPlayers + "):"));
        JTextField nPlayerField = new JTextField(""+defaultNPlayers, 10);  // integer of this is n players
        nPlayers.add(nPlayerField);

        JPanel[] playerOptions = new JPanel[nMaxPlayers];
        JComboBox<String>[] playerOptionsChoice = new JComboBox[nMaxPlayers];  // player is index of this selection
        String[] playerOptionsString = new String[PlayerType.values().length];
        for (int i = 0; i < playerOptionsString.length; i++) {
            playerOptionsString[i] = PlayerType.values()[i].name();
        }
        for (int i = 0; i < nMaxPlayers; i++) {
            playerOptions[i] = new JPanel();
            if (i >= defaultNPlayers) {
                playerOptions[i].setVisible(false);
            }
            playerOptions[i].add(new JLabel("Player " + i + ":"));
            playerOptionsChoice[i] = new JComboBox<>(playerOptionsString);
            playerOptions[i].add(playerOptionsChoice[i]);
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
        
        JPanel visualSelect = new JPanel();
        visualSelect.add(new JLabel("Visuals ON/OFF:"));
        JComboBox<Boolean> visualOptions = new JComboBox<>(new Boolean[]{false, true}); // index here is visuals on/off
        visualSelect.add(visualOptions);

        JPanel seedSelect = new JPanel();
        seedSelect.add(new JLabel("Seed:"));
        JTextField seedOption = new JTextField(""+System.currentTimeMillis());  // integer of this is seed
        JButton seedRefresh = new JButton("Refresh");
        seedRefresh.addActionListener(e -> seedOption.setText("" + System.currentTimeMillis()));
        seedSelect.add(seedOption);
        seedSelect.add(seedRefresh);

        JPanel gameOptionFullPanel = new JPanel();
        gameOptionFullPanel.setLayout(new BoxLayout(gameOptionFullPanel, BoxLayout.Y_AXIS));
        gameOptionFullPanel.add(gameSelect);
        gameOptionFullPanel.add(playerSelect);
        gameOptionFullPanel.add(visualSelect);
        gameOptionFullPanel.add(seedSelect);

        JPanel gamePanel = new JPanel();
        gamePanel.setVisible(false);

        JPanel gameControlButtons = new JPanel();
        JButton startGame = new JButton("Play!");
        startGame.addActionListener(e -> {

            GUI frame = this;
            Runnable runnable = () -> {

                ActionController ac = new ActionController();
                if (visualOptions.getSelectedIndex() == 0) ac = null;
                long seed = Long.parseLong(seedOption.getText());
                ArrayList<AbstractPlayer> players = new ArrayList<>();
                int nP = Integer.parseInt(nPlayerField.getText());
                // todo parameter choice for each player
                String[] playerNames = new String[nP];
                for (int i = 0; i < nP; i++) {
                    AbstractPlayer player = PlayerType.valueOf(playerOptionsChoice[i].getItemAt(playerOptionsChoice[i].getSelectedIndex())).createPlayerInstance(seed, ac, null);
                    playerNames[i] = player.toString();
                    players.add(player);
                }
                GameType gameType = GameType.valueOf(gameOptions.getItemAt(gameOptions.getSelectedIndex()));
                System.out.println("Playing `" + gameType.name() + "` with players: " + Arrays.toString(playerNames));

                gamePanel.removeAll();
                Game game = gameType.createGameInstance(players.size(), seed);
                if (game != null) {
                    // Reset game instance, passing the players for this game
                    game.reset(players);

                    AbstractGUIManager gui = null;
                    if (ac != null) {
                        // Create GUI (null if not implemented; running without visuals)
                        gui = gameType.createGUIManager(gamePanel, game, ac);
                    }
                    revalidate();
                    pack();
                    game.run(gui, frame);
                    System.out.println("Game over: " + Arrays.toString(game.getGameState().getPlayerResults()));

                }
            };

            Thread thread = new Thread(runnable);
            thread.start();
        });
        gameControlButtons.add(startGame);
        // todo stop game
        // todo tournaments, game report, player report etc

        gameOptionFullPanel.add(gameControlButtons);
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

}
