# examples/

Runnable example scripts. Each script prints Telegraph page URLs on completion.
Paste a URL into any Telegram chat to open it as Instant View.

---

## Prerequisites

### 1. Get a Telegraph access_token directly

No registration needed — create an account in one curl call:

```bash
curl -s "https://api.telegra.ph/createAccount?short_name=MyBot" | python3 -m json.tool
```

Copy the `access_token` from the response. You will pass it to each example.

> **Keep it safe.** The token is permanent unless you call `revokeAccessToken`.
> It is not tied to any Telegram account unless you visit the `auth_url` and
> log in with Telegram.

### 2. Dependencies

All examples use the project-level `deps.edn`. Run from the project root.

---

## Example scripts

### 01\_account.clj — Account management

Get a Telegraph access_token

```bash
clojure -M examples/01_account.clj MyBotName
```

Covers: `create-account!` · `get-account!` · `edit-account!` ·
`safe-create-account!` · `revoke-token!`

---

### 02\_node\_helpers.clj — All node helpers

```bash
clojure -M examples/02_node_helpers.clj YOUR_ACCESS_TOKEN
```

Builds a page with every node constructor and publishes it. Printed URL opens
as Instant View in the Telegram mobile app.

Covers: `p` `h3` `h4` `b` `em` `i` `s` `u` `code` `pre` `link` `img`
`figure` `figcaption` `iframe` `ul` `ol` `li` `blockquote` `aside` `hr`

---

### 03\_pages.clj — Page CRUD

```bash
clojure -M examples/03_pages.clj YOUR_ACCESS_TOKEN
```

Covers: `create-page!` · `edit-page!` · `get-page!` · `get-page-list!` ·
`safe-create-page!`

---

### 04\_views.clj — View statistics

```bash
# auto-creates a test page
clojure -M examples/04_views.clj YOUR_ACCESS_TOKEN

# query an existing page
clojure -M examples/04_views.clj YOUR_ACCESS_TOKEN My-Article-01-01
```

Covers: `get-views!` total / by year / by month · `safe-get-views!`

---

### 05\_html\_to\_nodes.clj — HTML conversion

```bash
# built-in HTML sample
clojure -M examples/05_html_to_nodes.clj YOUR_ACCESS_TOKEN

# fetch a real URL and publish
clojure -M examples/05_html_to_nodes.clj YOUR_ACCESS_TOKEN https://example.com/article
```

Covers: `html->nodes` · `:base-url` for relative link resolution ·
fetching and publishing external articles

---

### 06\_error\_handling.clj — Error handling

```bash
clojure -M examples/06_error_handling.clj YOUR_ACCESS_TOKEN
```

Covers: `try/catch ExceptionInfo` · `safe-*` variants · retry helper ·
bulk create with per-item result collection

---

### 07\_telegram\_bot\_integration.clj — Telegram bot

> Requires extra dependencies — see below.

```bash
clojure -M examples/07_telegram_bot_integration.clj BOT_TOKEN TELEGRAPH_TOKEN
```

User sends a URL → bot fetches the page → publishes to Telegraph →
replies with the Instant View link.

**Additional deps** (add to `deps.edn` `:deps`):

```clojure
com.github.marksto/clj-tg-bot-api {:mvn/version "0.9.2"}
com.github.oliyh/martian-clj-http {:mvn/version "0.1.28"}
```

**Get a bot token**: open [@BotFather](https://t.me/BotFather) in Telegram,
send `/newbot`, and copy the token.

---

## Quick start

```bash
# Step 1 — get a token (one time)
curl -s "https://api.telegra.ph/createAccount?short_name=MyBot" | python3 -m json.tool
# copy access_token

# Step 2 — publish a demo page
clojure -M examples/02_node_helpers.clj <access_token>
# prints: URL: https://telegra.ph/Node-Helpers-Demo-...

# Step 3 — send that URL in any Telegram chat
# The Telegram mobile app shows an "Instant View" button below the link preview.
```

> **Note**: telegra.ph URLs open as a normal webpage in a browser.
> Instant View only appears in the Telegram mobile app (not Telegram Desktop).
