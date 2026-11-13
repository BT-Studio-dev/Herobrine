const copyButton = document.querySelector("[data-copy-commit]");
const commitHash = document.querySelector("#commit-hash");
const copyStatus = document.querySelector("[data-copy-status]");

if (copyButton && commitHash) {
  copyButton.addEventListener("click", async () => {
    const value = commitHash.textContent.trim();

    try {
      await navigator.clipboard.writeText(value);
      copyButton.textContent = "Copied";
      copyStatus.textContent = "Commit hash copied to clipboard.";
    } catch {
      const range = document.createRange();
      range.selectNodeContents(commitHash);
      const selection = window.getSelection();
      selection.removeAllRanges();
      selection.addRange(range);
      copyStatus.textContent = "Select and copy the highlighted commit hash.";
      copyButton.textContent = "Selected";
    }

    window.setTimeout(() => {
      copyButton.textContent = "Copy";
    }, 1800);
  });
}
