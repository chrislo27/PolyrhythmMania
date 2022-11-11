package polyrhythmmania.storymode.inbox

import paintbox.binding.ReadOnlyVar
import paintbox.binding.asReadOnlyVar
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
            id: String, val hasToField: Boolean, hasSeparateListingName: Boolean,
            val subject: ReadOnlyVar<String> = StoryL10N.getVar("inboxItemDetails.memo.$id.subject"),
            val from: ReadOnlyVar<String> = StoryL10N.getVar("inboxItemDetails.memo.$id.from"),
            val to: ReadOnlyVar<String> = if (hasToField) StoryL10N.getVar("inboxItemDetails.memo.$id.to") else "".asReadOnlyVar(),
            val desc: ReadOnlyVar<String> = StoryL10N.getVar("inboxItemDetails.memo.$id.desc"),
    ) : InboxItem(id, if (hasSeparateListingName) StoryL10N.getVar("inboxItemDetails.memo.$id.listing") else subject)
    
    class InfoMaterial(
            id: String, hasSeparateListingName: Boolean,
            val topic: ReadOnlyVar<String> = StoryL10N.getVar("inboxItemDetails.infoMaterial.$id.topic"),
            val audience: ReadOnlyVar<String> = StoryL10N.getVar("inboxItemDetails.infoMaterial.$id.audience"),
            val desc: ReadOnlyVar<String> = StoryL10N.getVar("inboxItemDetails.infoMaterial.$id.desc"),
    ) : InboxItem(id, if (hasSeparateListingName) StoryL10N.getVar("inboxItemDetails.infoMaterial.$id.listing") else topic)

    class ContractDoc(
            val contract: Contract, itemID: String = getDefaultContractDoc(contract),
            listingName: ReadOnlyVar<String> = contract.name,
            override val subtype: ContractSubtype = ContractSubtype.NORMAL,
            override val hasLongCompanyName: Boolean = false,
    ) : InboxItem(itemID, listingName), IContractDoc, IHasContractTextInfo by contract {
        
        companion object {
            fun getDefaultContractDoc(contract: Contract): String = "contract_${contract.id}"
        }

        val headingText: ReadOnlyVar<String> = StoryL10N.getVar(subtype.headingL10NKey)
    }
    
    class PlaceholderContract(
            itemID: String, placeholderNumber: Int,
            name: String = "PLACEHOLDER-${placeholderNumber.toString().padStart(3, '0')}",
            desc: String = "",
            tagline: String = "Make up a tagline later!",
            override val requester: Requester = Requester.DEBUG,
            listingName: ReadOnlyVar<String> = name.asReadOnlyVar(),
            override val subtype: ContractSubtype = ContractSubtype.NORMAL,
            override val hasLongCompanyName: Boolean = false,
    ) : InboxItem(itemID, listingName), IContractDoc {

        override val name: ReadOnlyVar<String> = name.asReadOnlyVar()
        override val desc: ReadOnlyVar<String> = "Placeholder contract desc\n\n$desc".asReadOnlyVar()
        override val tagline: ReadOnlyVar<String> = tagline.asReadOnlyVar()
        val headingText: ReadOnlyVar<String> = StoryL10N.getVar(subtype.headingL10NKey)
    }

}
