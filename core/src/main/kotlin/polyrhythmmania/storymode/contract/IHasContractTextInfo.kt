package polyrhythmmania.storymode.contract

import paintbox.binding.ReadOnlyVar


interface IHasContractTextInfo {
    val name: ReadOnlyVar<String>
    val listingName: ReadOnlyVar<String>?
    val desc: ReadOnlyVar<String>
    val tagline: ReadOnlyVar<String>
    
    val requester: Requester
}
