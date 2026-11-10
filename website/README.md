# Herobrine: Haunting — website

A small, static site with no build step and no dependencies. Three pages:

| Path | Page |
| --- | --- |
| `/` (`index.html`) | The showcase: what the mod is, the versions it needs, the music, a seed |
| `/docs/source-and-safety/` | Source and safety guide, with the commit hash to verify against |
| `/docs/how-herobrine-is-built/` | How the mod is put together and released |

## Previewing

From the repository root:

```sh
python3 -m http.server 8000 --directory website
```

Then open <http://localhost:8000/>.

## Files

```
website/
  index.html                          showcase page
  showcase.css                        showcase styles (themes, glass, text scale)
  showcase.js                         showcase controls
  styles.css                          shared styles for the two guide pages
  how-its-built.css                   extra styles for the build guide
  app.js                              commit-hash copy button for the source guide
  manus-routes.json                   route manifest
  assets/                             logo, two backgrounds, both music tracks
  docs/source-and-safety/index.html
  docs/how-herobrine-is-built/index.html
```

## How the display controls work

Every setting is a data attribute on `<html>`, and the stylesheet reacts to it. Nothing writes
inline styles except the background image.

| Attribute | Values | What it does |
| --- | --- | --- |
| `data-theme` | `dark` \| `light` | The two colour schemes. First visit follows `prefers-color-scheme`. |
| `data-glass` | `on` \| `off` | Frosted translucent panels, or flat solid ones. |
| `data-bg` | `shader` \| `snow` \| `none` | Background image. |
| `data-text` | `s` \| `m` \| `l` | One scale multiplied into the root font size. |

Choices are kept in `localStorage` under `herobrine-site-prefs`.

Three things are loaded lazily on purpose. Both music tracks — his, ten minutes and five
megabytes; hers, two and a half minutes and half a megabyte — are marked `preload="none"`, so
neither is fetched until its own play button is pressed, and a visitor who came for one is not sent
the other. Neither is ever resumed automatically, because browsers refuse playback without a
gesture and nobody wants five megabytes on page load. The stored intent to play is dropped on every
load for the same reason, so the buttons can never claim to be playing a track nobody started. The
interface sounds are synthesised with WebAudio, so a click has no file behind it.

Because the tracks arrive slowly, each transport shows every state the audio element reports: the
position and length, how much of the file has buffered, "fetching" while the first bytes come down,
and a distinct message for each way playback can fail. If it fails the button falls back to stopped
rather than sitting there pressed. The two transports are built by one factory from a `data-player`
attribute, so they cannot drift apart.

## The atmosphere

Two effects are about the mod rather than about the mod's website, and both switch themselves off
under `prefers-reduced-motion`:

- **The fog** thickens as you scroll down the page, driven by scroll position on one `rAF`-free
  listener. With reduced motion it sits at a third strength instead of following the scroll.
- **The cursor light** puts a highlight under the pointer inside the glass panels. One
  `pointermove` listener feeds a single `requestAnimationFrame` that writes the cursor position
  into each panel in that panel's own coordinates, so the highlight lands under the pointer rather
  than under the page.

The default background is plain; the mod's page already carries its own gradients, and the two
images are there for anyone who wants them.

## Publishing

The site is static, so GitHub Pages can serve it from the `website` directory. See the repository
workflows for how it is deployed.
