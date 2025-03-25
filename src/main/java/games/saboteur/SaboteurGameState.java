package games.saboteur;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.components.PartialObservableGridBoard;
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
    PartialObservableGridBoard gridBoard;
    Deck<SaboteurCard> nuggetDeck;

    Set<Vector2D> pathCardOptions;
    int centerOfGrid;

    int nOfMiners;
    int nOfSaboteurs;

    public SaboteurGameState(AbstractParameters parameters, int nPlayers)
    {
        super(parameters, nPlayers);
        playerDecks = new ArrayList<>();
        toolDeck = new ArrayList<>();
        pathCardOptions = new HashSet<>();
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

        //copying brokenToolsDeck
        copy.toolDeck = new ArrayList<>();
        for(Map<ActionCard.ToolCardType, Boolean> brokenToolDeck : toolDeck)
        {
            copy.toolDeck.add(new HashMap<>(brokenToolDeck));
        }
        copy.goalDeck = goalDeck.copy();

        copy.pathCardOptions = new HashSet<>();
        for(Vector2D pathCardOption : pathCardOptions)
        {
            copy.pathCardOptions.add(pathCardOption.copy());
        }

        copy.centerOfGrid = centerOfGrid;
        copy.nOfMiners = nOfMiners;
        copy.nOfSaboteurs = nOfSaboteurs;

        copy.discardDeck = discardDeck.copy();
        copy.roleDeck = roleDeck.copy();
        copy.nuggetDeck = nuggetDeck.copy();
        copy.drawDeck = drawDeck.copy();

        // Board
        copy.gridBoard = gridBoard.emptyCopy();
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; j < gridBoard.getWidth(); j++) {
                PathCard c = (PathCard) gridBoard.getElement(j, i);
                if (c == null) continue;
                copy.gridBoard.setElement(j, i, c.copy());
                for (int p = 0; p < nPlayers; p++) {
                    copy.gridBoard.setElementVisibility(j, i, p, gridBoard.getElementVisibility(j, i, p));
                }
            }
        }

        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // Player cards
            copy.playerDecks = new ArrayList<>();
            for (int i = 0; i < playerDecks.size(); i++) {
                copy.playerDecks.add(playerDecks.get(i).copy());
                if (i != playerId) {
                    copy.drawDeck.add(copy.playerDecks.get(i));
                    copy.playerDecks.get(i).clear();
                }
            }
            copy.drawDeck.shuffle(redeterminisationRnd);
            for (int i = 0; i < playerDecks.size(); i++) {
                if (i != playerId) {
                    for (int j = 0; j < playerDecks.get(i).getSize(); j++) {
                        copy.playerDecks.get(i).add(copy.drawDeck.pick(0));
                    }
                }
            }

            // Nuggets
            copy.playerNuggetDecks = new ArrayList<>();
            for (int i = 0; i < playerNuggetDecks.size(); i++) {
                copy.playerNuggetDecks.add(playerNuggetDecks.get(i).copy());
                if (i != playerId) {
                    copy.nuggetDeck.add(copy.playerNuggetDecks.get(i));
                    copy.playerNuggetDecks.get(i).clear();
                }
            }
            copy.nuggetDeck.shuffle(redeterminisationRnd);
            for (int i = 0; i < playerNuggetDecks.size(); i++) {
                if (i != playerId) {
                    for (int j = 0; j < playerNuggetDecks.get(i).getSize(); j++) {
                        copy.playerNuggetDecks.get(i).add(copy.nuggetDeck.pick(0));
                    }
                }
            }

            // Goals on board, shuffle those unseen by current player
            for (int i = 0; i < gridBoard.getHeight(); i++) {
                for (int j = 0; j < gridBoard.getWidth(); j++) {
                    PathCard c = (PathCard) gridBoard.getElement(j, i);
                    if (c == null || c.type != PathCard.PathCardType.Goal) continue;
                    if (!copy.gridBoard.getElementVisibility(j, i, playerId)) {
                        copy.goalDeck.add((SaboteurCard) copy.gridBoard.getElement(j, i));
                    }
                }
            }
            if (copy.goalDeck.getSize() > 0) {
                copy.goalDeck.shuffle(redeterminisationRnd);
                for (int i = 0; i < gridBoard.getHeight(); i++) {
                    for (int j = 0; j < gridBoard.getWidth(); j++) {
                        PathCard c = (PathCard) gridBoard.getElement(j, i);
                        if (c == null || c.type != PathCard.PathCardType.Goal) continue;
                        if (!copy.gridBoard.getElementVisibility(j, i, playerId)) {
                            copy.gridBoard.setElement(j, i, copy.goalDeck.pick(0));
                        }
                    }
                }
            }

            // TODO: Discards should only be shuffled if not known - and if not known, then they should
            // TODO: be shuffled into the draw deck for redistribution and not just shuffled in place

            // Shuffle discard and role deck to hide info. Current player should have same role
            copy.discardDeck.shuffle(redeterminisationRnd);
            SaboteurCard rc = copy.roleDeck.pick(playerId);
            copy.roleDeck.shuffle(redeterminisationRnd);
            copy.roleDeck.add(rc, playerId);
        } else {
            //copying playerDecks
            copy.playerDecks = new ArrayList<>();
            for(Deck<SaboteurCard> playerDeck : playerDecks)
            {
                copy.playerDecks.add(playerDeck.copy());
            }

            // Nuggets
            copy.playerNuggetDecks = new ArrayList<>();
            for (Deck<SaboteurCard> playerNuggetDeck : playerNuggetDecks)
            {
                copy.playerNuggetDecks.add(playerNuggetDeck.copy());
            }
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

    public Set<Vector2D> getPathCardOptions() {
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

    public PartialObservableGridBoard getGridBoard() {
        return gridBoard;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return getGameScore(playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        int sum = 0;
        for (SaboteurCard card: playerNuggetDecks.get(playerId)) sum += card.nOfNuggets;
        return sum;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SaboteurGameState that)) return false;
        return centerOfGrid == that.centerOfGrid &&
                nOfMiners == that.nOfMiners &&
                nOfSaboteurs == that.nOfSaboteurs &&
                Objects.equals(playerDecks, that.playerDecks) &&
                Objects.equals(toolDeck, that.toolDeck) &&
                Objects.equals(roleDeck, that.roleDeck) &&
                Objects.equals(playerNuggetDecks, that.playerNuggetDecks) &&
                Objects.equals(drawDeck, that.drawDeck) &&
                Objects.equals(discardDeck, that.discardDeck) &&
                Objects.equals(goalDeck, that.goalDeck) &&
                Objects.equals(gridBoard, that.gridBoard) &&
                Objects.equals(nuggetDeck, that.nuggetDeck) &&
                Objects.equals(pathCardOptions, that.pathCardOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerDecks, toolDeck, roleDeck, playerNuggetDecks,
                drawDeck, discardDeck, goalDeck, gridBoard, nuggetDeck, pathCardOptions,
                centerOfGrid, nOfMiners, nOfSaboteurs);
    }

    @Override
    public String toString() {
        // we take each of the 13 hash coded and convert them to a string separated by |
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                playerDecks.hashCode(), toolDeck.hashCode(), roleDeck.hashCode(), playerNuggetDecks.hashCode(),
                drawDeck.hashCode(), discardDeck.hashCode(), goalDeck.hashCode(), gridBoard.hashCode(), nuggetDeck.hashCode(),
                pathCardOptions.hashCode(), centerOfGrid, nOfMiners, nOfSaboteurs);
    }

}
