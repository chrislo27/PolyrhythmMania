package polyrhythmmania.storymode.contract

import com.badlogic.gdx.Gdx
import polyrhythmmania.storymode.gamemode.*


object Contracts {
    
    val contracts: Map<String, Contract>
    
    init {
        this.contracts = mutableMapOf()
        
        fun add(contract: Contract) {
            this.contracts[contract.id] = contract
        }
        
        add(Contract("first", listOf(Condition.COMPLETE_TRAINING), 100, Requester.HR, JingleType.GBA) { main ->
            FirstContractGameMode(main)
        })
        
        add(Contract("fillbots", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.GBA) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/fillbots.prmproj"))
        })
        add(Contract("fillbots2", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.GBA) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/fillbots2.prmproj"))
        })
        add(Contract("screwbots", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.GBA) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/screwbots.prmproj"))
        })
        add(Contract("screwbots2", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.GBA) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/screwbots2.prmproj"))
        })
        add(Contract("ringside", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.MODERN) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/ringside.prmproj"))
        })
        add(Contract("shootemup", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.ARCADE) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/shootemup.prmproj"))
        })
        add(Contract("fruit_basket", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.GBA) { main ->
            StoryDunkGameModeFruitBasket(main)
        })
        add(Contract("hole_in_one_2", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.GBA) { main ->
            StoryDunkGameModeHoleInOne2(main)
        })
        add(Contract("bunny_hop", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.GBA) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/bunny_hop.prmproj"))
        })
        add(Contract("tram_and_pauline", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.GBA) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/tram_and_pauline.prmproj"))
        })
        add(Contract("air_rally", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.GBA) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/air_rally.prmproj"))
        })
        add(Contract("first_contact", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.ARCADE) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/first_contact.prmproj"))
        })
        add(Contract("spaceball", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.GBA) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/spaceball.prmproj"))
        })
        add(Contract("toss_boys", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.GBA) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/toss_boys.prmproj"))
        })
        add(Contract("second_contact", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.ARCADE) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/second_contact.prmproj"))
        })
        add(Contract("rhythm_tweezers", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.GBA) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/rhythm_tweezers.prmproj"))
        })
        add(Contract("rhythm_tweezers_2", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.GBA) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/rhythm_tweezers_2.prmproj"))
        })
        add(Contract("crop_stomp", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.GBA) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/crop_stomp.prmproj"))
        })
        add(Contract("boosted_tweezers", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.GBA) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/boosted_tweezers.prmproj"))
        })
        add(Contract("super_samurai_slice", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.GBA) { main ->
            StoryGameModeFromFile(main, Gdx.files.internal("story/levels/super_samurai_slice.prmproj"))
        })
        add(Contract("bouncy_road", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.MODERN) { main ->
            StoryAsmGameModeBouncyRoad(main)
        })
        add(Contract("bouncy_road_2", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.MODERN) { main ->
            StoryAsmGameModeBouncyRoad2(main)
        })
    }
    
    operator fun get(id: String): Contract = contracts.getValue(id)
    
}