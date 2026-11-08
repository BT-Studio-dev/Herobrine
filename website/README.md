# Herobrine: Haunting — Source & Build Guides

This is a small, static documentation site with two pages: the source and safety guide at `index.html`, and the separate [How it’s built page](docs/how-herobrine-is-built/). Shared styles, the guide’s commit-copy interaction, the route manifest, and local WebP artwork live alongside them.

To preview from the repository root, run:

```sh
python3 -m http.server 8000 --directory website
```

Then open `http://localhost:8000/`. The pages are static documentation; they do not run the optional `WAKE_UP.vbs` script or install the mod.
