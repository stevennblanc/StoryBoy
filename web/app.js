const STORE_INDEX_URL = "/store/store-index.json";
const state = {
  view: "library",
  store: [],
  library: [],
  query: "",
  activeBook: null,
  activePackage: null,
  currentNodeId: null,
  inventory: [],
  evidence: [],
};

const app = document.querySelector("#app");
const title = document.querySelector("#screen-title");
const refreshButton = document.querySelector("#refresh-button");
const navButtons = Array.from(document.querySelectorAll("[data-view]"));

refreshButton.addEventListener("click", () => loadStore(true));
navButtons.forEach((button) => {
  button.addEventListener("click", () => {
    state.view = button.dataset.view;
    state.activeBook = null;
    state.activePackage = null;
    updateNav();
    render();
  });
});

loadStore();

async function loadStore(force = false) {
  try {
    if (force) {
      app.innerHTML = `<p class="muted">Refreshing store...</p>`;
    }
    const response = await fetch(`${STORE_INDEX_URL}?t=${Date.now()}`, { cache: "no-store" });
    if (!response.ok) {
      throw new Error(`Store index returned ${response.status}`);
    }
    const index = await response.json();
    state.store = Array.isArray(index.gamebooks) ? index.gamebooks : [];
    state.library = state.store.filter((book) => readProgress(book.id));
    render();
  } catch (error) {
    app.innerHTML = `<p class="error">Could not load the StoryBoy store. ${escapeHtml(error.message)}</p>`;
  }
}

function updateNav() {
  navButtons.forEach((button) => {
    button.classList.toggle("active", button.dataset.view === state.view);
  });
}

function render() {
  if (state.activePackage && state.currentNodeId) {
    renderReader();
    return;
  }

  title.textContent = titleForView(state.view);
  if (state.view === "library") {
    renderLibrary();
  } else if (state.view === "store") {
    renderStore();
  } else {
    renderSettings();
  }
}

function titleForView(view) {
  if (view === "store") return "Store";
  if (view === "settings") return "Settings";
  return "Library";
}

function renderLibrary() {
  const books = state.store.filter((book) => readProgress(book.id));
  if (books.length === 0) {
    app.innerHTML = `
      <div class="panel">
        <h2>No gamebooks in progress</h2>
        <p class="muted">Open the store and start an adventure to add it here.</p>
        <button class="primary-button" type="button" data-open-store>Browse Store</button>
      </div>
    `;
    app.querySelector("[data-open-store]").addEventListener("click", () => {
      state.view = "store";
      updateNav();
      render();
    });
    return;
  }

  app.innerHTML = `
    <div class="search-row">
      <input id="search" type="search" placeholder="Search library" value="${escapeHtml(state.query)}">
    </div>
    <section class="book-grid">
      ${books.filter(matchesQuery).map(renderBookCard).join("")}
    </section>
  `;
  wireSearch();
  wireBookButtons();
}

function renderStore() {
  app.innerHTML = `
    <div class="search-row">
      <input id="search" type="search" placeholder="Search store" value="${escapeHtml(state.query)}">
    </div>
    <section class="store-list">
      ${state.store.filter(matchesQuery).map(renderStoreRow).join("")}
    </section>
  `;
  wireSearch();
  wireBookButtons();
}

function renderSettings() {
  const fontSize = Number(localStorage.getItem("storyboy.web.fontSize") || 22);
  app.innerHTML = `
    <section class="settings-list">
      <div class="panel">
        <h2>Web preview</h2>
        <p class="muted">This static version reads the same .gbk packages as Android. It stores progress in this browser only.</p>
      </div>
      <div class="panel">
        <h2>Reader text size</h2>
        <input id="font-size" type="range" min="18" max="30" step="1" value="${fontSize}">
        <p class="muted">${fontSize}px</p>
      </div>
      <div class="panel">
        <h2>Presentation</h2>
        <p class="muted">The web preview uses static presentation by default so behavior stays close to the future e-ink version.</p>
      </div>
    </section>
  `;
  const slider = app.querySelector("#font-size");
  slider.addEventListener("input", () => {
    localStorage.setItem("storyboy.web.fontSize", slider.value);
    document.documentElement.style.setProperty("--reader-size", `${slider.value}px`);
    slider.nextElementSibling.textContent = `${slider.value}px`;
  });
}

function renderBookCard(book) {
  const progress = readProgress(book.id);
  return `
    <article class="book-card">
      <button class="plain-button" type="button" data-book-id="${escapeHtml(book.id)}">
        <img class="cover" src="${escapeAttribute(book.posterUrl || "")}" alt="">
      </button>
      <div class="book-meta">
        <p class="book-title">${escapeHtml(book.title)}</p>
        <p class="muted">${progress ? "In progress" : escapeHtml(book.genre || "")}</p>
      </div>
    </article>
  `;
}

function renderStoreRow(book) {
  const progress = readProgress(book.id);
  return `
    <article class="store-row">
      <img src="${escapeAttribute(book.posterUrl || "")}" alt="">
      <div class="store-info">
        <h2>${escapeHtml(book.title)}</h2>
        <p class="muted">${escapeHtml(book.author || "")}</p>
        <p class="muted">${escapeHtml(book.genre || "")}</p>
        <p>${escapeHtml(book.description || "")}</p>
        <div class="actions">
          <button class="primary-button" type="button" data-book-id="${escapeHtml(book.id)}">
            ${progress ? "Resume" : "Play"}
          </button>
        </div>
      </div>
    </article>
  `;
}

function wireSearch() {
  const search = app.querySelector("#search");
  if (!search) return;
  search.addEventListener("input", () => {
    state.query = search.value;
    render();
  });
}

function wireBookButtons() {
  app.querySelectorAll("[data-book-id]").forEach((button) => {
    button.addEventListener("click", () => {
      const book = state.store.find((entry) => entry.id === button.dataset.bookId);
      if (book) openBook(book);
    });
  });
}

async function openBook(book) {
  title.textContent = "Loading";
  app.innerHTML = `<p class="muted">Opening ${escapeHtml(book.title)}...</p>`;
  try {
    const gamebook = await loadGamebookPackage(book.downloadUrl);
    const progress = readProgress(book.id);
    state.activeBook = book;
    state.activePackage = gamebook;
    state.inventory = progress?.inventory || [];
    state.evidence = progress?.evidence || [];
    state.currentNodeId = progress?.nodeId || gamebook.metadata.start_node || gamebook.metadata.startNode;
    saveProgress();
    renderReader();
  } catch (error) {
    app.innerHTML = `<p class="error">Could not open this gamebook. ${escapeHtml(error.message)}</p>`;
  }
}

function renderReader() {
  const book = state.activeBook;
  const gamebook = state.activePackage;
  const node = gamebook.nodes.get(state.currentNodeId);
  title.textContent = book.title;

  if (!node) {
    app.innerHTML = `<p class="error">Story node is missing: ${escapeHtml(state.currentNodeId)}</p>`;
    return;
  }

  applyNodeGains(node, gamebook);
  const imageHtml = renderNodeImages(node, gamebook);
  const inventoryPanel = renderGainPanel("Items", state.inventory);
  const evidencePanel = renderGainPanel("Evidence", state.evidence);
  const bodyHtml = renderNodeBody(node, gamebook);
  const choiceHtml = renderNodeActions(node);

  app.innerHTML = `
    <article class="reader">
      <div class="chip-row">
        <span class="chip">Items ${state.inventory.length}</span>
        <span class="chip">Evidence ${state.evidence.length}</span>
      </div>
      ${imageHtml}
      ${bodyHtml}
      ${inventoryPanel}
      ${evidencePanel}
      ${choiceHtml}
      <button class="secondary-button" type="button" data-close-reader>Library</button>
    </article>
  `;
  wireReaderActions(node);
  saveProgress();
}

function renderNodeImages(node, gamebook) {
  const images = normalizeImages(node);
  if (images.length === 0) return "";
  return images.map((image) => {
    const src = gamebook.imageUrls.get(image.path);
    if (!src) return "";
    return `
      <img class="reader-image" src="${escapeAttribute(src)}" alt="">
      ${image.caption ? `<p class="caption">${escapeHtml(image.caption)}</p>` : ""}
    `;
  }).join("");
}

function normalizeImages(node) {
  const images = [];
  if (node.image) {
    images.push({ path: node.image, caption: node.image_caption || node.imageCaption || "" });
  }
  if (Array.isArray(node.images)) {
    node.images.forEach((image) => {
      if (typeof image === "string") {
        images.push({ path: image, caption: "" });
      } else if (image?.path) {
        images.push({ path: image.path, caption: image.caption || "" });
      }
    });
  }
  return images;
}

function renderNodeBody(node) {
  if (node.type === "lore" && Array.isArray(node.entries)) {
    return node.entries.map((entry) => `
      <section class="panel">
        <h2>${escapeHtml(entry.title || "Lore")}</h2>
        <p class="reader-text">${escapeHtml(entry.text || "")}</p>
      </section>
    `).join("");
  }

  if (node.type === "battle" && state.lastBattle) {
    return `
      <section class="panel">
        <h2>${escapeHtml(node.title || "Battle")}</h2>
        <p class="reader-text">${escapeHtml(node.text || "")}</p>
        <p>Player: ${state.lastBattle.playerTotal} | Opponent: ${state.lastBattle.opponentTotal}</p>
        <p class="muted">${escapeHtml(state.lastBattle.outcome)}</p>
      </section>
    `;
  }

  return `
    ${node.title ? `<h2>${escapeHtml(node.title)}</h2>` : ""}
    <p class="reader-text">${escapeHtml(node.text || node.body || "")}</p>
  `;
}

function renderGainPanel(label, items) {
  if (!items.length) return "";
  return `
    <section class="panel">
      <h3>${label}</h3>
      ${items.map((item) => `<p><strong>${escapeHtml(item.title || item.id)}</strong><br><span class="muted">${escapeHtml(item.description || "")}</span></p>`).join("")}
    </section>
  `;
}

function renderNodeActions(node) {
  if (node.type === "puzzle") {
    return `
      <form class="puzzle-form" data-puzzle-form>
        <label>${escapeHtml(node.question || "Answer")}</label>
        <input name="answer" autocomplete="off">
        <button class="primary-button" type="submit">Submit</button>
      </form>
    `;
  }

  if (node.type === "map" && Array.isArray(node.locations)) {
    return renderChoices(node.locations.map((location) => ({
      text: `${location.title}${location.description ? `: ${location.description}` : ""}`,
      target: location.target,
    })));
  }

  if (node.type === "battle") {
    return `<button class="primary-button" type="button" data-resolve-battle>Resolve</button>`;
  }

  const choices = Array.isArray(node.choices) ? node.choices : [];
  if (choices.length > 0) {
    return renderChoices(choices);
  }
  if (node.return_to || node.returnTo) {
    return renderChoices([{ text: "Continue", target: node.return_to || node.returnTo }]);
  }
  return `<p class="muted">Ending reached.</p>`;
}

function renderChoices(choices) {
  return `
    <div class="choice-list">
      ${choices.map((choice) => `
        <button class="choice" type="button" data-target="${escapeAttribute(choice.target || "")}">
          ${escapeHtml(choice.text || choice.title || "Continue")}
        </button>
      `).join("")}
    </div>
  `;
}

function wireReaderActions(node) {
  app.querySelector("[data-close-reader]")?.addEventListener("click", () => {
    state.activePackage = null;
    state.activeBook = null;
    state.currentNodeId = null;
    state.view = "library";
    updateNav();
    render();
  });

  app.querySelectorAll("[data-target]").forEach((button) => {
    button.addEventListener("click", () => moveToNode(button.dataset.target));
  });

  app.querySelector("[data-puzzle-form]")?.addEventListener("submit", (event) => {
    event.preventDefault();
    const answer = normalizeAnswer(new FormData(event.currentTarget).get("answer") || "");
    const answers = Array.isArray(node.answers) ? node.answers.map(normalizeAnswer) : [];
    const target = answers.includes(answer)
      ? node.correct_target || node.correctTarget
      : node.incorrect_target || node.incorrectTarget || node.default_target || node.defaultTarget;
    moveToNode(target);
  });

  app.querySelector("[data-resolve-battle]")?.addEventListener("click", () => {
    const result = resolveBattle(node);
    state.lastBattle = result;
    moveToNode(result.target);
  });
}

function moveToNode(target) {
  if (!target) return;
  state.lastBattle = null;
  state.currentNodeId = target;
  renderReader();
}

function applyNodeGains(node, gamebook) {
  collectItems(node.items || node.inventory || node.gain_inventory || node.gains_inventory, gamebook.inventory, state.inventory);
  collectItems(node.evidence || node.gain_evidence || node.gains_evidence, gamebook.evidence, state.evidence);
}

function collectItems(rawItems, catalog, target) {
  if (!rawItems) return;
  const items = Array.isArray(rawItems) ? rawItems : [rawItems];
  items.forEach((item) => {
    const normalized = typeof item === "string" ? catalog.get(item) || { id: item, title: item } : item;
    if (!normalized?.id || target.some((existing) => existing.id === normalized.id)) return;
    target.push(normalized);
  });
}

function resolveBattle(node) {
  const battle = node.battle || node;
  const modifiers = Array.isArray(battle.item_modifiers) ? battle.item_modifiers : [];
  const inventoryIds = new Set(state.inventory.map((item) => item.id));
  const bonus = modifiers.reduce((sum, modifier) => {
    const itemId = modifier.item || modifier.item_id || modifier.itemId;
    return inventoryIds.has(itemId) ? sum + Number(modifier.bonus || 0) : sum;
  }, Number(battle.player_bonus || battle.playerBonus || 0));
  const player = rollDice(battle.player_dice || battle.playerDice || "1d6") + bonus;
  const opponent = rollDice(battle.opponent_dice || battle.opponentDice || "1d6") + Number(battle.opponent_bonus || battle.opponentBonus || 0);
  const outcome = player >= opponent ? "Success" : "Setback";
  const target = player > opponent
    ? battle.win_target || battle.winTarget
    : player < opponent
      ? battle.lose_target || battle.loseTarget
      : battle.draw_target || battle.drawTarget || battle.win_target || battle.winTarget;
  return { playerTotal: player, opponentTotal: opponent, outcome, target };
}

function rollDice(expression) {
  const match = String(expression).trim().match(/^(\d*)d(\d+)$/i);
  const count = Math.max(1, Number(match?.[1] || 1));
  const sides = Math.max(2, Number(match?.[2] || 6));
  let total = 0;
  for (let index = 0; index < count; index += 1) {
    total += Math.floor(Math.random() * sides) + 1;
  }
  return total;
}

async function loadGamebookPackage(url) {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Package returned ${response.status}`);
  }
  const buffer = await response.arrayBuffer();
  const zip = readZip(buffer);
  const storyText = await zip.text("story.json");
  const story = JSON.parse(storyText);
  const imageUrls = new Map();

  for (const name of zip.names()) {
    if (/\.(png|jpe?g|webp)$/i.test(name)) {
      const blob = await zip.blob(name);
      imageUrls.set(name, URL.createObjectURL(blob));
    }
  }

  return {
    metadata: story.metadata || {},
    nodes: new Map((story.nodes || []).map((node) => [node.id, node])),
    inventory: catalogMap(story.inventory),
    evidence: catalogMap(story.evidence),
    imageUrls,
  };
}

function catalogMap(items) {
  return new Map((Array.isArray(items) ? items : []).filter((item) => item?.id).map((item) => [item.id, item]));
}

function readZip(buffer) {
  const bytes = new Uint8Array(buffer);
  const view = new DataView(buffer);
  let eocd = -1;
  for (let index = bytes.length - 22; index >= 0; index -= 1) {
    if (view.getUint32(index, true) === 0x06054b50) {
      eocd = index;
      break;
    }
  }
  if (eocd < 0) throw new Error("Invalid zip package");

  const totalEntries = view.getUint16(eocd + 10, true);
  let offset = view.getUint32(eocd + 16, true);
  const entries = new Map();
  const decoder = new TextDecoder();

  for (let index = 0; index < totalEntries; index += 1) {
    if (view.getUint32(offset, true) !== 0x02014b50) throw new Error("Invalid zip directory");
    const method = view.getUint16(offset + 10, true);
    const compressedSize = view.getUint32(offset + 20, true);
    const fileNameLength = view.getUint16(offset + 28, true);
    const extraLength = view.getUint16(offset + 30, true);
    const commentLength = view.getUint16(offset + 32, true);
    const localOffset = view.getUint32(offset + 42, true);
    const name = decoder.decode(bytes.slice(offset + 46, offset + 46 + fileNameLength));
    entries.set(name, { method, compressedSize, localOffset });
    offset += 46 + fileNameLength + extraLength + commentLength;
  }

  async function extract(name) {
    const entry = entries.get(name);
    if (!entry) throw new Error(`Missing ${name}`);
    const localNameLength = view.getUint16(entry.localOffset + 26, true);
    const localExtraLength = view.getUint16(entry.localOffset + 28, true);
    const dataStart = entry.localOffset + 30 + localNameLength + localExtraLength;
    const compressed = bytes.slice(dataStart, dataStart + entry.compressedSize);
    if (entry.method === 0) return compressed;
    if (entry.method !== 8) throw new Error(`Unsupported zip compression for ${name}`);
    if (!("DecompressionStream" in window)) {
      throw new Error("This browser cannot decompress gamebook packages");
    }
    const stream = new Blob([compressed]).stream().pipeThrough(new DecompressionStream("deflate-raw"));
    return new Uint8Array(await new Response(stream).arrayBuffer());
  }

  return {
    names: () => Array.from(entries.keys()),
    text: async (name) => new TextDecoder().decode(await extract(name)),
    blob: async (name) => new Blob([await extract(name)], { type: mimeType(name) }),
  };
}

function mimeType(name) {
  if (/\.png$/i.test(name)) return "image/png";
  if (/\.jpe?g$/i.test(name)) return "image/jpeg";
  if (/\.webp$/i.test(name)) return "image/webp";
  return "application/octet-stream";
}

function readProgress(bookId) {
  const raw = localStorage.getItem(`storyboy.progress.${bookId}`);
  return raw ? JSON.parse(raw) : null;
}

function saveProgress() {
  if (!state.activeBook || !state.currentNodeId) return;
  localStorage.setItem(`storyboy.progress.${state.activeBook.id}`, JSON.stringify({
    nodeId: state.currentNodeId,
    inventory: state.inventory,
    evidence: state.evidence,
  }));
}

function matchesQuery(book) {
  const value = state.query.trim().toLowerCase();
  if (!value) return true;
  return [book.title, book.author, book.genre, book.description]
    .filter(Boolean)
    .some((field) => String(field).toLowerCase().includes(value));
}

function normalizeAnswer(value) {
  return String(value).trim().toLowerCase().replace(/\s+/g, " ");
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function escapeAttribute(value) {
  return escapeHtml(value);
}
