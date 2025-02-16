package games.root;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import evaluation.metrics.Event;
import games.GameType;
import games.root.components.cards.EyrieRulers;
import games.root.components.cards.RootCard;
import games.root.components.cards.RootQuestCard;
import games.root.components.cards.VagabondCharacter;
import games.root.components.Item;
import games.root.components.RootBoardNodeWithRootEdges;
import games.root.components.RootGraphBoard;

import java.util.*;

/**
 * Root Game State
 */
@SuppressWarnings("unused")
public class RootGameState extends AbstractGameState {
    protected RootGraphBoard gameMap;
    protected RootParameters.MapType mapType;
    protected int[] playerScores;
    protected RootParameters.VictoryCondition[] playerVictoryConditions;
    protected int playerSubGamePhase = 0;
    protected int actionsPlayed = 0;
    protected int playersSetUp = 0;
    protected List<RootParameters.Factions> playerFactions;

    /**
     * All game pieces
     */
    protected List<PartialObservableDeck<RootCard>> playerDecks;
    protected List<Deck<RootCard>> playerCraftedCards;
    protected List<List<Item>> craftedItems;
    protected Deck<RootCard> drawPile;
    protected Deck<RootCard> discardPile;
    protected Deck<RootQuestCard> questDrawPile;
    protected Deck<RootQuestCard> activeQuests;
    protected List<Item> craftableItems;
    protected List<Item> ruinItems;
    protected List<Item> startingItems;

    //region Cats state variables
    //Keep can be only placed once that is why the game state is only keeping track of whether the keep was destroyed
    protected boolean Keep;
    protected int CatWarriors;
    protected int Wood;
    protected int Workshops;
    protected int Recruiters;
    protected int Sawmills;
    //endregion

    //region Eyrie state variables
    protected int eyrieWarriors;
    protected List<Deck<RootCard>> eyrieDecree;
    protected List<RootParameters.ClearingTypes> playedSuits;
    protected Deck<EyrieRulers> rulers;
    protected Deck<RootCard> viziers;
    protected EyrieRulers activeRuler;
    protected int roosts;
    //endregion

    //region Woodland Alliance state variables
    protected int woodlandWarriors;
    protected int foxBase;
    protected int rabbitBase;
    protected int mouseBase;
    protected int sympathyTokens;
    protected int officers;
    protected PartialObservableDeck<RootCard> supporters;
    //endregion

    //region Vagabond state variables
    /**
     * The Satchel for holding damaged/undamaged items
     */
    protected VagabondCharacter vagabondCharacter;
    protected int vagabond;
    protected int foxQuests;
    protected int mouseQuests;
    protected int rabbitQuests;
    protected List<Item> satchel;
    protected List<Item> teas;
    protected List<Item> coins;
    protected List<Item> bags;
    protected HashMap<RootParameters.Factions, RootParameters.Relationship> relationships;
    protected HashMap<RootParameters.Factions, Integer> aidNumbers;
    //endregion

    public enum RootGamePhase implements IGamePhase {
        Setup,
        Birdsong,
        Daylight,
        Evening,
        Reaction
    }

    /**
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players in the game
     */
    public RootGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        playerScores = new int[nPlayers];
        playerVictoryConditions = new RootParameters.VictoryCondition[nPlayers];
        Arrays.fill(playerScores, 0);
        Arrays.fill(playerVictoryConditions, RootParameters.VictoryCondition.Score);
    }

    /**
     * @return the enum value corresponding to this game, declared in {@link GameType}.
     */
    @Override
    protected GameType _getGameType() {
        return GameType.Root;
    }

    /**
     * Returns all Components used in the game and referred to by componentId from actions or rules.
     * This method is called after initialising the game state, so all components will be initialised already.
     *
     * @return - List of Components in the game.
     */
    @Override
    protected List<Component> _getAllComponents() {
        ArrayList<Component> l = new ArrayList<>();
        l.add(gameMap);
        l.addAll(playerDecks);
        l.add(drawPile);
        l.add(discardPile);
        l.addAll(playerCraftedCards);
        l.addAll(craftableItems);
        for (List<Item> list : craftedItems) {
            l.addAll(list);
        }
        if (getNPlayers() > 1) {
            l.addAll(eyrieDecree);
            l.add(viziers);
            if (activeRuler != null) {
                l.add(activeRuler);
            }
            l.add(rulers);
        }
        if (getNPlayers() > 2) {
            l.add(supporters);
        }
        if (getNPlayers() > 3) {
            l.addAll(startingItems);
            l.addAll(ruinItems);
            if (vagabondCharacter != null) {
                l.add(vagabondCharacter);
            }
            l.addAll(craftableItems);
            l.addAll(satchel);
            l.addAll(bags);
            l.addAll(teas);
            l.addAll(coins);
            l.add(questDrawPile);
            l.add(activeQuests);
        }


        return l;
    }
    //Returns a deep copy of the game state from the viewpoints of a player
    @Override
    protected RootGameState _copy(int playerId) {
        RootGameState copy = new RootGameState(gameParameters, getNPlayers());
        copy.gameMap = gameMap.copy();
        copy.mapType = mapType;
        copy.playerFactions = new ArrayList<>(playerFactions);
        copy.discardPile = discardPile.copy();
        copy.playerScores = playerScores.clone();
        copy.playerVictoryConditions = playerVictoryConditions.clone();
        copy.gamePhase = gamePhase;
        copy.playersSetUp = playersSetUp;
        copy.drawPile = drawPile.copy();
        copy.setPlayerSubGamePhase(playerSubGamePhase);
        copy.setActionsPlayed(actionsPlayed);
        
        copy.craftableItems = new ArrayList<>();
        for (int i = 0; i < craftableItems.size(); i++) {
            copy.craftableItems.add(craftableItems.get(i).copy());
        }
        copy.ruinItems = new ArrayList<>();
        copy.startingItems = new ArrayList<>();

        copy.playerDecks = new ArrayList<>();
        copy.playerCraftedCards = new ArrayList<>();
        for (int e = 0; e < getNPlayers(); e++) {
            copy.playerCraftedCards.add(playerCraftedCards.get(e).copy());
        }
        copy.craftedItems = new ArrayList<>();
        for (int e = 0; e < craftedItems.size(); e++) {
            copy.craftedItems.add(new ArrayList<>());
            for (int i = 0; i < craftedItems.get(e).size(); i++) {
                copy.craftedItems.get(e).add(craftedItems.get(e).get(i).copy());
            }
        }
        copy.CatWarriors = CatWarriors;
        copy.Keep = Keep;
        copy.Sawmills = Sawmills;
        copy.Workshops = Workshops;
        copy.Recruiters = Recruiters;
        copy.Wood = Wood;

        if (getNPlayers() > 1) {
            copy.rulers = rulers.copy();
            copy.eyrieWarriors = eyrieWarriors;
            copy.eyrieDecree = new ArrayList<>();
            for (int j = 0; j < eyrieDecree.size(); j++) {
                copy.eyrieDecree.add(eyrieDecree.get(j).copy());
            }
            copy.playedSuits = new ArrayList<>();
            copy.playedSuits.addAll(playedSuits);
            copy.rulers = rulers.copy();
            copy.viziers = viziers.copy();
            if (activeRuler != null) {
                copy.activeRuler = activeRuler.copy();
            }
            copy.roosts = roosts;
        }

        if (getNPlayers() > 2) {
            copy.woodlandWarriors = woodlandWarriors;
            copy.foxBase = foxBase;
            copy.rabbitBase = rabbitBase;
            copy.mouseBase = mouseBase;
            copy.sympathyTokens = sympathyTokens;
            copy.officers = officers;
            copy.supporters = supporters.copy();
        }

        if (getNPlayers() > 3) {
            for (int i = 0; i < ruinItems.size(); i++) {
                copy.ruinItems.add(ruinItems.get(i).copy());
            }
            for (int i = 0; i < startingItems.size(); i++) {
                copy.startingItems.add(startingItems.get(i).copy());
            }
            copy.questDrawPile = questDrawPile.copy();
            if (playerId != -1) {
                copy.questDrawPile.shuffle(redeterminisationRnd);
            }
            copy.activeQuests = activeQuests.copy();
            if (vagabondCharacter != null) copy.vagabondCharacter = vagabondCharacter.copy();
            copy.vagabond = vagabond;
            copy.satchel = new ArrayList<>();
            for (int satchelCounter = 0; satchelCounter < satchel.size(); satchelCounter++) {
                copy.satchel.add(satchel.get(satchelCounter).copy());
            }
            copy.teas = new ArrayList<>();
            for (int teaCounter = 0; teaCounter < teas.size(); teaCounter++) {
                copy.teas.add(teas.get(teaCounter).copy());
            }
            copy.coins = new ArrayList<>();
            for (int coinCounter = 0; coinCounter < coins.size(); coinCounter++) {
                copy.coins.add(coins.get(coinCounter).copy());
            }
            copy.bags = new ArrayList<>();
            for (int bagCounter = 0; bagCounter < bags.size(); bagCounter++) {
                copy.bags.add(bags.get(bagCounter).copy());
            }
            copy.foxQuests = foxQuests;
            copy.rabbitQuests = rabbitQuests;
            copy.mouseQuests = mouseQuests;

            copy.relationships = new HashMap<>() {
                {
                    put(RootParameters.Factions.MarquiseDeCat, relationships.get(RootParameters.Factions.MarquiseDeCat));
                    put(RootParameters.Factions.EyrieDynasties, relationships.get(RootParameters.Factions.EyrieDynasties));
                    put(RootParameters.Factions.WoodlandAlliance, relationships.get(RootParameters.Factions.WoodlandAlliance));
                }
            };
            copy.aidNumbers = new HashMap<>() {
                {
                    put(RootParameters.Factions.MarquiseDeCat, aidNumbers.get(RootParameters.Factions.MarquiseDeCat));
                    put(RootParameters.Factions.EyrieDynasties, aidNumbers.get(RootParameters.Factions.EyrieDynasties));
                    put(RootParameters.Factions.WoodlandAlliance, aidNumbers.get(RootParameters.Factions.WoodlandAlliance));
                }
            };
        }

        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            copy.drawPile.shuffle(redeterminisationRnd);
            for (int i = 0; i < getNPlayers(); i++) {
                if (playerDecks.get(i).getOwnerId() == playerId) {
                    //owner of the deck gets a full copy
                    copy.playerDecks.add(playerDecks.get(i).copy());
                } else {
                    copy.playerDecks.add(playerDecks.get(i).copy());
                    int toDraw = 0;
                    for (int e = copy.playerDecks.get(i).getSize() - 1; e >= 0; e--) {
                        if (!copy.playerDecks.get(i).getVisibilityForPlayer(e, playerId)) {
                            //elements that are no visible/known get shuffled with drawPile and fresh cards are drawn
                            toDraw++;
                            copy.drawPile.add((copy.playerDecks.get(i).get(e)));
                            copy.playerDecks.get(i).remove(e);
                        }
                    }
                    copy.drawPile.shuffle(redeterminisationRnd);
                    for (int draw = 0; draw < toDraw; draw++) {
                        copy.playerDecks.get(i).add(copy.drawPile.draw());
                    }
                }
            }

            if (getPlayerFaction(playerId) != RootParameters.Factions.WoodlandAlliance && getNPlayers() > 2) {
                int supportersSize = copy.supporters.getSize();
                copy.drawPile.add(copy.supporters);
                copy.supporters.clear();
                copy.drawPile.shuffle(redeterminisationRnd);
                for (int i = 0; i < supportersSize; i++) {
                    copy.supporters.add(copy.drawPile.draw());
                }
            }
        } else {
            for (int i = 0; i < getNPlayers(); i++) {
                copy.playerDecks.add(playerDecks.get(i).copy());
            }
        }
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal()) {
            return new RootHeuristic().evaluateState(this, playerId);
        } else {
            // The game finished, we can instead return the actual result of the game for the given player.
            return getPlayerResults()[playerId].value;
        }
    }

    @Override
    public double getGameScore(int playerId) {
        return playerScores[playerId];
    }

    public void addGameScorePlayer(int playerID, int score) {
        if (playerVictoryConditions[playerID] == RootParameters.VictoryCondition.Score) {
            playerScores[playerID] += score;
        }
    }

    public void removeGameScorePlayer(int playerID) {
        if (playerVictoryConditions[playerID] == RootParameters.VictoryCondition.Score) {
            if (playerScores[playerID] > 0) {
                playerScores[playerID] -= 1;
            }
        }
    }

    public void setGameScorePlayer(int playerID, int score){
        if (playerVictoryConditions[playerID] == RootParameters.VictoryCondition.Score){
            playerScores[playerID] = score;
        }
    }

    public Deck<RootCard> getDrawPile() {
        return drawPile;
    }

    public Deck<RootQuestCard> getQuestDrawPile() {
        return questDrawPile;
    }

    public Deck<RootQuestCard> getActiveQuests() {
        return activeQuests;
    }

    public void setPlayerSubGamePhase(int number) {
        playerSubGamePhase = number;
    }

    public void setActionsPlayed(int number) {
        actionsPlayed = number;
    }

    public void increaseActionsPlayed() {
        actionsPlayed += 1;
    }

    public void decreaseActionsPlayed() {
        if (actionsPlayed > 0) {
            actionsPlayed--;
        }
    }

    public void increaseSubGamePhase() {
        playerSubGamePhase++;
        playedSuits.clear();
        actionsPlayed = 0;
    }

    public Deck<RootCard> getDiscardPile() {
        return discardPile;
    }

    public PartialObservableDeck<RootCard> getPlayerHand(int playerID) {
        return playerDecks.get(playerID);
    }

    public List<Item> getPlayerCraftedItems(int playerID) {
        return craftedItems.get(playerID);
    }

    public Deck<RootCard> getPlayerCraftedCards(int playerID) {
        return playerCraftedCards.get(playerID);
    }

    public boolean getKeep() {
        return Keep;
    }

    public int getWood() {
        return Wood;
    }

    public void setWood(int amount) {
        Wood = amount;
    }

    public void addWood() {
        Wood++;
    }

    public int getCatWarriors() {
        return CatWarriors;
    }

    public void removeCatWarrior() {
        CatWarriors--;
    }

    public void addCatWarrior() {
        CatWarriors++;
    }

    public int getBirdWarriors() {
        return eyrieWarriors;
    }

    public void removeBirdWarrior() {
        eyrieWarriors--;
    }

    public void addBirdWarrior() {
        eyrieWarriors++;
    }

    public int getWoodlandWarriors() {
        return woodlandWarriors;
    }

    public void removeWoodlandWarrior() {
        woodlandWarriors--;
    }

    public int getVagabond() {
        return vagabond;
    }

    public int getNumberOfTeas() {
        return teas.size();
    }

    public void removeVagabondWarrior() {
        vagabond--;
    }

    public void removeWood() {
        Wood--;
    }

    public void addWarrior(RootParameters.Factions faction) {
        switch (faction) {
            case MarquiseDeCat:
                CatWarriors++;
                break;
            case EyrieDynasties:
                eyrieWarriors++;
                break;
            case WoodlandAlliance:
                woodlandWarriors++;
                break;
            case Vagabond:
                System.out.println("Something went wrong -> attempting to remove a vagabond piece");
                break;
        }
    }

    public void removeBuilding(RootParameters.BuildingType bt) throws IllegalAccessException {
        switch (bt) {
            case Roost:
                if (roosts > 0) {
                    roosts--;
                } else {
                    throw new IllegalAccessException("Trying to place a roost which is not available");
                }
                break;
            case Recruiter:
                if (Recruiters > 0) {
                    Recruiters--;
                } else {
                    throw new IllegalAccessException("Trying to remove players recruiters which are not available");
                }
                break;
            case Workshop:
                if (Workshops > 0) {
                    Workshops--;
                } else {
                    throw new IllegalAccessException("Trying to remove players workshop which is not availabel");
                }
                break;
            case Sawmill:
                if (Sawmills > 0) {
                    Sawmills--;
                } else {
                    throw new IllegalAccessException("Trying to remove a sawmill which is not available");
                }
                break;
            case MouseBase:
                if (mouseBase > 0) {
                    mouseBase--;
                } else {
                    throw new IllegalAccessException("MouseBase unavailable");
                }
                break;
            case RabbitBase:
                if (rabbitBase > 0) {
                    rabbitBase--;
                } else {
                    throw new IllegalAccessException("RabbitBase unavailable");
                }
                break;
            case FoxBase:
                if (foxBase > 0) {
                    foxBase--;
                } else {
                    throw new IllegalAccessException("FoxBase unavailable");
                }
                break;
        }
    }

    public void addToken(RootParameters.TokenType tt) {
        switch (tt) {
            case Wood:
                Wood++;
                break;
            case Sympathy:
                sympathyTokens++;
                break;
            case Keep:
                //when destroyed the keep is no longer in game
                Keep = false;
        }
    }

    public void removeToken(RootParameters.TokenType tt) {
        switch (tt) {
            case Wood:
                if (Wood > 0) {
                    Wood--;
                } else {
                    System.out.println("Trying to remove wood token which does not exist");
                }
                break;
            case Sympathy:
                if (sympathyTokens > 0) {
                    sympathyTokens--;
                } else {
                    System.out.println("Trying to remove sympathy token which does not exist");
                }
                break;
            case Keep:
                // Keep can only be built once
                break;
        }
    }

    public void addBuilding(RootParameters.BuildingType type) {
        switch (type) {
            case Roost:
                roosts++;
                break;
            case Sawmill:
                Sawmills++;
                break;
            case Workshop:
                Workshops++;
                break;
            case Recruiter:
                Recruiters++;
                break;
            case FoxBase:
                foxBase++;
                break;
            case RabbitBase:
                rabbitBase++;
                break;
            case MouseBase:
                mouseBase++;
                break;
        }
    }

    @Override
    public boolean _equals(Object o) {
        if (!(o instanceof RootGameState that)) return false;
        return playerSubGamePhase == that.playerSubGamePhase && actionsPlayed == that.actionsPlayed && playersSetUp == that.playersSetUp && Keep == that.Keep && CatWarriors == that.CatWarriors && Wood == that.Wood && Workshops == that.Workshops && Recruiters == that.Recruiters && Sawmills == that.Sawmills && eyrieWarriors == that.eyrieWarriors && roosts == that.roosts && woodlandWarriors == that.woodlandWarriors && foxBase == that.foxBase && rabbitBase == that.rabbitBase && mouseBase == that.mouseBase && sympathyTokens == that.sympathyTokens && officers == that.officers && vagabond == that.vagabond && foxQuests == that.foxQuests && mouseQuests == that.mouseQuests && rabbitQuests == that.rabbitQuests && Objects.equals(gameMap, that.gameMap) && mapType == that.mapType && Objects.deepEquals(playerScores, that.playerScores) && Objects.deepEquals(playerVictoryConditions, that.playerVictoryConditions) && Objects.equals(playerFactions, that.playerFactions) && gamePhase == that.gamePhase && Objects.equals(playerDecks, that.playerDecks) && Objects.equals(playerCraftedCards, that.playerCraftedCards) && Objects.equals(craftedItems, that.craftedItems) && Objects.equals(drawPile, that.drawPile) && Objects.equals(discardPile, that.discardPile) && Objects.equals(questDrawPile, that.questDrawPile) && Objects.equals(activeQuests, that.activeQuests) && Objects.equals(craftableItems, that.craftableItems) && Objects.equals(ruinItems, that.ruinItems) && Objects.equals(startingItems, that.startingItems) && Objects.equals(eyrieDecree, that.eyrieDecree) && Objects.equals(playedSuits, that.playedSuits) && Objects.equals(rulers, that.rulers) && Objects.equals(viziers, that.viziers) && Objects.equals(activeRuler, that.activeRuler) && Objects.equals(supporters, that.supporters) && Objects.equals(vagabondCharacter, that.vagabondCharacter) && Objects.equals(satchel, that.satchel) && Objects.equals(teas, that.teas) && Objects.equals(coins, that.coins) && Objects.equals(bags, that.bags) && Objects.equals(relationships, that.relationships) && Objects.equals(aidNumbers, that.aidNumbers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameMap, mapType, Arrays.hashCode(playerScores),
                Arrays.hashCode(playerVictoryConditions), playerSubGamePhase, actionsPlayed, playersSetUp, playerFactions, playerDecks,
                playerCraftedCards, craftedItems, drawPile, discardPile, questDrawPile, activeQuests, craftableItems, ruinItems, startingItems, Keep,
                CatWarriors, Wood, Workshops, Recruiters, Sawmills, eyrieWarriors, eyrieDecree, playedSuits, rulers, viziers, activeRuler, roosts,
                woodlandWarriors, foxBase, rabbitBase, mouseBase, sympathyTokens, officers, supporters, vagabondCharacter, vagabond, foxQuests,
                mouseQuests, rabbitQuests, satchel, teas, coins, bags, relationships, aidNumbers, super.hashCode());
    }

    public RootGraphBoard getGameMap() {
        return gameMap;
    }

    public RootParameters.Factions getPlayerFaction(int playerID) {
        if (playerID == -1) {
            return null;
        }
        return playerFactions.get(playerID);
    }

    public Deck<EyrieRulers> getAvailableRulers() {
        return rulers;
    }

    public void addRulerToRulers(EyrieRulers ruler) {
        rulers.add(ruler);
    }

    public void setActiveRuler(EyrieRulers ruler) {
        activeRuler = ruler;
    }

    public void removeRulerFromList(EyrieRulers ruler) {
        rulers.remove(ruler);
    }

    public void addToDecree(int position, RootCard card) {
        eyrieDecree.get(position).add(card);
    }

    public List<Deck<RootCard>> getDecree() {
        return eyrieDecree;
    }

    public List<RootParameters.ClearingTypes> getDecreeSuits(int index) {
        Deck<RootCard> decreePart = eyrieDecree.get(index);
        List<RootParameters.ClearingTypes> availableTypes = new ArrayList<>();
        for (int i = 0; i < decreePart.getSize(); i++) {
            availableTypes.add(decreePart.get(i).suit);
        }
        availableTypes.removeAll(playedSuits);
        Set<RootParameters.ClearingTypes> uniqueAvailableTypes = new LinkedHashSet<>(availableTypes);
        return new ArrayList<>(uniqueAvailableTypes);
    }

    public void addPlayedSuit(RootParameters.ClearingTypes type) {
        playedSuits.add(type);
    }

    public Deck<RootCard> getViziers() {
        return viziers;
    }

    public PartialObservableDeck<RootCard> getSupporters() {
        return supporters;
    }

    public int getOfficers() {
        return officers;
    }

    public void addOfficer() throws IllegalAccessException {
        if (woodlandWarriors > 0) {
            officers++;
            woodlandWarriors--;
        } else {
            throw new IllegalAccessException("Trying to add and officer with no available warriors");
        }
    }

    public void incrementPlayersSetUp() {
        playersSetUp++;
    }

    public void setVagabondCharacter(VagabondCharacter character) {
        if (vagabondCharacter == null) {
            vagabondCharacter = character;
        }
    }

    public List<Item> getStartingItems() {
        return startingItems;
    }

    public List<Item> getCraftableItems() {
        return craftableItems;
    }

    public List<Item> getSatchel() {
        return satchel;
    }

    public int getSympathyTokens() {
        return sympathyTokens;
    }

    public void removeSympathyTokens() {
        if (sympathyTokens > 0) {
            sympathyTokens--;
        }
    }

    public boolean supportersContainClearingType(RootParameters.ClearingTypes clearingType) {
        int counter = 0;
        for (int i = 0; i < supporters.getSize(); i++) {
            if (supporters.get(i).suit == clearingType || supporters.get(i).suit == RootParameters.ClearingTypes.Bird) {
                counter++;
            }
        }
        RootParameters rp = (RootParameters) getGameParameters();
        return counter >= rp.sympathyDiscardCost.get(sympathyTokens);
    }

    public String getVagabondType() {
        if (vagabondCharacter == null) {
            return "Vagabond";
        }
        return vagabondCharacter.characterType.toString();
    }

    public int getBuildingCount(RootParameters.BuildingType bt) {
        return switch (bt) {
            case Workshop -> Workshops;
            case Recruiter -> Recruiters;
            case Sawmill -> Sawmills;
            case Roost -> roosts;
            case FoxBase -> foxBase;
            case MouseBase -> mouseBase;
            case RabbitBase -> rabbitBase;
            default -> 0;
        };
    }

    public int getTokenCount(RootParameters.TokenType tt) {
        return switch (tt) {
            case Keep -> Keep ? 1 : 0;
            case Sympathy -> sympathyTokens;
            case Wood -> Wood;
            default -> 0;
        };
    }

    public String getRulerName() {
        if (activeRuler == null) {
            return "None";
        }
        return activeRuler.ruler.toString();
    }

    public EyrieRulers getRuler() {
        return activeRuler;
    }

    public int getFactionPlayerID(RootParameters.Factions faction) {
        for (int i = 0; i < playerFactions.size(); i++) {
            if (playerFactions.get(i) == faction) {
                return i;
            }
        }
        return 0;
    }

    public int getRefreshedItemCount(Item.ItemType itemType) {
        int result = 0;
        for (Item item : satchel) {
            if (item.itemType == itemType && item.refreshed && !item.damaged) {
                result++;
            }
        }
        return result;
    }

    public List<Item> getTeas() {
        return teas;
    }

    public List<Item> getCoins() {
        return coins;
    }

    public List<Item> getBags() {
        return bags;
    }

    public Item getRandomRuinItem() {
        if (!ruinItems.isEmpty()) {
            int randomIndex = getRnd().nextInt(ruinItems.size());
            Item item = ruinItems.get(randomIndex);
            ruinItems.remove(randomIndex);
            return item;
        }
        return null;
    }

    public int getDamagedAndExhaustedNonSatchelItems() {
        int i = 0;
        for (Item bag : bags) {
            if (bag.damaged || !bag.refreshed) {
                i++;
            }
        }
        for (Item tea : teas) {
            if (tea.damaged || !tea.refreshed) {
                i++;
            }
        }
        for (Item coin : coins) {
            if (coin.damaged || !coin.refreshed) {
                i++;
            }
        }
        return i;
    }

    public boolean canOverwork(int playerID) {
        PartialObservableDeck<RootCard> hand = getPlayerHand(playerID);
        for (RootBoardNodeWithRootEdges location : gameMap.getSawmills()) {
            for (int i = 0; i < hand.getSize(); i++) {
                if (hand.get(i).suit == location.getClearingType() || hand.get(i).suit == RootParameters.ClearingTypes.Bird) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean locationIsOverworkable(int playerID, int nodeID) {
        RootBoardNodeWithRootEdges location = gameMap.getNodeByID(nodeID);
        PartialObservableDeck<RootCard> hand = getPlayerHand(playerID);
        for (int i = 0; i < hand.getSize(); i++) {
            if (hand.get(i).suit == location.getClearingType() || hand.get(i).suit == RootParameters.ClearingTypes.Bird) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEnoughAvailableWood(int playerID, int locationID, int cost) {
        // Check whether there is enough wood on the map to be used in the target location
        // A BFS method that terminates upon finding the required amount of wood
        int availableWood = 0;
        RootBoardNodeWithRootEdges location = gameMap.getNodeByID(locationID);
        if (location == null) {
            return false; // Return false if the location does not exist
        }
        Set<Integer> explored = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(location.getComponentID());
        explored.add(location.getComponentID());

        while (availableWood < cost && !queue.isEmpty()) {
            RootBoardNodeWithRootEdges current = gameMap.getNodeByID(queue.poll());

            if (current == null) {
                continue; // Skip null nodes if any
            }

            if (current.rulerID == playerID) {
                availableWood += current.getWood();
            }

            for (RootBoardNodeWithRootEdges neighbour : current.getNeighbours()) {
                int neighbourID = neighbour.getComponentID();
                if (!explored.contains(neighbourID) && neighbour.rulerID == playerID && neighbour.getClearingType() != RootParameters.ClearingTypes.Forrest) {
                    queue.add(neighbourID);
                    explored.add(neighbourID);
                }
            }
        }

        return availableWood >= cost;
    }

    public boolean isConnected(int playerID, int location1, int location2) {
        // A two-way BFS algorithm which checks whether two clearings are connected via a path of clearings with the same ruler
        RootBoardNodeWithRootEdges locationFirst = gameMap.getNodeByID(location1);
        RootBoardNodeWithRootEdges locationSecond = gameMap.getNodeByID(location2);

        // Null checks for initial locations
        if (locationFirst == null || locationSecond == null) {
            return false;
        }

        // Check if both locations have the same ruler
        if (locationFirst.rulerID != playerID || locationSecond.rulerID != playerID) {
            return false;
        }

        Set<Integer> exploredFirst = new HashSet<>();
        Set<Integer> exploredSecond = new HashSet<>();
        Queue<Integer> queueFirst = new LinkedList<>();
        Queue<Integer> queueSecond = new LinkedList<>();

        // Initialize the queues and explored sets
        queueFirst.add(locationFirst.getComponentID());
        queueSecond.add(locationSecond.getComponentID());
        exploredFirst.add(locationFirst.getComponentID());
        exploredSecond.add(locationSecond.getComponentID());

        while (!queueFirst.isEmpty() || !queueSecond.isEmpty()) {
            // Process nodes from the first queue
            if (!queueFirst.isEmpty()) {
                RootBoardNodeWithRootEdges currentFirst = gameMap.getNodeByID(queueFirst.poll());
                if (currentFirst != null) {
                    if (exploredSecond.contains(currentFirst.getComponentID())) {
                        return true;
                    }
                    for (RootBoardNodeWithRootEdges neighbourFirst : currentFirst.getNeighbours()) {
                        int neighbourID = neighbourFirst.getComponentID();
                        if (!exploredFirst.contains(neighbourID) && neighbourFirst.rulerID == playerID && neighbourFirst.getClearingType() != RootParameters.ClearingTypes.Forrest) {
                            queueFirst.add(neighbourID);
                            exploredFirst.add(neighbourID);
                        }
                    }
                }
            }

            // Process nodes from the second queue
            if (!queueSecond.isEmpty()) {
                RootBoardNodeWithRootEdges currentSecond = gameMap.getNodeByID(queueSecond.poll());
                if (currentSecond != null) {
                    if (exploredFirst.contains(currentSecond.getComponentID())) {
                        return true;
                    }
                    for (RootBoardNodeWithRootEdges neighbourSecond : currentSecond.getNeighbours()) {
                        int neighbourID = neighbourSecond.getComponentID();
                        if (!exploredSecond.contains(neighbourID) && neighbourSecond.rulerID == playerID && neighbourSecond.getClearingType() != RootParameters.ClearingTypes.Forrest) {
                            queueSecond.add(neighbourID);
                            exploredSecond.add(neighbourID);
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean canBuildCatBuilding(int playerID) {
        //checks whether any type of cat building is buildable
        RootParameters rp = (RootParameters) getGameParameters();
        int workshopCost = rp.getCatBuildingCost(Workshops);
        int sawmillCost = rp.getCatBuildingCost(Sawmills);
        int recruiterCost = rp.getCatBuildingCost(Recruiters);
        for (RootBoardNodeWithRootEdges location : gameMap.getNonForrestBoardNodes()) {
            if (location.hasBuildingRoom() && location.rulerID == playerID) {
                if (hasEnoughAvailableWood(playerID, location.getComponentID(), workshopCost)) {
                    return true;
                }
                if (hasEnoughAvailableWood(playerID, location.getComponentID(), sawmillCost)) {
                    return true;
                }
                if (hasEnoughAvailableWood(playerID, location.getComponentID(), recruiterCost)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canBuildSpecificCatBuilding(int playerID, int cost) {
        for (RootBoardNodeWithRootEdges location : gameMap.getNonForrestBoardNodes()) {
            if (location.hasBuildingRoom() && location.rulerID == playerID) {
                if (hasEnoughAvailableWood(playerID, location.getComponentID(), cost)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canCraftCard(List<RootParameters.ClearingTypes> available, RootCard card) {
        if (card.craftingType == RootCard.CraftingType.itemCard) {
            if (craftableItems.stream().noneMatch(item -> item.itemType == card.getCraftableItem())) {
                return false;
            }
        }
        Map<RootParameters.ClearingTypes, Integer> availableMap = new HashMap<>();

        for (RootParameters.ClearingTypes clearingType : available) {
            availableMap.put(clearingType, availableMap.getOrDefault(clearingType, 0) + 1);
        }
        int anyCount = 0;
        for (RootParameters.ClearingTypes any : card.craftingCost) {
            if (any.equals(RootParameters.ClearingTypes.Bird)) {
                anyCount++;
            }
        }
        for (RootParameters.ClearingTypes cost : card.craftingCost) {
            if (!cost.equals(RootParameters.ClearingTypes.Bird)) {
                if (availableMap.containsKey(cost) && availableMap.get(cost) > 0) {
                    availableMap.put(cost, availableMap.get(cost) - 1);
                } else {
                    return false;
                }
            }
        }
        int remainingAvailableTypes = 0;
        for (int count : availableMap.values()) {
            remainingAvailableTypes += count;
        }
        return remainingAvailableTypes >= anyCount;
    }

    public boolean canCraft(int playerID, List<RootParameters.ClearingTypes> available) {
        PartialObservableDeck<RootCard> hand = getPlayerHand(playerID);
        for (int i = 0; i < hand.getSize(); i++) {
            if (hand.get(i).craftingType != RootCard.CraftingType.unCraftable) {
                if (Objects.requireNonNull(hand.get(i).craftingType) == RootCard.CraftingType.itemCard) {
                    Item.ItemType itemType = hand.get(i).getCraftableItem();
                    if (craftableItems.stream().anyMatch(item -> item.itemType == itemType)) {
                        if (canCraftCard(available, hand.get(i))) {
                            return true;
                        }
                    }
                } else {
                    if (canCraftCard(available, hand.get(i))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean canAttack(int playerID) {
        switch (getPlayerFaction(playerID)) {
            case MarquiseDeCat:
                for (RootBoardNodeWithRootEdges clearing : gameMap.getNonForrestBoardNodes()) {
                    if (clearing.getWarrior(RootParameters.Factions.MarquiseDeCat) > 0) {
                        for (int i = 0; i < getNPlayers(); i++) {
                            if (i != playerID && clearing.isAttackable(getPlayerFaction(i))) {
                                return true;
                            }
                        }
                    }
                }
                break;
            case EyrieDynasties:
                for (RootParameters.ClearingTypes clearingType : getDecreeSuits(2)) {
                    for (RootBoardNodeWithRootEdges clearing : gameMap.getNonForrestBoardNodes()) {
                        if (clearing.getWarrior(RootParameters.Factions.EyrieDynasties) > 0 && (clearing.getClearingType() == clearingType || clearingType == RootParameters.ClearingTypes.Bird)) {
                            for (int i = 0; i < getNPlayers(); i++) {
                                if (i != playerID && clearing.isAttackable(getPlayerFaction(i))) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                break;
            case WoodlandAlliance:
                for (RootBoardNodeWithRootEdges clearing : gameMap.getNonForrestBoardNodes()) {
                    if (clearing.getWarrior(RootParameters.Factions.WoodlandAlliance) > 0) {
                        for (int i = 0; i < getNPlayers(); i++) {
                            if (i != playerID && clearing.isAttackable(getPlayerFaction(i))) {
                                return true;
                            }
                        }
                    }
                }
                break;
            case Vagabond:
                RootBoardNodeWithRootEdges vagabondClearing = gameMap.getVagabondClearing();
                if (vagabondClearing.getClearingType() != RootParameters.ClearingTypes.Forrest) {
                    for (int i = 0; i < getNPlayers(); i++) {
                        if (i != playerID && vagabondClearing.isAttackable(getPlayerFaction(i))) {
                            return true;
                        }
                    }
                }
                break;
        }
        return false;
    }

    public boolean canSteal(int playerID){
        RootBoardNodeWithRootEdges clearing = gameMap.getVagabondClearing();
        for (int i = 0; i <getNPlayers(); i++){
            if (i != playerID && clearing.isAttackable(getPlayerFaction(i)) && getPlayerHand(i).getSize()>0){
                return true;
            }
        }
        return false;
    }

    public int getVagabondUndamagedSwords() {
        int swords = 0;
        for (Item item : satchel) {
            if (item.itemType == Item.ItemType.sword && !item.damaged) {
                swords++;
            }
        }
        return swords;
    }

    public boolean canAid(int playerID) {
        boolean hasItemToExhaust = false;
        boolean hasMatchingCard = false;
        for (Item item : getSatchel()) {
            if (item.refreshed && !item.damaged) {
                hasItemToExhaust = true;
                break;
            }
        }
        for (Item bag : getBags()) {
            if (bag.refreshed && !bag.damaged) {
                hasItemToExhaust = true;
                break;
            }
        }
        for (Item tea : getTeas()) {
            if (tea.refreshed && !tea.damaged) {
                hasItemToExhaust = true;
                break;
            }
        }
        for (Item coin : getCoins()) {
            if (coin.refreshed && !coin.damaged) {
                hasItemToExhaust = true;
                break;
            }
        }

        PartialObservableDeck<RootCard> hand = getPlayerHand(playerID);
        RootBoardNodeWithRootEdges clearing = getGameMap().getVagabondClearing();
        for (int i = 0; i < hand.getSize(); i++) {
            if (hand.get(i).suit == clearing.getClearingType() || hand.get(i).suit == RootParameters.ClearingTypes.Bird) {
                hasMatchingCard = true;
            }
        }

        return hasItemToExhaust && hasMatchingCard && canAttack(playerID);
    }

    public boolean canCompleteSpecificQuest(RootQuestCard card) {
        if (card.suit == gameMap.getVagabondClearing().getClearingType() && getRefreshedItemCount(card.requirement1) > 0 && getRefreshedItemCount(card.requirement2) > 0) {
            if (card.requirement1 == card.requirement2) {
                return getRefreshedItemCount(card.requirement1) > 1;
            }
            return true;
        }
        return false;
    }

    public boolean canCompleteQuest() {
        for (int i = 0; i < activeQuests.getSize(); i++) {
            if (canCompleteSpecificQuest(activeQuests.get(i))) {
                return true;
            }
        }
        return false;
    }

    public void completeQuest(RootParameters.ClearingTypes clearingType) {
        switch (clearingType) {
            case Fox -> foxQuests++;
            case Rabbit -> rabbitQuests++;
            case Mouse -> mouseQuests++;
        }
    }

    public int getCompletedQuests(RootParameters.ClearingTypes clearingType) {
        return switch (clearingType) {
            case Mouse -> mouseQuests;
            case Rabbit -> rabbitQuests;
            case Fox -> foxQuests;
            default -> 0;
        };
    }

    public void aid(int playerID, RootParameters.Factions faction) {
        //noinspection StatementWithEmptyBody
        if (relationships.get(faction) == RootParameters.Relationship.Hostile) {
            //nothing
        } else if (relationships.get(faction) == RootParameters.Relationship.Neutral) {
            aidNumbers.put(faction, aidNumbers.get(faction) + 1);
            if (aidNumbers.get(faction) >= 1) {
                relationships.put(faction, RootParameters.Relationship.One);
                addGameScorePlayer(playerID, 1);
                aidNumbers.put(faction, 0);
                logEvent(Event.GameEvent.GAME_EVENT, faction.toString() + " is now at relationship One");
            }
        } else if (relationships.get(faction) == RootParameters.Relationship.One) {
            aidNumbers.put(faction, aidNumbers.get(faction) + 1);
            if (aidNumbers.get(faction) >= 2) {
                relationships.put(faction, RootParameters.Relationship.Two);
                addGameScorePlayer(playerID, 2);
                aidNumbers.put(faction, 0);
                logEvent(Event.GameEvent.GAME_EVENT, faction.toString() + " is now at relationship Two");
            }
        } else if (relationships.get(faction) == RootParameters.Relationship.Two) {
            aidNumbers.put(faction, aidNumbers.get(faction) + 1);
            if (aidNumbers.get(faction) >= 3) {
                relationships.put(faction, RootParameters.Relationship.Allied);
                addGameScorePlayer(playerID, 2);
                aidNumbers.put(faction, 0);
                logEvent(Event.GameEvent.GAME_EVENT, faction.toString() + " is now at relationship Allied");
            }
        } else if (relationships.get(faction) == RootParameters.Relationship.Allied) {
            aidNumbers.put(faction, aidNumbers.get(faction) + 1);
            addGameScorePlayer(playerID, 2);
        }
    }

    public void clearAidCounter(){
        aidNumbers.put(RootParameters.Factions.MarquiseDeCat,0);
        aidNumbers.put(RootParameters.Factions.EyrieDynasties,0);
        aidNumbers.put(RootParameters.Factions.WoodlandAlliance,0);
    }

    public RootParameters.Relationship getRelationship(RootParameters.Factions faction){
        return relationships.get(faction);
    }

    public void setHostile(RootParameters.Factions faction){
        relationships.put(faction, RootParameters.Relationship.Hostile);
    }

    public void setPlayerVictoryCondition (int playerID, RootParameters.VictoryCondition victoryCondition){
        playerVictoryConditions[playerID] = victoryCondition;
    }

    public RootParameters.VictoryCondition getPlayerVictoryCondition(int playerID){
        return playerVictoryConditions[playerID];
    }

    public boolean scoreGameOver(){
        return Arrays.stream(playerScores).anyMatch(x -> x >= ((RootParameters)getGameParameters()).scoreToEnd);
    }
}
