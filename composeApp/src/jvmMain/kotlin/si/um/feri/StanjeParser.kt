package si.um.feri
import com.microsoft.playwright.*

data class StanjeCeste(
    val id: String? = null,
    val tip: String = "",
    val relacija: String = "",
    val stanje: String = "",
    val title: String? = null,
    val description: String? = null,
    val sourceKey: String? = null,
    val sourceName: String? = null,
    val napCode: String? = null,
    val language: String? = null,
    val format: String? = null,
    val category: String? = null,
    val recordType: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val coordinates: List<List<Double>> = emptyList(),
    val geometryType: String? = null,
    val importedAt: String? = null,
    val lastUpdated: String? = null
)

fun parserStanjeList(): List<StanjeCeste> {
    return StanjeApi.importFromNap()
}
/*
fun parserStanjeList(): List<StanjeCeste> {
    Playwright.create().use { playwright ->
        val browser = playwright.chromium().launch()
        val page = browser.newPage()

        page.navigate("https://www.amzs.si/na-poti/stanje-na-slovenskih-cestah")
        page.waitForSelector(
            "div.road.ro.ty",
            Page.WaitForSelectorOptions().setTimeout(10000.0)
        )

        val roadList: MutableList<StanjeCeste> = mutableListOf()

        for (road in page.locator(".road.ro.ty").all()) {
            val cestaRaw = road.getAttribute("data-cesta") ?: continue
            val data = cestaRaw.split(",")

            val tip = data.getOrNull(0) ?: ""
            val relacija = data.getOrNull(1) ?: ""
            val stanje = road.getAttribute("data-work") ?: ""

            roadList.add(
                StanjeCeste(
                    tip = tip,
                    relacija = relacija,
                    stanje = stanje
                )
            )
        }

        browser.close()
        return roadList
    }
}*/