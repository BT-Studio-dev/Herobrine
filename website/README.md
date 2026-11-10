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
  assets/                             logo, two backgrounds, the music track
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

Two things are loaded lazily on purpose. The music is a ten minute, five megabyte track marked
`preload="none"`, so it is not fetched until it is switched on — and it is never resumed
automatically, because browsers refuse playback without a gesture and nobody wants five megabytes
on page load. The stored intent to play is dropped on every load for the same reason, so the
buttons can never claim to be playing a track nobody started. The interface sounds are synthesised
with WebAudio, so a click has no file behind it.

Because the track arrives slowly, the transport shows every state the audio element reports: the
position and length, how much of the file has buffered, "fetching" while the first bytes come down,
and a distinct message for each way playback can fail. If it fails the button falls back to stopped
rather than sitting there pressed.

## Publishing

The site is static, so GitHub Pages can serve it from the `website` directory. See the repository
workflows for how it is deployed.
