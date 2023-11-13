package games.monopolydeal;

import com.clearspring.analytics.util.Lists;
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

    // GameState members
    // Player Data members
    PartialObservableDeck<MonopolyDealCard>[] playerHands;
    Deck<MonopolyDealCard>[] playerBanks;
    PropertySet[][] playerPropertySets;

    Deck<MonopolyDealCard> drawPile;
    Deck<MonopolyDealCard> discardPile;

    // Player turn status members
    int actionsLeft;
    int boardModificationsLeft;
    boolean deckEmpty = false;

    MonopolyDealHeuristic heuristic;

    /**
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players in the game
     */
    public MonopolyDealGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        rnd = new Random(gameParameters.getRandomSeed());
        params = (MonopolyDealParameters) gameParameters;
        heuristic = new MonopolyDealHeuristic();
        this._reset();
    }

    protected void _reset() {
        playerHands = new PartialObservableDeck[getNPlayers()];
        playerBanks = new Deck[getNPlayers()];
        playerPropertySets = new PropertySet[getNPlayers()][11];
        drawPile = new Deck<>("Draw Pile",HIDDEN_TO_ALL);
        discardPile = new Deck<>("Discard Pile",VISIBLE_TO_ALL);
        for(int i=0;i<getNPlayers();i++){
            boolean[] handVisibility = new boolean[getNPlayers()];
            handVisibility[i] = true;
            playerHands[i] = new PartialObservableDeck<>("Hand of Player " + i + 1, handVisibility);
            playerBanks[i] = new Deck<>("Bank of Player"+i+1,VISIBLE_TO_ALL);
            initPropertySets(i);
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
            components.addAll(Arrays.asList(playerPropertySets[i]));
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
        MonopolyDealGameState retValue = new MonopolyDealGameState(gameParameters.copy(), getNPlayers());

        // Placeholder to know how many cards each player had for redrawing cards
        int[] playerHandSize = new int[getNPlayers()];
        retValue.drawPile = drawPile.copy();
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
        if(playerId != -1) {
            retValue.drawPile.shuffle(rnd);
            // Redrawing hidden cards into hands
            for (int p = 0; p < getNPlayers(); p++) {
                if (p != playerId) {
                    retValue.drawCard(p, playerHandSize[p]);
                }
            }
        }
        // Completely visible values
        for(int i=0;i<getNPlayers();i++){
            retValue.playerBanks[i] = playerBanks[i].copy();
            for(int j=0;j<11;j++)
                retValue.playerPropertySets[i][j] = playerPropertySets[i][j].copy();
        }
//        retValue.playerPropertySets = playerPropertySets.clone();
        retValue.discardPile = discardPile.copy();
        retValue.actionsLeft = actionsLeft;
        retValue.deckEmpty = deckEmpty;
        retValue.boardModificationsLeft = boardModificationsLeft;

        return retValue;
    }
    public boolean canModifyBoard(int playerID){
        if(boardModificationsLeft>0) {
            for (PropertySet pSet : playerPropertySets[playerID]) {
                if (pSet.hasWild) return true;
            }
        }
        return false;
    }
    public void drawCard(int playerID,int drawCount){
        for(int i=0;i<drawCount;i++) {
            if(drawPile.getSize() == 0){
                resetDrawPile();
            }
            if(!deckEmpty)
                playerHands[playerID].add(drawPile.draw());
        }
    }
    public void resetDrawPile(){
        if(discardPile.getSize()==0){
            deckEmpty = true;
            return;
//            throw new AssertionError("Draw pile exhausted");
        }
        drawPile.add(discardPile);
        discardPile.clear();
        drawPile.shuffle(rnd);
    }
    public void endTurn() {
        actionsLeft = params.ACTIONS_PER_TURN;
        boardModificationsLeft = params.BOARD_MODIFICATIONS_PER_TURN;
        int nextPlayer = getCurrentPlayer();
        if(playerHands[nextPlayer].getSize() == 0){
            drawCard(nextPlayer,params.DRAWS_WHEN_EMPTY);
        }
        else{
            drawCard(nextPlayer,params.DRAWS_PER_TURN);
        }
    }
    public void discardCard(CardType cardType, int playerID) {
        MonopolyDealCard card = new MonopolyDealCard(cardType);
        playerHands[playerID].remove(card);
        discardPile.add(card);
    }
    public void addMoney(int playerID, CardType money){
        MonopolyDealCard card = new MonopolyDealCard(money);
        playerBanks[playerID].add(card);
    }
    public void removeMoneyFrom(int playerID, CardType money) {
        MonopolyDealCard card = new MonopolyDealCard(money);
        playerBanks[playerID].remove(card);
    }
    public boolean isBoardEmpty(int playerID){
        if (playerBanks[playerID].getSize() == 0) {
            for (PropertySet pSet : playerPropertySets[playerID]) {
                for (int i=0; i<pSet.getSize(); i++) {
                    if(pSet.get(i).cardType()!= CardType.MulticolorWild) return false;
                }
            }
        } else if (playerBanks[playerID].getSize()>0) {
            return false;
        }
        return true;
    }
    // initialize propertySet
    public void initPropertySets(int playerID){
        for(int i=0;i<11;i++) {
            PropertySet pSet = new PropertySet(getSetType4Init(i).toString(), VISIBLE_TO_ALL, getSetType4Init(i));
            playerPropertySets[playerID][i] = pSet;
        }
    }
    public SetType getSetType4Init(int i){
        switch (i){
            case 0: return SetType.Brown;
            case 1: return SetType.LightBlue;
            case 2: return SetType.Pink;
            case 3: return SetType.Orange;
            case 4: return SetType.Red;
            case 5: return SetType.Yellow;
            case 6: return SetType.Green;
            case 7: return SetType.Blue;
            case 8: return SetType.RailRoad;
            case 9: return SetType.Utility;
            case 10: return SetType.UNDEFINED;
            default: throw new AssertionError("Should not happen");
        }
    }
    public int getSetIndx(SetType setType){
        switch (setType){
            case Brown: return 0;
            case LightBlue: return 1;
            case Pink: return 2;
            case Orange: return 3;
            case Red: return 4;
            case Yellow: return 5;
            case Green: return 6;
            case Blue: return 7;
            case RailRoad: return 8;
            case Utility: return 9;
            case UNDEFINED: return 10;
            default: throw new AssertionError("Should not happen");
        }
    }
    // add property
    public void addProperty(int playerID, CardType cardType){
        SetType SType = cardType.getSetType();
        addPropertyToSet(playerID,cardType,SType);
    }
    public void addPropertyToSet(int playerID, CardType cardType, SetType SType){
        MonopolyDealCard card = new MonopolyDealCard(cardType);
        card.setUseAs(SType);
        int indx = getSetIndx(SType);
        playerPropertySets[playerID][indx].add(card);
    }
    public void removePropertyFrom(int playerID, CardType cardType, SetType from){
        MonopolyDealCard card = new MonopolyDealCard(cardType);
        int indx = getSetIndx(from);
        playerPropertySets[playerID][indx].remove(card);
    }
    public void movePropertySetFromTo(SetType setType, int target, int playerID) {
        int indx = getSetIndx(setType);
        PropertySet pSet = playerPropertySets[target][indx].copy();
        for(int i=0; i<pSet.getSize(); i++){
            playerPropertySets[target][indx].remove(pSet.get(i));
            playerPropertySets[playerID][indx].add(pSet.get(i));
        }
    }
    public boolean checkForForcedDeal(int playerID){
        boolean target;
        target = checkForSlyDeal(playerID);
        if(target) {
            for (PropertySet pSet: playerPropertySets[playerID]) {
                if((!pSet.hasHouse && !pSet.hasHotel) && pSet.getSize() > 0){
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
            if(!pSet.isComplete && pSet.getSize()>0){
                return true;
            }
        }
        return false;
    }
    public boolean checkForMulticolorRent(int playerID){
        for (PropertySet pSet: playerPropertySets[playerID]) {
            if(pSet.getSetType() != SetType.UNDEFINED && pSet.getPropertySetSize()>0){
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
    public boolean playerHasSet(int playerID, SetType setType){
        if(playerPropertySets[playerID][getSetIndx(setType)].getPropertySetSize() > 0) return true;
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
            case GreenBlueRent:
                return playerHasSet(playerID, SetType.Green) || playerHasSet(playerID, SetType.Blue);
            case BrownLightBlueRent:
                return playerHasSet(playerID, SetType.Brown) || playerHasSet(playerID, SetType.LightBlue);
            case PinkOrangeRent:
                return playerHasSet(playerID, SetType.Pink) || playerHasSet(playerID, SetType.Orange);
            case RedYellowRent:
                return playerHasSet(playerID, SetType.Red) || playerHasSet(playerID, SetType.Yellow);
            case RailRoadUtilityRent:
                return playerHasSet(playerID, SetType.RailRoad) || playerHasSet(playerID, SetType.Utility);
            case PassGo:
            case DebtCollector:
            case ItsMyBirthday:
                return true;
            case JustSayNo:
            case DoubleTheRent:
            case House:
            case Hotel:
            default:
                return false;
        }
    }
    public boolean checkForActionCards(int playerID) {
        for(int i=0;i<playerHands[playerID].getSize();i++){
            if(checkActionCard(playerID,playerHands[playerID].get(i).cardType())) return true;
        }
        return false;
    }
    public PropertySet getPlayerPropertySet(int playerID, SetType setType) {
        return playerPropertySets[playerID][getSetIndx(setType)];
    }
    public void useAction(int actionCost) {
        actionsLeft = actionsLeft-actionCost;
    }
    public void modifyBoard(){ boardModificationsLeft--; }
    public int getActionsLeft(){return actionsLeft;}
    // remove property
    public Deck<MonopolyDealCard> getPlayerHand(int playerID){
        return playerHands[playerID];
    }
    public void removeCardFromHand(int playerID, CardType cardType){
        MonopolyDealCard card = new MonopolyDealCard(cardType);
        playerHands[playerID].remove(card);
    }
    public PropertySet[] getPropertySets(int playerID) {
        return playerPropertySets[playerID];
    }
    public Deck<MonopolyDealCard> getPlayerBank(int playerId) { return playerBanks[playerId]; }
    public Deck<MonopolyDealCard> getDiscardPile() { return discardPile; }
    public Deck<MonopolyDealCard> getDrawPile(){ return drawPile; }
    public boolean CheckForJustSayNo(int playerID) { return playerHands[playerID].getComponents().contains(MonopolyDealCard.create(CardType.JustSayNo)); }

    public boolean checkForGameEnd() {
        if(deckEmpty) return true;
        for(int i=0;i<getNPlayers();i++){
            if(getGameScore(i)==1.0d) return true;
        }
        return false;
    }
    /**
     * @param playerId - player observing the state.
     * @return a score for the given player approximating how well they are doing (e.g. how close they are to winning
     * the game); a value between 0 and 1 is preferred, where 0 means the game was lost, and 1 means the game was won.
     */
    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal()) {
            return heuristic.evaluateState(this,playerId);
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
        if(deckEmpty) return 0;
        int count = 0;
        for (PropertySet pSet:playerPropertySets[playerId]) {
            if(pSet.isComplete){
                count++;
            }
        }
        return count/(params.SETS_TO_WIN*1.0);
    }
    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MonopolyDealGameState state = (MonopolyDealGameState) o;
        return actionsLeft == state.actionsLeft && boardModificationsLeft == state.boardModificationsLeft && deckEmpty == state.deckEmpty && Arrays.equals(playerHands, state.playerHands) && Arrays.equals(playerBanks, state.playerBanks) && Arrays.equals(playerPropertySets, state.playerPropertySets) && Objects.equals(drawPile, state.drawPile) && Objects.equals(discardPile, state.discardPile);
    }
    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), drawPile, discardPile, actionsLeft, boardModificationsLeft, deckEmpty);
        result = 31 * result + Arrays.hashCode(playerHands);
        result = 31 * result + Arrays.hashCode(playerBanks);
        for(int i=0;i<getNPlayers();i++)
            result = 31 * result + Arrays.hashCode(playerPropertySets[i]);
        return result;
    }

    public enum MonopolyDealGamePhase implements IGamePhase {
        Play,
        Discard
    }
}
