package games.descent2e;

import core.AbstractGameState;
import core.components.BoardNode;
import core.properties.PropertyInt;
import core.properties.PropertyString;
import core.turnorders.ReactiveTurnOrder;
import core.turnorders.TurnOrder;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import games.descent2e.concepts.Quest;
import utilities.Vector2D;

import java.util.*;

import static core.CoreConstants.GameResult.GAME_END;
import static core.CoreConstants.GameResult.GAME_ONGOING;
import static core.CoreConstants.playersHash;
import static core.CoreConstants.sizeHash;

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
        //System.out.println("Group size: " + groupSize);
        //System.out.println("Current monster: " + monsterActingNext);
        int next = 0;
        // Only looks for the next monster in the group as long as the group is not empty
        if (groupSize != 0) {
            next = (groupSize + monsterActingNext + 1) % groupSize;
            //System.out.println(next);
        }
        if ((next == 0 && monsterActingNext == groupSize-1) || groupSize == 0) {
            // Next monster group
            //System.out.println("Next monster group");
            nextMonsterGroup(dgs);
        } else {
            monsterActingNext = next;
            //System.out.println("Next monster: " + monsterActingNext);
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
            System.out.println("Round " + roundCounter);
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
        overlordCheckFatigue(dgs, true);
    }

    private void overlordEndTurn(DescentGameState dgs)
    {
        overlordCheckFatigue(dgs, false);
        checkReinforcements(dgs);
    }

    private void checkReinforcements(DescentGameState dgs)
    {
        // TODO

        String questName = dgs.getCurrentQuest().getName();
        String monsterName = "";
        List<String> monsters = dgs.getMonsterGroups();
        List<Integer> maxMonsters = dgs.getMonstersPerGroup();
        int i = 0;

        boolean canSpawn = false;
        int noToSpawn = 0;

        // Check for within board piece 9A's range
        String tile = "";

        switch(questName) {
            case "Acolyte of Saradyn":
                monsterName = "Goblin Archer";
                i = getMonsterGroupIndex(monsters, monsterName);
                if (i >= 0)
                {
                    if (dgs.getMonsters().get(i).size() < maxMonsters.get(i))
                    {
                        canSpawn = true;
                        tile = "4A";
                        noToSpawn = Math.min(maxMonsters.get(i) - dgs.getMonsters().get(i).size(), 2);
                    }
                }
                break;

            default:
                break;
        }

        if (canSpawn)
        {
            System.out.println("Spawning " + noToSpawn + " " + monsterName + "s");
            List<Vector2D> tileCoords = new ArrayList<>(dgs.gridReferences.get(tile).keySet());
            spawnReinforcements(dgs, i, noToSpawn, tileCoords);
        }
    }
    private int getMonsterGroupIndex(List<String> monsters, String monsterName) {
        int i = 0;
        for (String monster : monsters) {
            if (monster.contains(monsterName)) {
                return i;
            }
            i++;
        }
        return -1;
    }
    private void spawnReinforcements(DescentGameState dgs, int index, int noToSpawn, List<Vector2D> tileCoords)
    {
        boolean masterExists = false;     // Checks if a Master monster originally spawned
        boolean canSpawnMaster = true;    // Checks if the Master monster is dead
        boolean minionExists = false;     // Checks if a Minion monster originally spawned

        // Assuming spawning a Master takes priority over spawning a Minion
        // We assume that we can always spawn a Minion
        // As we will just return if we do not have enough monsters to spawn otherwise

        //Random rnd = new Random(dgs.getGameParameters().getRandomSeed());
        Random rnd = new Random();
        List<Monster> monstersOriginal = dgs.getOriginalMonsters().get(index);
        List<Monster> monsters = dgs.getMonsters().get(index);
        for (Monster monster : monstersOriginal)
        {
            // If the original set of monsters contains a Master, we should spawn a Master
            if (monster.getName().contains("master"))
            {
                masterExists = true;
            }
            // Likewise, if the original set of monsters contains a Minion, we should spawn a Minion
            if (monster.getName().contains("minion"))
            {
                minionExists = true;
            }

            // No use checking the rest of the list if we know we can legally spawn both
            if(masterExists && minionExists)
                break;
        }
        for (Monster monster : monsters)
        {
            // If the alive Monsters contain a Master, we cannot spawn a Master
            if (monster.getName().contains("master"))
            {
                canSpawnMaster = false;
                break;
            }
        }

        //System.out.println("Master exists: " + masterExists);
        //System.out.println("Can spawn master: " + canSpawnMaster);
        //System.out.println("Minion exists: " + minionExists);

        for (int k = 0; k < noToSpawn; k++)
        {
            // If the Master exists, it is always the first Monster on the list
            // The only time we do not look at the first Monster on the list is
            // if we can only spawn a Minion when a Master exists
            Monster originalMonster = monstersOriginal.get(k);

            // If the Master is alive, we cannot spawn a Master
            // So we spawn a Minion instead
            if (masterExists && !canSpawnMaster)
            {
                originalMonster = monstersOriginal.get(1);
            }

            Monster monster = originalMonster.copyNewID();


            // TODO: copied straight from DescentForwardModel's spawning
            String size = ((PropertyString)monster.getProperty(sizeHash)).value;
            int w = Integer.parseInt(size.split("x")[0]);
            int h = Integer.parseInt(size.split("x")[1]);
            monster.setSize(w, h);

            while (tileCoords.size() > 0) {
                Vector2D option = tileCoords.get(rnd.nextInt(tileCoords.size()));
                tileCoords.remove(option);
                BoardNode position = dgs.masterBoard.getElement(option.getX(), option.getY());
                if (position.getComponentName().equals("plain") &&
                        ((PropertyInt)position.getProperty(playersHash)).value == -1) {
                    // TODO: some monsters want to spawn in lava/water.
                    // This can be top-left corner, check if the other tiles are valid too
                    boolean canPlace = true;
                    for (int i = 0; i < h; i++) {
                        for (int j = 0; j < w; j++) {
                            if (i == 0 && j == 0) continue;
                            Vector2D thisTile = new Vector2D(option.getX() + j, option.getY() + i);
                            BoardNode tile = dgs.masterBoard.getElement(thisTile.getX(), thisTile.getY());
                            if (tile == null || !tile.getComponentName().equals("plain") ||
                                    !tileCoords.contains(thisTile) ||
                                    ((PropertyInt)tile.getProperty(playersHash)).value != -1) {
                                canPlace = false;
                            }
                        }
                    }
                    if (canPlace) {
                        monster.setPosition(option.copy());
                        PropertyInt prop = new PropertyInt("players", monster.getComponentID());
                        for (int i = 0; i < h; i++) {
                            for (int j = 0; j < w; j++) {
                                dgs.masterBoard.getElement(option.getX() + j, option.getY() + i).setProperty(prop);
                            }
                        }
                        if (canSpawnMaster)
                            canSpawnMaster = false;
                        dgs.monsters.get(index).add(monster);
                        System.out.println("Spawned " + monster.getName() + " at " + option.getX() + ", " + option.getY());
                        break;
                    }
                }
            }

        }
    }

    private void overlordCheckFatigue(DescentGameState dgs, boolean startOfTurn)
    {
        int changeFatigueBy = 0;

        String questName = dgs.getCurrentQuest().getName();

        switch(questName)
        {
            case "Acolyte of Saradyn":
                changeFatigueBy = fatigueCheckForAcolyteOfSaradyn(dgs);
                break;

            case "Rellegar's Rest":
                // Encounter 1
                // We only check at the start of the Overlord's turns here
                if(startOfTurn)
                    changeFatigueBy = 1;
                break;

            case "Rellegar's Rest E2":
                // Encounter 2
                break;

            case "Siege of Skytower":
                // We decrease the Heroes' Side Fatigue at the end of the Overlord's turn
                // But the Overlord's Fatigue increases for every monster that makes it to the Exit
                if (!startOfTurn)
                    heroesSideDecreaseFatigue(dgs, 1);
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
            overlordDecreaseFatigue(dgs, Math.abs(changeFatigueBy));
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

    private void heroesSideIncreaseFatigue (DescentGameState dgs, int increaseFatigueBy)
    {
        dgs.heroesSide.incrementAttribute(Figure.Attribute.Fatigue, increaseFatigueBy);
        System.out.println("Heroes' Side Fatigue increased to: " + dgs.heroesSide.getAttribute(Figure.Attribute.Fatigue));
    }

    private void heroesSideDecreaseFatigue (DescentGameState dgs, int decreaseFatigueBy)
    {
        dgs.heroesSide.decrementAttribute(Figure.Attribute.Fatigue, decreaseFatigueBy);
        System.out.println("Heroes' Side Fatigue decreased to: " + dgs.heroesSide.getAttribute(Figure.Attribute.Fatigue));
    }

    // TODO This is just the check for the first quest
    // I'm putting it here for now, but once more quests are implemented we should put it into its own contained class
    // To avoid cluttering this class
    private int fatigueCheckForAcolyteOfSaradyn(DescentGameState dgs)
    {
        boolean changeFatigue = false;
        int changeFatigueBy = 0;

        // Check for within board piece 9A's range
        String tile = "9A";
        List<Vector2D> tileCoords = new ArrayList<>(dgs.gridReferences.get(tile).keySet());

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
                    if (tileCoords.contains(position))
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
