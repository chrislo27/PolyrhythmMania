package polyrhythmmania.storymode.test.gamemode

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import paintbox.util.gdxutils.isShiftDown
import polyrhythmmania.PRManiaGame
import polyrhythmmania.storymode.gamemode.boss.BossModifierModule


class TestStoryNoOpBossUIGameMode(main: PRManiaGame) : TestStoryGameMode(main) {

    private val modifierModule: BossModifierModule = BossModifierModule(engine.modifiers)

    init {
        engine.modifiers.addModifierModule(modifierModule)
    }

    override fun initialize() {
        modifierModule.uiOpacity.set(1f)
    }

    override fun renderUpdate() {
        super.renderUpdate()

        modifierModule.uiOpacity.set(1f)

        val bossHealth = modifierModule.bossHealth
        val playerHealth = modifierModule.playerHealth

        if (Gdx.input.isKeyJustPressed(Keys.B)) { // Boss takes damage
            val currentBossHP = bossHealth.currentHP.get()
            if (currentBossHP > 0) {
                bossHealth.currentHP.set((currentBossHP - (if (Gdx.input.isShiftDown()) 1 else 2)).coerceAtLeast(0))
                bossHealth.triggerHurtFlash()
            }
        }

        if (Gdx.input.isKeyJustPressed(Keys.P)) { // Player takes damage
            val playerHP = playerHealth.currentHP
            if (playerHP.get() > 0) {
                playerHP.decrementAndGet()
                playerHealth.triggerHurtFlash()
            }
        }

        if (Gdx.input.isKeyJustPressed(Keys.R)) { // Reset damages
            bossHealth.resetState()
            playerHealth.resetState()
        }
    }
}