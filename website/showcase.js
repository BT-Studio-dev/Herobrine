/*
 * Showcase site controls.
 *
 * Every setting is stored on <html> as a data attribute and mirrored into localStorage, so the
 * stylesheet does the work and the choice survives a reload.
 *
 * Two things are deliberately lazy. The music is a ten minute, five megabyte track, so it is never
 * fetched until it is switched on - the audio element is marked preload="none" for exactly that
 * reason. The interface sounds are synthesised with WebAudio rather than shipped as files, because
 * a click does not need a download behind it.
 */
(() => {
  const root = document.documentElement;
  const STORE = "herobrine-site-prefs";

  const BACKGROUNDS = {
    none: "none",
    shader: 'url("assets/herobrine-shader-background.webp")',
    snow: 'url("assets/herobrine-snow-one-moon.webp")'
  };

  const defaults = {
    theme: window.matchMedia("(prefers-color-scheme: light)").matches ? "light" : "dark",
    glass: "on",
    bg: "shader",
    sounds: "off",
    music: "off",
    text: "m",
    volume: 0.5
  };

  let prefs;

  try {
    prefs = Object.assign({}, defaults, JSON.parse(localStorage.getItem(STORE) || "{}"));
  } catch {
    prefs = Object.assign({}, defaults);
  }

  const save = () => {
    try {
      localStorage.setItem(STORE, JSON.stringify(prefs));
    } catch {
      /* Private browsing, or storage turned off: the controls still work for this visit. */
    }
  };

  const status = document.querySelector("[data-status]");
  const say = (message) => {
    if (status) status.textContent = message || "";
  };

  /* ---- Interface sounds -------------------------------------------------- */

  let audioContext;

  function blip() {
    if (prefs.sounds !== "on") return;

    try {
      const Ctx = window.AudioContext || window.webkitAudioContext;
      if (!Ctx) return;
      audioContext = audioContext || new Ctx();
      if (audioContext.state === "suspended") audioContext.resume();

      const osc = audioContext.createOscillator();
      const gain = audioContext.createGain();
      const now = audioContext.currentTime;

      osc.type = "sine";
      osc.frequency.setValueAtTime(540, now);
      gain.gain.setValueAtTime(0.0001, now);
      gain.gain.exponentialRampToValueAtTime(0.05, now + 0.012);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.17);

      osc.connect(gain).connect(audioContext.destination);
      osc.start(now);
      osc.stop(now + 0.19);
    } catch {
      /* No audio device, or WebAudio blocked. Silence is an acceptable outcome. */
    }
  }

  /* ---- Music ------------------------------------------------------------- */

  const track = document.getElementById("track");

  function applyVolume() {
    if (track) track.volume = Number(prefs.volume);
  }

  function syncMusic() {
    if (!track) return;

    if (prefs.music === "on") {
      applyVolume();
      track.play().then(
        () => say("Playing the ten minute haunting track."),
        () => say("Your browser blocked playback. Press play again to allow it.")
      );
    } else {
      track.pause();
      say("");
    }
  }

  /* ---- Applying and reflecting state ------------------------------------- */

  function apply() {
    root.dataset.theme = prefs.theme;
    root.dataset.glass = prefs.glass;
    root.dataset.bg = prefs.bg;
    root.dataset.text = prefs.text;
    root.style.setProperty("--bg-image", BACKGROUNDS[prefs.bg] || "none");
  }

  function reflect() {
    document.querySelectorAll("[data-set]").forEach((button) => {
      const on = prefs[button.dataset.set] === button.dataset.value;
      button.setAttribute("aria-pressed", String(on));
    });

    const select = document.querySelector("[data-bg-select]");
    if (select) select.value = prefs.bg;

    // There are two volume sliders - one in the panel, one beside the transport controls - and
    // both have to follow whatever the current value is.
    document.querySelectorAll("[data-volume]").forEach((slider) => {
      slider.value = String(prefs.volume);
    });
  }

  function set(key, value) {
    prefs[key] = value;
    save();
    apply();
    reflect();
    blip();

    if (key === "volume") applyVolume();
    if (key === "music") syncMusic();
    if (key === "sounds" && value === "on") blip();
  }

  document.querySelectorAll("[data-set]").forEach((button) => {
    button.addEventListener("click", () => set(button.dataset.set, button.dataset.value));
  });

  const bgSelect = document.querySelector("[data-bg-select]");
  if (bgSelect) {
    bgSelect.addEventListener("change", () => set("bg", bgSelect.value));
  }

  document.querySelectorAll("[data-volume]").forEach((slider) => {
    slider.addEventListener("input", () => set("volume", Number(slider.value)));
  });

  /* ---- Settings panel ---------------------------------------------------- */

  const panel = document.getElementById("settings");
  const panelToggle = document.querySelector("[data-toggle-settings]");

  if (panel && panelToggle) {
    panelToggle.addEventListener("click", () => {
      panel.hidden = !panel.hidden;
      panelToggle.setAttribute("aria-expanded", String(!panel.hidden));
      blip();
    });

    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape" && !panel.hidden) {
        panel.hidden = true;
        panelToggle.setAttribute("aria-expanded", "false");
        panelToggle.focus();
      }
    });
  }

  /* ---- Seed copy --------------------------------------------------------- */

  const seedButton = document.querySelector("[data-copy-seed]");
  const seedValue = document.querySelector("[data-seed-value]");

  if (seedButton && seedValue) {
    seedButton.addEventListener("click", async () => {
      const text = seedValue.textContent.trim();

      try {
        await navigator.clipboard.writeText(text);
        seedButton.textContent = "Copied";
        say("Seed copied to clipboard.");
      } catch {
        const range = document.createRange();
        range.selectNodeContents(seedValue);
        const selection = window.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
        seedButton.textContent = "Selected";
        say("Select and copy the highlighted seed.");
      }

      blip();
      window.setTimeout(() => {
        seedButton.textContent = "Copy seed";
      }, 1800);
    });
  }

  /* ---- Reset ------------------------------------------------------------- */

  const reset = document.querySelector("[data-reset]");
  if (reset) {
    reset.addEventListener("click", () => {
      prefs = Object.assign({}, defaults);
      save();
      apply();
      reflect();
      applyVolume();
      syncMusic();
      say("Settings back to defaults.");
      blip();
    });
  }

  apply();
  reflect();
  applyVolume();

  /* Music is never restored automatically: a browser will refuse to play without a gesture, and a
     visitor does not expect a five megabyte track the moment the page opens. */
  if (track) track.volume = Number(prefs.volume);
})();
