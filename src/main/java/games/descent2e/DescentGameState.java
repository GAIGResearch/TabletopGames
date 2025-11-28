package games.descent2e;

import core.AbstractGameState;
import core.AbstractParameters;
import core.actions.AbstractAction;
import core.components.*;
import core.interfaces.IPrintable;
import evaluation.listeners.IGameListener;
import games.GameType;
import games.descent2e.actions.DescentAction;
import games.descent2e.components.*;
import games.descent2e.components.tokens.DToken;
import games.descent2e.actions.Triggers;
import games.descent2e.components.tokens.Door;
import games.descent2e.concepts.Quest;
import utilities.Pair;
import utilities.Vector2D;

import java.util.*;
import java.util.stream.Collectors;

import static games.GameType.Descent2e;

public class DescentGameState extends AbstractGameState implements IPrintable {

    DescentGameData data;

    // For reference only
    // Mapping from board node ID in board configuration to tile configuration
    Map<Integer, GridBoard> tiles;
    // int corresponds to component ID of tile at that location in master board
    int[][] tileReferences;
    // Mapping from tile name to list of coordinates in master board for each cell (and corresponding coordinates on original tile)
    Map<String, Map<Vector2D, Vector2D>> gridReferences;
    boolean initData;
    boolean setup = true;
    boolean reinforcing = false;

    Deck<Card> searchCards;
    Deck<Card> act1ShopCards;
    Deck<Card> act2ShopCards;
    Deck<Card> relicCards;
    GridBoard masterBoard;
    DicePool attackDicePool;
    DicePool defenceDicePool;
    DicePool attributeDicePool;
    List<Hero> heroes;
    List<Door> doors;
    Figure overlord;
    Figure heroesSide;
    List<List<Monster>> monsters;
    List<List<Monster>> monstersOriginal;
    List<Integer> monstersPerGroup;
    List<String> monsterGroups;
    List<Integer> webMonstersIDs;   // Monsters with the Web passive ability
    int overlordPlayer;
    ArrayList<DToken> tokens;
    Quest currentQuest;
    List<Figure> activated;         // Figures activated this turn
    List<Figure> defeated;          // Figured defeated
    List<Monster> reinforcements;   // Reinforcements to spawn
    List<String> reinforcementPositions;
    List<Pair<String, String>> defeatedFigures;
    List<String> interruptAttacks;

    // old Turn Order fields
    int monsterGroupActingNext;
    int monsterActingNext;
    int heroActingNext;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players for this game.
     */
    public DescentGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        nTeams = 2; // Two teams in Descent - the Heroes, and the Overlord
        tiles = new HashMap<>();
        data = new DescentGameData();
        attackDicePool = new DicePool(Collections.emptyList());
        defenceDicePool = new DicePool(Collections.emptyList());
        attributeDicePool = new DicePool(Collections.emptyList());

        doors = new ArrayList<>();
        heroes = new ArrayList<>();
        monsters = new ArrayList<>();
        monstersOriginal = new ArrayList<>();
        monstersPerGroup = new ArrayList<>();
        monsterGroups = new ArrayList<>();
        webMonstersIDs = new ArrayList<>();
        activated = new ArrayList<>();
        defeatedFigures = new ArrayList<>();
        defeated = new ArrayList<>();
        reinforcements = new ArrayList<>();
        reinforcementPositions = new ArrayList<>();
        interruptAttacks = new ArrayList<>();
    }

    @Override
    protected GameType _getGameType() {
        return Descent2e;
    }

    @Override
    protected List<Component> _getAllComponents() {
        ArrayList<Component> components = new ArrayList<>();
        if (!initData) {
            components.addAll(data.decks);
            components.addAll(data.tiles);
            components.addAll(data.heroes);
            components.addAll(data.boardConfigurations);
            for (HashMap<String, Monster> m : data.monsters.values()) {
                components.addAll(m.values());
            }
            initData = true;
        }

        // Current state
        components.add(masterBoard);
        if (tokens != null) {
            components.addAll(tokens);
        }
        components.add(searchCards);
        components.addAll(heroes);
        for (Hero hero : heroes) {
            components.addAll(hero.getSkills().getComponents());
            components.addAll(hero.getHandEquipment().getComponents());
            components.add(hero.getArmor());
            components.addAll(hero.getOtherEquipment().getComponents());
            components.addAll(hero.getInventory().getComponents());
        }
        monsters.forEach(components::addAll);
        monstersOriginal.forEach(components::addAll);
        components.addAll(defeated);
        components.add(overlord);
        components.addAll(doors);
        //components.add(heroesSide);   // TODO This breaks it for some reason
        // TODO
        return components;
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        DescentGameState copy = new DescentGameState(gameParameters, getNPlayers());
        copy.data = data.copy();
        copy.tiles = new HashMap<>();
        for (Map.Entry<Integer, GridBoard> e : tiles.entrySet()) {
            copy.tiles.put(e.getKey(), e.getValue().copy());
        }
        copy.masterBoard = masterBoard.copy();
        copy.attackDicePool = attackDicePool.copy();
        copy.defenceDicePool = defenceDicePool.copy();
        copy.attributeDicePool = attributeDicePool.copy();
        copy.overlord = overlord.copy();
        copy.heroesSide = heroesSide.copy();
        copy.heroes = new ArrayList<>();
        for (Hero f : heroes) {
            copy.heroes.add(f.copy());
        }
        copy.doors = new ArrayList<>();
        for (Door d : doors) {
            copy.doors.add(d.copy());
        }
        copy.monsters = new ArrayList<>();
        for (List<Monster> ma : monsters) {
            List<Monster> maC = new ArrayList<>();
            for (Monster m : ma) {
                maC.add(m.copy());
            }
            copy.monsters.add(maC);
        }
        for (List<Monster> ma : monstersOriginal) {
            List<Monster> maC = new ArrayList<>();
            for (Monster m : ma) {
                maC.add(m.copy());
            }
            copy.monstersOriginal.add(maC);
        }
        copy.monstersPerGroup = new ArrayList<>();
        copy.monstersPerGroup.addAll(monstersPerGroup);
        copy.monsterGroups = new ArrayList<>();
        copy.monsterGroups.addAll(monsterGroups);
        copy.webMonstersIDs = new ArrayList<>();
        copy.webMonstersIDs.addAll(webMonstersIDs);
        copy.tileReferences = tileReferences.clone();
        copy.gridReferences = new HashMap<>();
        for (Map.Entry<String, Map<Vector2D, Vector2D>> e : gridReferences.entrySet()) {
            HashMap<Vector2D, Vector2D> map = new HashMap<>(e.getValue());
            copy.gridReferences.put(e.getKey(), map);
        }
        copy.initData = initData;
        copy.setup = setup;
        copy.reinforcing = reinforcing;
        copy.tokens = new ArrayList<>();
        for (DToken t : tokens) {
            copy.tokens.add(t.copy());
        }
        copy.searchCards = searchCards.copy();
        copy.act1ShopCards = act1ShopCards.copy();
        copy.act2ShopCards = act2ShopCards.copy();
        copy.relicCards = relicCards.copy();
        copy.currentQuest = currentQuest;  // TODO does this need to be deep? it (should be) read-only after data parsing
        copy.defeatedFigures = new ArrayList<>();
        for (Pair<String, String> p : defeatedFigures) {
            copy.defeatedFigures.add(new Pair<>(p.a, p.b));
        }
        copy.activated = new ArrayList<>();
        copy.activated.addAll(activated);
        copy.defeated = new ArrayList<>();
        copy.defeated.addAll(defeated);
        copy.reinforcements = new ArrayList<>();
        copy.reinforcements.addAll(reinforcements);
        copy.reinforcementPositions = new ArrayList<>();
        copy.reinforcementPositions.addAll(reinforcementPositions);
        copy.interruptAttacks.addAll(interruptAttacks);
        copy.monsterActingNext = monsterActingNext;
        copy.monsterGroupActingNext = monsterGroupActingNext;
        copy.heroActingNext = heroActingNext;
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (inSetup()) return 0.0;          // Everyone gets a blanket score of 0.0 during Setup
        return new DescentHeuristic().evaluateState(this, playerId);
        //return 0.0;
    }

    public List<Double> getHeuristicValues(int playerId) {
        return new DescentHeuristic().getHeuristics(this, playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        double retValue = 0.0;
        String questName = getCurrentQuest().getName();
        double isOverlord = playerId == overlordPlayer ? -1.0 : 1.0;

        switch (questName) {
            case "Acolyte of Saradyn":
                // What the Heroes need to win
                int barghestsDefeated = monstersOriginal.get(1).size() - monsters.get(1).size();

                // What the Overlord needs to win
                int overlordFatigue = overlord.getAttributeValue(Figure.Attribute.Fatigue);
                int heroesDefeated = 0;
                for (Hero h : getHeroes()) {
                    if (h.isDefeated())
                        heroesDefeated++;
                }

                // The score is admittedly arbitrary, but it's a start
                // The Overlord wants to keep the Heroes from defeating the Barghests
                // The Overlord wants to defeat the Heroes
                // The Overlord wants to raise their Fatigue as much as possible
                // Likewise, the Heroes want to defeat the Barghests, keep themselves alive, and keep the Overlord's Fatigue low

                double barghestScore = isOverlord * (10.0 * barghestsDefeated / monstersOriginal.get(1).size());
                double heroesScore = isOverlord * (5.0 * heroesDefeated / getHeroes().size());
                double fatigueScore = isOverlord * (5.0 * overlordFatigue / overlord.getAttributeMax(Figure.Attribute.Fatigue));
                retValue = (barghestScore) - (heroesScore + fatigueScore);
                retValue += 10.0 * getPlayerResults()[playerId].value;
                break;
            default:
                break;
        }

        return retValue;
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        // TODO IDs of all components that are not visible to the player
        // e.g. Overlord cards in their hands and deck (which aren't implemented in this branch yet)
        return new ArrayList<>();
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DescentGameState that)) return false;
        if (!super.equals(o)) return false;
        return initData == that.initData &&
                setup == that.setup &&
                reinforcing == that.reinforcing &&
                overlordPlayer == that.overlordPlayer &&
                Objects.equals(data, that.data) && Objects.equals(tiles, that.tiles) &&
                Arrays.deepEquals(tileReferences, that.tileReferences) &&
                Objects.equals(gridReferences, that.gridReferences) &&
                Objects.equals(searchCards, that.searchCards) &&
                Objects.equals(act1ShopCards, that.act1ShopCards) &&
                Objects.equals(act2ShopCards, that.act2ShopCards) &&
                Objects.equals(relicCards, that.relicCards) &&
                Objects.equals(masterBoard, that.masterBoard) &&
                Objects.equals(attackDicePool, that.attackDicePool) &&
                Objects.equals(defenceDicePool, that.defenceDicePool) &&
                Objects.equals(attributeDicePool, that.attributeDicePool) &&
                Objects.equals(heroes, that.heroes) && Objects.equals(overlord, that.overlord) &&
                Objects.equals(monsters, that.monsters) && Objects.equals(tokens, that.tokens) &&
                Objects.equals(doors, that.doors) && Objects.equals(monstersOriginal, that.monstersOriginal) &&
                Objects.equals(monstersPerGroup, that.monstersPerGroup) &&
                Objects.equals(monsterGroups, that.monsterGroups) &&
                Objects.equals(webMonstersIDs, that.webMonstersIDs) &&
                Objects.equals(currentQuest, that.currentQuest) &&
                Objects.equals(activated, that.activated) &&
                Objects.equals(defeatedFigures, that.defeatedFigures) &&
                Objects.equals(defeated, that.defeated) &&
                Objects.equals(reinforcements, that.reinforcements) &&
                Objects.equals(reinforcementPositions, that.reinforcementPositions) &&
                Objects.equals(interruptAttacks, that.interruptAttacks) &&
                monsterActingNext == that.monsterActingNext &&
                heroActingNext == that.heroActingNext &&
                monsterGroupActingNext == that.monsterGroupActingNext;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), data, tiles, gridReferences, initData, setup, reinforcing, searchCards, act1ShopCards, act2ShopCards, relicCards,
                masterBoard, attackDicePool, defenceDicePool, attributeDicePool, heroes, overlord, doors, heroesSide,
                monsters, monstersOriginal, monstersPerGroup, monsterGroups, webMonstersIDs, overlordPlayer, tokens, currentQuest, activated,
                defeatedFigures, defeated, reinforcements, reinforcementPositions, interruptAttacks, monsterActingNext, heroActingNext, monsterGroupActingNext);
        result = 31 * result + Arrays.deepHashCode(tileReferences);
        return result;
    }

    @Override
    public int[] hashCodeArray() {
        return new int[]{
                data.hashCode(),
                tiles.hashCode(),
                gridReferences.hashCode(),
                initData ? 1 : 0,
                searchCards.hashCode(),
                act1ShopCards.hashCode(),
                act2ShopCards.hashCode(),
                relicCards.hashCode(),
                masterBoard.hashCode(),
                attackDicePool.hashCode(),
                defenceDicePool.hashCode(),
                attributeDicePool.hashCode(),
                heroes.hashCode(),
                doors.hashCode(),
                overlord.hashCode(),
                heroesSide.hashCode(),
                monsters.hashCode(),
                monstersOriginal.hashCode(),
                monstersPerGroup.hashCode(),
                monsterGroups.hashCode(),
                webMonstersIDs.hashCode(),
                overlordPlayer,
                tokens.hashCode(),
                currentQuest.hashCode(),
                activated.hashCode(),
                defeatedFigures.hashCode(),
                defeated.hashCode(),
                reinforcements.hashCode(),
                reinforcementPositions.hashCode(),
                interruptAttacks.hashCode(),
                Arrays.deepHashCode(tileReferences)
        };
    }

    public DescentGameData getData() {
        return data;
    }

    public GridBoard getMasterBoard() {
        return masterBoard;
    }

    public List<Hero> getHeroes() {
        return heroes;
    }

    public Hero getHeroByName(String name) {
        for (Hero h : heroes) {
            if (h.getName().contains(name)) {
                return h;
            }
        }
        return null;
    }

    public DicePool getAttackDicePool() {
        return attackDicePool;
    }

    public DicePool getDefenceDicePool() {
        return defenceDicePool;
    }

    public DicePool getAttributeDicePool() {
        return attributeDicePool;
    }

    public void setAttackDicePool(DicePool pool) {
        attackDicePool = pool;
    }

    public void setDefenceDicePool(DicePool pool) {
        defenceDicePool = pool;
    }

    public void setAttributeDicePool(DicePool pool) {
        attributeDicePool = pool;
    }

    public void addActivated(Figure f) {
        activated.add(f);
    }

    public List<Figure> getActivated() {
        return activated;
    }

    public void addDefeated(Figure f) {
        defeated.add(f);
    }
    public void removeDefeated(Figure f) {
        defeated.remove(f);
    }

    public List<String> getReinforcementPositions() {
        return reinforcementPositions;
    }

    public List<Monster> getReinforcements() {
        return reinforcements;
    }

    public void addReinforcement(Monster f, String position) {
        reinforcements.add(f);
        reinforcementPositions.add(position);
    }

    public void removeReinforcement(Monster f) {
        reinforcementPositions.remove(reinforcementPositions.get(reinforcements.indexOf(f)));
        reinforcements.remove(f);
    }

    public void clearReinforcements() {
        reinforcements.clear();
        reinforcementPositions.clear();
    }

    public List<List<Monster>> getMonsters() {
        return monsters;
    }

    public List<List<Monster>> getOriginalMonsters() {
        return monstersOriginal;
    }

    public List<Integer> getMonstersPerGroup() {
        return monstersPerGroup;
    }

    public List<String> getMonsterGroups() {
        return monsterGroups;
    }

    public List<Monster> getCurrentMonsterGroup()
    {
        return getMonsters().get(monsterGroupActingNext);
    }
    public List<Integer> getWebMonstersIDs() {
        return webMonstersIDs;
    }

    public Deck<Card> getSearchCards() {
        return searchCards;
    }

    public Deck<Card> getAct1ShopCards() {
        return act1ShopCards;
    }

    public Deck<Card> getAct2ShopCards() {
        return act2ShopCards;
    }

    public Deck<Card> getRelicCards() {
        return relicCards;
    }

    public boolean inSetup() {
        return setup;
    }

    public void endSetup() {
        setup = false;
    }

    public boolean canReinforce() {
        return reinforcing;
    }

    public void startReinforcements() {
        reinforcing = true;
    }

    public void endReinforcing() {
        reinforcing = false;
    }

    public void setRoundCounter(int roundCounter) {
        this.roundCounter = roundCounter;
    }

    /*
    This method is fine when we are circulating through each hero and monster in turn for taking their actions
    It will not necessarily suffice when we have interrupt actions. Consider the situation in which one player has two
    heroes, and one of the heroes has an ability that allows a monster move to be interrupted.
    In this case the Action (interrupt Move ability) should encapsulate within it the Figure that is
    executing the action (if this is at all relevant).
     */
    public Figure getActingFigure() {
        // Find currently acting figure (hero or monster)
        Figure actingFigure;
        // TODO Ensure monsterGroup deletes itself when empty
        if (getCurrentPlayer() != overlordPlayer) {
            // If hero player, get corresponding hero
            actingFigure = heroes.get(heroActingNext);
        } else {
            // Find current monster group + monster playing
            List<Monster> monsterGroup = getCurrentMonsterGroup();

            // Otherwise, monster is playing
            if (monsterActingNext == -1) {
                throw new AssertionError("Monster index not set");
            }
            if (monsterActingNext >= monsterGroup.size()) {
                throw new AssertionError("Monster group index out of bounds");
            }
            actingFigure = monsterGroup.get(monsterActingNext);
        }
        return actingFigure;
    }

    public int getTeam(int player) {
        if (player == overlordPlayer)
            return 1;
        else
            return 0;
    }

    public int getOverlordPlayer() {
        return overlordPlayer;
    }

    public Figure getOverlord() {
        return overlord;
    }


    public int nextMonster() {

        if (!reinforcements.isEmpty()) {
            Monster monster = getMonsters().get(monsterGroupActingNext).get(monsterActingNext);
            if (reinforcements.contains(monster)) {
                int index = reinforcements.indexOf(monster);
                if (index + 1 >= reinforcements.size()) {
                    clearReinforcements();
                    return -1;
                }
                Monster next = reinforcements.get(index + 1);
                for (List<Monster> mon: monsters) {
                    if (mon.contains(next))
                        return mon.indexOf(next);
                }
            }
        }


        // Otherwise, proceed as normal
        do {
            int groupSize = getCurrentMonsterGroup().size();
            // Only looks for the next monster in the group as long as the group is not empty
            if (groupSize == 0) {
                // all dead
                monsterGroupActingNext = nextMonsterGroup();
                monsterActingNext = -1;
            } else if (monsterActingNext == groupSize - 1) {
                // finished with this group
                monsterGroupActingNext = nextMonsterGroup();
                monsterActingNext = -1;
            } else {
                return (groupSize + monsterActingNext + 1) % groupSize;
            }
        } while (monsterGroupActingNext > -1);
        return -1;
    }
    public void teamkill()
    {
        // If this function is called then somehow we've killed a Monster of the same group indexed before the actingFigure
        // So, as nextMonster() will return the next monster in the group
        // We need to go two steps back before we can go one step forwards
        monsterActingNext -= 2;
        monsterActingNext = nextMonster();
    }

    public int nextMonsterGroup() {
        int nMonsters = getMonsters().size();
        // Only looks for the next monster group as long as there are still groups in play
        if (nMonsters == 0) {
            return -1;  // no more monsters
        } else if (monsterGroupActingNext == nMonsters - 1) {
            return -1;  // no more monsters
        } else {
            return (nMonsters + monsterGroupActingNext + 1) % nMonsters;
        }
    }

    public List<AbstractAction> getInterruptActionsFor(int player, Triggers trigger) {
        List<DescentAction> descentActions;
        if (player == overlordPlayer) {
            // we run through monsters
            descentActions = monsters.stream().flatMap(List::stream)
                    .flatMap(m -> m.getAbilities().stream())
                    .collect(Collectors.toList());
        } else {
            // else we just look at heroes that belong to the acting Figure
            // TODO: Add in effects from cards in the player's hand
            // We rely on canExecute() to filter out irrelevant ones
            descentActions = heroes.stream().filter(h -> h.getOwnerId() == player)
                    .flatMap(h -> h.getAbilities().stream())
                    .collect(Collectors.toList());
        }
        // Then filter to just the ones applicable to this Trigger point that are executable
        List<AbstractAction> retValue = descentActions.stream()
                .filter(a -> a.canExecute(trigger, this))
                .map(a -> (AbstractAction) a).collect(Collectors.toList());
        return retValue;
    }

    public int[][] getTileReferences() {
        return tileReferences;
    }

    public Map<String, Map<Vector2D, Vector2D>> getGridReferences() {
        return gridReferences;
    }

    public Map<Integer, GridBoard> getTiles() {
        return tiles;
    }

    public List<DToken> getTokens() {
        return tokens;
    }

    public List<Door> getDoors() {
        return doors;
    }

    public Quest getCurrentQuest() {
        return currentQuest;
    }

    public void addDefeatedFigure(Figure target, int index1, Figure attacker, int index2) {
        String defeated = target.getName();
        defeated += " (" + target.getComponentID() + ");" + index1;

        String defeatedBy = attacker.getName();
        defeatedBy += " (" + attacker.getComponentID() + ");" + index2;

        defeatedFigures.add(new Pair<>(defeated, defeatedBy));
    }

    public void addDefeatedFigure(Figure target, int index1, String killedBy) {
        String defeated = target.getName();
        defeated += " (" + target.getComponentID() + ");" + index1;

        String defeatedBy = killedBy + ";-1";

        defeatedFigures.add(new Pair<>(defeated, defeatedBy));
    }

    public List<Pair<String, String>> getDefeatedFigures() {
        return defeatedFigures;
    }

    public void clearDefeatedFigures() {
        defeatedFigures.clear();
    }

    public List<String> getInterruptAttacks() {
        return interruptAttacks;
    }

    public boolean hasInterruptAttack(String attack) {
        return interruptAttacks.contains(attack);
    }

    public void addInterruptAttack(String attack) {
        interruptAttacks.add((attack));
    }

    public void removeInterruptAttack(String attack) {
        interruptAttacks.remove(attack);
    }

    public void clearInterruptAttacks() {
        interruptAttacks.clear();
    }

    @Override
    public String toString() {
        return masterBoard.toString();
    }

    @Override
    public void addListener(IGameListener listener) {
        super.addListener(listener);
        listeners.add(listener);
    }

    void addComponents() {
        super.addAllComponents();
    }
}
