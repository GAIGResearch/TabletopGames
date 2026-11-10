package games.monopoly;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import games.GameType;
import games.monopoly.actions.Auction;
import games.monopoly.actions.RaiseMoney;
import games.monopoly.components.MonopolyCard;
import utilities.DeterminisationUtilities;

import java.util.*;

/**
 * <p>State tracked (the board itself is fixed, in MonopolyParameters.getBoard()):</p>
 * <ul>
 *     <li>cash - each player's money. The Bank's money is unlimited, so it is not tracked</li>
 *     <li>position - the square each player's token is on (indexed by MonopolySquare.index())</li>
 *     <li>owner - the player owning each square, or -1 for the Bank (and for squares that are not properties)</li>
 *     <li>buildings - the houses on each street (0-4), or MonopolyParameters.HOTEL for a hotel</li>
 *     <li>mortgaged - whether each property is mortgaged</li>
 *     <li>inJail - whether each player is in Jail (their token is then on the Jail square, not Just Visiting)</li>
 *     <li>jailRolls - the failed rolls for doubles each player has made during their current stay in Jail</li>
 *     <li>nDoubles - the doubles the current player has rolled in a row this turn</li>
 *     <li>anotherRoll - whether the current player will roll again after MANAGE</li>
 *     <li>dice - the two dice of the last roll</li>
 *     <li>chanceDeck, communityChestDeck - the face-down card piles, top first</li>
 *     <li>jailCards - the Get Out of Jail Free cards each player holds, face up</li>
 *     <li>finalPlace - the place each bankrupt player finished in (nPlayers for the first out), 0 while in play</li>
 * </ul>
 * The step of the turn is the game phase (MonopolyGamePhase). The Bank never runs out of houses or hotels, so its
 * stock is not tracked.
 */
public class MonopolyGameState extends AbstractGameState {

    int[] cash;
    int[] position;
    int[] owner;
    int[] buildings;
    boolean[] mortgaged;
    boolean[] inJail;
    int[] jailRolls;
    int nDoubles;
    boolean anotherRoll;
    int[] dice;
    Deck<MonopolyCard> chanceDeck;
    Deck<MonopolyCard> communityChestDeck;
    List<Deck<MonopolyCard>> jailCards;
    int[] finalPlace;
    // for testing only (setNextRolls)
    private Deque<Integer> nextRolls;

    public MonopolyGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Monopoly;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.add(chanceDeck);
        components.add(communityChestDeck);
        components.addAll(jailCards);
        return components;
    }

    public MonopolyBoard getBoard() {
        return ((MonopolyParameters) gameParameters).getBoard();
    }

    public int getCash(int player) {
        return cash[player];
    }

    public MonopolySquare getPosition(int player) {
        return getBoard().square(position[player]);
    }

    public int getOwner(MonopolySquare square) {
        return owner[square.index()];
    }

    public int getBuildings(MonopolySquare square) {
        return buildings[square.index()];
    }

    public boolean isMortgaged(MonopolySquare square) {
        return mortgaged[square.index()];
    }

    public boolean isInJail(int player) {
        return inJail[player];
    }

    public int getJailRolls(int player) {
        return jailRolls[player];
    }

    public int getNDoubles() {
        return nDoubles;
    }

    public boolean hasAnotherRoll() {
        return anotherRoll;
    }

    public int[] getDice() {
        return dice.clone();
    }

    public Deck<MonopolyCard> getChanceDeck() {
        return chanceDeck;
    }

    public Deck<MonopolyCard> getCommunityChestDeck() {
        return communityChestDeck;
    }

    public Deck<MonopolyCard> getJailCards(int player) {
        return jailCards.get(player);
    }

    public int getFinalPlace(int player) {
        return finalPlace[player];
    }

    public boolean isBankrupt(int player) {
        return finalPlace[player] > 0;
    }

    /**
     * The properties the player owns, in board order.
     */
    public List<MonopolySquare> getProperties(int player) {
        List<MonopolySquare> owned = new ArrayList<>();
        for (MonopolySquare s : getBoard().squares())
            if (owner[s.index()] == player)
                owned.add(s);
        return owned;
    }

    /**
     * The player's total worth, as the rulebook values it for the short game.
     */
    public int getNetWorth(int player) {
        // cash, the printed price of each unmortgaged property and the mortgage value of each mortgaged one, and the
        // cost of the buildings (a hotel counts as MonopolyParameters.HOTEL houses)
        int worth = cash[player];
        for (MonopolySquare s : getProperties(player)) {
            worth += mortgaged[s.index()] ? s.mortgage() : s.price();
            worth += buildings[s.index()] * s.houseCost();
        }
        return worth;
    }

    public void setCash(int player, int amount) {
        cash[player] = amount;
    }

    public void setPosition(int player, MonopolySquare square) {
        position[player] = square.index();
    }

    /**
     * Sets the owner of the property: a player, or -1 for the Bank.
     */
    public void setOwner(MonopolySquare square, int player) {
        owner[square.index()] = player;
    }

    public void setBuildings(MonopolySquare square, int n) {
        buildings[square.index()] = n;
    }

    public void setMortgaged(MonopolySquare square, boolean value) {
        mortgaged[square.index()] = value;
    }

    public void setJailRolls(int player, int n) {
        jailRolls[player] = n;
    }

    /**
     * Frees the player from Jail (they stay on the Jail square, now Just Visiting).
     */
    public void leaveJail(int player) {
        inJail[player] = false;
        jailRolls[player] = 0;
    }

    public void setNDoubles(int n) {
        nDoubles = n;
    }

    public void setAnotherRoll(boolean value) {
        anotherRoll = value;
    }

    /**
     * Rolls the two dice (rollDie), and records them as the last roll (getDice).
     */
    public int[] rollDice() {
        dice = new int[]{rollDie(), rollDie()};
        return dice.clone();
    }

    /**
     * Rolls one die, giving 1-6. A value queued by setNextRolls is used first.
     */
    public int rollDie() {
        if (nextRolls != null && !nextRolls.isEmpty())
            return nextRolls.poll();
        return getRnd().nextInt(6) + 1;
    }

    /**
     * For testing only: the given values will be the next dice rolled, in order (two for each roll). Not copied, and
     * not part of equals.
     */
    public void setNextRolls(int... values) {
        nextRolls = new ArrayDeque<>();
        for (int v : values)
            nextRolls.add(v);
    }

    /**
     * Moves the player's token forward by the number of squares, collecting the GO salary on passing or landing on
     * GO, and then carries out what happens on the square it lands on (SquareType.landOn).
     */
    public void moveForward(int player, int steps) {
        MonopolyBoard board = getBoard();
        int to = position[player] + steps;
        // GO is square 0, so passing or landing on it wraps the position round the board
        if (to >= board.nSquares()) {
            to -= board.nSquares();
            cash[player] += board.goSalary();
        }
        position[player] = to;
        MonopolySquare square = board.square(to);
        square.type().landOn(this, player, square);
    }

    /**
     * Moves the player's token back to the square, without collecting the GO salary, and then carries out what
     * happens on it (SquareType.landOn).
     */
    public void moveBackTo(int player, MonopolySquare square) {
        position[player] = square.index();
        square.type().landOn(this, player, square);
    }

    /**
     * Draws the top card of the pile. It goes to the bottom of the pile at once, except a Get Out of Jail Free card,
     * which the card's effect gives to the player who drew it.
     */
    public MonopolyCard drawCard(MonopolyCard.Pile pile) {
        Deck<MonopolyCard> deck = getPile(pile);
        MonopolyCard card = deck.draw();
        if (card.effect != MonopolyCard.Effect.GET_OUT_OF_JAIL)
            deck.addToBottom(card);
        return card;
    }

    private Deck<MonopolyCard> getPile(MonopolyCard.Pile pile) {
        return pile == MonopolyCard.Pile.CHANCE ? chanceDeck : communityChestDeck;
    }

    /**
     * The player keeps the Get Out of Jail Free card until they use it.
     */
    public void keepJailCard(int player, MonopolyCard card) {
        jailCards.get(player).add(card);
    }

    /**
     * The player gives up the Get Out of Jail Free card they hold: it goes to the bottom of the pile it came from.
     */
    public void returnJailCard(int player, MonopolyCard card) {
        jailCards.get(player).remove(card);
        getPile(card.pile).addToBottom(card);
    }

    /**
     * Sends the player to Jail: their token goes straight to the Jail square, without passing GO, and they are in
     * Jail with no failed rolls yet.
     */
    public void sendToJail(int player) {
        position[player] = getBoard().jail().index();
        inJail[player] = true;
        jailRolls[player] = 0;
    }

    /**
     * The rent the property's owner charges a player landing on it now: 0 if it belongs to the Bank or is
     * mortgaged.
     */
    public int getRent(MonopolySquare square) {
        int holder = owner[square.index()];
        if (holder == -1 || mortgaged[square.index()])
            return 0;
        return switch (square.type()) {
            case STREET -> {
                int n = buildings[square.index()];
                // an unimproved street's rent doubles when its owner holds the whole group, mortgaged streets included
                if (n == 0 && ownsGroup(holder, square.group()))
                    yield 2 * square.rents()[0];
                yield square.rents()[n];
            }
            // a station's or utility's rent depends on how many of that type the owner holds, mortgaged or not
            case STATION -> square.rents()[countOwned(holder, SquareType.STATION) - 1];
            // a multiple of the last roll of the dice
            case UTILITY -> (dice[0] + dice[1]) * square.rents()[countOwned(holder, SquareType.UTILITY) - 1];
            default -> 0;
        };
    }

    /**
     * Whether the player owns every street of the group.
     */
    public boolean ownsGroup(int player, MonopolyGroup group) {
        for (MonopolySquare s : getBoard().streets(group))
            if (owner[s.index()] != player)
                return false;
        return true;
    }

    private int countOwned(int player, SquareType type) {
        int n = 0;
        for (MonopolySquare s : getBoard().squares(type))
            if (owner[s.index()] == player)
                n++;
        return n;
    }

    /**
     * Whether its owner may build on the street now.
     */
    public boolean canBuild(MonopolySquare square) {
        int holder = owner[square.index()];
        int n = buildings[square.index()];
        // the owner holds the whole group, the street has less than a hotel, and the owner has the house cost in cash
        if (square.type() != SquareType.STREET || holder == -1 || !ownsGroup(holder, square.group())
                || n >= MonopolyParameters.HOTEL || cash[holder] < square.houseCost())
            return false;
        // build evenly: no street of the group may have fewer buildings, or be mortgaged
        for (MonopolySquare s : getBoard().streets(square.group()))
            if (mortgaged[s.index()] || buildings[s.index()] < n)
                return false;
        return true;
    }

    /**
     * Whether its owner may sell a building from the street now.
     */
    public boolean canSellBuilding(MonopolySquare square) {
        int n = buildings[square.index()];
        if (n == 0)
            return false;
        // sell evenly: no street of the group may have more buildings
        for (MonopolySquare s : getBoard().streets(square.group()))
            if (buildings[s.index()] > n)
                return false;
        return true;
    }

    /**
     * Whether the property may be mortgaged now.
     */
    public boolean canMortgage(MonopolySquare square) {
        if (mortgaged[square.index()])
            return false;
        // a street may not be mortgaged while any street of its group has buildings
        if (square.type() == SquareType.STREET)
            for (MonopolySquare s : getBoard().streets(square.group()))
                if (buildings[s.index()] > 0)
                    return false;
        return true;
    }

    /**
     * The most cash the player could have by selling all their buildings to the Bank and then mortgaging all their
     * unmortgaged properties.
     */
    public int getRaisableValue(int player) {
        MonopolyParameters params = (MonopolyParameters) gameParameters;
        int value = cash[player];
        for (MonopolySquare s : getProperties(player)) {
            value += buildings[s.index()] * params.buildingSaleValue(s);
            if (!mortgaged[s.index()])
                value += s.mortgage();
        }
        return value;
    }

    /**
     * The player pays the amount to the creditor (another player, or -1 for the Bank). If the player's cash does not
     * cover it, they are bankrupt to the creditor (goBankrupt).
     */
    public void pay(int player, int creditor, int amount) {
        if (cash[player] < amount) {
            // a debt the player could cover by selling and mortgaging. A player never has two such debts at once: a
            // forced Jail fine is raised before the move, and a creditor's interest is one payment
            if (getRaisableValue(player) >= amount)
                setActionInProgress(new RaiseMoney(player, creditor, amount));
            else
                goBankrupt(player, creditor);
            return;
        }
        cash[player] -= amount;
        if (creditor != -1)
            cash[creditor] += amount;
    }

    /**
     * The player is bankrupt, owing the creditor (another player, or -1 for the Bank). They leave the game in the last
     * place not yet taken.
     */
    public void goBankrupt(int player, int creditor) {
        // the buildings are sold to the Bank first
        MonopolyParameters params = (MonopolyParameters) gameParameters;
        for (MonopolySquare s : getProperties(player)) {
            cash[player] += buildings[s.index()] * params.buildingSaleValue(s);
            buildings[s.index()] = 0;
        }
        // a player creditor takes the cash and the properties
        if (creditor != -1)
            cash[creditor] += cash[player];
        cash[player] = 0;
        List<MonopolySquare> properties = getProperties(player);
        for (MonopolySquare s : properties) {
            owner[s.index()] = creditor;
            // the Bank takes its properties back unmortgaged
            if (creditor == -1)
                mortgaged[s.index()] = false;
        }
        // Get Out of Jail Free cards go to a player creditor, or back to their piles
        for (MonopolyCard card : new ArrayList<>(jailCards.get(player).getComponents())) {
            if (creditor == -1)
                returnJailCard(player, card);
            else {
                jailCards.get(player).remove(card);
                keepJailCard(creditor, card);
            }
        }
        finalPlace[player] = getNPlayersIn();
        setPlayerResult(CoreConstants.GameResult.LOSE_GAME, player);
        // a player creditor pays the interest on the mortgaged properties they receive, all at once
        if (creditor != -1) {
            int interest = 0;
            for (MonopolySquare s : properties)
                if (mortgaged[s.index()])
                    interest += params.mortgageInterest(s);
            if (interest > 0)
                pay(creditor, -1, interest);
        }
        // the Bank auctions the properties it takes back, in board order (so the last is pushed first), unless the
        // game is over
        if (creditor == -1 && getNPlayersIn() > 1)
            for (int i = properties.size() - 1; i >= 0; i--)
                setActionInProgress(new Auction(this, properties.get(i), nextPlayerIn(player)));
    }

    /**
     * The next player round the table after the given one who is not bankrupt.
     */
    public int nextPlayerIn(int player) {
        int next = player;
        do {
            next = (next + 1) % getNPlayers();
        } while (isBankrupt(next));
        return next;
    }

    /**
     * The number of players not bankrupt.
     */
    public int getNPlayersIn() {
        int n = 0;
        for (int p = 0; p < getNPlayers(); p++)
            if (!isBankrupt(p))
                n++;
        return n;
    }

    @Override
    protected MonopolyGameState _copy(int playerId) {
        MonopolyGameState copy = new MonopolyGameState(gameParameters, getNPlayers());
        copy.cash = cash.clone();
        copy.position = position.clone();
        copy.owner = owner.clone();
        copy.buildings = buildings.clone();
        copy.mortgaged = mortgaged.clone();
        copy.inJail = inJail.clone();
        copy.jailRolls = jailRolls.clone();
        copy.nDoubles = nDoubles;
        copy.anotherRoll = anotherRoll;
        copy.dice = dice.clone();
        copy.chanceDeck = chanceDeck.copy();
        copy.communityChestDeck = communityChestDeck.copy();
        copy.jailCards = new ArrayList<>();
        for (Deck<MonopolyCard> d : jailCards)
            copy.jailCards.add(d.copy());
        copy.finalPlace = finalPlace.clone();
        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // the only hidden information is the order of the two face-down piles. Cards drawn and put back at the
            // bottom (whose order a player could remember) are reshuffled too - a simplification
            DeterminisationUtilities.reshuffle(playerId, List.of(copy.chanceDeck), c -> true, redeterminisationRnd);
            DeterminisationUtilities.reshuffle(playerId, List.of(copy.communityChestDeck), c -> true,
                    redeterminisationRnd);
        }
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        // win 1, draw 0.5, lose 0
        if (!isNotTerminal())
            return (getPlayerResults()[playerId].value + 1) / 2.0;
        // the player's share of the total worth of all players
        int total = 0;
        for (int p = 0; p < getNPlayers(); p++)
            total += getNetWorth(p);
        return total == 0 ? 0 : (double) getNetWorth(playerId) / total;
    }

    /**
     * The player's total worth (getNetWorth).
     */
    @Override
    public double getGameScore(int playerId) {
        return getNetWorth(playerId);
    }

    @Override
    public int getOrdinalPosition(int playerId) {
        // bankrupt players are last, in the order they went out; the players still in come first, by total worth
        if (isBankrupt(playerId))
            return finalPlace[playerId];
        int worth = getNetWorth(playerId);
        int position = 1;
        for (int p = 0; p < getNPlayers(); p++)
            if (p != playerId && !isBankrupt(p) && getNetWorth(p) > worth)
                position++;
        return position;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MonopolyGameState that)) return false;
        return Arrays.equals(cash, that.cash) &&
                Arrays.equals(position, that.position) &&
                Arrays.equals(owner, that.owner) &&
                Arrays.equals(buildings, that.buildings) &&
                Arrays.equals(mortgaged, that.mortgaged) &&
                Arrays.equals(inJail, that.inJail) &&
                Arrays.equals(jailRolls, that.jailRolls) &&
                nDoubles == that.nDoubles &&
                anotherRoll == that.anotherRoll &&
                Arrays.equals(dice, that.dice) &&
                Objects.equals(chanceDeck, that.chanceDeck) &&
                Objects.equals(communityChestDeck, that.communityChestDeck) &&
                Objects.equals(jailCards, that.jailCards) &&
                Arrays.equals(finalPlace, that.finalPlace);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), nDoubles, anotherRoll, chanceDeck, communityChestDeck, jailCards);
        result = 31 * result + Arrays.hashCode(cash);
        result = 31 * result + Arrays.hashCode(position);
        result = 31 * result + Arrays.hashCode(owner);
        result = 31 * result + Arrays.hashCode(buildings);
        result = 31 * result + Arrays.hashCode(mortgaged);
        result = 31 * result + Arrays.hashCode(inJail);
        result = 31 * result + Arrays.hashCode(jailRolls);
        result = 31 * result + Arrays.hashCode(dice);
        result = 31 * result + Arrays.hashCode(finalPlace);
        return result;
    }
}
