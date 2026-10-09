# Verification scope

The receipts in `reports/` identify the exact mod JAR SHA-256. The isolated client runs the installed mod stack, including Rejuvenation, Infinite Music, Music Notification 3.0.0, and Sound Physics Remastered 1.5.1, with the enabled resource-pack stack and Music volume 0.65. Its fixture is a separate development JAR and is never distributed or installed into the real profile.

| Check | Method |
| --- | --- |
| Nine generation themes, form generation precedence, gen7b/gen8a | Automated matcher tests |
| Every explicit special assignment, Galarian birds, paradox trios | Matcher tests and installed species/form coverage audit |
| Encounter UUID/key, biome tags, box and combined conditions | Automated matcher tests |
| Rule validation, immutable snapshots, null overrides | Automated matcher tests |
| Doubles priority, sticky incumbent, stable slot ordering | Automated selection tests |
| Wild generation and special theme, own special Pokémon ignored | Actual Cobblemon battles in isolated client |
| Kanto trainer identity and existing Rejuvenation integration | Actual RCT Brock, Blue and each Elite Four member in isolated client |
| Individual Elite Four tracks, including two original Ogg inputs | Each member's actual battle plays its own event with positive OpenAL gain |
| Mod Menu settings, default-off policy and persistence | Construct actual Cloth Config screen; default-off Arceus encounter; save/reload preference |
| Mid-battle toggle and Champion exception | Toggle off/on with active Arceus; Champion Blue retains theme with Arceus active and toggle on |
| Entrance switch, pause, switch-out, repeated entry, faint and resume | Actual battle actions; OpenAL source and decoder identity checks |
| Entrance theme retention and immediate leading theme | Fixed entrance stream offset while benched, same sound/source/decoder on re-entry; leading Arceus has full gain in initial revision |
| Recall/send-out timing | Test-only hooks timestamp committed recall and native send-out packets; audible gain assertions within 600 ms |
| Battle outro before background resumes | Independent quiet BGM source remains paused through fade and quiet gap, including forced Cobblemon and global engine resume attempts |
| Consistent levels and leading silence | Measure every finished Vorbis file; `reports/loudness.json` records LUFS, true peak and leading trim |
| Rapid fades and missing entrance recovery | Playback controller exercised in the live sound engine |
| Retained voice capacity | Twenty-four rapid retargets retain thirteen paused entrance streams plus base, avoiding the small reserved streaming-source pool |
| Stale decisions and battle-end cleanup | Live packet/controller checks |
| Music Notification metadata | Generated `assets/musicnotification/musics.json`, loaded by the installed mod |
| Dedicated environment without RCT, client-class isolation | Official server JAR with Fabric `--initSettings`; JVM class-loading log |

OpenAL's `AL_SEC_OFFSET` is relative to the current streaming buffer queue, so it can decrease when processed buffers are removed. The live check verifies that this offset remains fixed while paused, and that the same sound instance, channel manager, OpenAL source and decoder stream resume with positive gain. It does not treat a queue-relative offset as an absolute song timestamp.

The latest successful isolated run measured audible trainer resumption 302 ms after the committed Arceus recall began, and audible Arceus re-entry 323 ms after the native send-out packet. These include the four-tick crossfade and asynchronous test sampling; they are measurements of this local run, not a multiplayer latency guarantee. All 56 finished tracks measured between −16.13 and −15.98 LUFS. Lance measured −16.02 LUFS and Agatha −15.98 LUFS after normalization.

The isolated fixture replaces two lead Pokémon to make switching/fainting deterministic and clears team EVs in memory: a supplied boss team otherwise fails RCT's Cobblemon cloning path with `EVs cannot exceed a total of 510`. The music checks exercise live RCT identities, battle creation, networking and audio; they do not validate the supplied teams' competitive legality or balance. Neither the mod nor the installer changes trainer teams or their datapack files.

The automated run does not substitute for listening to the artistic quality of transitions or checking whole-track loop seams. Full multiplayer with separate machines, complete doubles battles, capture/flee-specific animations, spectator/raid sessions, physical audio-device replacement and an interactive resource-pack reload are not covered by this fixture. They share the implemented selection, lifecycle and recovery paths, but should be included in a longer playtest. Dedicated verification initializes the server environment; it does not start a world or test remote joins.
