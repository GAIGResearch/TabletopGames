package games.loyalist;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.interfaces.IGamePhase;

import games.GameType;

import java.util.*;

/**
 * Authoritative rules state, or a sampled world when obtained through copy(playerId). Agents must
 * use the observation supplied by TAG, never the authoritative state.
 */
public class LoyalistGameState extends AbstractGameState {
    public enum Role {
        KING,
        LOYALIST,
        MAGNATE,
        SPY
    }

    public enum Phase implements IGamePhase {
        EVENT_DRAW,
        PURCHASE_PREPARE,
        DISASTER,
        SUPPLY_PREPARE,
        INTRIGUE_PREPARE,
        SETTLEMENT,
        AFTER_SETTLEMENT,
        EXCHANGE_PREPARE,
        SALARY,
        ROUND_END,
        ASSIGN_OFFICES,
        ROUND_LEVY,
        ROUND_INFORMANT,
        EVENT_CHOICE,
        REFORM_PROPOSAL,
        PURCHASE,
        KEEP_CARDS,
        DISASTER_CHOICE,
        SUPPLY,
        SUPPLY_REPORT,
        AUDIT,
        FIRST_NOMINATION,
        INTRIGUE_STEAL,
        INTRIGUE_CONTRIBUTE,
        INTRIGUE_CARDS,
        TESTIMONY,
        NEXT_NOMINATION,
        REWARD,
        RETURN_RESOURCES,
        EXILE,
        EXILE_CARDS,
        REASSIGN_OFFICE,
        TREASURY_PLAN,
        TREASURY_REPORT,
        CONVERT_W_M,
        CONVERT_P_W,
        EXCHANGE,
        APPOINT_INSPECTOR,
        FINAL_PURGE,
        FINISHED
    }

    Role[] roles = new Role[5];
    int[] treasury = {12, 6, 3}, offices = {-1, -1, -1}, wealth = {0, 1, 1, 1, 1};
    int[] exileThroughRound = new int[5];
    List<List<Integer>> hands = fiveLists();
    List<Integer> deck = new ArrayList<>(), dead = new ArrayList<>(), bagCards = new ArrayList<>();
    List<List<Integer>> eventDecks = new ArrayList<>(), eventDiscards = new ArrayList<>();
    List<Integer> eventOptions = new ArrayList<>();
    List<List<Integer>> kingKnownEventBottom =
            new ArrayList<>(List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
    boolean[] reforms = new boolean[12],
            removedReforms = new boolean[12],
            triggered = new boolean[11];
    boolean[] bought = new boolean[5], acquired = new boolean[5], acted = new boolean[5];
    int round = 0,
            prosperity = 0,
            crisis = 0,
            reserve = 0,
            bagTokens = 0,
            eventId = 0,
            proposedReform = 0;
    int stage = 0, priorProsperity = 0, mSupply = 0, embezzled = 0, inspector = -1;
    int militarySupplier = -1;
    int officeIndex = 0, supplyResource = 0, firstSupplier = -1, queueIndex = 0;
    List<Integer> queue = new ArrayList<>();
    int exileTarget = -1, cardsToConfiscate = 0, reassignmentResource = 0, reportResource = 0;
    int pendingTokenChange = 0, lastTotal = 0, lastMilitaryBonus = 0;
    boolean roundSuccess = false;
    int publicTokenUpperBound = 25, publicTokenLowerBound = 25;
    Phase phase = Phase.ASSIGN_OFFICES, resumePhase = Phase.PURCHASE;
    int resumePlayer = 0;
    List<Integer> keepSelected = new ArrayList<>();
    List<String> publicLog = new ArrayList<>();
    List<List<String>> privateLog = fiveStringLists();
    // These are information records, not aliases to authoritative values. -1 means not currently
    // known.
    int[][] knownTreasury = new int[5][3],
            knownWealth = new int[5][5],
            knownHandSum = new int[5][5];
    int[] rememberedBagTokens = new int[5], rememberedBagCount = new int[5];
    boolean[] liveBagTokens = new boolean[5], liveBagCards = new boolean[5];
    List<List<Integer>> knownBagCards = fiveLists(), knownDead = fiveLists();
    int observer = -1;
    int[] theftOpeningBag = new int[5], theftOpeningAllowance = new int[5], theftTaken = new int[5];

    public LoyalistGameState(AbstractParameters parameters, int nPlayers) {
        super(parameters, nPlayers);
        if (nPlayers != 5)
            throw new IllegalArgumentException("Loyalist requires exactly five players");
        for (int p = 0; p < 5; p++) {
            Arrays.fill(knownTreasury[p], -1);
            Arrays.fill(knownWealth[p], -1);
            Arrays.fill(knownHandSum[p], Integer.MIN_VALUE);
        }
        Arrays.fill(rememberedBagTokens, -1);
        Arrays.fill(rememberedBagCount, -1);
        Arrays.fill(theftOpeningBag, -1);
        Arrays.fill(theftOpeningAllowance, -1);
    }

    static List<List<Integer>> fiveLists() {
        List<List<Integer>> list = new ArrayList<>();
        for (int p = 0; p < 5; p++) list.add(new ArrayList<>());
        return list;
    }

    static List<List<String>> fiveStringLists() {
        List<List<String>> list = new ArrayList<>();
        for (int p = 0; p < 5; p++) list.add(new ArrayList<>());
        return list;
    }

    static List<List<Integer>> copyLists(List<List<Integer>> source) {
        List<List<Integer>> result = new ArrayList<>();
        for (List<Integer> a : source) result.add(new ArrayList<>(a));
        return result;
    }

    static int[][] copyArray(int[][] a) {
        return Arrays.stream(a).map(int[]::clone).toArray(int[][]::new);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Loyalist;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return List.of();
    }

    public Phase getPhase() {
        return phase;
    }

    public int getRound() {
        return round;
    }

    public int getProsperity() {
        return prosperity;
    }

    public int getCrisis() {
        return crisis;
    }

    public int getEventId() {
        return eventId;
    }

    public int getTreasury(int resource) {
        return treasury[resource];
    }

    public int getWealth(int player) {
        return wealth[player];
    }

    public Role getRole(int player) {
        return roles[player];
    }

    public List<Integer> getHand(int player) {
        return List.copyOf(hands.get(player));
    }

    public int getOfficeHolder(int resource) {
        return offices[resource];
    }

    public boolean isExiled(int player) {
        return exileThroughRound[player] >= round && exileThroughRound[player] > 0;
    }

    public int getBagTokens() {
        return bagTokens;
    }

    public List<Integer> getBagCards() {
        List<Integer> values = new ArrayList<>(bagCards);
        Collections.sort(values);
        return List.copyOf(values);
    }

    public List<String> getPublicLog() {
        return List.copyOf(publicLog);
    }

    public List<String> getPrivateLog(int player) {
        return List.copyOf(privateLog.get(player));
    }

    public int getRememberedBagTokens(int player) {
        return rememberedBagTokens[player];
    }

    public int getRememberedBagCardCount(int player) {
        return rememberedBagCount[player];
    }

    public int getKnownTreasury(int player, int resource) {
        return knownTreasury[player][resource];
    }

    public int getKnownWealth(int player, int target) {
        return knownWealth[player][target];
    }

    public boolean hasReform(int id) {
        return reforms[id];
    }

    public int getReserve() {
        return reserve;
    }

    public int getEmbezzled() {
        return embezzled;
    }

    public int getLastTotal() {
        return lastTotal;
    }

    public int getPublicTokenUpperBound() {
        return publicTokenUpperBound;
    }

    public int getPublicTokenLowerBound() {
        return publicTokenLowerBound;
    }

    public int getObserver() {
        return observer;
    }

    public void setRedeterminisationSeed(long seed) {
        redeterminisationRnd = new Random(seed);
    }

    List<Integer> active() {
        List<Integer> ret = new ArrayList<>();
        for (int p = 1; p < 5; p++) if (!isExiled(p)) ret.add(p);
        return ret;
    }

    void roundBegan() {
        roundCounter = round - 1;
        turnCounter = 0;
    }

    void transition(Phase next, int actor) {
        if (turnOwner != actor) turnCounter++;
        phase = next;
        setGamePhase(next);
        setTurnOwner(actor);
    }

    void announce(String message) {
        publicLog.add("R" + round + ": " + message);
    }

    void remember(int player, String message) {
        privateLog.get(player).add("R" + round + ": " + message);
    }

    void refreshOwners() {
        for (int p = 1; p < 5; p++) knownWealth[p][p] = wealth[p];
        for (int r = 0; r < 3; r++)
            if (offices[r] >= 1 && !isExiled(offices[r]))
                knownTreasury[offices[r]][r] = treasury[r];
    }

    void treasuryChanged(int resource) {
        for (int p = 0; p < 5; p++) knownTreasury[p][resource] = -1;
        refreshOwners();
    }

    /**
     * Public fixed transfers update existing exact knowledge without revealing unknown holdings.
     */
    void publicWealthDelta(int player, int delta) {
        wealth[player] += delta;
        for (int viewer = 0; viewer < 5; viewer++)
            if (knownWealth[viewer][player] >= 0) knownWealth[viewer][player] += delta;
        knownWealth[player][player] = wealth[player];
    }

    void publicTreasuryDelta(int resource, int delta) {
        treasury[resource] += delta;
        for (int viewer = 0; viewer < 5; viewer++)
            if (knownTreasury[viewer][resource] >= 0) knownTreasury[viewer][resource] += delta;
        refreshOwners();
    }

    /** A known starting holding determines either a half confiscation or a one-token levy. */
    void transferWealthToTreasury(int player, int resource, boolean half) {
        int actual = half ? wealth[player] / 2 : Math.min(1, wealth[player]);
        wealth[player] -= actual;
        treasury[resource] += actual;
        for (int viewer = 0; viewer < 5; viewer++) {
            int prior = knownWealth[viewer][player];
            if (prior >= 0) {
                int inferred = half ? prior / 2 : Math.min(1, prior);
                knownWealth[viewer][player] = prior - inferred;
                if (knownTreasury[viewer][resource] >= 0)
                    knownTreasury[viewer][resource] += inferred;
            } else knownTreasury[viewer][resource] = -1;
        }
        refreshOwners();
    }

    void wealthChanged(int player) {
        for (int p = 0; p < 5; p++) knownWealth[p][player] = -1;
        knownWealth[player][player] = wealth[player];
    }

    void handChanged(int player) {
        for (int p = 0; p < 5; p++) knownHandSum[p][player] = Integer.MIN_VALUE;
    }

    void peekBag(int player, boolean cards) {
        rememberedBagTokens[player] = bagTokens;
        rememberedBagCount[player] = bagCards.size();
        liveBagTokens[player] = true;
        if (cards) {
            knownBagCards.set(player, new ArrayList<>(getBagCards()));
            liveBagCards[player] = true;
        }
        remember(
                player,
                "Bag observed: tokens="
                        + bagTokens
                        + ", cards="
                        + (cards ? knownBagCards.get(player) : bagCards.size()));
    }

    void closeBag() {
        Arrays.fill(liveBagTokens, false);
        Arrays.fill(liveBagCards, false);
    }

    void discardCard(int value, int playerWhoKnows) {
        dead.add(value);
        if (playerWhoKnows >= 0) knownDead.get(playerWhoKnows).add(value);
    }

    int remainingCards() {
        return deck.size() + dead.size();
    }

    int buyRate() {
        return crisis == 0 ? 2 : crisis == 1 ? 3 : 4;
    }

    int sellRate() {
        return crisis == 0 ? 3 : crisis == 1 ? 2 : 1;
    }

    int handCount(int player) {
        return hands.get(player).size()
                + ((phase == Phase.KEEP_CARDS && getCurrentPlayer() == player)
                        ? keepSelected.size()
                        : 0);
    }

    @Override
    protected LoyalistGameState _copy(int playerId) {
        LoyalistGameState c = new LoyalistGameState(gameParameters.copy(), 5);
        c.roles = roles.clone();
        c.treasury = treasury.clone();
        c.offices = offices.clone();
        c.wealth = wealth.clone();
        c.exileThroughRound = exileThroughRound.clone();
        c.hands = copyLists(hands);
        c.deck = new ArrayList<>(deck);
        c.dead = new ArrayList<>(dead);
        c.bagCards = new ArrayList<>(bagCards);
        c.eventDecks = copyLists(eventDecks);
        c.eventDiscards = copyLists(eventDiscards);
        c.eventOptions = new ArrayList<>(eventOptions);
        c.kingKnownEventBottom = copyLists(kingKnownEventBottom);
        c.reforms = reforms.clone();
        c.removedReforms = removedReforms.clone();
        c.triggered = triggered.clone();
        c.bought = bought.clone();
        c.acquired = acquired.clone();
        c.acted = acted.clone();
        c.round = round;
        c.prosperity = prosperity;
        c.crisis = crisis;
        c.reserve = reserve;
        c.bagTokens = bagTokens;
        c.eventId = eventId;
        c.proposedReform = proposedReform;
        c.stage = stage;
        c.priorProsperity = priorProsperity;
        c.mSupply = mSupply;
        c.militarySupplier = militarySupplier;
        c.embezzled = embezzled;
        c.inspector = inspector;
        c.officeIndex = officeIndex;
        c.supplyResource = supplyResource;
        c.firstSupplier = firstSupplier;
        c.queueIndex = queueIndex;
        c.queue = new ArrayList<>(queue);
        c.exileTarget = exileTarget;
        c.cardsToConfiscate = cardsToConfiscate;
        c.reassignmentResource = reassignmentResource;
        c.reportResource = reportResource;
        c.pendingTokenChange = pendingTokenChange;
        c.lastTotal = lastTotal;
        c.lastMilitaryBonus = lastMilitaryBonus;
        c.roundSuccess = roundSuccess;
        c.publicTokenUpperBound = publicTokenUpperBound;
        c.publicTokenLowerBound = publicTokenLowerBound;
        c.phase = phase;
        c.resumePhase = resumePhase;
        c.resumePlayer = resumePlayer;
        c.keepSelected = new ArrayList<>(keepSelected);
        c.publicLog = new ArrayList<>(publicLog);
        c.privateLog = fiveStringLists();
        for (int p = 0; p < 5; p++) c.privateLog.get(p).addAll(privateLog.get(p));
        c.knownTreasury = copyArray(knownTreasury);
        c.knownWealth = copyArray(knownWealth);
        c.knownHandSum = copyArray(knownHandSum);
        c.rememberedBagTokens = rememberedBagTokens.clone();
        c.rememberedBagCount = rememberedBagCount.clone();
        c.liveBagTokens = liveBagTokens.clone();
        c.liveBagCards = liveBagCards.clone();
        c.copyActor = getCurrentPlayer();
        c.theftOpeningBag = theftOpeningBag.clone();
        c.theftOpeningAllowance = theftOpeningAllowance.clone();
        c.theftTaken = theftTaken.clone();
        c.knownBagCards = copyLists(knownBagCards);
        c.knownDead = copyLists(knownDead);
        c.observer = observer;
        if (playerId >= 0 && isNotTerminal()) {
            // Only the observation ledger of this player constrains the sampled world.
            c.observer = playerId;
            for (int p = 0; p < 5; p++)
                if (p != playerId) {
                    c.privateLog.get(p).clear();
                    Arrays.fill(c.knownTreasury[p], -1);
                    Arrays.fill(c.knownWealth[p], -1);
                    Arrays.fill(c.knownHandSum[p], Integer.MIN_VALUE);
                    c.rememberedBagTokens[p] = -1;
                    c.rememberedBagCount[p] = -1;
                    c.theftOpeningBag[p] = -1;
                    c.theftOpeningAllowance[p] = -1;
                    c.theftTaken[p] = 0;
                    c.liveBagTokens[p] = false;
                    c.liveBagCards[p] = false;
                    c.knownBagCards.get(p).clear();
                    c.knownDead.get(p).clear();
                }
            c.sampleRoles(playerId, redeterminisationRnd);
            c.sampleCards(playerId, redeterminisationRnd);
            c.sampleTokens(playerId, redeterminisationRnd);
            for (int eventStage = 0; eventStage < c.eventDecks.size(); eventStage++) {
                List<Integer> events = c.eventDecks.get(eventStage);
                List<Integer> knownBottom = c.kingKnownEventBottom.get(eventStage);
                if (playerId != 0) knownBottom.clear();
                for (int known : knownBottom) {
                    if (!events.remove(Integer.valueOf(known)))
                        throw new IllegalStateException("Known event bottom missing from deck");
                }
                Collections.sort(events);
                Collections.shuffle(events, redeterminisationRnd);
                events.addAll(knownBottom);
            }
            // The two privately viewed event options are known only by the king.
            if (playerId != 0 && !c.eventOptions.isEmpty()) {
                List<Integer> events = c.eventDecks.get(stage);
                events.addAll(c.eventOptions);
                Collections.sort(events);
                Collections.shuffle(events, redeterminisationRnd);
                c.eventOptions.clear();
                c.eventOptions.add(events.remove(0));
                c.eventOptions.add(events.remove(0));
            }
            // The action interface reveals an allowance, not the exact amount stolen by
            // predecessors.
            c.sampleEmbezzlement(
                    playerId,
                    getCurrentPlayer(),
                    phase == Phase.INTRIGUE_STEAL
                            ? Math.min(
                                    bagTokens,
                                    Math.min(reforms[6] ? 2 : 3, (reforms[6] ? 3 : 4) - embezzled))
                            : -1,
                    redeterminisationRnd);
            if (militarySupplier < 0) c.mSupply = 0;
            else if (playerId != militarySupplier) {
                if (playerId == 0
                        && reforms[2]
                        && (phase == Phase.REWARD || phase == Phase.RETURN_RESOURCES)) {
                    c.mSupply =
                            lastMilitaryBonus == 0
                                    ? 0
                                    : 2 * lastMilitaryBonus - 1 + redeterminisationRnd.nextInt(2);
                } else
                    c.mSupply =
                            redeterminisationRnd.nextInt(Math.min(12, publicTokenUpperBound) + 1);
            }
            if (playerId != 0) c.lastMilitaryBonus = c.reforms[2] ? (c.mSupply + 1) / 2 : 0;
            c.pendingTokenChange =
                    0; // the actor's exact executed amount is already in their private memory
            c.refreshOwners();
        }
        return c;
    }

    private void sampleEmbezzlement(int viewer, int actor, int currentAllowance, Random random) {
        int total = reforms[6] ? 3 : 4, personal = reforms[6] ? 2 : 3;
        List<Integer> candidates = new ArrayList<>();
        if (viewer == actor && phase == Phase.INTRIGUE_STEAL) {
            for (int used = 0; used <= total; used++)
                if (Math.min(bagTokens, Math.min(personal, total - used)) == currentAllowance)
                    candidates.add(used);
        } else if (viewer == actor && isIntriguePhase() && theftOpeningAllowance[viewer] >= 0) {
            for (int before = 0; before <= total; before++)
                if (Math.min(theftOpeningBag[viewer], Math.min(personal, total - before))
                                == theftOpeningAllowance[viewer]
                        && before + theftTaken[viewer] <= total)
                    candidates.add(before + theftTaken[viewer]);
        } else for (int used = 0; used <= total; used++) candidates.add(used);
        if (candidates.isEmpty())
            throw new IllegalStateException("Theft allowance cannot be sampled");
        embezzled = candidates.get(random.nextInt(candidates.size()));
    }

    private boolean isIntriguePhase() {
        return phase == Phase.INTRIGUE_STEAL
                || phase == Phase.INTRIGUE_CONTRIBUTE
                || phase == Phase.INTRIGUE_CARDS
                || phase == Phase.TESTIMONY
                || phase == Phase.NEXT_NOMINATION;
    }

    private void sampleRoles(int viewer, Random random) {
        List<Role> pool =
                new ArrayList<>(List.of(Role.LOYALIST, Role.MAGNATE, Role.MAGNATE, Role.SPY));
        if (viewer > 0) pool.remove(roles[viewer]);
        Collections.shuffle(pool, random);
        roles[0] = Role.KING;
        for (int p = 1; p < 5; p++) if (p != viewer) roles[p] = pool.remove(0);
    }

    private void sampleCards(int viewer, Random random) {
        List<Integer> pool = LoyalistData.powerCards();
        if (viewer > 0) removeKnown(pool, hands.get(viewer));
        if (phase == Phase.KEEP_CARDS && getTurnOwnerForCopy() == viewer)
            removeKnown(pool, keepSelected);
        List<Integer> bagKnown =
                bagExists() ? new ArrayList<>(knownBagCards.get(viewer)) : new ArrayList<>();
        removeKnown(pool, bagKnown);
        List<Integer> deadKnown = new ArrayList<>(knownDead.get(viewer));
        removeKnown(pool, deadKnown);
        int bagN = bagCards.size(), deadN = dead.size(), deckN = deck.size();
        List<Integer> handSizes = new ArrayList<>();
        for (List<Integer> hand : hands) handSizes.add(hand.size());
        for (int p = 1; p < 5; p++)
            if (p != viewer && knownHandSum[viewer][p] != Integer.MIN_VALUE) {
                hands.set(p, takeWithSum(pool, handSizes.get(p), knownHandSum[viewer][p], random));
            }
        Collections.shuffle(pool, random);
        for (int p = 1; p < 5; p++)
            if (p != viewer && knownHandSum[viewer][p] == Integer.MIN_VALUE)
                hands.set(p, take(pool, handSizes.get(p)));
        if (phase == Phase.KEEP_CARDS && getTurnOwnerForCopy() != viewer)
            keepSelected = take(pool, keepSelected.size());
        bagCards = bagKnown;
        bagCards.addAll(take(pool, bagN - bagKnown.size()));
        Collections.sort(bagCards);
        dead = deadKnown;
        dead.addAll(take(pool, deadN - deadKnown.size()));
        deck = take(pool, deckN);
        if (!pool.isEmpty())
            throw new IllegalStateException("Card inventory mismatch in determinization");
    }

    // _copy runs before the framework copies turnOwner; retain the authoritative actor through this
    // helper.
    private int getTurnOwnerForCopy() {
        return copyActor;
    }

    int copyActor = 0;

    private static void removeKnown(List<Integer> pool, List<Integer> known) {
        for (int card : known)
            if (!pool.remove(Integer.valueOf(card)))
                throw new IllegalStateException("Inconsistent card knowledge");
    }

    static List<Integer> take(List<Integer> source, int n) {
        if (n < 0 || n > source.size())
            throw new IllegalStateException("Card count cannot be sampled: " + n);
        List<Integer> out = new ArrayList<>(source.subList(0, n));
        source.subList(0, n).clear();
        return out;
    }

    static List<Integer> takeWithSum(List<Integer> pool, int n, int sum, Random random) {
        int[] counts = new int[7];
        for (int x : pool) counts[x + 3]++;
        List<Integer> result = new ArrayList<>();
        if (!sumSearch(counts, 0, n, sum, result, random, new HashSet<>()))
            throw new IllegalStateException("Inconsistent observed hand sum");
        removeKnown(pool, result);
        return result;
    }

    private static boolean sumSearch(
            int[] counts,
            int index,
            int n,
            int sum,
            List<Integer> out,
            Random random,
            Set<String> failed) {
        if (index == 7) return n == 0 && sum == 0;
        String key = index + ":" + n + ":" + sum;
        if (failed.contains(key) || n < 0 || Math.abs(sum) > 3 * n) return false;
        List<Integer> options = new ArrayList<>();
        for (int k = 0; k <= Math.min(counts[index], n); k++) options.add(k);
        Collections.shuffle(options, random);
        for (int k : options) {
            int old = out.size();
            for (int j = 0; j < k; j++) out.add(index - 3);
            if (sumSearch(counts, index + 1, n - k, sum - k * (index - 3), out, random, failed))
                return true;
            out.subList(old, out.size()).clear();
        }
        failed.add(key);
        return false;
    }

    private void sampleTokens(int viewer, Random random) {
        int known = reserve;
        for (int r = 0; r < 3; r++) {
            treasury[r] = knownTreasury[viewer][r];
            if (treasury[r] >= 0) known += treasury[r];
        }
        for (int p = 1; p < 5; p++) {
            wealth[p] = knownWealth[viewer][p];
            if (wealth[p] >= 0) known += wealth[p];
        }
        // Outside an open bag phase, the bag is mechanically empty.
        bagTokens = !bagExists() ? 0 : liveBagTokens[viewer] ? rememberedBagTokens[viewer] : -1;
        if (bagTokens >= 0) known += bagTokens;
        List<Integer> slots = new ArrayList<>();
        for (int r = 0; r < 3; r++) if (treasury[r] < 0) slots.add(r);
        for (int p = 1; p < 5; p++) if (wealth[p] < 0) slots.add(3 + p);
        if (bagTokens < 0) slots.add(9);
        Collections.shuffle(slots, random);
        if (slots.isEmpty()) return; // All exact values already determine the inventory.
        int minimumTotal = Math.max(publicTokenLowerBound, known);
        int maximumTotal = Math.max(publicTokenUpperBound, minimumTotal);
        int sampledTotal = minimumTotal + random.nextInt(maximumTotal - minimumTotal + 1);
        int budget = sampledTotal - known;
        for (int index = 0; index < slots.size(); index++) {
            int slot = slots.get(index);
            int amount = index == slots.size() - 1 ? budget : random.nextInt(budget + 1);
            budget -= amount;
            if (slot < 3) treasury[slot] = amount;
            else if (slot == 9) bagTokens = amount;
            else wealth[slot - 3] = amount;
        }
    }

    private boolean bagExists() {
        return switch (phase) {
            case SUPPLY,
                            SUPPLY_REPORT,
                            AUDIT,
                            FIRST_NOMINATION,
                            INTRIGUE_STEAL,
                            INTRIGUE_CONTRIBUTE,
                            INTRIGUE_CARDS,
                            TESTIMONY,
                            NEXT_NOMINATION,
                            REWARD,
                            RETURN_RESOURCES ->
                    true;
            default -> false;
        };
    }

    @Override
    public double getGameScore(int player) {
        return isNotTerminal() ? 0 : getPlayerResults()[player].value;
    }

    @Override
    protected double _getHeuristicScore(int player) {
        if (!isNotTerminal()) return getGameScore(player);
        return roles[player] == Role.SPY
                ? crisis / 4.0 - prosperity / 10.0
                : prosperity / 10.0 - crisis / 4.0;
    }

    @Override
    protected boolean _equals(Object other) {
        if (!(other instanceof LoyalistGameState s)) return false;
        return snapshot().equals(s.snapshot());
    }

    private String snapshot() {
        return Arrays.toString(roles)
                + Arrays.toString(treasury)
                + Arrays.toString(wealth)
                + Arrays.toString(offices)
                + Arrays.toString(exileThroughRound)
                + hands
                + deck
                + dead
                + bagCards
                + eventDecks
                + eventDiscards
                + eventOptions
                + kingKnownEventBottom
                + Arrays.toString(reforms)
                + Arrays.toString(removedReforms)
                + Arrays.toString(triggered)
                + Arrays.toString(bought)
                + Arrays.toString(acquired)
                + Arrays.toString(acted)
                + round
                + ":"
                + prosperity
                + ":"
                + crisis
                + ":"
                + reserve
                + ":"
                + bagTokens
                + ":"
                + eventId
                + ":"
                + proposedReform
                + ":"
                + stage
                + ":"
                + priorProsperity
                + ":"
                + mSupply
                + ":"
                + militarySupplier
                + ":"
                + embezzled
                + ":"
                + inspector
                + ":"
                + officeIndex
                + ":"
                + supplyResource
                + ":"
                + firstSupplier
                + ":"
                + queueIndex
                + queue
                + ":"
                + exileTarget
                + ":"
                + cardsToConfiscate
                + ":"
                + reassignmentResource
                + ":"
                + reportResource
                + ":"
                + lastTotal
                + ":"
                + lastMilitaryBonus
                + ":"
                + roundSuccess
                + ":"
                + publicTokenUpperBound
                + ":"
                + publicTokenLowerBound
                + phase
                + resumePhase
                + resumePlayer
                + keepSelected
                + publicLog
                + privateLog
                + Arrays.deepToString(knownTreasury)
                + Arrays.deepToString(knownWealth)
                + Arrays.deepToString(knownHandSum)
                + Arrays.toString(rememberedBagTokens)
                + Arrays.toString(rememberedBagCount)
                + Arrays.toString(liveBagTokens)
                + Arrays.toString(liveBagCards)
                + knownBagCards
                + knownDead
                + Arrays.toString(theftOpeningBag)
                + Arrays.toString(theftOpeningAllowance)
                + Arrays.toString(theftTaken);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + snapshot().hashCode();
    }

    @Override
    public String toString() {
        return "Loyalist round="
                + round
                + " phase="
                + phase
                + " prosperity="
                + prosperity
                + " crisis="
                + crisis
                + " event="
                + eventId;
    }
}
