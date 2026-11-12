import { JSDOM } from "jsdom";
import fs from "node:fs";

// Run from the repository root: node website/tests/music-players.test.mjs
// Needs jsdom: npm i jsdom
const root = process.argv[2] || new URL("../", import.meta.url).pathname.replace(/\/$/, "");
const html = fs.readFileSync(root + "/index.html", "utf8");
const dom = new JSDOM(html, { runScripts: "outside-only", url: "https://example.test/" });
const { window } = dom;
const d = window.document;

let pass = 0, fail = 0;
const check = (name, cond) => { cond ? (pass++, console.log("  ok   " + name))
                                     : (fail++, console.log("  FAIL " + name)); };

console.log("music players");
const panels = [...d.querySelectorAll("[data-player]")];
check("three players on the page", panels.length === 3);

for (const panel of panels) {
  const key = panel.dataset.player;
  const audio = panel.querySelector("audio");
  check(`${key}: has an audio element`, !!audio);
  check(`${key}: src set`, !!audio?.getAttribute("src"));
  const file = root + "/" + (audio?.getAttribute("src") || "").replace("./", "");
  check(`${key}: file exists (${(audio?.getAttribute("src")||"").split("/").pop()})`, fs.existsSync(file));
  check(`${key}: preload="none" so it is not fetched up front`, audio?.getAttribute("preload") === "none");
  check(`${key}: loops`, audio?.hasAttribute("loop"));
  const play = panel.querySelector('[data-set][data-value="on"]');
  const stop = panel.querySelector('[data-set][data-value="off"]');
  check(`${key}: play button keyed to the player`, play?.dataset.set === key);
  check(`${key}: stop button keyed to the player`, stop?.dataset.set === key);
  check(`${key}: stop starts pressed`, stop?.getAttribute("aria-pressed") === "true");
  check(`${key}: has a status line`, !!panel.querySelector("[data-status]"));
  check(`${key}: has a seek slider`, !!panel.querySelector("[data-seek]"));
  check(`${key}: has a volume slider`, !!panel.querySelector("[data-volume]"));
  // The download button and the "Ogg Vorbis, ..." file row were removed from the
  // foot of every card; the track is still played from the file, just not offered
  // as one. Assert the row is gone rather than merely absent from this card.
  check(`${key}: no download link left in the card`, !panel.querySelector("a[download]"));
  check(`${key}: no file row left in the card`, !panel.querySelector(".foot"));
  check(`${key}: the audio source itself is untouched`, !!audio?.getAttribute("src"));
}

console.log("page-wide");
check("no download link anywhere on the page", d.querySelectorAll("a[download]").length === 0);
check("no .foot row anywhere on the page", d.querySelectorAll(".foot").length === 0);

console.log("backgrounds");
{
  const html = fs.readFileSync(root + "/index.html", "utf8");
  const js = fs.readFileSync(root + "/showcase.js", "utf8");
  const css = fs.readFileSync(root + "/showcase.css", "utf8");

  // The three places a background has to agree: the <option>, the map in the
  // script that sets --bg-image, and the stylesheet rule that works without
  // scripting. Miss one and the choice silently does nothing.
  const options = [...html.matchAll(/<option value="([^"]+)"[^>]*>([^<]*)</g)].map((m) => m[1]);
  const block = js.match(/const BACKGROUNDS = \{([\s\S]*?)\};/);
  const jsKeys = [...block[1].matchAll(/^\s*(\w+):/gm)].map((m) => m[1]);
  const cssKeys = [...css.matchAll(/--bg-image:url\("([^"]+)"\)/g)].map((m) => m[1]);
  const cssAttrs = [...css.matchAll(/html\[data-bg="(\w+)"\]\{--bg-image/g)].map((m) => m[1]);
  // Stop at the comma: the veiled backgrounds are written as one selector list,
  // and a pattern that runs to the brace would only ever capture the first.
  const veilAttrs = [...css.matchAll(/html\[data-bg="(\w+)"\][^{,]*#bg::after/g)].map((m) => m[1]);

  check("every dropdown option has a script entry",
        options.every((o) => o === "none" || jsKeys.includes(o)));
  check("every script entry is offered in the dropdown",
        jsKeys.every((k) => k === "none" || options.includes(k)));
  check("every background has a stylesheet rule",
        jsKeys.filter((k) => k !== "none").every((k) => cssAttrs.includes(k)));
  check("every background is veiled, so text stays legible over it",
        jsKeys.filter((k) => k !== "none").every((k) => veilAttrs.includes(k)));

  cssKeys.forEach((rel) => {
    const file = root + "/assets/" + rel.replace(/^assets\//, "");
    check(`background file exists: ${rel.replace(/^assets\//, "")}`, fs.existsSync(file));
  });
}

console.log("seed");
const seed = d.querySelector("[data-seed-value]")?.textContent.trim();
check("seed shown", seed === "10528182");
const javaHash = (s) => { let h = 0; for (const c of s) h = (31 * h + c.charCodeAt(0)) & 0xFFFFFFFF;
                          return h >= 0x80000000 ? h - 0x100000000 : h; };
check("seed equals \"Herobrine\".hashCode()", String(javaHash("Herobrine")) === seed);
check("hashCode impl validated against hello=99162322", javaHash("hello") === 99162322);

console.log("versions");
const body = d.body.textContent;
check("mentions both 26.3 and 1.21.11", body.includes("26.3") && body.includes("1.21.11"));
check("no longer claims it will refuse to load on 1.21.11", !body.includes("will refuse to load on 1.21.11"));

console.log(`\n${pass} passed, ${fail} failed`);
process.exit(fail ? 1 : 0);
