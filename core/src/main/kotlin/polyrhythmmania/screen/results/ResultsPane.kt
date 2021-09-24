package polyrhythmmania.screen.results

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Align
import paintbox.binding.FloatVar
import paintbox.binding.ReadOnlyVar
import paintbox.binding.Var
import paintbox.font.Markup
import paintbox.font.PaintboxFont
import paintbox.font.TextAlign
import paintbox.font.TextRun
import paintbox.packing.PackedSheet
import paintbox.registry.AssetRegistry
import paintbox.ui.*
import paintbox.ui.area.Insets
import paintbox.ui.control.TextLabel
import paintbox.ui.layout.VBox
import polyrhythmmania.Localization
import polyrhythmmania.PRManiaGame
import polyrhythmmania.engine.input.Challenges
import polyrhythmmania.engine.input.Ranking
import polyrhythmmania.engine.input.Score
import polyrhythmmania.ui.TextboxPane


class ResultsPane(main: PRManiaGame, initialScore: Score) : Pane() {

    val score: Var<Score> = Var(initialScore)
    
    val titleLabel: TextLabel
    val linesLabel: TextLabel
    val scoreLabel: TextLabel
    val scoreValueFloat: FloatVar = FloatVar { score.use().scoreInt.toFloat() }
    val scoreValue: ReadOnlyVar<Int> = Var.bind { scoreValueFloat.useF().toInt() }
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
            this.markup.set(Markup(mapOf(), TextRun(resultsFont, ""), Markup.FontStyles.ALL_DEFAULT, lenientMode = true))
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
            this += ImageWindowNode(TextureRegion(AssetRegistry.get<Texture>("results_score_bar"))).apply {
                Anchor.TopLeft.configure(this)
                this.windowU2.bind { (scoreValueFloat.useF() / 100f).coerceIn(0f, 1f) }
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
            this.margin.set(Insets(16f, 0f, 0f, 0f))
        }
        bonusStatsPane += TextLabel(binding = {
            val sc = score.use()
            val noMiss = sc.noMiss
            val skillStar = sc.skillStar
            val newHighScore = sc.newHighScore
            val list = mutableListOf<String>()
            list += Localization.getValue("play.results.nInputs", sc.inputsHit, sc.nInputs)
            if (noMiss) list += Localization.getValue(if (sc.challenges.goingForPerfect) "play.results.perfect" else "play.results.noMiss")
            if (skillStar) list += Localization.getValue("play.results.skillStar")
            if (newHighScore) list += Localization.getValue("play.results.newHighScore")
            list.joinToString(separator = " ") { it }
        }, resultsFont).apply {
            this.bounds.x.set(32f)
            this.bindWidthToParent(adjust = -32f)
            this.doXCompression.set(false)
            this.renderAlign.set(Align.topLeft)
            this.textColor.set(Color.WHITE)
            this.setScaleXY(0.75f)
        }
        bonusStatsPane += ImageIcon().apply { 
            val success = AssetRegistry.get<PackedSheet>("tileset_ui")["perfect"]
            val failed = AssetRegistry.get<PackedSheet>("tileset_ui")["perfect_failed"]
            this.textureRegion.bind { 
                val s = score.use()
                if (!s.noMiss) failed else success
            }
            this.visible.bind { score.use().challenges.goingForPerfect }
            this.bounds.width.set(48f)
            this.bindHeightToSelfWidth()
            this.bounds.y.set(-12f)
            this.bounds.x.bind { -(bounds.width.useF() - 32f + 8f) }
            this.padding.set(Insets(0f, 0f, 0f, 0f))
        }
        bonusStatsPane += TextLabel(binding = {
            val sc = score.use()
            val challenges = sc.challenges
            val isTempoUp = challenges.tempoUp > 100
            if (challenges.tempoUp == 100) {
                ""
            } else {
                Localization.getValue("play.results.${if (isTempoUp) "tempoUp" else "tempoDown"}", challenges.tempoUp)
            }
        }, resultsFont).apply {
            this.bounds.width.set(200f)
            this.bounds.height.set(48f)
            this.bounds.y.set(-12f)
            this.bounds.x.bind { -(bounds.width.useF() - 32f + 48f + 8f) }
            this.padding.set(Insets(0f, 0f, 0f, 2f))
            this.renderAlign.set(Align.right)
            this.setScaleXY(0.5f)
            this.textColor.bind {
                val sc = score.use()
                val challenges = sc.challenges
                val tempoChange = challenges.tempoUp
                when {
                    tempoChange > 100 -> Challenges.TEMPO_UP_COLOR
                    tempoChange < 100 -> Challenges.TEMPO_DOWN_COLOR
                    else -> Color.WHITE
                }
            }
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