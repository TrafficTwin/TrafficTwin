package si.um.feri

import com.microsoft.playwright.*

data class StanjeCeste(
    val tip: String,
    val relacija: String,
    val stanje: String,
)

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
}