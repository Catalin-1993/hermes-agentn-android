# Plan: aplicația Android ca versiune mobilă a proiectului desktop

Stare la 20 nov 2026. Analiză făcută pe `Catalin-1993/hermes-agent` (desktop, clona locală `Mac/`, ramura
`bot/js-autofix`, bazat pe Nous Research `main` din 17–18 nov) și pe fork-ul Android
(`codex/termux-five-goals`). Nimic din plan nu e implementat încă.

## 1. Ce am găsit

### Desktop
- Electron + renderer React (`apps/desktop`), peste un **gateway Python** (același nucleu Hermes). Desktop nu
  reimplementează agentul: sesiunile, uneltele și apelurile de model stau în backend.
- Backend-ul expune deja tot ce are nevoie un client mobil, în `gateway/platforms/api_server.py`:
  - `GET/POST/PATCH/DELETE /api/sessions`, `/messages`, `/fork`, `/chat`, `/chat/stream`, `/model`
  - `POST /v1/runs`, `GET /v1/runs/{id}/events` (SSE), `/approval`, `/steer`, `/stop`
  - `GET /v1/skills`, `/v1/toolsets`, `/v1/capabilities`, `/v1/models`, `/api/model/options`
  - `GET/POST/PATCH/DELETE /api/jobs` (+ pause, resume, run) = rutine/cron
  - același set și sub `/p/{profile}/…` (profiluri)
  - autentificare cu `Authorization: Bearer <API_SERVER_KEY>` (`API_SERVER_ENABLED=true`).
- Desktop se conectează la backenduri în patru moduri: **local**, **remote** (URL + autentificare),
  **cloud** (OAuth, agenți din cont) și **SSH**. Registrul de conexiuni e per mașină, profilul e ales de pe gateway.
- Există deja „Web Access" (Webapp pe portul 9120 sau URL HTTPS propriu, ex. Tailscale) și `hermes peer add`
  (gateway-uri care vorbesc între ele cu `API_SERVER_KEY`).
- Paginile desktop: chat, sesiuni importabile, Command Center, Capabilities (skills, toolsets, MCP, pluginuri),
  Messaging (Telegram/Discord etc.), Webhooks, Artifacts, Cron, Profiles, Agents/Bots (grupuri, voci, rutine,
  bot-to-bot), Starmap/Memory Graph, Settings (modele, provideri, chei, modele locale, memorie, backup,
  notificări, terminal backend, computer use, browser, actualizări).

### Android
- Rulează **propriul** runtime Hermes (Python în aplicație), cu un instantaneu **mult mai vechi** al nucleului:
  față de `main`-ul Nous lipsesc 221 din 478 de fișiere `agent/`, 450 din `hermes_cli/`, 288 din `gateway/`.
- Chat-ul vorbește doar cu runtime-ul local sau direct cu provideri (OpenAI-compatibil). **Nu există mod
  „gateway la distanță"** și clientul nu cunoaște `/api/sessions`, `/v1/runs`, `/api/jobs`.
- Există invers: aplicația își expune API-ul pe loopback/LAN (`AgentEndpointCard`), deci telefonul poate fi
  și **server** pentru desktop.
- Are deja: Library, Calendar, agenți + grupuri, modele locale (llama.cpp, LiteRT), skills, MCP, memorie,
  unelte de telefon (accesibilitate, shell, Tasker), aprobări de modificări, voce locală (Parakeet).

## 2. Decizia de arhitectură (hotărâtă de proprietar, 20 nov)

**Telefonul e o aplicație completă, de sine stătătoare. Desktopul e opțional.** Aplicația nu devine o
telecomandă: agentul rulează **local pe telefon**, implicit și întotdeauna disponibil, cu limitările Android.
Telecomanda există doar ca mod ales, cât timp ești conectat la desktop.

| Mod | Cine rulează agentul | Când |
|---|---|---|
| **Local** (implicit) | runtime-ul din telefon | oricând, inclusiv fără rețea și fără desktop |
| Conectat la desktop | gateway-ul desktop; telefonul e telecomandă | doar când alegi tu |
| Peer (ulterior) | ambele; își trimit mesaje | agenți care folosesc unelte de pe celălalt dispozitiv |

Reguli:
1. **Nimic nu pornește conectat fără întrebare.** Când aplicația găsește un desktop (Wi-Fi/Tailscale/QR),
   întreabă: „Te conectezi la desktop sau rămâi local?". Alegerea se poate ține minte per desktop
   („Întreabă mereu" / „Rămân local" / „Conectează-mă").
2. **Fiecare conversație are modul ei** (Local sau Desktop), afișat discret. O conversație locală nu se mută
   singură pe desktop și invers; se poate continua pe celălalt mod doar prin acțiune explicită (de ex. „Continuă
   pe desktop").
3. **Dacă desktopul dispare** în mijlocul unei conversații de tip Desktop, aplicația spune clar că s-a
   pierdut conexiunea și oferă „Continuă local"; nu schimbă modul pe tăcute.
4. **Funcțiile aduse din desktop trebuie să existe și local**, nu doar prin telecomandă (vezi secțiunea 4 și 6).
   Pe desktop se pot folosi și prin conectare; pe telefon, local, cu ce permite Android.
5. Telefonul nu face sincronizare bidirecțională de baze de date. Conversațiile Desktop se citesc de pe
   gateway (cache pentru citire offline); cele Locale rămân pe telefon.

## 3. Cum se conectează (faza 1)

1. **Pairing prin QR.** Desktop primește un buton „Asociază telefon" (Settings → Web Access): arată un QR cu
   `{url, cheie de un singur folos, nume gateway}`. Telefonul scanează, schimbă cheia pe un token de
   dispozitiv, îl salvează în `SecureSecretsStore`. Revocabil din desktop.
2. **Adresa.** Același Wi-Fi (`http://<ip>:<port>`) cu avertisment că nu e criptat, sau URL HTTPS propriu
   (Tailscale Serve/tunel). Fără port expus pe internet; recomandarea scrisă în aplicație: Tailscale.
3. **Client nou** `RemoteGatewayClient` (Kotlin) peste clientul SSE existent, cu `/api/sessions`,
   `/v1/runs` + `/events`, `/approval`, `/steer`, `/stop`, `/api/jobs`, `/v1/skills`, `/v1/toolsets`.
4. **Starea conexiunii** vizibilă în antet (verde/galben/roșu), reconectare automată, trecere pe local
   când gateway-ul nu răspunde, fără să piardă mesajul netrimis.
5. **Profiluri**: selector pe baza profilurilor descoperite pe gateway (`/p/{profile}/`).

Livrabil: telefonul se asociază cu desktopul, vezi sesiunile de pe desktop, continui o conversație pe telefon
și apare pe desktop (și invers).

## 4. Funcții de adus (matrice)

Prioritate: **A** = valoare mare, ușor pe telefon; **B** = merită, mai mult efort; **C** = doar citire/ușor;
**—** = nu are sens pe telefon.

| Funcție desktop | Pe telefon | Prio | Faza |
|---|---|---|---|
| Sesiuni (listă, căutare, fork, redenumire, ștergere) | listă + căutare + reluare | A | 2 |
| Chat în flux cu unelte, aprobări, steering, stop | cardurile de aprobare există deja; se leagă la `/approval` | A | 2 |
| Rutine / Cron (creare, pauză, rulare acum) | ecran „Rutine" + notificare la rezultat | A | 3 |
| Agenți/Bots, grupuri, bot-to-bot, avatare, voci | extind agenții și grupurile existente | A | 3 |
| Notificări (turn terminat, aprobare cerută) | serviciu foreground + notificare | A | 2 |
| Profiluri | selector | A | 1 |
| Skills / Toolsets / pluginuri (activare) | listă cu comutatoare | B | 3 |
| Artifacts (previzualizare, deschidere) | vizualizare în aplicație | B | 4 |
| Webhooks, Messaging (stare, conectare cu QR) | stare + acțiuni de bază | B | 4 |
| Memorie / Memory Graph | listă + căutare, graf simplificat | B | 4 |
| Command Center (utilizare, costuri, sănătate gateway) | tablou compact | B | 4 |
| Quick Entry | „Partajează către agent" din meniul Android | B | 4 |
| Import sesiuni, backup | export/import prin fișier | C | 5 |
| Modele locale din desktop | doar citire; descărcarea rămâne pe telefon | C | 5 |
| Terminal, git review/worktrees, file browser, HUD, pet, computer use, ferestre/tab-uri | — (sau citire a fișierelor, ulterior) | — | — |

Direcția inversă (**telefon → desktop**), faza 4: telefonul se înregistrează ca **peer** (`hermes peer add`) și
expune uneltele lui (Library, Calendar, notificări, cameră, senzori, UI) ca toolset de „suprafață mobilă"
al sesiunii (regula din `AGENTS.md`: capacitatea ține de sesiune, nu de variabile de mediu). Agenții de pe
desktop pot atunci cere „pune în calendar" sau „fă o poză".

## 5. Design pentru ecrane mici

- **Navigare:** bară de jos cu 4–5 destinații (Chat, Sesiuni, Rutine, Agenți, Mai mult) + meniul lateral
  existent. Pagini desktop „master-detail" devin liste → ecran de detaliu.
- **Același limbaj vizual** ca restul aplicației (HermesBackdrop, dimensiunile comune, linia de lucru).
- Ținte de atingere ≥ 48 dp, gesturi (glisare pentru meniu există), foi de jos în loc de dialoguri.
- Starea „offline / gateway indisponibil" proiectată de la început, nu adăugată după.
- Tabletă/pliabil: panou dublu (listă + conversație) peste 600 dp.

## 6. Nucleul Python: de aliniat (mutat mai devreme)

Pentru că telefonul trebuie să funcționeze singur, funcțiile din secțiunea 4 (rutine, bots, profiluri,
skills noi) trebuie să ruleze în **runtime-ul local**, iar acesta e mult în urma nucleului desktop. Alinierea
nu mai e un detaliu de la sfârșit: e coloana vertebrală a planului. Propunere:

1. Merge controlat al nucleului Nous din `Mac/` în fork-ul Android, în felii (`agent/`, `tools/`, `gateway/`,
   `hermes_cli/`), cu suita Android rulată după fiecare felie. Patch-urile Android rămân izolate în
   `hermes_android/`. Fiecare felie are o listă de ce se schimbă pentru Android.
2. Pe urmă, funcțiile se aduc **local întâi**, apoi li se adaugă varianta „conectat la desktop".
3. Teste care se învechesc (cele 25 care caută text în cod Kotlin) se rescriu pe comportament.
4. Limitări Android asumate: fără terminal desktop multi-tab, fără computer use, rutine care depind de
   serviciu în fundal (limitate de Doze/baterie; se folosește WorkManager și o notificare la rezultat).

## 7. Faze (prima variantă, înlocuită de secțiunea 10)

| Faza | Conținut | Gata când |
|---|---|---|
| 0 | Hotărâri din secțiunea 9; adăugat `RemoteGatewayClient` cu teste pe un server simulat | clientul trece testele de contract |
| 1 | Pairing QR (desktop + telefon), conexiune, profiluri, stare în antet | telefonul se asociază și vede gateway-ul |
| 2 | Sesiuni comune, chat în flux, aprobări, steering/stop, notificări | o conversație continuă între dispozitive |
| 3 | Rutine, agenți/bots/grupuri, skills și toolsets | rutinele create pe un dispozitiv apar pe celălalt |
| 4 | Telefon ca peer + unelte mobile, artifacts, memorie, command center, partajare | desktopul poate folosi Library/Calendar |
| 5 | Import/backup, aliniere nucleu local, teste, versiune | actualizarea nucleului nu strică Android |

Fiecare fază se livrează separat (ramură, teste, ghid de verificare pe dispozitiv).

## 8. Riscuri

- **Securitate:** gateway-ul poate rula comenzi și citește chei. Un token de dispozitiv, revocabil, expirare,
  doar prin rețea de încredere/Tailscale; fără expunere directă pe internet. Aprobările rămân pe telefon.
- **Licență:** repo-ul desktop e MIT (moștenit de la Nous); codul din OpenBot **nu** se copiază (PolyForm
  Noncommercial). Aici nu e nevoie.
- **Divergența nucleului:** cea mai mare sursă de efort; de aceea e în faza 5, nu la început.
- **Compilare:** în această mașină Gradle nu pornește fără bundle-ul Python și discul s-a umplut de două ori;
  fără build, fiecare fază cere verificare pe telefon.
- **Funcții desktop care par simple dar nu sunt:** Messaging (OAuth/QR per platformă), Memory Graph (randare).

## 9. Hotărâri

Hotărâte:
- Agentul rămâne **local** pe telefon, implicit; aplicația funcționează singură, fără desktop.
- Când găsește un desktop, aplicația **întreabă** (conectez sau rămân local); alegerea se poate ține minte.
- Telecomanda există doar când ești conectat, și doar ca mod ales.

Încă deschise:
1. Conexiune: **LAN + Tailscale** (recomandat) sau și **Hermes Cloud** (OAuth)?
2. Se acceptă ca desktopul să primească un buton „Asociază telefon" (modificare în `Mac/`)?
3. Ordinea: începem cu alinierea nucleului local (felii) sau cu conexiunea la desktop? Recomandare:
   conexiunea mai întâi (mică, vizibilă), alinierea nucleului în paralel, pe felii.

## 10. Faze (revizuite după hotărâre)

| Faza | Conținut |
|---|---|
| 0 | Detectare desktop (QR/Tailscale/LAN) + dialogul „conectez / rămân local" + modul per conversație |
| 1 | Conectare: sesiuni, chat în flux, aprobări, stop/steering, notificări (mod Desktop) |
| 2 | Aliniere nucleu local, felia 1 (`agent/`, `tools/`) + teste de comportament |
| 3 | Funcții locale: rutine, agenți/bots/grupuri, skills/toolsets, profiluri (apoi și varianta conectată) |
| 4 | Telefon ca peer, artifacts, memorie, command center, partajare din Android |
| 5 | Import/backup, ultimele felii de nucleu, release |
