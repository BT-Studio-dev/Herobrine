# Herobrine: Haunting — website

A small, static site with no build step and no dependencies.

| Path | Page |
| --- | --- |
| `/` (`index.html`) | The showcase: what the mod is, the versions it needs, the music, a seed |

## Previewing

From the repository root:

```sh
python3 -m http.server 8000 --directory website
```

Then open <http://localhost:8000/>.

## Files

```
website/
  index.html         showcase page
  showcase.css       showcase styles (themes, glass, text scale)
  showcase.js        showcase controls
  styles.css         shared styles
  app.js             shared script
  manus-routes.json  route manifest
  assets/            logo, backgrounds, icon, music tracks
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

Music tracks are marked `preload="none"` so none is fetched until its play button is pressed and
nothing is resumed automatically. The interface sounds are synthesised with WebAudio.

## The atmosphere

The fog thickens as you scroll down the page and switches off under `prefers-reduced-motion`.
The default background is plain.

## Publishing

The site is static, so GitHub Pages can serve it from the `website` directory. See the repository
workflows for how it is deployed.
