package games.loyalist;

import static core.CoreConstants.GameResult.*;

import static games.loyalist.LoyalistGameState.Phase.*;
import static games.loyalist.LoyalistGameState.Role.*;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;

import games.loyalist.actions.LoyalistAction;
import games.loyalist.actions.LoyalistAction.Kind;

import java.util.*;

/**
 * Native five-player rules for experimental TAG play. Structured speech is an explicit AI
 * abstraction.
 */
public class LoyalistForwardModel extends StandardForwardModel {
    @Override
    protected void _setup(AbstractGameState state) {
        LoyalistGameState s = (LoyalistGameState) state;
        // TAG's generic action history contains private action parameters and must not enter
        // observations.
        s.getCoreGameParameters().competitionMode = true;
        s.roles = new LoyalistGameState.Role[] {KING, LOYALIST, MAGNATE, MAGNATE, SPY};
        List<LoyalistGameState.Role> hidden =
                new ArrayList<>(List.of(LOYALIST, MAGNATE, MAGNATE, SPY));
        Collections.shuffle(hidden, s.getRnd());
        for (int p = 1; p < 5; p++) s.roles[p] = hidden.get(p - 1);
        s.treasury = new int[] {12, 6, 3};
        s.offices = new int[] {-1, -1, -1};
        s.wealth = new int[] {0, 1, 1, 1, 1};
        s.exileThroughRound = new int[5];
        s.hands = LoyalistGameState.fiveLists();
        s.deck = LoyalistData.powerCards();
        Collections.shuffle(s.deck, s.getRnd());
        s.dead = new ArrayList<>();
        s.bagCards = new ArrayList<>();
        for (int p = 1; p < 5; p++) s.hands.get(p).addAll(LoyalistGameState.take(s.deck, 2));
        s.eventDecks = new ArrayList<>();
        s.eventDiscards = new ArrayList<>();
        for (int stage = 0; stage < 3; stage++) {
            List<Integer> events = new ArrayList<>();
            int lo = stage == 0 ? 1 : stage == 1 ? 6 : 11,
                    hi = stage == 0 ? 5 : stage == 1 ? 10 : 17;
            for (int i = lo; i <= hi; i++) events.add(i);
            Collections.shuffle(events, s.getRnd());
            s.eventDecks.add(events);
            s.eventDiscards.add(new ArrayList<>());
        }
        s.eventOptions = new ArrayList<>();
        s.kingKnownEventBottom =
                new ArrayList<>(List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        s.reforms = new boolean[12];
        s.removedReforms = new boolean[12];
        s.triggered = new boolean[11];
        s.bought = new boolean[5];
        s.acquired = new boolean[5];
        s.acted = new boolean[5];
        s.round = 0;
        s.prosperity = 0;
        s.crisis = 0;
        s.reserve = 0;
        s.bagTokens = 0;
        s.eventId = 0;
        s.proposedReform = 0;
        s.stage = 0;
        s.priorProsperity = 0;
        s.mSupply = 0;
        s.militarySupplier = -1;
        s.embezzled = 0;
        s.inspector = -1;
        s.officeIndex = 0;
        s.supplyResource = 0;
        s.firstSupplier = -1;
        s.queueIndex = 0;
        s.queue = new ArrayList<>();
        s.exileTarget = -1;
        s.cardsToConfiscate = 0;
        s.reassignmentResource = 0;
        s.reportResource = 0;
        s.pendingTokenChange = 0;
        s.lastTotal = 0;
        s.lastMilitaryBonus = 0;
        s.roundSuccess = false;
        s.publicTokenUpperBound = 25;
        s.publicTokenLowerBound = 25;
        s.keepSelected = new ArrayList<>();
        s.publicLog = new ArrayList<>();
        s.privateLog = LoyalistGameState.fiveStringLists();
        s.knownBagCards = LoyalistGameState.fiveLists();
        s.knownDead = LoyalistGameState.fiveLists();
        s.knownTreasury = new int[5][3];
        s.knownWealth = new int[5][5];
        s.knownHandSum = new int[5][5];
        s.rememberedBagTokens = new int[5];
        s.rememberedBagCount = new int[5];
        s.liveBagTokens = new boolean[5];
        s.liveBagCards = new boolean[5];
        for (int p = 0; p < 5; p++) {
            // The starting allocation is printed in the rules and is common knowledge.
            s.knownTreasury[p] = s.treasury.clone();
            s.knownWealth[p] = s.wealth.clone();
            Arrays.fill(s.knownHandSum[p], Integer.MIN_VALUE);
            s.rememberedBagTokens[p] = -1;
            s.rememberedBagCount[p] = -1;
            if (p > 0) s.remember(p, "Role=" + s.roles[p] + "; hand=" + s.hands.get(p));
        }
        Arrays.fill(s.theftOpeningBag, -1);
        Arrays.fill(s.theftOpeningAllowance, -1);
        Arrays.fill(s.theftTaken, 0);
        s.transition(ASSIGN_OFFICES, 0);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        LoyalistGameState s = (LoyalistGameState) state;
        List<AbstractAction> a = new ArrayList<>();
        int p = s.getCurrentPlayer();
        switch (s.phase) {
            case ASSIGN_OFFICES -> {
                for (int x = 1; x < 5; x++) if (!contains(s.offices, x)) add(a, Kind.ASSIGN, x);
            }
            case ROUND_LEVY -> {
                add(a, Kind.PASS);
                for (int x : s.active()) add(a, Kind.LEVY, x);
            }
            case ROUND_INFORMANT -> {
                for (int x : s.active()) add(a, Kind.INFORMANT, x);
            }
            case EVENT_CHOICE -> {
                for (int id : s.eventOptions) add(a, Kind.CHOOSE, id);
            }
            case REFORM_PROPOSAL -> {
                add(a, Kind.PASS);
                for (int id = 1; id <= 11; id++)
                    if (!s.reforms[id] && !s.removedReforms[id]) add(a, Kind.CHOOSE, id);
            }
            case PURCHASE -> {
                add(a, Kind.PASS);
                if (canBuy(s, p)) add(a, Kind.BUY);
            }
            case KEEP_CARDS -> {
                for (int card : new TreeSet<>(s.hands.get(p))) add(a, Kind.KEEP, card);
            }
            case DISASTER_CHOICE -> {
                int max = s.active().stream().mapToInt(x -> s.hands.get(x).size()).max().orElse(0);
                for (int x : s.active()) if (s.hands.get(x).size() == max) add(a, Kind.CHOOSE, x);
            }
            case SUPPLY -> {
                int r = s.supplyResource;
                if (normalSupply(s, r))
                    for (int n = 0; n <= s.treasury[r]; n++) add(a, Kind.SUPPLY, n);
                if (r == 0 && s.reforms[11] && event(s).allows(2))
                    for (int pairs = 0; pairs <= s.treasury[0] / 2; pairs++)
                        add(a, Kind.CONVERT_SUPPLY, pairs);
            }
            case SUPPLY_REPORT, TREASURY_REPORT -> {
                for (int n = 0;
                        n <= ((LoyalistParameters) s.getGameParameters()).maximumReport;
                        n++) add(a, Kind.REPORT, n);
            }
            case AUDIT -> {
                for (int r = 0; r < 3; r++) add(a, Kind.AUDIT, r);
            }
            case FIRST_NOMINATION -> {
                for (int x : s.active()) if (x != p) add(a, Kind.NOMINATE, x);
            }
            case INTRIGUE_STEAL -> {
                int max =
                        Math.min(
                                s.bagTokens,
                                Math.min(
                                        s.reforms[6] ? 2 : 3,
                                        (s.reforms[6] ? 3 : 4) - s.embezzled));
                for (int n = 0; n <= max; n++) add(a, Kind.STEAL, n);
                if (s.inspector == p) for (int x : s.active()) if (x != p) add(a, Kind.INSPECT, x);
            }
            case INTRIGUE_CONTRIBUTE -> {
                for (int n = 0; n <= s.wealth[p]; n++) add(a, Kind.CONTRIBUTE, n);
            }
            case INTRIGUE_CARDS -> {
                add(a, Kind.FINISH_CARDS);
                for (int card : new TreeSet<>(s.hands.get(p))) add(a, Kind.PLAY_CARD, card);
            }
            case TESTIMONY -> {
                for (int claim = 0; claim < 8; claim++) add(a, Kind.TESTIFY, claim);
            }
            case NEXT_NOMINATION -> {
                for (int x : s.active()) if (!s.acted[x]) add(a, Kind.NOMINATE, x);
            }
            case REWARD -> {
                add(a, Kind.PASS);
                for (int x : s.active()) {
                    if (s.bagTokens > 0) add(a, Kind.REWARD_TOKEN, x);
                    if (s.remainingCards() > 0) add(a, Kind.REWARD_CARDS, x);
                }
            }
            case RETURN_RESOURCES -> {
                if (event(s).destroyed() || event(s).halfDestroyed()) add(a, Kind.RETURN, 2);
                else if (event(s).destination() >= 0) add(a, Kind.RETURN, event(s).destination());
                else for (int r = 0; r < 3; r++) add(a, Kind.RETURN, r);
            }
            case EXILE, FINAL_PURGE -> {
                add(a, Kind.PASS);
                for (int x : s.active()) add(a, Kind.EXILE, x);
            }
            case EXILE_CARDS -> {
                for (int i = 0; i < s.hands.get(s.exileTarget).size(); i++)
                    add(a, Kind.DISCARD_BLIND, i);
            }
            case REASSIGN_OFFICE -> {
                add(a, Kind.PASS);
                for (int x : s.active()) add(a, Kind.ASSIGN, x);
            }
            case TREASURY_PLAN -> {
                add(a, Kind.PASS);
                add(a, Kind.CHOOSE, 1);
            }
            case CONVERT_W_M, CONVERT_P_W -> {
                // A royal order is capped only by a publicly derivable maximum, never the hidden
                // stock.
                for (int n = 0; n <= s.publicTokenUpperBound / 2; n++) add(a, Kind.CONVERT, n);
            }
            case EXCHANGE -> {
                add(a, Kind.PASS);
                if (canBuy(s, p)) add(a, Kind.BUY);
                if (!s.acquired[p] && s.reserve > 0 && s.hands.get(p).size() >= s.sellRate())
                    add(a, Kind.SELL);
            }
            case APPOINT_INSPECTOR -> {
                for (int x : s.active()) add(a, Kind.APPOINT, x);
            }
            case FINISHED -> {}
            default -> throw new IllegalStateException("Unresolved automatic phase: " + s.phase);
        }
        if (a.isEmpty() && s.isNotTerminal())
            throw new IllegalStateException("No legal actions at " + s);
        return a;
    }

    static void add(List<AbstractAction> list, LoyalistAction.Kind kind, int value) {
        list.add(new LoyalistAction(kind, value));
    }

    static void add(List<AbstractAction> list, LoyalistAction.Kind kind) {
        add(list, kind, 0);
    }

    static boolean contains(int[] values, int x) {
        for (int y : values) if (x == y) return true;
        return false;
    }

    static boolean canBuy(LoyalistGameState s, int p) {
        return !s.bought[p] && s.wealth[p] > 0 && s.remainingCards() > 0;
    }

    static LoyalistData.Event event(LoyalistGameState s) {
        return LoyalistData.EVENTS[s.eventId];
    }

    static boolean normalSupply(LoyalistGameState s, int r) {
        return event(s).allows(r) || (r == 1 && s.reforms[3]);
    }

    public static void apply(LoyalistGameState s, LoyalistAction action) {
        int p = s.getCurrentPlayer(), x = action.value;
        switch (s.phase) {
            case ASSIGN_OFFICES -> {
                s.offices[s.officeIndex] = x;
                s.announce("Office " + s.officeIndex + " assigned to P" + x);
                s.officeIndex++;
                s.refreshOwners();
                if (s.officeIndex == 3) startRound(s);
            }
            case ROUND_LEVY -> {
                if (action.kind == Kind.LEVY) {
                    s.transferWealthToTreasury(x, 1, false);
                    s.announce("Forced levy ordered for P" + x);
                }
                s.transition(s.reforms[8] ? ROUND_INFORMANT : EVENT_DRAW, 0);
            }
            case ROUND_INFORMANT -> {
                int total = s.hands.get(x).stream().mapToInt(Integer::intValue).sum();
                s.knownHandSum[0][x] = total;
                s.remember(0, "Informant: P" + x + " hand sum=" + total);
                s.transition(EVENT_DRAW, 0);
            }
            case EVENT_CHOICE -> {
                s.eventId = x;
                for (int id : s.eventOptions)
                    if (id != x) {
                        s.eventDecks.get(s.stage).add(id);
                        s.kingKnownEventBottom.get(s.stage).add(id);
                    }
                s.eventOptions.clear();
                s.eventDiscards.get(s.stage).add(x);
                publishEvent(s);
            }
            case REFORM_PROPOSAL -> {
                s.triggered[s.prosperity] = true;
                if (action.kind == Kind.CHOOSE) {
                    s.proposedReform = x;
                    s.announce("Reform proposed: " + x);
                }
                s.transition(PURCHASE_PREPARE, 0);
            }
            case PURCHASE -> {
                if (action.kind == Kind.BUY) buy(s, p);
                s.queueIndex++;
                if (s.queueIndex < s.queue.size())
                    s.transition(PURCHASE, s.queue.get(s.queueIndex));
                else enforceCaps(s, DISASTER, 0);
            }
            case KEEP_CARDS -> {
                if (!s.hands.get(p).remove(Integer.valueOf(x)))
                    throw new IllegalArgumentException("Card absent");
                s.keepSelected.add(x);
                if (s.keepSelected.size() == 2) {
                    int n = s.hands.get(p).size();
                    for (int card : s.hands.get(p)) s.discardCard(card, p);
                    s.hands.set(p, new ArrayList<>(s.keepSelected));
                    s.keepSelected.clear();
                    s.handChanged(p);
                    s.announce("P" + p + " discarded " + n + " cards under centralisation");
                    enforceCaps(s, s.resumePhase, s.resumePlayer);
                }
            }
            case DISASTER_CHOICE -> {
                disasterFromHand(s, x);
                s.transition(SUPPLY_PREPARE, 0);
            }
            case SUPPLY -> {
                int r = s.supplyResource, spent = action.kind == Kind.CONVERT_SUPPLY ? 2 * x : x;
                s.treasury[r] -= spent;
                s.bagTokens += x;
                if (action.kind == Kind.CONVERT_SUPPLY) s.reserve += x;
                if (r == 2) {
                    s.mSupply += x;
                    s.militarySupplier = p;
                }
                s.treasuryChanged(r);
                s.peekBag(p, false);
                s.remember(p, "Supplied: resource=" + r + ", spent=" + spent + ", bag added=" + x);
                s.transition(SUPPLY_REPORT, p);
            }
            case SUPPLY_REPORT -> {
                s.announce("P" + p + " supply report for " + s.supplyResource + ": " + x);
                s.closeBag();
                s.supplyResource++;
                nextSupply(s);
            }
            case AUDIT -> {
                s.knownTreasury[0][x] = s.treasury[x];
                s.remember(0, "Treasury audit: " + x + "=" + s.treasury[x]);
                s.transition(INTRIGUE_PREPARE, 0);
            }
            case FIRST_NOMINATION, NEXT_NOMINATION -> {
                s.announce("P" + p + " nominated P" + x);
                beginIntrigue(s, x);
            }
            case INTRIGUE_STEAL -> {
                // Merely having an opportunity to alter wealth invalidates another player's old
                // observation.
                for (int q = 0; q < 5; q++) if (q != p) s.knownWealth[q][p] = -1;
                if (action.kind == Kind.INSPECT) {
                    s.knownWealth[p][x] = s.wealth[x];
                    s.knownWealth[0][x] = s.wealth[x];
                    s.remember(p, "Inspected P" + x + " wealth=" + s.wealth[x]);
                    s.remember(0, "Inspector found P" + x + " wealth=" + s.wealth[x]);
                    s.transition(TESTIMONY, p);
                } else {
                    s.bagTokens -= x;
                    s.wealth[p] += x;
                    s.embezzled += x;
                    s.theftTaken[p] = x;
                    s.wealthChanged(p);
                    s.peekBag(p, true);
                    s.remember(p, "Embezzled " + x);
                    s.transition(INTRIGUE_CONTRIBUTE, p);
                }
            }
            case INTRIGUE_CONTRIBUTE -> {
                s.wealth[p] -= x;
                s.bagTokens += x;
                s.wealthChanged(p);
                s.peekBag(p, true);
                s.remember(p, "Contributed private wealth " + x);
                s.transition(INTRIGUE_CARDS, p);
            }
            case INTRIGUE_CARDS -> {
                if (action.kind == Kind.PLAY_CARD) {
                    if (!s.hands.get(p).remove(Integer.valueOf(x)))
                        throw new IllegalArgumentException("Card absent");
                    s.bagCards.add(x);
                    s.handChanged(p);
                    s.peekBag(p, true);
                    s.remember(p, "Played card " + x);
                } else s.transition(TESTIMONY, p);
            }
            case TESTIMONY -> {
                String[] claims = {
                    "I helped the state",
                    "I did not embezzle",
                    "The bag is sufficient",
                    "The bag is insufficient",
                    "I suspect P1",
                    "I suspect P2",
                    "I suspect P3",
                    "I suspect P4"
                };
                s.announce("P" + p + " claims: " + claims[x]);
                s.acted[p] = true;
                s.closeBag();
                if (s.active().stream().allMatch(q -> s.acted[q])) s.transition(SETTLEMENT, 0);
                else s.transition(NEXT_NOMINATION, p);
            }
            case REWARD -> {
                if (action.kind == Kind.REWARD_TOKEN) {
                    s.bagTokens--;
                    s.publicWealthDelta(x, 1);
                    s.announce("P" + x + " rewarded 1 token");
                }
                if (action.kind == Kind.REWARD_CARDS) {
                    int n = draw(s, x, 2);
                    if (n > 0) s.acquired[x] = true;
                    s.announce("P" + x + " rewarded " + n + " cards");
                }
                s.peekBag(0, true);
                s.transition(RETURN_RESOURCES, 0);
            }
            case RETURN_RESOURCES -> {
                returnResources(s, x);
                enforceCaps(s, AFTER_SETTLEMENT, 0);
            }
            case EXILE -> {
                if (action.kind == Kind.PASS) beginTreasuryReports(s);
                else exile(s, x);
            }
            case EXILE_CARDS -> {
                int card = s.hands.get(s.exileTarget).remove(x);
                s.discardCard(card, s.exileTarget);
                s.handChanged(s.exileTarget);
                s.cardsToConfiscate--;
                if (s.cardsToConfiscate == 0) beginReassignment(s);
            }
            case REASSIGN_OFFICE -> {
                int r = s.reassignmentResource;
                s.offices[r] = action.kind == Kind.ASSIGN ? x : -1;
                s.announce("Office " + r + " assigned to " + s.offices[r]);
                s.refreshOwners();
                s.reassignmentResource++;
                nextReassignment(s);
            }
            case TREASURY_PLAN -> {
                if (action.kind == Kind.CHOOSE) {
                    s.reportResource = 0;
                    nextTreasuryReport(s);
                } else s.transition(CONVERT_W_M, 0);
            }
            case TREASURY_REPORT -> {
                s.announce("P" + p + " reports treasury " + s.reportResource + " contains " + x);
                s.reportResource++;
                nextTreasuryReport(s);
            }
            case CONVERT_W_M -> {
                convert(s, 1, 2, x);
                s.transition(CONVERT_P_W, 0);
            }
            case CONVERT_P_W -> {
                convert(s, 0, 1, x);
                s.transition(EXCHANGE_PREPARE, 0);
            }
            case EXCHANGE -> {
                if (action.kind == Kind.BUY) buy(s, p);
                if (action.kind == Kind.SELL) {
                    int n = s.sellRate();
                    for (int i = 0; i < n; i++) {
                        int card = Collections.min(s.hands.get(p));
                        s.hands.get(p).remove(Integer.valueOf(card));
                        s.discardCard(card, p);
                    }
                    s.publicWealthDelta(p, 1);
                    s.reserve--;
                    s.handChanged(p);
                    s.announce("P" + p + " sold " + n + " cards");
                }
                s.queueIndex++;
                if (s.queueIndex < s.queue.size())
                    s.transition(EXCHANGE, s.queue.get(s.queueIndex));
                else enforceCaps(s, SALARY, 0);
            }
            case APPOINT_INSPECTOR -> {
                s.inspector = x;
                s.announce("P" + x + " appointed royal inspector");
                s.transition(ROUND_END, 0);
            }
            case FINAL_PURGE -> {
                if (action.kind == Kind.EXILE) {
                    s.exileThroughRound[x] = s.round + 1;
                    s.announce("Final purge: P" + x + " was " + s.roles[x]);
                    if (s.roles[x] == LOYALIST) s.crisis++;
                }
                finish(s, s.crisis >= 4);
            }
            default -> throw new IllegalStateException("Action in automatic phase " + s.phase);
        }
        s.refreshOwners();
        advance(s);
    }

    private static void startRound(LoyalistGameState s) {
        s.round++;
        s.roundBegan();
        s.proposedReform = 0;
        s.mSupply = 0;
        s.militarySupplier = -1;
        s.embezzled = 0;
        s.bagTokens = 0;
        s.bagCards.clear();
        s.eventId = 0;
        Arrays.fill(s.bought, false);
        Arrays.fill(s.acquired, false);
        Arrays.fill(s.acted, false);
        s.closeBag();
        s.knownBagCards = LoyalistGameState.fiveLists();
        if (s.reforms[1]) {
            s.publicTreasuryDelta(1, 1);
            s.publicTokenUpperBound++;
            s.publicTokenLowerBound++;
        }
        s.announce("Round begins");
        s.transition(s.reforms[10] ? ROUND_LEVY : s.reforms[8] ? ROUND_INFORMANT : EVENT_DRAW, 0);
    }

    private static void advance(LoyalistGameState s) {
        for (int guard = 0; guard < 100; guard++) {
            switch (s.phase) {
                case EVENT_DRAW -> {
                    if (s.reforms[4]) {
                        s.eventOptions.add(drawEvent(s));
                        s.eventOptions.add(drawEvent(s));
                        s.transition(EVENT_CHOICE, 0);
                    } else {
                        s.eventId = drawEvent(s);
                        s.eventDiscards.get(s.stage).add(s.eventId);
                        publishEvent(s);
                    }
                }
                case PURCHASE_PREPARE -> {
                    s.queue = new ArrayList<>();
                    for (int r : new int[] {2, 1, 0}) {
                        int p = s.offices[r];
                        if (p > 0 && !s.isExiled(p) && !s.queue.contains(p)) s.queue.add(p);
                    }
                    for (int p : s.active()) if (!s.queue.contains(p)) s.queue.add(p);
                    s.queueIndex = 0;
                    s.transition(PURCHASE, s.queue.get(0));
                }
                case DISASTER -> {
                    int card = drawCard(s);
                    if (card != Integer.MIN_VALUE) {
                        s.bagCards.add(card);
                        s.transition(SUPPLY_PREPARE, 0);
                    } else {
                        int max =
                                s.active().stream()
                                        .mapToInt(p -> s.hands.get(p).size())
                                        .max()
                                        .orElse(0);
                        List<Integer> tied =
                                s.active().stream()
                                        .filter(p -> s.hands.get(p).size() == max)
                                        .toList();
                        if (max == 0) s.transition(SUPPLY_PREPARE, 0);
                        else if (tied.size() == 1) {
                            disasterFromHand(s, tied.get(0));
                            s.transition(SUPPLY_PREPARE, 0);
                        } else s.transition(DISASTER_CHOICE, 0);
                    }
                }
                case SUPPLY_PREPARE -> {
                    s.supplyResource = 0;
                    s.firstSupplier = -1;
                    nextSupply(s);
                }
                case INTRIGUE_PREPARE -> {
                    s.embezzled = 0;
                    int first = s.firstSupplier >= 1 ? s.firstSupplier : s.active().get(0);
                    if (s.active().size() == 1) beginIntrigue(s, first);
                    else s.transition(FIRST_NOMINATION, first);
                }
                case SETTLEMENT -> settle(s);
                case AFTER_SETTLEMENT -> {
                    if (s.prosperity >= 10) s.transition(FINAL_PURGE, 0);
                    else beginTreasuryReports(s);
                }
                case EXCHANGE_PREPARE -> {
                    s.queue = s.active();
                    s.queueIndex = 0;
                    s.transition(EXCHANGE, s.queue.get(0));
                }
                case SALARY -> {
                    for (int p : s.active()) {
                        int n = draw(s, p, 1);
                        s.announce("P" + p + " salary: " + n + " cards");
                    }
                    enforceCaps(s, s.reforms[7] ? APPOINT_INSPECTOR : ROUND_END, 0);
                }
                case ROUND_END -> {
                    for (int p = 1; p < 5; p++)
                        if (s.exileThroughRound[p] == s.round) {
                            s.exileThroughRound[p] = 0;
                            s.announce("P" + p + " returned from exile without office");
                        }
                    s.priorProsperity = s.prosperity;
                    startRound(s);
                }
                default -> {
                    return;
                }
            }
        }
        throw new IllegalStateException("Automatic phase cycle");
    }

    static int drawEvent(LoyalistGameState s) {
        List<Integer> deck = s.eventDecks.get(s.stage);
        if (deck.isEmpty()) {
            deck.addAll(s.eventDiscards.get(s.stage));
            s.eventDiscards.get(s.stage).clear();
            s.kingKnownEventBottom.get(s.stage).clear();
            Collections.shuffle(deck, s.getRnd());
        }
        if (deck.isEmpty()) throw new IllegalStateException("No event available");
        int card = deck.remove(0);
        List<Integer> knownBottom = s.kingKnownEventBottom.get(s.stage);
        if (!knownBottom.isEmpty() && knownBottom.get(0) == card) knownBottom.remove(0);
        return card;
    }

    private static void publishEvent(LoyalistGameState s) {
        s.announce("Event " + s.eventId + " revealed");
        boolean trigger =
                (s.prosperity == 2 || s.prosperity == 5 || s.prosperity == 8)
                        && !s.triggered[s.prosperity];
        s.transition(trigger ? REFORM_PROPOSAL : PURCHASE_PREPARE, 0);
    }

    static int drawCard(LoyalistGameState s) {
        if (s.deck.isEmpty() && !s.dead.isEmpty()) {
            s.deck.addAll(s.dead);
            s.dead.clear();
            s.knownDead = LoyalistGameState.fiveLists();
            Collections.shuffle(s.deck, s.getRnd());
        }
        return s.deck.isEmpty() ? Integer.MIN_VALUE : s.deck.remove(0);
    }

    static int draw(LoyalistGameState s, int p, int n) {
        int received = 0;
        for (int i = 0; i < n; i++) {
            int card = drawCard(s);
            if (card == Integer.MIN_VALUE) break;
            s.hands.get(p).add(card);
            s.remember(p, "Drew card " + card);
            received++;
        }
        if (received > 0) s.handChanged(p);
        return received;
    }

    static void buy(LoyalistGameState s, int p) {
        int n = draw(s, p, s.buyRate());
        s.publicWealthDelta(p, -1);
        s.reserve++;
        s.bought[p] = true;
        s.acquired[p] = n > 0;
        s.announce("P" + p + " purchased " + n + " cards");
    }

    private static void enforceCaps(
            LoyalistGameState s, LoyalistGameState.Phase resume, int actor) {
        if (s.reforms[6])
            for (int p : s.active())
                if (s.hands.get(p).size() > 2) {
                    s.resumePhase = resume;
                    s.resumePlayer = actor;
                    s.keepSelected.clear();
                    s.transition(KEEP_CARDS, p);
                    return;
                }
        s.transition(resume, actor);
    }

    private static void disasterFromHand(LoyalistGameState s, int p) {
        int at = s.getRnd().nextInt(s.hands.get(p).size());
        int card = s.hands.get(p).remove(at);
        s.bagCards.add(card);
        s.knownBagCards.get(p).add(card);
        s.handChanged(p);
        s.remember(p, "Natural disaster took card " + card);
        s.announce("Natural disaster took one card from P" + p);
    }

    private static void nextSupply(LoyalistGameState s) {
        while (s.supplyResource < 3) {
            int r = s.supplyResource, p = s.offices[r];
            boolean allowed = normalSupply(s, r) || (r == 0 && s.reforms[11] && event(s).allows(2));
            if (allowed && p >= 1 && !s.isExiled(p)) {
                if (s.firstSupplier < 0) s.firstSupplier = p;
                s.transition(SUPPLY, p);
                s.peekBag(p, false);
                return;
            }
            s.supplyResource++;
        }
        s.transition(s.reforms[9] ? AUDIT : INTRIGUE_PREPARE, 0);
    }

    private static void beginIntrigue(LoyalistGameState s, int p) {
        s.closeBag();
        s.transition(INTRIGUE_STEAL, p);
        s.peekBag(p, true);
        s.theftOpeningBag[p] = s.bagTokens;
        s.theftOpeningAllowance[p] =
                Math.min(
                        s.bagTokens,
                        Math.min(s.reforms[6] ? 2 : 3, (s.reforms[6] ? 3 : 4) - s.embezzled));
        s.theftTaken[p] = 0;
        // Once this private turn starts, others cannot assume an old wealth observation is still
        // current.
        for (int q = 0; q < 5; q++) if (q != p) s.knownWealth[q][p] = -1;
    }

    static void settle(LoyalistGameState s) {
        s.inspector = -1;
        s.peekBag(0, true);
        if (s.reforms[5] && s.bagCards.stream().anyMatch(c -> c < 0)) {
            int lowest = Collections.min(s.bagCards);
            s.bagCards.remove(Integer.valueOf(lowest));
            s.knownBagCards.get(0).remove(Integer.valueOf(lowest));
            s.knownDead.get(0).add(lowest);
            // Other players do not see which negative was removed. Retain historical observations
            // in privateLog, but do not update their current-card ledger using the secret result.
            for (int p = 1; p < 5; p++) s.knownBagCards.get(p).clear();
            s.dead.add(lowest);
            s.peekBag(0, true);
        }
        if (s.reforms[5]) for (int p = 1; p < 5; p++) s.knownBagCards.get(p).clear();
        s.lastMilitaryBonus = s.reforms[2] ? (s.mSupply + 1) / 2 : 0;
        s.lastTotal =
                s.bagTokens
                        + s.bagCards.stream().mapToInt(Integer::intValue).sum()
                        + s.lastMilitaryBonus;
        s.roundSuccess = s.lastTotal >= event(s).threshold();
        s.announce(
                "National total=" + s.lastTotal + "; " + (s.roundSuccess ? "success" : "failure"));
        if (s.roundSuccess) {
            s.prosperity++;
            if (s.proposedReform > 0) {
                s.reforms[s.proposedReform] = true;
                s.announce("Reform adopted: " + s.proposedReform);
            }
            s.transition(REWARD, 0);
        } else {
            s.crisis++;
            if (s.crisis >= 4) {
                finish(s, true);
                return;
            }
            if (s.proposedReform > 0) {
                s.removedReforms[s.proposedReform] = true;
                s.announce("Reform rejected: " + s.proposedReform);
            }
            s.reserve += s.bagTokens;
            s.bagTokens = 0;
            discardBag(s);
            s.transition(EXILE, 0);
        }
    }

    static void returnResources(LoyalistGameState s, int destination) {
        LoyalistData.Event ev = event(s);
        int tokens = s.bagTokens;
        if (ev.destroyed()) s.reserve += tokens + 1;
        else if (ev.halfDestroyed()) {
            s.reserve += (tokens + 1) / 2 + 1;
            s.treasury[2] += tokens / 2;
            s.treasuryChanged(2);
        } else {
            s.treasury[destination] += tokens * ev.multiplier() + ev.additional() + 1;
            s.treasuryChanged(destination);
        }
        s.publicTokenUpperBound = s.publicTokenUpperBound * ev.multiplier() + ev.additional() + 1;
        s.publicTokenLowerBound += ev.additional() + 1;
        s.announce("Resources returned to " + (ev.destroyed() ? "reserve" : destination));
        s.bagTokens = 0;
        discardBag(s);
        s.stage = s.prosperity < 3 ? 0 : s.prosperity < 6 ? 1 : 2;
    }

    private static void discardBag(LoyalistGameState s) {
        for (int p = 0; p < 5; p++) {
            s.knownDead.get(p).addAll(s.knownBagCards.get(p));
            s.knownBagCards.get(p).clear();
        }
        s.dead.addAll(s.bagCards);
        s.bagCards.clear();
        s.closeBag();
    }

    static void exile(LoyalistGameState s, int target) {
        s.exileTarget = target;
        s.exileThroughRound[target] = s.round + 1;
        s.transferWealthToTreasury(target, 1, true);
        s.cardsToConfiscate = s.hands.get(target).size() / 2;
        s.announce("P" + target + " exiled through round " + (s.round + 1));
        // Offices are marked vacant immediately; the king can assign each independently, including
        // multiple offices.
        for (int r = 0; r < 3; r++) if (s.offices[r] == target) s.offices[r] = -2;
        if (s.cardsToConfiscate > 0) s.transition(EXILE_CARDS, 0);
        else beginReassignment(s);
    }

    private static void beginReassignment(LoyalistGameState s) {
        s.reassignmentResource = 0;
        nextReassignment(s);
    }

    private static void nextReassignment(LoyalistGameState s) {
        while (s.reassignmentResource < 3 && s.offices[s.reassignmentResource] != -2)
            s.reassignmentResource++;
        if (s.reassignmentResource < 3) s.transition(REASSIGN_OFFICE, 0);
        else beginTreasuryReports(s);
    }

    private static void beginTreasuryReports(LoyalistGameState s) {
        s.transition(TREASURY_PLAN, 0);
    }

    private static void nextTreasuryReport(LoyalistGameState s) {
        while (s.reportResource < 3) {
            int p = s.offices[s.reportResource];
            if (p > 0 && !s.isExiled(p)) {
                s.transition(TREASURY_REPORT, p);
                return;
            }
            s.reportResource++;
        }
        s.transition(CONVERT_W_M, 0);
    }

    static void convert(LoyalistGameState s, int from, int to, int requested) {
        int actual = Math.min(requested, s.treasury[from] / 2);
        s.treasury[from] -= actual * 2;
        s.treasury[to] += actual;
        s.reserve += actual;
        for (int viewer = 0; viewer < 5; viewer++) {
            int knownSource = s.knownTreasury[viewer][from];
            if (knownSource >= 0) {
                int inferred = Math.min(requested, knownSource / 2);
                s.knownTreasury[viewer][from] -= inferred * 2;
                if (s.knownTreasury[viewer][to] >= 0) s.knownTreasury[viewer][to] += inferred;
            } else s.knownTreasury[viewer][to] = -1;
        }
        s.refreshOwners();
        s.announce("King ordered " + requested + " conversions " + from + " -> " + to);
    }

    static void finish(LoyalistGameState s, boolean spyWins) {
        Set<Integer> winners = new HashSet<>();
        if (spyWins) {
            for (int p = 1; p < 5; p++) if (s.roles[p] == SPY) winners.add(p);
        } else {
            int loyal = 0, maximum = 0;
            for (int p = 1; p < 5; p++)
                if (!s.isExiled(p)) {
                    if (s.roles[p] == LOYALIST) loyal = s.wealth[p];
                    if (s.roles[p] != SPY) maximum = Math.max(maximum, s.wealth[p]);
                }
            for (int p = 1; p < 5; p++)
                if (!s.isExiled(p)
                        && s.roles[p] == MAGNATE
                        && s.wealth[p] == maximum
                        && s.wealth[p] >= 3
                        && s.wealth[p] > s.treasury[2] + loyal) winners.add(p);
            if (winners.isEmpty()) {
                winners.add(0);
                for (int p = 1; p < 5; p++) if (s.roles[p] == LOYALIST) winners.add(p);
            }
        }
        for (int p = 0; p < 5; p++)
            s.setPlayerResult(winners.contains(p) ? WIN_GAME : LOSE_GAME, p);
        s.setGameStatus(GAME_END);
        s.transition(FINISHED, 0);
        s.announce("Game over; winners=" + winners + "; roles=" + Arrays.toString(s.roles));
    }
}
