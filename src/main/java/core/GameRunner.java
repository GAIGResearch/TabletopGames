package core;

import evaluation.listeners.GameListener;
import evaluation.summarisers.TAGNumericStatSummary;
import games.GameType;
import gui.AbstractGUIManager;
import gui.GUI;
import gui.GamePanel;
import gui.IGUI;
import io.humble.video.*;
import io.humble.video.awt.MediaPictureConverter;
import io.humble.video.awt.MediaPictureConverterFactory;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import players.simple.RandomPlayer;
import utilities.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static utilities.Utils.componentToImage;

public class GameRunner {

    private MediaPictureConverter converter = null;
    private MediaPacket packet;
    private MediaPicture picture;
    private Encoder encoder;
    private Muxer muxer;
    private boolean recordingVideo = false;
    private Rectangle areaBounds;

    public GameRunner()
    {

    }

    /**
     * Runs one game.
     *
     * @param gameToPlay          - game to play
     * @param players             - list of players for the game
     * @param seed                - random seed for the game
     * @param randomizeParameters - if true, parameters are randomized for each run of each game (if possible).
     * @return - game instance created for the run
     */
    public Game runOne(GameType gameToPlay, String parameterConfigFile, List<AbstractPlayer> players, long seed,
                              boolean randomizeParameters, List<GameListener> listeners, ActionController ac, int turnPause) {
        // Creating game instance (null if not implemented)
        Game game;
        if (parameterConfigFile != null) {
            AbstractParameters params = ParameterFactory.createFromFile(gameToPlay, parameterConfigFile);
            game = gameToPlay.createGameInstance(players.size(), seed, params);
        } else game = gameToPlay.createGameInstance(players.size(), seed);

        if (game != null) {
            if (listeners != null)
                listeners.forEach(game::addListener);

            // Randomize parameters
            if (randomizeParameters) {
                AbstractParameters gameParameters = game.getGameState().getGameParameters();
                gameParameters.randomize();
            }

            // Reset game instance, passing the players for this game
            game.reset(players);
            game.setTurnPause(turnPause);

            if (ac != null) {
                // We spawn the GUI off in another thread

                //TODO: All this is the old system, needs fixing.

                GUI gui = new GUI();
                GamePanel gamePanel = new GamePanel();
                gui.setContentPane(gamePanel);
                AbstractGUIManager guiManager = gameToPlay.createGUIManager(gamePanel, game, ac);
                gui.init();
                // Video recording setup
                if (recordingVideo) {
                    areaBounds = new Rectangle(0, 0, gui.getWidth(), gui.getHeight());
                    setupVideoRecording(game.fileName, game.formatName, game.codecName, game.snapsPerSecond);
                }

                Timer guiUpdater = new Timer((int) game.getCoreParameters().frameSleepMS, event -> updateGUI(game, guiManager, gui));
                guiUpdater.start();

                game.run();
                guiUpdater.stop();
                // and update GUI to final game state
                updateGUI(game, guiManager, gui);

            } else {

                // Run!
                game.run();
            }

            terminateVideoRecording();
        } else {
            System.out.println("Error game: " + gameToPlay);
        }

        return game;
    }



    /**
     * Performs GUI update.
     *
     * @param guiManager - gui to update.
     */
    public void updateGUI(Game game, AbstractGUIManager guiManager, IGUI gui) {
        // synchronise on game to avoid updating GUI in middle of action being taken
        AbstractGameState gameState = game.getGameState();
        int currentPlayer = gameState.getCurrentPlayer();
        AbstractPlayer player = game.getPlayers().get(currentPlayer);
        if (guiManager != null) {
            guiManager.update(player, gameState, game.isHumanToMove());
            gui.repaint();
            videoRecordFrame(game.getGameState(), gui);
        }
    }

    public void setupVideoRecording(String filename, String formatname,
                                    String codecname, int snapsPerSecond) {
        if (recordingVideo) {
            try {
                final Rational framerate = Rational.make(1, snapsPerSecond);

                // First we create a muxer using the passed in filename and formatname if given.
                muxer = Muxer.make(filename, null, formatname);

                /* Now, we need to decide what type of codec to use to encode video. Muxers
                 * have limited sets of codecs they can use. We're going to pick the first one that
                 * works, or if the user supplied a codec name, we're going to force-fit that
                 * in instead.
                 */
                final MuxerFormat format = muxer.getFormat();
                final Codec codec;
                if (codecname != null) {
                    codec = Codec.findEncodingCodecByName(codecname);
                } else {
                    codec = Codec.findEncodingCodec(format.getDefaultVideoCodecId());
                }

                // Now that we know what codec, we need to create an encoder
                encoder = Encoder.make(codec);

                /*
                 * Video encoders need to know at a minimum:
                 *   width
                 *   height
                 *   pixel format
                 * Some also need to know frame-rate (older codecs that had a fixed rate at which video files could
                 * be written needed this). There are many other options you can set on an encoder, but we're
                 * going to keep it simpler here.
                 */
                encoder.setWidth(areaBounds.width);
                encoder.setHeight(areaBounds.height);
                // We are going to use 420P as the format because that's what most video formats these days use
                final PixelFormat.Type pixelformat = PixelFormat.Type.PIX_FMT_YUV420P;
                encoder.setPixelFormat(pixelformat);
                encoder.setTimeBase(framerate);

                /* An annoynace of some formats is that they need global (rather than per-stream) headers,
                 * and in that case you have to tell the encoder. And since Encoders are decoupled from
                 * Muxers, there is no easy way to know this beyond
                 */
                if (format.getFlag(MuxerFormat.Flag.GLOBAL_HEADER))
                    encoder.setFlag(Encoder.Flag.FLAG_GLOBAL_HEADER, true);

                // Open the encoder.
                encoder.open(null, null);
                // Add this stream to the muxer.
                muxer.addNewStream(encoder);
                // And open the muxer for business.
                muxer.open(null, null);

                /* Next, we need to make sure we have the right MediaPicture format objects
                 * to encode data with. Java (and most on-screen graphics programs) use some
                 * variant of Red-Green-Blue image encoding (a.k.a. RGB or BGR). Most video
                 * codecs use some variant of YCrCb formatting. So we're going to have to
                 * convert. To do that, we'll introduce a MediaPictureConverter object later. object.
                 */
                picture = MediaPicture.make(
                        encoder.getWidth(),
                        encoder.getHeight(),
                        pixelformat);
                picture.setTimeBase(framerate);

                /* Now begin our main loop of taking screen snaps.
                 * We're going to encode and then write out any resulting packets. */
                packet = MediaPacket.make();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void terminateVideoRecording() {
        if (recordingVideo) {
            /* Encoders, like decoders, sometimes cache pictures so it can do the right key-frame optimizations.
             * So, they need to be flushed as well. As with the decoders, the convention is to pass in a null
             * input until the output is not complete.
             */
            do {
                encoder.encode(packet, null);
                if (packet.isComplete())
                    muxer.write(packet, false);
            } while (packet.isComplete());

            // Finally, let's clean up after ourselves.
            muxer.close();
        }
    }


    private void videoRecordFrame(AbstractGameState gameState, IGUI gui) {
        if (recordingVideo) {
            // Make the screen capture && convert image to TYPE_3BYTE_BGR
            // TODO: This casting to (GUI) makes it incompatible with non-Swing interfaces.
            final BufferedImage screen = componentToImage((GUI)gui, BufferedImage.TYPE_3BYTE_BGR);

            // This is LIKELY not in YUV420P format, so we're going to convert it using some handy utilities.
            if (converter == null)
                converter = MediaPictureConverterFactory.createConverter(screen, picture);
            converter.toPicture(picture, screen, gameState.getGameTick());

            do {
                encoder.encode(packet, picture);
                if (packet.isComplete())
                    muxer.write(packet, false);
            } while (packet.isComplete());
        }
    }


    /**
     * Runs several games with a set of random seeds, one for each repetition of a game.
     *
     * @param gamesToPlay         - list of games to play.
     * @param players             - list of players for the game.
     * @param nRepetitions        - number of repetitions of each game.
     * @param seeds               - random seeds array, one for each repetition of a game.
     * @param ac                  - action controller for GUI interactions, null if playing without visuals.
     * @param randomizeParameters - if true, game parameters are randomized for each run of each game (if possible).
     */
    public void runMany(List<GameType> gamesToPlay, List<AbstractPlayer> players, int nRepetitions,
                               long[] seeds, ActionController ac, boolean randomizeParameters, List<GameListener> listeners, int turnPause) {
        int nPlayers = players.size();

        // Save win rate statistics over all games
        TAGNumericStatSummary[] overall = new TAGNumericStatSummary[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            overall[i] = new TAGNumericStatSummary("Overall Player " + i);
        }

        // For each game...
        for (GameType gt : gamesToPlay) {

            // Save win rate statistics over all repetitions of this game
            TAGNumericStatSummary[] statSummaries = new TAGNumericStatSummary[nPlayers];
            for (int i = 0; i < nPlayers; i++) {
                statSummaries[i] = new TAGNumericStatSummary("Game: " + gt.name() + "; Player: " + i);
            }

            // Play n repetitions of this game and record player results
            for (int i = 0; i < nRepetitions; i++) {
                Game game = runOne(gt, null, players, seeds[i], randomizeParameters, listeners, null, turnPause);
                if (game != null) {
                    recordPlayerResults(statSummaries, game);
                }
            }

            for (int i = 0; i < nPlayers; i++) {
                // Print statistics for this game
                System.out.println(statSummaries[i].toString());

                // Record in overall statistics
                overall[i].add(statSummaries[i]);
            }
        }

        // Print final statistics
        System.out.println("\n---------------------\n");
        for (int i = 0; i < nPlayers; i++) {
            // Print statistics for this game
            System.out.println(overall[i].toString());
        }
    }



    /**
     * Runs several games with a given random seed.
     *
     * @param gamesToPlay         - list of games to play.
     * @param players             - list of players for the game.
     * @param nRepetitions        - number of repetitions of each game.
     * @param seed                - random seed for all games. If null, a new random seed is used for each game.
     * @param randomizeParameters - if true, game parameters are randomized for each run of each game (if possible).
     * @param detailedStatistics  - if true, detailed statistics are printed, otherwise just average of wins
     */
    public void runMany(List<GameType> gamesToPlay, List<AbstractPlayer> players, Long seed,
                               int nRepetitions, boolean randomizeParameters,
                               boolean detailedStatistics, List<GameListener> listeners, int turnPause) {
        int nPlayers = players.size();

        // Save win rate statistics over all games
        TAGNumericStatSummary[] overall = new TAGNumericStatSummary[nPlayers];
        String[] agentNames = new String[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            String[] split = players.get(i).getClass().toString().split("\\.");
            String agentName = split[split.length - 1] + "-" + i;
            overall[i] = new TAGNumericStatSummary("Overall " + agentName);
            agentNames[i] = agentName;
        }

        // For each game...
        for (GameType gt : gamesToPlay) {

            // Save win rate statistics over all repetitions of this game
            TAGNumericStatSummary[] statSummaries = new TAGNumericStatSummary[nPlayers];
            for (int i = 0; i < nPlayers; i++) {
                statSummaries[i] = new TAGNumericStatSummary("{Game: " + gt.name() + "; Player: " + agentNames[i] + "}");
            }

            // Play n repetitions of this game and record player results
            Game game = null;
            int offset = 0;
            for (int i = 0; i < nRepetitions; i++) {
                Long s = seed;
                if (s == null) s = System.currentTimeMillis();
                s += offset;
                game = runOne(gt, null, players, s, randomizeParameters, listeners, null, turnPause);
                if (game != null) {
                    recordPlayerResults(statSummaries, game);
                    offset = game.getGameState().getTurnOrder().getRoundCounter() * game.getGameState().getNPlayers();
                } else {
                    break;
                }
                // System.out.println("Game " + i + "/" + nRepetitions);
            }

            if (game != null) {
                System.out.println("---------------------");
                for (int i = 0; i < nPlayers; i++) {
                    // Print statistics for this game
                    if (detailedStatistics) {
                        System.out.println(statSummaries[i].toString());
                    } else {
                        System.out.println(statSummaries[i].name + ": " + statSummaries[i].mean() + " (n=" + statSummaries[i].n() + ")");
                    }

                    // Record in overall statistics
                    overall[i].add(statSummaries[i]);
                }
            }
        }

        // Print final statistics
        System.out.println("\n=====================\n");
        for (int i = 0; i < nPlayers; i++) {
            // Print statistics for this game
            if (detailedStatistics) {
                System.out.println(overall[i].toString());
            } else {
                System.out.println(overall[i].name + ": " + overall[i].mean());
            }
        }
    }



    /**
     * Records statistics of given game into the given StatSummary objects. Only WIN, LOSE or DRAW are valid results
     * recorded.
     *
     * @param statSummaries - object recording statistics
     * @param game          - finished game
     */
    public static void recordPlayerResults(TAGNumericStatSummary[] statSummaries, Game game) {
        int nPlayers = statSummaries.length;
        CoreConstants.GameResult[] results = game.getGameState().getPlayerResults();
        for (int p = 0; p < nPlayers; p++) {
            if (results[p] == CoreConstants.GameResult.WIN || results[p] == CoreConstants.GameResult.LOSE || results[p] == CoreConstants.GameResult.DRAW) {
                statSummaries[p].add(results[p].value);
            }
        }
    }

    /**
     * The recommended way to run a game is via evaluations.Frontend, however that may not work on
     * some games for some screen sizes due to the vagaries of Java Swing...
     * <p>
     * Test class used to run a specific game. The user must specify:
     * 1. Action controller for GUI interactions / null for no visuals
     * 2. Random seed for the game
     * 3. Players for the game
     * 4. Game parameter configuration
     * 5. Mode of running
     * and then run this class.
     */
    public static void main(String[] args) {
        String gameType = Utils.getArg(args, "game", "SushiGo");
        boolean useGUI = Utils.getArg(args, "gui", true);
        int playerCount = Utils.getArg(args, "nPlayers", 2);
        int turnPause = Utils.getArg(args, "turnPause", 0);
        long seed = Utils.getArg(args, "seed", System.currentTimeMillis());

        ActionController ac = new ActionController(); //null;

        /* Set up players for the game */
        ArrayList<AbstractPlayer> players = new ArrayList<>(playerCount);

        players.add(new RandomPlayer());
//        players.add(new RandomPlayer());
//        players.add(new MCTSPlayer());
//        MCTSParams params1 = new MCTSParams();
//        players.add(new MCTSPlayer(params1));
//        players.add(new OSLAPlayer());
//        players.add(new RMHCPlayer());
        players.add(new HumanGUIPlayer(ac));
//        players.add(new HumanConsolePlayer());
//        players.add(new FirstActionPlayer());
//        players.add(new HumanConsolePlayer());

        /* 4. Game parameter configuration. Set to null to ignore and use default parameters */
        String gameParams = null;

        /* 5. Run! */
        GameRunner gr = new GameRunner();
        gr.runOne(GameType.valueOf(gameType), gameParams, players, seed, false, null, useGUI ? ac : null, turnPause);

//        ArrayList<GameType> games = new ArrayList<>(Arrays.asList(GameType.values()));
//        games.remove(LoveLetter);
//        games.remove(Pandemic);
//        games.remove(TicTacToe);
//        GameRunner gr = new GameRunner();
//        gr.runMany(games, players, 100L, 100, false, false, null, turnPause);
//        gr.runMany(new ArrayList<GameType>() {{add(Uno);}}, players, 100L, 100, false, false, null, turnPause);
    }

}
