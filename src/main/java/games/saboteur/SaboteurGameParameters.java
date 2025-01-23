package games.saboteur;

import core.AbstractParameters;
import games.saboteur.components.ActionCard;
import games.saboteur.components.PathCard;
import games.saboteur.components.RoleCard;
import utilities.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SaboteurGameParameters extends AbstractParameters
{
    public int nPlayers          = 10;
    public int nNuggets          = 27;
    public int nGoalCards        = 3;
    public int nRounds           = 3;
    public int gridSize = 500;
    public int goalSpacingX = 8;
    public int goalSpacingY = 1;
    public int nGoals = 3;
    public int nTreasures = 1;
    public int nStartingCards = 5;

    //map combination of specific cards to number of cards in that deck
    public Map<Pair<PathCard.PathCardType,boolean[]>, Integer> pathCardDeck= new HashMap<>();
    public Map<RoleCard.RoleCardType, Integer> roleCardDeck = new HashMap<>();
    public Map<Pair<ActionCard.ActionCardType, ActionCard.ToolCardType[]>, Integer> toolCards = new HashMap<>();
    public Map<ActionCard.ActionCardType, Integer> actionCards = new HashMap<>();
    public Map<Pair<PathCard.PathCardType,boolean[]>, Integer> goalCardDeck = new HashMap<>();
    public Map<Integer, Integer> goldNuggetDeck = new HashMap<>();

    public SaboteurGameParameters ()
    {
        //All Path type cards in a deck excluding goal and start card
        PathCard.PathCardType edge = PathCard.PathCardType.Edge;
        PathCard.PathCardType path = PathCard.PathCardType.Path;

        pathCardDeck.put(new Pair<>(edge, new boolean[]{false, true , false, false}), 1);
        pathCardDeck.put(new Pair<>(edge, new boolean[]{false, false, true , false}), 1);
        pathCardDeck.put(new Pair<>(edge, new boolean[]{true , true , false, false}), 1);
        pathCardDeck.put(new Pair<>(edge, new boolean[]{false, false, true , true }), 1);
        pathCardDeck.put(new Pair<>(edge, new boolean[]{false, true , false, true }), 1);
        pathCardDeck.put(new Pair<>(edge, new boolean[]{false, true , true , false}), 1);
        pathCardDeck.put(new Pair<>(edge, new boolean[]{true , true , false, true }), 1);
        pathCardDeck.put(new Pair<>(edge, new boolean[]{true , false, true , true }), 1);
        pathCardDeck.put(new Pair<>(edge, new boolean[]{true , true , true , true }), 1);

        pathCardDeck.put(new Pair<>(path, new boolean[]{true , true , false, false}), 4);
        pathCardDeck.put(new Pair<>(path, new boolean[]{false, false, true , true }), 3);
        pathCardDeck.put(new Pair<>(path, new boolean[]{false, true , false, true }), 4);
        pathCardDeck.put(new Pair<>(path, new boolean[]{false, true , true , false}), 5);
        pathCardDeck.put(new Pair<>(path, new boolean[]{true , true , false, true }), 5);
        pathCardDeck.put(new Pair<>(path, new boolean[]{true , false, true , true }), 5);
        pathCardDeck.put(new Pair<>(path, new boolean[]{true , true , true , true }), 5);

        //All goal cards
        goalCardDeck.put(new Pair<>(PathCard.PathCardType.Goal, new boolean[]{true, true, true, true}), 5);

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
        int nMiners;
        int nSaboteurs = switch (nPlayers) {
            case 3 -> {
                nMiners = 3;
                yield 1;
            }
            case 4 -> {
                nMiners = 4;
                yield 1;
            }
            case 5 -> {
                nMiners = 4;
                yield 2;
            }
            case 6 -> {
                nMiners = 5;
                yield 2;
            }
            case 7 -> {
                nMiners = 5;
                yield 3;
            }
            case 8 -> {
                nMiners = 6;
                yield 3;
            }
            case 9 -> {
                nMiners = 7;
                yield 3;
            }
            case 10 -> {
                nMiners = 7;
                yield 4;
            }
            default -> {
                nMiners = -1;
                yield -1;
            }
        };
        roleCardDeck.put(RoleCard.RoleCardType.GoldMiner, nMiners);
        roleCardDeck.put(RoleCard.RoleCardType.Saboteur, nSaboteurs);

        //All Actions Cards
        ActionCard.ToolCardType mineCart = ActionCard.ToolCardType.MineCart;
        ActionCard.ToolCardType lantern = ActionCard.ToolCardType.Lantern;
        ActionCard.ToolCardType pickaxe = ActionCard.ToolCardType.Pickaxe;

        ActionCard.ActionCardType brokenTools = ActionCard.ActionCardType.BrokenTools;
        ActionCard.ActionCardType fixTools = ActionCard.ActionCardType.FixTools;
        ActionCard.ActionCardType map = ActionCard.ActionCardType.Map;
        ActionCard.ActionCardType rockFall = ActionCard.ActionCardType.RockFall;

        toolCards.put(new Pair<>(brokenTools, new ActionCard.ToolCardType[]{mineCart}), 3);
        toolCards.put(new Pair<>(brokenTools, new ActionCard.ToolCardType[]{lantern}), 3);
        toolCards.put(new Pair<>(brokenTools, new ActionCard.ToolCardType[]{pickaxe}), 3);
        toolCards.put(new Pair<>(fixTools, new ActionCard.ToolCardType[]{mineCart}), 2);
        toolCards.put(new Pair<>(fixTools, new ActionCard.ToolCardType[]{lantern}), 2);
        toolCards.put(new Pair<>(fixTools, new ActionCard.ToolCardType[]{pickaxe}), 2);
        toolCards.put(new Pair<>(fixTools, new ActionCard.ToolCardType[]{mineCart,lantern}), 1);
        toolCards.put(new Pair<>(fixTools, new ActionCard.ToolCardType[]{lantern, pickaxe}), 1);
        toolCards.put(new Pair<>(fixTools, new ActionCard.ToolCardType[]{pickaxe, mineCart}), 1);

        actionCards.put(map, 3);
        actionCards.put(rockFall, 6);

        //Nugget cards
        goldNuggetDeck.put(3, 4);
        goldNuggetDeck.put(2, 8);
        goldNuggetDeck.put(1, 16);
    }

    @Override
    protected AbstractParameters _copy()
    {
        SaboteurGameParameters sgp = new SaboteurGameParameters();
        sgp.nPlayers = nPlayers;
        sgp.nNuggets = nNuggets;
        sgp.nGoalCards = nGoalCards;
        sgp.nRounds = nRounds;
        sgp.gridSize = gridSize;
        sgp.goalSpacingY = goalSpacingY;
        sgp.goalSpacingX = goalSpacingX;
        sgp.nGoals = nGoals;
        sgp.nTreasures = nTreasures;
        sgp.nStartingCards = nStartingCards;
        sgp.pathCardDeck = new HashMap<>();
        for (Map.Entry<Pair<PathCard.PathCardType, boolean[]>, Integer> entry : pathCardDeck.entrySet())
            sgp.pathCardDeck.put(new Pair<>(entry.getKey().a, entry.getKey().b.clone()), entry.getValue());
        sgp.roleCardDeck = new HashMap<>(roleCardDeck);
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
        return nPlayers == that.nPlayers && nNuggets == that.nNuggets && nGoalCards == that.nGoalCards && nRounds == that.nRounds && gridSize == that.gridSize && goalSpacingX == that.goalSpacingX && goalSpacingY == that.goalSpacingY && nGoals == that.nGoals && nTreasures == that.nTreasures && nStartingCards == that.nStartingCards && Objects.equals(pathCardDeck, that.pathCardDeck) && Objects.equals(roleCardDeck, that.roleCardDeck) && Objects.equals(toolCards, that.toolCards) && Objects.equals(actionCards, that.actionCards) && Objects.equals(goalCardDeck, that.goalCardDeck) && Objects.equals(goldNuggetDeck, that.goldNuggetDeck);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nPlayers, nNuggets, nGoalCards, nRounds, gridSize, goalSpacingX, goalSpacingY, nGoals, nTreasures, nStartingCards, pathCardDeck, roleCardDeck, toolCards, actionCards, goalCardDeck, goldNuggetDeck);
    }
}
