package games.diamant;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Counter;
import core.components.Deck;
import core.interfaces.ITreeActionSpace;
import games.diamant.actions.ContinueInCave;
import games.diamant.actions.ExitFromCave;
import games.diamant.actions.OutOfCave;
import games.diamant.cards.DiamantCard;
import games.diamant.components.ActionsPlayed;
import utilities.ActionTreeNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;

public class DiamantForwardModel extends StandardForwardModel implements ITreeActionSpace {
    @Override
    protected void _setup(AbstractGameState firstState) {
        DiamantGameState dgs = (DiamantGameState) firstState;
        dgs._reset();

        for (int i = 0; i < dgs.getNPlayers(); i++) {
            String counter_hand_name = "CounterHand" + i;
            String counter_chest_name = "CounterChest" + i;
            dgs.hands.add(new Counter(0, 0, 1000, counter_hand_name));
            dgs.treasureChests.add(new Counter(0, 0, 1000, counter_chest_name));
            dgs.playerInCave.add(true);
        }

        dgs.mainDeck = new Deck<>("MainDeck", HIDDEN_TO_ALL);
        dgs.discardDeck = new Deck<>("DiscardDeck", VISIBLE_TO_ALL);
        dgs.path = new Deck<>("Path", VISIBLE_TO_ALL);
        dgs.actionsPlayed = new ActionsPlayed();
        dgs.recordOfPlayerActions = new ArrayList<>();

        createCards(dgs);
        dgs.mainDeck.shuffle(dgs.getRnd());

        // Draw first card and play it
        drawAndPlayCard(dgs);

        dgs.setFirstPlayer(0);
    }

    /**
     * Create all the cards and include them into the main deck.
     *
     * @param dgs - current game state.
     */
    private void createCards(DiamantGameState dgs) {
        DiamantParameters dp = (DiamantParameters) dgs.getGameParameters();

        // 3 of each hazard
        // 15 treasures :1,2,3,4,5,7,9,10,11,12,13,14,15,16,17

        // Add artifacts
        //for (int i=0; i< dp.nArtifactCards; i++)
        //    dgs.mainDeck.add(new DiamantCard(DiamantCard.DiamantCardType.Artifact, DiamantCard.HazardType.None, 0));

        // Add hazards
        for (int i = 0; i < dp.nHazardCardsPerType; i++) {
            for (DiamantCard.HazardType h : DiamantCard.HazardType.values())
                if (h != DiamantCard.HazardType.None)
                    dgs.mainDeck.add(new DiamantCard(DiamantCard.DiamantCardType.Hazard, h, 0));
        }

        // Add treasures
        for (int t : dp.treasures)
            dgs.mainDeck.add(new DiamantCard(DiamantCard.DiamantCardType.Treasure, DiamantCard.HazardType.None, t));
    }

    /**
     * In this game, all players play the action at the same time.
     * When an agent call next, the action is just stored in the gameState.
     *
     * @param currentState: current state of the game
     * @param action:       action to be executed
     */
    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        DiamantGameState dgs = (DiamantGameState) currentState;
        // If all players have an action, execute them
        if (dgs.actionsPlayed.size() == dgs.getNPlayers()) {
            playActions(dgs);
            dgs.actionsPlayed.clear();
        }
        if (dgs.isNotTerminal())
            endPlayerTurn(dgs);
    }


    public void playActions(DiamantGameState dgs) {
        // How many players play ExitFromCave?
        int nPlayersExit = 0;
        for (int p : dgs.actionsPlayed.keySet())
            if (dgs.actionsPlayed.get(p) instanceof ExitFromCave) {
                nPlayersExit += 1;
                dgs.recordOfPlayerActions.add(new DiamantGameState.PlayerTurnRecord(p, dgs.nCave, dgs.discardDeck.getSize()));
            }


        if (nPlayersExit == dgs.getNPlayersInCave()) {
            // All active players left the cave
            distributeGemsAmongPlayers(dgs, nPlayersExit);
            dgs.nGemsOnPath = 0;
            prepareNewCave(dgs);
        } else {
            if (nPlayersExit > 0) {
                // Not all Continue
                distributeGemsAmongPlayers(dgs, nPlayersExit);
            }

            drawAndPlayCard(dgs);
        }

    }

    private void distributeGemsAmongPlayers(DiamantGameState dgs, int nPlayersExit) {
        int gems_to_players;
        if (nPlayersExit == 1) {
            gems_to_players = dgs.nGemsOnPath;
            dgs.nGemsOnPath = 0;
        } else {
            gems_to_players = (int) Math.floor(dgs.nGemsOnPath / (double) nPlayersExit);
            dgs.nGemsOnPath = dgs.nGemsOnPath % nPlayersExit;
        }

        for (int p = 0; p < dgs.getNPlayers(); p++) {
            if (dgs.actionsPlayed.get(p) instanceof ExitFromCave) {
                dgs.hands.get(p).increment(gems_to_players);                             // increment hand gems
                dgs.treasureChests.get(p).increment(dgs.hands.get(p).getValue());   // hand gems to chest
                dgs.hands.get(p).setValue(0);                                 // hand gems <- 0
                dgs.playerInCave.set(p, false);                                       // Set to not in Cave
            }
        }
    }

    /**
     * Prepare the game for playing a new Cave
     *
     * @param dgs: current game state
     */
    private void prepareNewCave(DiamantGameState dgs) {
        DiamantParameters dp = (DiamantParameters) dgs.getGameParameters();

        dgs.nCave++;

        // No more caves ?
        if (dgs.nCave == dp.nCaves)
            endGame(dgs);
        else {
            // Move path cards to maindeck and shuffle
            dgs.mainDeck.add(dgs.path);
            dgs.path.clear();
            dgs.mainDeck.shuffle(dgs.getRnd());

            // Initialize game state
            dgs.nHazardExplosionsOnPath = 0;
            dgs.nHazardPoissonGasOnPath = 0;
            dgs.nHazardRockfallsOnPath = 0;
            dgs.nHazardScorpionsOnPath = 0;
            dgs.nHazardSnakesOnPath = 0;

            // All the player will participate in next cave
            for (int p = 0; p < dgs.getNPlayers(); p++)
                dgs.playerInCave.set(p, true);

            drawAndPlayCard(dgs);
        }
    }


    /**
     * Gets the possible actions to be played
     * If the player is not in the cave, only OutOfCave action can be played
     * If the player is in the cave, there are only two actions: ExitFromCave, ContinueInCave
     *
     * @param gameState: current game state
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        DiamantGameState dgs = (DiamantGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();

        // If the player is still in the cave
        if (dgs.playerInCave.get(gameState.getCurrentPlayer())) {
            actions.add(new ContinueInCave());
            actions.add(new ExitFromCave());
        } else
            actions.add(new OutOfCave());

        return actions;
    }

    /**
     * Play the card
     *
     * @param dgs: current game state
     */
    private void drawAndPlayCard(DiamantGameState dgs) {
        DiamantCard card = dgs.mainDeck.draw();
        dgs.path.add(card);

        if (card.getCardType() == DiamantCard.DiamantCardType.Treasure) {
            int gems_to_players = (int) Math.floor(card.getNumberOfGems() / (double) dgs.getNPlayersInCave());
            int gems_to_path = card.getNumberOfGems() % dgs.getNPlayersInCave();

            for (int p = 0; p < dgs.getNPlayers(); p++)
                if (dgs.playerInCave.get(p))
                    dgs.hands.get(p).increment(gems_to_players);

            dgs.nGemsOnPath += gems_to_path;
        } else if (card.getCardType() == DiamantCard.DiamantCardType.Hazard) {
            if (card.getHazardType() == DiamantCard.HazardType.Explosions) dgs.nHazardExplosionsOnPath += 1;
            else if (card.getHazardType() == DiamantCard.HazardType.PoissonGas) dgs.nHazardPoissonGasOnPath += 1;
            else if (card.getHazardType() == DiamantCard.HazardType.Rockfalls) dgs.nHazardRockfallsOnPath += 1;
            else if (card.getHazardType() == DiamantCard.HazardType.Scorpions) dgs.nHazardScorpionsOnPath += 1;
            else if (card.getHazardType() == DiamantCard.HazardType.Snakes) dgs.nHazardSnakesOnPath += 1;

            DiamantParameters dp = (DiamantParameters) dgs.getGameParameters();
            // If there are two hazards cards of the same type -> finish the cave
            if (dgs.nHazardSnakesOnPath == dp.nHazardsToDead ||
                    dgs.nHazardScorpionsOnPath == dp.nHazardsToDead ||
                    dgs.nHazardRockfallsOnPath == dp.nHazardsToDead ||
                    dgs.nHazardPoissonGasOnPath == dp.nHazardsToDead ||
                    dgs.nHazardExplosionsOnPath == dp.nHazardsToDead) {
                // All active players loose all gems on hand.
                for (int p = 0; p < dgs.getNPlayers(); p++) {
                    if (dgs.playerInCave.get(p)) {
                        dgs.hands.get(p).setValue(0);
                        dgs.recordOfPlayerActions.add(new DiamantGameState.PlayerTurnRecord(p, dgs.nCave, -1));
                    }
                }
                // Gems on Path are also loosed
                dgs.nGemsOnPath = 0;

                // Remove last card (it is the hazard one) from path and add to discardDeck
                dgs.path.draw();
                dgs.discardDeck.add(card);

                // Start new cave
                prepareNewCave(dgs);
            }
        }
    }

    @Override
    public ActionTreeNode initActionTree(AbstractGameState gameState) {
        ActionTreeNode tree = new ActionTreeNode(0, "root");
        tree.addChild(0, "continue");
        tree.addChild(0, "exit");
        tree.addChild(0, "out"); // dummy action for staying in cave
        return tree;
    }

    @Override
    public ActionTreeNode updateActionTree(ActionTreeNode root, AbstractGameState gameState) {
        DiamantGameState dgs = (DiamantGameState) gameState;
        root.resetTree();
        if (dgs.playerInCave.get(gameState.getCurrentPlayer())) {
            root.findChildrenByName("continue").setAction(new ContinueInCave());
            root.findChildrenByName("exit").setAction(new ExitFromCave());
        } else {
            root.findChildrenByName("out").setAction(new OutOfCave());
        }
        return root;
    }
}
