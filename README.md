# TrafficTwin

Na tem projektu bomo delali s tremi glavnimi branchi:

- `dev`
- `release`
- `main`

Vse nove funkcionalnosti in popravki se razvijajo na ločenih branchih, ki izhajajo iz brancha `dev`.

---

## Člani ekipe

- Vid Madjar
- Jan Maček
- Aljoša Nanut

---

## Branch strategija

### `main`

Branch `main` vsebuje stabilno verzijo projekta.

Ta branch predstavlja produkcijsko verzijo kode. Na `main` ne dodajamo sprememb neposredno.

Spremembe pridejo na `main` samo prek Pull Requesta iz brancha `release`.

---

### `release`

Branch `release` vsebuje verzijo projekta, ki je pripravljena za končno testiranje pred objavo.

Na ta branch se združujejo preverjene spremembe iz brancha `dev`.

Ko je verzija na `release` uspešno testirana, se naredi Pull Request v `main`.

---

### `dev`

Branch `dev` je glavni razvojni branch.

Na njem se zbirajo vse nove funkcionalnosti, popravki in spremembe, ki so bile narejene na posameznih task branchih.

Na `dev` ne delamo neposredno. Vsaka sprememba mora biti dodana prek Pull Requesta.

---

## Delo na novem tasku

Za vsak nov task ustvarimo nov branch iz brancha `dev`.

Ime brancha naj bo v obliki:

```
dev_idtaska
```
Ta branch pushamo na repozitorij in naredimo pull request na branch dev.

## Zagon strežnika

Za zagon strežnika je potrebno zagnati naslednje ukaze:

``` docker build -t traffictwin .```
``` docker run -d -p 3000:3000 -e MONGO_URI="mongodb+srv://TafficTwin_db_user:geslo@cluster54.xxxx.mongodb.net/traffic_twin" --name myServer traffictwin```

## Navodila za zagon Dockerja
``` docker-compose up: ``` zagon 

``` docker-compose down ``` zaustavitev in odstranitev kontejnerja

``` docker-compose stop ``` zaustavitev kontejnerja

## TrafficTwin DSL engine

DSL del projekta je v modulu `dsl-engine`.

Pomembne datoteke:

- `dsl-engine/src/main/kotlin/traffictwin/dsl/Types.kt`
- `dsl-engine/src/main/kotlin/traffictwin/dsl/Ast.kt`
- `dsl-engine/src/main/kotlin/traffictwin/dsl/Token.kt`
- `dsl-engine/src/main/resources/grammar/TrafficTwin.bnf`
- `dsl-engine/src/main/resources/grammar/tokens.regex`
- `dsl-engine/src/main/resources/test_mesto.dsl`
- `dsl-engine/src/main/resources/test_expected.json`
- `dsl-engine/src/test/kotlin/traffictwin/dsl/DslResourcesTest.kt`

Modul vsebuje začetno strukturo za DSL jezik: podatkovne tipe, AST konstrukte, tokene, BNF slovnico, regex pravila in 10 testnih programov.

---

## Zagon projekta

Projekt je sestavljen iz več glavnih delov:

- `server` – Node.js/Express backend
- `frontend` – React/Vite frontend
- `composeApp` – Kotlin Compose namizna aplikacija
- `dsl-engine` – Kotlin DSL modul

Za lokalni zagon potrebuješ:

- Node.js 22 ali novejši
- npm
- JDK 21
- Docker in Docker Compose
- Git

---

## Okoljske spremenljivke

Za Docker zagon v korenu projekta ustvari datoteko `.env`:

```env
PORT=3000
MONGO_URI=mongodb://mongodb:27017
JWT_SECRET=zamenjaj_me_z_mocnim_secretom
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=admin123
PUBLIC_URL=localhost
```

Pomen spremenljivk:

- `PORT` – port, na katerem teče backend
- `MONGO_URI` – povezava do MongoDB baze
- `JWT_SECRET` – skrivnost za podpisovanje JWT tokenov
- `ADMIN_EMAIL` – e-pošta administratorskega uporabnika
- `ADMIN_PASSWORD` – geslo administratorskega uporabnika
- `PUBLIC_URL` – javni naslov za izpis WebSocket povezave

Če backend zaganjaš ročno brez Dockerja, uporabi lokalni MongoDB naslov:

```env
PORT=3000
MONGO_URI=mongodb://127.0.0.1:27017
JWT_SECRET=zamenjaj_me_z_mocnim_secretom
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=admin123
PUBLIC_URL=localhost
```

Datoteke `.env` ne dodajaj v Git repozitorij.

---

## Zagon z Docker Compose

Najlažji način za zagon backenda in MongoDB baze je z Docker Compose.

V korenu projekta zaženi:

```bash
docker compose up -d
```

To zažene:

- MongoDB na portu `27017`
- backend na portu `3000`

Backend je nato dostopen na:

```text
http://localhost:3000
```

Za pregled logov:

```bash
docker compose logs -f
```

Za ustavitev kontejnerjev:

```bash
docker compose stop
```

Za ustavitev in odstranitev kontejnerjev:

```bash
docker compose down
```

---

## Ročni zagon backenda

Če backend zaganjaš brez Dockerja, mora biti MongoDB že zagnan lokalno ali dostopen prek povezave `MONGO_URI`.

Premakni se v mapo `server`:

```bash
cd server
```

Namesti odvisnosti:

```bash
npm install
```

V mapi `server` ustvari datoteko `.env`:

```env
PORT=3000
MONGO_URI=mongodb://127.0.0.1:27017
JWT_SECRET=zamenjaj_me_z_mocnim_secretom
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=admin123
PUBLIC_URL=localhost
```

Zaženi strežnik:

```bash
npm start
```

Backend bo dostopen na:

```text
http://localhost:3000
```

WebSocket je dostopen na:

```text
ws://localhost:3000/ws
```

---

## Ročni zagon frontenda

Premakni se v mapo `frontend`:

```bash
cd frontend
```

Namesti odvisnosti:

```bash
npm install
```

Po potrebi ustvari datoteko `.env` v mapi `frontend`:

```env
VITE_API_BASE_URL=http://127.0.0.1:3000
```

Zaženi frontend:

```bash
npm run dev
```

Frontend bo dostopen na:

```text
http://localhost:5173
```

Za produkcijski build:

```bash
npm run build
```

Za predogled produkcijskega builda:

```bash
npm run preview
```

---

## Zagon Kotlin Compose aplikacije

Kotlin Compose aplikacija se nahaja v modulu `composeApp`.

Za zagon iz korena projekta uporabi:

```bash
./gradlew :composeApp:run
```

Na Windows:

```bash
gradlew.bat :composeApp:run
```

Aplikacija privzeto uporablja produkcijski API:

```text
https://traffictwin.duckdns.org
```

Za lokalni backend nastavi `API_URL`.

Linux/macOS:

```bash
API_URL=http://127.0.0.1:3000 ./gradlew :composeApp:run
```

Windows PowerShell:

```powershell
$env:API_URL="http://127.0.0.1:3000"
.\gradlew.bat :composeApp:run
```

Če uporabljaš funkcije scraperja, nastavi še `SCRAPER_TOKEN`.

Linux/macOS:

```bash
API_URL=http://127.0.0.1:3000 SCRAPER_TOKEN=<jwt_token> ./gradlew :composeApp:run
```

Windows PowerShell:

```powershell
$env:API_URL="http://127.0.0.1:3000"
$env:SCRAPER_TOKEN="<jwt_token>"
.\gradlew.bat :composeApp:run
```

---

## Zagon DSL engine modula

DSL modul se nahaja v mapi `dsl-engine`.

Za zagon vseh testov in izvoz demo datoteke v `output.geojson`:

```bash
./gradlew :dsl-engine:run
```

Na Windows:

```bash
gradlew.bat :dsl-engine:run
```

Za zagon samo testov:

```bash
./gradlew :dsl-engine:run --args="test"
```

Za izvoz DSL datoteke v GeoJSON:

```bash
./gradlew :dsl-engine:run --args="geojson dsl-engine/src/main/resources/demo_mesto.dsl output.geojson"
```

Ustvarjena datoteka `output.geojson` se lahko odpre v orodju, kot je geojson.io.

---

## Priporočen vrstni red zagona za lokalni razvoj

1. Zaženi MongoDB in backend:

```bash
docker compose up -d
```

2. Zaženi frontend:

```bash
cd frontend
npm install
npm run dev
```

3. Odpri aplikacijo v brskalniku:

```text
http://localhost:5173
```

4. Po potrebi zaženi še namizno aplikacijo:

```bash
./gradlew :composeApp:run
```

---

## Admin prijava

Administrator je določen prek spremenljivk v `.env` datoteki:

```env
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=admin123
```

Za prijavo v aplikacijo uporabi te podatke.

Navadni uporabniki se lahko registrirajo prek obrazca za registracijo v frontendu.
