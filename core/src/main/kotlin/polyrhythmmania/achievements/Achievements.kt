package polyrhythmmania.achievements

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonObject
import paintbox.Paintbox
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.binding.VarChangedListener
import polyrhythmmania.PRMania
import polyrhythmmania.statistics.StatTrigger
import polyrhythmmania.achievements.Achievement.*
import polyrhythmmania.achievements.AchievementCategory.*
import polyrhythmmania.achievements.AchievementRank.*
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
    
    var tutorialFlag: Int = 0 // LSB = I, next is II
    var practiceFlag: Int = 0 // LSB = PR1, next is PR2
    
    // ACHIEVEMENT LIST ------------------------------------------------------------------------------------------------


    // Category GENERAL
    /**
     * Triggered after every Tutorial (I + II) has been completed.
     */
    val playAllTutorials = register(Ordinary("play_all_tutorials", OBJECTIVE, GENERAL, false))
    /**
     * Triggered after every Practice (PR1 + 2) has been completed with an OK or better.
     */
    val playAllPractices = register(Ordinary("play_all_practices", OBJECTIVE, GENERAL, false))
    /**
     * Triggered when a Perfect is earned in a level with at least 40 inputs.
     */
    val perfectFirstTime = register(NumericalThreshold("perfect_first_time", OBJECTIVE, GENERAL, false, 40))
    /**
     * Triggered when at least 15 unique levels are in the Library.
     */
    val libraryCollection15 = register(NumericalThreshold("library_collection_15", STATISTICAL, GENERAL, false, 15))
    /**
     * Triggered when 30 unique levels have been played.
     */
    val uniqueLevelsPlayed30 = register(StatTriggered("unique_levels_played_30", STATISTICAL, GENERAL, false, GlobalStats.timesPlayedUniqueCustomLevel, 30))
    
    /**
     * Triggered when X successful inputs are gotten.
     */
    val successfulInputs1000 = register(StatTriggered("successful_inputs_1000", STATISTICAL, GENERAL, false, GlobalStats.inputsGottenTotal, 1000))
    /**
     * Triggered when X successful inputs are gotten.
     */
    val successfulInputs5000 = register(StatTriggered("successful_inputs_5000", STATISTICAL, GENERAL, false, GlobalStats.inputsGottenTotal, 5000))
    /**
     * Triggered when X successful inputs are gotten.
     */
    val successfulInputs10000 = register(StatTriggered("successful_inputs_10000", OBJECTIVE, GENERAL, false, GlobalStats.inputsGottenTotal, 10000))
    /**
     * Triggered when X ace inputs are gotten.
     */
    val aceInputs1000 = register(StatTriggered("ace_inputs_1000", STATISTICAL, GENERAL, false, GlobalStats.inputsGottenAce, 1000))
    /**
     * Triggered when X ace inputs are gotten.
     */
    val aceInputs5000 = register(StatTriggered("ace_inputs_5000", OBJECTIVE, GENERAL, false, GlobalStats.inputsGottenAce, 5000))
    /**
     * Triggered when a Skill Star is earned for the 5th time.
     */
    val skillStar5 = register(StatTriggered("skill_star_5", STATISTICAL, GENERAL, false, GlobalStats.skillStarsEarned, 5))
    /**
     * Triggered when a Skill Star is earned for the 100th time.
     */
    val skillStar100 = register(StatTriggered("skill_star_100", OBJECTIVE, GENERAL, false, GlobalStats.skillStarsEarned, 100))
    /**
     * Triggered when X rods have exploded.
     */
    val rodsExploded10000 = register(StatTriggered("rods_exploded_10000", STATISTICAL, GENERAL, false, GlobalStats.rodsExploded, 10000))
    /**
     * Triggered when 10 hours of total playtime have been reached.
     */
    val playtimeHours10 = register(StatTriggered("playtime_hours_10", STATISTICAL, GENERAL, false, GlobalStats.totalPlayTime, 10 * (60 * 60)))

    
    // Category ENDLESS_MODE
    /**
     * Triggered when Endless Mode is played for the first time.
     */
    val endlessPlayFirstTime = register(StatTriggered("endless_play_first_time", STATISTICAL, ENDLESS_MODE, false, GlobalStats.timesPlayedEndlessMode, 1, showProgress = false))
    /**
     * Triggered when getting a score of at least 25 in Endless Mode, any settings.
     */
    val endlessScore25 = register(ScoreThreshold("endless_score_25", OBJECTIVE, ENDLESS_MODE, false, 25))
    /**
     * Triggered when getting a score of at least 50 in Endless Mode, any settings.
     */
    val endlessScore50 = register(ScoreThreshold("endless_score_50", OBJECTIVE, ENDLESS_MODE, false, 50))
    /**
     * Triggered when getting a score of at least 75 in Endless Mode, any settings.
     */
    val endlessScore75 = register(ScoreThreshold("endless_score_75", OBJECTIVE, ENDLESS_MODE, false, 75))
    /**
     * Triggered when getting a score of at least 100 in Endless Mode, any settings.
     */
    val endlessScore100 = register(ScoreThreshold("endless_score_100", OBJECTIVE, ENDLESS_MODE, false, 100))
    /**
     * Triggered when getting a score of at least 125 in Endless Mode, any settings.
     */
    val endlessScore125 = register(ScoreThreshold("endless_score_125", CHALLENGE, ENDLESS_MODE, false, 125))
    
    /**
     * Triggered when getting a score of at least 100 in Endless Mode with life regen disabled.
     */
    val endlessNoLifeRegen100 = register(ScoreThreshold("endless_no_regen_100", CHALLENGE, ENDLESS_MODE, false, 100))
    /**
     * Triggered when getting a score of at least 100 in Endless Mode in daredevil mode.
     */
    val endlessDaredevil100 = register(ScoreThreshold("endless_daredevil_100", CHALLENGE, ENDLESS_MODE, false, 100))

    /**
     * Triggered in Endless Mode when paused in between a pattern and the point is still awarded at score 50 or greater.
     * NB: This will also trigger if the new score is 50.
     */
    val endlessPauseBetweenInputs = register(ScoreThreshold("endless_pause_between_inputs", CHALLENGE, ENDLESS_MODE, false, 50))
    /**
     * Triggered in Endless Mode when getting a score of at least 50 while the master volume is 0.
     */
    val endlessSilent50 = register(ScoreThreshold("endless_silent_50", CHALLENGE, ENDLESS_MODE, false, 50))


    // Category DAILY
    /**
     * Triggered when Daily Challenge is played for the first time.
     */
    val dailyPlayFirstTime = register(StatTriggered("daily_play_first_time", STATISTICAL, DAILY, false, GlobalStats.timesPlayedDailyChallenge, 1, showProgress = false))
    /**
     * Triggered when Daily Challenge is played twice in the same play session.
     */
    val dailyTwiceInOneSession = register(Ordinary("daily_twice_in_one_session", OBJECTIVE, DAILY, false))
    /**
     * Triggered when Daily Challenge is played 7 days in a row.
     */
    val dailyWeekStreak = register(Ordinary("daily_week_streak", OBJECTIVE, DAILY, false))
    /**
     * Triggered when getting a score of at least 25 in Daily Challenge.
     */
    val dailyScore25 = register(ScoreThreshold("daily_score_25", OBJECTIVE, DAILY, false, 25))
    /**
     * Triggered when getting a score of at least 50 in Daily Challenge.
     */
    val dailyScore50 = register(ScoreThreshold("daily_score_50", OBJECTIVE, DAILY, false, 50))
    /**
     * Triggered when getting a score of at least 75 in Daily Challenge.
     */
    val dailyScore75 = register(ScoreThreshold("daily_score_75", OBJECTIVE, DAILY, false, 75))
    /**
     * Triggered when getting a score of at least 100 in Daily Challenge.
     */
    val dailyScore100 = register(ScoreThreshold("daily_score_100", OBJECTIVE, DAILY, false, 100))
    /**
     * Triggered when getting a score of at least 125 in Daily Challenge.
     */
    val dailyScore125 = register(ScoreThreshold("daily_score_125", CHALLENGE, DAILY, false, 125))

    
    // Category EDITOR
    /**
     * Triggered when the editor is opened for the first time.
     */
    val editorOpenFirstTime = register(StatTriggered("editor_open_first_time", OBJECTIVE, EDITOR, false, GlobalStats.editorTime, 1, showProgress = false))
    /**
     * Triggered when a level with at least 20 inputs is exported successfully.
     */
    val editorFirstGoodExport = register(NumericalThreshold("editor_first_good_export", OBJECTIVE, EDITOR, false, 20))
    /**
     * Triggered in the editor when a rod is launched with no other platforms raised afterward.
     */
    val rodToSpace = register(Ordinary("rod_to_space", OBJECTIVE, EDITOR, isHidden = true))
    
    
    // Category EXTRAS
    /**
     * Triggered when Dunk is played for the first time.
     */
    val dunkPlayFirstTime = register(StatTriggered("dunk_play_first_time", STATISTICAL, EXTRAS, false, GlobalStats.timesPlayedDunk, 1, showProgress = false))
    /**
     * Triggered when Dunk is played on a (local time) Friday between 5:00pm and 11:59pm.
     */
    val dunkFridayNight = register(Ordinary("dunk_friday_night", OBJECTIVE, EXTRAS, false))
    /**
     * Triggered when getting a score of at least 10 in Dunk.
     */
    val dunkScore10 = register(ScoreThreshold("dunk_score_10", OBJECTIVE, EXTRAS, false, 10))
    /**
     * Triggered when getting a score of at least 20 in Dunk.
     */
    val dunkScore20 = register(ScoreThreshold("dunk_score_20", OBJECTIVE, EXTRAS, false, 20))
    /**
     * Triggered when getting a score of at least 30 in Dunk.
     */
    val dunkScore30 = register(ScoreThreshold("dunk_score_30", OBJECTIVE, EXTRAS, false, 30))
    /**
     * Triggered when getting a score of at least 50 in Dunk.
     */
    val dunkScore50 = register(ScoreThreshold("dunk_score_50", OBJECTIVE, EXTRAS, false, 50))
    
    /**
     * Triggered when Assemble is played for the first time.
     */
    val assemblePlayFirstTime = register(StatTriggered("assemble_play_first_time", STATISTICAL, EXTRAS, false, GlobalStats.timesPlayedAssemble, 1, showProgress = false))
    /**
     * Triggered when a No Miss is achieved in Assemble.
     */
    val assembleNoMiss = register(Ordinary("assemble_no_miss", OBJECTIVE, EXTRAS, false))

    
    // End of Category GENERAL
    /**
     * Triggered after scrolling to the end of the credits list.
     */
    val seeAllCredits = register(Ordinary("see_all_credits", OBJECTIVE, GENERAL, isHidden = true))
    
    /**
     * Triggered when all other achievements are awarded.
     */
    val polyrhythmManiac = register(Ordinary("polyrhythm_maniac", CHALLENGE, GENERAL, false))
    
    
    // -----------------------------------------------------------------------------------------------------------------

    // Polyrhythm Maniac achievement awarding
    private val allOtherAchievements = achievementIDMap.values - polyrhythmManiac
    private val maniacListener = VarChangedListener<Map<Achievement, Fulfillment>> { mapVar ->
        val map = mapVar.getOrCompute()
        if (polyrhythmManiac !in map) {
            if (map.keys.containsAll(allOtherAchievements)) {
                Gdx.app.postRunnable {
                    awardAchievement(polyrhythmManiac)
                }
            }
        }
    }
    
    init {
        _achFulfillmentMap.addListener(maniacListener)
    }
    
//    init {
//        dumpAchievementList()
//    }
    
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
    
    fun dumpAchievementList() {
        Paintbox.LOGGER.debug("\n" + achievementIDMap.values.groupBy { it.category }.entries.sortedBy { it.key }.joinToString(separator = "\n") { (category, list) ->
            val listStr = list.joinToString(separator = "\n-------------------------------------------\n") { ach ->
                "${ach.getLocalizedName().getOrCompute()}\nRank: ${ach.rank} | Hidden? ${ach.isHidden} | ID: ${ach.id}\n${ach.getLocalizedDesc().getOrCompute()}"
            }
            
            "\n\n\n===========================================\nCategory $category:\n$listStr"
        } + "\n")
    }
    
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
    
    fun attemptAwardThresholdAchievement(achievement: NumericalThreshold, value: Int) {
        if (achievement !in _achFulfillmentMap.getOrCompute() && value >= achievement.minimumValue) {
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
        
        tutorialFlag = 0
        try {
            tutorialFlag = rootObj.getInt("tutorialFlag", 0)
        } catch (ignored: Exception) {}
        practiceFlag = 0
        try {
            practiceFlag = rootObj.getInt("practiceFlag", 0)
        } catch (ignored: Exception) {}
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
        rootObj.add("tutorialFlag", tutorialFlag)
        rootObj.add("practiceFlag", practiceFlag)
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
        this.fromJsonFile(storageLoc)
        Paintbox.LOGGER.debug("Achievements loaded", "Achievements")
    }

    fun persist() {
        this.toJsonFile(storageLoc)
        Paintbox.LOGGER.debug("Achievements saved", "Achievements")
    }
}