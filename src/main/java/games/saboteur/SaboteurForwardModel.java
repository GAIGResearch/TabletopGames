package games.saboteur;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Card;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.saboteur.actions.*;
import games.saboteur.components.*;
import utilities.Vector2D;

import static core.CoreConstants.VisibilityMode.*;

import java.util.*;

public class SaboteurForwardModel extends StandardForwardModel {
//--------------------------------------------------------------------------------------------------//
//region Setup Functions
    @Override
    protected void _setup(AbstractGameState firstState)
    {
        SaboteurGameState sgs = (SaboteurGameState) firstState;
        SaboteurGameParameters sgp = (SaboteurGameParameters) sgs.getGameParameters();

        sgs.roleDeck = new PartialObservableDeck<>("RoleDeck", sgs.getNPlayers());
        for(int i = 0; i < sgs.roleDeck.getSize(); i++)
        {
            sgs.roleDeck.setVisibilityOfComponent(i, i, true);
        }

        sgs.goalDeck = new Deck<>("GoalDeck", HIDDEN_TO_ALL);
        for(int i = 0; i < 3; i++)
        {
            sgs.goalDeck.add(new PathCard(PathCard.PathCardType.Goal, new boolean[]{true, true, true, true}));
        }

        sgs.drawDeck = new Deck<>("DrawDeck", HIDDEN_TO_ALL);
        sgs.discardDeck = new PartialObservableDeck<>("DiscardDeck", sgs.getNPlayers());
        sgs.gridBoard = new PartialObservableGridBoard<>(sgp.GridSize, sgp.GridSize, sgs.getNPlayers(), true);
        sgs.nuggetDeck = new Deck<>("NuggetDeck", HIDDEN_TO_ALL);


        FillDeckViaMap(sgs.nuggetDeck, sgp.goldNuggetDeck);
        sgs.nuggetDeck.shuffle(new Random(sgs.getGameParameters().getRandomSeed()));
        SetupPlayerDecks(sgs);
        SetupRound(sgs, sgp);
    }

    private void SetupRound(SaboteurGameState sgs, SaboteurGameParameters sgp)
    {
        ResetDecks(sgs, sgp);
        ResetBoard(sgs, sgp);
        ResetPathCardOptions(sgs);
        SetupStartingHand(sgs);
    }

    private void SetupPlayerDecks(SaboteurGameState sgs)
    {
        //Initialise Player Decks
        for (int i = 0; i < sgs.getNPlayers(); i++)
        {
            sgs.playerDecks.add(new Deck<>("Player" + i + "Deck", VISIBLE_TO_OWNER));
            sgs.brokenToolDecks.add(new Deck<>("Player" + i + "BrokenToolDeck", VISIBLE_TO_OWNER));
            sgs.playerNuggetDecks.add(new PartialObservableDeck<>("Player" + i + "NuggetDeck", i));
        }
    }

    private void SetupStartingHand(SaboteurGameState sgs)
    {
        for (int i = 0; i < sgs.getNPlayers(); i++)
        {
            for (int j = 0; j < 5; j++)
            {
                sgs.playerDecks.get(i).add(sgs.drawDeck.draw());
            }
        }
    }
//endregion
//--------------------------------------------------------------------------------------------------//
//region Reset Functions
    private void ResetBoard(SaboteurGameState sgs, SaboteurGameParameters sgp)
    {
        //Initialise GridBoard with starting card
        sgs.centerOfGrid = (int) Math.floor(sgp.GridSize / 2.0);
        sgs.gridBoard = new PartialObservableGridBoard<>(sgp.GridSize, sgp.GridSize, sgs.getNPlayers(), true);
        sgs.gridBoard.setElement(sgs.centerOfGrid, sgs.centerOfGrid, new PathCard(PathCard.PathCardType.Start, new boolean[]{true, true, true, true}));
        ResetGoals(sgs,sgp);
        }

    private void ResetGoals(SaboteurGameState sgs, SaboteurGameParameters sgp)
    {
        int totalLength = sgs.goalDeck.getSize() * (sgp.GoalSpacingY + 1);
        int startingY = (int) Math.floor(totalLength / 2.0) - 1;

        assert sgp.GoalSpacingX > Math.floor(sgs.gridBoard.getWidth() / 2.0): "Placing Goal card out of bounds for X";

        for(SaboteurCard goalCard: sgs.goalDeck.getComponents())
        {
            assert startingY > sgs.gridBoard.getHeight(): "Placing Goal card out of bounds for Y";
            PathCard currentCard = (PathCard) goalCard;
            sgs.gridBoard.setElement(sgp.GoalSpacingX + 1 + sgs.centerOfGrid, startingY + sgs.centerOfGrid, currentCard);
            startingY -= (sgp.GoalSpacingY + 1);
        }
        //System.out.println(sgs.gridBoard.toString());
    }

    private void ResetDecks(SaboteurGameState sgs, SaboteurGameParameters sgp)
    {
        //Clear player decks besides their gold nugget deck
        sgs.drawDeck.clear();
        sgs.discardDeck.clear();
        sgs.roleDeck.clear();
        for (int i = 0; i < sgs.getNPlayers(); i++)
        {
            sgs.brokenToolDecks.get(i).clear();
            sgs.playerDecks.get(i).clear();
        }


        //Fill in decks
        FillDeckViaMap(sgs.drawDeck, sgp.pathCardDeck);
        FillDeckViaMap(sgs.drawDeck, sgp.actionCardDeck);
        FillDeckViaMap(sgs.roleDeck, sgp.roleCardDeck);

        //Shuffle Necessary decks
        Random r = new Random(sgs.getGameParameters().getRandomSeed() + sgs.getRoundCounter());
        sgs.drawDeck.shuffle(r);
        sgs.goalDeck.shuffle(r);
        sgs.roleDeck.shuffle(r);
        sgs.nOfSaboteurs = 0;
        sgs.nOfMiners = 0;
        for(int i = 0; i < sgs.getNPlayers(); i++)
        {
            sgs.roleDeck.setVisibilityOfComponent(i, i, true);
            RoleCard currentRole = (RoleCard) sgs.roleDeck.get(i);                                                       //does this remove the card?
            if(currentRole.type == RoleCard.RoleCardType.Saboteur)
            {
                sgs.nOfSaboteurs += 1;
            }
            else
            {
                sgs.nOfMiners += 1;
            }
        }
    }

    private void ResetPathCardOptions(SaboteurGameState sgs)
    {
        sgs.pathCardOptions.clear();
        sgs.pathCardOptions.add(new Vector2D(sgs.centerOfGrid + 1, sgs.centerOfGrid));
        sgs.pathCardOptions.add(new Vector2D(sgs.centerOfGrid - 1, sgs.centerOfGrid));
        sgs.pathCardOptions.add(new Vector2D(sgs.centerOfGrid, sgs.centerOfGrid + 1));
        sgs.pathCardOptions.add(new Vector2D(sgs.centerOfGrid, sgs.centerOfGrid - 1));
    }
//endregion
//--------------------------------------------------------------------------------------------------//
//region Compute Action Functions
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState)
    {
        //Initialise ArrayList of Abstract Actions
        ArrayList<AbstractAction> actions = new ArrayList<>();
        SaboteurGameState sgs = (SaboteurGameState) gameState;

        int player = gameState.getCurrentPlayer();
        Deck<SaboteurCard> currentPlayersDeck = sgs.playerDecks.get(player);
        boolean playerHasBrokenTool = sgs.brokenToolDecks.get(player).getSize() > 0;

        //Check Each card in players deck
        //Switch Case for each type of card you would find in hand

        for(SaboteurCard card: currentPlayersDeck.getComponents())
        {
            switch(card.type)
            {
                case Path:
                    if(!playerHasBrokenTool)
                    {
                        actions.addAll(ComputePathAction((PathCard) card, sgs));
                        break;
                    }
                    break;

                case Action:
                    actions.addAll(ComputeActionAction((ActionCard) card ,sgs));
                    break;
            }
        }

        for(Card card: currentPlayersDeck.getComponents())
        {
            actions.add(new Pass((SaboteurCard) card));
        }
        System.out.println("Current Round " + sgs.getRoundCounter());
        System.out.println("Current Turn " + sgs.getTurnCounter());
        System.out.println("Current Player " + player);
        System.out.println(sgs.brokenToolDecks.get(player) != null ? sgs.brokenToolDecks.get(player).toString() : "No Broken Tools");
        System.out.println("Available Actions: " + actions.size());
        System.out.println(PrintArray(actions.toArray()));
        if(actions.size() == 0)
        {
            actions.add(new DoNothing());
        }
        return actions;
    }

//region GettingPathActions
    //Updates the map of possible path card locations and directions whenever a path card is placed
    private ArrayList<AbstractAction> ComputePathAction(PathCard card, SaboteurGameState sgs)
    {
        //Check if card can fit into key pair value
        //Rotate card, and recheck
        //If it can fit
            //Add new action to place card
        ArrayList<AbstractAction> actions = new ArrayList<>();
        for(Vector2D location: sgs.pathCardOptions)
        {
            if(CheckPathCardPlacement(card, sgs, location))
            {
                //System.out.println(card.toString() + " can be placed at " + location.toString());
                actions.add(new PlacePathCard(0, location.getX(), location.getY(), card, false));
            }

            //check when its rotated
            card.Rotate();
            if(CheckPathCardPlacement(card, sgs, location))
            {
                //System.out.println(card.toString() + " can be placed at " + location.toString() + " rotated");
                actions.add(new PlacePathCard(0, location.getX(), location.getY(), card, true));
            }
            card.Rotate();
        }
        return actions;
    }

    //Check if the path card can be placed at the location
    private boolean CheckPathCardPlacement(PathCard card, SaboteurGameState sgs, Vector2D location)
    {
        boolean[] currentDirections = card.getDirections();
        for(int i = 0 ; i < 4; i++)
        {
            Vector2D offset = getCardOffset(i);
            int neighborX = location.getX() + offset.getX();
            int neighborY = location.getY() + offset.getY();
            PathCard neighborCard = sgs.gridBoard.getElement(neighborX, neighborY);
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
    private void RecalculatePathCardOptions(SaboteurGameState sgs)
    {
        sgs.pathCardOptions.clear();
        PathCard currentCard = sgs.gridBoard.getElement(sgs.centerOfGrid, sgs.centerOfGrid);
        Map<Vector2D, PathCard> previousCards = new HashMap<>();
        previousCards.put(new Vector2D(sgs.centerOfGrid, sgs.centerOfGrid), currentCard);
        RecalculatePathCardOptionsRecursive(previousCards,sgs, new Vector2D(sgs.centerOfGrid, sgs.centerOfGrid));
    }

    private void RecalculatePathCardOptionsRecursive(Map<Vector2D, PathCard> previousCards, SaboteurGameState sgs, Vector2D location)
    {
        PathCard currentCard = sgs.gridBoard.getElement(location.getX(), location.getY());
        if(currentCard == null)
        {
            //System.out.println("adding " + location.getX() + " " + location.getY() + " to pathCardOptions");
            //System.out.println(sgs.gridBoard.toString(location.getX(), location.getY()));
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
            PathCard neighbourCard = sgs.gridBoard.getElement(neighborX, neighborY);
            if (currentCard.getDirections()[i] && neighbourCard != previousCards.get(location))
            {
                //System.out.println(currentCard.getDirections()[i]);
                previousCards.put(new Vector2D(location.getX(), location.getY()), currentCard);
                RecalculatePathCardOptionsRecursive(previousCards, sgs, new Vector2D(neighborX, neighborY));
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
        switch (value)
        {
            case 0:
                return new Vector2D(0,-1);
            case 1:
                return new Vector2D(0,1);
            case 2:
                return new Vector2D(-1,0);
            case 3:
                return new Vector2D(1,0);
            default:
                return new Vector2D(999,999);
        }
    }
//endregion
//region GettingActionActions
    private ArrayList<AbstractAction> ComputeActionAction (ActionCard card, SaboteurGameState sgs)
    {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        switch(card.actionType)
        {
            case BrokenTools:
                actions.addAll(ComputeActionBrokenTools(card, sgs));
                break;

            case FixTools:
                actions.addAll(ComputeActionFixTools(card, sgs));
                break;

            case Map:
                actions.addAll(ComputeActionMap(card, sgs));
                break;

            case RockFall:
                actions.addAll(ComputeActionRockFall(card, sgs));
                break;
        }
        return actions;
    }
    private ArrayList<AbstractAction> ComputeActionBrokenTools(ActionCard card, SaboteurGameState sgs)
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
            Deck<SaboteurCard> currentPlayerBrokenToolDeck = sgs.brokenToolDecks.get(currentPlayer);

            for(ActionCard.ToolCardType type : card.toolTypes)
            {
                if(!HasToolType(type, currentPlayerBrokenToolDeck))
                {
                    actions.add(new PlayBrokenToolCard(card, currentPlayer, type));
                }
            }
        }
        return actions;
    }
    private ArrayList<AbstractAction> ComputeActionFixTools(ActionCard card, SaboteurGameState sgs)
    {
        //for everyone's BrokenToolDeck
        //If that player does have a BrokenTool Matching it
        //new action to remove players
        ArrayList<AbstractAction> actions = new ArrayList<>();
        for(int currentPlayer = 0; currentPlayer < sgs.getNPlayers(); currentPlayer++)
        {
            if(currentPlayer == sgs.getCurrentPlayer())
            {
                continue;
            }
            Deck<SaboteurCard> currentPlayerBrokenToolDeck = sgs.brokenToolDecks.get(currentPlayer);
            for(ActionCard.ToolCardType type : card.toolTypes)
            {
                if(HasToolType(type, currentPlayerBrokenToolDeck))
                {
                    actions.add(new PlayFixToolCard(currentPlayer, card, type));
                }
            }
        }
        return actions;
    }
    private ArrayList<AbstractAction> ComputeActionMap(ActionCard card, SaboteurGameState sgs)
    {
        //new action to check either 1 of the 3 goals and make it visible to the player
        //need to make grid board have visibility of some kind
        ArrayList<AbstractAction> actions = new ArrayList<>();
        PartialObservableGridBoard<PathCard> gridBoard = sgs.gridBoard;
        for(int x = 0; x < gridBoard.getWidth(); x++)
        {
            for(int y = 0; y < gridBoard.getHeight(); y++)
            {
                PathCard currentCard =  gridBoard.getElement(x, y);
                if(currentCard != null && currentCard.type == PathCard.PathCardType.Goal)
                {
                    actions.add(new PlayMapCard(x, y, card));
                }
            }
        }
        return actions;
    }
    private ArrayList<AbstractAction> ComputeActionRockFall(ActionCard card, SaboteurGameState sgs)
    {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        for(int x = 0; x < sgs.gridBoard.getWidth(); x++)
        {
            for(int y = 0; y < sgs.gridBoard.getHeight(); y++)
            {
                PathCard currentCard = sgs.gridBoard.getElement(x, y);
                if(currentCard != null && (currentCard.type == PathCard.PathCardType.Path || currentCard.type == PathCard.PathCardType.Edge))
                {
                    actions.add(new PlayRockFallCard(sgs.gridBoard.getComponentID(), x, y, card));
                }
            }
        }
        return actions;
    }
    private boolean HasToolType(ActionCard.ToolCardType toolType, Deck<SaboteurCard> brokenToolsDeck)
    {
        //assume that the card has only 1 tool type as brokenTools only have 1 type
        for(SaboteurCard card: brokenToolsDeck.getComponents())
        {
            ActionCard currentCard = (ActionCard) card;
            if (currentCard.toolTypes[0] == toolType)
            {
                return true;
            }
        }
        return false;
    }
//endregion
//endregion
//--------------------------------------------------------------------------------------------------//
//region OtherFunctions
    private void FillDeckViaMap(Deck<SaboteurCard> deck, Map<SaboteurCard, Integer> map)
    {
        //add all the Path Cards
        for (Map.Entry<SaboteurCard, Integer> entry : map.entrySet())
        {
            for (int i = 0; i < entry.getValue(); i++)
            {
                deck.add(entry.getKey());
            }
        }
    }

    private String PrintArray(Object[] array)
    {
        if(array.length == 0)
        {
            return "";
        }
        String value = "------------------------------------------------\n";
        for(Object input: array)
        {
            value += input.toString() + "\n";
        }
        value += "------------------------------------------------\n";
        return value;
    }
//endregion
//--------------------------------------------------------------------------------------------------//
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
        if (action instanceof PlacePathCard)
        {
            PlacePathCard currentPlacement = (PlacePathCard) action;
            int goalDirection = HasGoalInPossibleDirection(sgs, currentPlacement);
            if(goalDirection != -1)
            {
                Vector2D offset = getCardOffset(goalDirection);
                PathCard goalCard = sgs.gridBoard.getElement(((PlacePathCard) action).getX() + offset.getX(), ((PlacePathCard) action).getY() + offset.getY());
                for(int i = 0; i < sgs.getNPlayers(); i++)
                {
                    sgs.gridBoard.setElementVisibility(((PlacePathCard) action).getX() + offset.getX(), ((PlacePathCard) action).getY() + offset.getY(), i, true);
                }
                if(goalCard.hasTreasure)
                {
                    System.out.println("Miners won!");
                    DistributeMinerEarnings(sgs);
                    DisplayNuggetsWorth(sgs);
                }
            }
            Vector2D location = new Vector2D(currentPlacement.getX(), currentPlacement.getY());
            PathCard currentCard = sgs.gridBoard.getElement(location.getX(), location.getY());
            boolean[] directions = currentCard.getDirections();
            //add available options
            if(currentCard instanceof PathCard && currentCard.type == PathCard.PathCardType.Path)
            {
                for(int i = 0; i < 4; i++)
                {
                    Vector2D offset = getCardOffset(i);
                    int neighborX = location.getX() + offset.getX();
                    int neighborY = location.getY() + offset.getY();
                    if(sgs.gridBoard.getElement(neighborX, neighborY) == null && directions[i])
                    {
                        //System.out.println(sgs.gridBoard.getElement(neighborX, neighborY));
                        //System.out.println("ADDING " + neighborX + " " + neighborY + " to pathCardOptions");
                        //System.out.println(sgs.gridBoard.toString(neighborX, neighborY));
                        sgs.pathCardOptions.add(new Vector2D(neighborX, neighborY));
                    }
                    else if(sgs.gridBoard.getElement(neighborX, neighborY) != null && directions[i])
                    {
                        RecalculatePathCardOptions(sgs);
                    }
                }
            }
        }
        else if(action instanceof PlayRockFallCard)
        {
            RecalculatePathCardOptions(sgs);
        }
        else if(action instanceof DoNothing)
        {
            System.out.println("Saboteurs won!");
            DistributeSaboteurEarnings(sgs);
            DisplayNuggetsWorth(sgs);
        }
        endPlayerTurn(sgs);
    }

    //check if path card goes into a goal
    private int HasGoalInPossibleDirection(SaboteurGameState sgs, PlacePathCard placePathCard)
    {
        PathCard pathCard = sgs.gridBoard.getElement(placePathCard.getX(), placePathCard.getY());
        boolean directions[] = pathCard.getDirections();
        for(int i = 0; i < pathCard.getDirections().length; i++)
        {
            if(directions[i])
            {
                getCardOffset(i);
                PathCard currentCard = sgs.gridBoard.getElement(placePathCard.getX() + getCardOffset(i).getX(), placePathCard.getY() + getCardOffset(i).getY());
                if(currentCard != null && currentCard.type == PathCard.PathCardType.Goal)
                {
                    return i;
                }
            }
        }
        return -1;
    }

    //Distribute earnings for all saboteurs (if any exists)
    private void DistributeMinerEarnings(SaboteurGameState sgs)
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
            PartialObservableDeck<SaboteurCard> currentPlayerNuggetDeck = sgs.playerNuggetDecks.get(player);
            if(sgs.nuggetDeck.getSize() != 0)
            {
                currentPlayerNuggetDeck.add(sgs.nuggetDeck.draw());
            }
        }

        if(sgs.getRoundCounter() > 1)
        {
            System.out.println("Game Finished");
            endGame(sgs);
        }
        SetupRound(sgs, (SaboteurGameParameters) sgs.getGameParameters());
        endRound(sgs);
    }

    //Distribute earnings for all miners
    private void DistributeSaboteurEarnings(SaboteurGameState sgs)
    {
        int targetNuggetValue = 0;
        int currentValue = 0;
        switch (sgs.nOfSaboteurs)
        {
            case 1:
                targetNuggetValue = 4;
                break;
            case 2:
            case 3:
                targetNuggetValue = 3;
                break;
            case 4:
                targetNuggetValue = 2;
                break;
        }

        System.out.println("There are " + sgs.nOfSaboteurs + " saboteurs");
        System.out.println("The target value is " + targetNuggetValue);
        for(int player = 0; player < sgs.getNPlayers(); player++)
        {
            RoleCard currentPlayersRole = (RoleCard) sgs.roleDeck.get(player);
            //System.out.println("Player " + player + " has role " + currentPlayersRole.type.toString());
            if(currentPlayersRole.type == RoleCard.RoleCardType.GoldMiner)
            {
                continue;
            }
            if(sgs.nuggetDeck.getSize() == 0)
            {
                break;
            }
            GiveSaboteurGold(sgs, targetNuggetValue, player);

        }
        if(sgs.getRoundCounter() > 2)
        {
            System.out.println("Game Finished");
            endGame(sgs);
        }
        SetupRound(sgs, (SaboteurGameParameters) sgs.getGameParameters());
        endRound(sgs);
    }
    private void DisplayNuggetsWorth(SaboteurGameState sgs)
    {
        for(int i = 0; i < sgs.getNPlayers(); i++)
        {
            int value = 0;
            for(SaboteurCard card: sgs.playerNuggetDecks.get(i).getComponents())
            {
                value += card.nOfNuggets;
            }
            System.out.println("Player " + i + " has nuggets worth: " + value + " gold");
        }
    }

    private boolean NuggetExists(SaboteurGameState sgs, int targetValue)
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

    private void GiveSaboteurGold(SaboteurGameState sgs, int targetValue, int currentPlayer)
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
            if(NuggetExists(sgs, preferredNuggetValue))
            {
                for (int i = 0; i < sgs.nuggetDeck.getSize(); i++)
                {
                    if (sgs.nuggetDeck.peek(i).nOfNuggets == preferredNuggetValue)
                    {
                        System.out.println("Player " + currentPlayer + " has received a nugget worth " + preferredNuggetValue + " gold");
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
//--------------------------------------------------------------------------------------------------//

}