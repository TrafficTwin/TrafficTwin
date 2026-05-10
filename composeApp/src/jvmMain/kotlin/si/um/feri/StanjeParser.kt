package si.um.feri

import com.microsoft.playwright.*

data class StanjeCeste(
    val tip: String,
    val relacija: String,
    val stanje: String,
)

fun parserStanje(){
    Playwright.create().use { playwright ->
        val browser = playwright.chromium().launch()
        val page = browser.newPage()

        page.navigate( "https://www.amzs.si/na-poti/stanje-na-slovenskih-cestah")
        page.waitForSelector("div.road.ro.ty",Page.WaitForSelectorOptions().setTimeout(10000.0))

        val roadList: MutableList<StanjeCeste> = mutableListOf()
        for (road in page.locator(".road.ro.ty").all()) {
            val data = road.getAttribute("data-cesta").split(',')
            val desc = road.allInnerTexts()
            println(desc)
            roadList.add(
                StanjeCeste(
                    data[0],
                    data[1],
                    road.getAttribute("data-work")
                )
            )
        }

        for (road in roadList){
            println(road)
        }

        browser.close()
    }
}