package games.saboteur;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.components.grid.SparseGrid;
import core.components.grid.SquareCoord;
import games.saboteur.actions.*;
import games.saboteur.components.*;
import utilities.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_OWNER;
import static games.saboteur.components.ActionCard.ActionCardType.*;
import static games.saboteur.components.RoleCard.RoleCardType.Saboteur;

public class SaboteurForwardModel extends StandardForwardModel {

    //region Setup Functions
    @Override
    protected void _setup(AbstractGameState firstState) {
        SaboteurGameState sgs = (SaboteurGameState) firstState;
        SaboteurGameParameters sgp = (SaboteurGameParameters) sgs.getGameParameters();

        sgs.roleDeck = new PartialObservableDeck<>("RoleDeck", sgs.getNPlayers(), new boolean[sgs.getNPlayers()]);
        int nSaboteurs = sgp.saboteursForPlayerCount[sgs.getNPlayers()];
        int nMiners = sgp.minersForPlayerCount[sgs.getNPlayers()];
        for (int i = 0; i < nSaboteurs; i++)
            sgs.roleDeck.add(new RoleCard(Saboteur));
        for (int i = 0; i < nMiners; i++)
            sgs.roleDeck.add(new RoleCard(RoleCard.RoleCardType.GoldMiner));

        sgs.goalDeck = new Deck<>("GoalDeck", HIDDEN_TO_ALL);
        int treasures = sgp.nTreasures;
        for (int i = 0; i < sgp.nGoals; i++) {
            sgs.goalDeck.add(new PathCard(PathCard.PathCardType.Goal, new boolean[]{true, true, true, true}, treasures > 0));
            treasures--;
        }

        sgs.drawDeck = new Deck<>("DrawDeck", HIDDEN_TO_ALL);
        for (Map.Entry<Pair<PathCard.PathCardType, boolean[]>, Integer> entry : sgp.pathCardDeck.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                sgs.drawDeck.add(new PathCard(entry.getKey().a, entry.getKey().b.clone()));
            }
        }
        for (Map.Entry<Pair<ActionCard.ActionCardType, ActionCard.ToolCardType[]>, Integer> entry : sgp.toolCards.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                sgs.drawDeck.add(new ActionCard(entry.getKey().a, entry.getKey().b.clone()));
            }
        }
        for (int i = 0; i < sgp.mapCardsInDeck; i++) {
            sgs.drawDeck.add(new ActionCard(ActionCard.ActionCardType.Map, new ActionCard.ToolCardType[]{}));
        }
        for (int i = 0; i < sgp.rockfallCardsInDeck; i++) {
            sgs.drawDeck.add(new ActionCard(RockFall, new ActionCard.ToolCardType[]{}));
        }

        sgs.discardDeck = new PartialObservableDeck<>("DiscardDeck", -1, sgs.getNPlayers(), HIDDEN_TO_ALL);

        // the board is not infinite, to avoid silliness: see SaboteurGameState.isOnBoard
        sgs.gridBoard = new SparseGrid<>("Board", sgs.getNPlayers());
        // we start just in from the left edge, and half way down
        sgs.startingSquare = new SquareCoord(sgp.horizontalPadding, sgp.boardHeight() / 2);
        sgs.gridBoard.set(sgs.startingSquare, new PathCard(PathCard.PathCardType.Start, new boolean[]{true, true, true, true}));
        sgs.nuggetDeck = new Deck<>("NuggetDeck", HIDDEN_TO_ALL);
        for (int goldValue = 0; goldValue < sgp.goldSupply.length; goldValue++) {
            for (int i = 0; i < sgp.goldSupply[goldValue]; i++) {
                sgs.nuggetDeck.add(new SaboteurCard(goldValue));
            }
        }
        sgs.nuggetDeck.shuffle(sgs.getRnd());
        setupPlayerDecks(sgs);

        setupRound(sgs, sgp);
    }

    private void setupRound(SaboteurGameState sgs, SaboteurGameParameters sgp) {
        resetBoard(sgs, sgp);
        resetDecks(sgs);
        resetPathCardOptions(sgs);
        setupStartingHand(sgs, sgp);
    }

    private void setupPlayerDecks(SaboteurGameState sgs) {
        Map<ActionCard.ToolCardType, Boolean> brokenTools = new HashMap<>();
        for (ActionCard.ToolCardType toolType : ActionCard.ToolCardType.values()) {
            brokenTools.put(toolType, true);
        }
        //Initialise Player Decks
        sgs.playerDecks.clear();
        sgs.toolDeck.clear();
        sgs.playerNuggetDecks.clear();
        for (int i = 0; i < sgs.getNPlayers(); i++) {
            sgs.playerDecks.add(new Deck<>("Player" + i + "Deck", VISIBLE_TO_OWNER));
            sgs.toolDeck.add(new HashMap<>(brokenTools));
            sgs.playerNuggetDecks.add(new Deck<>("Player" + i + "NuggetDeck", i, VISIBLE_TO_OWNER));
        }
    }

    private void setupStartingHand(SaboteurGameState sgs, SaboteurGameParameters sgp) {
        for (int i = 0; i < sgs.getNPlayers(); i++) {
            for (int j = 0; j < sgp.cardsPerPlayer[sgs.getNPlayers()]; j++) {
                sgs.playerDecks.get(i).add(sgs.drawDeck.draw());
            }
        }
    }

    private void resetBoard(SaboteurGameState sgs, SaboteurGameParameters sgp) {
        // clear the board, but for the starting card
        for (SquareCoord pos : sgs.gridBoard.sortedPositions()) {
            if (pos.equals(sgs.startingSquare)) {
                continue;
            }
            PathCard card = sgs.gridBoard.remove(pos);
            if (card.type != PathCard.PathCardType.Goal) {
                sgs.drawDeck.add(card);
            }
        }
        resetGoals(sgs, sgp);
    }

    private void resetGoals(SaboteurGameState sgs, SaboteurGameParameters sgp) {
        int goalCards = sgs.goalDeck.getSize();
        int verticalSpaceNeeded = goalCards * (sgp.goalSpacingY + 1) - 1;
        int startingY = sgs.startingSquare.y() - verticalSpaceNeeded / 2;

        for (SaboteurCard goalCard : sgs.goalDeck.getComponents()) {
            PathCard currentCard = (PathCard) goalCard;
            SquareCoord goal = new SquareCoord(sgp.goalSpacingX + sgs.startingSquare.x(), startingY);
            sgs.gridBoard.set(goal, currentCard);
            // goal cards start face down: seen only by a Map card or when a path reaches them
            for (int p = 0; p < sgs.getNPlayers(); p++)
                sgs.gridBoard.setVisible(goal, p, false);
            startingY += (sgp.goalSpacingY + 1);
        }
    }

    private void resetDecks(SaboteurGameState sgs) {
        sgs.drawDeck.add(sgs.discardDeck);
        sgs.discardDeck.clear();
        for (int i = 0; i < sgs.getNPlayers(); i++) {
            for (ActionCard.ToolCardType toolType : ActionCard.ToolCardType.values()) {
                sgs.toolDeck.get(i).put(toolType, true);
            }
            sgs.drawDeck.add(sgs.playerDecks.get(i));
            sgs.playerDecks.get(i).clear();
        }

        //Shuffle Necessary decks
        sgs.drawDeck.shuffle(sgs.getRnd());
        sgs.goalDeck.shuffle(sgs.getRnd());

        // Assign roles
        sgs.roleDeck.shuffle(sgs.getRnd());
        for (int i = 0; i < sgs.roleDeck.getSize(); i++) {
            for (int j = 0; j < sgs.getNPlayers(); j++) {
                sgs.roleDeck.setVisibilityOfComponent(i, j, i == j);
            }
        }

        sgs.nOfSaboteurs = 0;
        sgs.nOfMiners = 0;
        //does this remove the card?
        IntStream.range(0, sgs.getNPlayers()).mapToObj(i -> (RoleCard) sgs.roleDeck.get(i)).forEach(currentRole -> {
            if (currentRole.type == Saboteur) {
                sgs.nOfSaboteurs++;
            } else {
                sgs.nOfMiners++;
            }
        });
    }

    private void resetPathCardOptions(SaboteurGameState sgs) {
        sgs.pathCardOptions.clear();
        sgs.goalLocationsFound.clear();
        sgs.pathCardOptions.addAll(sgs.startingSquare.neighbours());
    }
    //endregion

    //region Compute Action Functions
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        //Initialise ArrayList of Abstract Actions
        ArrayList<AbstractAction> actions = new ArrayList<>();
        SaboteurGameState sgs = (SaboteurGameState) gameState;

        int player = gameState.getCurrentPlayer();
        Deck<SaboteurCard> currentPlayersDeck = sgs.playerDecks.get(player);
        boolean playerHasBrokenTool = sgs.isAnyToolBroken(player);

        //Check Each card in players deck
        //Switch Case for each type of card you would find in hand
        boolean consideredRockfall = false, consideredMap = false;
        // in order, so that equal states list their actions in the same order
        List<SquareCoord> locations = new ArrayList<>(sgs.pathCardOptions);
        Collections.sort(locations);
        for (int i = 0; i < currentPlayersDeck.getSize(); i++) {
            SaboteurCard card = currentPlayersDeck.peek(i);
            switch (card.type) {
                case Path:
                    if (!playerHasBrokenTool) {
                        actions.addAll(computePathAction((PathCard) card, sgs, locations));
                        break;
                    }
                    break;

                case Action:
                    // This code is for the special case that we have two Rockfall cards in hand.
                    // In this case there is not distinction between the two cards, so we only consider one of them
                    // this avoids the need for an expensive distinct() check when we return the final actions list
                    ActionCard actionCard = (ActionCard) card;
                    if (actionCard.actionType == RockFall) {
                        if (consideredRockfall)
                            break;
                        else
                            consideredRockfall = true;
                    }
                    // likewise two Map cards are interchangeable (PlayMapCard names only the goal to look at)
                    if (actionCard.actionType == Map) {
                        if (consideredMap)
                            break;
                        else
                            consideredMap = true;
                    }
                    actions.addAll(computeActionAction((ActionCard) card, i, sgs));
                    break;
            }
        }

        for (int i = 0; i < currentPlayersDeck.getSize(); i++) {
            actions.add(new Pass(i));
        }
        if (actions.isEmpty()) {
            actions.add(new DoNothing());
        }
        return actions;
    }

    //Updates the map of possible path card locations and directions whenever a path card is placed
    private ArrayList<AbstractAction> computePathAction(PathCard card, SaboteurGameState sgs, List<SquareCoord> locations) {
        //Check if card can fit into key pair value
        //Rotate card, and recheck
        //If it can fit
        //Add new action to place card
        ArrayList<AbstractAction> actions = new ArrayList<>();
        for (SquareCoord location : locations) {
            if (checkPathCardPlacement(card, sgs, location)) {
                actions.add(new PlacePathCard(location, card.getComponentID(), false));
            }

            //check when its rotated
            if (!card.isSymmetric() && checkPathCardPlacement(card.rotated(), sgs, location)) {
                actions.add(new PlacePathCard(location, card.getComponentID(), true));
            }
        }
        return actions;
    }

    //Check if the path card can be placed at the location
    private boolean checkPathCardPlacement(PathCard card, SaboteurGameState sgs, SquareCoord location) {
        if (!sgs.isOnBoard(location)) return false;
        for (int i = 0; i < location.nDirections(); i++) {
            PathCard neighborCard = sgs.gridBoard.get(location.neighbour(i));
            if (neighborCard == null) {
                continue;
            }
            // the paths must meet across the side the cards share
            if (card.opensTo(i) != neighborCard.opensTo(location.opposite(i))) {
                return false;
            }
        }
        return true;
    }

    //For when Rockfall card is played
    //Recalculate all possible path card options via recursion
    private void recalculatePathCardOptions(SaboteurGameState sgs) {
        sgs.pathCardOptions.clear();
        sgs.goalLocationsFound.clear();
        recalculatePathCardOptionsRecursive(new HashSet<>(), sgs, sgs.startingSquare);
    }

    private void recalculatePathCardOptionsRecursive(Set<SquareCoord> checkedLocations, SaboteurGameState sgs, SquareCoord location) {
        PathCard currentCard = sgs.gridBoard.get(location);
        checkedLocations.add(location);
        if (currentCard == null) {
            sgs.pathCardOptions.add(location);
            return; // no card for connectivity - but we can add one here in the future
        } else if (currentCard.type == PathCard.PathCardType.Edge) {
            return; // Edge breaks connectivity
        } else if (!sgs.isOnBoard(location)) {
            return; // out of bounds
        } else if (currentCard.type == PathCard.PathCardType.Goal) {
            // we have found a goal
            for (int i = 0; i < sgs.getNPlayers(); i++) {
                sgs.gridBoard.setVisible(location, i, true);
            }
            sgs.goalLocationsFound.add(location);
        }

        //check adjacent cards for path card
        for (int i = 0; i < location.nDirections(); i++) {
            if (currentCard.opensTo(i)) { // we can reach this space from currentCard
                SquareCoord neighborLocation = location.neighbour(i);
                if (!checkedLocations.contains(neighborLocation))
                    recalculatePathCardOptionsRecursive(checkedLocations, sgs, neighborLocation);
            }
        }
    }

    private ArrayList<AbstractAction> computeActionAction(ActionCard card, int cardIdx, SaboteurGameState sgs) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        switch (card.actionType) {
            case BrokenTools, FixTools:
                actions.addAll(computeToolActions(card, cardIdx, sgs));
                break;

            case Map:
                actions.addAll(computeActionMap(sgs));
                break;

            case RockFall:
                actions.addAll(computeActionRockFall(sgs));
                break;
        }
        return actions;
    }

    private ArrayList<AbstractAction> computeToolActions(ActionCard card, int cardIdx, SaboteurGameState sgs) {
        //for everyone's BrokenToolDeck
        //If that player doesn't have a BrokenTool Matching it
        //new action to add card onto their BrokenToolDeck
        ArrayList<AbstractAction> actions = new ArrayList<>();
        for (int currentPlayer = 0; currentPlayer < sgs.getNPlayers(); currentPlayer++) {
            if (currentPlayer == sgs.getCurrentPlayer()) {
                continue;
            }
            for (ActionCard.ToolCardType type : card.toolTypes) {
                if (card.actionType == BrokenTools && sgs.isToolFunctional(currentPlayer, type)
                        || card.actionType == FixTools && !sgs.isToolFunctional(currentPlayer, type)) {
                    actions.add(new PlayToolCard(cardIdx, currentPlayer, type, card.actionType == ActionCard.ActionCardType.FixTools));
                }
            }
        }
        return actions;
    }

    private ArrayList<AbstractAction> computeActionMap(SaboteurGameState sgs) {
        //new action to check either 1 of the 3 goals and make it visible to the player
        //need to make grid board have visibility of some kind
        ArrayList<AbstractAction> actions = new ArrayList<>();
        for (SquareCoord pos : sgs.gridBoard.sortedPositions()) {
            if (sgs.gridBoard.get(pos).type == PathCard.PathCardType.Goal && !sgs.gridBoard.isVisible(pos, sgs.getCurrentPlayer())) {
                actions.add(new PlayMapCard(pos));
            }
        }
        return actions;
    }

    private List<AbstractAction> computeActionRockFall(SaboteurGameState sgs) {
        RoleCard currentPlayersRole = (RoleCard) sgs.roleDeck.get(sgs.getCurrentPlayer());
        List<AbstractAction> actions = new ArrayList<>();
        for (SquareCoord pos : sgs.gridBoard.sortedPositions()) {
            PathCard currentCard = sgs.gridBoard.get(pos);
            if (currentCard.type == PathCard.PathCardType.Path && currentPlayersRole.type == Saboteur) {
                actions.add(new PlayRockFallCard(pos));
            }
            if (currentCard.type == PathCard.PathCardType.Edge) {
                actions.add(new PlayRockFallCard(pos));
            }
        }
        return actions;
    }
    //endregion

    //region AfterAction Functions
    @Override
    protected void _afterAction(AbstractGameState gameState, AbstractAction action) {
        SaboteurGameState sgs = (SaboteurGameState) gameState;
        //draw card for player
        Deck<SaboteurCard> currentDeck = sgs.playerDecks.get(sgs.getCurrentPlayer());
        if (sgs.drawDeck.getSize() != 0) {
            currentDeck.add(sgs.drawDeck.draw());
        }
        if (action instanceof PlacePathCard) {
            recalculatePathCardOptions(sgs);
            boolean treasureFound = sgs.goalLocationsFound.stream()
                    .map(loc -> sgs.gridBoard.get(loc))
                    .filter(Objects::nonNull).anyMatch(PathCard::hasTreasure);
            if (treasureFound)
                distributeMinerEarnings(sgs);

        } else if (action instanceof PlayRockFallCard) {
            recalculatePathCardOptions(sgs);
        } else if (action instanceof DoNothing) {
            distributeSaboteurEarnings(sgs);
        }
        endPlayerTurn(sgs);
    }

    //Distribute earnings for all miners
    private void distributeMinerEarnings(SaboteurGameState sgs) {
        Deck<SaboteurCard> winningPlayersNuggetDeck = sgs.playerNuggetDecks.get(sgs.getCurrentPlayer());

        int highestNuggetSizeIndex = 0;
        int highestNuggetSize = 0;
        for (int i = 0; i < sgs.nuggetDeck.getSize(); i++) {
            int currentNuggetSize = sgs.nuggetDeck.peek(i).nOfNuggets;
            if (currentNuggetSize > highestNuggetSize) {
                highestNuggetSize = currentNuggetSize;
                highestNuggetSizeIndex = i;
            }
        }
        if (sgs.nuggetDeck.getSize() != 0) {
            winningPlayersNuggetDeck.add(sgs.nuggetDeck.pick(highestNuggetSizeIndex));
        }

        for (int player = 0; player < sgs.getNPlayers(); player++) {
            RoleCard currentPlayersRole = (RoleCard) sgs.roleDeck.get(player);
            if (player == sgs.getCurrentPlayer() || currentPlayersRole.type == Saboteur) {
                continue;
            }
            Deck<SaboteurCard> currentPlayerNuggetDeck = sgs.playerNuggetDecks.get(player);
            if (sgs.nuggetDeck.getSize() != 0) {
                currentPlayerNuggetDeck.add(sgs.nuggetDeck.draw());
            }
        }

        if (sgs.getRoundCounter() > 1) {
            endGame(sgs);
            return;
        }
        sgs.minersWinByRound[sgs.getRoundCounter()] = true;
        endRound(sgs);
        setupRound(sgs, (SaboteurGameParameters) sgs.getGameParameters());
    }

    //Distribute earnings for all saboteurs
    private void distributeSaboteurEarnings(SaboteurGameState sgs) {
        int targetNuggetValue = 0;
        targetNuggetValue = switch (sgs.nOfSaboteurs) {
            case 1 -> 4;
            case 2, 3 -> 3;
            case 4 -> 2;
            default -> targetNuggetValue;
        };

        for (int player = 0; player < sgs.getNPlayers(); player++) {
            RoleCard currentPlayersRole = (RoleCard) sgs.roleDeck.get(player);
            if (currentPlayersRole.type == RoleCard.RoleCardType.GoldMiner) {
                continue;
            }
            if (sgs.nuggetDeck.getSize() == 0) {
                break;
            }
            giveSaboteurGold(sgs, targetNuggetValue, player);

        }
        if (sgs.getRoundCounter() > 2) {
            endGame(sgs);
            return;
        }
        sgs.minersWinByRound[sgs.getRoundCounter()] = false;
        endRound(sgs);
        setupRound(sgs, (SaboteurGameParameters) sgs.getGameParameters());
    }

    private boolean nuggetExists(SaboteurGameState sgs, int targetValue) {
        for (int i = 0; i < sgs.nuggetDeck.getSize(); i++) {
            if (sgs.nuggetDeck.peek(i).nOfNuggets == targetValue) {
                return true;
            }
        }
        return false;
    }

    private void giveSaboteurGold(SaboteurGameState sgs, int targetValue, int currentPlayer) {
        Deck<SaboteurCard> currentNuggetDeck = sgs.playerNuggetDecks.get(currentPlayer);
        int currentValue = 0;
        int preferredNuggetValue = 3;
        if (preferredNuggetValue > targetValue) {
            preferredNuggetValue = targetValue;
        }
        while (targetValue != currentValue && preferredNuggetValue > 0) {
            if (nuggetExists(sgs, preferredNuggetValue)) {
                for (int i = 0; i < sgs.nuggetDeck.getSize(); i++) {
                    if (sgs.nuggetDeck.peek(i).nOfNuggets == preferredNuggetValue) {
                        currentNuggetDeck.add(sgs.nuggetDeck.pick(i));
                        currentValue += preferredNuggetValue;
                        preferredNuggetValue = targetValue - currentValue;
                        break;
                    }
                }
            } else {
                preferredNuggetValue -= 1;
            }
        }
    }
//endregion
}