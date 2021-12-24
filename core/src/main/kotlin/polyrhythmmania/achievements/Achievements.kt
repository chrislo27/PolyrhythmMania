package polyrhythmmania.achievements

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import paintbox.Paintbox
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import polyrhythmmania.PRMania
import polyrhythmmania.statistics.StatTrigger
import polyrhythmmania.achievements.Achievement.*
import polyrhythmmania.achievements.AchievementCategory.*
import polyrhythmmania.achievements.AchievementQuality.*
import polyrhythmmania.statistics.GlobalStats
import java.time.Instant


object Achievements {
    
    fun interface FulfillmentListener {
        fun onFulfilled(achievement: Achievement, fulfillment: Fulfillment)
    }
    
    const val SAVE_VERSION: Int = 1
    
    private val storageLoc: FileHandle by lazy { FileHandle(PRMania.MAIN_FOLDER.resolve("prefs/achievements.json")) }
    
    private val _achIDMap: MutableMap<String, Achievement> = linkedMapOf()
    val achievementIDMap: Map<String, Achievement> = _achIDMap
    private val _achFulfillmentMap: Var<Map<Achievement, Fulfillment>> = Var(emptyMap())
    val fulfillmentMap: ReadOnlyVar<Map<Achievement, Fulfillment>> = _achFulfillmentMap
    
    val fulfillmentListeners: MutableList<FulfillmentListener> = mutableListOf()
    
    // ACHIEVEMENT METADATA (tracking other sub-stats) -----------------------------------------------------------------
    
    // ACHIEVEMENT LIST ------------------------------------------------------------------------------------------------

    /**
     * Triggered when Endless Mode is played for the first time.
     */
    val endlessPlayFirstTime = register(StatTriggered("endless_play_first_time", STATISTICAL, ENDLESS_MODE, false, GlobalStats.timesPlayedEndlessMode, 1))
    /**
     * Triggered when getting a score of at least 25 in Endless Mode, any settings. TODO
     */
    val endlessScore25 = register(ScoreThreshold("endless_score_25", STATISTICAL, ENDLESS_MODE, false, 25))
    /**
     * Triggered when getting a score of at least 50 in Endless Mode, any settings. TODO
     */
    val endlessScore50 = register(ScoreThreshold("endless_score_50", STATISTICAL, ENDLESS_MODE, false, 50))
    /**
     * Triggered when getting a score of at least 75 in Endless Mode, any settings. TODO
     */
    val endlessScore75 = register(ScoreThreshold("endless_score_75", STATISTICAL, ENDLESS_MODE, false, 75))
    /**
     * Triggered when getting a score of at least 100 in Endless Mode, any settings. TODO
     */
    val endlessScore100 = register(ScoreThreshold("endless_score_100", STATISTICAL, ENDLESS_MODE, false, 100))
    /**
     * Triggered when getting a score of at least 125 in Endless Mode, any settings. TODO
     */
    val endlessScore125 = register(ScoreThreshold("endless_score_125", STATISTICAL, ENDLESS_MODE, false, 125))
    
    /**
     * Triggered when getting a score of at least 100 in Endless Mode with life regen disabled. TODO
     */
    val endlessNoLifeRegen100 = register(ScoreThreshold("endless_no_regen_100", STATISTICAL, ENDLESS_MODE, false, 100))
    /**
     * Triggered when getting a score of at least 100 in Endless Mode in daredevil mode. TODO
     */
    val endlessDaredevil100 = register(ScoreThreshold("endless_daredevil_100", STATISTICAL, ENDLESS_MODE, false, 100))


    /**
     * Triggered when Daily Challenge is played for the first time.
     */
    val dailyPlayFirstTime = register(StatTriggered("daily_play_first_time", STATISTICAL, DAILY, false, GlobalStats.timesPlayedDailyChallenge, 1))
    /**
     * Triggered when Daily Challenge is played twice in the same play session. TODO
     */
    val dailyTwiceInOneSession = register(Ordinary("daily_twice_in_one_session", NORMAL, DAILY, false))
    /**
     * Triggered when Daily Challenge is played 7 days in a row. TODO
     */
    val dailyWeekStreak = register(Ordinary("daily_week_streak", NORMAL, DAILY, false))
    /**
     * Triggered when getting a score of at least 25 in Daily Challenge. TODO
     */
    val dailyScore25 = register(ScoreThreshold("daily_score_25", STATISTICAL, DAILY, false, 25))
    /**
     * Triggered when getting a score of at least 50 in Daily Challenge. TODO
     */
    val dailyScore50 = register(ScoreThreshold("daily_score_50", STATISTICAL, DAILY, false, 50))
    /**
     * Triggered when getting a score of at least 75 in Daily Challenge. TODO
     */
    val dailyScore75 = register(ScoreThreshold("daily_score_75", STATISTICAL, DAILY, false, 75))
    /**
     * Triggered when getting a score of at least 100 in Daily Challenge. TODO
     */
    val dailyScore100 = register(ScoreThreshold("daily_score_100", STATISTICAL, DAILY, false, 100))
    /**
     * Triggered when getting a score of at least 125 in Daily Challenge. TODO
     */
    val dailyScore125 = register(ScoreThreshold("daily_score_125", STATISTICAL, DAILY, false, 125))
    
    
    /**
     * Triggered when Dunk is played for the first time.
     */
    val dunkPlayFirstTime = register(StatTriggered("dunk_play_first_time", STATISTICAL, EXTRAS, false, GlobalStats.timesPlayedDunk, 1))
    /**
     * Triggered when Dunk is played on a (local time) Friday between 5:00pm and 11:59pm. TODO
     */
    val dunkFridayNight = register(Ordinary("dunk_friday_night", NORMAL, EXTRAS, false))
    /**
     * Triggered when getting a score of at least 10 in Dunk. TODO
     */
    val dunkScore10 = register(ScoreThreshold("dunk_score_10", STATISTICAL, EXTRAS, false, 10))
    /**
     * Triggered when getting a score of at least 20 in Dunk. TODO
     */
    val dunkScore20 = register(ScoreThreshold("dunk_score_20", STATISTICAL, EXTRAS, false, 20))
    /**
     * Triggered when getting a score of at least 30 in Dunk. TODO
     */
    val dunkScore30 = register(ScoreThreshold("dunk_score_30", STATISTICAL, EXTRAS, false, 30))
    /**
     * Triggered when getting a score of at least 50 in Dunk. TODO
     */
    val dunkScore50 = register(ScoreThreshold("dunk_score_50", STATISTICAL, EXTRAS, false, 50))


    /**
     * Triggered when Assemble is played for the first time.
     */
    val assemblePlayFirstTime = register(StatTriggered("assemble_play_first_time", NORMAL, EXTRAS, false, GlobalStats.timesPlayedAssemble, 1))
    /**
     * Triggered when a No Miss is achieved in Assemble. TODO
     */
    val assembleNoMiss = register(Ordinary("assemble_no_miss", NORMAL, EXTRAS, false))


    /**
     * Triggered when the editor is opened for the first time.
     */
    val editorOpenFirstTime = register(StatTriggered("editor_open_first_time", NORMAL, EDITOR, false, GlobalStats.editorTime, 1))
    /**
     * Triggered when a level with at least 10 inputs is exported successfully. TODO
     */
    val editorFirstGoodExport = register(Ordinary("editor_first_good_export", NORMAL, EDITOR, false))


    /**
     * Triggered when a Perfect is earned in a level with at least 40 inputs. TODO
     */
    val perfectFirstTime = register(Ordinary("perfect_first_time", NORMAL, GENERAL, false))
    /**
     * Triggered when at least 10 unique levels are in the Library. TODO
     */
    val libraryCollection10 = register(Ordinary("library_collection_10", NORMAL, GENERAL, false))
    
    
    /**
     * Triggered when X successful inputs are gotten.
     */
    val successfulInputs1000 = register(StatTriggered("successful_inputs_1000", NORMAL, GENERAL, false, GlobalStats.inputsGottenTotal, 1000))
    /**
     * Triggered when X ace inputs are gotten.
     */
    val aceInputs1000 = register(StatTriggered("ace_inputs_1000", NORMAL, GENERAL, false, GlobalStats.inputsGottenAce, 1000))
    /**
     * Triggered when X rods have exploded.
     */
    val rodsExploded10000 = register(StatTriggered("rods_exploded_10000", NORMAL, GENERAL, false, GlobalStats.rodsExploded, 10000))
    /**
     * Triggered when a Skill Star is earned for the 5th time.
     */
    val skillStar5 = register(StatTriggered("skill_star_5", NORMAL, GENERAL, false, GlobalStats.skillStarsEarned, 5))
    /**
     * Triggered when a Skill Star is earned for the 100th time.
     */
    val skillStar100 = register(StatTriggered("skill_star_100", NORMAL, GENERAL, false, GlobalStats.skillStarsEarned, 100))
    /**
     * Triggered when 30 unique levels have been played.
     */
    val uniqueLevelsPlayed30 = register(StatTriggered("unique_levels_played_30", NORMAL, GENERAL, false, GlobalStats.timesPlayedUniqueCustomLevel, 30))
    /**
     * Triggered when 10 hours of total playtime have been reached.
     */
    val playtimeHours10 = register(StatTriggered("playtime_hours_10", NORMAL, GENERAL, false, GlobalStats.totalPlayTime, 10 * 60 * 60))

    
    /**
     * Triggered in the editor when a rod is launched with no other platforms raised afterward. TODO
     */
    val rodToSpace = register(Ordinary("rod_to_space", NORMAL, EDITOR, false))
    /**
     * Triggered in Endless Mode when paused in between a pattern and the point is still awarded. TODO
     */
    val endlessPauseBetweenInputs = register(Ordinary("endless_pause_between_inputs", NORMAL, ENDLESS_MODE, false))
    /**
     * Triggered in Endless Mode when getting a score of at least 50 while the master volume is 0. TODO
     */
    val endlessSilentEndless = register(Ordinary("endless_silent_endless", NORMAL, ENDLESS_MODE, false))
    /**
     * Triggered after every Tutorial (I + II) has been completed. TODO
     */
    val playAllTutorials = register(Ordinary("play_all_tutorials", NORMAL, GENERAL, false))
    /**
     * Triggered after every Practice (PR1 + 2) has been completed. TODO
     */
    val playAllPractices = register(Ordinary("play_all_practices", NORMAL, GENERAL, false))
    /**
     * Triggered after scrolling to the end of the credits list. TODO
     */
    val seeAllCredits = register(Ordinary("see_all_credits", NORMAL, GENERAL, false))

    
    /**
     * Triggered when all other achievements are awarded.
     */
    val polyrhythmManiac = register(Ordinary("polyrhythm_maniac", CHALLENGE, GENERAL, false))
    
    
    // -----------------------------------------------------------------------------------------------------------------
    
    init {
        // Polyrhythm Maniac achievement awarding
        val allOtherAchievements = achievementIDMap.values - polyrhythmManiac
        _achFulfillmentMap.addListener { mapVar ->
            val map = mapVar.getOrCompute()
            if (polyrhythmManiac !in map) {
                if (map.keys.containsAll(allOtherAchievements)) {
                    Gdx.app.postRunnable { awardAchievement(polyrhythmManiac) }
                }
            }
        }
    }
    
    private fun <A : Achievement> register(ach: A): A {
        _achIDMap[ach.id] = ach
        if (ach is StatTriggered) {
            // Add stat trigger for fulfillment
            ach.stat.triggers += StatTrigger { stat, oldValue, newValue ->
                if (newValue >= ach.threshold) {
                    awardAchievement(ach)
                }
            }
        }
        return ach
    }
    
    // -----------------------------------------------------------------------------------------------------------------
    
    fun clearAllFulfilledAchievements() {
        _achFulfillmentMap.set(emptyMap())
    }

    /**
     * Marks the [achievement] as fulfilled.
     */
    fun awardAchievement(achievement: Achievement) {
        val previousMap = _achFulfillmentMap.getOrCompute()
        if (achievement !in previousMap) {
            val fulfillment = Fulfillment(Instant.now())
            _achFulfillmentMap.set(previousMap + (achievement to fulfillment))
            fulfillmentListeners.forEach { it.onFulfilled(achievement, fulfillment) }
            Paintbox.LOGGER.info("Achievement awarded: ${achievement.id}", "Achievements")
        }
    }

    fun attemptAwardScoreAchievement(achievement: ScoreThreshold, score: Int) {
        if (achievement !in _achFulfillmentMap.getOrCompute() && score >= achievement.scoreMinimum) {
            awardAchievement(achievement)
        }
    }

    /**
     * Manually forces a check for all [Achievement.StatTriggered] achievements.
     */
    fun checkAllStatTriggeredAchievements() {
        _achIDMap.values.forEach { ach ->
            if (ach is StatTriggered) {
                val newValue = ach.stat.value.get()
                if (newValue >= ach.threshold) {
                    awardAchievement(ach)
                }
            }
        }
    }
    
    // -----------------------------------------------------------------------------------------------------------------
    
    fun fromJson(rootObj: JsonObject) {
        clearAllFulfilledAchievements()
        
        val newMap = mutableMapOf<Achievement, Fulfillment>()
        val achObj = rootObj["achievements"].asObject()
        for (ach in achievementIDMap.values) {
            try {
                val fObj = achObj[ach.id].asObject()
                if (fObj != null) {
                    val f = Fulfillment.fromJson(fObj) ?: continue
                    newMap[ach] = f
                }
            } catch (ignored: Exception) {}
        }
        this._achFulfillmentMap.set(newMap)
    }

    fun fromJsonFile(file: FileHandle) {
        clearAllFulfilledAchievements()
        if (!file.exists() || file.isDirectory) return
        
        return try {
            val str = file.readString("UTF-8")
            fromJson(Json.parse(str).asObject())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toJson(rootObj: JsonObject) {
        rootObj.add("version", SAVE_VERSION)
        rootObj.add("achievements", Json.`object`().also { obj ->
            fulfillmentMap.getOrCompute().forEach { (ach, ful) ->
                obj.add(ach.id, Json.`object`().also { o ->
                    ful.toJson(o)
                })
            }
        })
    }

    fun toJsonFile(file: FileHandle) {
        try {
            file.writeString(Json.`object`().also { obj ->
                toJson(obj)
            }.toString(), false, "UTF-8")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun load() {
        Paintbox.LOGGER.debug("Achievements loaded", "Achievements")
        this.fromJsonFile(storageLoc)
    }

    fun persist() {
        Paintbox.LOGGER.debug("Achievements saved", "Achievements")
        this.toJsonFile(storageLoc)
    }
}