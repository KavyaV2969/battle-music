# Datapack authoring

Put rules at `data/<namespace>/battlemusic/rules/<name>.json` inside a Minecraft 1.21.1 datapack (`pack_format: 48`). A rule ID is its namespace and relative file path. Higher-priority datapacks replace a file at the same resource path. Distinct rule IDs are merged.

```json
{
  "schema_version": 1,
  "kind": "entrance",
  "priority": 500,
  "match": {"battle_type": "trainer", "species": ["cobblemon:pikachu"]},
  "sound": "battlemusic:legendary.victini_alternative",
  "transition_ticks": 30
}
```

`kind` is `base` or `entrance`. Base chooses an encounter's underlying music. Entrance temporarily replaces that base while a matching opponent is active. The winning entrance must have a sound; a winning `sound: null` suppresses the entrance and prevents a lower-priority rule from supplying one. A base rule with `sound: null` retains the native event.

All supplied selector fields must match. Lists use **OR** within one field; omit a field for no restriction. Empty lists are invalid.

| Selector | Meaning |
| --- | --- |
| `battle_type` | `wild` or `trainer`; omitted matches both |
| `species` | Namespaced species IDs, e.g. `cobblemon:articuno` |
| `forms` | Exact Cobblemon form names, e.g. `Galar`, `Alola` |
| `aspects` | At least one listed aspect, e.g. `galarian` |
| `labels` | Species/form labels; generation labels use current form then species fallback |
| `trainers` | Full trainer identity, e.g. `rctmod:kanto_brock` |
| `pokemon_uuids` | Persistent Pokémon UUID, not the temporary entity UUID |
| `encounters` | Persistent encounter keys assigned through the command/API |
| `dimensions` | Dimension identifiers |
| `biomes` | Biome identifiers or `#namespace:biome_tag` |
| `box` | Inclusive `min` and `max` coordinate triples |

Location and trainer identity are captured at battle start. Forms/aspects and active status are re-evaluated as the battle progresses. Generation tags such as `gen7b` and `gen8a` normalize to `gen7` and `gen8`; special-category labels survive transformations. Form rules should usually use `aspects` rather than depending on display spelling.

The highest numeric priority wins. Equal-priority rules use ascending rule ID. When multiple active opponents have entrance themes, highest priority wins; the current Pokémon retains its theme on a tie, followed by stable battle slot order. A fainted, gone, allied or benched Pokémon cannot trigger entrance music.

Default priorities: generation **100**, special species **200**, Galarian birds/Kanto trainers **300**, existing explicit native music **400**, author overrides **500**, scripted encounter overrides **800**. A rule must exceed 400 to replace an existing explicit native `battleTheme`. Default trainer event pools are not explicit overrides. RCT's native `battleTheme` field remains supported.

Supported sounds are listed in `catalog/music.json`. Add more through a resource pack's `assets/<namespace>/sounds.json`:

```json
{"boss.custom":{"sounds":[{"name":"mypack:battle/custom_boss","stream":true}]}}
```

Place the Vorbis file at `assets/mypack/sounds/battle/custom_boss.ogg` and reference `mypack:boss.custom` from your rule. Use stereo for music. No recompilation is needed. The server cannot validate client assets; missing entrance audio retains the base, and missing base audio tries the original event.

## Individual scripted encounters

Select a loaded Pokémon entity with a Minecraft entity selector:

```mcfunction
battlemusic encounter @e[type=cobblemon:pokemon,sort=nearest,limit=1] set mypack:cave_guardian
battlemusic encounter @e[type=cobblemon:pokemon,sort=nearest,limit=1] clear
battlemusic explain
```

Commands require operator level 2. `encounter` keys are saved on the Pokémon and are matched by the rule's `encounters` selector. Set a key before starting battle; active sessions retain their current base choice. These commands do not change the Pokémon's trainer or species data.

Java integrations can call `BattleMusicApi.setEncounterKey(pokemon, "mypack:cave_guardian")` on the server thread; pass null to clear it. Register trainer adapters during common initialization with `BattleMusicApi.registerTrainerAdapter(actor -> ...)`. Return a namespaced ID or null for unsupported actors. Adapters run in registration order, first non-null wins; built-in adapters cover RCT and native Cobblemon NPCs. Adapter identities are captured once per battle.

## Reloads and diagnostics

Use `/reload` after editing rules. Parsing validates the entire new set before publishing it. A malformed rule retains the last good set and logs the failing resource ID. Existing battles retain their rule snapshot. `/battlemusic explain` reports your current snapshot revision, base rule, selected sounds and active entrance Pokémon. New battles use the most recently accepted rules.

Examples are standalone rule documents; copy only the examples you want into your own datapack. They are not installed by the build.
