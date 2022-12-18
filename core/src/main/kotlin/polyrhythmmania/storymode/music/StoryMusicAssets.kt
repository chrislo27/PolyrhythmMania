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
    
    const val STEM_ID_BOSS_1_INTRO: String = "boss1_intro"
    const val STEM_ID_BOSS_1_A1: String = "boss1_a1"
    const val STEM_ID_BOSS_1_A2: String = "boss1_a2"
    const val STEM_ID_BOSS_1_B1: String = "boss1_b1"
    const val STEM_ID_BOSS_1_B2: String = "boss1_b2"
    const val STEM_ID_BOSS_1_C: String = "boss1_c"
    const val STEM_ID_BOSS_1_D: String = "boss1_d"
    const val STEM_ID_BOSS_1_E1: String = "boss1_e1"
    const val STEM_ID_BOSS_1_E2: String = "boss1_e2"
    const val STEM_ID_BOSS_1_F: String = "boss1_f"
    const val STEM_ID_BOSS_2: String = "boss2"
    

    val titleStems: StemCache = StemCache(mapOf(
            STEM_ID_TITLE_FULL1 to { Stem(Gdx.files.internal("story/music/title/full1.ogg")) },
            STEM_ID_TITLE_PERC1 to { Stem(Gdx.files.internal("story/music/title/perc1.ogg")) },
            STEM_ID_DESKTOP_HARM to { Stem(Gdx.files.internal("story/music/desktop/harm.ogg")) },
            STEM_ID_DESKTOP_MAIN to { Stem(Gdx.files.internal("story/music/desktop/main.ogg")) },
            STEM_ID_DESKTOP_PERC to { Stem(Gdx.files.internal("story/music/desktop/perc.ogg")) },
    ))
    val bossStems: StemCache = StemCache(mapOf(
            STEM_ID_BOSS_1_INTRO to { Stem(Gdx.files.internal("story/music/boss/boss1_intro.ogg")) },
            STEM_ID_BOSS_1_A1 to { Stem(Gdx.files.internal("story/music/boss/boss1_a1.ogg")) },
            STEM_ID_BOSS_1_A2 to { Stem(Gdx.files.internal("story/music/boss/boss1_a2.ogg")) },
            STEM_ID_BOSS_1_B1 to { Stem(Gdx.files.internal("story/music/boss/boss1_b1_pat0.ogg")) },
            STEM_ID_BOSS_1_B2 to { Stem(Gdx.files.internal("story/music/boss/boss1_b2_pat0.ogg")) },
            STEM_ID_BOSS_1_C to { Stem(Gdx.files.internal("story/music/boss/boss1_c_pat0.ogg")) },
            STEM_ID_BOSS_1_D to { Stem(Gdx.files.internal("story/music/boss/boss1_d_pat0.ogg")) },
            STEM_ID_BOSS_1_E1 to { Stem(Gdx.files.internal("story/music/boss/boss1_e1.ogg")) },
            STEM_ID_BOSS_1_E2 to { Stem(Gdx.files.internal("story/music/boss/boss1_e2.ogg")) },
            STEM_ID_BOSS_1_F to { Stem(Gdx.files.internal("story/music/boss/boss1_f_pat0.ogg")) },
            STEM_ID_BOSS_2 to { Stem(Gdx.files.internal("story/music/boss/boss2.ogg")) },
    ))
    
    fun initTitleStems() {
        titleStems.loadAll()
    }
    
    fun initBossStems() {
        bossStems.loadAll()
    }
    
    override fun close() {
        listOf(titleStems, bossStems).forEach { stemCache ->
            stemCache.closeQuietly()
        }
    }
}
