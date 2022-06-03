package polyrhythmmania.storymode.contract


object Contracts {
    
    val contracts: Map<String, Contract>
    
    init {
        this.contracts = mutableMapOf()
        
        fun add(contract: Contract) {
            this.contracts[contract.id] = contract
        }
        
        add(Contract.createWithAutofill("first", listOf(Condition.PASS_THE_LEVEL), 100, Requester.HR, JingleType.GBA))
    }
    
    operator fun get(id: String): Contract = contracts.getValue(id)
    
}