package polyrhythmmania.storymode.music

import com.badlogic.gdx.Gdx


object StoryMusicAssets {
    
    const val STEM_ID_TITLE_FULL1: String = "title_full1"
    const val STEM_ID_TITLE_PERC1: String = "title_perc1"
    const val STEM_ID_DESKTOP_HARM: String = "desktop_harm"
    const val STEM_ID_DESKTOP_MAIN: String = "desktop_main"
    const val STEM_ID_DESKTOP_PERC: String = "desktop_perc"
    

    val titleStems: StemCache = StemCache(mapOf(
            STEM_ID_TITLE_FULL1 to { Stem(Gdx.files.internal("story/music/title/full1.ogg")) },
            STEM_ID_TITLE_PERC1 to { Stem(Gdx.files.internal("story/music/title/perc1.ogg")) },
            STEM_ID_DESKTOP_HARM to { Stem(Gdx.files.internal("story/music/desktop/harm.ogg")) },
            STEM_ID_DESKTOP_MAIN to { Stem(Gdx.files.internal("story/music/desktop/main.ogg")) },
            STEM_ID_DESKTOP_PERC to { Stem(Gdx.files.internal("story/music/desktop/perc.ogg")) },
    ))
    
    fun initTitleStems() {
        titleStems.loadAll()
    }
}
