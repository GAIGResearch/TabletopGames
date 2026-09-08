package games.diamant;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Counter;
import core.components.Deck;
import core.interfaces.ITreeActionSpace;
import games.diamant.actions.ContinueInCave;
import games.diamant.actions.ExitFromCave;
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
     * In this game, all players in the cave choose at the same time. Executing an action just
     * records the choice in the game state; the turn is resolved once every player in the cave has
     * chosen. Choices may arrive one at a time (the turn is handed round the players still to
     * choose) or all together as a single SimultaneousAction, and both reach the same state.
     *
     * @param currentState: current state of the game
     * @param action:       action to be executed
     */
    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        DiamantGameState dgs = (DiamantGameState) currentState;
        int startingRound = dgs.getRoundCounter();
        if (dgs.getPlayersStillToChoose().isEmpty()) {
            playActions(dgs);
            dgs.actionsPlayed.clear();
        }
        // If the cave ended, endRound has already handed the turn to the first player of the new one
        if (dgs.getRoundCounter() == startingRound && dgs.isNotTerminal())
            endPlayerTurn(dgs, nextPlayerToChoose(dgs));
    }

    /**
     * The player who holds the turn when a new cave starts: the lowest-numbered player who was
     * outside the cave when it ended, or player 0 if nobody was. Called before everyone is put back
     * into the cave.
     * <p>
     * For the game itself any player would do, since everyone chooses at once. The choice matters
     * to a sequential open-loop search tree, which files the node reached after an action under the
     * player who then holds the turn. When the cave continues, that is one of the players who chose
     * to continue; when a hazard ends it, everyone is back in the cave and all of them decide next.
     * The same action leads to both, by chance, so the turn owner after a collapse must not be one
     * of the continuing players, or the two outcomes would share a node. A decoupled search files a
     * successor in which several players decide in a slot of its own (SingleTreeNode.childSlot),
     * so it does not depend on this choice.
     */
    private int firstPlayerOfNextCave(DiamantGameState dgs) {
        for (int p = 0; p < dgs.getNPlayers(); p++)
            if (!dgs.playerInCave.get(p))
                return p;
        return 0;
    }

    /**
     * The next player, cycling round from the current one, who is in the cave and has not yet
     * chosen this turn. Players who have left the cave are never given the turn.
     */
    private int nextPlayerToChoose(DiamantGameState dgs) {
        int current = dgs.getCurrentPlayer();
        for (int i = 1; i <= dgs.getNPlayers(); i++) {
            int p = (current + i) % dgs.getNPlayers();
            if (dgs.playerInCave.get(p) && !dgs.actionsPlayed.containsKey(p))
                return p;
        }
        throw new AssertionError("No player in the cave is still to choose");
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

        endRound(dgs, firstPlayerOfNextCave(dgs));

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


    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        return _computeAvailableActions(gameState, gameState.getCurrentPlayer());
    }

    /**
     * Gets the possible actions for the given player.
     * A player in the cave has exactly two: ExitFromCave and ContinueInCave.
     * A player who has left the cave has no decision to make until the next cave, and is never
     * given the turn (see DiamantGameState.getCurrentSimultaneousPlayers), so their list is empty.
     *
     * @param gameState:    current game state
     * @param activePlayer: the player whose actions are wanted
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState, int activePlayer) {
        DiamantGameState dgs = (DiamantGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();

        if (dgs.playerInCave.get(activePlayer)) {
            actions.add(new ContinueInCave(activePlayer));
            actions.add(new ExitFromCave(activePlayer));
        }

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
        return tree;
    }

    @Override
    public ActionTreeNode updateActionTree(ActionTreeNode root, AbstractGameState gameState) {
        DiamantGameState dgs = (DiamantGameState) gameState;
        root.resetTree();
        int player = gameState.getCurrentPlayer();
        if (dgs.playerInCave.get(player)) {
            root.findChildrenByName("continue").setAction(new ContinueInCave(player));
            root.findChildrenByName("exit").setAction(new ExitFromCave(player));
        }
        return root;
    }
}
