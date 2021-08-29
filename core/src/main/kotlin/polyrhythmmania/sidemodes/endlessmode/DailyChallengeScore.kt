package polyrhythmmania.sidemodes.endlessmode

import com.badlogic.gdx.Gdx
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import org.apache.http.util.EntityUtils
import paintbox.Paintbox
import paintbox.binding.Var
import polyrhythmmania.PRMania
import polyrhythmmania.PRManiaGame
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.thread


data class DailyChallengeScore(val date: LocalDate, val score: Int) {
    companion object {
        val ZERO: DailyChallengeScore = DailyChallengeScore(LocalDate.MIN, 0)
    }
}

data class EndlessHighScore(val seed: UInt, val score: Int) {
    companion object {
        val ZERO: EndlessHighScore = EndlessHighScore(0u, -1)
    }
}

data class DailyLeaderboardScore(val countryCode: String, val score: Int, val name: String)

object DailyChallengeUtils {
    
    fun sendNonceRequest(date: LocalDate, nonceVar: Var<UUID?>) {
        thread(isDaemon = true, name = "Daily Challenge UUID getter", start = true) {
            val post = HttpPost(
                    URIBuilder("https://api.rhre.dev:10443/prmania/dailychallenge/start/${date.format(DateTimeFormatter.ISO_DATE)}")
                            .setParameter("v", PRMania.VERSION.toString())
                            .build()
            )
            try {
                val httpClient = PRManiaGame.instance.httpClient
                httpClient.execute(post).use { response ->
                    val status = response.statusLine.statusCode
                    if (status == 200) {
                        val content = EntityUtils.toString(response.entity)
                        try {
                            val uuid: UUID = UUID.fromString(content.trim())
                            nonceVar.set(uuid)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Paintbox.LOGGER.warn("Failed to get daily challenge high score nonce from server: bad uuid $content")
                        }
                    } else {
                        Paintbox.LOGGER.warn("Failed to get daily challenge high score nonce from server: status was $status for url ${post.uri} ${EntityUtils.toString(response.entity)}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun submitHighScore(date: LocalDate, score: Int, name: String, nonce: UUID,
                        noCountry: Boolean) {
        thread(isDaemon = true, name = "Daily Challenge high score submission", start = true) {
            val uriBuilder = URIBuilder("https://api.rhre.dev:10443/prmania/dailychallenge/submit/${date.format(DateTimeFormatter.ISO_DATE)}")
                    .setParameter("v", PRMania.VERSION.toString())
                    .setParameter("uuid", nonce.toString())
                    .setParameter("score", score.toString())
                    .setParameter("name", name)
            if (noCountry) {
                uriBuilder.setParameter("nocountry", "1")
            }
            
            val post = HttpPost(uriBuilder.build())
            try {
                val httpClient = PRManiaGame.instance.httpClient
                httpClient.execute(post).use { response ->
                    val status = response.statusLine.statusCode
                    if (status != 204) {
                        Paintbox.LOGGER.warn("Failed to post daily challenge high score: status was $status for url ${post.uri} ${EntityUtils.toString(response.entity)}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getLeaderboard(date: LocalDate, listVar: Var<List<DailyLeaderboardScore>?>) {
        thread(isDaemon = true, name = "Daily Challenge leaderboard GET", start = true) {
            val uriBuilder = URIBuilder("https://api.rhre.dev:10443/prmania/dailychallenge/top/${date.format(DateTimeFormatter.ISO_DATE)}")
                    .setParameter("v", PRMania.VERSION.toString())

            val get = HttpGet(uriBuilder.build())
            var returnList: List<DailyLeaderboardScore>? = null
            try {
                val httpClient = PRManiaGame.instance.httpClient
                httpClient.execute(get).use { response ->
                    val status = response.statusLine.statusCode
                    if (status != 200) {
                        Paintbox.LOGGER.warn("Failed to get daily challenge leaderboard: status was $status for url ${get.uri} ${EntityUtils.toString(response.entity)}")
                    } else {
                        val content = EntityUtils.toString(response.entity)
                        val jsonArray = Json.parse(content).asArray()
                        val list = mutableListOf<DailyLeaderboardScore>()
                        jsonArray.forEach { v ->
                            v as JsonObject
                            list += DailyLeaderboardScore(v.getString("countryCode", ""), v.getInt("score", 0), v.getString("name", ""))
                        }
                        returnList = list
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                returnList = null
            }
            
            Gdx.app.postRunnable { 
                listVar.set(returnList)
            }
        }
    }
}
