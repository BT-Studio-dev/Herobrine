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
  check(`${key}: download link points at the track`,
        panel.querySelector('a[download]')?.getAttribute("href") === audio?.getAttribute("src"));
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
