package polyrhythmmania.engine.input

import com.badlogic.gdx.math.MathUtils
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import paintbox.util.sumOfFloat
import polyrhythmmania.Localization
import kotlin.math.abs

data class ResultsText(
        val title: String?,
        val ok: String?,
        val firstNegative: String?,
        val secondNegative: String?,
        val firstPositive: String?,
        val secondPositive: String?,
                       ) {
    companion object {
        val DEFAULT: ResultsText = ResultsText(null, null, null, null, null, null)
        
        fun fromJson(obj: JsonObject): ResultsText {
            val title: String? = obj.getString("title", null)
            val ok: String? = obj.getString("ok", null)
            val firstNegative: String? = obj.getString("firstNegative", null)
            val secondNegative: String? = obj.getString("secondNegative", null)
            val firstPositive: String? = obj.getString("firstPositive", null)
            val secondPositive: String? = obj.getString("secondPositive", null)
            return ResultsText(title, ok, firstNegative, secondNegative, firstPositive, secondPositive)
        }
    }
    
    fun toJson(): JsonObject {
        return Json.`object`().apply { 
            add("title", title)
            add("ok", ok)
            add("firstNegative", firstNegative)
            add("secondNegative", secondNegative)
            add("firstPositive", firstPositive)
            add("secondPositive", secondPositive)
        }
    }


    fun generateLinesOfText(score: Int, badLeftGoodRight: Boolean, doRandomization: Boolean = true): Pair<String, String> {
        val resultsText = this
        return when {
            score < 60 -> if (resultsText.firstNegative != null) {
                Pair(resultsText.firstNegative, resultsText.secondNegative ?: "")
            } else {
                // If the dpad side wasn't as good as the A side...
                Pair(if (badLeftGoodRight) Localization.getValue("play.results.defaultTryAgain.2")
                else Localization.getValue("play.results.defaultTryAgain.1"),
                        if (score < 50) Localization.getValue("play.results.defaultTryAgain.3") else "")
            }
            score in 60 until 75 -> {
                Pair(resultsText.ok ?: Localization.getValue("play.results.defaultOK.${if (doRandomization) MathUtils.random(1, 4) else 1}"), "")
            }
            score >= 75 -> {
                if (resultsText.firstPositive != null) {
                    Pair(resultsText.firstPositive, if (score >= 85) (resultsText.secondPositive ?: "") else "")
                } else {
                    Pair(Localization.getValue("play.results.defaultSuperb.1"),
                            if (score >= 85) Localization.getValue("play.results.defaultSuperb.2") else "")
                }
            }
            else -> "Score was out of bounds ($score)" to ""
        }
    }
}