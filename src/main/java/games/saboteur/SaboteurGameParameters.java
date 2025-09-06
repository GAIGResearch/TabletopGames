package games.saboteur;

import core.AbstractParameters;
import games.saboteur.components.ActionCard;
import games.saboteur.components.PathCard;
import utilities.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static games.saboteur.components.ActionCard.ActionCardType.*;
import static games.saboteur.components.ActionCard.ToolCardType.*;

public class SaboteurGameParameters extends AbstractParameters {
    public int nNuggets = 27;
    public int nGoalCards = 3;
    public int gridSize = 500;
    public int goalSpacingX = 8;
    public int goalSpacingY = 1;
    public int nGoals = 3;
    public int nTreasures = 1;
    public int nStartingCards = 5;

    //map combination of specific cards to number of cards in that deck
    public Map<Pair<PathCard.PathCardType, boolean[]>, Integer> pathCardDeck = new HashMap<>();
    public Map<Pair<ActionCard.ActionCardType, ActionCard.ToolCardType[]>, Integer> toolCards = new HashMap<>();
    public Map<ActionCard.ActionCardType, Integer> actionCards = new HashMap<>();
    public Map<Pair<PathCard.PathCardType, boolean[]>, Integer> goalCardDeck = new HashMap<>();
    public Map<Integer, Integer> goldNuggetDeck = new HashMap<>();

    //All RolesCards in a deck depending on number of players
    //nPlayers, nMiners, nSaboteurs
    //4	    3	1
    //5	    4	1
    //6	    4	2
    //7	    5	2
    //8	    5	3
    //9	    6	3
    //10	7	3
    //11    7	4
    public int[] saboteursForPlayerCount = new int[]{0, 0, 0, 1, 1, 2, 2, 3, 3, 3, 4};
    public int[] minersForPlayerCount = new int[]{0, 0, 0, 3, 4, 4, 5, 5, 6, 7, 8};


    public SaboteurGameParameters() {
        //All Path type cards in a deck excluding goal and start card
        PathCard.PathCardType edge = PathCard.PathCardType.Edge;
        PathCard.PathCardType path = PathCard.PathCardType.Path;

        pathCardDeck.put(new Pair<>(edge, new boolean[]{false, true, false, false}), 1);
        pathCardDeck.put(new Pair<>(edge, new boolean[]{false, false, true, false}), 1);
        pathCardDeck.put(new Pair<>(edge, new boolean[]{true, true, false, false}), 1);
        pathCardDeck.put(new Pair<>(edge, new boolean[]{false, false, true, true}), 1);
        pathCardDeck.put(new Pair<>(edge, new boolean[]{false, true, false, true}), 1);
        pathCardDeck.put(new Pair<>(edge, new boolean[]{false, true, true, false}), 1);
        pathCardDeck.put(new Pair<>(edge, new boolean[]{true, true, false, true}), 1);
        pathCardDeck.put(new Pair<>(edge, new boolean[]{true, false, true, true}), 1);
        pathCardDeck.put(new Pair<>(edge, new boolean[]{true, true, true, true}), 1);

        pathCardDeck.put(new Pair<>(path, new boolean[]{true, true, false, false}), 4);
        pathCardDeck.put(new Pair<>(path, new boolean[]{false, false, true, true}), 3);
        pathCardDeck.put(new Pair<>(path, new boolean[]{false, true, false, true}), 4);
        pathCardDeck.put(new Pair<>(path, new boolean[]{false, true, true, false}), 5);
        pathCardDeck.put(new Pair<>(path, new boolean[]{true, true, false, true}), 5);
        pathCardDeck.put(new Pair<>(path, new boolean[]{true, false, true, true}), 5);
        pathCardDeck.put(new Pair<>(path, new boolean[]{true, true, true, true}), 5);

        //All goal cards
        goalCardDeck.put(new Pair<>(PathCard.PathCardType.Goal, new boolean[]{true, true, true, true}), 5);

        toolCards.put(new Pair<>(BrokenTools, new ActionCard.ToolCardType[]{MineCart}), 3);
        toolCards.put(new Pair<>(BrokenTools, new ActionCard.ToolCardType[]{Lantern}), 3);
        toolCards.put(new Pair<>(BrokenTools, new ActionCard.ToolCardType[]{Pickaxe}), 3);
        toolCards.put(new Pair<>(FixTools, new ActionCard.ToolCardType[]{MineCart}), 2);
        toolCards.put(new Pair<>(FixTools, new ActionCard.ToolCardType[]{Lantern}), 2);
        toolCards.put(new Pair<>(FixTools, new ActionCard.ToolCardType[]{Pickaxe}), 2);
        toolCards.put(new Pair<>(FixTools, new ActionCard.ToolCardType[]{MineCart, Lantern}), 1);
        toolCards.put(new Pair<>(FixTools, new ActionCard.ToolCardType[]{Lantern, Pickaxe}), 1);
        toolCards.put(new Pair<>(FixTools, new ActionCard.ToolCardType[]{Pickaxe, MineCart}), 1);

        actionCards.put(Map, 3);
        actionCards.put(RockFall, 6);

        //Nugget cards
        goldNuggetDeck.put(3, 4);
        goldNuggetDeck.put(2, 8);
        goldNuggetDeck.put(1, 16);
    }

    @Override
    protected AbstractParameters _copy() {
        SaboteurGameParameters sgp = new SaboteurGameParameters();
        sgp.nNuggets = nNuggets;
        sgp.nGoalCards = nGoalCards;
        sgp.gridSize = gridSize;
        sgp.goalSpacingY = goalSpacingY;
        sgp.goalSpacingX = goalSpacingX;
        sgp.nGoals = nGoals;
        sgp.nTreasures = nTreasures;
        sgp.nStartingCards = nStartingCards;
        sgp.saboteursForPlayerCount = saboteursForPlayerCount;
        sgp.minersForPlayerCount = minersForPlayerCount;
        sgp.pathCardDeck = new HashMap<>();
        for (Map.Entry<Pair<PathCard.PathCardType, boolean[]>, Integer> entry : pathCardDeck.entrySet())
            sgp.pathCardDeck.put(new Pair<>(entry.getKey().a, entry.getKey().b.clone()), entry.getValue());
        sgp.toolCards = new HashMap<>();
        for (Map.Entry<Pair<ActionCard.ActionCardType, ActionCard.ToolCardType[]>, Integer> entry : toolCards.entrySet())
            sgp.toolCards.put(new Pair<>(entry.getKey().a, entry.getKey().b.clone()), entry.getValue());
        sgp.actionCards = new HashMap<>(actionCards);
        sgp.goalCardDeck = new HashMap<>();
        for (Map.Entry<Pair<PathCard.PathCardType, boolean[]>, Integer> entry : goalCardDeck.entrySet())
            sgp.goalCardDeck.put(new Pair<>(entry.getKey().a, entry.getKey().b.clone()), entry.getValue());
        sgp.goldNuggetDeck = new HashMap<>(goldNuggetDeck);
        return sgp;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaboteurGameParameters that = (SaboteurGameParameters) o;
        return nNuggets == that.nNuggets && nGoalCards == that.nGoalCards &&
                gridSize == that.gridSize && goalSpacingX == that.goalSpacingX && goalSpacingY == that.goalSpacingY &&
                nGoals == that.nGoals && nTreasures == that.nTreasures && nStartingCards == that.nStartingCards &&
                Objects.equals(pathCardDeck, that.pathCardDeck) && Objects.equals(toolCards, that.toolCards) &&
                Objects.equals(actionCards, that.actionCards) && Objects.equals(goalCardDeck, that.goalCardDeck) &&
                Objects.equals(goldNuggetDeck, that.goldNuggetDeck) && Arrays.equals(saboteursForPlayerCount, that.saboteursForPlayerCount) &&
                Arrays.equals(minersForPlayerCount, that.minersForPlayerCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nNuggets, nGoalCards, gridSize, goalSpacingX, goalSpacingY, nGoals, nTreasures,
                nStartingCards, pathCardDeck, toolCards, actionCards, goalCardDeck, goldNuggetDeck) +
                Arrays.hashCode(saboteursForPlayerCount) + 31 * Arrays.hashCode(minersForPlayerCount);
    }
}
