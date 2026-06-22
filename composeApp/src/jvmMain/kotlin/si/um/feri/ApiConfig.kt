package si.um.feri

object ApiConfig {
    val baseUrl: String = System.getenv("API_URL") ?: "https://traffictwin.duckdns.org"
    val scraperToken: String = System.getenv("SCRAPER_TOKEN") ?: ""
    var jwtToken: String = ""
}