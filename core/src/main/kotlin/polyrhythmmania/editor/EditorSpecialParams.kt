package polyrhythmmania.editor

import polyrhythmmania.storymode.StorySession


data class EditorSpecialParams(
        val storySession: StorySession? = null, // For STORY_MODE flag
)
