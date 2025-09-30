package games.saboteur;

import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;
import games.saboteur.components.ActionCard;
import games.saboteur.components.PathCard;
import utilities.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static games.saboteur.components.ActionCard.ActionCardType.*;
import static games.saboteur.components.ActionCard.ToolCardType.*;

public class SaboteurGameParameters extends TunableParameters<SaboteurGameParameters> {
    public int goalSpacingX = 8; // the distance from start to goal cards (9 means there are 8 empty spaces between them)
    // the standard game has this set to 8, but 6 works better with general MCTS agents
    public int goalSpacingY = 1; // the gap between goal cards (1 means there is one empty space between them)
    int horizontalPadding = 2; // the number of spaces to left of start, and to right of goal cards
    int verticalPadding = 3; // the number of spaces above/below the top/bottom goal cards

    public int nGoals = 3;
    public int nTreasures = 1;
    public int mapCardsInDeck = 6;
    public int rockfallCardsInDeck = 3;

    //map combination of specific cards to number of cards in that deck
    public Map<Pair<PathCard.PathCardType, boolean[]>, Integer> pathCardDeck = new HashMap<>();
    public Map<Pair<ActionCard.ActionCardType, ActionCard.ToolCardType[]>, Integer> toolCards = new HashMap<>();


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
    public int[] goldSupply = new int[] {0, 16, 8, 4};


    public SaboteurGameParameters() {

        addTunableParameter("nGoals", 3);
        addTunableParameter("nTreasures", 1);
        addTunableParameter("goalSpacingX", 8);
        addTunableParameter("goalSpacingY", 1);
        addTunableParameter("nuggets_1", 16);
        addTunableParameter("nuggets_2", 8);
        addTunableParameter("nuggets_3", 4);
        addTunableParameter("mapCardsInDeck", 6);
        addTunableParameter("rockfallCardsInDeck", 3);
        addTunableParameter("horizontalPadding", 2);
        addTunableParameter("verticalPadding", 3);

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

    }


    @Override
    public void _reset() {
        goldSupply[1] = (int) getParameterValue("nuggets_1");
        goldSupply[2] = (int) getParameterValue("nuggets_2");
        goldSupply[3] = (int) getParameterValue("nuggets_3");
        nGoals = (int) getParameterValue("nGoals");
        nTreasures = (int) getParameterValue("nTreasures");
        goalSpacingX = (int) getParameterValue("goalSpacingX");
        goalSpacingY = (int) getParameterValue("goalSpacingY");
        mapCardsInDeck = (int) getParameterValue("mapCardsInDeck");
        rockfallCardsInDeck = (int) getParameterValue("rockfallCardsInDeck");
        horizontalPadding = (int) getParameterValue("horizontalPadding");
        verticalPadding = (int) getParameterValue("verticalPadding");
    }

    @Override
    protected SaboteurGameParameters _copy() {
        SaboteurGameParameters sgp = new SaboteurGameParameters();
        sgp.saboteursForPlayerCount = saboteursForPlayerCount.clone();
        sgp.minersForPlayerCount = minersForPlayerCount.clone();
        sgp.goldSupply = goldSupply.clone();
        sgp.cardsPerPlayer = cardsPerPlayer.clone();
        sgp.pathCardDeck = new HashMap<>();
        for (Map.Entry<Pair<PathCard.PathCardType, boolean[]>, Integer> entry : pathCardDeck.entrySet())
            sgp.pathCardDeck.put(new Pair<>(entry.getKey().a, entry.getKey().b.clone()), entry.getValue());
        sgp.toolCards = new HashMap<>();
        for (Map.Entry<Pair<ActionCard.ActionCardType, ActionCard.ToolCardType[]>, Integer> entry : toolCards.entrySet())
            sgp.toolCards.put(new Pair<>(entry.getKey().a, entry.getKey().b.clone()), entry.getValue());
        return sgp;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SaboteurGameParameters that = (SaboteurGameParameters) o;
        return  Arrays.equals(cardsPerPlayer, that.cardsPerPlayer) &&
                Objects.equals(pathCardDeck, that.pathCardDeck) && Objects.equals(toolCards, that.toolCards) &&
                Arrays.equals(saboteursForPlayerCount, that.saboteursForPlayerCount) &&
                Arrays.equals(minersForPlayerCount, that.minersForPlayerCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathCardDeck, toolCards, super.hashCode()) +
                Arrays.hashCode(saboteursForPlayerCount) + 31 * Arrays.hashCode(minersForPlayerCount) +
                31 * 31 * Arrays.hashCode(cardsPerPlayer);
    }

    @Override
    public SaboteurGameParameters instantiate() {
        return this;
    }
}
