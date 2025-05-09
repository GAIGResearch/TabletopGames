package games.monopolydeal;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.interfaces.IGamePhase;
import games.GameType;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.PropertySet;
import games.monopolydeal.cards.SetType;

import java.util.*;

import static core.CoreConstants.VisibilityMode.*;

/**
 * <p>The game state encapsulates all game information. It is a data-only class, with game functionality present
 * in the Forward Model or actions modifying the state of the game.</p>
 * <p>Most variables held here should be {@link Component} subclasses as much as possible.</p>
 * <p>No initialisation or game logic should be included here (not in the constructor either). This is all handled externally.</p>
 * <p>Computation may be included in functions here for ease of access, but only if this is querying the game state information.
 * Functions on the game state should never <b>change</b> the state of the game.</p>
 */
public class MonopolyDealGameState extends AbstractGameState {

    // GameState members
    // Player Data members
    List<Deck<MonopolyDealCard>> playerHands;
    List<Deck<MonopolyDealCard>> playerBanks;
    List<PropertySet[]> playerPropertySets;

    Deck<MonopolyDealCard> drawPile;
    Deck<MonopolyDealCard> discardPile;

    // Player turn status members
    int actionsLeft;
    int boardModificationsLeft;
    boolean deckEmpty = false;

    /**
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players in the game
     */
    public MonopolyDealGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        rnd = new Random(gameParameters.getRandomSeed());
        playerHands = new ArrayList<>();
        playerBanks = new ArrayList<>();
        playerPropertySets = new ArrayList<>();
        for (int i = 0; i < nPlayers; i++) {
            playerPropertySets.add(new PropertySet[SetType.values().length]);
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
        components.addAll(playerHands);
        components.addAll(playerBanks);
        for (PropertySet[] playerPropertySet : playerPropertySets) {
            components.addAll(List.of(playerPropertySet));
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
            retValue.playerHands.add(playerHands.get(p).copy());
            if (playerId != -1 && getCoreGameParameters().partialObservable && p != playerId) {
                playerHandSize[p] = retValue.playerHands.get(p).getSize();

                // Adding all hidden cards back to deck
                retValue.drawPile.add(retValue.playerHands.get(p));
                retValue.playerHands.get(p).clear();
            }
        }
        if(playerId != -1 && getCoreGameParameters().partialObservable) {
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
            retValue.playerBanks.add(playerBanks.get(i).copy());
            for(int j=0;j<SetType.values().length;j++) {
                retValue.playerPropertySets.get(i)[j] = playerPropertySets.get(i)[j].copy();
            }
        }

        retValue.discardPile = discardPile.copy();
        retValue.actionsLeft = actionsLeft;
        retValue.deckEmpty = deckEmpty;
        retValue.boardModificationsLeft = boardModificationsLeft;

        return retValue;
    }
    public boolean canModifyBoard(int playerID){
        if(boardModificationsLeft>0) {
            for (PropertySet pSet : playerPropertySets.get(playerID)) {
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
                playerHands.get(playerID).add(drawPile.draw());
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
        MonopolyDealParameters params = (MonopolyDealParameters) getGameParameters();
        actionsLeft = params.ACTIONS_PER_TURN;
        boardModificationsLeft = params.BOARD_MODIFICATIONS_PER_TURN;
        int nextPlayer = getCurrentPlayer();
        if(playerHands.get(nextPlayer).getSize() == 0){
            drawCard(nextPlayer,params.DRAWS_WHEN_EMPTY);
        }
        else{
            drawCard(nextPlayer,params.DRAWS_PER_TURN);
        }
    }
    public void discardCard(CardType cardType, int playerID) {
        MonopolyDealCard card = new MonopolyDealCard(cardType);
        playerHands.get(playerID).remove(card);
        discardPile.add(card);
    }
    public void addMoney(int playerID, CardType money){
        MonopolyDealCard card = new MonopolyDealCard(money);
        playerBanks.get(playerID).add(card);
    }
    public void removeMoneyFrom(int playerID, CardType money) {
        MonopolyDealCard card = new MonopolyDealCard(money);
        playerBanks.get(playerID).remove(card);
    }
    public boolean isBoardEmpty(int playerID){
        if (playerBanks.get(playerID).getSize() == 0) {
            for (PropertySet pSet : playerPropertySets.get(playerID)) {
                for (int i=0; i<pSet.getSize(); i++) {
                    if(pSet.get(i).cardType()!= CardType.MulticolorWild) return false;
                }
            }
        } else return playerBanks.get(playerID).getSize() <= 0;
        return true;
    }
    // initialize propertySet
    public void initPropertySets(int playerID){
        for(int i=0;i<SetType.values().length;i++) {
            PropertySet pSet = new PropertySet("Set " + SetType.values()[i].name(), VISIBLE_TO_ALL, SetType.values()[i]);
            playerPropertySets.get(playerID)[i] = pSet;
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
        playerPropertySets.get(playerID)[SType.ordinal()].add(card);
    }
    public void removePropertyFrom(int playerID, CardType cardType, SetType from){
        MonopolyDealCard card = new MonopolyDealCard(cardType);
        card.setUseAs(from);
        playerPropertySets.get(playerID)[from.ordinal()].remove(card);
    }
    public void movePropertySetFromTo(SetType setType, int target, int playerID) {
        PropertySet pSet = playerPropertySets.get(target)[setType.ordinal()].copy();
        for(int i=0; i<pSet.getSize(); i++){
            playerPropertySets.get(target)[setType.ordinal()].remove(pSet.get(i));
            playerPropertySets.get(playerID)[setType.ordinal()].add(pSet.get(i));
        }
    }
    public boolean checkForForcedDeal(int playerID){
        boolean target;
        target = checkForSlyDeal(playerID);
        if(target) {
            for (PropertySet pSet: playerPropertySets.get(playerID)) {
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
        for (PropertySet pSet: playerPropertySets.get(playerID)) {
            if(!pSet.isComplete && pSet.getSize()>0){
                return true;
            }
        }
        return false;
    }
    public boolean checkForMulticolorRent(int playerID){
        for (PropertySet pSet: playerPropertySets.get(playerID)) {
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
        for (PropertySet pSet: playerPropertySets.get(playerID)) {
            if(pSet.isComplete) return true;
        }
        return false;
    }
    public boolean playerHasSet(int playerID, SetType setType){
        return playerPropertySets.get(playerID)[setType.ordinal()].getPropertySetSize() > 0;
    }
    public boolean checkActionCard(int playerID, CardType cardType){
        return switch (cardType) {
            case ForcedDeal -> checkForForcedDeal(playerID);
            case SlyDeal -> checkForSlyDeal(playerID);
            case DealBreaker -> checkForDealBreaker(playerID);
            case MulticolorRent -> checkForMulticolorRent(playerID);
            case GreenBlueRent -> playerHasSet(playerID, SetType.Green) || playerHasSet(playerID, SetType.Blue);
            case BrownLightBlueRent ->
                    playerHasSet(playerID, SetType.Brown) || playerHasSet(playerID, SetType.LightBlue);
            case PinkOrangeRent -> playerHasSet(playerID, SetType.Pink) || playerHasSet(playerID, SetType.Orange);
            case RedYellowRent -> playerHasSet(playerID, SetType.Red) || playerHasSet(playerID, SetType.Yellow);
            case RailRoadUtilityRent ->
                    playerHasSet(playerID, SetType.RailRoad) || playerHasSet(playerID, SetType.Utility);
            case PassGo, DebtCollector, ItsMyBirthday -> true;
            default -> false;
        };
    }
    public boolean checkForActionCards(int playerID) {
        for(int i=0;i<playerHands.get(playerID).getSize();i++){
            if(checkActionCard(playerID,playerHands.get(playerID).get(i).cardType())) return true;
        }
        return false;
    }
    public PropertySet getPlayerPropertySet(int playerID, SetType setType) {
        return playerPropertySets.get(playerID)[setType.ordinal()];
    }
    public void useAction(int actionCost) {
        actionsLeft = actionsLeft-actionCost;
    }
    public void modifyBoard(){ boardModificationsLeft--; }
    public int getActionsLeft(){return actionsLeft;}
    // remove property
    public Deck<MonopolyDealCard> getPlayerHand(int playerID){
        return playerHands.get(playerID);
    }
    public void removeCardFromHand(int playerID, CardType cardType){
        MonopolyDealCard card = new MonopolyDealCard(cardType);
        playerHands.get(playerID).remove(card);
    }
    public PropertySet[] getPropertySets(int playerID) {
        return playerPropertySets.get(playerID);
    }
    public Deck<MonopolyDealCard> getPlayerBank(int playerId) { return playerBanks.get(playerId); }
    public Deck<MonopolyDealCard> getDiscardPile() { return discardPile; }
    public Deck<MonopolyDealCard> getDrawPile(){ return drawPile; }
    public boolean CheckForJustSayNo(int playerID) { return playerHands.get(playerID).getComponents().contains(MonopolyDealCard.create(CardType.JustSayNo)); }

    public int getBankValue(int playerID){
        Deck<MonopolyDealCard> playerBank = getPlayerBank(playerID);
        int bankValue = 0;
        for(int i=0;i<playerBank.getSize();i++)
            bankValue = bankValue + playerBank.get(i).cardMoneyValue();
        return bankValue;
    }

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
//            return heuristic.evaluateState(this,playerId);
            return getGameScore(playerId);
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
        MonopolyDealParameters params = (MonopolyDealParameters) getGameParameters();
        if(deckEmpty) return 0;
        int count = 0;
        for (PropertySet pSet:playerPropertySets.get(playerId)) {
            if(pSet.isComplete){
                count++;
            }
        }
        return count/(params.SETS_TO_WIN*1.0);
    }

    @Override
    public boolean _equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MonopolyDealGameState that = (MonopolyDealGameState) o;
        for (int i = 0; i < nPlayers; i++) {
            if (!Arrays.equals(playerPropertySets.get(i), that.playerPropertySets.get(i))) return false;
        }
        return actionsLeft == that.actionsLeft && boardModificationsLeft == that.boardModificationsLeft &&
                deckEmpty == that.deckEmpty && Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(playerBanks, that.playerBanks) && Objects.equals(drawPile, that.drawPile) &&
                Objects.equals(discardPile, that.discardPile);
    }

    @Override
    public int hashCode() {
        int retValue = Objects.hash(playerHands, playerBanks, drawPile, discardPile, actionsLeft, boardModificationsLeft, deckEmpty);
        for (int i = 0; i < nPlayers; i++) {
            retValue += Arrays.hashCode(playerPropertySets.get(i));
        }
        return retValue;
    }

    public enum MonopolyDealGamePhase implements IGamePhase {
        Play,
        Discard
    }
}
