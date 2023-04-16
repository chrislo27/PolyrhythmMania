package polyrhythmmania.storymode.music

import com.badlogic.gdx.Gdx
import paintbox.util.closeQuietly
import java.io.Closeable


object StoryMusicAssets : Closeable {
    
    const val STEM_ID_TITLE_FULL1: String = "title_full1"
    const val STEM_ID_TITLE_PERC1: String = "title_perc1"
    const val STEM_ID_DESKTOP_HARM: String = "desktop_harm"
    const val STEM_ID_DESKTOP_MAIN: String = "desktop_main"
    const val STEM_ID_DESKTOP_PERC: String = "desktop_perc"
    const val STEM_ID_BONUS: String = "desktop_bonus"
    
    val STEM_ID_BOSS_1_INTRO: StemID = StemID("boss1_intro")
    val STEM_ID_BOSS_1_A1: StemID = StemID("boss1_a1")
    val STEM_ID_BOSS_1_A2: StemID = StemID("boss1_a2")
    val STEM_ID_BOSS_1_B1: StemID = StemID("boss1_b1")
    val STEM_ID_BOSS_1_B2: StemID = StemID("boss1_b2")
    val STEM_ID_BOSS_1_C: StemID = StemID("boss1_c", variants = 3)
    val STEM_ID_BOSS_1_D: StemID = StemID("boss1_d", variants = 3)
    val STEM_ID_BOSS_1_E1: StemID = StemID("boss1_e1")
    val STEM_ID_BOSS_1_E2: StemID = StemID("boss1_e2")
    val STEM_ID_BOSS_1_F: StemID = StemID("boss1_f", variants = 3)
    

    val titleStems: StemCache = StemCache(mapOf(
            STEM_ID_TITLE_FULL1 to { Stem(Gdx.files.internal("story/music/title/full1.ogg")) },
            STEM_ID_TITLE_PERC1 to { Stem(Gdx.files.internal("story/music/title/perc1.ogg")) },
            STEM_ID_DESKTOP_HARM to { Stem(Gdx.files.internal("story/music/desktop/harm.ogg")) },
            STEM_ID_DESKTOP_MAIN to { Stem(Gdx.files.internal("story/music/desktop/main.ogg")) },
            STEM_ID_DESKTOP_PERC to { Stem(Gdx.files.internal("story/music/desktop/perc.ogg")) },
    ))
    val bonusMusicStems: StemCache = StemCache(mapOf(
            STEM_ID_BONUS to { Stem(Gdx.files.internal("story/music/postgame/end_of_the_assembly_line.ogg"), inMemory = true) },
    ))
    val bossStems: StemCache = run {
        val fileExt = "ogg"
        val stemIDsToPathBase: List<Pair<StemID, String>> = listOf(
            STEM_ID_BOSS_1_INTRO to "story/music/boss/boss1_intro",
            STEM_ID_BOSS_1_A1 to "story/music/boss/boss1_a1",
            STEM_ID_BOSS_1_A2 to "story/music/boss/boss1_a2",
            STEM_ID_BOSS_1_B1 to "story/music/boss/boss1_b1",
            STEM_ID_BOSS_1_B2 to "story/music/boss/boss1_b2",
            STEM_ID_BOSS_1_C to "story/music/boss/boss1_c",
            STEM_ID_BOSS_1_D to "story/music/boss/boss1_d",
            STEM_ID_BOSS_1_E1 to "story/music/boss/boss1_e1",
            STEM_ID_BOSS_1_E2 to "story/music/boss/boss1_e2",
            STEM_ID_BOSS_1_F to "story/music/boss/boss1_f",
        )
        val stemIDsWithVariantsToPath: List<Pair<String, String>> = stemIDsToPathBase.flatMap { (stemID, pathBase) ->
            if (stemID.hasNoVariants) {
                listOf(stemID.getID(StemID.NO_VARIANT) to "${pathBase}.${fileExt}")
            } else {
                stemID.getAllIDsWithVariant().map { (variant, id) ->
                    id to "${pathBase}_var${variant}.${fileExt}"
                }
            }
        }

        StemCache(stemIDsWithVariantsToPath.associate { (id, filename) ->
            id to { Stem(Gdx.files.internal(filename), inMemory = true) }
        })
    }
    
    fun initTitleStems() {
        titleStems.loadAll()
    }
    
    fun initBonusMusicStems() {
        bonusMusicStems.loadAll()
    }
    
    fun initBossStems() {
        bossStems.loadAll()
    }
    
    override fun close() {
        listOf(titleStems, bonusMusicStems, bossStems).forEach { stemCache ->
            stemCache.closeQuietly()
        }
    }
}
