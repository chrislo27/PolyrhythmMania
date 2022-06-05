package polyrhythmmania.storymode.contract

import polyrhythmmania.storymode.gamemode.FirstContractGameMode


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
    }
    
    operator fun get(id: String): Contract = contracts.getValue(id)
    
}