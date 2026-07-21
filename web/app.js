const STORE_INDEX_URL = "/store/store-index.json";
const SUPABASE_URL = "https://ndgguqbrhatvcqetgeks.supabase.co";
const SUPABASE_KEY = "sb_publishable_H46jhfVAHcILTZQyu7BleA_I2YohwoI";

let supabase = null;
try {
  const { createClient } = await import("https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2/+esm");
  supabase = createClient(SUPABASE_URL, SUPABASE_KEY);
} catch (error) {
  console.warn("Supabase client unavailable, using static store index.", error);
}

const state = {
  view: "library",
  store: [],
  library: [],
  storeSource: "static",
  query: "",
  session: null,
  ownedBookIds: new Set(),
  detailBookId: null,
  accountMessage: "",
  profileMessage: "",
  activeBook: null,
  activePackage: null,
  currentNodeId: null,
  inventory: [],
  evidence: [],
  equipment: [],
  equippedBySlot: {},
  map: [],
  flags: [],
  stats: {},
  spentUses: {},
  itemMessage: "",
  checkResult: null,
  enemyHp: null,
  combatLog: [],
  combatEnd: null,
  shopMessage: "",
  showEquipment: false,
  selectingCharacter: false,
  chosenCharacter: null,
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
    state.detailBookId = null;
    updateNav();
    render();
  });
});

initAuth();
loadStore();

async function initAuth() {
  if (!supabase) return;
  const { data } = await supabase.auth.getSession();
  state.session = data?.session || null;
  await loadPurchases();
  supabase.auth.onAuthStateChange(async (_event, session) => {
    state.session = session;
    await loadPurchases();
    render();
  });
  render();
}

async function loadPurchases() {
  state.ownedBookIds = new Set();
  if (!supabase || !state.session) return;
  const { data, error } = await supabase.from("purchases").select("book_id");
  if (!error && Array.isArray(data)) {
    state.ownedBookIds = new Set(data.map((row) => row.book_id));
  }
}

async function loadStore(force = false) {
  if (force) {
    app.innerHTML = `<p class="muted">Refreshing store...</p>`;
  }
  const books = await loadCatalogue();
  if (books) {
    state.store = books;
    render();
  } else {
    app.innerHTML = `<p class="error">Could not load the StoryBoy store.</p>`;
  }
}

async function loadCatalogue() {
  if (supabase) {
    const { data, error } = await supabase
      .from("books")
      .select("*")
      .eq("is_published", true)
      .order("published_on", { ascending: false });
    if (!error && Array.isArray(data) && data.length > 0) {
      state.storeSource = "supabase";
      return data.map(normalizeCatalogueBook);
    }
    console.warn("Falling back to static store index.", error);
  }
  try {
    const response = await fetch(`${STORE_INDEX_URL}?t=${Date.now()}`, { cache: "no-store" });
    if (!response.ok) throw new Error(`Store index returned ${response.status}`);
    const index = await response.json();
    state.storeSource = "static";
    return (Array.isArray(index.gamebooks) ? index.gamebooks : []).map(normalizeStaticBook);
  } catch (error) {
    console.error(error);
    return null;
  }
}

function normalizeCatalogueBook(row) {
  return {
    id: row.id,
    title: row.title,
    author: row.author,
    genre: row.genre || "",
    description: row.description || "",
    about: row.about || "",
    version: row.version || "",
    priceUsd: Number(row.price_usd || 0),
    language: row.language || "",
    publisher: row.publisher || "",
    publishedOn: row.published_on || "",
    nodeCount: row.node_count,
    endingCount: row.ending_count,
    fileSizeBytes: row.file_size_bytes,
    features: Array.isArray(row.features) ? row.features : [],
    downloadUrl: row.download_path,
    posterUrl: row.poster_path || "",
    bannerUrl: row.banner_path || "",
  };
}

function normalizeStaticBook(book) {
  return {
    id: book.id,
    title: book.title,
    author: book.author || "",
    genre: book.genre || "",
    description: book.description || "",
    about: "",
    version: book.version || "",
    priceUsd: 0,
    language: "",
    publisher: "",
    publishedOn: "",
    nodeCount: null,
    endingCount: null,
    fileSizeBytes: null,
    features: [],
    downloadUrl: book.downloadUrl,
    posterUrl: book.posterUrl || "",
    bannerUrl: book.bannerUrl || "",
  };
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
    if (state.detailBookId) {
      renderBookDetail();
    } else {
      renderStore();
    }
  } else if (state.view === "account") {
    renderAccount();
  } else {
    renderSettings();
  }
}

function titleForView(view) {
  if (view === "store") return "Store";
  if (view === "account") return "Account";
  if (view === "settings") return "Settings";
  return "Library";
}

function renderLibrary() {
  const books = state.store.filter((book) => state.ownedBookIds.has(book.id) || readProgress(book.id));
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
      ${renderProfilePanel()}
      <div class="panel">
        <h2>Reader text size</h2>
        <input id="font-size" type="range" min="18" max="30" step="1" value="${fontSize}">
        <p class="muted">${fontSize}px</p>
      </div>
      <div class="panel">
        <h2>Presentation</h2>
        <p class="muted">StoryBoy on the web uses static presentation so behavior stays close to the future e-ink reader. Reading progress is stored in this browser.</p>
      </div>
    </section>
  `;
  const slider = app.querySelector("#font-size");
  slider.addEventListener("input", () => {
    localStorage.setItem("storyboy.web.fontSize", slider.value);
    document.documentElement.style.setProperty("--reader-size", `${slider.value}px`);
    slider.nextElementSibling.textContent = `${slider.value}px`;
  });
  wireProfilePanel();
}

function renderProfilePanel() {
  if (!supabase || !state.session) {
    return `
      <div class="panel">
        <h2>Profile</h2>
        <p class="muted">Sign in to manage your StoryBoy profile and library.</p>
        <button class="secondary-button" type="button" data-go-account>Go to Account</button>
      </div>
    `;
  }
  const user = state.session.user;
  const displayName = user.user_metadata?.display_name || "";
  const provider = user.app_metadata?.provider || "email";
  return `
    <div class="panel">
      <h2>Profile</h2>
      <form class="auth-form" data-profile-form>
        <label class="muted" for="profile-display-name">Display name</label>
        <input id="profile-display-name" name="displayName" type="text" value="${escapeAttribute(displayName)}" placeholder="Display name" autocomplete="nickname">
        <button class="primary-button" type="submit">Save profile</button>
      </form>
      <p class="muted">Signed in as ${escapeHtml(user.email)} (${escapeHtml(provider)})</p>
      <p class="muted">${state.ownedBookIds.size} book${state.ownedBookIds.size === 1 ? "" : "s"} in your library</p>
      ${state.profileMessage ? `<p class="muted">${escapeHtml(state.profileMessage)}</p>` : ""}
    </div>
  `;
}

function wireProfilePanel() {
  app.querySelector("[data-go-account]")?.addEventListener("click", () => {
    state.view = "account";
    updateNav();
    render();
  });
  app.querySelector("[data-profile-form]")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const displayName = String(new FormData(event.currentTarget).get("displayName") || "").trim();
    state.profileMessage = "Saving...";
    render();
    const { error } = await supabase.auth.updateUser({ data: { display_name: displayName } });
    if (!error) {
      await supabase.from("profiles").update({ display_name: displayName }).eq("id", state.session.user.id);
    }
    state.profileMessage = error ? `Could not save: ${error.message}` : "Profile saved.";
    render();
  });
}

function renderBookCard(book) {
  const progress = readProgress(book.id);
  const statusLine = progress ? "In progress" : state.ownedBookIds.has(book.id) ? "In your library" : escapeHtml(book.genre || "");
  return `
    <article class="book-card">
      <button class="plain-button" type="button" data-book-id="${escapeHtml(book.id)}">
        <img class="cover" src="${escapeAttribute(book.posterUrl || "")}" alt="">
      </button>
      <div class="book-meta">
        <p class="book-title">${escapeHtml(book.title)}</p>
        <p class="muted">${statusLine}</p>
      </div>
    </article>
  `;
}

function renderStoreRow(book) {
  const owned = state.ownedBookIds.has(book.id);
  return `
    <article class="store-row">
      <button class="plain-button" type="button" data-detail-id="${escapeHtml(book.id)}">
        <img src="${escapeAttribute(book.posterUrl || "")}" alt="">
      </button>
      <div class="store-info">
        <button class="plain-button store-title-button" type="button" data-detail-id="${escapeHtml(book.id)}">
          <h2>${escapeHtml(book.title)}</h2>
        </button>
        <p class="muted">${escapeHtml(book.author || "")}</p>
        <p class="muted">${escapeHtml(book.genre || "")}</p>
        <p class="price-line">${owned ? `<span class="owned-badge">In your library</span>` : renderPrice(book)}</p>
      </div>
    </article>
  `;
}

function renderPrice(book) {
  if (!book.priceUsd) return `<span class="free-badge">Free</span>`;
  return `<span class="price-badge">$${book.priceUsd.toFixed(2)}</span>`;
}

function renderBookDetail() {
  const book = state.store.find((entry) => entry.id === state.detailBookId);
  if (!book) {
    state.detailBookId = null;
    renderStore();
    return;
  }
  title.textContent = "Store";
  const owned = state.ownedBookIds.has(book.id);
  const progress = readProgress(book.id);
  const signedIn = Boolean(state.session);

  const detailCells = [
    book.nodeCount ? detailCell("Length", `${book.nodeCount} passages`) : "",
    book.endingCount ? detailCell("Endings", String(book.endingCount)) : "",
    book.language ? detailCell("Language", book.language) : "",
    book.fileSizeBytes ? detailCell("Size", formatBytes(book.fileSizeBytes)) : "",
    book.version ? detailCell("Version", book.version) : "",
    book.publishedOn ? detailCell("Published", book.publishedOn) : "",
  ].filter(Boolean).join("");

  let actions = "";
  if (owned || progress) {
    actions = `<button class="primary-button" type="button" data-read-id="${escapeHtml(book.id)}">${progress ? "Continue reading" : "Read now"}</button>`;
  } else if (signedIn) {
    actions = `<button class="primary-button" type="button" data-get-id="${escapeHtml(book.id)}">Get${book.priceUsd ? ` — $${book.priceUsd.toFixed(2)}` : " — Free"}</button>`;
  } else {
    actions = `
      <button class="primary-button" type="button" data-read-id="${escapeHtml(book.id)}">Read now</button>
      <button class="secondary-button" type="button" data-go-account>Sign in to add it to your library</button>
    `;
  }

  app.innerHTML = `
    <article class="book-detail">
      <button class="secondary-button back-button" type="button" data-back-to-store>&#8592; Store</button>
      <div class="detail-hero">
        <img class="detail-poster" src="${escapeAttribute(book.posterUrl || "")}" alt="">
        <div class="detail-headline">
          <h2>${escapeHtml(book.title)}</h2>
          <p class="detail-author">${escapeHtml(book.author)}</p>
          <p class="muted">${escapeHtml(book.genre || "")}</p>
          <p class="price-line">${owned ? `<span class="owned-badge">In your library</span>` : renderPrice(book)}</p>
        </div>
      </div>
      <div class="actions detail-actions">${actions}</div>
      <p class="reader-text detail-description">${escapeHtml(book.description || "")}</p>
      ${book.features.length ? `<div class="chip-row">${book.features.map((f) => `<span class="chip">${escapeHtml(f)}</span>`).join("")}</div>` : ""}
      ${detailCells ? `<section class="panel"><h3>Book details</h3><div class="detail-grid">${detailCells}</div></section>` : ""}
      ${book.about ? `<section class="panel"><h3>About this book</h3><p class="detail-about">${escapeHtml(book.about)}</p></section>` : ""}
    </article>
  `;

  app.querySelector("[data-back-to-store]")?.addEventListener("click", () => {
    state.detailBookId = null;
    render();
  });
  app.querySelector("[data-go-account]")?.addEventListener("click", () => {
    state.view = "account";
    updateNav();
    render();
  });
  app.querySelector("[data-get-id]")?.addEventListener("click", (event) => acquireBook(event.currentTarget.dataset.getId));
  app.querySelector("[data-read-id]")?.addEventListener("click", (event) => {
    const target = state.store.find((entry) => entry.id === event.currentTarget.dataset.readId);
    if (target) openBook(target);
  });
}

function detailCell(label, value) {
  return `
    <div class="detail-cell">
      <p class="muted">${escapeHtml(label)}</p>
      <p>${escapeHtml(value)}</p>
    </div>
  `;
}

async function acquireBook(bookId) {
  if (!supabase || !state.session) return;
  const { error } = await supabase.from("purchases").insert({
    user_id: state.session.user.id,
    book_id: bookId,
  });
  if (error && error.code !== "23505") {
    app.querySelector(".detail-actions").innerHTML = `<p class="error">Could not add this book: ${escapeHtml(error.message)}</p>`;
    return;
  }
  state.ownedBookIds.add(bookId);
  render();
}

function formatBytes(bytes) {
  return `${(Number(bytes) / (1024 * 1024)).toFixed(1)} MB`;
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
  app.querySelectorAll("[data-detail-id]").forEach((button) => {
    button.addEventListener("click", () => {
      state.detailBookId = button.dataset.detailId;
      render();
    });
  });
}

function renderAccount() {
  if (!supabase) {
    app.innerHTML = `
      <div class="panel">
        <h2>Accounts unavailable</h2>
        <p class="muted">The store service could not be reached. Reading still works without an account.</p>
      </div>
    `;
    return;
  }

  if (state.session) {
    const user = state.session.user;
    const displayName = user.user_metadata?.display_name || user.email;
    app.innerHTML = `
      <section class="settings-list">
        <div class="panel">
          <h2>${escapeHtml(displayName)}</h2>
          <p class="muted">${escapeHtml(user.email)}</p>
          <p class="muted">${state.ownedBookIds.size} book${state.ownedBookIds.size === 1 ? "" : "s"} in your library</p>
        </div>
        ${state.accountMessage ? `<p class="muted">${escapeHtml(state.accountMessage)}</p>` : ""}
        <button class="secondary-button" type="button" data-sign-out>Sign out</button>
      </section>
    `;
    app.querySelector("[data-sign-out]").addEventListener("click", async () => {
      state.accountMessage = "";
      await supabase.auth.signOut();
    });
    return;
  }

  app.innerHTML = `
    <section class="settings-list">
      <div class="panel">
        <h2>Sign in</h2>
        <div class="oauth-row">
          <button class="secondary-button" type="button" data-oauth="google">Continue with Google</button>
          <button class="secondary-button" type="button" data-oauth="facebook">Continue with Facebook</button>
        </div>
        <p class="muted oauth-divider">or with email</p>
        <form class="auth-form" data-sign-in-form>
          <input name="email" type="email" placeholder="Email" autocomplete="email" required>
          <input name="password" type="password" placeholder="Password" autocomplete="current-password" required>
          <button class="primary-button" type="submit">Sign in</button>
        </form>
      </div>
      <div class="panel">
        <h2>Create account</h2>
        <p class="muted">An account keeps your StoryBoy library together. All current books are free.</p>
        <form class="auth-form" data-sign-up-form>
          <input name="displayName" type="text" placeholder="Display name" autocomplete="nickname">
          <input name="email" type="email" placeholder="Email" autocomplete="email" required>
          <input name="password" type="password" placeholder="Password (8+ characters)" autocomplete="new-password" minlength="8" required>
          <button class="primary-button" type="submit">Create account</button>
        </form>
      </div>
      ${state.accountMessage ? `<p class="muted account-message">${escapeHtml(state.accountMessage)}</p>` : ""}
    </section>
  `;

  app.querySelectorAll("[data-oauth]").forEach((button) => {
    button.addEventListener("click", async () => {
      state.accountMessage = "";
      const { error } = await supabase.auth.signInWithOAuth({
        provider: button.dataset.oauth,
        options: { redirectTo: window.location.origin },
      });
      if (error) {
        state.accountMessage = `${button.textContent.trim()} is not available yet: ${error.message}`;
        render();
      }
    });
  });

  app.querySelector("[data-sign-in-form]").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    state.accountMessage = "Signing in...";
    render();
    const { error } = await supabase.auth.signInWithPassword({
      email: String(form.get("email")),
      password: String(form.get("password")),
    });
    state.accountMessage = error ? `Sign in failed: ${error.message}` : "";
    render();
  });

  app.querySelector("[data-sign-up-form]").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    state.accountMessage = "Creating account...";
    render();
    const { data, error } = await supabase.auth.signUp({
      email: String(form.get("email")),
      password: String(form.get("password")),
      options: {
        data: { display_name: String(form.get("displayName") || "").trim() || undefined },
      },
    });
    if (error) {
      state.accountMessage = `Could not create the account: ${error.message}`;
    } else if (!data.session) {
      state.accountMessage = "Account created. Check your email for a confirmation link, then sign in.";
    } else {
      state.accountMessage = "";
    }
    render();
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
    state.equipment = progress?.equipment || [];
    state.equippedBySlot = progress?.equippedBySlot || {};
    state.map = progress?.map || [];
    state.flags = progress?.flags || [];
    state.spentUses = progress?.spentUses || {};
    state.itemMessage = "";
    state.chosenCharacter = progress?.chosenCharacter || null;
    const savedStats = progress?.stats || {};
    state.stats = {};
    for (const def of gamebook.statDefs) {
      state.stats[def.id] = def.id in savedStats ? savedStats[def.id] : def.start;
    }
    if (gamebook.characters.length && !progress?.nodeId) {
      state.selectingCharacter = true;
      renderCharacterSelect();
    } else {
      enterNode(progress?.nodeId || gamebook.metadata.start_node || gamebook.metadata.startNode);
    }
  } catch (error) {
    app.innerHTML = `<p class="error">Could not open this gamebook. ${escapeHtml(error.message)}</p>`;
  }
}

function renderCharacterSelect() {
  const book = state.activeBook;
  const gamebook = state.activePackage;
  title.textContent = book.title;
  const cards = gamebook.characters.map((character) => {
    const img = character.image ? gamebook.imageUrls.get(character.image) : null;
    const statChips = Object.entries(character.stats).map(([id, value]) => {
      const def = (gamebook.statDefs || []).find((d) => d.id === id);
      let text = `${def ? def.label : id} ${value}`;
      if (def && def.ability) { const m = statModifierOf(def, value); text += ` (${m >= 0 ? "+" + m : m})`; }
      return `<span class="chip">${escapeHtml(text)}</span>`;
    }).join("");
    return `
      <button class="plain-button character-card" type="button" data-character="${escapeAttribute(character.id)}">
        ${img ? `<img class="reader-image" src="${escapeAttribute(img)}" alt="">` : ""}
        <h3>${escapeHtml(character.name)}</h3>
        ${character.description ? `<p class="reader-text">${escapeHtml(character.description)}</p>` : ""}
        ${statChips ? `<div class="chip-row">${statChips}</div>` : ""}
      </button>
    `;
  }).join("");
  app.innerHTML = `
    <article class="reader">
      <h2>Choose your character</h2>
      <div class="character-list">${cards}</div>
      <button class="secondary-button" type="button" data-close-reader>Library</button>
    </article>
  `;
  app.querySelectorAll("[data-character]").forEach((btn) => {
    btn.addEventListener("click", () => chooseCharacter(btn.dataset.character));
  });
  app.querySelector("[data-close-reader]")?.addEventListener("click", closeReader);
}

function chooseCharacter(characterId) {
  const gamebook = state.activePackage;
  const character = gamebook.characters.find((c) => c.id === characterId);
  if (!character) return;
  for (const def of gamebook.statDefs) {
    state.stats[def.id] = def.id in character.stats ? character.stats[def.id] : def.start;
  }
  state.equipment = character.equipment
    .map((id) => gamebook.equipment.get(id))
    .filter(Boolean);
  state.equippedBySlot = { ...character.equippedBySlot };
  state.chosenCharacter = character.id;
  state.selectingCharacter = false;
  enterNode(character.startNode || gamebook.metadata.start_node || gamebook.metadata.startNode);
}

function closeReader() {
  state.activePackage = null;
  state.activeBook = null;
  state.currentNodeId = null;
  state.selectingCharacter = false;
  state.view = "library";
  updateNav();
  render();
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

  const imageHtml = renderNodeImages(node, gamebook);
  const inventoryPanel = gamebook.inventoryConfig.enabled
    ? renderGainPanel(gamebook.inventoryConfig.label, state.inventory, gamebook, state.itemMessage)
    : "";
  const equipmentPanel = gamebook.equipmentConfig.enabled
    ? renderEquipmentPanel(gamebook.equipmentConfig.label, state.equipment)
    : "";
  const mapPanel = gamebook.mapConfig.enabled
    ? renderMapPanel(gamebook.mapConfig.label, gamebook)
    : "";
  const evidencePanel = gamebook.evidenceConfig.enabled
    ? renderGainPanel(gamebook.evidenceConfig.label, state.evidence, gamebook)
    : "";
  const bodyHtml = renderNodeBody(node, gamebook);
  const choiceHtml = renderNodeActions(node);
  const statBar = renderStatBar(gamebook);
  const chips = [
    gamebook.inventoryConfig.enabled ? renderCollectionChip(gamebook.inventoryConfig, state.inventory.length) : "",
    gamebook.equipmentConfig.enabled ? renderCollectionChip(gamebook.equipmentConfig, state.equipment.length) : "",
    gamebook.mapConfig.enabled ? renderCollectionChip(gamebook.mapConfig, state.map.length) : "",
    gamebook.evidenceConfig.enabled ? renderCollectionChip(gamebook.evidenceConfig, state.evidence.length) : "",
  ].join("");

  app.innerHTML = `
    <article class="reader">
      ${statBar}
      ${chips ? `<div class="chip-row">${chips}</div>` : ""}
      ${imageHtml}
      ${bodyHtml}
      ${inventoryPanel}
      ${equipmentPanel}
      ${mapPanel}
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

function renderCollectionChip(config, count) {
  const text = config.showCount ? `${config.label} ${count}` : config.label;
  return `<span class="chip">${escapeHtml(text)}</span>`;
}

function renderGainPanel(label, items, gamebook, message = "") {
  if (!items.length) return "";
  const rows = items.map((item) => {
    const summary = `<strong>${escapeHtml(item.title || item.id)}</strong>${item.description ? `<br><span class="muted">${escapeHtml(item.description)}</span>` : ""}`;
    const imageSrc = item.image ? gamebook.imageUrls.get(item.image) : null;
    const detail = item.detail || "";
    const spec = resolveItem(item);
    const useButton = usableEffects(spec)
      ? `<button class="secondary-button" type="button" data-use="${escapeAttribute(item.id)}">${escapeHtml(useLabelOf(spec))}${itemUses(spec) > 1 ? ` (${usesLeft(spec)})` : ""}</button>`
      : "";
    if (!imageSrc && !detail) {
      return `<div class="shop-row"><div>${summary}</div>${useButton}</div>`;
    }
    return `
      <div class="shop-row">
        <details class="collection-item">
          <summary>${summary}</summary>
          ${imageSrc ? `<img class="reader-image" src="${escapeAttribute(imageSrc)}" alt="">` : ""}
          ${detail ? `<p class="reader-text">${escapeHtml(detail)}</p>` : ""}
        </details>
        ${useButton}
      </div>
    `;
  }).join("");
  return `
    <section class="panel">
      <h3>${escapeHtml(label)}</h3>
      ${message ? `<p class="muted">${escapeHtml(message)}</p>` : ""}
      ${rows}
    </section>
  `;
}

// Use data lives in the catalog, so a saved playthrough or an inline grant that
// stored only the bare item still knows how to be used. Same reason
// equipmentItem() exists for looted gear.
function resolveItem(item) {
  return state.activePackage?.inventory?.get(item.id) || item;
}

function usableEffects(item) {
  const raw = item.use || item.use_effects || item.on_use;
  return raw && typeof raw === "object" ? raw : null;
}

function useLabelOf(item) {
  return item.use_label || item.use_verb || "Use";
}

function itemUses(item) {
  const n = item.uses ?? item.charges;
  return typeof n === "number" && n > 0 ? n : 1;
}

function usesLeft(item) {
  return Math.max(0, itemUses(item) - (state.spentUses[item.id] || 0));
}

/// Spend one charge: apply its effects, then drop it once it is used up.
function useItem(itemId) {
  const gamebook = state.activePackage;
  const owned = state.inventory.find((x) => x.id === itemId);
  if (!gamebook || !owned) return;
  const item = resolveItem(owned);
  const effects = usableEffects(item);
  if (!effects || usesLeft(item) <= 0) return;

  const labels = new Map(gamebook.statDefs.map((d) => [d.id, d.label]));
  const gained = [];
  for (const [id, delta] of Object.entries(effects)) {
    if (typeof delta !== "number") continue;
    const before = state.stats[id] ?? 0;
    applyStatDelta(gamebook, id, delta);
    const change = (state.stats[id] ?? 0) - before;
    if (change !== 0) gained.push(`${change > 0 ? "+" : ""}${change} ${labels.get(id) || id}`);
  }

  const spent = (state.spentUses[item.id] || 0) + 1;
  state.spentUses[item.id] = spent;
  if (spent >= itemUses(item)) {
    state.inventory = state.inventory.filter((x) => x.id !== item.id);
  }
  const effect = gained.length ? ` (${gained.join(", ")})` : "";
  state.itemMessage = item.use_text ? `${item.use_text}${effect}` : `${useLabelOf(item)} ${item.title || item.id}.${effect}`;
  render();
}

function renderEquipmentPanel(label, items) {
  if (!items.length) return "";
  const rows = items.map((item) => {
    const slot = item.slot || item.equip_slot;
    const equipped = slot && state.equippedBySlot[slot] === item.id;
    const summary = `<strong>${escapeHtml((item.title || item.id) + (equipped ? " (equipped)" : ""))}</strong>${item.description ? `<br><span class="muted">${escapeHtml(item.description)}</span>` : ""}`;
    const button = slot
      ? `<button class="secondary-button" type="button" data-equip="${escapeAttribute(item.id)}">${equipped ? "Unequip" : "Equip"}</button>`
      : "";
    return `<div class="shop-row"><div>${summary}</div>${button}</div>`;
  }).join("");
  return `<section class="panel"><h3>${escapeHtml(label)}</h3>${rows}</section>`;
}

function renderMapPanel(label, gamebook) {
  const order = gamebook.mapOrder.length ? gamebook.mapOrder : state.map;
  const fragments = order.filter((id) => state.map.includes(id));
  if (!fragments.length) return "";
  const rows = fragments.map((id) => {
    const fragment = gamebook.mapCatalog.get(id);
    if (!fragment) return "";
    const src = fragment.image ? gamebook.imageUrls.get(fragment.image) : null;
    return `
      ${fragment.title ? `<p class="reader-text"><strong>${escapeHtml(fragment.title)}</strong></p>` : ""}
      ${src ? `<img class="reader-image" src="${escapeAttribute(src)}" alt="">` : ""}
      ${fragment.description ? `<p class="muted">${escapeHtml(fragment.description)}</p>` : ""}
    `;
  }).join("");
  return `<section class="panel"><h3>${escapeHtml(label)}</h3>${rows}</section>`;
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
      requires: location.requires || location.condition,
      locked_text: location.locked_text,
    })));
  }

  if (node.type === "battle") {
    return `<button class="primary-button" type="button" data-resolve-battle>Resolve</button>`;
  }

  if (node.type === "check") {
    return renderCheck(node);
  }

  if (node.type === "combat") {
    return renderCombat(node);
  }

  if (node.type === "shop" || node.type === "store") {
    return renderShop(node);
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
  const visible = choices.filter((c) => meetsRequirement(c.requires || c.condition || c.if) || c.locked_text);
  return `
    <div class="choice-list">
      ${visible.map((choice) => {
        const ok = meetsRequirement(choice.requires || choice.condition || choice.if);
        if (!ok) {
          return `<button class="choice" type="button" disabled>${escapeHtml(choice.locked_text)}</button>`;
        }
        return `
        <button class="choice" type="button" data-target="${escapeAttribute(choice.target || "")}">
          ${escapeHtml(choice.text || choice.title || "Continue")}
        </button>`;
      }).join("")}
    </div>
  `;
}

function renderCheck(node) {
  const dice = node.dice || node.roll || "1d20";
  const need = num(node.target ?? node.difficulty ?? node.dc ?? node.against, 10);
  const result = state.checkResult;
  if (!result) {
    return `
      <section class="panel">
        <p class="muted">Roll ${escapeHtml(dice)}${escapeHtml(bonusText(node.modifier ?? node.bonus))} — need ${need}+</p>
      </section>
      <button class="primary-button" type="button" data-roll-check>Roll</button>
    `;
  }
  return `
    <section class="panel">
      <p><strong>${escapeHtml(result.label)}</strong></p>
      <p class="muted">Rolled ${result.rolls.join(" + ")}${escapeHtml(bonusText(result.bonus))} = ${result.total} vs ${result.need}</p>
    </section>
    ${renderChoices([{ text: "Continue", target: result.next }])}
  `;
}

function renderShop(node) {
  const gamebook = state.activePackage;
  const currencyStat = node.currency_stat || node.currency || node.cost_stat || "gold";
  const funds = state.stats[currencyStat] || 0;
  let currencyLabel = "Gold";
  const def = (gamebook.statDefs || []).find((d) => d.id === currencyStat);
  if (def) currencyLabel = def.label;
  const items = node.items || node.stock || node.wares || [];
  const rows = items.map((entry, index) => {
    const equipmentId = entry.equipment || entry.gear;
    const inventoryId = entry.inventory || entry.item || entry.items;
    const isEquip = Boolean(equipmentId);
    const id = equipmentId || inventoryId || entry.id;
    const catalog = isEquip ? gamebook.equipment : gamebook.inventory;
    const item = catalog.get(id);
    if (!item) return "";
    const price = num(entry.price ?? entry.cost, 0);
    const owned = (isEquip ? state.equipment : state.inventory).some((existing) => existing.id === id);
    const summary = `<strong>${escapeHtml(item.title || id)}</strong>${item.description ? `<br><span class="muted">${escapeHtml(item.description)}</span>` : ""}`;
    const control = owned
      ? `<span class="muted">Owned</span>`
      : `<button class="primary-button" type="button" data-buy="${index}" ${funds >= price ? "" : "disabled"}>${price} ${escapeHtml(currencyLabel)}</button>`;
    return `<div class="shop-row"><div>${summary}</div>${control}</div>`;
  }).join("");
  const returnTarget = node.return_target || node.return_to || node.leave_target || node.done_target;
  return `
    <section class="panel">
      <p><strong>${escapeHtml(currencyLabel)}: ${funds}</strong></p>
      ${rows}
      ${state.shopMessage ? `<p class="muted">${escapeHtml(state.shopMessage)}</p>` : ""}
    </section>
    ${returnTarget ? renderChoices([{ text: node.leave_label || "Leave", target: returnTarget }]) : ""}
  `;
}

function buyShopItem(node, index) {
  const gamebook = state.activePackage;
  const currencyStat = node.currency_stat || node.currency || node.cost_stat || "gold";
  const items = node.items || node.stock || node.wares || [];
  const entry = items[index];
  if (!entry) return;
  const equipmentId = entry.equipment || entry.gear;
  const inventoryId = entry.inventory || entry.item || entry.items;
  const isEquip = Boolean(equipmentId);
  const id = equipmentId || inventoryId || entry.id;
  const catalog = isEquip ? gamebook.equipment : gamebook.inventory;
  const item = catalog.get(id);
  if (!item) return;
  const price = num(entry.price ?? entry.cost, 0);
  const funds = state.stats[currencyStat] || 0;
  if (funds < price) {
    state.shopMessage = `Not enough to buy ${item.title || id}.`;
    renderReader();
    return;
  }
  state.stats[currencyStat] = funds - price;
  const target = isEquip ? state.equipment : state.inventory;
  if (!target.some((existing) => existing.id === id)) target.push(item);
  state.shopMessage = `Bought ${item.title || id}.`;
  saveProgress();
  renderReader();
}

function renderCombat(node) {
  const enemy = node.enemy || node.monster || {};
  const label = enemy.label || enemy.name || node.enemy_label || "Enemy";
  const enemyHp = state.enemyHp == null ? num(enemy.hp ?? enemy.health, 1) : state.enemyHp;
  const logHtml = state.combatLog.map((entry) => `<p class="muted">${escapeHtml(entry)}</p>`).join("");
  const panel = `
    <section class="panel">
      <div class="combat-head"><strong>${escapeHtml(label)}</strong><span class="muted">HP ${enemyHp}</span></div>
      ${logHtml}
    </section>
  `;
  if (state.combatEnd != null) {
    return `${panel}${renderChoices([{ text: "Continue", target: state.combatEnd }])}`;
  }
  const talk = node.talk_target;
  const flee = node.flee_target || node.run_target || node.escape_target;
  return `
    ${panel}
    <div class="choice-list">
      <button class="primary-button" type="button" data-combat-attack>Attack</button>
      ${talk ? `<button class="secondary-button" type="button" data-target="${escapeAttribute(talk)}">${escapeHtml(node.talk_label || "Talk")}</button>` : ""}
      ${flee ? `<button class="secondary-button" type="button" data-combat-flee>Run away</button>` : ""}
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

  app.querySelector("[data-roll-check]")?.addEventListener("click", () => rollCheck(node));
  app.querySelector("[data-combat-attack]")?.addEventListener("click", () => combatRound(node));
  app.querySelector("[data-combat-flee]")?.addEventListener("click", () => fleeCombat(node));
  app.querySelectorAll("[data-use]").forEach((button) => {
    button.addEventListener("click", () => useItem(button.dataset.use));
  });
  app.querySelectorAll("[data-equip]").forEach((button) => {
    button.addEventListener("click", () => toggleEquip(button.dataset.equip));
  });
  app.querySelectorAll("[data-buy]").forEach((button) => {
    button.addEventListener("click", () => buyShopItem(node, Number(button.dataset.buy)));
  });
}

function moveToNode(target) {
  if (!target) return;
  enterNode(target);
}

// Enters a node once: applies its gains and stat changes, resets per-node
// combat/check state, then renders. Re-renders within a node (combat rounds)
// call renderReader directly so gains are not re-applied.
function enterNode(nodeId) {
  const gamebook = state.activePackage;
  if (!gamebook) return;
  state.lastBattle = null;
  state.checkResult = null;
  state.itemMessage = "";
  state.combatLog = [];
  state.combatEnd = null;
  state.enemyHp = null;
  state.shopMessage = "";
  state.currentNodeId = nodeId;
  const node = gamebook.nodes.get(nodeId);
  if (node) {
    applyNodeGains(node, gamebook);
    if (node.type === "combat") {
      const enemy = node.enemy || node.monster || {};
      state.enemyHp = num(enemy.hp ?? enemy.health ?? enemy.hit_points, 1);
    }
  }
  saveProgress();
  renderReader();
}

function applyNodeGains(node, gamebook) {
  collectItems(node.items || node.inventory || node.gain_inventory || node.gains_inventory, gamebook.inventory, state.inventory);
  collectItems(node.evidence || node.gain_evidence || node.gains_evidence, gamebook.evidence, state.evidence);
  collectItems(node.equipment || node.gain_equipment || node.gains_equipment || node.gear, gamebook.equipment, state.equipment);
  revealMap(node);
  applyFlags(node);
  applyStatChanges(node, gamebook);
}

function applyFlags(node) {
  const set = node.set_flags || node.set_flag || node.flags;
  for (const f of (Array.isArray(set) ? set : (typeof set === "string" ? [set] : []))) {
    if (!state.flags.includes(f)) state.flags.push(f);
  }
  const clear = node.clear_flags || node.clear_flag;
  for (const f of (Array.isArray(clear) ? clear : (typeof clear === "string" ? [clear] : []))) {
    state.flags = state.flags.filter((x) => x !== f);
  }
}

function asList(raw) {
  if (typeof raw === "string" && raw.trim()) return [raw.trim()];
  if (Array.isArray(raw)) return raw.filter((v) => typeof v === "string" && v.trim());
  return [];
}

// True when every predicate on a choice's `requires` block currently holds.
function meetsRequirement(req) {
  if (!req || typeof req !== "object") return true;
  const hasIn = (list, arr) => asList(list).every((id) => arr.some((x) => (x.id || x) === id));
  if (!hasIn(req.item || req.items || req.has_item, state.inventory)) return false;
  if (asList(req.not_item || req.without_item || req.missing_item)
        .some((id) => state.inventory.some((x) => x.id === id))) return false;
  if (!hasIn(req.equipment || req.gear, state.equipment)) return false;
  if (!asList(req.equipped).every((id) => Object.values(state.equippedBySlot).includes(id))) return false;
  if (!hasIn(req.evidence, state.evidence)) return false;
  if (!asList(req.flag || req.flags || req.has_flag).every((f) => state.flags.includes(f))) return false;
  if (asList(req.not_flag || req.not_flags || req.without_flag).some((f) => state.flags.includes(f))) return false;
  const chars = asList(req.character || req.characters || req.class);
  if (chars.length && !chars.includes(state.chosenCharacter)) return false;
  if (asList(req.not_character || req.not_characters || req.not_class)
        .includes(state.chosenCharacter)) return false;
  const statBlock = req.stat || req.stats;
  if (statBlock && typeof statBlock === "object") {
    for (const [id, cond] of Object.entries(statBlock)) {
      const value = effectiveStat(id);
      if (typeof cond === "number") { if (value < cond) return false; continue; }
      if (!cond || typeof cond !== "object") continue;
      const gt = cond.gt ?? cond.greater_than ?? cond.above;
      const lt = cond.lt ?? cond.less_than ?? cond.below;
      const min = cond.min ?? cond.at_least ?? cond.gte ?? (typeof gt === "number" ? gt + 1 : undefined);
      const max = cond.max ?? cond.at_most ?? cond.lte ?? (typeof lt === "number" ? lt - 1 : undefined);
      const eq = cond.equals ?? cond.eq ?? cond.is;
      if (typeof min === "number" && value < min) return false;
      if (typeof max === "number" && value > max) return false;
      if (typeof eq === "number" && value !== eq) return false;
    }
  }
  return true;
}

function revealMap(node) {
  const raw = node.reveal_map || node.map_reveal || node.reveals_map || node.reveal_fragment;
  if (!raw) return;
  const ids = Array.isArray(raw) ? raw : [raw];
  for (const id of ids) {
    if (typeof id === "string" && id && !state.map.includes(id)) state.map.push(id);
  }
}

// Resolves an equipment item by id from collected items (which carry the full
// object for looted, inline-granted gear) or the top-level catalog.
function equipmentItem(id) {
  return state.equipment.find((item) => item.id === id) || state.activePackage?.equipment.get(id) || null;
}

// An ability score's tiered modifier, or a plain stat's value.
function statModifierOf(def, value) {
  if (!def || !def.ability) return value;
  for (const b of def.modifierTable) {
    if (value >= b.min && value <= b.max) return b.mod;
  }
  if (value <= 3) return -3;
  if (value <= 5) return -2;
  if (value <= 8) return -1;
  if (value <= 12) return 0;
  if (value <= 15) return 1;
  if (value <= 17) return 2;
  return 3;
}

function statBonus(statId) {
  const def = (state.activePackage.statDefs || []).find((d) => d.id === statId);
  return statModifierOf(def, effectiveStat(statId));
}

function effectiveStat(statId) {
  let value = state.stats[statId] || 0;
  for (const itemId of Object.values(state.equippedBySlot)) {
    const item = equipmentItem(itemId);
    const effects = item && (item.equip_effects || item.effects || item.while_equipped);
    if (effects && typeof effects[statId] === "number") value += effects[statId];
  }
  return value;
}

function equippedWeapon() {
  for (const itemId of Object.values(state.equippedBySlot)) {
    const item = equipmentItem(itemId);
    if (item && (item.damage || item.damage_dice)) return item;
  }
  return null;
}

function toggleEquip(itemId) {
  const item = equipmentItem(itemId);
  const slot = item && (item.slot || item.equip_slot);
  if (!slot) return;
  if (state.equippedBySlot[slot] === itemId) {
    delete state.equippedBySlot[slot];
  } else {
    state.equippedBySlot[slot] = itemId;
  }
  saveProgress();
  renderReader();
}

/// Write a stat, clamped to [0, max]. The single place stat values are set.
function setStatClamped(gamebook, id, value) {
  const def = (gamebook.statDefs || []).find((d) => d.id === id);
  let next = value;
  if (def && def.max != null && next > def.max) next = def.max;
  if (next < 0) next = 0;
  state.stats[id] = next;
}

function applyStatDelta(gamebook, id, delta) {
  setStatClamped(gamebook, id, (state.stats[id] || 0) + delta);
}

function applyStatChanges(node, gamebook) {
  const setStat = (id, value) => setStatClamped(gamebook, id, value);
  const add = node.stat_changes || node.adjust_stats || node.gain_stats;
  if (add && typeof add === "object") {
    for (const [id, value] of Object.entries(add)) {
      if (typeof value === "number") setStat(id, (state.stats[id] || 0) + value);
    }
  }
  const set = node.set_stats;
  if (set && typeof set === "object") {
    for (const [id, value] of Object.entries(set)) {
      if (typeof value === "number") setStat(id, value);
    }
  }
}

function renderStatBar(gamebook) {
  const defs = (gamebook.statDefs || []).filter((def) => !def.hidden);
  if (!defs.length) return "";
  return `<div class="chip-row">${defs.map((def) => {
    const value = effectiveStat(def.id);
    let text;
    if (def.ability) {
      const mod = statModifierOf(def, value);
      text = `${def.label} ${value} (${mod >= 0 ? "+" + mod : mod})`;
    } else {
      text = def.max != null ? `${def.label} ${value}/${def.max}` : `${def.label} ${value}`;
    }
    return `<span class="chip">${escapeHtml(text)}</span>`;
  }).join("")}</div>`;
}

function rollDiceList(expression) {
  const match = String(expression).trim().match(/^(\d*)d(\d+)$/i);
  const count = Math.max(1, Number(match?.[1] || 1));
  const sides = Math.max(2, Number(match?.[2] || 6));
  const rolls = [];
  for (let index = 0; index < count; index += 1) {
    rolls.push(Math.floor(Math.random() * sides) + 1);
  }
  return rolls;
}

function d20() {
  return Math.floor(Math.random() * 20) + 1;
}

function bonusText(bonus) {
  const value = Number(bonus || 0);
  if (value === 0) return "";
  return value > 0 ? ` +${value}` : ` ${value}`;
}

function resolveHealthId(node) {
  if (node.health_stat && node.health_stat in state.stats) return node.health_stat;
  const def = (state.activePackage.statDefs || []).find((entry) => entry.role === "health");
  return def ? def.id : (node.health_stat || "hp");
}

function rollCheck(node) {
  const dice = node.dice || node.roll || "1d20";
  const modifier = num(node.modifier ?? node.bonus, 0);
  const statMod = node.stat_modifier || node.modifier_stat || node.stat;
  const bonus = statMod ? statBonus(statMod) : 0;
  const rolls = rollDiceList(dice);
  const total = rolls.reduce((sum, value) => sum + value, 0) + modifier + bonus;
  const need = num(node.target ?? node.difficulty ?? node.dc ?? node.against, 10);
  const success = total >= need;
  state.checkResult = {
    rolls,
    bonus: modifier + statBonus,
    total,
    need,
    label: success ? (node.success_label || "Success") : (node.failure_label || "Failure"),
    next: success
      ? (node.success_target || node.on_success || node.pass_target)
      : (node.failure_target || node.on_failure || node.fail_target || node.default_target),
  };
  renderReader();
}

function combatRound(node) {
  const enemy = node.enemy || node.monster || {};
  const player = node.player || {};
  const label = enemy.label || enemy.name || node.enemy_label || "Enemy";
  const healthId = resolveHealthId(node);
  let enemyHp = state.enemyHp == null ? num(enemy.hp ?? enemy.health, 1) : state.enemyHp;
  let playerHp = state.stats[healthId] ?? 0;
  const log = [];

  const weapon = equippedWeapon();
  const hitStat = player.hit_stat || player.to_hit_stat || node.hit_stat;
  const damageStat = player.damage_stat || node.damage_stat;
  const hitMod = hitStat ? statBonus(hitStat) : 0;
  const damageMod = damageStat ? statBonus(damageStat) : 0;
  const enemyHitTarget = num(enemy.hit_target ?? enemy.to_hit ?? enemy.ac ?? enemy.armor_class, 10);
  const playerHitBonus = num(player.hit_bonus ?? player.to_hit_bonus, 0) + (weapon ? num(weapon.hit_bonus ?? weapon.to_hit_bonus, 0) : 0) + hitMod;
  const playerRoll = d20();
  if (playerRoll + playerHitBonus >= enemyHitTarget) {
    const damageDice = (weapon && (weapon.damage || weapon.damage_dice)) || player.damage || player.damage_dice || node.player_damage || "1d6";
    const damageBonus = num(player.damage_bonus ?? player.bonus, 0) + (weapon ? num(weapon.damage_bonus, 0) : 0) + damageMod;
    const dmg = rollDiceList(damageDice).reduce((sum, value) => sum + value, 0) + damageBonus;
    enemyHp -= dmg;
    log.push(`You hit (rolled ${playerRoll}) for ${dmg} damage.`);
  } else {
    log.push(`You miss (rolled ${playerRoll}).`);
  }

  if (enemyHp > 0) {
    const armorStat = node.armor_stat || node.defense_stat;
    const hitsOn = armorStat ? effectiveStat(armorStat) : num(enemy.hits_on ?? enemy.hit_you_on ?? enemy.attack_target, 11);
    const attackBonus = num(enemy.attack_bonus ?? enemy.hit_bonus ?? node.enemy_attack_bonus, 0);
    const enemyRoll = d20();
    if (enemyRoll + attackBonus >= hitsOn) {
      const dmg = rollDiceList(enemy.damage || "1d6").reduce((sum, value) => sum + value, 0);
      playerHp -= dmg;
      log.push(`${label} hits (rolled ${enemyRoll}) for ${dmg} damage.`);
    } else {
      log.push(`${label} misses (rolled ${enemyRoll}).`);
    }
  }

  if (enemyHp < 0) enemyHp = 0;
  if (playerHp < 0) playerHp = 0;
  state.enemyHp = enemyHp;
  state.stats[healthId] = playerHp;
  state.combatLog = log;
  if (enemyHp <= 0) {
    state.combatEnd = node.win_target || node.on_win || node.victory_target;
  } else if (playerHp <= 0) {
    state.combatEnd = node.lose_target || node.on_lose || node.death_target || node.defeat_target;
  }
  saveProgress();
  renderReader();
}

function fleeCombat(node) {
  const enemy = node.enemy || node.monster || {};
  const label = enemy.label || enemy.name || node.enemy_label || "Enemy";
  const fleeTarget = node.flee_target || node.run_target || node.escape_target;
  if (!fleeTarget) return;
  const healthId = resolveHealthId(node);
  let playerHp = state.stats[healthId] ?? 0;
  const armorStat = node.armor_stat || node.defense_stat;
  const hitsOn = armorStat ? effectiveStat(armorStat) : num(enemy.hits_on ?? enemy.hit_you_on ?? enemy.attack_target, 11);
  const attackBonus = num(enemy.attack_bonus ?? enemy.hit_bonus ?? node.enemy_attack_bonus, 0);
  const enemyRoll = d20();
  const log = [];
  if (enemyRoll + attackBonus >= hitsOn) {
    const dmg = rollDiceList(enemy.damage || "1d6").reduce((sum, value) => sum + value, 0);
    playerHp -= dmg;
    log.push(`${label} strikes as you flee for ${dmg} damage.`);
  } else {
    log.push("You slip away cleanly.");
  }
  if (playerHp < 0) playerHp = 0;
  state.stats[healthId] = playerHp;
  saveProgress();
  if (playerHp <= 0) {
    state.combatLog = log;
    state.combatEnd = node.lose_target || node.death_target;
    renderReader();
  } else {
    moveToNode(fleeTarget);
  }
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

  const nodes = Array.isArray(story.nodes) ? story.nodes : [];
  const inventoryCatalog = catalogMap(story.inventory);
  const evidenceCatalog = catalogMap(story.evidence);
  const equipmentCatalog = catalogMap(story.equipment);
  const mapCatalog = catalogMap(story.map || story.map_fragments || story.maps);
  const hasInventory = inventoryCatalog.size > 0
    || nodes.some((node) => node.items || node.inventory || node.gain_inventory || node.gains_inventory);
  const hasEvidence = evidenceCatalog.size > 0
    || nodes.some((node) => node.evidence || node.gain_evidence || node.gains_evidence);
  const hasEquipment = equipmentCatalog.size > 0
    || nodes.some((node) => node.equipment || node.gain_equipment || node.gains_equipment || node.gear);
  const hasMap = mapCatalog.size > 0
    || nodes.some((node) => node.reveal_map || node.map_reveal || node.reveals_map || node.reveal_fragment);
  const collections = story.collections || {};

  const systems = story.systems || {};
  return {
    metadata: story.metadata || {},
    nodes: new Map(nodes.map((node) => [node.id, node])),
    inventory: inventoryCatalog,
    evidence: evidenceCatalog,
    equipment: equipmentCatalog,
    mapCatalog: mapCatalog,
    mapOrder: (Array.isArray(story.map || story.map_fragments || story.maps) ? (story.map || story.map_fragments || story.maps) : [])
      .filter((entry) => entry && entry.id).map((entry) => entry.id),
    inventoryConfig: normalizeCollectionConfig(collections.inventory || collections.items, "Items", hasInventory),
    evidenceConfig: normalizeCollectionConfig(collections.evidence, "Evidence", hasEvidence),
    equipmentConfig: normalizeCollectionConfig(collections.equipment || collections.gear, "Equipment", hasEquipment),
    mapConfig: normalizeCollectionConfig(collections.map, "Map", hasMap),
    statDefs: normalizeStats(story.stats || systems.stats),
    characters: (Array.isArray(story.characters) ? story.characters : [])
      .filter((c) => c && c.id)
      .map((c) => ({
        id: String(c.id),
        name: c.name || c.title || c.label || c.id,
        description: c.description || "",
        image: c.image || null,
        stats: (c.stats && typeof c.stats === "object") ? c.stats : {},
        equipment: Array.isArray(c.equipment || c.gear) ? (c.equipment || c.gear) : [],
        equippedBySlot: (c.equipped && typeof c.equipped === "object") ? c.equipped : {},
        startNode: c.start_node || c.startNode || null,
      })),
    imageUrls,
  };
}

function normalizeStats(raw) {
  if (!Array.isArray(raw)) return [];
  return raw
    .filter((entry) => entry && typeof entry === "object" && entry.id)
    .map((entry) => {
      const roleText = entry.role
        || ((entry.is_health || entry.health) ? "health" : ((entry.is_armor || entry.armor) ? "armor" : "normal"));
      const label = entry.label || entry.name || entry.title || displayTitle(entry.id);
      const max = firstNum(entry, ["max", "maximum"]);
      const tableRaw = entry.modifier_table || entry.modifiers;
      const modifierTable = (Array.isArray(tableRaw) ? tableRaw : []).map((b) => ({
        min: num(b.min ?? b.from, 0), max: num(b.max ?? b.to, 0), mod: num(b.mod ?? b.modifier ?? b.bonus, 0),
      }));
      return {
        id: String(entry.id),
        label: String(label),
        start: firstNum(entry, ["start", "value", "initial", "default"]) ?? 0,
        max: max,
        role: roleText === "health" ? "health" : (roleText === "armor" ? "armor" : "normal"),
        hidden: entry.hidden === true,
        ability: entry.ability === true || entry.ability_score === true || modifierTable.length > 0,
        modifierTable,
      };
    });
}

function displayTitle(id) {
  return String(id)
    .replace(/[_-]+/g, " ")
    .split(" ")
    .filter(Boolean)
    .map((word) => word[0].toUpperCase() + word.slice(1))
    .join(" ");
}

function firstNum(obj, keys) {
  for (const key of keys) {
    if (typeof obj[key] === "number") return obj[key];
  }
  return undefined;
}

function num(value, fallback = 0) {
  return typeof value === "number" ? value : fallback;
}

function normalizeCollectionConfig(raw, defaultLabel, hasEntries) {
  const config = raw && typeof raw === "object" ? raw : {};
  const enabled = config.enabled ?? config.include ?? hasEntries;
  return {
    label: String(config.label || config.title || config.name || defaultLabel),
    showCount: Boolean(config.show_count ?? config.showCount ?? true),
    enabled: Boolean(enabled),
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
    equipment: state.equipment,
    equippedBySlot: state.equippedBySlot,
    map: state.map,
    flags: state.flags,
    stats: state.stats,
    spentUses: state.spentUses,
    chosenCharacter: state.chosenCharacter,
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
