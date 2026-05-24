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

## Delo na novem tasku

Za zagon strežnika je potrebno zagnati naslednje ukaze:

``` docker build -t traffictwin .```
``` docker run -d -p 3000:3000 -e MONGO_URI="mongodb+srv://TafficTwin_db_user:geslo@cluster54.xxxx.mongodb.net/traffic_twin" --name myServer traffictwin```

## Navodila za zagon Dockerja
``` docker-compose up: ``` zagon 

``` docker-compose down ``` zaustavitev in odstranitev kontejnerja

``` docker-compose stop ``` zaustavitev kontejnerja
