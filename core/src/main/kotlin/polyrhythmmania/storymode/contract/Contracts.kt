package polyrhythmmania.storymode.contract


object Contracts {
    
    val contracts: Map<String, Contract>
    
    init {
        this.contracts = mutableMapOf()
        
        fun add(contract: Contract) {
            this.contracts[contract.id] = contract
        }
        
        add(Contract.createWithAutofill("first", Requester.HR, listOf(Condition.PASS_THE_LEVEL), 100))
    }
    
    operator fun get(id: String): Contract = contracts.getValue(id)
    
}