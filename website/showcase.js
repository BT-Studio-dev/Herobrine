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

  const prefersLight = typeof window.matchMedia === "function"
    && window.matchMedia("(prefers-color-scheme: light)").matches;

  const defaults = {
    theme: prefersLight ? "light" : "dark",
    glass: "on",
    bg: "shader",
    sounds: "off",
    music: "off",
    alex: "off",
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

  /* Written to every status line, one per track, so a message about the page as a whole is not
     left sitting under a transport you are not looking at. */
  const say = (message) => {
    document.querySelectorAll("[data-status]").forEach((line) => {
      line.textContent = message || "";
    });
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

  /* ---- Music -------------------------------------------------------------
   *
   * Two tracks, two transports, one factory. Both are marked preload="none" and neither is
   * fetched until its own play button is pressed: his is five megabytes, and a visitor who came
   * for one of them has no reason to be sent the other.
   */

  const clock = (seconds) => {
    if (!Number.isFinite(seconds) || seconds < 0) return "--:--";

    const whole = Math.floor(seconds);
    return Math.floor(whole / 60) + ":" + String(whole % 60).padStart(2, "0");
  };

  function createPlayer(panel) {
    const track = panel.querySelector("audio");
    const seekSlider = panel.querySelector("[data-seek]");
    const seekFill = panel.querySelector("[data-seek-fill]");
    const seekBuffer = panel.querySelector("[data-seek-buffer]");
    const positionLabel = panel.querySelector("[data-position]");
    const durationLabel = panel.querySelector("[data-duration]");
    const line = panel.querySelector("[data-status]");

    const prefKey = panel.dataset.player;
    const PLAYING = track ? (track.dataset.playing || "Playing.") : "Playing.";
    const FETCHING = track ? (track.dataset.fetching || "Fetching the track\u2026") : "";

    let scrubbing = false;

    const tell = (message) => {
      if (line) line.textContent = message || "";
    };

    function applyVolume() {
      if (track) track.volume = Number(prefs.volume);
    }

    /* The transport is the only place the difference between "play was pressed" and "sound is
       coming out" is visible, because the track is fetched lazily. So every state the element
       reports is drawn: how far in we are, how much has arrived, and what went wrong when it
       does not play. */
    function paint() {
      if (!track) return;

      const duration = Number.isFinite(track.duration) ? track.duration : 0;
      const here = track.currentTime || 0;
      const percent = duration ? (here / duration) * 100 : 0;

      if (seekFill) seekFill.style.width = percent + "%";
      if (positionLabel) positionLabel.textContent = clock(here);
      if (durationLabel) durationLabel.textContent = duration ? clock(duration) : "--:--";

      if (seekSlider && !scrubbing) {
        seekSlider.value = String(Math.round(percent * 10));
      }

      if (seekBuffer && duration && track.buffered.length) {
        // Of the buffered ranges, the one we are standing in: a long track arrives in pieces,
        // and the piece behind you says nothing about whether the next one is here.
        let end = 0;

        for (let i = 0; i < track.buffered.length; i++) {
          if (track.buffered.start(i) <= here + 0.25 && track.buffered.end(i) >= here) {
            end = track.buffered.end(i);
            break;
          }
        }

        seekBuffer.style.width = Math.min(100, (end / duration) * 100) + "%";
      }
    }

    /* Dragging the bar must not fight the element: while the thumb is held the clock follows the
       thumb, and the jump is made once on release. */
    if (track && seekSlider) {
      seekSlider.addEventListener("input", () => {
        scrubbing = true;

        const duration = Number.isFinite(track.duration) ? track.duration : 0;
        const at = duration ? (Number(seekSlider.value) / 1000) * duration : 0;

        if (positionLabel) positionLabel.textContent = clock(at);
        if (seekFill) seekFill.style.width = Number(seekSlider.value) / 10 + "%";
      });

      seekSlider.addEventListener("change", () => {
        if (!scrubbing) return;
        scrubbing = false;

        const duration = Number.isFinite(track.duration) ? track.duration : 0;
        if (duration) track.currentTime = (Number(seekSlider.value) / 1000) * duration;
      });
    }

    function stop(message) {
      prefs[prefKey] = "off";
      save();
      reflect();
      if (track) track.pause();
      tell(message || "");
    }

    function sync() {
      if (!track) return;

      if (prefs[prefKey] !== "on") {
        track.pause();
        tell("");
        return;
      }

      applyVolume();
      if (track.readyState < track.HAVE_FUTURE_DATA) tell(FETCHING);

      let started = null;

      try {
        started = track.play();
      } catch {
        started = null;
      }

      // Very old browsers return undefined rather than a promise, which is fine: they simply
      // cannot tell us how it went.
      if (!started || typeof started.then !== "function") return;

      started.then(() => tell(PLAYING), (error) => {
        const name = error && error.name;

        // A pause that lands before play() settles rejects with AbortError. That is the browser
        // behaving, and not worth reporting as a refusal.
        if (name === "AbortError") return;
        if (name === "NotAllowedError") {
          stop("Your browser blocked playback. Press play again to allow it.");
        } else if (name === "NotSupportedError") {
          stop("This browser cannot play the Ogg track. Use the download link below.");
        } else {
          stop("The track could not be played. Check the connection and try again.");
        }
      });
    }

    if (track) {
      track.addEventListener("loadedmetadata", paint);
      track.addEventListener("durationchange", paint);
      track.addEventListener("timeupdate", paint);
      track.addEventListener("progress", paint);
      track.addEventListener("seeked", paint);
      track.addEventListener("playing", () => tell(PLAYING));
      track.addEventListener("waiting", () => {
        if (prefs[prefKey] === "on") tell("Buffering\u2026");
      });
      track.addEventListener("error", () => {
        if (track.error) stop("The track could not be loaded. Try the download link below.");
      });
    }

    return { prefKey: prefKey, applyVolume: applyVolume, paint: paint, sync: sync };
  }

  const players = [];

  document.querySelectorAll("[data-player]").forEach((panel) => {
    players.push(createPlayer(panel));
  });

  function applyVolume() {
    players.forEach((player) => player.applyVolume());
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
    if (key === "sounds" && value === "on") blip();

    players.forEach((player) => {
      if (player.prefKey === key) player.sync();
    });
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
      players.forEach((player) => player.sync());
      say("Settings back to defaults.");
      blip();
    });
  }

  /* Neither track is restored automatically: a browser refuses to play without a gesture, and no
     visitor wants five megabytes the moment the page opens. The stored intent is dropped on load
     for the same reason, so the buttons can never claim to be playing a track nobody started. */
  players.forEach((player) => {
    prefs[player.prefKey] = "off";
  });

  save();

  apply();
  reflect();
  applyVolume();
  players.forEach((player) => player.paint());
})();
