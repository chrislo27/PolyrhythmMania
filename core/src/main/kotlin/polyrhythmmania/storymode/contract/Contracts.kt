package polyrhythmmania.storymode.contract

import com.badlogic.gdx.Gdx
import polyrhythmmania.editor.block.BlockEndState
import polyrhythmmania.storymode.gamemode.*


object Contracts {
    
    val contracts: Map<String, Contract>
    
    init {
        this.contracts = mutableMapOf()
        
        fun add(contract: Contract) {
            this.contracts[contract.id] = contract
        }
        
        add(Contract("tutorial1", Requester.POLYRHYTHM_INC, JingleType.GBA, null, 0) { main ->
            Tutorial1GameMode(main)
        })
        
        add(Contract("fillbots", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.megamix("Fillbots"), listOf("J-D Thunder")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/fillbots.prmproj"))
        })
        add(Contract("fillbots2", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.ds("Fillbots", listOf(SongNameAndSource.megamix("Fillbots 2"))), listOf("J-D Thunder")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/fillbots2.prmproj"))
        })
        add(Contract("screwbots", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.fever("Screwbot Factory"), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/screwbots.prmproj"))
        })
        add(Contract("screwbots2", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.fever("Screwbot Factory 2"), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/screwbots2.prmproj"))
        })
        add(Contract("ringside", Requester.DEBUG, JingleType.MODERN, Attribution(SongInfo.fever("Ringside"), listOf("Dream Top")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/ringside.prmproj"))
        })
        add(Contract("shootemup", Requester.DEBUG, JingleType.ARCADE, Attribution(SongInfo.megamix("Shoot-'em-up"), listOf("Huebird")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/shootemup.prmproj"))
        })
        add(Contract("fruit_basket", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.megamix("Fruit Basket"), listOf("chrislo27")), 60) { main ->
            StoryDunkGameModeFruitBasket(main)
        })
        add(Contract("hole_in_one_2", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.fever("Hole in One 2"), listOf("Lvl100Feraligatr")), 60) { main ->
            StoryDunkGameModeHoleInOne2(main)
        })
        add(Contract("bunny_hop", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.tengoku("Bunny Hop"), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/bunny_hop.prmproj"))
        })
        add(Contract("tram_and_pauline", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.tengoku("トランとポリン (Tram and Pauline)"), listOf("J-D Thunder")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/tram_and_pauline.prmproj"))
        })
        add(Contract("air_rally", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.megamix("Air Rally"), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/air_rally.prmproj"))
        })
        add(Contract("air_rally_one_life", Requester.DEBUG, JingleType.GBA, null, 0) { main ->
            // FIXME this is a debug contract
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/air_rally.prmproj")).apply { 
                val lives = this.engine.modifiers.livesMode
                lives.maxLives.set(1)
                lives.enabled.set(true)
            }
        })
        add(Contract("air_rally_early_end", Requester.DEBUG, JingleType.GBA, null, 0) { main ->
            // FIXME this is a debug contract
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/air_rally.prmproj")).apply { 
                this.container.addBlock(BlockEndState(this.engine).apply { 
                    this.beat = 16f
                })
            }
        })
        add(Contract("air_rally_early_end2", Requester.DEBUG, JingleType.GBA, null, 50) { main ->
            // FIXME this is a debug contract
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/air_rally.prmproj")).apply { 
                this.container.addBlock(BlockEndState(this.engine).apply { 
                    this.beat = 16f
                })
            }
        })
        add(Contract("first_contact", Requester.DEBUG, JingleType.ARCADE, Attribution(SongInfo.megamix("First Contact"), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/first_contact.prmproj"))
        })
        add(Contract("spaceball", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.tengoku("Spaceball"), listOf("spoopster")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/spaceball.prmproj"))
        })
        add(Contract("toss_boys", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.tengoku("トスボーイズ (Toss Boys)"), listOf("Dream Top")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/toss_boys.prmproj"))
        })
        add(Contract("second_contact", Requester.DEBUG, JingleType.ARCADE, Attribution(SongInfo.megamix("Second Contact"), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/second_contact.prmproj"))
        })
        add(Contract("rhythm_tweezers", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.megamix("Rhythm Tweezers"), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/rhythm_tweezers.prmproj"))
        })
        add(Contract("rhythm_tweezers_2", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.tengoku("Rhythm Tweezers", listOf(SongNameAndSource.megamix("Rhythm Tweezers 2"))), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/rhythm_tweezers_2.prmproj"))
        })
        add(Contract("crop_stomp", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.ds("Crop Stomp"), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/crop_stomp.prmproj"))
        })
        add(Contract("boosted_tweezers", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.tengoku("Rhythm Tweezers", listOf(SongNameAndSource.megamix("Rhythm Tweezers 2"))), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/boosted_tweezers.prmproj"))
        })
        add(Contract("super_samurai_slice", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.megamix("Super Samurai Slice"), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/super_samurai_slice.prmproj"))
        })
        add(Contract("bouncy_road", Requester.DEBUG, JingleType.MODERN, Attribution(SongInfo.tengoku("Bouncy Road"), listOf("J-D Thunder")), 60) { main ->
            StoryAsmGameModeBouncyRoad(main)
        })
        add(Contract("bouncy_road_2", Requester.DEBUG, JingleType.MODERN, Attribution(SongInfo.tengoku("Bouncy Road"), listOf("chrislo27")), 60) { main ->
            StoryAsmGameModeBouncyRoad2(main)
        })
        add(Contract("super_samurai_slice_2", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.megamix("Super Samurai Slice 2"), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/super_samurai_slice_2.prmproj"))
        })
        add(Contract("rhythm_rally", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.megamix("Rhythm Rally"), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/rhythm_rally.prmproj"))
        })
        add(Contract("hole_in_one", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.fever("Hole in One"), listOf("J-D Thunder")), 60) { main ->
            StoryDunkGameModeHoleInOne(main)
        })
        add(Contract("rhythm_rally_2", Requester.DEBUG, JingleType.ARCADE, Attribution(SongInfo.ds("Rhythm Rally", listOf(SongNameAndSource.megamix("Rhythm Rally 2"))), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/rhythm_rally_2.prmproj"))
        })
        add(Contract("built_to_scale_ds", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.ds("Built to Scale"), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/built_to_scale_ds.prmproj"))
        })
        add(Contract("fruit_basket_2", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.megamix("Fruit Basket 2"), listOf("J-D Thunder")), 60) { main ->
            StoryDunkGameModeFruitBasket2(main)
        })
        add(Contract("air_rally_2", Requester.DEBUG, JingleType.MODERN, Attribution(SongInfo.fever("Air Rally", listOf(SongNameAndSource.megamix("Air Rally 2"))), listOf("GENERIC")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/air_rally_2.prmproj"))
        })
        add(Contract("monkey_watch", Requester.DEBUG, JingleType.MODERN, Attribution(SongInfo.fever("Monkey Watch"), listOf("Kievit")), 60) { main ->
            StoryAsmGameModeMonkeyWatch(main)
        })
        add(Contract("flock_step", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.fever("Flock Step"), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/flock_step.prmproj"))
        })
        add(Contract("tap_trial", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.tengoku("Tap Trial"), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/tap_trial.prmproj"))
        })
        add(Contract("tap_trial_2", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.tengoku("Tap Trial 2"), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/tap_trial_2.prmproj"))
        })
        add(Contract("fork_lifter", Requester.DEBUG, JingleType.GBA, Attribution(SongInfo.tengoku("Tap Trial 2"), listOf("Kievit")), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/fork_lifter.prmproj"))
        })
    }
    
    operator fun get(id: String): Contract = contracts.getValue(id)
    
}