package games.monopolydeal.cards;

import core.components.Deck;

import java.util.ArrayList;
import java.util.List;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;

public class Board {
    public Deck<MonopolyDealCard> playerBank;
    public List<PropertySet> playerPropertySets;

    public Board(int playerId) {
        playerBank = new Deck<MonopolyDealCard>("Bank of Player:" + playerId, VISIBLE_TO_ALL);
        playerPropertySets = new ArrayList<PropertySet>();
    }
}
