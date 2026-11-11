# Shader packs for Minecraft 26.3

These are **separate Iris shader packs**, not part of the Herobrine mod JAR.
Put the chosen ZIP, unextracted, in your Minecraft instance's `shaderpacks`
folder, and select it in Iris. Choose one pack at a time.

| Pack | Status for Minecraft 26.3 |
| --- | --- |
| [Cursed Fog 1.0.10](https://modrinth.com/shader/cursed-fog/version/9yVXZsfs) by LoLip_p | The original ZIP matches the official Modrinth file, which lists 26.3. No rebuild needed. |
| [Sildur's Vibrant 2.02 Extreme](https://modrinth.com/shader/sildurs-vibrant-shaders/version/J4RlzpKF) by Sildur | The original ZIP matches the official Modrinth file, which lists 26.3. No rebuild needed. Extreme can be demanding. Sildur's pack is all rights reserved; follow the author's distribution terms. |
| [E-LITE 5.1.3](https://modrinth.com/shader/lite-shaders/version/ChU59bMQ) by Entokito, based on MakeUp Ultra Fast by Javier Garduño | The original ZIP matches the official Modrinth file, which lists 26.3. No rebuild needed. Its bundled README lists older tested versions, while the release notes explicitly mention 26.3. |
| [Optimism 1.0.2](https://modrinth.com/shader/optimism-shader/version/jXxqg4FV) by Nachtwind1 | The original ZIP matches Modrinth, but its official listing stops at 1.21.3. `optimism-26.3-test.zip` is an unofficial, experimental modification; see its README and MIT license. **Tested in-game on Minecraft 26.3: it loads and runs.** |

## Verification and building

Run `python3 shaders/check_packs.py` from the repository root to check the
original archives against the recorded Modrinth SHA-1 hashes, verify ZIP CRCs,
and ensure the Optimism test ZIP matches its source. These checks do **not**
verify in-game compatibility or performance.

To compile-check and rebuild the experimental Optimism ZIP, install glslang
(or set `GLSLANG` to the executable) and run:

```sh
python3 shaders/build_optimism_test.py
```

That script expands the included GLSL, checks and links the two Optimism
programs, and packages the test ZIP. The other packs are official prebuilt
ZIPs and are not modified or rebuilt by the script. Graphics-driver support
and FPS on a particular machine remain unverified.

## In-game results

`optimism-26.3-test.zip` was run on Minecraft 26.3 with Iris. It loads and
compiles without error, and it is **not** broken. Its behaviour is:

- **Daytime:** no visible effect. The shader's darkness term is zero, so
  colour passes through unchanged at any distance.
- **Night:** near-total blackout, by design. The terrain shader divides each
  colour channel by `distance^2.2 * darkness` and then applies `pow(x, 2.2)`;
  the sky shader divides by a flat 1000. Only about 2-3 blocks around a light
  source stay visible, and the sky and stars go black.

If you want atmosphere without the blackout, use Cursed Fog instead.
