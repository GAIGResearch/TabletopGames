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
    public int goalSpacingX = 3;
    public int goalSpacingY = 1;
    public int nGoals = 3;
    public int nTreasures = 1;

    //map combination of specific cards to number of cards in that deck
    public Map<Pair<PathCard.PathCardType, boolean[]>, Integer> pathCardDeck = new HashMap<>();
    public Map<Pair<ActionCard.ActionCardType, ActionCard.ToolCardType[]>, Integer> toolCards = new HashMap<>();
    public Map<Integer, Integer> goldNuggetDeck = new HashMap<>();


    // TODO: Map and Rockfall cards are not implemented (well, there is some code, but action cards are never added to the deck)
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
    public int[] cardsPerPlayer = new int[]{0, 0, 0, 6, 6, 6, 5, 5, 4, 4, 4};


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

        toolCards.put(new Pair<>(BrokenTools, new ActionCard.ToolCardType[]{MineCart}), 3);
        toolCards.put(new Pair<>(BrokenTools, new ActionCard.ToolCardType[]{Lantern}), 3);
        toolCards.put(new Pair<>(BrokenTools, new ActionCard.ToolCardType[]{Pickaxe}), 3);
        toolCards.put(new Pair<>(FixTools, new ActionCard.ToolCardType[]{MineCart}), 2);
        toolCards.put(new Pair<>(FixTools, new ActionCard.ToolCardType[]{Lantern}), 2);
        toolCards.put(new Pair<>(FixTools, new ActionCard.ToolCardType[]{Pickaxe}), 2);
        toolCards.put(new Pair<>(FixTools, new ActionCard.ToolCardType[]{MineCart, Lantern}), 1);
        toolCards.put(new Pair<>(FixTools, new ActionCard.ToolCardType[]{Lantern, Pickaxe}), 1);
        toolCards.put(new Pair<>(FixTools, new ActionCard.ToolCardType[]{Pickaxe, MineCart}), 1);

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
        sgp.goalSpacingY = goalSpacingY;
        sgp.goalSpacingX = goalSpacingX;
        sgp.nGoals = nGoals;
        sgp.nTreasures = nTreasures;
        sgp.saboteursForPlayerCount = saboteursForPlayerCount;
        sgp.minersForPlayerCount = minersForPlayerCount;
        sgp.cardsPerPlayer = cardsPerPlayer;
        sgp.pathCardDeck = new HashMap<>();
        for (Map.Entry<Pair<PathCard.PathCardType, boolean[]>, Integer> entry : pathCardDeck.entrySet())
            sgp.pathCardDeck.put(new Pair<>(entry.getKey().a, entry.getKey().b.clone()), entry.getValue());
        sgp.toolCards = new HashMap<>();
        for (Map.Entry<Pair<ActionCard.ActionCardType, ActionCard.ToolCardType[]>, Integer> entry : toolCards.entrySet())
            sgp.toolCards.put(new Pair<>(entry.getKey().a, entry.getKey().b.clone()), entry.getValue());
        sgp.goldNuggetDeck = new HashMap<>(goldNuggetDeck);
        return sgp;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaboteurGameParameters that = (SaboteurGameParameters) o;
        return nNuggets == that.nNuggets && nGoalCards == that.nGoalCards &&
                goalSpacingX == that.goalSpacingX && goalSpacingY == that.goalSpacingY &&
                nGoals == that.nGoals && nTreasures == that.nTreasures && Arrays.equals(cardsPerPlayer, that.cardsPerPlayer) &&
                Objects.equals(pathCardDeck, that.pathCardDeck) && Objects.equals(toolCards, that.toolCards) &&
                Objects.equals(goldNuggetDeck, that.goldNuggetDeck) && Arrays.equals(saboteursForPlayerCount, that.saboteursForPlayerCount) &&
                Arrays.equals(minersForPlayerCount, that.minersForPlayerCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nNuggets, nGoalCards, goalSpacingX, goalSpacingY, nGoals, nTreasures,
                pathCardDeck, toolCards, goldNuggetDeck) +
                Arrays.hashCode(saboteursForPlayerCount) + 31 * Arrays.hashCode(minersForPlayerCount) +
                31 * 31 * Arrays.hashCode(cardsPerPlayer);
    }
}
