package polyrhythmmania.storymode.music

import polyrhythmmania.storymode.music.StoryMusicAssets.STEM_ID_DESKTOP_HARM
import polyrhythmmania.storymode.music.StoryMusicAssets.STEM_ID_DESKTOP_MAIN
import polyrhythmmania.storymode.music.StoryMusicAssets.STEM_ID_DESKTOP_PERC
import polyrhythmmania.storymode.music.StoryMusicAssets.STEM_ID_TITLE_FULL1
import polyrhythmmania.storymode.music.StoryMusicAssets.STEM_ID_TITLE_PERC1


object StemMixes {
    
    val titleMain: StemMix = StemMix(setOf(STEM_ID_TITLE_FULL1))
    
    val desktopResults: StemMix = StemMix(setOf(STEM_ID_DESKTOP_PERC))
    val desktopResultsSuperHard: StemMix = StemMix(setOf(STEM_ID_TITLE_PERC1))
    val desktopPreTraining101: StemMix = StemMix(setOf(STEM_ID_DESKTOP_PERC))
    val desktopInternship: StemMix = StemMix(setOf(STEM_ID_DESKTOP_PERC, STEM_ID_DESKTOP_HARM))
    val desktopMain: StemMix = StemMix(setOf(STEM_ID_DESKTOP_MAIN, STEM_ID_DESKTOP_HARM, STEM_ID_DESKTOP_PERC))
    val desktopPreBossQuiet: StemMix = StemMix(setOf(STEM_ID_DESKTOP_HARM))
    val desktopPostBossSilent: StemMix = StemMix.NONE
    val desktopPostBossMinimal: StemMix = desktopPreTraining101
    val desktopPostBossQuiet: StemMix = desktopInternship
    val desktopPostBossMain: StemMix = desktopMain
    val desktopPostGame: StemMix = StemMix(setOf(STEM_ID_DESKTOP_MAIN, STEM_ID_DESKTOP_HARM, STEM_ID_TITLE_PERC1))
    
}