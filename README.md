![Herobrine beneath the moon in a snowy landscape](gallery/herobrine-snow-one-moon.png)

# Herobrine: Haunting

Herobrine for Fabric on Minecraft 26.3: staged hauntings, seven apparitions, the fog closing in, 13 original sounds,
gold shrines, churches, holy water and the cross. Non-destructive by design.

## Summary

Herobrine: Haunting brings staged, escalating hauntings to Fabric on Minecraft 26.3 — seven apparitions, 13 original
sounds, gold shrines, churches, holy water and the cross. Non-destructive by design.

## Purpose

Herobrine: Haunting is a Fabric mod for Minecraft 26.3 that adds a staged, escalating haunting system centered around
Herobrine. It is non-destructive by design: the haunting alters the world procedurally (torches placed, leaves stripped,
tunnels dug, pyramids built) but never touches player-built structures, with the single optional exception of fire,
which is behind its own switch.

## Features

- **Staged hauntings** — the haunting escalates every two in-game days across four stages. Events roll more frequently,
  apparitions appear closer, and from stage 3 he sometimes hunts instead of watches.
- **Seven apparitions** — Watch, Runner, Lurking, Dwelling, Scare, Jumpscare, Spike — ranging from a distant watcher to
  a figure in your face, with a 1-in-12 chance of starting a chase frenzy.
- **Chase mode** — a few minutes where apparitions come three times as often and he refuses to leave you alone.
- **13 original sounds** with in-game subtitles (whispers, footsteps, heartbeat, mining, glitches, fog, the haunting
  track, and more).
- **World events** — torches placed, lights snuffed or swapped, signs with one line, doors opening themselves, sand
  pyramids (including out in water), gravel mounds, gold-and-redstone markers, mined trees, haunted animals, ghost
  miner pickaxe sounds, window sightings.
- **Churches** spawn in the world with a holy-water cauldron on the altar; bottle holy water with an empty bottle.
- **Gold shrine** — eight gold blocks and four redstone torches, lit with flint and steel.
- **Items** — Iron Stick, Holy Water, The Cross — the cross banishes him when it holds.
- **Notch Apple ritual** — burn a Notch Apple on netherrack to banish him to the void (or call him back).
- **Steve friend** — summon Steve to travel with you; set him to stay or follow.
- **Purist mode** (True Herobrine rules) — he never edits a block, never sends chat, never attacks — he only ever
  appears above Y 60 in the Overworld and watches.
- **Banish toggle** — send him to the void until you call him back.
- **Settings screen** (H key or `/herobrine_settings`) saved per world: haunted herd, ghost miner, fire/arson, noises,
  music, purist, banished, music volume.
- **Commands** — `/herobrine summon`, `haunt`, `apparition`, `mode`, `schizo`, `shrine`, `give`, `config`, `phantom`,
  and more.
- **No sleep** while he stands nearby.

The settings screen is also available through Mod Menu. Its Haunting and Audio & World tabs group
the existing controls, including a music-volume slider. Changes apply immediately; Reset defaults
restores the current dimension's settings. Join a world before changing settings: commands require
cheats in singleplayer or operator permission on a server. In multiplayer, install the same mod
version on the server and check chat for confirmation; the screen cannot read server settings.
The world continues running while the settings screen is open.

## Compatibility

- Minecraft 26.3
- Fabric Loader >= 0.19.0
- Fabric API >= 0.165.0
- Java >= 25
- Suggests Mod Menu >= 21.0.0 for in-game settings integration
- Runs on both client and server; settings are saved with the world and apply per dimension

## License

CC-BY-NC-SA-4.0 — see [LICENSE](LICENSE).

## Source

https://github.com/BT-Studio-dev/Herobrine

## Commands

```
/herobrine summon <stalker|hostile|elite|inferno|watcher>
/herobrine haunt [player]
/herobrine apparition <watch|scare|jumpscare|spike|runner|lurking|dwelling> [player]
/herobrine leaves | tunnel | structure [water] | tree | cobble | warts
/herobrine gravel | marker | herd | miner | window
/herobrine torches | curse | candles | sign | donation | door | fire
/herobrine phantom [add|clear]
/herobrine mode [full|purist|toggle]
/herobrine schizo [on|off] | hauntstate | banish [on|off] | config [herd|miner|arson]
/herobrine shrine [gold] | church | give <iron_stick|holy_water|the_cross>
```

The fog closes in when he is close. His own sounds are in the subtitles.
