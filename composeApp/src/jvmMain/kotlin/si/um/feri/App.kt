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

@Composable
fun App() {
    val scope = rememberCoroutineScope()

    // Podatki, ki jih prikazujemo v seznamu parkirišč
    var parkingLots by remember { mutableStateOf(listOf<ParkingDto>()) }
    var searchQuery by remember { mutableStateOf("") }

    // Stanja za generator namišljenih podatkov
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

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Traffic Twin: Management System",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                TabRow(
                    selectedTabIndex = selectedPage,
                    modifier = Modifier.padding(vertical = 8.dp)
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
                    // ISKANJE
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Išči lokacijo") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    )

                    // GLAVNA VRSTICA Z GUMBI
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val res = withContext(Dispatchers.IO) {
                                        runCatching { ParkingApi.getAll() }.getOrNull()
                                    }

                                    if (res != null) {
                                        parkingLots = res
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Text(" Osveži")
                        }

                        var showSortMenu by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Sort, null)
                                Text(" Razvrsti")
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Abecedno (A-Z)") },
                                    onClick = {
                                        parkingLots = parkingLots.sortedBy { it.location }
                                        showSortMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Najbolj prosto") },
                                    onClick = {
                                        parkingLots = parkingLots.sortedByDescending {
                                            it.capacity - it.occupied
                                        }
                                        showSortMenu = false
                                    }
                                )
                            }
                        }

                        // Uvozi podatke preko parserja
                        Button(
                            onClick = {
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching {
                                            runParser()
                                            ParkingApi.getAll()
                                        }.getOrNull()
                                    }

                                    if (result != null) {
                                        parkingLots = result
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2196F3)
                            )
                        ) {
                            Text("Uvozi MB")
                        }

                        // Shrani trenutno stanje v bazo preko API
                        Button(
                            onClick = {
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching {
                                            ParkingApi.sync(parkingLots)
                                            ParkingApi.getAll()
                                        }.getOrNull()
                                    }

                                    if (result != null) {
                                        parkingLots = result
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("Shrani")
                        }
                    }

                    // Testni gumb za promet
                    /*Button(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    parserGostota()
                                }
                            }
                        }
                    ) {
                        Text("Test promet")
                    }*/

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Generator podatkov", fontWeight = FontWeight.Bold)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextField(
                                    value = countInput,
                                    onValueChange = { countInput = it },
                                    label = { Text("Št.") },
                                    modifier = Modifier.weight(1f)
                                )

                                TextField(
                                    value = minCap,
                                    onValueChange = { minCap = it },
                                    label = { Text("Min") },
                                    modifier = Modifier.weight(1f)
                                )

                                TextField(
                                    value = maxCap,
                                    onValueChange = { maxCap = it },
                                    label = { Text("Max") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Button(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                onClick = {
                                    val count = countInput.toIntOrNull() ?: 10
                                    val min = minCap.toIntOrNull() ?: 10
                                    val max = maxCap.toIntOrNull() ?: 100

                                    scope.launch {
                                        val generated = generateAdvancedFake(count, min, max)

                                        val result = withContext(Dispatchers.IO) {
                                            runCatching {
                                                val combinedList = parkingLots + generated
                                                ParkingApi.sync(combinedList)
                                                ParkingApi.getAll()
                                            }.getOrNull()
                                        }

                                        if (result != null) {
                                            parkingLots = result
                                        }
                                    }
                                }
                            ) {
                                Text("Generiraj in shrani v bazo")
                            }
                        }
                    }

                    // Filtriranje seznama glede na vnos v iskalnik
                    val filteredLots = parkingLots.filter {
                        it.location.contains(searchQuery, ignoreCase = true)
                    }

                    // GLAVNI SEZNAM PARKIRIŠČ
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
                                    val availableSpaces =
                                        (lot.capacity - lot.occupied).coerceAtLeast(0)

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = lot.location,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )

                                        Text(
                                            text = "Prosto: $availableSpaces / ${lot.capacity} (${lot.typeOfPayment})",
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        val ratio =
                                            if (lot.capacity > 0) {
                                                availableSpaces.toFloat() / lot.capacity.toFloat()
                                            } else {
                                                0f
                                            }

                                        // Indikator zasedenosti
                                        LinearProgressIndicator(
                                            progress = { ratio.coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                                .height(8.dp),
                                            color = if (ratio > 0.2f) {
                                                Color(0xFF4CAF50)
                                            } else {
                                                Color.Red
                                            },
                                            trackColor = Color.LightGray,
                                            strokeCap = StrokeCap.Round
                                        )
                                    }

                                    // Gumb za urejanje
                                    IconButton(onClick = { editingParking = lot }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Uredi",
                                            tint = Color.Blue
                                        )
                                    }

                                    // Gumb za brisanje
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val result = withContext(Dispatchers.IO) {
                                                    runCatching {
                                                        ParkingApi.delete(lot.id)
                                                        ParkingApi.getAll()
                                                    }.getOrNull()
                                                }

                                                if (result != null) {
                                                    parkingLots = result
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Izbriši",
                                            tint = Color.Red
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    StanjeCestScreen(
                        roadStates = roadStates,
                        onRefresh = {
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        StanjeApi.getAll()
                                    }.getOrElse {
                                        emptyList()
                                    }
                                }

                                roadStates = result
                            }
                        },
                        onImport = {
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        parserStanjeList()
                                    }.getOrElse {
                                        emptyList()
                                    }
                                }

                                roadStates = result
                            }
                        },
                        onSave = {
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        StanjeApi.sync(roadStates)
                                        StanjeApi.getAll()
                                    }.getOrElse {
                                        roadStates
                                    }
                                }

                                roadStates = result
                            }
                        },
                        onEdit = { index ->
                            editingRoadIndex = index
                        },
                        onDelete = { index ->
                            roadStates = roadStates.filterIndexed { i, _ -> i != index }
                        }
                    )
                }
            }
        }

        // DIALOG ZA UREJANJE PARKIRIŠČA
        editingParking?.let { parking ->
            var editLoc by remember(parking.id) { mutableStateOf(parking.location) }
            var editCap by remember(parking.id) { mutableStateOf(parking.capacity.toString()) }
            var editOcc by remember(parking.id) { mutableStateOf(parking.occupied.toString()) }

            AlertDialog(
                onDismissRequest = { editingParking = null },
                title = { Text("Uredi parkirišče") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextField(
                            value = editLoc,
                            onValueChange = { editLoc = it },
                            label = { Text("Lokacija") }
                        )

                        TextField(
                            value = editCap,
                            onValueChange = { editCap = it },
                            label = { Text("Kapaciteta") }
                        )

                        TextField(
                            value = editOcc,
                            onValueChange = { editOcc = it },
                            label = { Text("Zasedeno") }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                val nCap = editCap.toIntOrNull() ?: parking.capacity
                                val nOcc = editOcc.toIntOrNull() ?: parking.occupied

                                val updated = parking.copy(
                                    location = editLoc,
                                    capacity = nCap,
                                    occupied = nOcc
                                )

                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        ParkingApi.update(parking.id, updated)
                                        ParkingApi.getAll()
                                    }.getOrNull()
                                }

                                if (result != null) {
                                    parkingLots = result
                                }

                                editingParking = null
                            }
                        }
                    ) {
                        Text("Shrani")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingParking = null }) {
                        Text("Prekliči")
                    }
                }
            )
        }

        // DIALOG ZA UREJANJE STANJA CEST
        editingRoadIndex?.let { index ->
            val road = roadStates.getOrNull(index)

            if (road != null) {
                var editTip by remember(index) { mutableStateOf(road.tip) }
                var editRelacija by remember(index) { mutableStateOf(road.relacija) }
                var editStanje by remember(index) { mutableStateOf(road.stanje) }

                AlertDialog(
                    onDismissRequest = { editingRoadIndex = null },
                    title = { Text("Uredi stanje ceste") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextField(
                                value = editTip,
                                onValueChange = { editTip = it },
                                label = { Text("Tip") }
                            )

                            TextField(
                                value = editRelacija,
                                onValueChange = { editRelacija = it },
                                label = { Text("Lokacija / relacija") }
                            )

                            TextField(
                                value = editStanje,
                                onValueChange = { editStanje = it },
                                label = { Text("Stanje ceste") }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val updated = StanjeCeste(
                                    tip = editTip,
                                    relacija = editRelacija,
                                    stanje = editStanje
                                )

                                roadStates = roadStates.mapIndexed { i, item ->
                                    if (i == index) updated else item
                                }

                                editingRoadIndex = null
                            }
                        ) {
                            Text("Shrani")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingRoadIndex = null }) {
                            Text("Prekliči")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StanjeCestScreen(
    roadStates: List<StanjeCeste>,
    onRefresh: () -> Unit,
    onImport: () -> Unit,
    onSave: () -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    var searchRoadQuery by remember { mutableStateOf("") }
    var showSortMenu by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf("LOCATION") }

    val filteredRoads = roadStates
        .withIndex()
        .filter {
            it.value.relacija.contains(searchRoadQuery, ignoreCase = true)
        }
        .let { list ->
            when (sortMode) {
                "LOCATION" -> list.sortedBy { it.value.relacija.lowercase() }
                "STATE" -> list.sortedBy { it.value.stanje.lowercase() }
                "TYPE" -> list.sortedBy { it.value.tip.lowercase() }
                else -> list
            }
        }

    Column(modifier = Modifier.fillMaxSize()) {
        // ISKANJE
        TextField(
            value = searchRoadQuery,
            onValueChange = { searchRoadQuery = it },
            label = { Text("Išči lokacijo") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            }
        )

        // GLAVNA VRSTICA Z GUMBI
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text(" Osveži")
            }

            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Sort, contentDescription = null)
                    Text(" Razvrsti")
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Lokacija A-Ž") },
                        onClick = {
                            sortMode = "LOCATION"
                            showSortMenu = false
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Stanje ceste A-Ž") },
                        onClick = {
                            sortMode = "STATE"
                            showSortMenu = false
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Tip ceste A-Ž") },
                        onClick = {
                            sortMode = "TYPE"
                            showSortMenu = false
                        }
                    )
                }
            }

            Button(
                onClick = onImport,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("Uvozi stanje")
            }

            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Shrani")
            }
        }

        if (roadStates.isEmpty()) {
            Text(
                text = "Ni podatkov. Klikni 'Uvozi stanje'.",
                modifier = Modifier.padding(8.dp)
            )
        } else if (filteredRoads.isEmpty()) {
            Text(
                text = "Ni zadetkov za iskano lokacijo.",
                modifier = Modifier.padding(8.dp)
            )
        } else {
            Text(
                text = "Število zadetkov: ${filteredRoads.size}",
                modifier = Modifier.padding(bottom = 8.dp),
                fontWeight = FontWeight.Bold
            )

            // GLAVNI SEZNAM STANJA CEST
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
                                Text(
                                    text = road.relacija,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Text(
                                    text = "Tip: ${road.tip}",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Text(
                                    text = "Stanje: ${road.stanje}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            IconButton(onClick = { onEdit(index) }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Uredi",
                                    tint = Color.Blue
                                )
                            }

                            IconButton(onClick = { onDelete(index) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Izbriši",
                                    tint = Color.Red
                                )
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

    return List(count) {
        val cap = (min..max).random()

        ParkingDto(
            id = (10000..999999).random(),
            location = faker.address.streetName(),
            capacity = cap,
            occupied = (0..cap).random(),
            typeOfPayment = if ((0..1).random() == 0) "FREE" else "PAYABLE"
        )
    }
}