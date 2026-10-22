"""
A round-robin tournament between the player configs in a directory, as a series of RunGames chunks so that it can be
resumed: a chunk with a GAME_OVER.csv is complete and is not run again, so after an interruption run the same command.

usage: python3 run_tournament.py GAME PLAYER_DIR SECONDS CHUNKS GAMES_PER_CHUNK OUTDIR [--procs N] [--players N]

Every config's budget is set to SECONDS (with SECONDS 0, each config keeps its own). In 'exhaustive' mode each chunk
plays every ordering of players, so GAMES_PER_CHUNK should be a multiple of the number of orderings. Run from the
TabletopGames root, after mvn compile, with TAG_CP naming a file that holds the Maven dependency classpath.
"""
import argparse, os, subprocess, time

ap = argparse.ArgumentParser()
ap.add_argument("game")
ap.add_argument("player_dir")
ap.add_argument("seconds", type=int)
ap.add_argument("chunks", type=int)
ap.add_argument("games_per_chunk", type=int)
ap.add_argument("outdir")
ap.add_argument("--procs", type=int, default=4)
ap.add_argument("--players", type=int, default=2)
args = ap.parse_args()

cp = "target/classes:" + open(os.environ["TAG_CP"]).read().strip()
pending = [i for i in range(args.chunks)
           if not os.path.exists(os.path.join(args.outdir, "chunk%03d" % i, "GAME_OVER.csv"))]
print("%d of %d chunks to run" % (len(pending), args.chunks), flush=True)
running = []
while pending or running:
    while pending and len(running) < args.procs:
        i = pending.pop(0)
        dest = os.path.join(args.outdir, "chunk%03d" % i)
        os.makedirs(dest, exist_ok=True)
        log = open(dest + ".log", "w")
        cmd = ["java", "-Xmx2g", "-cp", cp, "evaluation.RunGames", "game=" + args.game,
               "nPlayers=%d" % args.players, "mode=exhaustive", "playerDirectory=" + args.player_dir,
               "matchups=%d" % args.games_per_chunk, "budget=%d" % args.seconds,
               "listener=metrics/timeManagement.json", "destDir=" + dest, "seed=%d" % (7919 * (i + 1))]
        running.append((i, subprocess.Popen(cmd, stdout=log, stderr=subprocess.STDOUT)))
    time.sleep(10)
    for i, p in list(running):
        if p.poll() is not None:
            running.remove((i, p))
            print("chunk %d exit code %d" % (i, p.returncode), flush=True)
