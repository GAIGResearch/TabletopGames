package games.saboteur;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.components.PartialObservableGridBoard;
import games.saboteur.actions.*;
import games.saboteur.components.*;
import utilities.Pair;
import utilities.Vector2D;

import java.util.*;
import java.util.stream.IntStream;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_OWNER;
import static games.saboteur.components.ActionCard.ActionCardType.BrokenTools;
import static games.saboteur.components.ActionCard.ActionCardType.FixTools;

public class SaboteurForwardModel extends StandardForwardModel {

    //region Setup Functions
    @Override
    protected void _setup(AbstractGameState firstState)
    {
        SaboteurGameState sgs = (SaboteurGameState) firstState;
        SaboteurGameParameters sgp = (SaboteurGameParameters) sgs.getGameParameters();

        sgs.roleDeck = new PartialObservableDeck<>("RoleDeck", sgs.getNPlayers(), new boolean[sgs.getNPlayers()]);
        for (Map.Entry<RoleCard.RoleCardType, Integer> entry: sgp.roleCardDeck.entrySet())
        {
            for (int i = 0; i < entry.getValue(); i++)
            {
                sgs.roleDeck.add(new RoleCard(entry.getKey()));
            }
        }
        sgs.goalDeck = new Deck<>("GoalDeck", HIDDEN_TO_ALL);
        int treasures = sgp.nTreasures;
        for(int i = 0; i < sgp.nGoals; i++)
        {
            sgs.goalDeck.add(new PathCard(PathCard.PathCardType.Goal, new boolean[]{true, true, true, true}, treasures > 0));
            treasures--;
        }

        sgs.drawDeck = new Deck<>("DrawDeck", HIDDEN_TO_ALL);
        for (Map.Entry<Pair<PathCard.PathCardType, boolean[]>, Integer> entry : sgp.pathCardDeck.entrySet())
        {
            for (int i = 0; i < entry.getValue(); i++)
            {
                sgs.drawDeck.add(new PathCard(entry.getKey().a, entry.getKey().b.clone()));
            }
        }
        for (Map.Entry<Pair<ActionCard.ActionCardType, ActionCard.ToolCardType[]>, Integer> entry: sgp.toolCards.entrySet())
        {
            for (int i = 0; i < entry.getValue(); i++)
            {
                sgs.drawDeck.add(new ActionCard(entry.getKey().a, entry.getKey().b.clone()));
            }
        }

        sgs.discardDeck = new Deck<>("DiscardDeck", sgs.getNPlayers(), HIDDEN_TO_ALL);
        sgs.gridBoard = new PartialObservableGridBoard(sgp.gridSize, sgp.gridSize, sgs.getNPlayers(), true);
        sgs.centerOfGrid = (int) Math.floor(sgp.gridSize / 2.0);
        sgs.gridBoard.setElement(sgs.centerOfGrid, sgs.centerOfGrid, new PathCard(PathCard.PathCardType.Start, new boolean[]{true,true,true,true}));
        sgs.nuggetDeck = new Deck<>("NuggetDeck", HIDDEN_TO_ALL);
        for (Map.Entry<Integer, Integer> entry : sgp.goldNuggetDeck.entrySet())
        {
            for (int i = 0; i < entry.getValue(); i++)
            {
                sgs.nuggetDeck.add(new SaboteurCard(entry.getKey()));
            }
        }
        sgs.nuggetDeck.shuffle(new Random(sgs.getGameParameters().getRandomSeed()));
        setupPlayerDecks(sgs);

        setupRound(sgs, sgp);
    }

    private void setupRound(SaboteurGameState sgs, SaboteurGameParameters sgp)
    {
        resetBoard(sgs, sgp);
        resetDecks(sgs);
        resetPathCardOptions(sgs);
        setupStartingHand(sgs, sgp);
    }

    private void setupPlayerDecks(SaboteurGameState sgs)
    {
        Map<ActionCard.ToolCardType, Boolean> brokenTools = new HashMap<>();
        for (ActionCard.ToolCardType toolType : ActionCard.ToolCardType.values())
        {
            brokenTools.put(toolType, true);
        }
        //Initialise Player Decks
        sgs.playerDecks.clear();
        sgs.toolDeck.clear();
        sgs.playerNuggetDecks.clear();
        for (int i = 0; i < sgs.getNPlayers(); i++)
        {
            sgs.playerDecks.add(new Deck<>("Player" + i + "Deck", VISIBLE_TO_OWNER));
            sgs.toolDeck.add(new HashMap<>(brokenTools));
            sgs.playerNuggetDecks.add(new Deck<>("Player" + i + "NuggetDeck", i, VISIBLE_TO_OWNER));
        }
    }

    private void setupStartingHand(SaboteurGameState sgs, SaboteurGameParameters sgp)
    {
        for (int i = 0; i < sgs.getNPlayers(); i++)
        {
            for (int j = 0; j < sgp.nStartingCards; j++)
            {
                sgs.playerDecks.get(i).add(sgs.drawDeck.draw());
            }
        }
    }

    private void resetBoard(SaboteurGameState sgs, SaboteurGameParameters sgp)
    {
        //Initialise GridBoard with starting card
        sgs.centerOfGrid = (int) Math.floor(sgp.gridSize / 2.0);
        for (int x = 0; x < sgs.gridBoard.getWidth(); x++)
        {
            for (int y = 0; y < sgs.gridBoard.getHeight(); y++)
            {
                if (x == sgs.centerOfGrid && y == sgs.centerOfGrid)
                {
                    continue;
                }
                PathCard card = (PathCard) sgs.gridBoard.getElement(x, y);
                if (card != null && card.type != PathCard.PathCardType.Goal) {
                    sgs.drawDeck.add(card);
                }
                sgs.gridBoard.setElement(x, y, null);
            }
        }
        resetGoals(sgs,sgp);
    }

    private void resetGoals(SaboteurGameState sgs, SaboteurGameParameters sgp)
    {
        int totalLength = sgs.goalDeck.getSize() * (sgp.goalSpacingY + 1);
        int startingY = (int) Math.floor(totalLength / 2.0) - 1;

        assert sgp.goalSpacingX <= Math.floor(sgs.gridBoard.getWidth() / 2.0): "Placing Goal card out of bounds for X";

        for(SaboteurCard goalCard: sgs.goalDeck.getComponents())
        {
            assert startingY <= sgs.gridBoard.getHeight(): "Placing Goal card out of bounds for Y";
            PathCard currentCard = (PathCard) goalCard;
            sgs.gridBoard.setElement(sgp.goalSpacingX + 1 + sgs.centerOfGrid, startingY + sgs.centerOfGrid, currentCard);
            startingY -= (sgp.goalSpacingY + 1);
        }
    }

    private void resetDecks(SaboteurGameState sgs)
    {
        sgs.drawDeck.add(sgs.discardDeck);
        sgs.discardDeck.clear();
        for (int i = 0; i < sgs.getNPlayers(); i++)
        {
            for (ActionCard.ToolCardType toolType : ActionCard.ToolCardType.values())
            {
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
        for(int i = 0; i < sgs.roleDeck.getSize(); i++)
        {
            for (int j = 0; j < sgs.getNPlayers(); j++)
            {
                sgs.roleDeck.setVisibilityOfComponent(i, j, i == j);
            }
        }

        sgs.nOfSaboteurs = 0;
        sgs.nOfMiners = 0;
        //does this remove the card?
        IntStream.range(0, sgs.getNPlayers()).mapToObj(i -> (RoleCard) sgs.roleDeck.get(i)).forEach(currentRole -> {
            if (currentRole.type == RoleCard.RoleCardType.Saboteur) {
                sgs.nOfSaboteurs ++;
            } else {
                sgs.nOfMiners ++;
            }
        });
    }

    private void resetPathCardOptions(SaboteurGameState sgs)
    {
        sgs.pathCardOptions.clear();
        sgs.pathCardOptions.add(new Vector2D(sgs.centerOfGrid + 1, sgs.centerOfGrid));
        sgs.pathCardOptions.add(new Vector2D(sgs.centerOfGrid - 1, sgs.centerOfGrid));
        sgs.pathCardOptions.add(new Vector2D(sgs.centerOfGrid, sgs.centerOfGrid + 1));
        sgs.pathCardOptions.add(new Vector2D(sgs.centerOfGrid, sgs.centerOfGrid - 1));
    }
    //endregion

    //region Compute Action Functions
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState)
    {
        //Initialise ArrayList of Abstract Actions
        ArrayList<AbstractAction> actions = new ArrayList<>();
        SaboteurGameState sgs = (SaboteurGameState) gameState;

        int player = gameState.getCurrentPlayer();
        Deck<SaboteurCard> currentPlayersDeck = sgs.playerDecks.get(player);
        boolean playerHasBrokenTool = sgs.isAnyToolBroken(player);

        //Check Each card in players deck
        //Switch Case for each type of card you would find in hand

        for(int i = 0; i < currentPlayersDeck.getSize(); i++)
        {
            SaboteurCard card = currentPlayersDeck.peek(i);
            switch(card.type)
            {
                case Path:
                    if(!playerHasBrokenTool)
                    {
                        actions.addAll(computePathAction((PathCard) card, sgs));
                        break;
                    }
                    break;

                case Action:
                    actions.addAll(computeActionAction((ActionCard) card, i, sgs));
                    break;
            }
        }

        for(int i = 0; i < currentPlayersDeck.getSize(); i++)
        {
            actions.add(new Pass(i));
        }
        if(actions.isEmpty())
        {
            actions.add(new DoNothing());
        }
        return actions;
    }

    //Updates the map of possible path card locations and directions whenever a path card is placed
    private ArrayList<AbstractAction> computePathAction(PathCard card, SaboteurGameState sgs)
    {
        //Check if card can fit into key pair value
        //Rotate card, and recheck
        //If it can fit
            //Add new action to place card
        ArrayList<AbstractAction> actions = new ArrayList<>();
        for(Vector2D location: sgs.pathCardOptions)
        {
            if(checkPathCardPlacement(card, sgs, location))
            {
                actions.add(new PlacePathCard(sgs.gridBoard.getComponentID(), location.getX(), location.getY(), card.getComponentID(), false));
            }

            //check when its rotated
            card.rotate();
            if(checkPathCardPlacement(card, sgs, location))
            {
                actions.add(new PlacePathCard(sgs.gridBoard.getComponentID(), location.getX(), location.getY(), card.getComponentID(), true));
            }
            card.rotate();
        }
        return actions;
    }

    //Check if the path card can be placed at the location
    private boolean checkPathCardPlacement(PathCard card, SaboteurGameState sgs, Vector2D location)
    {
        if (location.getX() < 0 || location.getY() < 0
                || location.getX() >= ((SaboteurGameParameters)sgs.getGameParameters()).gridSize
                || location.getY() >= ((SaboteurGameParameters)sgs.getGameParameters()).gridSize) return false;
        boolean[] currentDirections = card.getDirections();
        for(int i = 0 ; i < 4; i++)
        {
            Vector2D offset = getCardOffset(i);
            int neighborX = location.getX() + offset.getX();
            int neighborY = location.getY() + offset.getY();
            PathCard neighborCard = (PathCard) sgs.gridBoard.getElement(neighborX, neighborY);
            if(neighborCard == null)
            {
                continue;
            }
            boolean[] neighbourDirections = neighborCard.getDirections();
            if(currentDirections[i] != neighbourDirections[neighborCard.getOppositeDirection(i)])
            {
                return false;
            }
        }
        return true;
    }

    //For when Rockfall card is played
    //Recalculate all possible path card options via recursion
    private void recalculatePathCardOptions(SaboteurGameState sgs)
    {
        sgs.pathCardOptions.clear();
        PathCard currentCard = (PathCard) sgs.gridBoard.getElement(sgs.centerOfGrid, sgs.centerOfGrid);
        Map<Vector2D, PathCard> previousCards = new HashMap<>();
        previousCards.put(new Vector2D(sgs.centerOfGrid, sgs.centerOfGrid), currentCard);
        recalculatePathCardOptionsRecursive(previousCards,sgs, new Vector2D(sgs.centerOfGrid, sgs.centerOfGrid));
    }

    private void recalculatePathCardOptionsRecursive(Map<Vector2D, PathCard> previousCards, SaboteurGameState sgs, Vector2D location)
    {
        PathCard currentCard = (PathCard) sgs.gridBoard.getElement(location.getX(), location.getY());
        if(currentCard == null)
        {
            sgs.pathCardOptions.add(location);
            return;
        }
        else if (currentCard.type == PathCard.PathCardType.Edge)
        {
            return;
        }
        else if (previousCards.containsKey(location) && previousCards.size() != 1)
        {
            return;
        }
        //check adjacent cards for path card
        for(int i = 0; i < 4; i++)
        {
            Vector2D offset = getCardOffset(i);
            int neighborX = location.getX() + offset.getX();
            int neighborY = location.getY() + offset.getY();
            PathCard neighbourCard = (PathCard) sgs.gridBoard.getElement(neighborX, neighborY);
            if (currentCard.getDirections()[i] && neighbourCard != previousCards.get(location))
            {
                previousCards.put(new Vector2D(location.getX(), location.getY()), currentCard);
                recalculatePathCardOptionsRecursive(previousCards, sgs, new Vector2D(neighborX, neighborY));
            }
        }
    }

    //down, up, left, right as the grid is 0 starts on the top left
    //0,1,2,3,4,5,6
    //1
    //2
    //3
    //4
    //5
    //6

    private Vector2D getCardOffset(int value)
    {
        return switch (value) {
            case 0 -> new Vector2D(0, -1);
            case 1 -> new Vector2D(0, 1);
            case 2 -> new Vector2D(-1, 0);
            case 3 -> new Vector2D(1, 0);
            default -> new Vector2D(999, 999);
        };
    }

    private ArrayList<AbstractAction> computeActionAction(ActionCard card, int cardIdx, SaboteurGameState sgs)
    {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        switch(card.actionType)
        {
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

    private ArrayList<AbstractAction> computeToolActions(ActionCard card, int cardIdx, SaboteurGameState sgs)
    {
        //for everyone's BrokenToolDeck
        //If that player doesn't have a BrokenTool Matching it
        //new action to add card onto their BrokenToolDeck
        ArrayList<AbstractAction> actions = new ArrayList<>();
        for(int currentPlayer = 0; currentPlayer < sgs.getNPlayers(); currentPlayer++)
        {
            if(currentPlayer == sgs.getCurrentPlayer())
            {
                continue;
            }
            for(ActionCard.ToolCardType type : card.toolTypes)
            {
                if(card.actionType == BrokenTools && sgs.isToolFunctional(currentPlayer, type)
                    || card.actionType == FixTools && !sgs.isToolFunctional(currentPlayer, type))
                {
                    actions.add(new PlayToolCard(cardIdx, currentPlayer, type, card.actionType == ActionCard.ActionCardType.FixTools));
                }
            }
        }
        return actions;
    }

    private ArrayList<AbstractAction> computeActionMap(SaboteurGameState sgs)
    {
        //new action to check either 1 of the 3 goals and make it visible to the player
        //need to make grid board have visibility of some kind
        ArrayList<AbstractAction> actions = new ArrayList<>();
        PartialObservableGridBoard gridBoard = sgs.gridBoard;
        for(int x = 0; x < gridBoard.getWidth(); x++)
        {
            for(int y = 0; y < gridBoard.getHeight(); y++)
            {
                PathCard currentCard = (PathCard) gridBoard.getElement(x, y);
                if(currentCard != null && currentCard.type == PathCard.PathCardType.Goal)
                {
                    actions.add(new PlayMapCard(x, y));
                }
            }
        }
        return actions;
    }

    private List<AbstractAction> computeActionRockFall(SaboteurGameState sgs)
    {
        List<AbstractAction> actions = new ArrayList<>();
        for(int x = 0; x < sgs.gridBoard.getWidth(); x++)
        {
            for(int y = 0; y < sgs.gridBoard.getHeight(); y++)
            {
                PathCard currentCard = (PathCard) sgs.gridBoard.getElement(x, y);
                if(currentCard != null && (currentCard.type == PathCard.PathCardType.Path || currentCard.type == PathCard.PathCardType.Edge))
                {
                    actions.add(new PlayRockFallCard(sgs.gridBoard.getComponentID(), x, y));
                }
            }
        }
        return actions;
    }
    //endregion

    //region AfterAction Functions
    @Override
    protected void _afterAction(AbstractGameState gameState, AbstractAction action)
    {
        SaboteurGameState sgs = (SaboteurGameState) gameState;
        //draw card for player
        Deck<SaboteurCard> currentDeck = sgs.playerDecks.get(sgs.getCurrentPlayer());
        if(sgs.drawDeck.getSize() != 0)
        {
            currentDeck.add(sgs.drawDeck.draw());
        }
        if (action instanceof PlacePathCard currentPlacement)
        {
            boolean roundOver = false;
            int goalDirection = hasGoalInPossibleDirection(sgs, currentPlacement);
            if(goalDirection != -1)
            {
                Vector2D offset = getCardOffset(goalDirection);
                PathCard goalCard = (PathCard) sgs.gridBoard.getElement(((PlacePathCard) action).getX() + offset.getX(), ((PlacePathCard) action).getY() + offset.getY());
                for(int i = 0; i < sgs.getNPlayers(); i++)
                {
                    sgs.gridBoard.setElementVisibility(((PlacePathCard) action).getX() + offset.getX(), ((PlacePathCard) action).getY() + offset.getY(), i, true);
                }
                if(goalCard.hasTreasure())
                {
                    distributeMinerEarnings(sgs);
                    roundOver = true;
                }
            }
            if (!roundOver) {
                Vector2D location = new Vector2D(currentPlacement.getX(), currentPlacement.getY());
                PathCard currentCard = (PathCard) sgs.gridBoard.getElement(location.getX(), location.getY());
                if (currentCard == null) {
                    throw new AssertionError("Card should not be null");
                }
                boolean[] directions = currentCard.getDirections();
                //add available options
                if (currentCard.type == PathCard.PathCardType.Path) {
                    for (int i = 0; i < 4; i++) {
                        Vector2D offset = getCardOffset(i);
                        int neighborX = location.getX() + offset.getX();
                        int neighborY = location.getY() + offset.getY();
                        if (sgs.gridBoard.getElement(neighborX, neighborY) == null && directions[i]) {
                            sgs.pathCardOptions.add(new Vector2D(neighborX, neighborY));
                        } else if (sgs.gridBoard.getElement(neighborX, neighborY) != null && directions[i]) {
                            recalculatePathCardOptions(sgs);
                        }
                    }
                }
            }
        }
        else if(action instanceof PlayRockFallCard)
        {
            recalculatePathCardOptions(sgs);
        }
        else if(action instanceof DoNothing)
        {
            distributeSaboteurEarnings(sgs);
        }
        endPlayerTurn(sgs);
    }

    //check if path card goes into a goal
    private int hasGoalInPossibleDirection(SaboteurGameState sgs, PlacePathCard placePathCard)
    {
        PathCard pathCard = (PathCard) sgs.gridBoard.getElement(placePathCard.getX(), placePathCard.getY());
        boolean[] directions = pathCard.getDirections();
        for(int i = 0; i < pathCard.getDirections().length; i++)
        {
            if(directions[i])
            {
                getCardOffset(i);
                PathCard currentCard = (PathCard) sgs.gridBoard.getElement(placePathCard.getX() + getCardOffset(i).getX(), placePathCard.getY() + getCardOffset(i).getY());
                if(currentCard != null && currentCard.type == PathCard.PathCardType.Goal)
                {
                    return i;
                }
            }
        }
        return -1;
    }

    //Distribute earnings for all saboteurs (if any exists)
    private void distributeMinerEarnings(SaboteurGameState sgs)
    {
        Deck<SaboteurCard> winningPlayersNuggetDeck = sgs.playerNuggetDecks.get(sgs.getCurrentPlayer());

        int highestNuggetSizeIndex = 0;
        int highestNuggetSize = 0;
        for(int i = 0; i < sgs.nuggetDeck.getSize(); i++)
        {
            int currentNuggetSize = sgs.nuggetDeck.peek(i).nOfNuggets;
            if(currentNuggetSize > highestNuggetSize)
            {
                highestNuggetSize = currentNuggetSize;
                highestNuggetSizeIndex = i;
            }
        }
        if(sgs.nuggetDeck.getSize() != 0)
        {
            winningPlayersNuggetDeck.add(sgs.nuggetDeck.pick(highestNuggetSizeIndex));
        }

        for(int player = 0; player < sgs.getNPlayers(); player++)
        {
            RoleCard currentPlayersRole = (RoleCard) sgs.roleDeck.get(player);
            if(player == sgs.getCurrentPlayer() || currentPlayersRole.type == RoleCard.RoleCardType.Saboteur)
            {
                continue;
            }
            Deck<SaboteurCard> currentPlayerNuggetDeck = sgs.playerNuggetDecks.get(player);
            if(sgs.nuggetDeck.getSize() != 0)
            {
                currentPlayerNuggetDeck.add(sgs.nuggetDeck.draw());
            }
        }

        if(sgs.getRoundCounter() > 1)
        {
            endGame(sgs);
            return;
        }
        setupRound(sgs, (SaboteurGameParameters) sgs.getGameParameters());
        endRound(sgs);
    }

    //Distribute earnings for all miners
    private void distributeSaboteurEarnings(SaboteurGameState sgs)
    {
        int targetNuggetValue = 0;
        targetNuggetValue = switch (sgs.nOfSaboteurs) {
            case 1 -> 4;
            case 2, 3 -> 3;
            case 4 -> 2;
            default -> targetNuggetValue;
        };

        for(int player = 0; player < sgs.getNPlayers(); player++)
        {
            RoleCard currentPlayersRole = (RoleCard) sgs.roleDeck.get(player);
            if(currentPlayersRole.type == RoleCard.RoleCardType.GoldMiner)
            {
                continue;
            }
            if(sgs.nuggetDeck.getSize() == 0)
            {
                break;
            }
            giveSaboteurGold(sgs, targetNuggetValue, player);

        }
        if(sgs.getRoundCounter() > 2)
        {
            endGame(sgs);
            return;
        }
        setupRound(sgs, (SaboteurGameParameters) sgs.getGameParameters());
        endRound(sgs);
    }

    private boolean nuggetExists(SaboteurGameState sgs, int targetValue)
    {
        for(int i = 0; i < sgs.nuggetDeck.getSize(); i++)
        {
            if(sgs.nuggetDeck.peek(i).nOfNuggets == targetValue)
            {
                return true;
            }
        }
        return false;
    }

    private void giveSaboteurGold(SaboteurGameState sgs, int targetValue, int currentPlayer)
    {
        Deck<SaboteurCard> currentNuggetDeck = sgs.playerNuggetDecks.get(currentPlayer);
        int currentValue = 0;
        int preferredNuggetValue = 3;
        if(preferredNuggetValue > targetValue)
        {
            preferredNuggetValue = targetValue;
        }
        while(targetValue != currentValue && preferredNuggetValue > 0)
        {
            if(nuggetExists(sgs, preferredNuggetValue))
            {
                for (int i = 0; i < sgs.nuggetDeck.getSize(); i++)
                {
                    if (sgs.nuggetDeck.peek(i).nOfNuggets == preferredNuggetValue)
                    {
                        currentNuggetDeck.add(sgs.nuggetDeck.pick(i));
                        currentValue += preferredNuggetValue;
                        preferredNuggetValue = targetValue - currentValue;
                        break;
                    }
                }
            } else
            {
                preferredNuggetValue -= 1;
            }
        }
    }
//endregion
}