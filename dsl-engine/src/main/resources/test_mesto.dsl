// Test 1 - minimalno veljavno mesto
// Pozitiven test. Parser mora prepoznati city, road in building.
city "Mini" {
 road "Glavna" {
  line((0, 0), (10, 0));
 };
 building "Obcina" {
  box((2, 2), (4, 0));
 };
}

// Test 2 - cesta s stanjem in relacijo
// Pozitiven test. Road vsebuje atribute, povzete po TrafficTwin stanju cest.
city "Promet" {
 road "R2-441" {
  type "regionalna";
  relation "Murska Sobota - Rakičan";
  state WORKS;
  speedLimit 50;
  polyline((16.1600, 46.6620), (16.1700, 46.6640), (16.1800, 46.6660));
 };
}

// Test 3 - parkirišče z zasedenostjo
// Pozitiven test. Zasedenost je manjša od kapacitete.
city "Parkirisca" {
 parking "Parkirisce center" {
  id 1;
  point (16.1608, 46.6624);
  capacity 120;
  occupied 73;
  payment PAID;
  status OPEN;
 };
}

// Test 4 - poizvedba nearby
// Pozitiven test. Preveri sintakso poizvedbe po bližnjih parkiriščih.
city "Iskanje" {
 let lokacija = (16.1608, 46.6624);
 parking "P1" { id 1; point (16.1610, 46.6620); capacity 50; occupied 10; payment FREE; };
 parking "P2" { id 2; point (16.1900, 46.6800); capacity 30; occupied 30; payment PAID; };
 query "Prosta v blizini" {
  nearby(lokacija, 1000, parking);
  where freeSpaces > 0;
  sortBy distance;
 };
}

// Test 5 - spremenljivke in izrazi
// Pozitiven test. Preveri let, fst, snd in aritmetiko.
city "Izrazi" {
 let p = (16.1000, 46.6000);
 let q = (fst(p) + 0.0100, snd(p) + 0.0050);
 road "Izracunana cesta" {
  line(p, q);
  state OPEN;
 };
}

// Test 6 - park in križišče
// Pozitiven test. Preveri polygon in junction.
city "Zeleno mesto" {
 park "Mestni park" {
  polygon((0, 0), (4, 0), (4, 3), (0, 3), (0, 0));
  set("surface", "grass");
 };
 junction "Krizisce pri parku" (4, 1.5);
}

// Test 7 - nezaključen poligon zgradbe
// Negativen test. Parser lahko sprejme sintakso, semantični analizator pa mora vrniti napako.
city "Napaka poligon" {
 building "Nezakljucena" {
  polygon((0, 0), (3, 0), (3, 2), (0, 2));
 };
}

// Test 8 - zasedenost večja od kapacitete
// Negativen test. occupied ne sme biti večji od capacity.
city "Napaka parkirisce" {
 parking "Prepolno" {
  id 5;
  point (16.1608, 46.6624);
  capacity 20;
  occupied 25;
  payment PAID;
 };
}

// Test 9 - neveljavne geografske koordinate
// Negativen test. Latitude 120 ni veljavna geografska širina.
city "Napaka koordinate" {
 parking "Napacna tocka" {
  id 8;
  point (16.1608, 120.0000);
  capacity 10;
  occupied 2;
  payment FREE;
 };
}

// Test 10 - cesta seka zgradbo
// Opozorilni test. Sintaksa je veljavna, validator pa naj opozori, da cesta poteka skozi zgradbo.
city "Presek" {
 road "Cesta skozi stavbo" {
  line((0, 1), (5, 1));
  state OPEN;
 };
 building "Stavba" {
  box((2, 2), (4, 0));
 };
}