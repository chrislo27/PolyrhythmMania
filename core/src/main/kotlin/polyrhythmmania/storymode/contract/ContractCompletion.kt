package polyrhythmmania.storymode.contract


sealed class ContractCompletion {
    
    object NotCompleted : ContractCompletion()
    
    object Skipped : ContractCompletion()
    
    class Completed : ContractCompletion() // TODO add fields like high score
    
}
