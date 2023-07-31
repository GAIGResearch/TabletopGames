package games.descent2e;

import core.AbstractGameState;
import core.turnorders.ReactiveTurnOrder;
import core.turnorders.TurnOrder;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import games.descent2e.concepts.Quest;
import utilities.Vector2D;

import java.util.LinkedList;
import java.util.List;

import static core.CoreConstants.GameResult.GAME_END;
import static core.CoreConstants.GameResult.GAME_ONGOING;

// Order is all heroes (controlled by their owner ID player), then all monsters by monster group (controlled by overlord)
public class DescentTurnOrder extends ReactiveTurnOrder {
    int monsterGroupActingNext;
    int monsterActingNext;
    int heroFigureActingNext;

    public DescentTurnOrder(int nPlayers) {
        super(nPlayers);
    }

    @Override
    protected void _reset() {
        super._reset();
        monsterGroupActingNext = 0;
        monsterActingNext = 0;
        heroFigureActingNext = 0;
    }

    public int getMonsterGroupActingNext() {
        return monsterGroupActingNext;
    }

    public int getHeroFigureActingNext() {
        return heroFigureActingNext;
    }

    public int getMonsterActingNext() {
        return monsterActingNext;
    }

    public void nextMonster(DescentGameState dgs) {
        int groupSize = dgs.getMonsters().get(monsterGroupActingNext).size();
        int next = 0;
        // Only looks for the next monster in the group as long as the group is not empty
        if (groupSize != 0) {
            next = (groupSize + monsterActingNext + 1) % groupSize;
        }
        if ((next == 0 && monsterActingNext == groupSize-1) || groupSize == 0) {
            // Next monster group
            nextMonsterGroup(dgs);
        } else {
            monsterActingNext = next;
        }
    }
    public void nextMonsterGroup(DescentGameState dgs) {
        int nMonsters = dgs.getMonsters().size();
        int next = 0;
        // Only looks for the next monster group as long as there are still groups in play
        if (nMonsters != 0) {
            next = (nMonsters + monsterGroupActingNext + 1) % nMonsters;
        }
        monsterActingNext = 0;
        if ((next == 0 && monsterGroupActingNext == nMonsters-1) || nMonsters == 0) {
            monsterGroupActingNext = 0;
            heroFigureActingNext = 0;
            turnOwner = dgs.heroes.get(heroFigureActingNext).getOwnerId();

        } else {
            monsterGroupActingNext = next;
        }
    }

    // Here this really means "end figure turn"
    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
        if (gameState.getGameStatus() != GAME_ONGOING) return;
        DescentGameState dgs = (DescentGameState) gameState;
        int nFigures = dgs.getMonsters().stream().mapToInt(List::size).sum() + dgs.getHeroes().size();

        // TODO Any figure that ends its turn in a lava/hazard space is immediately defeated.
        //  Heroes that are defeated in this way place their hero token in the nearest empty space
        //  (from where they were defeated) that does not contain lava/hazard. A large monster is immediately defeated
        //  only if all spaces it occupies are lava spaces.

        // TODO end-of-turn abilities

        turnCounter++;
        if (turnCounter >= nFigures)
        {
            if (turnOwner == ((DescentGameState)gameState).overlordPlayer)
            {
                // End of Overlord's turn
                overlordEndTurn((DescentGameState)gameState);
            }
            endRound(gameState);
        }
        else {
            turnOwner = nextPlayer(gameState);

            int n = 0;
            while (gameState.getPlayerResults()[turnOwner] != GAME_ONGOING) {
                turnOwner = nextPlayer(gameState);
                n++;
                if (n >= nPlayers) {
                    gameState.setGameStatus(GAME_END);
                    break;
                }
            }
        }
    }

    @Override
    public int nextPlayer(AbstractGameState gameState) {
        if (turnOwner == ((DescentGameState)gameState).overlordPlayer) {
            // Move to next monster, or next monster group
            nextMonster((DescentGameState) gameState);
            // Always return overlord, player turn init on end round when all monsters are finished
            return ((DescentGameState)gameState).overlordPlayer;
        } else {
            // Return next hero, or overlord if we cycled back
            int nHeroes = ((DescentGameState)gameState).heroes.size();
            int next = (nHeroes + heroFigureActingNext +1)%nHeroes;
            if (next == 0 && heroFigureActingNext == nHeroes-1)
            {
                // Start of Overlord's turn
                overlordStartTurn((DescentGameState)gameState);
                return ((DescentGameState)gameState).overlordPlayer;
            }

            else {
                heroFigureActingNext = next;
                return ((DescentGameState)gameState).heroes.get(next).getOwnerId();
            }
        }
    }

    @Override
    public void _endRound(AbstractGameState gameState) {

        // Reset figures for the next round
        DescentGameState dgs = (DescentGameState) gameState;
        for (Figure f: dgs.getHeroes()) {
            f.resetRound();
        }
        for (List<Monster> mList: dgs.getMonsters()) {
            for (Monster m: mList) {
                m.resetRound();
            }
        }
        dgs.overlord.resetRound();
        monsterGroupActingNext = 0;
        monsterActingNext = 0;
        heroFigureActingNext = 0;
    }

    @Override
    protected TurnOrder _copy() {
        DescentTurnOrder pto = new DescentTurnOrder(nPlayers);
        pto.reactivePlayers = new LinkedList<>(reactivePlayers);
        pto.monsterActingNext = monsterActingNext;
        pto.monsterGroupActingNext = monsterGroupActingNext;
        pto.heroFigureActingNext = heroFigureActingNext;
        return pto;
    }

    private void overlordStartTurn(DescentGameState dgs)
    {
        overlordCheckFatigue(dgs);
    }

    private void overlordEndTurn(DescentGameState dgs)
    {
        overlordCheckFatigue(dgs);
    }
    private void overlordCheckFatigue(DescentGameState dgs)
    {
        int changeFatigueBy = 0;

        String questName = dgs.getCurrentQuest().getName();

        switch(questName)
        {
            case "Acolyte of Saradyn":
                changeFatigueBy = fatigueCheckForAcolyteOfSaradyn(dgs);
                break;

            default:
                break;
        }

        // We only need to change the Overlord's Fatigue if we have met the conditions to increase or decrease
        if (changeFatigueBy > 0)
        {
            overlordIncreaseFatigue(dgs, changeFatigueBy);
        }
        else if (changeFatigueBy < 0)
        {
            overlordDecreaseFatigue(dgs, changeFatigueBy);
        }
    }

    private void overlordIncreaseFatigue (DescentGameState dgs, int increaseFatigueBy)
    {
        dgs.overlord.incrementAttribute(Figure.Attribute.Fatigue, increaseFatigueBy);
        System.out.println("Overlord's Fatigue increased to: " + dgs.overlord.getAttribute(Figure.Attribute.Fatigue));
    }

    private void overlordDecreaseFatigue (DescentGameState dgs, int decreaseFatigueBy)
    {
        dgs.overlord.decrementAttribute(Figure.Attribute.Fatigue, decreaseFatigueBy);
        System.out.println("Overlord's Fatigue decreased to: " + dgs.overlord.getAttribute(Figure.Attribute.Fatigue));
    }

    // TODO This is just the check for the first quest
    // I'm putting it here for now, but once more quests are implemented we should put it into its own contained class
    // To avoid cluttering this class
    private int fatigueCheckForAcolyteOfSaradyn(DescentGameState dgs)
    {
        boolean changeFatigue = false;
        int changeFatigueBy = 0;

        Vector2D min = new Vector2D(3, 14);
        Vector2D max = new Vector2D(6, 17);

        List<List<Monster>> monsters = dgs.getMonsters();
        for (List<Monster> mList: monsters)
        {

            if(changeFatigue)
            {
                break;
            }

            for (Monster m : mList)
            {
                if (m.getName().contains("Goblin Archer"))
                {
                    Vector2D position = m.getPosition();
                    if ((min.getX() <= position.getX() && position.getX() <= max.getX())
                            && (min.getY() <= position.getY() && position.getY() <= max.getY()))
                    {
                        changeFatigue = true;
                        changeFatigueBy = 1;
                        break;
                    }
                }
            }
        }

        return changeFatigueBy;
    }
}
