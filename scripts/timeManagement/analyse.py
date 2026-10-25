"""
Win rates and time use from run_matchup.py or run_tournament.py output directories (two-player games).

usage: python3 analyse.py OUTDIR [OUTDIR ...]
"""
import csv, glob, math, os, sys
from collections import defaultdict


def output_files(outdir, name):
    return glob.glob(os.path.join(outdir, "run*", name)) + glob.glob(os.path.join(outdir, "chunk*", name))


def games(outdir):
    for f in output_files(outdir, "GAME_OVER.csv"):
        for row in csv.DictReader(open(f)):
            types = [row["PlayerType(PlayerType-%d)" % p] for p in (0, 1)]
            ranks = [int(row["OrdinalPosition(Player-%d)" % p]) for p in (0, 1)]
            yield types, ranks


def decisions(outdir):
    for f in output_files(outdir, "ACTION_CHOSEN.csv"):
        seen = set()
        for row in csv.DictReader(open(f)):
            row["file"] = f  # game IDs restart in each output directory
            # before fb92401 the metric could record a search a second time (after a decision with one action);
            # with BUDGET_ROUND_SECONDS decisions are numbered within the round
            key = (row["GameID"], row["Round"], row["TimeManagement(PlayerID)"], row["TimeManagement(Decision)"])
            if key not in seen:
                seen.add(key)
                yield row


def analyse(outdir):
    score, n, first, n_first = defaultdict(float), defaultdict(int), defaultdict(float), defaultdict(int)
    pair_score, pair_n = defaultdict(float), defaultdict(int)
    for types, ranks in games(outdir):
        for p in (0, 1):
            s = 0.5 if ranks[0] == ranks[1] else (1.0 if ranks[p] < ranks[1 - p] else 0.0)
            score[types[p]] += s
            n[types[p]] += 1
            pair_score[(types[p], types[1 - p])] += s
            pair_n[(types[p], types[1 - p])] += 1
            if p == 0:
                first[types[p]] += s
                n_first[types[p]] += 1
    names = sorted(n)
    if not names:
        print(outdir, "no games")
        return
    rows = list(decisions(outdir))
    per_game = defaultdict(lambda: defaultdict(int))
    by_decision = defaultdict(lambda: defaultdict(list))
    by_round = defaultdict(lambda: defaultdict(int))
    stats = defaultdict(lambda: defaultdict(float))
    for r in rows:
        t = r["TimeManagement(PlayerType)"]
        ms = int(r["TimeManagement(DecisionMS)"])
        per_game[t][(r["file"], r["GameID"], r["GameSeed"])] += ms
        by_decision[t][int(r["TimeManagement(Decision)"])].append(ms)
        by_round[t][int(r["Round"])] += ms
        stats[t]["decisions"] += 1
        stats[t]["stopped"] += r["TimeManagement(StoppedEarly)"] == "true"
        stats[t]["extended"] += int(r["TimeManagement(Extensions)"]) > 0
        stats[t]["iterations"] += int(r["TimeManagement(Iterations)"])
        stats[t]["searchMS"] += float(r["TimeManagement(SearchMS)"])
        stats[t]["minClock"] = min(stats[t].get("minClock", 1e12), int(r["TimeManagement(ClockMS)"]))
    print("==", outdir)
    for t in names:
        w, games_played = score[t] / n[t], n[t]
        se = math.sqrt(w * (1 - w) / games_played)
        totals = list(per_game[t].values())
        st = stats[t]
        print("%-9s win %.1f%% (95%% CI %.1f-%.1f) over %d games; as first player %.1f%%" %
              (t, 100 * w, 100 * (w - 1.96 * se), 100 * (w + 1.96 * se), games_played,
               100 * first[t] / max(n_first[t], 1)))
        if len(names) > 2:
            print("          against: " + ", ".join("%s %.1f%% (%d)" % (o, 100 * pair_score[(t, o)] / pair_n[(t, o)],
                                                                       pair_n[(t, o)])
                                                  for o in names if pair_n[(t, o)] > 0))
        print("          clock used per game: mean %.0f ms, max %d ms; min clock left %d ms; decisions/game %.1f;"
              " stopped early %.0f%%, extended %.0f%%; iterations/s %.0f" %
              (sum(totals) / len(totals), max(totals), st["minClock"], st["decisions"] / len(totals),
               100 * st["stopped"] / st["decisions"], 100 * st["extended"] / st["decisions"],
               1000 * st["iterations"] / max(st["searchMS"], 1)))
        dist = by_decision[t]
        print("          mean ms by decision: " + " ".join(
            "%d:%.0f" % (d, sum(dist[d]) / len(dist[d])) for d in sorted(dist) if len(dist[d]) >= 10))
        if 1 < len(by_round[t]) <= 10:
            print("          mean ms per game in each round: " + " ".join(
                "%d:%.0f" % (rnd, ms / len(totals)) for rnd, ms in sorted(by_round[t].items())))


for d in sys.argv[1:]:
    analyse(d)
