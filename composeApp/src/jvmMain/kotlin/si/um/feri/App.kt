package si.um.feri

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.serpro69.kfaker.Faker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import si.um.feri.ParkingApi.runParserLocal
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun App() {
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowSize = maxWidth > 800.dp // Popravljeno ime in zaznavanje širine

        // Podatki, ki jih prikazujemo v seznamu parkirišč
        var parkingLots by remember { mutableStateOf(listOf<ParkingDto>()) }
        var searchQuery by remember { mutableStateOf("") }

        // Stanja za generator namišljenih podatkov parkirišč
        var countInput by remember { mutableStateOf("1") }
        var minCap by remember { mutableStateOf("10") }
        var maxCap by remember { mutableStateOf("150") }

        // Stanje za urejanje posameznega parkirišča
        var editingParking by remember { mutableStateOf<ParkingDto?>(null) }

        // Strani
        var selectedPage by remember { mutableStateOf(0) }

        // Podatki za stanje cest
        var roadStates by remember { mutableStateOf(listOf<StanjeCeste>()) }
        var editingRoadIndex by remember { mutableStateOf<Int?>(null) }

        //GeoJSON
        var latitudeInput by remember { mutableStateOf("46.5547") }
        var longitudeInput by remember { mutableStateOf("15.6459") }
        var radiusInput by remember { mutableStateOf("1000") }

        var showDialog by remember { mutableStateOf(false) }

        // Stanja za vnosna polja
        var inputLocation by remember { mutableStateOf("") }
        var inputPayment by remember { mutableStateOf("") }
        var inputCapacity by remember { mutableStateOf("") }
        var inputOccupied by remember { mutableStateOf("") }
        var inputLat by remember { mutableStateOf("") }
        var inputLng by remember { mutableStateOf("") }

        var isReady by remember { mutableStateOf(false) }



        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                runCatching {
                    ParkingApi.login("admin@city.si", "admin123")
                }
            }
            isReady = true
        }

        if (!isReady) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Traffic Twin: Management System",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    TabRow(
                        selectedTabIndex = selectedPage,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Tab(
                            selected = selectedPage == 0,
                            onClick = { selectedPage = 0 },
                            text = { Text("Parkirišča") }
                        )

                        Tab(
                            selected = selectedPage == 1,
                            onClick = { selectedPage = 1 },
                            text = { Text("Stanje cest") }
                        )
                    }

                    if (selectedPage == 0) {
                        // Zmanjšan vertikalni padding iskalnika na minimum za manjšo razdaljo
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Išči lokacijo") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            }
                        )


                        // ODZIVNA VRSTICA ZA GEOJSON (Če jo boste odkomentirali)
                        if (windowSize) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(value = latitudeInput, onValueChange = { latitudeInput = it }, label = { Text("Lat") }, modifier = Modifier.weight(1f))
                                TextField(value = longitudeInput, onValueChange = { longitudeInput = it }, label = { Text("Lon") }, modifier = Modifier.weight(1f))
                                TextField(value = radiusInput, onValueChange = { radiusInput = it }, label = { Text("Radij m") }, modifier = Modifier.weight(1f))
                                Button(
                                    onClick = { /* GeoJSON klic */ },
                                    modifier = Modifier.weight(1f)
                                ) { Text("V bližini") }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextField(value = latitudeInput, onValueChange = { latitudeInput = it }, label = { Text("Lat") }, modifier = Modifier.weight(1f))
                                    TextField(value = longitudeInput, onValueChange = { longitudeInput = it }, label = { Text("Lon") }, modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    TextField(value = radiusInput, onValueChange = { radiusInput = it }, label = { Text("Radij m") }, modifier = Modifier.weight(1f))
                                    Button(onClick = { /* GeoJSON klic */ }, modifier = Modifier.weight(1f)) { Text("V bližini") }
                                }
                            }
                        }


                        // ODZIVNA VRSTICA Z GUMBI: Če je okno široko, so v vrstici, drugače v dveh vrsticah
                        if (windowSize) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val res = withContext(Dispatchers.IO) {
                                                runCatching { ParkingApi.getAll() }.getOrNull()
                                            }
                                            if (res != null) parkingLots = res
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Refresh, null)
                                    Text(" Osveži")
                                }

                                var showSortMenu by remember { mutableStateOf(false) }

                                Box(modifier = Modifier.weight(1f)) {
                                    Button(onClick = { showSortMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Sort, null)
                                        Text(" Razvrsti")
                                    }
                                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                        DropdownMenuItem(text = { Text("Abecedno A-Ž") }, onClick = { parkingLots = parkingLots.sortedBy { it.location.lowercase() }; showSortMenu = false })
                                        DropdownMenuItem(text = { Text("Najbolj prosto") }, onClick = { parkingLots = parkingLots.sortedByDescending { it.capacity - it.occupied }; showSortMenu = false })
                                        DropdownMenuItem(text = { Text("Najbližje") }, onClick = { parkingLots = parkingLots.sortedBy { it.distanceMeters ?: Double.MAX_VALUE }; showSortMenu = false })
                                    }
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            val result = withContext(Dispatchers.IO) { runCatching { runParserLocal() }.getOrNull() }
                                            if (result != null) parkingLots = result
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                                ) {
                                    Text("Uvozi")
                                }

                                Button(
                                    onClick = { showDialog = true },
                                    modifier = Modifier.weight(1.2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Default.Add, null)
                                    Text("Dodaj ročno")
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            val result = withContext(Dispatchers.IO) {
                                                runCatching {
                                                    ParkingApi.sync(parkingLots)
                                                    ParkingApi.getAll()
                                                }.getOrNull()
                                            }
                                            if (result != null) parkingLots = result
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Text("Shrani")
                                }


                            }
                        } else {
                            // Prilagoditev za manjša okna (Zloženo v dva vrstična bloka, da gumbi niso preozki)
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val res = withContext(Dispatchers.IO) { runCatching { ParkingApi.getAll() }.getOrNull() }
                                                if (res != null) parkingLots = res
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Refresh, null)
                                        Text(" Osveži")
                                    }

                                    var showSortMenu by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.weight(1f)) {
                                        Button(onClick = { showSortMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                            Icon(Icons.Default.Sort, null)
                                            Text(" Razvrsti")
                                        }
                                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                            DropdownMenuItem(text = { Text("Abecedno A-Ž") }, onClick = { parkingLots = parkingLots.sortedBy { it.location.lowercase() }; showSortMenu = false })
                                            DropdownMenuItem(text = { Text("Najbolj prosto") }, onClick = { parkingLots = parkingLots.sortedByDescending { it.capacity - it.occupied }; showSortMenu = false })
                                            DropdownMenuItem(text = { Text("Najbližje") }, onClick = { parkingLots = parkingLots.sortedBy { it.distanceMeters ?: Double.MAX_VALUE }; showSortMenu = false })
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val result = withContext(Dispatchers.IO) { runCatching { runParserLocal() }.getOrNull() }
                                                if (result != null) parkingLots = result
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                                    ) {
                                        Text("Uvozi")
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val result = withContext(Dispatchers.IO) {
                                                    runCatching {
                                                        ParkingApi.sync(parkingLots)
                                                        ParkingApi.getAll()
                                                    }.getOrNull()
                                                }
                                                if (result != null) parkingLots = result
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                    ) {
                                        Text("Shrani")
                                    }

                                    Button(
                                        onClick = { showDialog = true },
                                        modifier = Modifier.weight(1.2f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Icon(Icons.Default.Add, null)
                                        Text("Dodaj")
                                    }
                                }
                            }
                        }

                        // DIALOG ZA VNOS PODATKOV (Z omejitvijo širine)
                        // DIALOG ZA VNOS PODATKOV (Z odzivnimi polji)
                        // DIALOG ZA VNOS PODATKOV (Vedno eno pod drugim z drsnikom)
                        if (showDialog) {
                            AlertDialog(
                                onDismissRequest = { showDialog = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.90f)
                                    .widthIn(max = 450.dp), // Lepa, standardna širina za obrazec
                                title = { Text(text = "Ročni vnos parkirišča", fontWeight = FontWeight.Bold) },
                                text = {
                                    // RememberScrollState omogoča, da se znotraj dialoga pojavi drsnik, če je okno premajhno
                                    val scrollState = rememberScrollState()

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(scrollState), // Rešitev za majhna okna!
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(value = inputLocation, onValueChange = { inputLocation = it }, label = { Text("Ime / Lokacija") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = inputPayment, onValueChange = { inputPayment = it }, label = { Text("Tip plačila") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = inputCapacity, onValueChange = { inputCapacity = it }, label = { Text("Kapaciteta") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = inputOccupied, onValueChange = { inputOccupied = it }, label = { Text("Zasedenost") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = inputLat, onValueChange = { inputLat = it }, label = { Text("Širina (Latitude)") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = inputLng, onValueChange = { inputLng = it }, label = { Text("Dolžina (Longitude)") }, modifier = Modifier.fillMaxWidth())
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val capacityInt = inputCapacity.toIntOrNull() ?: 0
                                            val occupiedInt = inputOccupied.toIntOrNull() ?: 0
                                            val latDouble = inputLat.toDoubleOrNull()
                                            val lngDouble = inputLng.toDoubleOrNull()

                                            val newParkingDto = ParkingDto(
                                                id = 0, // Backend bo sam dodelil pravi ID
                                                location = inputLocation,
                                                typeOfPayment = inputPayment,
                                                capacity = capacityInt,
                                                occupied = occupiedInt,
                                                latitude = latDouble,
                                                longitude = lngDouble
                                            )

                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    val uspesno = ParkingApi.add(newParkingDto)
                                                    if (uspesno) {
                                                        // Ko uspešno shranimo v bazo, takoj potegnemo osvežen seznam
                                                        val osvezenSeznam = ParkingApi.getAll()
                                                        withContext(Dispatchers.Main) {
                                                            parkingLots = osvezenSeznam

                                                            // Polja izpraznimo in dialog zapremo ŠELE, KO JE SHRANJENO
                                                            inputLocation = ""
                                                            inputPayment = ""
                                                            inputCapacity = ""
                                                            inputOccupied = ""
                                                            inputLat = ""
                                                            inputLng = ""
                                                            showDialog = false
                                                        }
                                                    } else {
                                                        println("LOG_NAPAKA: Strežnik je vrnil neuspešen status.")
                                                    }
                                                } catch (e: Exception) {
                                                    println("LOG_NAPAKA: Prišlo je do napake pri povezavi: ${e.message}")
                                                    e.printStackTrace()
                                                }
                                            }
                                        }
                                    ) { Text("Potrdi") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDialog = false }) { Text("Prekliči") }
                                }
                            )
                        }

                        val filteredLots = parkingLots.filter {
                            it.location.contains(searchQuery, ignoreCase = true)
                        }

                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            items(filteredLots) { lot ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val availableSpaces = (lot.capacity - lot.occupied).coerceAtLeast(0)

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = lot.location, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                            Text(text = "Prosto: $availableSpaces / ${lot.capacity} (${lot.typeOfPayment})", style = MaterialTheme.typography.bodyMedium)

                                            if (lot.latitude != null && lot.longitude != null) {
                                                Text(text = "Koordinate: ${"%.5f".format(lot.latitude)}, ${"%.5f".format(lot.longitude)}", style = MaterialTheme.typography.bodySmall)
                                            }

                                            lot.distanceMeters?.let { distance ->
                                                Text(text = "Oddaljenost: ${"%.0f".format(distance)} m", style = MaterialTheme.typography.bodySmall)
                                            }

                                            val ratio = if (lot.capacity > 0) availableSpaces.toFloat() / lot.capacity.toFloat() else 0f

                                            LinearProgressIndicator(
                                                progress = { ratio.coerceIn(0f, 1f) },
                                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(8.dp),
                                                color = if (ratio > 0.2f) Color(0xFF4CAF50) else Color.Red,
                                                trackColor = Color.LightGray,
                                                strokeCap = StrokeCap.Round
                                            )
                                        }

                                        IconButton(onClick = { editingParking = lot }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Uredi", tint = Color.Blue)
                                        }

                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    val result = withContext(Dispatchers.IO) {
                                                        runCatching {
                                                            ParkingApi.delete(lot.id)
                                                            ParkingApi.getAll()
                                                        }.getOrNull()
                                                    }
                                                    if (result != null) parkingLots = result
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Izbriši", tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        StanjeCestScreen(
                            roadStates = roadStates,
                            windowSize = windowSize,
                            onRefresh = {
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { StanjeApi.getAll() }.getOrElse { emptyList() }
                                    }
                                    roadStates = result
                                }
                            },
                            onImport = {
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { parserStanjeList() }.getOrElse { emptyList() }
                                    }
                                    roadStates = result
                                }
                            },
                            onGenerate = {
                                val generated = generateFakeStanjeCest(10)
                                roadStates = roadStates + generated
                            },
                            onSave = {
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching {
                                            StanjeApi.sync(roadStates)
                                            StanjeApi.getAll()
                                        }.getOrElse { roadStates }
                                    }
                                    roadStates = result
                                }
                            },
                            onEdit = { index -> editingRoadIndex = index },
                            onDelete = { index -> roadStates = roadStates.filterIndexed { i, _ -> i != index } }
                        )
                    }
                }
            }

            // DIALOG ZA UREJANJE PARKIRIŠČA
            editingParking?.let { parking ->
                var editLoc by remember(parking.id) { mutableStateOf(parking.location) } //
                var editCap by remember(parking.id) { mutableStateOf(parking.capacity.toString()) } //
                var editOcc by remember(parking.id) { mutableStateOf(parking.occupied.toString()) } //
                // Dodana stanja za koordinate (privzeto se izpišejo obstoječe ali prazne)
                var editLat by remember(parking.id) { mutableStateOf(parking.latitude?.toString() ?: "") }
                var editLng by remember(parking.id) { mutableStateOf(parking.longitude?.toString() ?: "") }

                AlertDialog(
                    onDismissRequest = { editingParking = null }, //
                    title = { Text("Uredi / Ročni vnos parkirišča") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { //
                            TextField(
                                value = editLoc, //
                                onValueChange = { editLoc = it }, //
                                label = { Text("Lokacija") } //
                            )

                            TextField(
                                value = editCap, //
                                onValueChange = { editCap = it }, //
                                label = { Text("Kapaciteta") } //
                            )

                            TextField(
                                value = editOcc, //
                                onValueChange = { editOcc = it }, //
                                label = { Text("Zasedeno") } //
                            )

                            // NOVO: Polje za Latitude (Širina)
                            TextField(
                                value = editLat,
                                onValueChange = { editLat = it },
                                label = { Text("Širina (Latitude) npr. 46.5547") }
                            )

                            // NOVO: Polje za Longitude (Dolžina)
                            TextField(
                                value = editLng,
                                onValueChange = { editLng = it },
                                label = { Text("Dolžina (Longitude) npr. 15.6459") }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch { //
                                    val nCap = editCap.toIntOrNull() ?: parking.capacity //
                                    val nOcc = editOcc.toIntOrNull() ?: parking.occupied //

                                    // Pretvorba vnesenih koordinat v Double
                                    val nLat = editLat.toDoubleOrNull()
                                    val nLng = editLng.toDoubleOrNull()

                                    // Če dodajaš popolnoma novo parkirišče in ne želiš podvojenih ID-jev,
                                    // lahko obdržiš naključen ID, če je bil prejšnji 0
                                    val finalId = if (parking.id == 0) (10000..999999).random() else parking.id

                                    val updated = parking.copy(
                                        id = finalId,
                                        location = editLoc, //
                                        capacity = nCap, //
                                        occupied = nOcc, //
                                        latitude = nLat,
                                        longitude = nLng
                                    )

                                    // POPRAVLJENA KODA:
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching {
                                            // Ker gre za NOV ročni vnos, pokličemo add namesto update!
                                            ParkingApi.add(updated)
                                            ParkingApi.getAll()
                                        }.getOrNull()
                                    }
                                    if (result != null) { //
                                        parkingLots = result //
                                    }

                                    editingParking = null //
                                }
                            }
                        ) {
                            Text("Shrani") //
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingParking = null }) { //
                            Text("Prekliči") //
                        }
                    }
                )
            }

            // DIALOG ZA UREJANJE CESTE
            editingRoadIndex?.let { index ->
                val road = roadStates.getOrNull(index)
                if (road != null) {
                    var editTip by remember(index) { mutableStateOf(road.tip) }
                    var editRelacija by remember(index) { mutableStateOf(road.relacija) }
                    var editStanje by remember(index) { mutableStateOf(road.stanje) }

                    AlertDialog(
                        onDismissRequest = { editingRoadIndex = null },
                        modifier = Modifier.fillMaxWidth(0.95f).widthIn(max = 600.dp),
                        title = { Text("Uredi stanje ceste", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(value = editTip, onValueChange = { editTip = it }, label = { Text("Tip ceste") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = editRelacija, onValueChange = { editRelacija = it }, label = { Text("Lokacija / relacija") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = editStanje, onValueChange = { editStanje = it }, label = { Text("Stanje ceste") }, modifier = Modifier.fillMaxWidth())
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val updated = StanjeCeste(tip = editTip, relacija = editRelacija, stanje = editStanje)
                                    roadStates = roadStates.mapIndexed { i, item -> if (i == index) updated else item }
                                    editingRoadIndex = null
                                }
                            ) { Text("Shrani") }
                        },
                        dismissButton = {
                            TextButton(onClick = { editingRoadIndex = null }) { Text("Prekliči") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StanjeCestScreen(
    roadStates: List<StanjeCeste>,
    windowSize: Boolean, // Sprejme stanje velikosti okna
    onRefresh: () -> Unit,
    onImport: () -> Unit,
    onGenerate: () -> Unit,
    onSave: () -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    var searchRoadQuery by remember { mutableStateOf("") }
    var showSortMenu by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf("LOCATION") }

    val filteredRoads = roadStates
        .withIndex()
        .filter { it.value.relacija.contains(searchRoadQuery, ignoreCase = true) }
        .let { list ->
            when (sortMode) {
                "LOCATION" -> list.sortedBy { it.value.relacija.lowercase() }
                "STATE" -> list.sortedBy { it.value.stanje.lowercase() }
                "TYPE" -> list.sortedBy { it.value.tip.lowercase() }
                else -> list
            }
        }

    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            value = searchRoadQuery,
            onValueChange = { searchRoadQuery = it },
            label = { Text("Išči lokacijo") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )

        // Odzivna vrstica gumbov za ceste
        if (windowSize) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text(" Osveži")
                }

                Box(modifier = Modifier.weight(1f)) {
                    Button(onClick = { showSortMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Sort, contentDescription = null)
                        Text(" Razvrsti")
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(text = { Text("Lokacija A-Ž") }, onClick = { sortMode = "LOCATION"; showSortMenu = false })
                        DropdownMenuItem(text = { Text("Stanje ceste A-Ž") }, onClick = { sortMode = "STATE"; showSortMenu = false })
                        DropdownMenuItem(text = { Text("Tip ceste A-Ž") }, onClick = { sortMode = "TYPE"; showSortMenu = false })
                    }
                }

                Button(onClick = onImport, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) {
                    Text("Uvozi")
                }
                Button(onClick = onGenerate, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Text("Dodaj")
                }

                Button(onClick = onSave, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                    Text("Shrani")
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Text(" Osveži")
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        Button(onClick = { showSortMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Sort, contentDescription = null)
                            Text(" Razvrsti")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(text = { Text("Lokacija A-Ž") }, onClick = { sortMode = "LOCATION"; showSortMenu = false })
                            DropdownMenuItem(text = { Text("Stanje ceste A-Ž") }, onClick = { sortMode = "STATE"; showSortMenu = false })
                            DropdownMenuItem(text = { Text("Tip ceste A-Ž") }, onClick = { sortMode = "TYPE"; showSortMenu = false })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = onImport, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) { Text("Uvozi") }
                    Button(onClick = onGenerate, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Dodaj") }
                    Button(onClick = onSave, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("Shrani") }
                }
            }
        }

        if (roadStates.isEmpty()) {
            Text(text = "Ni podatkov. Klikni 'Uvozi stanje' ali 'Generiraj'.", modifier = Modifier.padding(8.dp))
        } else if (filteredRoads.isEmpty()) {
            Text(text = "Ni zadetkov za iskano lokacijo.", modifier = Modifier.padding(8.dp))
        } else {
            Text(text = "Število zadetkov: ${filteredRoads.size}", modifier = Modifier.padding(bottom = 4.dp), fontWeight = FontWeight.Bold)

            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(filteredRoads) { indexedRoad ->
                    val index = indexedRoad.index
                    val road = indexedRoad.value

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = road.relacija, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text(text = "Tip: ${road.tip}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Stanje: ${road.stanje}", style = MaterialTheme.typography.bodyMedium)
                            }

                            IconButton(onClick = { onEdit(index) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Uredi", tint = Color.Blue)
                            }

                            IconButton(onClick = { onDelete(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Izbriši", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun generateAdvancedFake(count: Int, min: Int, max: Int): List<ParkingDto> {
    val faker = Faker()
    val safeCount = count.coerceAtLeast(0)
    val safeMin = min.coerceAtLeast(1)
    val safeMax = max.coerceAtLeast(safeMin)

    return List(safeCount) {
        val cap = (safeMin..safeMax).random()
        ParkingDto(
            id = (10000..999999).random(),
            location = faker.address.streetName(),
            capacity = cap,
            occupied = (0..cap).random(),
            typeOfPayment = if ((0..1).random() == 0) "FREE" else "PAYABLE",
            latitude = (465000..466200).random() / 10000.0,
            longitude = (155800..157200).random() / 10000.0
        )
    }
}

fun generateFakeStanjeCest(count: Int): List<StanjeCeste> {
    val faker = Faker()
    val tipiCest = listOf("AC", "HC", "G1", "G2", "R1", "R2", "LC")
    val stanja = listOf("Normalen promet", "Zastoj", "Dela na cesti", "Zapora ceste", "Omejen promet", "Prometna nesreča", "Povečana gostota prometa", "Spolzko vozišče", "Megla", "Sneg na cesti")
    val safeCount = count.coerceAtLeast(0)

    return List(safeCount) {
        val kraj1 = faker.address.city()
        val kraj2 = faker.address.city()
        StanjeCeste(
            tip = tipiCest.random(),
            relacija = "$kraj1 - $kraj2",
            stanje = stanja.random()
        )
    }
}