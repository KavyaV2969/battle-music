# Battle Music agent handoff

Prepared 2026-10-10 (Asia/Singapore). Continue in this independent, private Git repository, **not** the parent modpack as a source repository.

## Current stopping point

The requested implementation and installation are complete. The latest user request is to preserve this handoff for another coding agent; there is no new feature request pending. The preceding request was to install the verified build into the actual playthrough modpack and identify the music and datapack locations. Installed artifacts were rechecked during this handoff and all three match `dist/manifest.json`.

Baseline commit: `213ea97` (`Fade returning background music and refresh battle tracks`). Earlier implementation commits are `9b0fb3e`, `b55bcfe`, and `61d3f25`. The working tree was clean before creating this document. This handoff is an additional file; no runtime changes or new tests were made in this handoff session.

## Read these first

- `README.md`: current product behavior, dependencies, build/install workflow, audio pipeline, compatibility, and limitations. It incorporates the user's later corrections to the original architecture proposal.
- `docs/AUTHORING.md`: rule semantics, priorities, trainer identity/champion handling, persistent encounter API/commands, and reload behavior.
- `docs/VERIFICATION.md`: evidence boundaries, live test details, timing interpretation, and remaining playtest cases.
- `reports/installation.json` and `dist/manifest.json`: installed target, backups, and exact artifact hashes.
- `reports/live-client.json`, `reports/dedicated-server.json`, `reports/unit-tests.json`, `reports/coverage.json`, and `reports/loudness.json`: recorded validation results, not promises of exhaustive testing.

Refer to those files and the commits instead of reconstructing the design from the original chat. In particular, the original plan's entrance restart, 30-tick default transition, native trainer aliases, and untrimmed Victini assignment were superseded.

## User intent to preserve

The user cares about responsive, audible transitions, especially Brock/Arceus switches, and equal perceived track volume referenced to Agatha's **battle theme**, not her room BGM. The current implementation and mastering are described in the README. Preserve the latest behavior when making further changes:

- Trainer entrance interruptions are opt-in via Mod Menu, off by default; Champions remain exempt even when enabled. This preference does not disable the configured trainer base tracks.
- Only opposing active Pokémon can trigger entrances. Re-entering Pokémon resume their retained music position; a special lead begins immediately at full gain.
- Trainer endings let battle music trail off, hold background BGM until the tail/gap completes, then fade that BGM in.
- Victini uses the author's trimmed `Legendary/Victini.mp3`, with the untrimmed recording retained only as an alternative.

The user prefers completion without repeated permission questions. Existing authorization covers local implementation, isolated verification, and installation into this playthrough. Close Minecraft before changing installed artifacts; do not terminate a user's game. Keep the project private and keep imported audio, dependencies, generated packs, and game data out of Git.

## Locations and installation state

Paths below are relative to this repository unless marked otherwise. `..` is the actual COBBLEVERSE Modrinth profile, not an isolated test profile.

| Purpose | Location |
| --- | --- |
| Installed mod | `../mods/battlemusic-0.1.0.jar` |
| Installed audio pack | `../resourcepacks/battlemusic-audio-0.1.0.zip` |
| Playthrough music rules | `../saves/New World/datapacks/battlemusic-defaults-0.1.0.zip` |
| Editable default rules | `datapack/data/battlemusic/battlemusic/rules/` |
| Assignment generator | `scripts/catalog.py` |
| Track manifest and import receipts | `catalog/music.json`, `catalog/imports.json` |
| Canonical original tracks | `%USERPROFILE%/Desktop/MusicMod/` |
| Ignored copied source audio | `audio-src/` |
| Generated deliverables | `dist/` |
| Latest installation backup | `../battle-music-backups/20261010-040920/` |
| Isolated client and server directories | `integration/game/`, `integration/server/` |

The actual save is `New World`; another dated `New World` directory also exists and was not the installation target. Audio pack activation was verified in the actual profile's `options.txt`; Music volume was 0.65 and the installer preserves the user's preference. The actual `config/battlemusic-client.json` was absent when checked during handoff, so trainer interruptions use the default-off setting.

The defaults ZIP is installed. Its name was not yet present in the save's persisted enabled datapack list when inspected after installation, and it was not disabled. On the next actual world load, confirm `/datapack list`; enable `file/battlemusic-defaults-0.1.0.zip` if necessary, as documented in the README. Do not claim an interactive restart of the user's actual playthrough has been performed.

## Navigation for future fixes

- Server timing/selection: `src/main/java/dev/battlemusic/server/Sessions.java`, `ContextFactory.java`, `RctAdapter.java`, and common mixins `InitialMusicMixin`, `PlayerActorMixin`, `SwitchMusicMixin`, `BattleTickMixin`.
- Rule selection and network decisions: `src/main/java/dev/battlemusic/rules/` and `net/`.
- Retained channels and background transitions: `src/client/java/dev/battlemusic/client/Playback.java`, `ManagedSound.java`, `Envelope.java`, and client sound/background mixins.
- Client preference/UI: `ClientSettings.java`, `MusicModMenu.java` in that client directory.
- Audio import/mastering: `scripts/catalog.py`, `audio.py`, `loudness.py`; edit generator assignments rather than generated default rules.
- Automated verification: `src/test/java/dev/battlemusic/Verification.java`; actual engine/battle probes: `src/integration/java/dev/battlemusic/fixture/`.

## Validation and safe continuation

Existing receipts report 323 automated checks, complete audited special-species/form coverage, successful isolated client checks, and dedicated initialization without client classes. Read the receipts and verification document for the exact scope. The isolated client harness intentionally terminates its owned JVM after a successful fixture; `exit_code: 1` alongside `success: true` and `terminated_by_harness: true` is not a failed verification run.

No rebuild or live re-test was needed to produce this document. For a future code change, start with `git status`, inspect the relevant source and receipts, and use the documented checks appropriate to that change. A typical sequence from the repository root is:

```powershell
$env:BATTLEMUSIC_PYTHON = Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe'
./build.ps1 -SkipAudio
& $env:BATTLEMUSIC_PYTHON scripts/audit.py
# When runtime code or deliverable assets change:
& $env:BATTLEMUSIC_PYTHON scripts/server_smoke.py
& $env:BATTLEMUSIC_PYTHON scripts/live.py
# Only after exact-artifact live verification, with Minecraft closed:
& $env:BATTLEMUSIC_PYTHON scripts/install.py --world 'New World'
```

Use a full build without `-SkipAudio` for audio changes; see README for dependency preparation. This is an offline Java/intermediary build against installed game/mod dependencies, not a conventional Gradle/Loom project. Python tool dependencies live in ignored `.tools/`; Java defaults to the installed JDK 21. Launch test games only through the isolated harness, never against real save chunks/playerdata. Never install `integration/battlemusic-verification.jar` into the real profile.

The installer backs up changes and checks that the live receipt matches all three artifacts. Its Windows process check may need sandbox escalation; default sandbox execution previously denied that query. Do not bypass its running-game or exact-build checks.

If continuing quality verification without a new user bug report, prioritize the remaining cases listed at the end of `docs/VERIFICATION.md`, and subjective listening to the transitions. The fixture demonstrates source/decoder retention and positive gain; it does not establish artistic smoothness. It also clears boss EVs in test memory to work around supplied RCT team cloning errors; production teams/datapacks were not changed.

## Suggested skills

- **handoff** from `https://github.com/mattpocock/skills`: use again when updating this document or ending another session. Invoke the environment's Skill tool if exposed; this session fetched the instructions using `npx skills use "https://github.com/mattpocock/skills" --skill "handoff"`.
- **computer-use:computer-use**: read its `SKILL.md` before any interactive game/client UI work if supported by the next agent's environment. The automated local verification harness does not require this skill.
- No additional skill is required for ordinary Java/Python maintenance of this repository. Use only skills relevant to the next user request.

The fetched handoff skill normally requests the OS temporary directory. The user explicitly requested storage in this project, so this file intentionally follows that location override. The initial sandboxed fetch failed with npm `ENOTFOUND`; the same requested command succeeded with approved network access. Account names, credentials, and launcher tokens are intentionally omitted from this handoff; resolve machine paths through environment variables or the repository location.
