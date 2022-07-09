package polyrhythmmania.storymode.contract

import com.badlogic.gdx.Gdx
import polyrhythmmania.storymode.gamemode.FirstContractGameMode
import polyrhythmmania.storymode.gamemode.StoryDunkGameModeFruitBasket
import polyrhythmmania.storymode.gamemode.StoryGameModeFromFile


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
        add(Contract("fruitbasket", listOf(Condition.PASS_THE_LEVEL), 100, Requester.DEBUG, JingleType.GBA) { main ->
            StoryDunkGameModeFruitBasket(main)
        })
    }
    
    operator fun get(id: String): Contract = contracts.getValue(id)
    
}