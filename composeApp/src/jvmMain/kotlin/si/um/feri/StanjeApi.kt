package si.um.feri

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

data class NapImportSourceResult(
    val key: String? = null,
    val code: String? = null,
    val sourceName: String? = null,
    val count: Int = 0,
    val ok: Boolean = false,
    val error: String? = null
)

data class NapImportError(
    val source: String? = null,
    val code: String? = null,
    val error: String? = null
)

data class NapImportResponse(
    val message: String? = null,
    val importedAt: String? = null,
    val count: Int = 0,
    val sources: List<NapImportSourceResult> = emptyList(),
    val errors: List<NapImportError> = emptyList(),
    val items: List<StanjeCeste> = emptyList()
)

object StanjeApi {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val BASE: String get() = ApiConfig.baseUrl

    private fun Request.Builder.withAuth() =
        addHeader("Authorization", "Bearer ${ApiConfig.jwtToken}")

    private fun responseBodyOrThrow(request: Request, errorPrefix: String): String {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("$errorPrefix: HTTP ${response.code} $body")
            }
            return body
        }
    }

    fun getAll(): List<StanjeCeste> {
        val request = Request.Builder()
            .url("$BASE/api/stanje-cest")
            .get()
            .withAuth()
            .build()

        val body = responseBodyOrThrow(request, "Napaka pri branju stanja cest")
        val type = object : TypeToken<List<StanjeCeste>>() {}.type
        return gson.fromJson(body, type) ?: emptyList()
    }

    fun importFromNap(): List<StanjeCeste> {
        val request = Request.Builder()
            .url("$BASE/api/stanje-cest/nap/import")
            .post(ByteArray(0).toRequestBody(JSON))
            .withAuth()
            .build()

        val body = responseBodyOrThrow(request, "Napaka pri uvozu NAP vsebin")
        val response = gson.fromJson(body, NapImportResponse::class.java)
        return response?.items ?: emptyList()
    }

    fun sync(list: List<StanjeCeste>): Boolean {
        val body = gson.toJson(list).toRequestBody(JSON)

        val request = Request.Builder()
            .url("$BASE/api/stanje-cest/sync")
            .post(body)
            .withAuth()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                println("Napaka pri shranjevanju stanja cest: ${response.code}")
                println(response.body?.string().orEmpty())
            }
            return response.isSuccessful
        }
    }

    fun clear(): Boolean {
        val request = Request.Builder()
            .url("$BASE/api/stanje-cest")
            .delete()
            .withAuth()
            .build()

        client.newCall(request).execute().use { response ->
            return response.isSuccessful
        }
    }
}
