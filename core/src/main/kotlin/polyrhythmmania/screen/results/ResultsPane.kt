package polyrhythmmania.screen.results

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.binding.Var
import paintbox.font.Markup
import paintbox.font.PaintboxFont
import paintbox.font.TextAlign
import paintbox.font.TextRun
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.Anchor
import paintbox.ui.ImageNode
import paintbox.ui.Pane
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.input.Ranking
import polyrhythmmania.engine.input.Score
import polyrhythmmania.ui.TextboxPane


class ResultsPane(main: PRManiaGame, initialScore: Score) : Pane() {

    val score: Var<Score> = Var(initialScore)
    
    val titleLabel: TextLabel
    val linesLabel: TextLabel
    val scoreLabel: TextLabel
    val scoreValue: Var<Int> = Var.bind { score.use().scoreInt }
    val rankingPane: Pane
    val bonusStatsPane: Pane
    private val rankingImage: ImageNode

    init {
        val resultsFont: PaintboxFont = main.fontResultsMain
        val scoreTextFont: PaintboxFont = main.fontResultsScore

        val pane: Pane = this
        pane.margin.set(Insets(64f, 18f, 128f, 128f))

        titleLabel = TextLabel(binding = { score.use().title }, font = resultsFont).apply {
            this.padding.set(Insets(10f))
            this.textColor.set(Color.BLACK)
            this.renderAlign.set(Align.center)
        }
        pane += TextboxPane().apply {
            Anchor.TopCentre.configure(this)
            this.padding.set(Insets(16f))
            this.bounds.width.set(600f)
            this.bounds.height.set(80f)

            this += titleLabel
        }

        val vbox = VBox().apply {
            Anchor.TopCentre.configure(this, offsetY = 150f)
            this.bounds.width.set(800f)
            this.bounds.height.set(370f)
        }
        pane += vbox
        linesLabel = TextLabel(binding = {
            val s = score.use()
            s.line1 + (if (s.line2.isNotEmpty()) "\n\n${s.line2}" else "")
        }, font = resultsFont).apply {
            this.markup.set(Markup(mapOf(), TextRun(resultsFont, ""), Markup.FontStyles.ALL_DEFAULT))
            this.textColor.set(Color.WHITE)
            this.renderAlign.set(Align.top)
            this.textAlign.set(TextAlign.LEFT)
            this.bounds.height.set(280f)
            this.doLineWrapping.set(true)
        }
        vbox += linesLabel
        
        val scorePane = Pane().apply {
            this.bounds.height.set(90f)
            this.visible.bind { scoreValue.use() >= 0 }
        }
        vbox += scorePane
        
        scorePane += Pane().apply { 
            this.doClipping.set(true)
            this.bindWidthToParent(multiplierBinding = {
                scoreValue.use() / 100f                                       
            }, adjustBinding = {0f})
            this += ImageNode(TextureRegion(AssetRegistry.get<Texture>("results_score_bar"))).apply {
                Anchor.TopLeft.configure(this)
                this.bounds.width.bind { parent.use()?.parent?.use()?.bounds?.width?.useF() ?: 0f }
            }
        }
        scorePane += ImageNode(TextureRegion(AssetRegistry.get<Texture>("results_score_bar_border")))
        
        scoreLabel = TextLabel(binding = {
            val s = scoreValue.use()
            if (s < 0) "" else "$s"
        }, font = scoreTextFont).apply {
            this.textColor.sideEffecting(Color()) { existing ->
                val sc = scoreValue.use()
                val ranking = Ranking.getRanking(sc)
                existing.set(ranking.color)
                existing
            }
            this.renderAlign.set(Align.center)
            this.padding.set(Insets(0f, 6f, 100f, 100f))
        }
        scorePane += scoreLabel

        bonusStatsPane = Pane().apply {
            this.bounds.width.set(500f)
            this.bounds.height.set(64f)
            this.margin.set(Insets(16f, 0f, 32f, 0f))
        }
        bonusStatsPane += TextLabel(binding = {
            val sc = score.use()
            val noMiss = sc.noMiss
            val skillStar = sc.skillStar
            val list = mutableListOf<String>()
            if (noMiss) list += "play.results.noMiss"
            if (skillStar) list += "play.results.skillStar"
            list.joinToString(separator = " ") { Localization.getValue(it) }
        }, resultsFont).apply {
            this.doXCompression.set(false)
            this.renderAlign.set(Align.topLeft)
            this.textColor.set(Color.WHITE)
            this.setScaleXY(0.75f)
        }
        vbox += bonusStatsPane
        
        rankingPane = Pane().apply {
            Anchor.BottomRight.configure(this, offsetX = 64f)
            this.bounds.width.set(350f)
            this.bounds.height.set(96f)
        }
        pane += rankingPane
        
        rankingImage = ImageNode(binding = {
            AssetRegistry.get<PackedSheet>("results_ranking")[score.use().ranking.rankingIconID]
        })
        rankingPane += rankingImage
        rankingPane += TextLabel(binding = {
            val sc = score.use()
            val stillJust = sc.butStillJustOk
            if (stillJust) Localization.getValue("play.results.ranking.ok.butStillJust") else ""
        }, resultsFont).apply {
            this.doXCompression.set(false)
            this.renderAlign.set(Align.topRight)
            this.bounds.width.set(90f)
            this.textColor.set(Color.WHITE)
            this.margin.set(Insets(10f, 0f, 0f, 0f))
            this.setScaleXY(0.75f)
        }
    }
}