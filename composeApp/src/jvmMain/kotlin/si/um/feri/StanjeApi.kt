package si.um.feri

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

object StanjeApi {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private const val BASE = "http://127.0.0.1:3000"

    fun getAll(): List<StanjeCeste> {
        val req = Request.Builder()
            .url("$BASE/api/stanje-cest")
            .get()
            .build()

        client.newCall(req).execute().use { response ->
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                error("Napaka pri branju stanja cest: ${response.code} $body")
            }

            val type = object : TypeToken<List<StanjeCeste>>() {}.type
            return gson.fromJson(body, type) ?: emptyList()
        }
    }

    fun sync(list: List<StanjeCeste>): Boolean {
        val body = gson.toJson(list).toRequestBody(JSON)

        val req = Request.Builder()
            .url("$BASE/api/stanje-cest/sync")
            .post(body)
            .build()

        client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) {
                println("Napaka pri shranjevanju stanja cest: ${response.code}")
                println(response.body?.string().orEmpty())
            }

            return response.isSuccessful
        }
    }

    fun clear(): Boolean {
        val req = Request.Builder()
            .url("$BASE/api/stanje-cest")
            .delete()
            .build()

        client.newCall(req).execute().use { response ->
            return response.isSuccessful
        }
    }
}