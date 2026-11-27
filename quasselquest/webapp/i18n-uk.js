// Bedienung auf Ukrainisch („Meine Sprache: Українська“), Stand 27.11.2026. Nur aktiv mit window.UK_AKTIV und Einstellung meineSprache = 'uk'.
// Einfaches Verfahren: Texte, die genau (nach Trim) in der Tabelle stehen, werden im Dokument ersetzt. Unbekannte Texte bleiben Deutsch.
// Alle Übersetzungen sind von uns geschrieben und müssen vor der Freigabe von einer Muttersprachlerin geprüft werden (PRUEFBOGEN-UKRAINISCH.md).
(function () {
  const T = {
    'Weiter ➜': 'Далі ➜', 'Weiter': 'Далі', 'Fertig 🏠': 'Готово 🏠', 'Fertig': 'Готово', 'OK': 'Гаразд', 'Abbrechen': 'Скасувати',
    'Zur Einheit': 'До уроку', '💾 Speichern': '💾 Зберегти', 'Ändern': 'Змінити', 'Öffnen': 'Відкрити', 'Ansehen': 'Переглянути',
    '🔒 Deine Daten': '🔒 Твої дані', '🔒 Deine Daten – kurz erklärt': '🔒 Твої дані – коротко', '👀 Noch mal zeigen': '👀 Показати ще раз',
    'Tipp auf 🎤 und sag es laut!': 'Натисни 🎤 і скажи вголос!', '💡 Ruby erklärt': '💡 Рубі пояснює', 'Verstanden! 👍': 'Зрозуміло! 👍',
    '🔊 Noch mal': '🔊 Ще раз', '🔁 Noch eine Runde': '🔁 Ще один раунд', 'Ich sage es leise für mich': 'Я кажу це про себе',
    'Zum Hafen ⚓': 'До гавані ⚓', '← Alle Apps': '← Усі застосунки', 'Start': 'Старт', 'Los': 'Вперед',
    '🔔 Töne': '🔔 Звуки', '📳 Vibration': '📳 Вібрація', '🐢 Immer langsamer sprechen': '🐢 Завжди говорити повільніше',
    '🙂 Name': '🙂 Ім’я', '👪 Für Eltern': '👪 Для батьків', 'Einstellungen': 'Налаштування', 'Zurück': 'Назад',
    'Gut gemacht!': 'Молодець!', 'Super!': 'Чудово!', 'Noch mal versuchen': 'Спробуй ще раз',
  };
  const aktiv = () => window.UK_AKTIV === true && window.APP_STATE_MEINE_SPRACHE === 'uk';
  function tausche(root) {
    if (!aktiv()) return;
    const w = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    let n; while ((n = w.nextNode())) {
      const t = n.nodeValue.trim(); if (!t || !T[t]) continue;
      n.nodeValue = n.nodeValue.replace(t, T[t]);
    }
  }
  let ruht = false;
  const beob = typeof MutationObserver === 'function' ? new MutationObserver(() => { if (ruht) return; ruht = true; try { tausche(document.body); } finally { ruht = false; } }) : null;
  window.I18N = {
    TABELLE: T,
    setze(sprache) { window.APP_STATE_MEINE_SPRACHE = sprache; if (document.documentElement) document.documentElement.lang = sprache === 'uk' ? 'uk' : 'de'; if (beob && sprache === 'uk') { beob.observe(document.body, { childList: true, subtree: true, characterData: true }); tausche(document.body); } },
    uebersetze: (t) => (aktiv() && T[String(t).trim()]) || t,
  };
  if (typeof module !== 'undefined') module.exports = window.I18N;
})();
