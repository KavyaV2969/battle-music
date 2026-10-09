# Cobblemon Battle Music

Private Fabric mod for Minecraft **1.21.1**, **Java 21**, **Cobblemon 1.7.3**, and Fabric API **0.116.14+1.21.1**. Optional RCT support targets **rctapi 0.15.2-beta**. Code compiles directly against the installed intermediary jars; no remapping step or downloaded game dependencies are required.

The server selects encounter music from datapack rules. Clients stream the audio resource pack. An opposing special Pokémon can temporarily interrupt a trainer's music; the original sound/channel is paused and resumes at its retained position after a switch or faint. Your own Pokémon do not trigger entrance themes. PvP, spectators, and raids retain their existing playback.

## Artifacts and installation

Build output is in `dist/`:

| File | Placement |
| --- | --- |
| `battlemusic-0.1.0.jar` | Server and each client's `mods/` |
| `battlemusic-defaults-0.1.0.zip` | Server/world `datapacks/` |
| `battlemusic-audio-0.1.0.zip` | Each client's `resourcepacks/`; enable it |

Restart Minecraft after changing mods. Run `/reload` after adding or changing rules. Use `/datapack list` to confirm the defaults are enabled. If necessary enable `file/battlemusic-defaults-0.1.0.zip`. Keep the existing Original Pokémon Battle Music and COBBLEVERSE Soundtrack packs enabled: the Kanto trainer aliases reference their audio paths.

Music uses Minecraft's **Music** slider. This source profile had that slider at **0**; raise it to hear battle themes. The installer preserves your volume preference unless given `--music-volume 0.65`.

`compat/sound_rates.properties` contains exclusions for **Sound Physics Remastered 1.5.1**. Merge those entries into `config/sound_physics_remastered/sound_rates.properties`. A value of **0 bypasses physics processing**, rather than muting the sound. Do not replace the rest of the configuration. These entries use the format of the installed version, not the older `allowed_sounds.properties` format.

The optional installer backs up files it changes, adds the audio pack to the enabled pack list, and merges these exclusions:

```powershell
& $env:BATTLEMUSIC_PYTHON scripts/install.py --world "New World"
```

Run it with Minecraft closed. By default the target profile is this repository's parent; pass `--profile` for another installation. A dedicated server needs the JAR and defaults pack, while clients need the JAR and audio pack. Missing-mod clients keep Cobblemon's native playback because the server checks payload support before applying its choices.

The local installation receipt is `reports/installation.json`; it lists changed files and the backup directory. Backups preserve the previous options and Sound Physics configuration. To remove the mod later, disable its defaults datapack, close Minecraft, remove its three installed artifacts, and restore those backed-up settings if desired.

## Building

Set `BATTLEMUSIC_PYTHON` to Python 3.12+ and `JAVA_HOME` to a Java 21 JDK if needed. `build.ps1` also discovers the bundled Codex Python runtime on this machine.

```powershell
& $env:BATTLEMUSIC_PYTHON -m pip install --target .tools imageio-ffmpeg==0.6.0 mutagen==1.48.1 jsonschema==4.26.0
& $env:BATTLEMUSIC_PYTHON scripts/prepare.py
& $env:BATTLEMUSIC_PYTHON scripts/catalog.py
./build.ps1
& $env:BATTLEMUSIC_PYTHON scripts/audit.py
```

`build.ps1 -SkipAudio` recompiles code and runs tests without re-encoding audio. A full build reuses unchanged encoded files by source SHA-256. `scripts/catalog.py` generates `catalog/music.json` and the default datapack from the explicit assignments in that script; edit the generator, not generated rules. Original music is copied into ignored `audio-src/`; the initial source locations are Desktop/MusicMod and the supplied Rejuvenation BGM folder. Imports include Celebi/Aquatic, Gen7 Specials/Cyanide, Gen8 Specials/Popstar, and Victini/Undertale. The old Victini recording remains an unassigned alternative.

No Minecraft jars, third-party mods, original MP3s, converted audio, or launcher credentials enter Git. The generated audio pack is private and is not intended for public distribution.

## Rules and authoring

See [docs/AUTHORING.md](docs/AUTHORING.md), [schemas/rule.schema.json](schemas/rule.schema.json), and [examples](examples/). Server choices are authoritative; a datapack cannot supply audio bytes. Add new sound events and Ogg files to a client resource pack and reference their identifiers from a server rule. Events do not need hard-coded registry entries: the client creates resource-backed sound events by identifier.

Default rules assign Gen 1–9 wild themes, special species and forms, and 13 Kanto league trainers. Kanto gyms use `gym_battle_johto`, Elite Four use `hgss/trainer_kanto`, and Blue uses `champion_battle`. Existing resource packs choose the arrangement at those paths. Trainer team/AI/field data is not changed.

## Architecture and verification

`rules/` is an immutable, game-independent matcher. Server sessions capture location and trainer identity at battle start, retain their rule snapshot across reloads, and observe active slots after battle dispatch. The bridge sends a bounded versioned music state immediately before Cobblemon's normal music packet. Client code owns only managed sessions, crossfades transient entrance voices, and retains the original streamed base channel.

Transitions default to 30 ticks, with a 60-tick battle-end fade. A later entrance restarts its theme. Base position is preserved while its audio channel exists; a resource/audio-engine reload rebuilds playback from the latest selection. Whole recordings loop from the beginning and have not been edited into seamless musical loops. Muting the Music category allows Minecraft to discard audio channels; restoring volume restarts available playback if needed.

`reports/unit-tests.json` records rule, envelope, wire-format and installed-ABI checks. `reports/coverage.json` audits installed special species/forms. The test-only fixture JAR is excluded from distribution. Run `scripts/live.py` for an anonymous isolated client using cached game libraries and the enabled profile's full resource-pack/mod stack. It copies configuration and `level.dat`, creates fresh chunks, and enables music in the isolated copy. The original world's region and playerdata directories are not copied. The live receipt records actual OpenAL pause/resume measurements and the built JAR hash. Logs and run directories are ignored by Git.

All reports describe the checks actually performed; they are not a claim of exhaustive testing of every third-party mod or battle format.

See [docs/VERIFICATION.md](docs/VERIFICATION.md) for the test matrix and remaining playtest cases. The dedicated smoke test uses `--initSettings` to verify common initialization and client-class isolation without starting a server world.
