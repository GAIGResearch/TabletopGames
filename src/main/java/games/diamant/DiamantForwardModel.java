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
import games.diamant.cards.DiamantCard.HazardType;
import games.diamant.components.ActionsPlayed;
import utilities.ActionTreeNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static java.util.stream.Collectors.*;

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

        // Relic deck initialization if relicVariant is true
        DiamantParameters dp = (DiamantParameters) dgs.getGameParameters();
        if (dp.relicVariant) {
            dgs.relicDeck = new Deck<>("RelicDeck", VISIBLE_TO_ALL);
        }

        createCards(dgs);

        // If relic variant, add top relic to main deck before shuffling
        if (dp.relicVariant && dgs.relicDeck != null && dgs.relicDeck.getSize() > 0) {
            dgs.mainDeck.add(dgs.relicDeck.draw());
        }

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
            for (HazardType h : HazardType.values())
                if (h != HazardType.None)
                    dgs.mainDeck.add(new DiamantCard(DiamantCard.DiamantCardType.Hazard, h, 0));
        }

        // Add treasures
        for (int t : dp.treasures)
            dgs.mainDeck.add(new DiamantCard(DiamantCard.DiamantCardType.Treasure, HazardType.None, t));

        // Add relics if relicVariant is enabled (we add these in reverse order so
        // that the first relic drawn is the last one added
        if (dp.relicVariant && dgs.relicDeck != null) {
            for (int i = dp.relics.length - 1; i >= 0; i--) {
                DiamantCard relicCard = new DiamantCard(DiamantCard.DiamantCardType.Relic, HazardType.None, dp.relics[i]);
                dgs.relicDeck.add(relicCard); // rest in relic deck
            }
        }
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
        int lastExitingPlayer = -1;
        for (int p : dgs.actionsPlayed.keySet())
            if (dgs.actionsPlayed.get(p) instanceof ExitFromCave) {
                nPlayersExit += 1;
                lastExitingPlayer = p;
                dgs.recordOfPlayerActions.add(new DiamantGameState.PlayerTurnRecord(p, dgs.nCave, dgs.discardDeck.getSize()));
            }

        DiamantParameters params = (DiamantParameters) dgs.getGameParameters();

        // If exactly one player leaves, and relicVariant is enabled, they pick up all relics on the path
        if (params.relicVariant && nPlayersExit == 1) {
            int relicValue = removeRelicsFromPath(dgs);
            dgs.treasureChests.get(lastExitingPlayer).increment(relicValue);
        }

        if (nPlayersExit == dgs.getNPlayersInCave()) {
            // All active players left the cave
            distributeGemsAmongPlayers(dgs, nPlayersExit);
            dgs.gemsOnPath.clear();
            prepareNewCave(dgs);
        } else {
            if (nPlayersExit > 0) {
                // Not all Continue
                distributeGemsAmongPlayers(dgs, nPlayersExit);
            }

            drawAndPlayCard(dgs);
        }

    }

    private int removeRelicsFromPath(DiamantGameState dgs) {
        List<DiamantCard> relicsOnPath = dgs.path.stream()
                .filter(c -> c.getCardType() == DiamantCard.DiamantCardType.Relic)
                .toList();
        int relicValue = relicsOnPath.stream().mapToInt(DiamantCard::getValue).sum();
        dgs.path.removeAll(relicsOnPath);
        dgs.discardDeck.add(relicsOnPath); // Add relics to discard deck
        return relicValue;
    }

    private void distributeGemsAmongPlayers(DiamantGameState dgs, int nPlayersExit) {
        int nPlayers = dgs.getNPlayers();
        int gemsCollected = 0;

        // Divide up gems per space
        for (int i = 0; i < dgs.gemsOnPath.size(); i++) {
            int gems = dgs.gemsOnPath.get(i);
            int gemsPerPlayer = nPlayersExit > 0 ? gems / nPlayersExit : 0;
            int gemsLeft = nPlayersExit > 0 ? gems % nPlayersExit : gems;

            gemsCollected += gemsPerPlayer;
            // Update gems left on this path card
            dgs.gemsOnPath.set(i, gemsLeft);
        }

        // Give collected gems to players, move hand to chest, set not in cave
        int actualExiting = 0;
        for (int p = 0; p < nPlayers; p++) {
            if (dgs.actionsPlayed.get(p) instanceof ExitFromCave) {
                actualExiting++;
                dgs.hands.get(p).increment(gemsCollected); // increment hand gems
                dgs.treasureChests.get(p).increment(dgs.hands.get(p).getValue()); // hand gems to chest
                dgs.hands.get(p).setValue(0); // hand gems <- 0
                dgs.playerInCave.set(p, false); // Set to not in Cave
            }
        }
        if (actualExiting != nPlayersExit) {
            throw new AssertionError("Number of players exiting does not match nPlayersExit parameter.");
        }
    }

    /**
     * Prepare the game for playing a new Cave
     *
     * @param dgs: current game state
     */
    private void prepareNewCave(DiamantGameState dgs) {
        DiamantParameters dp = (DiamantParameters) dgs.getGameParameters();

        endRound(dgs);

        dgs.nCave++;

        // No more caves ?
        if (dgs.nCave == dp.nCaves)
            endGame(dgs);
        else {

            if (dp.relicVariant) {
                // remove any untaken relics from the path and add to discard
                removeRelicsFromPath(dgs);
                // add new relic to main deck
                dgs.mainDeck.add(dgs.relicDeck.draw());
            }

            // Move path cards to maindeck and shuffle
            dgs.mainDeck.add(dgs.path);
            dgs.path.clear();

            dgs.mainDeck.shuffle(dgs.getRnd());

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

        // Add gems for this card to gemsOnPath (0 if not treasure)
        if (card.getCardType() == DiamantCard.DiamantCardType.Treasure) {
            int nInCave = dgs.getNPlayersInCave();
            int gems_to_players = nInCave > 0 ? (int) Math.floor(card.getValue() / (double) nInCave) : 0;
            int gems_to_path = nInCave > 0 ? card.getValue() % nInCave : card.getValue();

            for (int p = 0; p < dgs.getNPlayers(); p++)
                if (dgs.playerInCave.get(p))
                    dgs.hands.get(p).increment(gems_to_players);

            dgs.gemsOnPath.add(gems_to_path);
        } else if (card.getCardType() == DiamantCard.DiamantCardType.Hazard) {
            dgs.gemsOnPath.add(0);

            DiamantParameters dp = (DiamantParameters) dgs.getGameParameters();
            // If there are two hazards cards of the same type -> finish the cave
            Map<HazardType, Long> hazardCount = dgs.path.stream()
                    .filter(c -> c.getCardType() == DiamantCard.DiamantCardType.Hazard)
                    .map(DiamantCard::getHazardType)
                    .collect(groupingBy(h -> h, counting()));
            if (hazardCount.getOrDefault(card.getHazardType(), 0L) >= dp.nHazardsToDead) {
                // Hazard card is the second of its type, cave ends
                // All active players lose all gems on hand.
                for (int p = 0; p < dgs.getNPlayers(); p++) {
                    if (dgs.playerInCave.get(p)) {
                        dgs.hands.get(p).setValue(0);
                        dgs.recordOfPlayerActions.add(new DiamantGameState.PlayerTurnRecord(p, dgs.nCave, -1));
                    }
                }
                // Gems on Path are also lost
                dgs.gemsOnPath.clear();

                // Remove last card (it is the hazard one) from path and add to discardDeck
                dgs.path.draw();
                dgs.discardDeck.add(card);

                // Remove any relics from the path if relicVariant is enabled (also to discard deck)
                DiamantParameters params = (DiamantParameters) dgs.getGameParameters();
                if (params.relicVariant) {
                    // Remove all relic cards from the path
                    removeRelicsFromPath(dgs);
                }

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
