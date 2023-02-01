package polyrhythmmania.storymode.inbox

import paintbox.binding.ReadOnlyVar
import paintbox.binding.toConstVar
import polyrhythmmania.storymode.StoryL10N
import polyrhythmmania.storymode.contract.Contract
import polyrhythmmania.storymode.contract.IHasContractTextInfo
import polyrhythmmania.storymode.contract.Requester
import polyrhythmmania.storymode.inbox.IContractDoc.ContractSubtype


sealed class InboxItem(
        val id: String,

        val listingName: ReadOnlyVar<String>,
) {

    class Debug(id: String, listingName: String, val subtype: DebugSubtype, val description: String = "<no desc>")
        : InboxItem(id, ReadOnlyVar.const(listingName)) {

        enum class DebugSubtype {
            PROGRESSION_ADVANCER,
        }
    }

    class Memo(
            id: String,
            val hasToField: Boolean, hasSeparateListingName: Boolean, 
            hasDifferentShortFrom: Boolean = false,
            val subject: ReadOnlyVar<String> = StoryL10N.getVar("inboxItemDetails.memo.$id.subject"),
            val from: ReadOnlyVar<String> = StoryL10N.getVar("inboxItemDetails.memo.$id.from"),
            val to: ReadOnlyVar<String> = if (hasToField) StoryL10N.getVar("inboxItemDetails.memo.$id.to") else "".toConstVar(),
            val desc: ReadOnlyVar<String> = StoryL10N.getVar("inboxItemDetails.memo.$id.desc"),
            val shortFrom: ReadOnlyVar<String> = if (hasDifferentShortFrom) StoryL10N.getVar("inboxItemDetails.memo.$id.from.short") else from,
    ) : InboxItem(id, if (hasSeparateListingName) StoryL10N.getVar("inboxItemDetails.memo.$id.listing") else subject)
    
    class InfoMaterial(
            id: String, hasSeparateListingName: Boolean,
            val topic: ReadOnlyVar<String> = StoryL10N.getVar("inboxItemDetails.infoMaterial.$id.topic"),
            val audience: ReadOnlyVar<String> = StoryL10N.getVar("inboxItemDetails.infoMaterial.$id.audience"),
            val desc: ReadOnlyVar<String> = StoryL10N.getVar("inboxItemDetails.infoMaterial.$id.desc"),
    ) : InboxItem(id, if (hasSeparateListingName) StoryL10N.getVar("inboxItemDetails.infoMaterial.$id.listing") else topic)

    class ContractDoc(
            val contract: Contract, itemID: String = getDefaultContractDocID(contract),
            listingName: ReadOnlyVar<String>? = contract.listingName,
            override val subtype: ContractSubtype = ContractSubtype.NORMAL,
            override val hasLongCompanyName: Boolean = contract.requester.isNameLong,
            
            override val name: ReadOnlyVar<String> = contract.name
    ) : InboxItem(itemID, listingName ?: contract.name), IContractDoc, IHasContractTextInfo {
        
        companion object {
            fun getDefaultContractDocID(contractID: String): String = "contract_${contractID}"
            fun getDefaultContractDocID(contract: Contract): String = getDefaultContractDocID(contract.id)
        }

        val headingText: ReadOnlyVar<String> = StoryL10N.getVar(subtype.headingL10NKey)
        val ignoreNoMiss: Boolean get() = subtype == ContractSubtype.TRAINING
        val ignoreSkillStar: Boolean get() = subtype == ContractSubtype.TRAINING

        override val desc: ReadOnlyVar<String> get() = contract.desc
        override val tagline: ReadOnlyVar<String> get() = contract.tagline
        override val requester: Requester get() = contract.requester
        val contractListingName: ReadOnlyVar<String>? get() = contract.listingName
        
        override fun isCompletedWhenRead(): Boolean = false
    }
    
    class PlaceholderContract(
            itemID: String, placeholderNumber: Int,
            name: String = "PLACEHOLDER-${placeholderNumber.toString().padStart(3, '0')}",
            desc: String = "",
            tagline: String = "Make up a tagline later!",
            override val requester: Requester = Requester.DEBUG,
            listingName: ReadOnlyVar<String> = name.toConstVar(),
            override val subtype: ContractSubtype = ContractSubtype.NORMAL,
            override val hasLongCompanyName: Boolean = requester.isNameLong,
    ) : InboxItem(itemID, listingName), IContractDoc {

        override val name: ReadOnlyVar<String> = name.toConstVar()
        override val desc: ReadOnlyVar<String> = "Placeholder contract desc\n\n$desc".toConstVar()
        override val tagline: ReadOnlyVar<String> = tagline.toConstVar()
        val headingText: ReadOnlyVar<String> = StoryL10N.getVar(subtype.headingL10NKey)
        
        override fun isCompletedWhenRead(): Boolean = true // There is no contract to play
    }

    class EmploymentContract(id: String) : InboxItem(id, "".toConstVar()) {
        override fun isCompletedWhenRead(): Boolean = false
    }
    
    open var heading: Heading? = null
    
    open fun isCompletedWhenRead(): Boolean = true

}
