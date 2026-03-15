const matches = [
  { title: "Bayern vs Leverkusen", channel: "Sport 1", tag: "Tor erkannt" },
  { title: "Real Madrid vs Barcelona", channel: "Sport 2", tag: "Hauptspiel" },
  { title: "Inter vs Milan", channel: "Sport 3", tag: "Naechstes Highlight" }
];

const rail = document.getElementById("scoreRail");

if (rail) {
  matches.forEach((match, index) => {
    const row = document.createElement("article");
    row.className = "match-row";
    row.style.animationDelay = `${index * 120}ms`;
    row.innerHTML = `
      <div>
        <strong>${match.title}</strong>
        <small>${match.channel}</small>
      </div>
      <span class="goal-pill">${match.tag}</span>
    `;
    rail.appendChild(row);
  });
}
