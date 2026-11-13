# The Legend of Herobrine — upstream source, kept for reading

An unmodified copy of `src/main` from
[The Legend of Herobrine](https://github.com/Alex-MacLean/TheLegendOfHerobrine) by Alexander
MacLean, at commit `9bd110b3c9d426956bd0cd5bbd44d16cbf270b56` (branch `Fabric`). 471 files,
6.2 MB. Their `LICENSE` is `LICENSE.md` and their readme is `README-upstream.md`.

It is here so the code can be read. **It is not wired into the build and it cannot be dropped
into `src/` as it stands** — see the four reasons below, any one of which is enough.

## Why none of this can be copied into `src/` directly

**1. It would overwrite our own files.** Both projects use `mod_id = herobrine`, so their
resources land on exactly our paths. Seven files collide byte-for-byte in path:

| their file | what it would clobber |
| --- | --- |
| `fabric.mod.json` | our mod metadata |
| `herobrine.mixins.json` | our mixin configuration |
| `assets/herobrine/lang/en_us.json` | our entire English translation |
| `assets/herobrine/sounds.json` | our sound event definitions |
| `assets/herobrine/textures/entity/herobrine.png` | our Herobrine texture |
| `assets/herobrine/models/item/holy_water.json` | our holy water model |
| `data/minecraft/tags/item/swords.json` | our sword tag |

**2. The mappings are different.** Upstream is built on Yarn (`net.minecraft.sound`,
intermediary names in places); this project uses official Mojang mappings
(`net.minecraft.sounds`, real names). Every single import in all 131 Java files differs.

**3. The Minecraft versions are different.** Upstream targets 1.21.1 with Fabric API
`0.116.4+1.21.1` and Loader `0.16.14`. This project builds against 26.3 and 1.21.11.

**4. The licences do not combine.** Upstream is **LGPL-3.0**. This mod is **CC BY-NC-SA 4.0**.
LGPLv3 §4 requires the combined work be conveyed under GPLv3, and the NonCommercial term is the
sort of additional restriction GPLv3 §7 forbids. Merging their code into ours puts the project in
conflict with itself whatever `fabric.mod.json` says.

## What is in here

- **Entities** — `HerobrineEntity`, and the `Mage`, `Spy`, `Stalker`, `Warrior`, `Builder` and
  `FakeHerobrineMage` variants; `SurvivorEntity` with `SurvivorSkinRegistry` and `SurvivorTrades`;
  `HolyWaterEntity` / `UnholyWaterEntity`; and the full infected-mob set (armadillo through wolf).
- **Blocks** — `HerobrineAltarBlock`, `HerobrineStatueBlock`, `CursedDiamondBlock`.
- **Items** — holy and unholy water, cursed diamond axe/hoe/sword.
- **Mixins** — `CrashReportMixin`, `ServerWorldMixin`, `BlockMixin`, `LivingEntityMixin`,
  `PotionEntityMixin`, `VanillaBiomeParametersMixin`.
- **Savedata** — `Config`, `ConfigHandler`, `SaveDataHandler`, `WorldSaveData`.
- Plus `sounds/`, `worldgen/`, `component/`, `entities/goals/`, and 338 resource files under
  `assets/herobrine` and `data/herobrine`.

## The route that does work

Pick the feature, not the file. Something in here worth having can be written again from the
idea, in our own code, against our mappings, under our licence — which is what `HerobrineEyesLayer`
and the fog already are relative to From The Fog. An idea is not the same thing as the expression
of it, and a reimplementation carries none of the four problems above.
