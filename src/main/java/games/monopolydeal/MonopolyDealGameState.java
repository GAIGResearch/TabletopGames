package games.monopolydeal;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import games.GameType;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.PropertySet;
import games.monopolydeal.cards.SetType;
import org.apache.poi.ss.formula.atp.Switch;
import scala.sys.Prop;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;

import java.util.*;

/**
 * <p>The game state encapsulates all game information. It is a data-only class, with game functionality present
 * in the Forward Model or actions modifying the state of the game.</p>
 * <p>Most variables held here should be {@link Component} subclasses as much as possible.</p>
 * <p>No initialisation or game logic should be included here (not in the constructor either). This is all handled externally.</p>
 * <p>Computation may be included in functions here for ease of access, but only if this is querying the game state information.
 * Functions on the game state should never <b>change</b> the state of the game.</p>
 */
public class MonopolyDealGameState extends AbstractGameState {

    MonopolyDealParameters params;
    Random rnd;

    //GameState members
    //Player Data members
    PartialObservableDeck<MonopolyDealCard>[] playerHands;
    //Board[] playerBoards;
    Deck<MonopolyDealCard>[] playerBanks;
    List<PropertySet>[] playerPropertySets;

    Deck<MonopolyDealCard> drawPile;
    Deck<MonopolyDealCard> discardPile;

    // Player turn status members
    int actionsLeft = 3;
    boolean turnStart = true;
    // Decision making placeholders for extended action sequence
    // int target = 99;
    // SlyDeal and ForcedDeal
    // MonopolyDealCard give,take;

    /**
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players in the game
     */
    public MonopolyDealGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);

        rnd = new Random(gameParameters.getRandomSeed());
        params = (MonopolyDealParameters) gameParameters;
        actionsLeft = params.ACTIONS_PER_TURN;
        turnStart = true;
        this._reset();
    }

    protected void _reset() {
        playerHands = new PartialObservableDeck[getNPlayers()];
        playerBanks = new Deck[getNPlayers()];
        playerPropertySets = new List[getNPlayers()];

        drawPile = new Deck<>("Draw Pile",HIDDEN_TO_ALL);
        discardPile = new Deck<>("Discard Pile",VISIBLE_TO_ALL);
        for(int i=0;i<getNPlayers();i++){
            boolean[] handVisibility = new boolean[getNPlayers()];
            handVisibility[i] = true;
            playerHands[i] = new PartialObservableDeck<>("Hand of Player " + i + 1, handVisibility);
            playerBanks[i] = new Deck<>("Bank of Player"+i+1,VISIBLE_TO_ALL);
            playerPropertySets[i] = new ArrayList<>();
        }
    }

    /**
     * @return the enum value corresponding to this game, declared in {@link GameType}.
     */
    @Override
    protected GameType _getGameType() {
        return GameType.MonopolyDeal;
    }

    /**
     * Returns all Components used in the game and referred to by componentId from actions or rules.
     * This method is called after initialising the game state, so all components will be initialised already.
     *
     * @return - List of Components in the game.
     */
    @Override
    protected List<Component> _getAllComponents() {
        // add all components to the list
        List<Component> components = new ArrayList<>();
        components.addAll(Arrays.asList(playerHands));
        components.addAll(Arrays.asList(playerBanks));
        for(int i=0;i<getNPlayers();i++){
            components.addAll(playerPropertySets[i]);
        }
        components.add(discardPile);
        components.add(drawPile);
        return components;
    }

    /**
     * <p>Create a deep copy of the game state containing only those components the given player can observe.</p>
     * <p>If the playerID is NOT -1 and If any components are not visible to the given player (e.g. cards in the hands
     * of other players or a face-down deck), then these components should instead be randomized (in the previous examples,
     * the cards in other players' hands would be combined with the face-down deck, shuffled together, and then new cards drawn
     * for the other players).</p>
     * <p>If the playerID passed is -1, then full observability is assumed and the state should be faithfully deep-copied.</p>
     *
     * <p>Make sure the return type matches the class type, and is not AbstractGameState.</p>
     *
     * @param playerId - player observing this game state.
     */
    @Override
    protected MonopolyDealGameState _copy(int playerId) {
        MonopolyDealGameState retValue = new MonopolyDealGameState(gameParameters, getNPlayers());
        // TODO: deep copy all variables to the new game state.

        // Placeholder to know how many cards each player had for redrawing cards
        int[] playerHandSize = new int[getNPlayers()];
        retValue.drawPile = drawPile.copy();
        retValue.rnd = rnd;

        // Hidden values
        for (int p = 0; p < getNPlayers(); p++) {
            if (playerId == -1) {
                // No hidden information
                retValue.playerHands[p] = playerHands[p].copy();
                
            } else if (playerId == p) {
                // Current players hand is visible information
                retValue.playerHands[p] = playerHands[p].copy();
            } else{
                retValue.playerHands[p] = playerHands[p].copy();
                playerHandSize[p] = retValue.playerHands[p].getSize();

                // Adding all hidden cards back to deck
                retValue.drawPile.add(retValue.playerHands[p]);
                retValue.playerHands[p].clear();
            }
        }
        retValue.drawPile.shuffle(rnd);
        // Redrawing hidden cards into hands
        for (int p = 0; p < getNPlayers(); p++){
            if(p != playerId && playerId!= -1){
                retValue.drawCard(p,playerHandSize[p]);
            }
        }
        // Completely visible values
        for(int i=0;i<getNPlayers();i++){
            retValue.playerBanks[i] = playerBanks[i].copy();
            for (PropertySet propertySet:playerPropertySets[i]) {
                retValue.playerPropertySets[i].add(propertySet.copy());
            }
        }
        retValue.discardPile = discardPile.copy();
        retValue.actionsLeft = actionsLeft;
        retValue.turnStart = turnStart;

        return retValue;
    }

    public boolean canModifyBoard(int playerID){
        for (PropertySet pSet: playerPropertySets[playerID]) {
            if(pSet.hasWild) return true;
        }
        return false;
    }
    public void drawCard(int playerID,int drawCount){
        for(int i=0;i<drawCount;i++) {
            if(drawPile.getSize() == 0){
                resetDrawPile();
            }
            playerHands[playerID].add(drawPile.draw());
        }
    }
    public void resetDrawPile(){
        drawPile.add(discardPile);
        discardPile.clear();
        drawPile.shuffle(rnd);
    }
    public void endTurn() {
        actionsLeft = params.ACTIONS_PER_TURN;
        turnStart = true;
    }
    public void discardCard(MonopolyDealCard card, int playerID) {
        playerHands[playerID].remove(card);
        discardPile.add(card);
    }
    public void addMoney(int playerID, MonopolyDealCard money){
        playerBanks[playerID].add(money);
        playerHands[playerID].remove(money);
    }
    public void removeMoneyFrom(int playerID, MonopolyDealCard money) {
        playerBanks[playerID].add(money);
    }
    public boolean isBoardEmpty(int playerID){
        if(playerBanks[playerID].getSize() == 0 && playerPropertySets[playerID].size() == 0) return true;
        else return false;
    }
    // add property
    public void addProperty(int playerID, MonopolyDealCard card){
        SetType SType = card.getUseAs();
        addPropertyToSet(playerID,card,SType);
    }
    public void addPropertyToSet(int playerID, MonopolyDealCard card, SetType SType){
        playerHands[playerID].remove(card);
        card.setUseAs(SType);
        int indx = getSetIndx(playerID,SType);
        if(indx != 99){
            playerPropertySets[playerID].get(indx).add(card);
        }
        else{
            PropertySet pSet = new PropertySet(SType.toString(),VISIBLE_TO_ALL,SType);
            pSet.add(card);
            playerPropertySets[playerID].add(pSet);
        }
    }
    public void removePropertyFrom(int playerID, MonopolyDealCard card, SetType from){
        int indx = getSetIndx(playerID,from);
        playerPropertySets[playerID].get(indx).remove(card);
        if(playerPropertySets[playerID].get(indx).stream().count() == 0){
            playerPropertySets[playerID].remove(indx);
        }
    }
    public void movePropertySetFromTo(SetType setType, int target, int playerID) {
        int indx = getSetIndx(target,setType);
        PropertySet pSet = playerPropertySets[target].get(indx);
        playerPropertySets[target].remove(indx);
        playerPropertySets[playerID].add(pSet);
    }
    // Using setSize as a verification incase there are multiple versions of same setType
    public int getSetIndx(int playerID, SetType type){
        int setIndx = 99;
        for (PropertySet set:playerPropertySets[playerID]) {
            if(set.getSetType() == type){
                setIndx = playerPropertySets[playerID].indexOf(set);
            }
        }
        return setIndx;
    }
    public boolean checkForForcedDeal(int playerID){
        boolean target;
        target = checkForSlyDeal(playerID);
        if(target) {
            for (PropertySet pSet: playerPropertySets[playerID]) {
                if((!pSet.hasHouse || !pSet.hasHotel) && pSet.getSize() > 0){
                    return true;
                }
            }
        }
        return false;
    }
    public boolean checkForSlyDeal(int playerID){
        for(int i=0;i<getNPlayers();i++){
            if(i!=playerID && checkForFreeProperty(i))
                return true;
        }
        return false;
    }
    // Returns true if there is a free property( i.e. property which can be stolen/ traded )
    public boolean checkForFreeProperty(int playerID){
        for (PropertySet pSet: playerPropertySets[playerID]) {
            if(!pSet.isComplete){
                return true;
            }
        }
        return false;
    }
    public boolean checkForMulticolorRent(int playerID){
        for (PropertySet pSet: playerPropertySets[playerID]) {
            if(pSet.getSetType() != SetType.UNDEFINED){
                return true;
            }
        }
        return false;
    }
    public boolean checkForDealBreaker(int playerID){
        for(int i = 0;i <getNPlayers();i++){
            if(i!=playerID && playerDealBreaker(i)) return true;
        }
        return false;
    }
    public boolean playerDealBreaker(int playerID){
        for (PropertySet pSet: playerPropertySets[playerID]) {
            if(pSet.isComplete) return true;
        }
        return false;
    }
    public boolean checkActionCard(int playerID, CardType cardType){
        switch (cardType){
            case ForcedDeal:
                return checkForForcedDeal(playerID);
            case SlyDeal:
                return checkForSlyDeal(playerID);
            case DealBreaker:
                return checkForDealBreaker(playerID);
            case MulticolorRent:
                return checkForMulticolorRent(playerID);
            case PassGo:
            case DebtCollector:
                return true;
            case JustSayNo:
            case DoubleTheRent:
            default:
                return false;
        }
    }
    public boolean checkForActionCards(int playerID) {
        boolean check = false;
        for(int i=0;i<playerHands[playerID].getSize();i++){
            check = checkActionCard(playerID,playerHands[playerID].get(i).cardType());
            if(check) return true;
        }
        return check;
    }
    public void useAction(int actionCost) {
        actionsLeft = actionsLeft-actionCost;
    }
    public int getActionsLeft(){return actionsLeft;}
    // remove property
    public Deck<MonopolyDealCard> getPlayerHand(int playerID){
        return playerHands[playerID];
    }
    public List<PropertySet> getPropertySets(int playerID) {
        return playerPropertySets[playerID];
    }
    public Deck<MonopolyDealCard> getPlayerBank(int playerId) { return playerBanks[playerId]; }
    /**
     * @param playerId - player observing the state.
     * @return a score for the given player approximating how well they are doing (e.g. how close they are to winning
     * the game); a value between 0 and 1 is preferred, where 0 means the game was lost, and 1 means the game was won.
     */
    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal()) {
            // TODO calculate an approximate value
            return 0;
        } else {
            // The game finished, we can instead return the actual result of the game for the given player.
            return getPlayerResults()[playerId].value;
        }
    }

    /**
     * @param playerId - player observing the state.
     * @return the true score for the player, according to the game rules. May be 0 if there is no score in the game.
     */
    @Override
    public double getGameScore(int playerId) {
        // TODO: What is this player's score (if any)?
//        return 0;
        int count = 0;
        for (PropertySet pSet:playerPropertySets[playerId]) {
            if(pSet.isComplete){
                count++;
            }
        }
        return count/params.SETS_TO_WIN;
    }
    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MonopolyDealGameState state = (MonopolyDealGameState) o;
        return actionsLeft == state.actionsLeft && turnStart == state.turnStart && Objects.equals(params, state.params) && Objects.equals(rnd, state.rnd) && Arrays.equals(playerHands, state.playerHands) && Arrays.equals(playerBanks, state.playerBanks) && Arrays.equals(playerPropertySets, state.playerPropertySets) && Objects.equals(drawPile, state.drawPile) && Objects.equals(discardPile, state.discardPile);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), params, rnd, drawPile, discardPile, actionsLeft, turnStart);
        result = 31 * result + Arrays.hashCode(playerHands);
        result = 31 * result + Arrays.hashCode(playerBanks);
        result = 31 * result + Arrays.hashCode(playerPropertySets);
        return result;
    }
    public enum MonopolyDealGamePhase implements IGamePhase {
        Play,
        Discard
    }
}
