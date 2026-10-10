// Remplace « Beszel » par « Sopiker Monitoring » dans le titre de l'onglet,
// y compris quand Beszel le change en naviguant (ex. « Paramètres / Beszel »).
(function () {
    function fix() {
        if (document.title.indexOf("Beszel") !== -1) {
            document.title = document.title.replace(/Beszel/g, "Sopiker Monitoring");
        }
    }
    function start() {
        var el = document.querySelector("title");
        if (el) {
            new MutationObserver(fix).observe(el, { childList: true, characterData: true, subtree: true });
        }
        fix();
    }
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", start);
    } else {
        start();
    }
})();
