package si.um.feri

object ApiConfig {
    val baseUrl: String = System.getenv("API_URL") ?: "http://localhost:3000"
}