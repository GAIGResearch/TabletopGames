package games.saboteur;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.GameType;
import games.saboteur.components.*;
import utilities.Vector2D;

import java.util.*;

public class SaboteurGameState extends AbstractGameState
{
    List<Deck<SaboteurCard>> playerDecks;
    List<Map<ActionCard.ToolCardType, Boolean>> toolDeck;  // for each player, if that tool is functional (true) or broken (false)
    PartialObservableDeck<SaboteurCard> roleDeck; // add list for roles as well due to visibility when copying
    List<Deck<SaboteurCard>> playerNuggetDecks;

    Deck<SaboteurCard> drawDeck;
    Deck<SaboteurCard> discardDeck;
    Deck<SaboteurCard> goalDeck;
    PartialObservableGridBoard<PathCard> gridBoard;
    Deck<SaboteurCard> nuggetDeck;

    List<Vector2D> pathCardOptions;
    int centerOfGrid;

    int nOfMiners;
    int nOfSaboteurs;

    public SaboteurGameState(AbstractParameters parameters, int nPlayers)
    {
        super(parameters, nPlayers);
        playerDecks = new ArrayList<>();
        toolDeck = new ArrayList<>();
        pathCardOptions = new ArrayList<>();
        playerNuggetDecks = new ArrayList<>();
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Saboteur;
    }

    @Override
    protected List<Component> _getAllComponents()
    {
        return new ArrayList<>()
        {{
            addAll(playerDecks);
            add(drawDeck);
            add(discardDeck);
            add(goalDeck);
            add(roleDeck);
            add(gridBoard);
            add(nuggetDeck);
            addAll(playerNuggetDecks);

        }};
    }



    @Override
    protected SaboteurGameState _copy(int playerId)
    {
        SaboteurGameState copy = new SaboteurGameState(gameParameters.copy(), getNPlayers());

        //copying playerDecks
        copy.playerDecks = new ArrayList<>();
        for(Deck<SaboteurCard> playerDeck : playerDecks)
        {
            copy.playerDecks.add(playerDeck.copy());
        }

        //copying brokenToolsDeck
        copy.toolDeck = new ArrayList<>();
        for(Map<ActionCard.ToolCardType, Boolean> brokenToolDeck : toolDeck)
        {
            copy.toolDeck.add(new HashMap<>(brokenToolDeck));
        }

        copy.drawDeck = drawDeck.copy();
        copy.discardDeck = discardDeck.copy();
        copy.goalDeck = goalDeck.copy();
        copy.roleDeck = roleDeck.copy();
        copy.nuggetDeck = nuggetDeck.copy();
        copy.gridBoard = gridBoard.emptyCopy();
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; j < gridBoard.getWidth(); j++) {
                PathCard c = gridBoard.getElement(j, i);
                if (c == null) continue;
                copy.gridBoard.setElement(j, i, c.copy());
                for (int p = 0; p < nPlayers; p++) {
                    copy.gridBoard.setElementVisibility(j, i, p, gridBoard.getElementVisibility(j, i, p));
                }
            }
        }

        copy.playerNuggetDecks = new ArrayList<>();
        for (Deck<SaboteurCard> playerNuggetDeck : playerNuggetDecks)
        {
            copy.playerNuggetDecks.add(playerNuggetDeck.copy());
        }

        copy.pathCardOptions = new ArrayList<>();
        for(Vector2D pathCardOption : pathCardOptions)
        {
            copy.pathCardOptions.add(pathCardOption.copy());
        }

        copy.playerNuggetDecks = new ArrayList<>();
        for(Deck<SaboteurCard> playerNuggetDeck : playerNuggetDecks)
        {
            copy.playerNuggetDecks.add(playerNuggetDeck.copy());
        }

        copy.centerOfGrid = centerOfGrid;
        copy.nOfMiners = nOfMiners;
        copy.nOfSaboteurs = nOfSaboteurs;

        // todo partial observability
        if (playerId != -1 && getCoreGameParameters().partialObservable) {

        }

        return copy;
    }

    public Deck<SaboteurCard> getDiscardDeck() {
        return discardDeck;
    }

    public Deck<SaboteurCard> getDrawDeck() {
        return drawDeck;
    }

    public Deck<SaboteurCard> getGoalDeck() {
        return goalDeck;
    }

    public Deck<SaboteurCard> getNuggetDeck() {
        return nuggetDeck;
    }

    public List<Deck<SaboteurCard>> getPlayerDecks() {
        return playerDecks;
    }

    public List<Deck<SaboteurCard>> getPlayerNuggetDecks() {
        return playerNuggetDecks;
    }

    public List<Vector2D> getPathCardOptions() {
        return pathCardOptions;
    }

    public PartialObservableDeck<SaboteurCard> getRoleDeck() {
        return roleDeck;
    }

    public RoleCard.RoleCardType getRole(int playerId)
    {
        return ((RoleCard) roleDeck.peek(playerId)).type;
    }

    public List<Map<ActionCard.ToolCardType, Boolean>> getToolDeck() {
        return toolDeck;
    }

    public boolean isToolFunctional(int playerId, ActionCard.ToolCardType toolType)
    {
        return toolDeck.get(playerId).get(toolType);
    }

    public void setToolFunctional(int playerId, ActionCard.ToolCardType toolType, boolean isFunctional)
    {
        toolDeck.get(playerId).put(toolType, isFunctional);
    }

    public boolean isAnyToolBroken (int playerId)
    {
        for (Map.Entry<ActionCard.ToolCardType, Boolean> entry : toolDeck.get(playerId).entrySet())
        {
            if (!entry.getValue())
            {
                return true;
            }
        }
        return false;
    }

    public PartialObservableGridBoard<PathCard> getGridBoard() {
        return gridBoard;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return getGameScore(playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        return 0;  // todo nuggets
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SaboteurGameState that)) return false;
        return centerOfGrid == that.centerOfGrid && nOfMiners == that.nOfMiners && nOfSaboteurs == that.nOfSaboteurs && Objects.equals(playerDecks, that.playerDecks) && Objects.equals(toolDeck, that.toolDeck) && Objects.equals(roleDeck, that.roleDeck) && Objects.equals(playerNuggetDecks, that.playerNuggetDecks) && Objects.equals(drawDeck, that.drawDeck) && Objects.equals(discardDeck, that.discardDeck) && Objects.equals(goalDeck, that.goalDeck) && Objects.equals(gridBoard, that.gridBoard) && Objects.equals(nuggetDeck, that.nuggetDeck) && Objects.equals(pathCardOptions, that.pathCardOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerDecks, toolDeck, roleDeck, playerNuggetDecks, drawDeck, discardDeck, goalDeck, gridBoard, nuggetDeck, pathCardOptions, centerOfGrid, nOfMiners, nOfSaboteurs);
    }

}
