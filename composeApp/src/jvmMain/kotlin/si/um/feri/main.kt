package si.um.feri

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.network.parseGetRequestBlocking
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.select.Elements

fun parserGostota(){
    val doc: Document = Ksoup.parseGetRequestBlocking(url = "https://www.promet.si/sl/stevci-prometa")
    val countContainer: Element = doc.getElementById("stevci-detail-container-34")!!
    println(countContainer)
    val cards: Elements = countContainer.select(".row")

    cards.forEach {
            card: Element ->
        println(card)
    }
}

fun main() {
    parserGostota()
}