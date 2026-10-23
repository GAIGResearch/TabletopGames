package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.monopoly.MonopolyGameState;
import games.monopoly.MonopolyParameters;
import games.monopoly.MonopolySquare;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The Bank auctions a property, round the table from firstBidder. Every player not bankrupt may bid, including the
 * player who declined to buy it and players in Jail. In turn each bidder still in either bids (Bid) or passes
 * (PassBid), which takes them out of this auction.
 */
public class Auction implements IExtendedSequence {

    public final MonopolySquare square;
    // the players out of the auction: those who have passed, and bankrupt players
    boolean[] out;
    int highBid;
    // -1 until the first bid
    int highBidder = -1;
    // the player whose turn it is to bid
    int bidder;
    boolean complete;

    public Auction(MonopolyGameState state, MonopolySquare square, int firstBidder) {
        this.square = square;
        out = new boolean[state.getNPlayers()];
        for (int p = 0; p < out.length; p++)
            out[p] = state.isBankrupt(p);
        bidder = firstBidder;
    }

    private Auction(MonopolySquare square) {
        this.square = square;
    }

    public int getHighBid() {
        return highBid;
    }

    public int getHighBidder() {
        return highBidder;
    }

    public boolean isOut(int player) {
        return out[player];
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        MonopolyGameState state = (MonopolyGameState) gs;
        MonopolyParameters params = (MonopolyParameters) state.getGameParameters();
        int cash = state.getCash(bidder);
        List<AbstractAction> actions = new ArrayList<>();
        // the opening bid, or a raise of the high bid by one of the increments, within the bidder's cash
        if (highBidder == -1) {
            if (params.minimumBid <= cash)
                actions.add(new Bid(params.minimumBid));
        } else {
            for (int increment : params.bidIncrements)
                if (highBid + increment <= cash)
                    actions.add(new Bid(highBid + increment));
        }
        actions.add(new PassBid());
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return bidder;
    }

    @Override
    public void _afterAction(AbstractGameState gs, AbstractAction action) {
        MonopolyGameState state = (MonopolyGameState) gs;
        if (action instanceof Bid bid) {
            highBid = bid.amount;
            highBidder = bidder;
        } else if (action instanceof PassBid) {
            out[bidder] = true;
        } else {
            return;
        }
        int nIn = 0;
        for (boolean o : out)
            if (!o) nIn++;
        if (nIn == 0) {
            // everyone passed without a bid: the property stays with the Bank
            complete = true;
        } else if (nIn == 1 && highBidder != -1) {
            // only the high bidder is left in
            complete = true;
            state.pay(highBidder, -1, highBid);
            state.setOwner(square, highBidder);
        } else {
            do {
                bidder = (bidder + 1) % out.length;
            } while (out[bidder]);
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }

    @Override
    public Auction copy() {
        Auction copy = new Auction(square);
        copy.out = out.clone();
        copy.highBid = highBid;
        copy.highBidder = highBidder;
        copy.bidder = bidder;
        copy.complete = complete;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Auction a && a.square.equals(square) && Arrays.equals(a.out, out) &&
                a.highBid == highBid && a.highBidder == highBidder && a.bidder == bidder && a.complete == complete;
    }

    @Override
    public int hashCode() {
        return Objects.hash(square, highBid, highBidder, bidder, complete) + 31 * Arrays.hashCode(out) + 640233;
    }

    @Override
    public String toString() {
        return "Auction of " + square.name();
    }
}
