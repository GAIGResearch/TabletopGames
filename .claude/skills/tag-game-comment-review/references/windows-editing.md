# Windows: how to edit files

(Copied from the tag-game-implement skill.)

- **Write Java, JSON and scripts with the Write tool, and change them with the Edit tool.** Never pass source text
  through a Bash heredoc or a Python/sed string:
  - a Bash heredoc containing an apostrophe fails with "unexpected EOF" (Klaverjassen: a Python edit script with
    "pagat's" in it) - a JSON or Python file passed to a script counts: write it with Write into the scratchpad and
    run the file;
  - **backslashes lose a level of escaping** when Java text goes through a Python or shell string: the fmtester
    line `"agent=json\\players\\mcts.json"` came out as `"agent=json\players\mcts.json"` (Klaverjassen), and a bad
    escape in an fmtester file stopped a whole rule-break run (GolfSix). Any Java string literal with a backslash
    (paths, regexes, `split("\\.")`) goes in with Write or Edit, and is checked by reading the file back.
- **`sed -i` in Git Bash rewrites a CRLF file as LF.** The repo is checked out CRLF with `core.autocrlf=true`, which
  stores LF, so the diff looks clean and only a "LF will be replaced by CRLF" warning shows it. Use the Edit tool (or
  a Python script written with Write, which keeps CRLF in text mode); if sed was used, convert the files back.
- **Files written with the Write tool are LF**, while files checked out or edited in place are CRLF, so a new game's
  files end up mixed (President: new files LF, edited ones CRLF, and one file with both after a Python edit that
  assumed CRLF). A Python edit script opened with `newline=''` must detect each file's ending
  (`'\r\n' in text`) rather than assume one. The clean-up stage normalises the game's files to CRLF.
- **`rulebreaks.py` leaves every file it breaks CRLF.** It reads and writes in text mode, which on Windows writes
  CRLF, and restores the file the same way - so an LF file (e.g. one just made with Write) is CRLF after the first
  run that touches it. In Sueca a Python edit written for the LF file failed its assert after Phase A's run. Its
  breaks JSON may use `\n` in `old`/`new` whatever the file's endings (text mode reads CRLF as `\n`).
- **A Python edit script must assert that each text it replaces is found exactly once**
  (`assert text.count(old) == 1, old`) before writing anything. `str.replace` does nothing when the text is not
  there, so a mismatch is silent: in Scarto two Javadoc edits with the wrong indentation never happened, and were
  only noticed stages later.
- **Check line endings only by counting bytes in Python** (`b.count(b'\r\n')` against `b.count(b'\n')`). The Read
  tool does not show carriage returns, so a file read with it looks LF whatever it holds: in Rummy the comment-review
  sub-agent reported CRLF files as LF-only. Sub-agent briefs need not mention line endings; the Edit tool keeps them.
- In PowerShell, quote dotted `-D` properties.
