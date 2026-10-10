// Runs inside the grammar page shown in the Grammar tab. The app has its own navigation, so the
// page's rail of site links is hidden, and the app is told which grammar point is open so it
// can offer that point's audiobook chapter.
(() => {
  if (window.__hoshiGrammarEmbed) return;
  window.__hoshiGrammarEmbed = true;

  const rail = document.querySelector("nav.rail");
  if (rail) {
    rail.style.display = "none";
    document.body.style.display = "block";
  }

  const report = () => {
    let id = "";
    try { id = decodeURIComponent(location.hash.slice(1)); } catch { /* Not an ID. */ }
    HoshiGrammar.onCard(id);
  };
  window.addEventListener("hashchange", report);
  report();
})();
