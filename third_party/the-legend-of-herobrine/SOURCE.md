# The Legend of Herobrine — upstream source copy

This directory contains an unmodified source copy of
[The Legend of Herobrine](https://github.com/Alex-MacLean/TheLegendOfHerobrine) by Alexander
MacLean, at commit `9bd110b3c9d426956bd0cd5bbd44d16cbf270b56` (branch `Fabric`).

Only `src/main` is copied here — 469 files, 6.2 MB: the Java sources under
`com.herobrinemod.herobrine` (entities, blocks, items, mixins, savedata, worldgen) and the
matching `src/main/resources` under the `herobrine` namespace. The upstream `LICENSE` is here as
`LICENSE.md`, and the upstream readme as `README-upstream.md`.

## The licence is not the same as this project's

**The upstream files are LGPL-3.0. Herobrine: Haunting is CC BY-NC-SA 4.0.** Those two do not
combine: LGPLv3 §4 requires the combined work be conveyed under GPLv3, and the NonCommercial term
in CC BY-NC-SA is the kind of additional restriction GPLv3 §7 forbids.

So this copy is reference material. Nothing here has been merged into the mod's own sources, and
nothing here may be copied into `src/` without first settling the licence — either by relicensing
this project, or by keeping that code out of the work entirely.

If a feature from here is wanted, the clean route is to read it and write the equivalent
ourselves, in our own code and under our own licence. An idea is not the same thing as the
expression of it.

## Not part of the build

This directory is not a Gradle source root and is not processed by `processResources`. None of it
is compiled, none of it is packaged into either jar (26.3 or 1.21.11), and none of it ships. It is
here so the code can be read without fetching it again.
