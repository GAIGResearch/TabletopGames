package games.saboteur;

import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;
import games.saboteur.components.ActionCard;
import games.saboteur.components.PathCard;
import games.saboteur.components.RoleCard;
import games.saboteur.components.SaboteurCard;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SaboteurGameParameters extends TunableParameters
{
    public int nPlayers          = 10;
    public int nNuggets          = 27;
    public int nGoalCards        = 3;
    public int nRounds           = 3;
    public int GridSize          = 37;
    public int GoalSpacingX      = 8;
    public int GoalSpacingY      = 1;

    //map combination of specific cards to number of cards in that deck
    public Map<SaboteurCard, Integer> pathCardDeck= new HashMap<>();
    public Map<SaboteurCard, Integer> roleCardDeck = new HashMap<>();
    public Map<SaboteurCard, Integer> actionCardDeck = new HashMap<>();
    public Map<SaboteurCard, Integer> goalCardDeck = new HashMap<>();
    public Map<SaboteurCard, Integer> goldNuggetDeck = new HashMap<>();

    public SaboteurGameParameters (long seed)
    {
        super(seed);
        addTunableParameter("nPlayers", 5,Arrays.asList(3,4,5,6,7));
        addTunableParameter("nNuggets", 44,Arrays.asList(44,88,132,176));
        addTunableParameter("nGoalCards", 3,Arrays.asList(3,6,9,12));
        addTunableParameter("nRounds", 3,Arrays.asList(3,6,9,12));
        addTunableParameter("GridSize", 37,Arrays.asList(37,53,69,85));
        addTunableParameter("GoalSpacingX", 7,Arrays.asList(7,14,21,28));
        addTunableParameter("GoalSpacingY", 1,Arrays.asList(1,2,3,4));

        //All Path type cards in a deck excluding goal and start card
        PathCard.PathCardType edge = PathCard.PathCardType.Edge;
        PathCard.PathCardType path = PathCard.PathCardType.Path;

        pathCardDeck.put(new PathCard(edge, new boolean[]{false, true , false, false}), 1);
        pathCardDeck.put(new PathCard(edge, new boolean[]{false, false, true , false}), 1);
        pathCardDeck.put(new PathCard(edge, new boolean[]{true , true , false, false}), 1);
        pathCardDeck.put(new PathCard(edge, new boolean[]{false, false, true , true }), 1);
        pathCardDeck.put(new PathCard(edge, new boolean[]{false, true , false, true }), 1);
        pathCardDeck.put(new PathCard(edge, new boolean[]{false, true , true , false}), 1);
        pathCardDeck.put(new PathCard(edge, new boolean[]{true , true , false, true }), 1);
        pathCardDeck.put(new PathCard(edge, new boolean[]{true , false, true , true }), 1);
        pathCardDeck.put(new PathCard(edge, new boolean[]{true , true , true , true }), 1);

        pathCardDeck.put(new PathCard(path, new boolean[]{true , true , false, false}), 4);
        pathCardDeck.put(new PathCard(path, new boolean[]{false, false, true , true }), 3);
        pathCardDeck.put(new PathCard(path, new boolean[]{false, true , false, true }), 4);
        pathCardDeck.put(new PathCard(path, new boolean[]{false, true , true , false}), 5);
        pathCardDeck.put(new PathCard(path, new boolean[]{true , true , false, true }), 5);
        pathCardDeck.put(new PathCard(path, new boolean[]{true , false, true , true }), 5);
        pathCardDeck.put(new PathCard(path, new boolean[]{true , true , true , true }), 5);

        //All goal cards
        goalCardDeck.put(new PathCard(PathCard.PathCardType.Goal, new boolean[]{true, true, true, true}), 5);

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
        int nSaboteurs;
        switch(nPlayers)
        {
            case 3:
                nMiners = 3;
                nSaboteurs = 1;
                break;
            case 4:
                nMiners = 4;
                nSaboteurs = 1;
                break;
            case 5:
                nMiners = 4;
                nSaboteurs = 2;
                break;
            case 6:
                nMiners = 5;
                nSaboteurs = 2;
                break;
            case 7:
                nMiners = 5;
                nSaboteurs = 3;
                break;
            case 8:
                nMiners = 6;
                nSaboteurs = 3;
                break;
            case 9:
                nMiners = 7;
                nSaboteurs = 3;
                break;
            case 10:
                nMiners = 7;
                nSaboteurs = 4;
                break;
            default:
                nMiners = -1;
                nSaboteurs = -1;
        }
        roleCardDeck.put(new RoleCard(RoleCard.RoleCardType.GoldMiner), nMiners);
        roleCardDeck.put(new RoleCard(RoleCard.RoleCardType.Saboteur), nSaboteurs);

        //All Actions Cards
        ActionCard.ToolCardType mineCart = ActionCard.ToolCardType.MineCart;
        ActionCard.ToolCardType lantern = ActionCard.ToolCardType.Lantern;
        ActionCard.ToolCardType pickaxe = ActionCard.ToolCardType.Pickaxe;

        ActionCard.ActionCardType brokenTools = ActionCard.ActionCardType.BrokenTools;
        ActionCard.ActionCardType fixTools = ActionCard.ActionCardType.FixTools;
        ActionCard.ActionCardType map = ActionCard.ActionCardType.Map;
        ActionCard.ActionCardType rockFall = ActionCard.ActionCardType.RockFall;

        actionCardDeck.put(new ActionCard(brokenTools, mineCart), 3);
        actionCardDeck.put(new ActionCard(brokenTools, lantern), 3);
        actionCardDeck.put(new ActionCard(brokenTools, pickaxe), 3);

        actionCardDeck.put(new ActionCard(fixTools, mineCart), 2);
        actionCardDeck.put(new ActionCard(fixTools, lantern), 2);
        actionCardDeck.put(new ActionCard(fixTools, pickaxe), 2);
        actionCardDeck.put(new ActionCard(fixTools, new ActionCard.ToolCardType[]{mineCart,lantern}), 1);
        actionCardDeck.put(new ActionCard(fixTools, new ActionCard.ToolCardType[]{lantern, pickaxe}), 1);
        actionCardDeck.put(new ActionCard(fixTools, new ActionCard.ToolCardType[]{pickaxe, mineCart}), 1);

        actionCardDeck.put(new ActionCard(map), 3);
        actionCardDeck.put(new ActionCard(rockFall), 6);

        //Nugget cards
        goldNuggetDeck.put(new SaboteurCard(3), 4);
        goldNuggetDeck.put(new SaboteurCard(2), 8);
        goldNuggetDeck.put(new SaboteurCard(1), 16);
    }

    @Override
    protected AbstractParameters _copy()
    {
        SaboteurGameParameters sgp = new SaboteurGameParameters(System.currentTimeMillis());
        sgp.nPlayers = nPlayers;
        sgp.nNuggets = nNuggets;
        sgp.nGoalCards = nGoalCards;
        sgp.nRounds = nRounds;
        sgp.GridSize = GridSize;
        sgp.GoalSpacingY = GoalSpacingY;
        sgp.GoalSpacingX = GoalSpacingX;
        return sgp;
    }

    @Override
    protected boolean _equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof SaboteurGameParameters)) return false;
        if (!super.equals(o)) return false;
        SaboteurGameParameters that = (SaboteurGameParameters) o;
        return nPlayers == that.nPlayers &&
                nNuggets == that.nNuggets &&
                nGoalCards == that.nGoalCards &&
                nRounds == that.nRounds &&
                GridSize == that.GridSize &&
                GoalSpacingX == that.GoalSpacingX &&
                GoalSpacingY == that.GoalSpacingY;
    }

    @Override
    public Object instantiate()
    {
        //to be completed later
        return null;
    }

    @Override
    public void _reset()
    {
        nPlayers = (int) getParameterValue("nPlayers");
        nNuggets = (int) getParameterValue("nNuggets");
        nGoalCards = (int) getParameterValue("nGoalCards");
        nRounds = (int) getParameterValue("nRounds");
        GridSize = (int) getParameterValue("GridSize");
        GoalSpacingX = (int) getParameterValue("GoalSpacingX");
        GoalSpacingY = (int) getParameterValue("GoalSpacingY");
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(),
                nPlayers,
                nNuggets,
                nGoalCards,
                nRounds,
                GridSize,
                GoalSpacingX,
                GoalSpacingY);
    }
}
