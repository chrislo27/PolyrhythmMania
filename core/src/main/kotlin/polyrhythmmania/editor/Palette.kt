package polyrhythmmania.editor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import paintbox.binding.Var
import paintbox.font.Markup
import paintbox.font.PaintboxFont
import paintbox.font.TextRun
import paintbox.registry.AssetRegistry
import paintbox.util.gdxutils.grey
import polyrhythmmania.PRManiaColors
import polyrhythmmania.PRManiaGame


open class Palette(val main: PRManiaGame) {
    protected val background: Var<Color> = Var(Color().grey(0f, 0.75f))
    protected val borderTrim: Var<Color> = Var(Color().grey(1f, 1f))
    protected val separator: Var<Color> = Var(Color().grey(0.75f, 0.5f))

    val bgColor: Var<Color> = Var(Color().grey(0.094f))
    val toolbarBg: Var<Color> = Var(Color().grey(0.3f))

    val menubarIconTint: Var<Color> = Var(Color().grey(0.1f))

    val statusBg: Var<Color> = Var.bind { background.use() }
    val statusBorder: Var<Color> = Var.bind { borderTrim.use() }
    val statusTextColor: Var<Color> = Var(Color().grey(1f, 1f))

    val upperPaneBorder: Var<Color> = Var.bind { borderTrim.use() }
    val instantiatorPaneBorder: Var<Color> = Var.bind { upperPaneBorder.use() }
    val instantiatorSummaryText: Var<Color> = Var.bind { Color().grey(1f, 1f) }
    val instantiatorDescText: Var<Color> = Var.bind { instantiatorSummaryText.use() }
    val previewPaneBorder: Var<Color> = Var.bind { borderTrim.use() }
    val previewPaneSeparator: Var<Color> = Var.bind { separator.use() }
    val toolbarIconTint: Var<Color> = Var.bind { menubarIconTint.use() }
    val toolbarIconToolNeutralTint: Var<Color> = Var.bind { toolbarIconTint.use() }
    val toolbarIconToolActiveTint: Var<Color> = Var(Color.valueOf("00A070"))
    val toolbarIndentedButtonBorderTint: Var<Color> = Var(Color.valueOf("00F2AD"))

    val trackPaneBorder: Var<Color> = Var.bind { borderTrim.use() }
    val trackPaneTimeBg: Var<Color> = Var(Color().grey(0f, 1f))
    val trackPaneTimeText: Var<Color> = Var(Color().grey(1f, 1f))
    val trackPaneTextColor: Var<Color> = Var(Color().grey(1f, 1f))
    val trackPaneTempoBg: Var<Color> = Var(PRManiaColors.TEMPO)
    val trackPaneMusicVolBg: Var<Color> = Var(PRManiaColors.MUSIC_VOLUME)
    val trackVerticalBeatLineColor: Var<Color> = Var(Color().grey(1f, 0.4f))
    val trackPlayback: Var<Color> = Var(Color(0f, 1f, 0f, 1f))

    // Fonts

    val sidePanelFont: PaintboxFont = main.fontEditorBordered
    val beatTimeFont: PaintboxFont = main.fontEditorBeatTime
    val beatSecondsFont: PaintboxFont = main.fontCache["editor_status"]
    val beatTrackFont: PaintboxFont = main.fontEditorBeatTrack
    val beatMarkerFont: PaintboxFont = main.fontEditorMarker
    val instantiatorNameFont: PaintboxFont = main.fontEditorInstantiatorName
    val instantiatorSummaryFont: PaintboxFont = main.fontEditorInstantiatorSummary
    val exitDialogFont: PaintboxFont = main.fontEditor
    val musicDialogFont: PaintboxFont = main.fontEditor
    val musicDialogFontBold: PaintboxFont = main.fontEditorBold
    val loadDialogFont: PaintboxFont = main.fontEditor
    val musicScoreFont: PaintboxFont = main.fontEditorMusicScore
    val rodinDialogFont: PaintboxFont = main.fontEditorRodin
    val rodinBorderedDialogFont: PaintboxFont = main.fontEditorRodinBordered

    // Markup
    val markup: Markup = Markup.createWithBoldItalic(main.fontEditor, main.fontEditorBold, main.fontEditorItalic,
            main.fontEditorBoldItalic, additionalMappings = mapOf(
            "rodin" to main.fontEditorRodin,
            "prmania_icons" to main.fontIcons,
    ))
    val markupBordered: Markup = Markup.createWithBoldItalic(main.fontEditorBordered, main.fontEditorBoldBordered,
            main.fontEditorItalicBordered, main.fontEditorBoldItalicBordered, additionalMappings = mapOf(
            "rodin" to main.fontEditorRodinBordered,
            "prmania_icons" to main.fontIcons,
    ))
    val markupInstantiatorSummary: Markup = Markup(mapOf(
            "rodin" to main.fontEditorRodin,
            "prmania_icons" to main.fontIcons,
    ), TextRun(instantiatorSummaryFont, ""), Markup.FontStyles.ALL_USING_DEFAULT_FONT)
    val markupInstantiatorDesc: Markup = Markup.createWithBoldItalic(main.fontCache["editor_instantiator_desc"],
            main.fontCache["editor_instantiator_desc_BOLD"], main.fontCache["editor_instantiator_desc_ITALIC"],
            main.fontCache["editor_instantiator_desc_BOLD_ITALIC"], additionalMappings = mapOf(
            "rodin" to main.fontEditorRodin,
            "prmania_icons" to main.fontIcons,
    ))
    val markupStatusBar: Markup = Markup.createWithBoldItalic(main.fontCache["editor_status"],
            main.fontCache["editor_status_BOLD"], main.fontCache["editor_status_ITALIC"],
            main.fontCache["editor_status_BOLD_ITALIC"], additionalMappings = mapOf(
            "rodin" to main.fontEditorRodin,
            "prmania_icons" to main.fontIcons,
    ))
    val markupHelp: Markup = markupInstantiatorDesc

    // Textures
    val blockFlatTexture: Texture by lazy { AssetRegistry.get<Texture>("ui_icon_block_flat") }
    val blockFlatPlatformRegion: TextureRegion by lazy { TextureRegion(blockFlatTexture, 16 * 0, 16 * 0, 16, 16) }
    val blockFlatPistonARegion: TextureRegion by lazy { TextureRegion(blockFlatTexture, 16 * 1, 16 * 0, 16, 16) }
    val blockFlatPistonDpadRegion: TextureRegion by lazy { TextureRegion(blockFlatTexture, 16 * 2, 16 * 0, 16, 16) }
    val blockFlatNoneRegion: TextureRegion by lazy { TextureRegion(blockFlatTexture, 16 * 3, 16 * 0, 16, 16) }
    val blockFlatCube1Region: TextureRegion by lazy { TextureRegion(blockFlatTexture, 16 * 0, 16 * 1, 16, 16) }
    val blockFlatCube2Region: TextureRegion by lazy { TextureRegion(blockFlatTexture, 16 * 1, 16 * 1, 16, 16) }
    val blockFlatCube1LineRegion: TextureRegion by lazy { TextureRegion(blockFlatTexture, 16 * 2, 16 * 1, 16, 16) }
    val blockFlatCube2LineRegion: TextureRegion by lazy { TextureRegion(blockFlatTexture, 16 * 3, 16 * 1, 16, 16) }
    val blockFlatPistonAOpenRegion: TextureRegion by lazy { TextureRegion(blockFlatTexture, 16 * 1, 16 * 2, 16, 16) }
    val blockFlatPistonDpadOpenRegion: TextureRegion by lazy { TextureRegion(blockFlatTexture, 16 * 2, 16 * 2, 16, 16) }
    val blockFlatNoChangeRegion: TextureRegion by lazy { TextureRegion(blockFlatTexture, 16 * 3, 16 * 2, 16, 16) }
    val blockFlatRetractRegion: TextureRegion by lazy { TextureRegion(blockFlatTexture, 16 * 0, 16 * 3, 16, 16) }
    val blockFlatSpotlightRegion: TextureRegion by lazy { TextureRegion(blockFlatTexture, 16 * 1, 16 * 3, 16, 16) }
    
}