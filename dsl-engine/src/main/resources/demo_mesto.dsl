city "TrafficTwin demo" {
    let center = (16.1608, 46.6624);

    road "Cesta do centra" {
        type "mestna";
        relation "avtobusna postaja - center";
        state CONGESTED;
        lanes 2;
        speedLimit 30;
        polyline((16.1500, 46.6600), (16.1608, 46.6624), (16.1700, 46.6650));
        set("source", "manual");
    };

    road "Zaprta servisna cesta" {
        type "servisna";
        relation "skladisce - center";
        state CLOSED;
        line((16.1550, 46.6610), (16.1580, 46.6618));
    };

    building "Obcina" {
        polygon((16.1580, 46.6610), (16.1595, 46.6610), (16.1595, 46.6620), (16.1580, 46.6620), (16.1580, 46.6610));
        set("type", "public");
    };

    park "Mestni park" {
        polygon((16.1620, 46.6620), (16.1640, 46.6620), (16.1640, 46.6640), (16.1620, 46.6640), (16.1620, 46.6620));
        set("surface", "grass");
    };

    zone "Cona umirjenega prometa" {
        polygon((16.1500, 46.6590), (16.1540, 46.6590), (16.1540, 46.6615), (16.1500, 46.6615), (16.1500, 46.6590));
        set("speed", 30);
    };

    parking "Center P1" {
        id 1001;
        point (16.1610, 46.6625);
        capacity 60;
        occupied 44;
        payment PAID;
        status OPEN;
    };

    parking "Center P2" {
        id 1002;
        point (16.1650, 46.6640);
        capacity 40;
        occupied 40;
        payment MIXED;
        status FULL;
    };

    parking "Makadamsko parkirisce" {
        id 1003;
        point (16.1560, 46.6605);
        capacity 25;
        occupied 5;
        payment UNKNOWN;
        status UNKNOWN;
    };

    junction "Krizisce pri parku" (16.1640, 46.6630);

    sensor "Stevec prometa 1" (16.1600, 46.6620) {
        "kind" = "traffic-counter";
        "active" = true;
    };

    query "Parkirisca do 1 km" {
        nearby(center, 1000, parking);
        where freeSpaces > 0;
        sortBy distance;
    };

    query "Stavbe do 500 m" {
        nearby(center, 500, building);
        sortBy distance;
    };

    query "Senzorji do 1 km" {
        nearby(center, 1000, sensor);
        sortBy distance;
    };
}