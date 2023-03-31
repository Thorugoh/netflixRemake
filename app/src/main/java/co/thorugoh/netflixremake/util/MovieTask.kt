package co.thorugoh.netflixremake.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import co.thorugoh.netflixremake.model.Category
import co.thorugoh.netflixremake.model.Movie
import co.thorugoh.netflixremake.model.MovieDetail
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

class MovieTask(private val callback: Callback) {
    interface Callback {
        fun onPreExecute()
        fun onResult(movieDetail: MovieDetail)
        fun onFailure(message: String)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    fun execute(url: String) {
        callback.onPreExecute()

        executor.execute {
            var urlConnection: HttpsURLConnection? = null
            var buffer: BufferedInputStream? = null
            var stream: InputStream? = null
            try {
                // executrando em uma nova thread [processo paralelo]
                val requestURL = URL(url) // criar conexão
                urlConnection = requestURL?.openConnection() as HttpsURLConnection // abrir conexão
                urlConnection.readTimeout = 2000 // timeout de chamada
                urlConnection.connectTimeout = 2000 // tempo de conexão

                val statusCode = urlConnection.responseCode
                if(statusCode == 400){
                    stream = urlConnection.errorStream
                    buffer = BufferedInputStream(stream)
                    val jsonAsString = toString(buffer)
                    val json = JSONObject(jsonAsString)
                    val message = json.getString("message")
                    throw IOException(message)

                } else if (statusCode > 400) {
                    throw IOException("Erro na comunicação com o servidor!")
                }

                stream = urlConnection.inputStream // sequencia de bytes
//                val jsonAsString = stream.bufferedReader().use { it.readText() }
                buffer = BufferedInputStream(stream)
                val jsonAsString = toString(buffer)

                val movieDetail = toMovieDetail(jsonAsString)
                handler.post {
                    callback.onResult(movieDetail)
                }

            } catch (e: IOException) {
                val message = e.message ?: "erro desconhecido"
                Log.e("Teste", message, e)
                handler.post {
                    callback.onFailure(message)
                }
            } finally {
                urlConnection?.disconnect()
                stream?.close()
                buffer?.close()
            }

        }
    }

    private fun toMovieDetail(jsonAsString: String): MovieDetail {
        val jsonRoot = JSONObject(jsonAsString)

        val id = jsonRoot.getInt("id")
        val title = jsonRoot.getString("title")
        val desc = jsonRoot.getString("desc")
        val coverUrl = jsonRoot.getString("cover_url")
        val cast = jsonRoot.getString("cast")
        val jsonMovies = jsonRoot.getJSONArray("movie")

        val similars = mutableListOf<Movie>()
        for (i in 0 until jsonMovies.length()) {
            val jsonMovie = jsonMovies.getJSONObject(i)
            val similarId = jsonMovie.getInt("id")
            val similarCoverUrl = jsonMovie.getString("cover_url")

            val m = Movie(similarId, similarCoverUrl)
            similars.add(m)
        }
        val movie = Movie(id, coverUrl, title, desc, cast)

        return MovieDetail(movie, similars)
    }

    private fun toString(stream: InputStream): String {
        val bytes = ByteArray(1024)
        val baos = ByteArrayOutputStream()
        var read: Int
        while (true) {
            read = stream.read(bytes)
            if (read <= 0) {
                break
            }
            baos.write(bytes, 0, read)
        }

        return String(baos.toByteArray())
    }
}