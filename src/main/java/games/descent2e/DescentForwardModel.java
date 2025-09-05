package games.descent2e;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.*;
import core.components.Component;
import core.properties.*;
import evaluation.metrics.Event;
import games.descent2e.DescentTypes.*;
import games.descent2e.abilities.HeroAbilities;
import games.descent2e.actions.*;
import games.descent2e.actions.attack.*;
import games.descent2e.actions.conditions.Diseased;
import games.descent2e.actions.conditions.Poisoned;
import games.descent2e.actions.conditions.Stunned;
import games.descent2e.actions.herofeats.*;
import games.descent2e.actions.tokens.TokenAction;
import games.descent2e.components.*;
import games.descent2e.components.tokens.DToken;
import games.descent2e.concepts.GameOverCondition;
import games.descent2e.concepts.HeroicFeat;
import games.descent2e.concepts.Quest;
import utilities.Pair;
import utilities.Vector2D;

import java.awt.*;
import java.util.List;
import java.util.*;

import static core.CoreConstants.*;
import static games.descent2e.DescentConstants.*;
import static games.descent2e.DescentHelper.*;
import static games.descent2e.actions.archetypeskills.ArchetypeSkills.getArchetypeSkillActions;
import static games.descent2e.actions.monsterfeats.MonsterAbilities.getMonsterActions;
import static games.descent2e.components.DicePool.constructDicePool;
import static games.descent2e.components.Figure.Attribute.Health;
import static utilities.Utils.getNeighbourhood;

public class DescentForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        DescentGameState dgs = (DescentGameState) firstState;
        DescentParameters descentParameters = (DescentParameters) firstState.getGameParameters();
        descentParameters.setTimeoutRounds(20);    // No game of Descent should feasibly last more than 20 rounds
        int nActionsPerFigure = descentParameters.nActionsPerFigure;
        dgs.data.load(descentParameters.getDataPath());
        dgs.initData = false;
        dgs.addComponents();
        DescentGameData _data = dgs.getData();

        // Set up dice pools
        // Revive: 2 red dice
        // Heal: 1 red die
        DicePool.revive = constructDicePool(descentParameters.reviveDice);
        DicePool.heal = constructDicePool(descentParameters.healDice);

        // TODO: epic play options (pg 19)

        // Get campaign from game parameters, load all the necessary information
        Campaign campaign = ((DescentParameters) dgs.getGameParameters()).campaign;
        campaign.load(_data, descentParameters.dataPath);
        // TODO: Separate shop items (+shuffle), monster and lieutenent cards into 2 acts.

        Quest firstQuest = campaign.getQuests()[0];
        dgs.currentQuest = firstQuest;
        String firstBoard = firstQuest.getBoards().get(0);

        // Set up first board of first quest
        setupBoard(dgs, _data, firstBoard);

        // Create a Heroes Side figure for tracking stats relevant to everyone in the heroes' party
        // Some encounters use Fatigue in the Heroes Side pool to act as a turn timer, so we can track it here
        dgs.heroesSide = new Figure("Heroes Side", -1);
        dgs.heroesSide.setTokenType("Heroes Side");
        dgs.heroesSide.setAttribute(Figure.Attribute.Fatigue, new Counter(0, 0, 8, "Heroes Side Fatigue"));

        // Overlord setup
        dgs.overlordPlayer = 0;  // First player is always the overlord
        // Overlord will also have a figure, but not on the board (to store xp and skill info)
        dgs.overlord = new Figure("Overlord", -1);
        // TODO: read the following from quest setup
        dgs.overlord.setAttribute(Figure.Attribute.Fatigue, new Counter(0, 0, 7, "Overlord Fatigue"));
        dgs.overlord.setTokenType("Overlord");
        // Overlord is player 0, first hero is player 1
        dgs.setTurnOwner(1);
        dgs.setFirstPlayer(1);
        dgs.monsterGroupActingNext = 0;
        dgs.monsterActingNext = 0;
        dgs.heroActingNext = 0;

        // TODO: Shuffle overlord deck and give overlord nPlayers cards.

        // TODO: is this quest phase or campaign phase?

        // TODO: Let players choose these, for now randomly assigned
        // 5. Player setup phase interrupts, after which setup continues:
        // Players choose heroes & class

        List<Vector2D> heroStartingPositions = firstQuest.getStartingLocations().get(firstBoard);
        List<Archetype> archetypes = new ArrayList<>(List.of(Archetype.values()));
        Random rnd = dgs.getRnd();
        dgs.heroes = new ArrayList<>();
        for (int i = 1; i < Math.max(3, dgs.getNPlayers()); i++) {

            Hero figure;
            if (descentParameters.heroesToBePlayed.size() >= i) {
                // in this case we do not do anything randomly
                String heroName = descentParameters.heroesToBePlayed.get(i - 1);
                figure = _data.findHero(heroName);
            } else {
                // Choose random archetype from those remaining
                Archetype archetype = archetypes.get(rnd.nextInt(archetypes.size()));

                // Choose random hero from that archetype
                List<Hero> heroes = _data.findHeroes(archetype);
                figure = heroes.get(rnd.nextInt(heroes.size())).copyNewID();
            }
            figure.getNActionsExecuted().setMaximum(nActionsPerFigure);
            figure.setComponentName("Hero: " + figure.getComponentName());  // For reference in rules

            String archetypeName = figure.getProperty("archetype").toString();
            Archetype archetype = Archetype.valueOf(archetypeName);
            archetypes.remove(archetype);

            if (dgs.getNPlayers() == 2) {
                // In 2-player games, 1 player controls overlord, the other 2 heroes
                figure.setOwnerId(1 - dgs.overlordPlayer);
            } else {
                figure.setOwnerId(i);
            }

            // Choose random class from that archetype
            HeroClass[] options = HeroClass.getClassesForArchetype(archetype);
            HeroClass heroClass = options[rnd.nextInt(options.length)];

            // Inform figure of chosen class
            figure.setProperty(new PropertyString("class", heroClass.name()));

            // Assign starting skills and equipment from chosen class
            Deck<Card> classDeck = _data.findDeck(heroClass.name());
            for (Card c : classDeck.getComponents()) {
                if (((PropertyInt) c.getProperty(xpHash)).value <= figure.getAttribute(Figure.Attribute.XP).getValue()) {
                    figure.equip(new DescentCard(c));
                }
            }
            // After equipping, set up abilities
            figure.getWeapons().stream().flatMap(w -> w.getWeaponSurges().stream())
                    .forEach(s -> figure.addAbility(new SurgeAttackAction(s, figure.getComponentID())));
            // All Heroes have a Surge action of Recover 1 Fatigue
            figure.addAbility(new SurgeAttackAction(Surge.RECOVER_1_FATIGUE, figure.getComponentID()));

            // Set up abilities from other equipment
            List<DescentAction> passiveActions = getOtherEquipmentActions(figure);
            if (!passiveActions.isEmpty()) {
                for (DescentAction act : passiveActions) {
                    figure.addAbility(act);
                }
            }

            // Enable the Heroic Feat
            figure.setFeatAvailable(true);

            // Place hero on the board in random starting position out of those available
            Vector2D position = heroStartingPositions.get(rnd.nextInt(heroStartingPositions.size()));
            figure.setPosition(position);

            // Tell the board there's a hero there
            PropertyInt prop = new PropertyInt("players", figure.getComponentID());
            dgs.masterBoard.getElement(position.getX(), position.getY()).setProperty(prop);

            // This starting position no longer an option (one hero per space)
            heroStartingPositions.remove(position);

            // Inform game of this hero figure
            dgs.heroes.add(figure);
        }

        // Add in Hero Abilities as potential actions we can take where appropriate
        for (Hero hero : dgs.getHeroes()) {
            // Widow Tarha's Hero Ability
            // Once per round, when we make an attack roll, we may reroll one attack or power die, and must keep the new result
            if (hero.getAbility().equals(HeroAbilities.HeroAbility.RerollOnce)) {
                for (int i = 0; i < (hero.getAttackDice().getSize()); i++) {
                    TarhaAbilityReroll reroll = new TarhaAbilityReroll(i);
                    if (!hero.getAbilities().contains(reroll)) {
                        hero.addAbility(reroll);
                    }
                }
            }

            // Jain Fairwood's Hero Ability
            // When we take damage, we can convert some (or all) of that damage into Fatigue, up to our max Fatigue
            if (hero.getAbility().equals(HeroAbilities.HeroAbility.DamageToFatigue)) {
                for (int i = 0; i < (hero.getAttribute(Figure.Attribute.Fatigue).getMaximum()); i++) {
                    JainTurnDamageIntoFatigue reduce = new JainTurnDamageIntoFatigue(hero.getComponentID(), (i + 1));
                    if (!hero.getAbilities().contains(reduce)) {
                        hero.addAbility(reduce);
                    }
                }
            }

            // Tomble Burrowell's Hero Ability
            // If we are targeted by an attack, and we are adjacent to an ally
            // We can add their defense pool to our own defense pool before we roll
            if (hero.getAbility().equals(HeroAbilities.HeroAbility.CopyAllyDefense)) {
                for (int i = 0; i < dgs.heroes.size(); i++) {
                    // Prevent Tomble to targeting himself
                    if (dgs.heroes.get(i).equals(hero)) {
                        continue;
                    }
                    TombleCopyDefence copyDefence = new TombleCopyDefence(hero.getComponentID(), dgs.heroes.get(i).getComponentID());
                    if (!hero.getAbilities().contains(copyDefence)) {
                        hero.addAbility(copyDefence);
                    }
                }
            }
        }

        // Overlord chooses monster groups // TODO, for now randomly selected
        // Create and place monsters
        createMonsters(dgs, firstQuest, _data, rnd, nActionsPerFigure);

        // Set up tokens
        dgs.tokens = new ArrayList<>();
        for (DToken.DTokenDef def : firstQuest.getTokens()) {
            int n = (def.getSetupHowMany().equalsIgnoreCase("nHeroes") ? dgs.getHeroes().size() : Integer.parseInt(def.getSetupHowMany()));
            // Find position, if only 1 value for all this is a tile where they have to go, pick random locations
            // TODO let overlord pick locations if not fixed
            String tileName = null;
            List<Integer> previousKeys = new ArrayList<>();
            if (def.getLocations().length == 1) tileName = def.getLocations()[0];
            for (int i = 0; i < n; i++) {
                Vector2D location = null;
                if (tileName == null) {
                    // Find fixed location
                    String[] split = def.getLocations()[i].split("-");
                    tileName = split[0];
                    String[] splitPos = split[1].split(";");
                    Vector2D locOnTile = new Vector2D(Integer.parseInt(splitPos[0]), Integer.parseInt(splitPos[1]));
                    Map<Vector2D, Vector2D> map = dgs.gridReferences.get(tileName);
                    for (Map.Entry<Vector2D, Vector2D> e : map.entrySet()) {
                        if (e.getValue().equals(locOnTile)) {
                            location = e.getKey();
                            break;
                        }
                    }
                    tileName = null;
                } else if (!tileName.equalsIgnoreCase("player")) {
                    // Find random location on tile
                    int idx = rnd.nextInt(dgs.gridReferences.get(tileName).size());
                    while (previousKeys.contains(idx)) idx = rnd.nextInt(dgs.gridReferences.get(tileName).size());
                    int k = 0;
                    for (Vector2D key : dgs.gridReferences.get(tileName).keySet()) {
                        if (k == idx) {
                            location = key;
                            previousKeys.add(idx);
                            break;
                        }
                        k++;
                    }
                } else {
                    // A player should hold these tokens, not on the board, location is left null
                }
                DToken token = new DToken(def.getTokenType(), location);
                token.setEffects(def.getEffectsCopy());
                for (TokenAction ta : token.getEffects()) {
                    ta.setTokenID(token.getComponentID());
                }
                token.setAttributeModifiers(def.getAttributeModifiers());
                if (location == null) {
                    // Make a hero owner of it TODO: players choose?
                    int idx = rnd.nextInt(dgs.getHeroes().size() - 1);
                    token.setOwnerId(dgs.getHeroes().get(idx).getComponentID(), dgs);
                }
                token.setComponentName(def.getAltName());
                dgs.tokens.add(token);
            }
        }

        // Set up dice!
        dgs.attributeDicePool = new DicePool(Collections.emptyList());
        dgs.attackDicePool = new DicePool(Collections.emptyList());
        dgs.defenceDicePool = new DicePool(Collections.emptyList());

        dgs.defeatedFigures = new ArrayList<>();

        // Shuffle search cards deck
        dgs.searchCards = _data.searchCards;
        dgs.searchCards.shuffle(rnd);

        // Announce all figures in play, including their stats and starting positions
        // Primarily for debug purposes
        boolean announce = false;
        if (announce) {
            for (Hero hero : dgs.heroes) {
                System.out.println(hero.getComponentName().replace("Hero: ", "") + " - " + hero.getProperty("archetype") + " (" + hero.getProperty("class") + ") - " +
                        hero.getAttributeValue(Health) + " HP - " + hero.getHandEquipment().toString() + " - " + hero.getPosition().toString());
            }
            for (List<Monster> monsters : dgs.getMonsters()) {
                for (Monster monster : monsters) {
                    System.out.println(monster.getComponentName() + " - " + monster.getAttributeValue(Health) + " HP - " + monster.getPosition().toString());
                }
            }

            // Ready to start playing!
            System.out.println("Game begin!");
        }
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        DescentGameState dgs = (DescentGameState) currentState;

        Figure actingFigure = dgs.getActingFigure();

        if (checkEndOfGame(dgs))
            return;  // TODO: this should be more efficient, and work with triggers so they're not checked after each small action, but only after actions that can actually trigger them

        if (action instanceof EndFigureTurn) {
            // now check to see if we need to end the player turn
            dgs.logEvent(Event.GameEvent.GAME_EVENT, "End Turn: " + actingFigure.getName().replace("Hero: ", "") + "; " + actingFigure.getComponentID() + ";" + actingFigure.getPosition());
            if (actingFigure instanceof Hero) {
                dgs.heroActingNext = (dgs.heroActingNext + 1) % dgs.heroes.size();
                if (dgs.heroActingNext == 0) {
                    // we have reached the overlord player
                    endPlayerTurn(dgs, 0);  // we just move on to the next player
                    overlordCheckFatigue(dgs, true);
                    // reset all monsters for acting
                    dgs.monsterActingNext = -1;
                    dgs.monsterGroupActingNext = 0;
                    dgs.monsterActingNext = dgs.nextMonster();
                    if (dgs.monsterActingNext == -1) {
                        throw new AssertionError("No monsters to activate - game should be over");
                    }
                } else {
                    // next hero
                    actingFigure = dgs.heroes.get(dgs.heroActingNext);
                    if (dgs.getTurnOwner() != actingFigure.getOwnerId()) {
                        endPlayerTurn(dgs, actingFigure.getOwnerId()); // change player if the next hero is controlled by a different player
                    }
                }

            } else {
                // we check to see if we need to end the overlord turn (and hence also the round)
                int nextMonster = dgs.nextMonster();
                if (nextMonster == -1) {
                    // Overlord has no more monsters to activate
                    endPlayerTurn(dgs);
                    overlordCheckFatigue(dgs, false);
                    checkReinforcements(dgs);

                    // Reset figures for the next round
                    for (Figure f : dgs.getHeroes()) {
                        f.resetRound();
                    }
                    for (List<Monster> mList : dgs.getMonsters()) {
                        for (Monster m : mList) {
                            m.resetRound();
                        }
                    }
                    dgs.overlord.resetRound();
                    dgs.monsterGroupActingNext = 0;
                    dgs.monsterActingNext = 0;
                    dgs.heroActingNext = 0;
                    while (dgs.getHeroes().get(dgs.heroActingNext).isDefeated() && dgs.heroActingNext < dgs.getHeroes().size()) {
                        dgs.heroActingNext = (dgs.heroActingNext + 1) % dgs.heroes.size();
                    }
                    endRound(dgs, 1);
                } else {
                    dgs.monsterActingNext = nextMonster;  // continue turn with the next monster
                }
            }
        }

        /*
        Hero turn:
        1. Start of turn:
            - start of turn abilities
            - refresh cards
            - test attributes for conditions
        2. Take 2 actions

        Overlord turn: TODO: current turn order alternates 1 monster group, 1 hero player etc.
        1. Start of turn:
            - Start of turn abilities
            - Draw 1 Overlord card
            - Refresh cards
        2. Activate monsters:
            - Choose monster group
            - On-activation group effects
            - Choose unactivated monster in group
            - On-activation effects
            - Perform 2 actions with the monster
            - End of monster activation effects
            - End of monster group activation effects
            - Repeat steps for each remaining monster group
        3. End of turn abilities
         */

        // Any figure that ends its turn in a lava space is immediately defeated.
        // Heroes that are defeated in this way place their hero token in the nearest empty space
        // (from where they were defeated) that does not contain lava. A large monster is immediately defeated only
        // if all spaces it occupies are lava spaces.

        // TODO

        // Quest finished -> Campaign phase
        // Set up campaign phase
        // receive gold from search cards and return cards to the deck.
        // recover all damage and fatigue, discard conditions and effects
        // receive quest reward (1XP per hero + bonus from quest)
        // shopping (if right after interlude, can buy any act 1 cards, then remove these from game)
        // spend XP points for skills
        // choose next quest (winner chooses)
        // setup next quest
        // Campaign phase -> quest phase

        // choosing interlude: the heroes pick if they won >= 2 act 1 quests, overlord picks if they won >=2 quests

        // TODO: in 2-hero games, free regular attack action each turn or recover 2 damage.
    }


    private void checkReinforcements(DescentGameState dgs) {
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

        switch (questName) {
            case "Acolyte of Saradyn":
                monsterName = "Goblin Archer";
                i = getMonsterGroupIndex(monsters, monsterName);
                if (i >= 0) {
                    if (dgs.getMonsters().get(i).size() < maxMonsters.get(i)) {
                        canSpawn = true;
                        tile = "4A";
                        noToSpawn = Math.min(maxMonsters.get(i) - dgs.getMonsters().get(i).size(), 2);
                    }
                }
                break;

            default:
                break;
        }

        if (canSpawn) {
            //System.out.println("Spawning " + noToSpawn + " " + monsterName + "s");
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

    private void spawnReinforcements(DescentGameState dgs, int index, int noToSpawn, List<Vector2D> tileCoords) {
        boolean masterExists = false;     // Checks if a Master monster originally spawned
        boolean canSpawnMaster = true;    // Checks if the Master monster is dead
        boolean minionExists = false;     // Checks if a Minion monster originally spawned

        // Assuming spawning a Master takes priority over spawning a Minion
        // We assume that we can always spawn a Minion
        // As we will just return if we do not have enough monsters to spawn otherwise

        Random rnd = dgs.getRnd();
        List<Monster> monstersOriginal = dgs.getOriginalMonsters().get(index);
        List<Monster> monsters = dgs.getMonsters().get(index);
        for (Monster monster : monstersOriginal) {
            // If the original set of monsters contains a Master, we should spawn a Master
            if (monster.getName().contains("master")) {
                masterExists = true;
            }
            // Likewise, if the original set of monsters contains a Minion, we should spawn a Minion
            if (monster.getName().contains("minion")) {
                minionExists = true;
            }

            // No use checking the rest of the list if we know we can legally spawn both
            if (masterExists && minionExists)
                break;
        }
        for (Monster monster : monsters) {
            // If the alive Monsters contain a Master, we cannot spawn a Master
            if (monster.getName().contains("master")) {
                canSpawnMaster = false;
                break;
            }
        }

        //System.out.println("Master exists: " + masterExists);
        //System.out.println("Can spawn master: " + canSpawnMaster);
        //System.out.println("Minion exists: " + minionExists);

        for (int k = 0; k < noToSpawn; k++) {
            // If the Master exists, it is always the first Monster on the list
            // The only time we do not look at the first Monster on the list is
            // if we can only spawn a Minion when a Master exists
            int indexToSpawn = k;

            // If the Master is alive, we cannot spawn a Master
            // So we spawn a Minion instead
            if (masterExists && !canSpawnMaster) {
                indexToSpawn = DescentHelper.getFirstMissingIndex(monsters);
            }

            Monster originalMonster = monstersOriginal.get(indexToSpawn);

            Monster monster = originalMonster.copyNewID();


            // TODO: copied straight from DescentForwardModel's spawning
            String size = ((PropertyString) monster.getProperty(sizeHash)).value;
            int w = Integer.parseInt(size.split("x")[0]);
            int h = Integer.parseInt(size.split("x")[1]);
            monster.setSize(w, h);

            while (!tileCoords.isEmpty()) {
                Vector2D option = tileCoords.get(rnd.nextInt(tileCoords.size()));
                tileCoords.remove(option);
                BoardNode position = dgs.masterBoard.getElement(option.getX(), option.getY());
                if (position.getComponentName().equals("plain") &&
                        ((PropertyInt) position.getProperty(playersHash)).value == -1) {
                    //if (position.getComponentName().equals("plain") && !Move.checkCollision(dgs, monster, option)) {
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
                                    ((PropertyInt) tile.getProperty(playersHash)).value != -1) {
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
                        if (canSpawnMaster) {
                            dgs.monsters.get(index).add(indexToSpawn, monster);
                            canSpawnMaster = false;
                        } else {
                            dgs.monsters.get(index).add(indexToSpawn, monster);
                        }
                        //System.out.println("Spawned " + monster.getName() + " at " + option.getX() + ", " + option.getY());
                        break;
                    }
                }
            }

        }
    }

    private void overlordCheckFatigue(DescentGameState dgs, boolean startOfTurn) {
        int changeFatigueBy = 0;

        String questName = dgs.getCurrentQuest().getName();

        switch (questName) {
            case "Acolyte of Saradyn":
                changeFatigueBy = fatigueCheckForAcolyteOfSaradyn(dgs);
                break;

            case "Rellegar's Rest":
                // Encounter 1
                // We only check at the start of the Overlord's turns here
                if (startOfTurn)
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
        if (changeFatigueBy > 0) {
            overlordIncreaseFatigue(dgs, changeFatigueBy);
        } else if (changeFatigueBy < 0) {
            overlordDecreaseFatigue(dgs, Math.abs(changeFatigueBy));
        }
    }

    private void overlordIncreaseFatigue(DescentGameState dgs, int increaseFatigueBy) {
        dgs.overlord.incrementAttribute(Figure.Attribute.Fatigue, increaseFatigueBy);
        //System.out.println("Overlord's Fatigue increased to: " + dgs.overlord.getAttribute(Figure.Attribute.Fatigue));
    }

    private void overlordDecreaseFatigue(DescentGameState dgs, int decreaseFatigueBy) {
        dgs.overlord.decrementAttribute(Figure.Attribute.Fatigue, decreaseFatigueBy);
        //System.out.println("Overlord's Fatigue decreased to: " + dgs.overlord.getAttribute(Figure.Attribute.Fatigue));
    }

    private void heroesSideIncreaseFatigue(DescentGameState dgs, int increaseFatigueBy) {
        dgs.heroesSide.incrementAttribute(Figure.Attribute.Fatigue, increaseFatigueBy);
//        System.out.println("Heroes' Side Fatigue increased to: " + dgs.heroesSide.getAttribute(Figure.Attribute.Fatigue));
    }

    private void heroesSideDecreaseFatigue(DescentGameState dgs, int decreaseFatigueBy) {
        dgs.heroesSide.decrementAttribute(Figure.Attribute.Fatigue, decreaseFatigueBy);
//        System.out.println("Heroes' Side Fatigue decreased to: " + dgs.heroesSide.getAttribute(Figure.Attribute.Fatigue));
    }

    // TODO This is just the check for the first quest
    // I'm putting it here for now, but once more quests are implemented we should put it into its own contained class
    // To avoid cluttering this class
    private int fatigueCheckForAcolyteOfSaradyn(DescentGameState dgs) {
        boolean changeFatigue = false;
        int changeFatigueBy = 0;

        // Check for within board piece 9A's range
        String tile = "9A";
        List<Vector2D> tileCoords = new ArrayList<>(dgs.gridReferences.get(tile).keySet());

        List<List<Monster>> monsters = dgs.getMonsters();
        for (List<Monster> mList : monsters) {

            if (changeFatigue) {
                break;
            }

            for (Monster m : mList) {
                if (m.getName().contains("Goblin Archer")) {
                    Vector2D position = m.getPosition();
                    if (tileCoords.contains(position)) {
                        changeFatigue = true;
                        changeFatigueBy = 1;
                        break;
                    }
                }
            }
        }

        return changeFatigueBy;
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        DescentGameState dgs = (DescentGameState) gameState;

        // Init action list
        List<AbstractAction> actions = new ArrayList<>();
        Figure actingFigure = dgs.getActingFigure();

        // End Of Turn Actions
        // We only access this when there is nothing else to do
//        if (EndTurn.turnEnded)
//        {
//            // - Special (specified by quest)
//            if (actingFigure.getAbilities() != null) {
//                for (DescentAction act : actingFigure.getAbilities()) {
//                    // Check if action can be executed right now
//                    if (act.canExecute(Triggers.END_TURN, dgs)) {
//                        actions.add(act);
//                    }
//                }
//            }
//
//            if (!actions.isEmpty()) return new ArrayList<>(new HashSet<>(actions));
//        }

        // End turn is (almost) always possible
        // Certain scenarios will need to prevent it from being taken
        // But they will remove it from the list when required, instead of preventing it here
        EndFigureTurn endTurn = new EndFigureTurn();
        actions.add(endTurn);

        // First, we must check if our Figure is a defeated Hero
        // Defeated Heroes can only perform the StandUp action

        if (actingFigure instanceof Hero && ((Hero) actingFigure).isDefeated()) {
            StandUp standUp = new StandUp();
            if (standUp.canExecute(dgs)) {
                actions.add(standUp);
                actions.remove(endTurn);
            }
            return new ArrayList<>(new HashSet<>(actions));
        }

        // Next, we check if we can make a Poisoned or Diseased attribute test
        // We cannot make any actions before we have taken our attribute tests against these conditions
        if (actingFigure.hasCondition(DescentCondition.Poison) || actingFigure.hasCondition(DescentCondition.Disease)) {
            boolean tested = false;

            Diseased diseased = new Diseased(actingFigure.getComponentID(), Figure.Attribute.Willpower);
            if (diseased.canExecute(dgs)) {
                actions.add(diseased);
                tested = true;
            }

            Poisoned poisoned = new Poisoned(actingFigure.getComponentID(), Figure.Attribute.Might);
            if (poisoned.canExecute(dgs)) {
                actions.add(poisoned);
                tested = true;
            }

            if (tested) {
                actions.remove(endTurn);
                return new ArrayList<>(new HashSet<>(actions));
            }
        }

        // Ashrian's Hero Ability
        // If we are a Minion Monster, and we start our turn adjacent to Ashrian, we are forced to take the Stunned condition
        if (actingFigure.getName().toLowerCase().contains("minion")) {
            HeroAbilities.ashrian(dgs);
        }

        // If we are stunned, we can only take the 'Stunned' action
        if (actingFigure.hasCondition(DescentCondition.Stun)) {
            Stunned stunned = new Stunned();
            if (stunned.canExecute(dgs)) {
                actions.add(stunned);
                actions.remove(endTurn);
            }
            return new ArrayList<>(new HashSet<>(actions));
        }

        // Grisban the Thirsty's Hero Ability
        // If we have used the Rest action this turn, we can remove 1 Condition from ourselves
        if (actingFigure instanceof Hero && ((Hero) actingFigure).hasRested())
            if (((Hero) actingFigure).getAbility().equals(HeroAbilities.HeroAbility.HealCondition)) {
                actions.addAll(HeroAbilities.grisban(dgs));
            }

        // If we have made all our Attribute Tests, then we can take our other actions
        // If we have movement points to spend, and not immobilized, add move actions
        if ((!actingFigure.hasCondition(DescentTypes.DescentCondition.Immobilize))
                && (actingFigure.getAttributeValue(Figure.Attribute.MovePoints) > 0)) {
            actions.addAll(moveActions(dgs, actingFigure));
        }

        // If a hero has stamina to spare, add move actions that cost fatigue
        if (actingFigure instanceof Hero && !actingFigure.isOffMap()) {
            if (!actingFigure.getAttribute(Figure.Attribute.Fatigue).isMaximum()) {
                GetFatiguedMovementPoints fatiguedMovementPoints = new GetFatiguedMovementPoints();
                if (fatiguedMovementPoints.canExecute(dgs)) actions.add(fatiguedMovementPoints);
            }
        }

        List<AbstractAction> attacks = new ArrayList<>();

        // Add actions that cost action points
        // These can only be taken if we have not already taken 2 actions
        // Also, the acting figure must be on the map to take them
        if (!actingFigure.getNActionsExecuted().isMaximum() && !actingFigure.isOffMap()) {
            // Get movement points action
            GetMovementPoints movePoints = new GetMovementPoints();
            if (movePoints.canExecute(dgs)) actions.add(movePoints);

            // - Attack with 1 equipped weapon [ + monsters, the rest are just heroes] TODO

            AttackType attackType = getAttackType(actingFigure);

            // Monsters may only perform one attack per activation, so we must check if we have already attacked this turn
            if (!(actingFigure instanceof Monster && actingFigure.hasAttacked())) {
                if (attackType == AttackType.MELEE || attackType == AttackType.BOTH) {
                    actions.addAll(meleeAttackActions(dgs, actingFigure));
                    attacks.addAll(meleeAttackActions(dgs, actingFigure));
                }
                if (attackType == AttackType.RANGED || attackType == AttackType.BOTH) {
                    actions.addAll(rangedAttackActions(dgs, actingFigure));
                    attacks.addAll(rangedAttackActions(dgs, actingFigure));
                }
            }

            // - Open/close a door TODO

            // Hero Only Actions
            // Rest
            // Revive
            // Search
            // Heroic Abilities and Feats
            if (actingFigure instanceof Hero) {

                // Rest
                Rest act = new Rest();
                if (act.canExecute(dgs)) {
                    actions.add(act);
                }

                // Revive
                Vector2D loc = actingFigure.getPosition();
                GridBoard board = dgs.getMasterBoard();
                List<Vector2D> neighbours = getNeighbourhood(loc.getX(), loc.getY(), board.getWidth(), board.getHeight(), true);
                for (Hero hero : dgs.heroes) {
                    if (actingFigure.equals(hero)) continue;
                    if (hero.isDefeated() && hero.getPosition() != null
                            && (neighbours.contains(hero.getPosition()) || hero.getPosition().equals(loc))) {
                        Revive revive = new Revive(hero.getComponentID());
                        if (revive.canExecute(dgs)) {
                            actions.add(revive);
                        }
                    }
                }

                // Search for adjacent Search tokens (or ones they're sitting on top of)
                for (DToken token : dgs.tokens) {
                    if (token.getDescentTokenType() == DescentToken.Search
                            && token.getPosition() != null
                            && (neighbours.contains(token.getPosition()) || token.getPosition().equals(loc))) {
                        for (DescentAction da : token.getEffects()) {
                            if (da.canExecute(dgs))
                                actions.add(da.copy());
                        }
                    }
                }

                // Avric Albright's Hero Ability
                // If we are a Hero (including Avric himself) within 3 spaces of Avric, we gain a Surge action of Recover 1 Heart
                HeroAbilities.avric(dgs);
            }

            // Get Monster Unique Actions
            // Monster Actions can only be taken once per turn, if they haven't already attacked yet
            if (actingFigure instanceof Monster && !actingFigure.hasAttacked()) {
                List<AbstractAction> monsterActions = monsterActions(dgs);
                if (!monsterActions.isEmpty()) {
                    actions.addAll(monsterActions);
                    attacks.addAll(monsterActions);
                }
            }

            // - Special (specified by quest)
            if (actingFigure.getAbilities() != null) {
                for (DescentAction act : actingFigure.getAbilities()) {
                    // Check if action can be executed right now
                    if (act.canExecute(Triggers.ACTION_POINT_SPEND, dgs)) {
                        actions.add(act);
                    }
                }
            }

            // TODO: exhaust a card for an action/modifier/effect "free" action
        }

        if (actingFigure instanceof Hero) {
            // Archetype Skills
            List<AbstractAction> archetypeSkills = getArchetypeSkillActions(dgs, actingFigure.getComponentID());
            if (!archetypeSkills.isEmpty()) actions.addAll(archetypeSkills);

            // Heroic Feat
            if (((Hero) actingFigure).isFeatAvailable()) {
                List<DescentAction> heroicFeats = heroicFeatAction(dgs);
                if (!heroicFeats.isEmpty())
                    actions.addAll(heroicFeats);

                // Tomble Burrowell's Heroic Feat
                // Prevents End Turn from being taken, to stop him from being stuck as a Token
                if (actions.contains(new ReturnToMapMove(4)) || actions.contains(new ReturnToMapPlace()))
                    actions.remove(endTurn);
            }
        }

        // Special - If there are only 2 Heroes in player, each Hero gets a free extra action
        // This does not cost any action points to take
        // This action may be used as a free attack, or to Restore 2 Hearts
        List<AbstractAction> twoHeroActions = new ArrayList<>();
        if (dgs.getHeroes().size() == 2) {
            if (actingFigure instanceof Hero && !actingFigure.hasUsedExtraAction()) {
                Restore restore = new Restore(2);
                if (restore.canExecute(dgs)) {
                    actions.add(restore);
                    twoHeroActions.add(restore);
                }

                // TODO: Fix this

                // Free Attack
                AttackType attackType = getAttackType(actingFigure);

                if (attackType == AttackType.MELEE || attackType == AttackType.BOTH) {
                    List<Integer> targets = getMeleeTargets(dgs, actingFigure);
                    for (Integer target : targets) {
                        FreeAttack freeAttack = new FreeAttack(actingFigure.getComponentID(), target, true);
                        if (freeAttack.canExecute(dgs)) {
                            actions.add(freeAttack);
                            twoHeroActions.add(freeAttack);
                        }
                    }
                }

                if (attackType == AttackType.RANGED || attackType == AttackType.BOTH) {
                    List<Integer> targets = getRangedTargets(dgs, actingFigure);
                    for (Integer target : targets) {
                        FreeAttack freeAttack = new FreeAttack(actingFigure.getComponentID(), target, false);
                        if (freeAttack.canExecute(dgs)) {
                            actions.add(freeAttack);
                            twoHeroActions.add(freeAttack);
                        }
                    }
                }

            }
        }

        // We should remove EndTurn to prevent premature ending of turn
        if (actions.size() > 1) {
            // Heroes can only End Turn if they have no more actions to take (including Free Actions for Two Heroes), or have moved this turn
            // This prevents choosing to GetMovementPoints, then immediately Ending Turn without using them
            if (actingFigure instanceof Hero) {
                if (!actingFigure.getNActionsExecuted().isMaximum() ||
                        !twoHeroActions.isEmpty() || DescentHelper.canStillMove(actingFigure))
                    actions.remove(endTurn);
            }
            if (actingFigure instanceof Monster) {
                // As Monsters can only attack once, they should be allowed to End Turn without using up all of their actions
                // It should be acceptable for a Monster to use an Attack action, then End Turn without moving
                if ((!actingFigure.getNActionsExecuted().isMaximum() && !attacks.isEmpty()) || DescentHelper.canStillMove(actingFigure))
                    actions.remove(endTurn);
            }
        }

        return new ArrayList<>(new HashSet<>(actions));
        //return actions;
    }

    private List<DescentAction> heroicFeatAction(DescentGameState dgs) {
        List<DescentAction> myFeats = HeroicFeat.getHeroicFeatActions(dgs);
        if (myFeats != null)
            return myFeats;
        return new ArrayList<>();

//        switch (actingFigure.getName().replace("Hero: ", ""))
//        {
//            // Healer
//            case "Ashrian":
//                // Ashrian can choose which Monster Group to target
//                for (List<Monster> monsters : dgs.getMonsters()) {
//                    heroicFeat = new StunAllInMonsterGroup(monsters, 3);
//                    if (heroicFeat.canExecute(dgs))
//                        heroicFeats.add(heroicFeat);
//                }
//                break;
//            case "Avric Albright":
//                // Avric heals all allies within 3 spaces
//                Vector2D position = dgs.getActingFigure().getPosition();
//                int range = 3;
//                List<Hero> heroesInRange = new ArrayList<>();
//                for(Hero hero : dgs.getHeroes()) {
//                    if (DescentHelper.inRange(position, hero.getPosition(), range)) {
//                        heroesInRange.add(hero);
//                    }
//                }
//                heroicFeat = new HealAllInRange(heroesInRange, 3);
//                if (heroicFeat.canExecute(dgs))
//                    heroicFeats.add(heroicFeat);
//                break;
//
//            // Mage
//            case "Leoric of the Book":
//                // Leoric attacks all adjacent monsters with a magic weapon
//                List<Integer> monsters = getMeleeTargets(dgs, actingFigure);
//                if (!monsters.isEmpty()) {
//                    heroicFeat = new AttackAllAdjacent(dgs.getActingFigure().getComponentID(), monsters);
//                    if (heroicFeat.canExecute(dgs))
//                        heroicFeats.add(heroicFeat);
//                }
//                break;
//            case "Widow Tarha":
//                // Tarha attacks two targets with only one attack roll
//                monsters = getRangedTargets(dgs, actingFigure);
//                // Get all possible monster pairs available
//                for (int i = 0; i < monsters.size(); i++) {
//                    for (int j = i + 1; j < monsters.size(); j++) {
//                        List<Integer> monsterPair = new ArrayList<>();
//                        monsterPair.add(monsters.get(i));
//                        monsterPair.add(monsters.get(j));
//
//                        heroicFeat = new DoubleAttack(dgs.getActingFigure().getComponentID(), monsterPair);
//                        if (heroicFeat.canExecute(dgs))
//                            heroicFeats.add(heroicFeat);
//                    }
//                }
//                break;
//
//            // Scout
//            case "Jain Fairwood":
//                // Jain can move double her speed and make an attack at any point during that movement (before, during and after)
//                heroicFeat = new DoubleMoveAttack((Hero) dgs.getActingFigure());
//                if (heroicFeat.canExecute(dgs))
//                    heroicFeats.add(heroicFeat);
//                break;
//            case "Tomble Burrowell":
//                // Tomble's Heroic Feat comes in three parts
//                // Remove from Map
//                heroicFeat = new RemoveFromMap();
//                if (heroicFeat.canExecute(dgs))
//                    heroicFeats.add(heroicFeat);
//                // Choose where to return to Map (by up to 4 spaces away)
//                heroicFeat = new ReturnToMapMove(4);
//                if (heroicFeat.canExecute(dgs))
//                    heroicFeats.add(heroicFeat);
//
//                // Return to Map
//                heroicFeat = new ReturnToMapPlace();
//                if (heroicFeat.canExecute(dgs))
//                    heroicFeats.add(heroicFeat);
//                break;
//
//            // Warrior
//            case "Grisban the Thirsty":
//                // Grisban can make a free extra attack
//                monsters = getMeleeTargets(dgs, actingFigure);
//                for (int monster : monsters) {
//                    // TODO: Enable check to see if Grisban can used Ranged Attacks (by default he is Melee)
//                    heroicFeat = new HeroicFeatExtraAttack(dgs.getActingFigure().getComponentID(), monster, true);
//                    if (heroicFeat.canExecute(dgs))
//                        heroicFeats.add(heroicFeat);
//                }
//                break;
//            case "Syndrael":
//                // Syndrael allows her and any ally of her choice within 3 spaces to immediately make a Move action
//                List<Hero> heroes = dgs.getHeroes();
//                for (Hero hero : heroes) {
//                    if (hero == actingFigure)
//                        continue;
//                    position = dgs.getActingFigure().getPosition();
//                    range = 3;
//                    if (DescentHelper.inRange(position, hero.getPosition(), range)) {
//                        heroicFeat = new HeroicFeatExtraMovement(actingFigure, hero);
//                        if (heroicFeat.canExecute(dgs))
//                            heroicFeats.add(heroicFeat);
//                    }
//
//                }
//                break;
//            default:
//                break;
//        }
//        return heroicFeats;
    }

    private ArrayList<AbstractAction> monsterActions(DescentGameState dgs) {
        return getMonsterActions(dgs);
    }

    private List<AbstractAction> meleeAttackActions(DescentGameState dgs, Figure f) {

        List<Integer> targets = getMeleeTargets(dgs, f);
        List<MeleeAttack> actions = new ArrayList<>();

        for (Integer target : targets) {
            MeleeAttack attack = new MeleeAttack(f.getComponentID(), target);
            if (attack.canExecute(dgs))
                actions.add(attack);
        }

        Collections.sort(actions, Comparator.comparingInt(MeleeAttack::getDefendingFigure));

        List<AbstractAction> sortedActions = new ArrayList<>();

        sortedActions.addAll(actions);

        return sortedActions;
    }


    private List<AbstractAction> rangedAttackActions(DescentGameState dgs, Figure f) {

        List<Integer> targets = getRangedTargets(dgs, f);
        List<RangedAttack> actions = new ArrayList<>();

        for (Integer target : targets) {
            RangedAttack attack = new RangedAttack(f.getComponentID(), target);
            if (attack.canExecute(dgs))
                actions.add(attack);
        }

        Collections.sort(actions, Comparator.comparingInt(RangedAttack::getDefendingFigure));

        List<AbstractAction> sortedActions = new ArrayList<>();

        sortedActions.addAll(actions);

        return sortedActions;
    }

    private boolean checkEndOfGame(DescentGameState dgs) {
        // TODO end of campaign / other phases
        for (GameOverCondition condition : dgs.currentQuest.getGameOverConditions()) {
            if (condition.test(dgs) == CoreConstants.GameResult.GAME_END) {
                // Quest is over, give rewards
                if (condition.getString(dgs).contains("Heroes: WIN_GAME")) {
                    dgs.setGameStatus(GameResult.WIN_GAME);
                } else {
                    dgs.setGameStatus(GameResult.LOSE_GAME);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected void endGame(AbstractGameState gameState) {

        DescentGameState dgs = (DescentGameState) gameState;
        GameResult gameResult = dgs.getGameStatus();

        switch (gameResult) {
            case TIMEOUT:
                for (int i = 0; i < dgs.getNPlayers(); i++) {
                    dgs.setPlayerResult(GameResult.TIMEOUT, i);
                }
                break;

            case WIN_GAME:
                for (int i = 0; i < dgs.getNPlayers(); i++) {
                    if (i == dgs.getOverlordPlayer()) {
                        dgs.setPlayerResult(GameResult.LOSE_GAME, i);
                    } else {
                        dgs.setPlayerResult(GameResult.WIN_GAME, i);
                    }
                }
                break;

            case LOSE_GAME:
                for (int i = 0; i < dgs.getNPlayers(); i++) {
                    if (i != dgs.getOverlordPlayer()) {
                        dgs.setPlayerResult(GameResult.LOSE_GAME, i);
                    } else {
                        dgs.setPlayerResult(GameResult.WIN_GAME, i);
                    }
                }
                break;

            default:
//                System.out.println("Game ended in an unknown way! + " + gameResult);
                for (int i = 0; i < dgs.getNPlayers(); i++) {
                    dgs.setPlayerResult(GameResult.DRAW_GAME, i);
                }
                break;
        }
    }

    private void setupBoard(DescentGameState dgs, DescentGameData _data, String bConfig) {

        // 1. Read the graph board configuration for the master grid board; a graph board where nodes are individual tiles (grid boards) and connections between them
        GraphBoard config = _data.findGraphBoard(bConfig);

        // 2. Read all necessary tiles, which are all grid boards. Keep in a list.
        dgs.tiles = new HashMap<>();  // Maps from component ID to gridboard object
        HashMap<Integer, GridBoard> tileConfigs = new HashMap<>();
        dgs.gridReferences = new HashMap<>();  // Maps from tile name to list of positions in the master grid board that its cells occupy
        for (BoardNode bn : config.getBoardNodes()) {
            String name = bn.getComponentName();
            String tileName = name;
            if (name.contains("-")) {  // There may be multiples of one tile in the board, which follow format "tilename-#"
                tileName = tileName.split("-")[0];
            }
            GridBoard tile = _data.findGridBoard(tileName).copyNewID();
            if (tile != null) {
                tile = tile.copyNewID();
                tile.setProperty(bn.getProperty(orientationHash));
                tile.setComponentName(name);
                dgs.tiles.put(tile.getComponentID(), tile);
                tileConfigs.put(bn.getComponentID(), tile);
                dgs.gridReferences.put(name, new HashMap<>());
            }
        }

        // 3. Put together the master grid board
        // Find maximum board width and height, if all were put together side by side
        int width = 0;
        int height = 0;
        for (BoardNode bn : config.getBoardNodes()) {
            // Find width of this tile, according to orientation
            GridBoard tile = tileConfigs.get(bn.getComponentID());
            if (tile != null) {
                int orientation = ((PropertyInt) bn.getProperty(orientationHash)).value;
                if (orientation % 2 == 0) {
                    width += tile.getWidth();
                    height += tile.getHeight();
                } else {
                    width += tile.getHeight();
                    height += tile.getWidth();
                }
            }
        }

        // First tile will be in the center, board could expand in more directions
        width *= 2;
        height *= 2;

        // Create big board
        BoardNode[][] board = new BoardNode[height][width];  // Board nodes here will be the individual cells in the tiles
        dgs.tileReferences = new int[height][width];  // Reference to component ID of tile placed at that position
        HashMap<BoardNode, BoardNode> drawn = new HashMap<>();  // Keeps track of which tiles have been added to the board already, for recursive purposes

        // TODO iterate all that are not already drawn, to make sure disconnected tiles are also placed.
        // StartX / Y Will need to be adjusted to not draw on top of existing things

        // Find first tile, as board node in the board configuration graph board
        BoardNode firstTile = config.getBoardNodes().iterator().next();
        if (firstTile != null) {
            // Find grid board of first tile, rotate to correct orientation and add its tiles to the board
            GridBoard tile = tileConfigs.get(firstTile.getComponentID());
            int orientation = ((PropertyInt) firstTile.getProperty(orientationHash)).value;
            Component[][] rotated = tile.rotate(orientation);
            int startX = width / 2 - rotated[0].length / 2;
            int startY = height / 2 - rotated.length / 2;
            // Bounds will keep track of where tiles actually exist in the master board, to trim to size later
            Rectangle bounds = new Rectangle(startX, startY, rotated[0].length, rotated.length);
            // Recursive call, will add all tiles in relation to their neighbours as per the board configuration
            addTilesToBoard(null, firstTile, startX, startY, board, null, tileConfigs, dgs.tileReferences, dgs.gridReferences, drawn, bounds, dgs, null);

            // Trim the resulting board and tile references to remove excess border of nulls according to 'bounds' rectangle
            BoardNode[][] trimBoard = new BoardNode[bounds.height][bounds.width];
            int[][] trimTileRef = new int[bounds.height][bounds.width];
            for (int i = 0; i < bounds.height; i++) {
                if (bounds.width >= 0) {
                    System.arraycopy(board[i + bounds.y], bounds.x, trimBoard[i], 0, bounds.width);
                    System.arraycopy(dgs.tileReferences[i + bounds.y], bounds.x, trimTileRef[i], 0, bounds.width);
                }
            }
            dgs.tileReferences = trimTileRef;
            // And grid references
            for (Map.Entry<String, Map<Vector2D, Vector2D>> e : dgs.gridReferences.entrySet()) {
                for (Vector2D v : e.getValue().keySet()) {
                    v.subtract(bounds.x, bounds.y);
                }
            }

            // This is the final master board!
            dgs.masterBoard = new GridBoard(trimBoard);
            // Init each node (cell) properties - not occupied ("players" int property), and its position in the master grid
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    BoardNode bn = dgs.masterBoard.getElement(j, i);
                    if (bn != null) {
                        bn.setProperty(new PropertyVector2D("coordinates", new Vector2D(j, i)));
                        bn.setProperty(new PropertyInt("players", -1));
                    }
                }
            }
        } else {
//            System.out.println("Tiles for the map not found");
        }


        // Pathfinder utility (TEST).
//        dgs.addAllComponents();
//        Pathfinder pf = new Pathfinder(dgs.masterBoard);
//        int orig = dgs.masterBoard.getElement(10,3).getComponentID();
//        int dest = dgs.masterBoard.getElement(10,5).getComponentID();
//        Path p = pf.getPath(dgs, orig, dest);
//
//        System.out.println(p.toString());
//        int a = 0; //10,4 -> 10,5
    }

    /**
     * Recursively adds tiles to the board, iterating through all neighbours and updating references from grid
     * to tiles, list of valid neighbours for movement graph according to Descent movement rules, and bounds for
     * the resulting grid of the master board (with all tiles put together).
     *
     * @param tileToAdd      - board node representing tile to add to board
     * @param x              - top-left x-coordinate for this tile's location in the master board
     * @param y              - top-left y-coordinate for this tile's location in the master board
     * @param board          - the master grid board representation
     * @param tileGrid       - grid representation of tile to add to board (possibly a trimmed version from its corresponding
     *                       object in the "tiles" map to fit together with existing board)
     * @param tiles          - mapping from board node component ID, to GridBoard object representing the tile
     * @param tileReferences - references from each cell in the grid to the component ID of the GridBoard representing
     *                       the tile at that location
     * @param drawn          - a list of board nodes already drawn, to avoid drawing twice during recursive calls.
     * @param bounds         - bounds of contents of the master grid board
     */
    private void addTilesToBoard(BoardNode parentTile, BoardNode tileToAdd, int x, int y, BoardNode[][] board,
                                 BoardNode[][] tileGrid,
                                 Map<Integer, GridBoard> tiles,
                                 int[][] tileReferences, Map<String, Map<Vector2D, Vector2D>> gridReferences,
                                 Map<BoardNode, BoardNode> drawn,
                                 Rectangle bounds,
                                 DescentGameState dgs,
                                 String sideWithOpening) {
        if (!drawn.containsKey(parentTile) || !drawn.get(parentTile).equals(tileToAdd)) {
            // Draw this tile in the big board at x, y location
            GridBoard tile = tiles.get(tileToAdd.getComponentID());
            BoardNode[][] originalTileGrid = tile.rotate(((PropertyInt) tileToAdd.getProperty(orientationHash)).value);
            if (tileGrid == null) {
                tileGrid = originalTileGrid;
            }
            int height = tileGrid.length;
            int width = tileGrid[0].length;

            // Add cells from new tile to the master board
            for (int i = y; i < y + height; i++) {
                for (int j = x; j < x + width; j++) {
                    // Avoid removing already set tiles
                    if (tileGrid[i - y][j - x] == null || tileGrid[i - y][j - x].getComponentName().equalsIgnoreCase("null")
                            || board[i][j] != null && !board[i][j].getComponentName().equalsIgnoreCase("null")
                            || !TerrainType.isInsideTerrain(tileGrid[i - y][j - x].getComponentName())) continue;

                    // Set
                    board[i][j] = tileGrid[i - y][j - x].copy();
                    board[i][j].setProperty(new PropertyInt("connections", tileToAdd.getComponentID()));

                    // Don't keep references for edge tiles
                    if (board[i][j] == null || board[i][j].getComponentName().equals("edge")
                            || board[i][j].getComponentName().equals("open")) continue;

                    // Set references
                    tileReferences[i][j] = tile.getComponentID();
                    for (String s : gridReferences.keySet()) {
                        gridReferences.get(s).remove(new Vector2D(j, i));
                    }
                    gridReferences.get(tile.getComponentName()).put(new Vector2D(j, i), new Vector2D(j - x, i - y));
                }
            }

            // Add connections at opening side with existing tiles
            addConnectionsAtOpeningOnSide(board, originalTileGrid, x, y, width, height, sideWithOpening);

            // Add connections inside the tile, ignoring blocked spaces
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    BoardNode currentSpace = tileGrid[i][j];
                    if (currentSpace != null && (TerrainType.isWalkableTerrain(currentSpace.getComponentName())
                            || currentSpace.getComponentName().equalsIgnoreCase("pit"))) {  // pits connect to walkable spaces only (pit-pit not allowed)
                        List<Vector2D> boardNs = getNeighbourhood(j, i, width, height, true);
                        for (Vector2D n2 : boardNs) {
                            if (tileGrid[n2.getY()][n2.getX()] != null && TerrainType.isWalkableTerrain(tileGrid[n2.getY()][n2.getX()].getComponentName())) {
                                board[n2.getY() + y][n2.getX() + x].addNeighbourWithCost(board[i + y][j + x], TerrainType.getMovePointsCost(board[i + y][j + x].getComponentName()));
                                board[i + y][j + x].addNeighbourWithCost(board[n2.getY() + y][n2.getX() + x], TerrainType.getMovePointsCost(board[n2.getY() + y][n2.getX() + x].getComponentName()));
                            }
                        }
                    }
                }
            }

            // This tile was drawn
//            drawn.add(tileToAdd);
            drawn.put(parentTile, tileToAdd);

            // Draw neighbours
            for (BoardNode neighbour : tileToAdd.getNeighbours().keySet()) {

                // Find location to start drawing neighbour
                Pair<String, Vector2D> connectionToNeighbour = findConnection(tileToAdd, neighbour, findOpenings(tileGrid));

                if (connectionToNeighbour != null) {
                    connectionToNeighbour.b.add(x, y);
                    // Find orientation and opening connection from neighbour, generate top-left corner of neighbour from that
                    GridBoard tileN = tiles.get(neighbour.getComponentID());
                    if (tileN != null) {
                        BoardNode[][] tileGridN = tileN.rotate(((PropertyInt) neighbour.getProperty(orientationHash)).value);

                        // Find location to start drawing neighbour
                        Pair<String, Vector2D> conn2 = findConnection(neighbour, tileToAdd, findOpenings(tileGridN));

                        int w = tileGridN[0].length;
                        int h = tileGridN.length;

                        if (conn2 != null) {
                            String side = conn2.a;
                            Vector2D connectionFromNeighbour = conn2.b;
                            if (side.equalsIgnoreCase("W")) {
                                // Remove first column
                                BoardNode[][] tileGridNTrim = new BoardNode[h][w - 1];
                                for (int i = 0; i < h; i++) {
                                    System.arraycopy(tileGridN[i], 1, tileGridNTrim[i], 0, w - 1);
                                }
                                tileGridN = tileGridNTrim;
                            } else if (side.equalsIgnoreCase("E")) {
                                connectionFromNeighbour.subtract(1, 0);
                                // Remove last column
                                BoardNode[][] tileGridNTrim = new BoardNode[h][w - 1];
                                for (int i = 0; i < h; i++) {
                                    System.arraycopy(tileGridN[i], 0, tileGridNTrim[i], 0, w - 1);
                                }
                                tileGridN = tileGridNTrim;
                            } else if (side.equalsIgnoreCase("N")) {
                                // Remove first row
                                BoardNode[][] tileGridNTrim = new BoardNode[h - 1][w];
                                for (int i = 1; i < h; i++) {
                                    System.arraycopy(tileGridN[i], 0, tileGridNTrim[i - 1], 0, w);
                                }
                                tileGridN = tileGridNTrim;
                            } else {
                                connectionFromNeighbour.subtract(0, 1);
                                // Remove last row
                                BoardNode[][] tileGridNTrim = new BoardNode[h - 1][w];
                                for (int i = 0; i < h - 1; i++) {
                                    System.arraycopy(tileGridN[i], 0, tileGridNTrim[i], 0, w);
                                }
                                tileGridN = tileGridNTrim;
                            }
                            Vector2D topLeftCorner = new Vector2D(connectionToNeighbour.b.getX() - connectionFromNeighbour.getX(),
                                    connectionToNeighbour.b.getY() - connectionFromNeighbour.getY());

                            // Update area bounds
                            if (topLeftCorner.getX() < bounds.x) bounds.x = topLeftCorner.getX();
                            if (topLeftCorner.getY() < bounds.y) bounds.y = topLeftCorner.getY();
                            int deltaMaxX = (int) (topLeftCorner.getX() + tileGridN[0].length - bounds.getMaxX());
                            if (deltaMaxX > 0) bounds.width += deltaMaxX;
                            int deltaMaxY = (int) (topLeftCorner.getY() + tileGridN.length - bounds.getMaxY());
                            if (deltaMaxY > 0) bounds.height += deltaMaxY;

                            // Draw neighbour recursively
                            addTilesToBoard(tileToAdd, neighbour, topLeftCorner.getX(), topLeftCorner.getY(), board, tileGridN,
                                    tiles, tileReferences, gridReferences, drawn, bounds, dgs, side);
                        }
                    }
                }
            }
        }
    }

    private void addConnectionsAtOpeningOnSide(BoardNode[][] board, BoardNode[][] originalTileGrid,
                                               int x, int y, int width, int height, String side) {
        if (side != null) {
            if (side.equalsIgnoreCase("n")) {
                // Nodes at opening that should connect are on the top row. Above them on original tile grid there is an "open" space
                int i = 0;
                for (int j = 0; j < width; j++) {
                    if (originalTileGrid[i][j] != null &&  // Same row, in the tile that was placed this is trimmed
                            originalTileGrid[i][j].getComponentName().equalsIgnoreCase("open")
                            && board[i + y - 1][j + x] != null) {
                        // Add connections for this node
                        for (int x1 = x - 1; x1 <= x + 1; x1++) {
                            if (j + x1 >= 0 && j + x1 < board[0].length) {
                                if (board[i + y][j + x] != null && board[i + y - 1][j + x1] != null) {
                                    board[i + y][j + x].addNeighbourWithCost(board[i + y - 1][j + x1], TerrainType.getMovePointsCost(board[i + y - 1][j + x1].getComponentName()));
                                    board[i + y - 1][j + x1].addNeighbourWithCost(board[i + y][j + x], TerrainType.getMovePointsCost(board[i + y][j + x].getComponentName()));
                                }
                            }
                        }
                        // And connections back from the node in front too
                        for (int x1 = x - 1; x1 <= x + 1; x1++) {
                            if (j + x1 >= 0 && j + x1 < board[0].length) {
                                if (board[i + y - 1][j + x] != null && board[i + y][j + x1] != null) {
                                    board[i + y - 1][j + x].addNeighbourWithCost(board[i + y][j + x1], TerrainType.getMovePointsCost(board[i + y][j + x1].getComponentName()));
                                    board[i + y][j + x1].addNeighbourWithCost(board[i + y - 1][j + x], TerrainType.getMovePointsCost(board[i + y - 1][j + x].getComponentName()));
                                }
                            }
                        }
                    }
                }
            } else if (side.equalsIgnoreCase("s")) {
                // Nodes at opening that should connect are on the bottom row. Below them is an "open" space
                int i = height - 1;
                for (int j = 0; j < width; j++) {
                    if (originalTileGrid[i + 1][j] != null &&  // Next row
                            originalTileGrid[i + 1][j].getComponentName().equalsIgnoreCase("open")
                            && board[i + y + 1][j + x] != null) {
                        // Add connections for this node
                        for (int x1 = x - 1; x1 <= x + 1; x1++) {
                            if (j + x1 >= 0 && j + x1 < board[0].length) {
                                if (board[i + y][j + x] != null && board[i + y + 1][j + x1] != null) {
                                    board[i + y][j + x].addNeighbourWithCost(board[i + y + 1][j + x1], TerrainType.getMovePointsCost(board[i + y + 1][j + x1].getComponentName()));
                                    board[i + y + 1][j + x1].addNeighbourWithCost(board[i + y][j + x], TerrainType.getMovePointsCost(board[i + y][j + x].getComponentName()));
                                }
                            }
                        }
                        // And connections back from the node in front too
                        for (int x1 = x - 1; x1 <= x + 1; x1++) {
                            if (j + x1 >= 0 && j + x1 < board[0].length) {
                                if (board[i + y + 1][j + x] != null && board[i + y][j + x1] != null) {
                                    board[i + y + 1][j + x].addNeighbourWithCost(board[i + y][j + x1], TerrainType.getMovePointsCost(board[i + y][j + x1].getComponentName()));
                                    board[i + y][j + x1].addNeighbourWithCost(board[i + y + 1][j + x], TerrainType.getMovePointsCost(board[i + y + 1][j + x].getComponentName()));
                                }
                            }
                        }
                    }
                }
            } else if (side.equalsIgnoreCase("e")) {
                // Nodes are on the rightmost column. To their right is "open"
                int j = width - 1;
                for (int i = 0; i < height; i++) {
                    if (originalTileGrid[i][j + 1] != null &&  // Next column
                            originalTileGrid[i][j + 1].getComponentName().equalsIgnoreCase("open")
                            && board[i + y][j + x + 1] != null) {
                        // Add connections for this node
                        for (int y1 = y - 1; y1 <= y + 1; y1++) {
                            if (i + y1 >= 0 && i + y1 < board.length
                                    && board[i + y][j + x] != null && board[i + y1][j + x + 1] != null) {
                                board[i + y][j + x].addNeighbourWithCost(board[i + y1][j + x + 1], TerrainType.getMovePointsCost(board[i + y1][j + x + 1].getComponentName()));
                                board[i + y1][j + x + 1].addNeighbourWithCost(board[i + y][j + x], TerrainType.getMovePointsCost(board[i + y][j + x].getComponentName()));
                            }
                        }
                        // And connections back from the node in front too
                        for (int y1 = y - 1; y1 <= y + 1; y1++) {
                            if (i + y1 >= 0 && i + y1 < board.length
                                    && board[i + y][j + x + 1] != null && board[i + y1][j + x] != null) {
                                board[i + y][j + x + 1].addNeighbourWithCost(board[i + y1][j + x], TerrainType.getMovePointsCost(board[i + y1][j + x].getComponentName()));
                                board[i + y1][j + x].addNeighbourWithCost(board[i + y][j + x + 1], TerrainType.getMovePointsCost(board[i + y][j + x + 1].getComponentName()));
                            }
                        }
                    }
                }
            } else if (side.equalsIgnoreCase("w")) {
                // Nodes are in the first column (leftmost). To their left is "open"
                int j = 0;
                for (int i = 0; i < height; i++) {
                    if (originalTileGrid[i][j] != null &&  // Same column, trimmed in tile that was placed
                            originalTileGrid[i][j].getComponentName().equalsIgnoreCase("open")
                            && board[i + y][j + x - 1] != null) {
                        // Add connections for this node
                        for (int y1 = y - 1; y1 <= y + 1; y1++) {
                            if (i + y1 >= 0 && i + y1 < board.length
                                    && board[i + y][j + x] != null && board[i + y1][j + x - 1] != null) {
                                board[i + y][j + x].addNeighbourWithCost(board[i + y1][j + x - 1], TerrainType.getMovePointsCost(board[i + y1][j + x - 1].getComponentName()));
                                board[i + y1][j + x - 1].addNeighbourWithCost(board[i + y][j + x], TerrainType.getMovePointsCost(board[i + y][j + x].getComponentName()));
                            }
                        }
                        // And connections back from the node in front too
                        for (int y1 = y - 1; y1 <= y + 1; y1++) {
                            if (i + y1 >= 0 && i + y1 < board.length
                                    && board[i + y][j + x - 1] != null && board[i + y1][j + x] != null) {
                                board[i + y][j + x - 1].addNeighbourWithCost(board[i + y1][j + x], TerrainType.getMovePointsCost(board[i + y1][j + x].getComponentName()));
                                board[i + y1][j + x].addNeighbourWithCost(board[i + y][j + x - 1], TerrainType.getMovePointsCost(board[i + y][j + x - 1].getComponentName()));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds a connection between two boardnodes representing tiles in the game (i.e. where the 2 tiles should be
     * connecting according to board configuration)
     *
     * @param from     - origin board node to find connection from
     * @param to       - board node to find connection to
     * @param openings - list of openings for the origin board node
     * @return - a pair of side, and location (in tile space) of openings that would connect to the given tile as required
     */
    private Pair<String, Vector2D> findConnection(BoardNode from, BoardNode to, HashMap<String, ArrayList<Vector2D>> openings) {
        String[] neighbours = ((PropertyStringArray) from.getProperty(neighbourHash)).getValues();
        String[] connections = ((PropertyStringArray) from.getProperty(connectionHash)).getValues();

        for (int i = 0; i < neighbours.length; i++) {
            if (neighbours[i].equalsIgnoreCase(to.getComponentName())) {
                String conn = connections[i];

                String side = conn.split("-")[0];
                int countFromTop = Integer.parseInt(conn.split("-")[1]);
                if (openings.containsKey(side)) {
                    if (countFromTop >= 0 && countFromTop < openings.get(side).size()) {
                        return new Pair<>(side, openings.get(side).get(countFromTop));
                    }
                }
                break;
            }
        }
        return null;
    }

    /**
     * Finds coordinates (in tile space) for where openings on all sides (top-left locations).
     * // TODO: assumes all openings 2-tile wide + no openings are next to each other.
     *
     * @param tileGrid - grid to look for openings in
     * @return - Mapping from side (N, S, W, E) to a list of openings on that particular side.
     */
    private HashMap<String, ArrayList<Vector2D>> findOpenings(BoardNode[][] tileGrid) {
        int height = tileGrid.length;
        int width = tileGrid[0].length;

        HashMap<String, ArrayList<Vector2D>> openings = new HashMap<>();
        // TOP, check each column, stop at the first encounter in each column.
        for (int j = 0; j < width; j++) {
            for (int i = 0; i < height; i++) {
                if (tileGrid[i][j] != null && tileGrid[i][j].getComponentName().equalsIgnoreCase("open")) {
                    // Check valid: nothing, null, or edge tile above
                    if (i == 0 || tileGrid[i - 1][j].getComponentName().equalsIgnoreCase("null") ||
                            tileGrid[i - 1][j].getComponentName().equalsIgnoreCase("edge")) {
                        // Check valid: nothing or not "open" to the left (already included, all openings 2-wide)
                        // But another "open" to the right
                        if ((j == 0 || tileGrid[i][j - 1] != null && j < width - 1 && tileGrid[i][j + 1] != null
                                && !tileGrid[i][j - 1].getComponentName().equalsIgnoreCase("open"))
                                && (j < width - 1 && tileGrid[i][j + 1].getComponentName().equalsIgnoreCase("open"))) {
                            if (!openings.containsKey("N")) {
                                openings.put("N", new ArrayList<>());
                            }
                            openings.get("N").add(new Vector2D(j, i));
                            break;
                        }
                    }
                }
            }
        }
        // BOTTOM, check each column, stop at the first encounter in each column (read from bottom to top).
        for (int j = 0; j < width; j++) {
            for (int i = height - 1; i >= 0; i--) {
                if (tileGrid[i][j] != null && tileGrid[i][j].getComponentName().equalsIgnoreCase("open")) {
                    // Check valid: nothing, null, or edge tile below
                    if (i == height - 1 || tileGrid[i + 1][j].getComponentName().equalsIgnoreCase("null") ||
                            tileGrid[i + 1][j].getComponentName().equalsIgnoreCase("edge")) {
                        // Check valid: nothing or not "open" to the left (already included, all openings 2-wide)
                        // But another "open" to the right
                        if ((j == 0 || !tileGrid[i][j - 1].getComponentName().equalsIgnoreCase("open")) &&
                                (j < width - 1 && tileGrid[i][j + 1].getComponentName().equalsIgnoreCase("open"))) {
                            if (!openings.containsKey("S")) {
                                openings.put("S", new ArrayList<>());
                            }
                            openings.get("S").add(new Vector2D(j, i));
                            break;
                        }
                    }
                }
            }
        }
        // LEFT, check each row, stop at the first encounter in each row.
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (tileGrid[i][j] != null && tileGrid[i][j].getComponentName().equalsIgnoreCase("open")) {
                    // Check valid: nothing, null, or edge tile to the left
                    if (j == 0 || tileGrid[i][j - 1].getComponentName().equalsIgnoreCase("null") ||
                            tileGrid[i][j - 1].getComponentName().equalsIgnoreCase("edge")) {
                        // Check valid: nothing or not "open" above (already included, all openings 2-wide)
                        // But another "open" below
                        if ((i == 0 || !tileGrid[i - 1][j].getComponentName().equalsIgnoreCase("open")) &&
                                (i < height - 1 && tileGrid[i + 1][j].getComponentName().equalsIgnoreCase("open"))) {
                            if (!openings.containsKey("W")) {
                                openings.put("W", new ArrayList<>());
                            }
                            openings.get("W").add(new Vector2D(j, i));
                            break;
                        }
                    }
                }
            }
        }
        // RIGHT, check each row, stop at the first encounter in each row (read from right to left).
        for (int i = 0; i < height; i++) {
            for (int j = width - 1; j >= 0; j--) {
                if (tileGrid[i][j] != null && tileGrid[i][j].getComponentName().equalsIgnoreCase("open")) {
                    // Check valid: nothing, null, or edge tile to the right
                    if (j == width - 1 || tileGrid[i][j + 1].getComponentName().equalsIgnoreCase("null") ||
                            tileGrid[i][j + 1].getComponentName().equalsIgnoreCase("edge")) {
                        // Check valid: nothing or not "open" above (already included, all openings 2-wide)
                        // But another "open" below
                        if ((i == 0 || !tileGrid[i - 1][j].getComponentName().equalsIgnoreCase("open")) &&
                                (i < height - 1 && tileGrid[i + 1][j].getComponentName().equalsIgnoreCase("open"))) {
                            if (!openings.containsKey("E")) {
                                openings.put("E", new ArrayList<>());
                            }
                            openings.get("E").add(new Vector2D(j, i));
                            break;
                        }
                    }
                }
            }
        }
        return openings;
    }

    /**
     * Creates all the monsters according to given quest information and places them randomly in the map on requested tile.
     *
     * @param dgs   - game state
     * @param quest - quest defining monsters
     * @param _data - all game data
     * @param rnd   - random generator
     */
    private void createMonsters(DescentGameState dgs, Quest quest, DescentGameData _data, Random rnd, int nActionsPerFigure) {
        dgs.monsters = new ArrayList<>();
        dgs.monstersOriginal = new ArrayList<>();
        dgs.monstersPerGroup = new ArrayList<>();
        dgs.monsterGroups = new ArrayList<>();
        List<String[]> monsters = quest.getMonsters();
        for (String[] mDef : monsters) {
            List<Monster> monsterGroup = new ArrayList<>();
            List<Monster> monsterOriginalGroup = new ArrayList<>();

            String nameDef = mDef[0];
            String name = nameDef.split(":")[0];
            String tile = mDef[1];
            Set<Vector2D> tileCoords = dgs.gridReferences.get(tile).keySet();
            int act = quest.getAct();
            Map<String, Monster> monsterDef = _data.findMonster(name);
            Monster superDef = monsterDef.get("super");
            int[] monsterSetup = ((PropertyIntArray) superDef.getProperty(setupHash)).getValues();

            // TODO: this could be adding/removing abilities too
            // Check attribute modifiers
            // Map from who (all/minion/master) -> list of modifiers in pairs (Attribute, howMuch)
            HashMap<String, ArrayList<Pair<Figure.Attribute, Integer>>> attributeModifiers = new HashMap<>();
            if (mDef.length > 2) {
                String mod = mDef[2];
                String[] modifiers = mod.split(";");
                for (String modifier : modifiers) {
                    String who = modifier.split(":")[0];
                    String attribute = modifier.split(":")[1];
                    String howMuch = modifier.split(":")[2];
                    int amount = Integer.parseInt(howMuch);

                    if (!attributeModifiers.containsKey(who)) {
                        attributeModifiers.put(who, new ArrayList<>());
                    }
                    attributeModifiers.get(who).add(new Pair<>(Figure.Attribute.valueOf(attribute), amount));
                }
            }

            // When playing with only 2 Heroes, some stronger monsters only spawn 1 Minion, with no Master
            // These are arrayed as [1, 0, 1] in the monster definition
            // So we check if there are only 2 Heroes (minimum 3 players - 2 Hero players and Overlord player)
            // If that is the case, we do not spawn a Master
            // Otherwise, there is always 1 Master

            boolean spawnMaster = true;

            if ((dgs.getNPlayers() <= 3 && monsterSetup[1] == 0)) {
                spawnMaster = false;
            }
            Monster master = monsterDef.get(act + "-master").copyNewID();
            master.getNActionsExecuted().setMaximum(nActionsPerFigure);
            master.setProperties(monsterDef.get(act + "-master").getProperties());
            master.setComponentName(name + " master");

            PropertyStringArray passives = (PropertyStringArray) master.getProperty("passive");
            if (passives != null)
                master.setPassivesAndSurges(passives.getValues());

            PropertyStringArray actions = ((PropertyStringArray) master.getProperty("action"));
            if (actions != null) {
                master.setActions(actions.getValues());
            }

            if (attributeModifiers.containsKey("master")) {
                for (Pair<Figure.Attribute, Integer> modifier : attributeModifiers.get("master")) {
                    master.getAttribute(modifier.a).setMaximum(master.getAttribute(modifier.a).getMaximum() + modifier.b);
                }
            }
            if (attributeModifiers.containsKey("all")) {
                for (Pair<Figure.Attribute, Integer> modifier : attributeModifiers.get("all")) {
                    master.getAttribute(modifier.a).setMaximum(master.getAttribute(modifier.a).getMaximum() + modifier.b);
                }
            }

            for (Surge surge : master.getSurges()) {
                master.addAbility(new SurgeAttackAction(surge, master.getComponentID()));
            }

            // If our Health was just modified, set it to the new maximum
            if (!master.getAttribute(Figure.Attribute.Health).isMaximum()) {
                master.setAttributeToMax(Figure.Attribute.Health);
            }

            // Don't spawn the Master monster if we're only supposed to spawn 1 Minion only
            if (spawnMaster) {
                placeMonster(dgs, master, new ArrayList<>(tileCoords), rnd, superDef);
                master.setOwnerId(dgs.overlordPlayer);
                monsterGroup.add(master);
                monsterOriginalGroup.add(master.copyNewID());
            }

            // How many minions?
            int nMinions;
            if (nameDef.contains("group")) {
                if (nameDef.contains("ignore")) {
                    // Ignore group limits, max number
                    nMinions = monsterSetup[monsterSetup.length - 1];
                } else {
                    // Respect group limits
                    nMinions = monsterSetup[Math.max(0, dgs.getNPlayers() - 3)];
                }
            } else {
                // Format name:#minions
                nMinions = Integer.parseInt(nameDef.split(":")[1]);
            }

            // Place minions
            for (int i = 0; i < nMinions; i++) {
                Monster minion = monsterDef.get(act + "-minion").copyNewID();
                minion.setProperties(monsterDef.get(act + "-minion").getProperties());
                minion.setComponentName(name + " minion " + (i + 1));

                passives = (PropertyStringArray) minion.getProperty("passive");
                if (passives != null)
                    minion.setPassivesAndSurges(passives.getValues());

                actions = ((PropertyStringArray) minion.getProperty("action"));
                if (actions != null)
                    minion.setActions(actions.getValues());

                for (Surge surge : minion.getSurges()) {
                    minion.addAbility(new SurgeAttackAction(surge, minion.getComponentID()));
                }

                placeMonster(dgs, minion, new ArrayList<>(tileCoords), rnd, superDef);
                minion.setOwnerId(dgs.overlordPlayer);
                minion.getNActionsExecuted().setMaximum(nActionsPerFigure);
                if (attributeModifiers.containsKey("minion")) {
                    for (Pair<Figure.Attribute, Integer> modifier : attributeModifiers.get("minion")) {
                        minion.getAttribute(modifier.a).setMaximum(minion.getAttribute(modifier.a).getMaximum() + modifier.b);
                    }
                }
                if (attributeModifiers.containsKey("all")) {
                    for (Pair<Figure.Attribute, Integer> modifier : attributeModifiers.get("all")) {
                        minion.getAttribute(modifier.a).setMaximum(minion.getAttribute(modifier.a).getMaximum() + modifier.b);
                    }
                }

                // If our Health was just modified, set it to the new maximum
                if (!minion.getAttribute(Figure.Attribute.Health).isMaximum()) {
                    minion.setAttributeToMax(Figure.Attribute.Health);
                }

                monsterGroup.add(minion);
                monsterOriginalGroup.add(minion.copyNewID());
            }
            dgs.monsters.add(monsterGroup);
            dgs.monstersOriginal.add(monsterOriginalGroup);
            dgs.monstersPerGroup.add(monsterOriginalGroup.size());
            dgs.monsterGroups.add(name);
        }

        //System.out.println(dgs.monsters.get(0).get(1).getSurges());
        //System.out.println(dgs.monstersOriginal);
        //System.out.println(dgs.monstersPerGroup);
    }

    /**
     * Places a monster in the board, randomly choosing one valid tile from given list.
     *
     * @param dgs        - current game state
     * @param monster    - monster to place
     * @param tileCoords - coordinate options for the monster
     * @param rnd        - random generator
     */
    private void placeMonster(DescentGameState dgs, Monster monster, List<Vector2D> tileCoords, Random rnd, Token superDef) {
        // Finish setup of monster
        monster.setProperties(superDef.getProperties());

        // TODO: maybe change orientation if monster doesn't fit vertically
        String size = ((PropertyString) monster.getProperty(sizeHash)).value;
        int w = Integer.parseInt(size.split("x")[0]);
        int h = Integer.parseInt(size.split("x")[1]);
        monster.setSize(w, h);

        while (tileCoords.size() > 0) {
            Vector2D option = tileCoords.get(rnd.nextInt(tileCoords.size()));
            tileCoords.remove(option);
            BoardNode position = dgs.masterBoard.getElement(option.getX(), option.getY());
            if (position.getComponentName().equals("plain") &&
                    ((PropertyInt) position.getProperty(playersHash)).value == -1) {
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
                                ((PropertyInt) tile.getProperty(playersHash)).value != -1) {
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
                    break;
                }
            }
        }
    }
}
