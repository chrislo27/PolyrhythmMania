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
        
        add(Contract("first", listOf(Condition.COMPLETE_TRAINING), Requester.HR, JingleType.GBA, null, 0) { main ->
            FirstContractGameMode(main)
        })
        
        add(Contract("fillbots", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.megamix("Fillbots"), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/fillbots.prmproj"))
        })
        add(Contract("fillbots2", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.ds("Fillbots", listOf(SongNameAndSource.megamix("Fillbots 2"))), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/fillbots2.prmproj"))
        })
        add(Contract("screwbots", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.fever("Screwbot Factory"), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/screwbots.prmproj"))
        })
        add(Contract("screwbots2", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.fever("Screwbot Factory 2"), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/screwbots2.prmproj"))
        })
        add(Contract("ringside", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.MODERN, SongInfo.fever("Ringside"), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/ringside.prmproj"))
        })
        add(Contract("shootemup", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.ARCADE, SongInfo.megamix("Shoot-'em-up"), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/shootemup.prmproj"))
        })
        add(Contract("fruit_basket", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.megamix("Fruit Basket"), 60) { main ->
            StoryDunkGameModeFruitBasket(main)
        })
        add(Contract("hole_in_one_2", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.fever("Hole in One 2"), 60) { main ->
            StoryDunkGameModeHoleInOne2(main)
        })
        add(Contract("bunny_hop", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.tengoku("Bunny Hop"), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/bunny_hop.prmproj"))
        })
        add(Contract("tram_and_pauline", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.tengoku("トランとポリン (Tram and Pauline)"), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/tram_and_pauline.prmproj"))
        })
        add(Contract("air_rally", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.megamix("Air Rally"), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/air_rally.prmproj"))
        })
        add(Contract("air_rally_one_life", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, null, 0) { main ->
            // FIXME this is a debug contract
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/air_rally.prmproj")).apply { 
                val lives = this.engine.modifiers.livesMode
                lives.maxLives.set(1)
                lives.enabled.set(true)
            }
        })
        add(Contract("air_rally_early_end", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, null, 0) { main ->
            // FIXME this is a debug contract
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/air_rally.prmproj")).apply { 
                this.container.addBlock(BlockEndState(this.engine).apply { 
                    this.beat = 16f
                })
            }
        })
        add(Contract("air_rally_early_end2", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, null, 50) { main ->
            // FIXME this is a debug contract
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/air_rally.prmproj")).apply { 
                this.container.addBlock(BlockEndState(this.engine).apply { 
                    this.beat = 16f
                })
            }
        })
        add(Contract("first_contact", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.ARCADE, SongInfo.megamix("First Contact"), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/first_contact.prmproj"))
        })
        add(Contract("spaceball", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.tengoku("Spaceball"), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/spaceball.prmproj"))
        })
        add(Contract("toss_boys", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.tengoku("トスボーイズ (Toss Boys)"), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/toss_boys.prmproj"))
        })
        add(Contract("second_contact", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.ARCADE, SongInfo.megamix("Second Contact"), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/second_contact.prmproj"))
        })
        add(Contract("rhythm_tweezers", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.megamix("Rhythm Tweezers"), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/rhythm_tweezers.prmproj"))
        })
        add(Contract("rhythm_tweezers_2", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.tengoku("Rhythm Tweezers", listOf(SongNameAndSource.megamix("Rhythm Tweezers 2"))), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/rhythm_tweezers_2.prmproj"))
        })
        add(Contract("crop_stomp", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.ds("Crop Stomp"), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/crop_stomp.prmproj"))
        })
        add(Contract("boosted_tweezers", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.tengoku("Rhythm Tweezers", listOf(SongNameAndSource.megamix("Rhythm Tweezers 2"))), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/boosted_tweezers.prmproj"))
        })
        add(Contract("super_samurai_slice", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.megamix("Super Samurai Slice"), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/super_samurai_slice.prmproj"))
        })
        add(Contract("bouncy_road", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.MODERN, SongInfo.tengoku("Bouncy Road"), 60) { main ->
            StoryAsmGameModeBouncyRoad(main)
        })
        add(Contract("bouncy_road_2", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.MODERN, SongInfo.tengoku("Bouncy Road"), 60) { main ->
            StoryAsmGameModeBouncyRoad2(main)
        })
        add(Contract("super_samurai_slice_2", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.megamix("Super Samurai Slice 2"), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/super_samurai_slice_2.prmproj"))
        })
        add(Contract("rhythm_rally", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.megamix("Rhythm Rally"), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/rhythm_rally.prmproj"))
        })
        add(Contract("hole_in_one", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.fever("Hole in One"), 60) { main ->
            StoryDunkGameModeHoleInOne(main)
        })
        add(Contract("rhythm_rally_2", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.ARCADE, SongInfo.ds("Rhythm Rally", listOf(SongNameAndSource.megamix("Rhythm Rally 2"))), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/rhythm_rally_2.prmproj"))
        })
        add(Contract("built_to_scale_ds", listOf(Condition.PASS_THE_LEVEL), Requester.DEBUG, JingleType.GBA, SongInfo.ds("Built to Scale"), 60) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/built_to_scale_ds.prmproj"))
        })
    }
    
    operator fun get(id: String): Contract = contracts.getValue(id)
    
}