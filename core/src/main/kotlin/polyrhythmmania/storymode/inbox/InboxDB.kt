package polyrhythmmania.storymode.inbox

import polyrhythmmania.storymode.contract.Contracts
import polyrhythmmania.storymode.inbox.IContractDoc.ContractSubtype


open class InboxDB : InboxItems() {

    init {
        val toAdd = mutableListOf<InboxItem>()

        toAdd += InboxItem.Memo("intern_memo1", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.ContractDoc(Contracts["tutorial1"], subtype = ContractSubtype.TRAINING)
        toAdd += InboxItem.Memo("intern_memo2", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.InfoMaterial("info_on_contracts", hasSeparateListingName = true)
        toAdd += InboxItem.ContractDoc(Contracts["fillbots"])
        toAdd += InboxItem.Memo("intern_memo3", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.ContractDoc(Contracts["shootemup"])
        toAdd += InboxItem.ContractDoc(Contracts["rhythm_tweezers"])
        toAdd += InboxItem.Memo("intern_final_contract", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.ContractDoc(Contracts["crop_stomp"])
        toAdd += InboxItem.Memo("intern_done", hasToField = false, hasSeparateListingName = false)
        // TODO possibly another "offer letter"? Separate inbox item type?

        // Post-internship
        toAdd += InboxItem.Memo("welcome_back", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.ContractDoc(Contracts["air_rally"])
        toAdd += InboxItem.ContractDoc(Contracts["first_contact"])
        toAdd += InboxItem.Memo("weekly_sports", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.ContractDoc(Contracts["fruit_basket"])
        toAdd += InboxItem.ContractDoc(Contracts["bunny_hop"])
        toAdd += InboxItem.Memo("intro_to_lives", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.ContractDoc(Contracts["toss_boys"]) // TODO add lives setting
        toAdd += InboxItem.Memo("big_contract_coming_up", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.ContractDoc(Contracts["screwbots"])
        toAdd += InboxItem.ContractDoc(Contracts["ringside"])
        toAdd += InboxItem.ContractDoc(Contracts["spaceball"])
        toAdd += InboxItem.ContractDoc(Contracts["rhythm_rally"])
        toAdd += InboxItem.Memo("another_big_contract_soon", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.ContractDoc(Contracts["fillbots2"])
        toAdd += InboxItem.ContractDoc(Contracts["rhythm_tweezers_2"])
        toAdd += InboxItem.Memo("new_work_type_assemble", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.ContractDoc(Contracts["bouncy_road"])
        toAdd += InboxItem.ContractDoc(Contracts["super_samurai_slice"])
        toAdd += InboxItem.Memo("weekly_sports_2", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.ContractDoc(Contracts["hole_in_one"])
        toAdd += InboxItem.ContractDoc(Contracts["fork_lifter"])
        toAdd += InboxItem.ContractDoc(Contracts["tap_trial"])
        toAdd += InboxItem.ContractDoc(Contracts["built_to_scale_ds"])
        toAdd += InboxItem.Memo("intro_to_defective_rods", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.ContractDoc(Contracts["rhythm_rally_2"])
        toAdd += InboxItem.ContractDoc(Contracts["flock_step"])
        toAdd += InboxItem.Memo("weekly_sports_3", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.ContractDoc(Contracts["fruit_basket_2"])
        toAdd += InboxItem.ContractDoc(Contracts["air_rally_2"])
        toAdd += InboxItem.ContractDoc(Contracts["tram_and_pauline"])
        toAdd += InboxItem.ContractDoc(Contracts["monkey_watch"])
        toAdd += InboxItem.ContractDoc(Contracts["second_contact"])
        toAdd += InboxItem.Memo("weekly_sports_4", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.ContractDoc(Contracts["hole_in_one_2"])
        toAdd += InboxItem.ContractDoc(Contracts["super_samurai_slice_2"])
        toAdd += InboxItem.ContractDoc(Contracts["bouncy_road_2"])
        toAdd += InboxItem.Memo("huge_contract_soon", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.ContractDoc(Contracts["screwbots2"])
        toAdd += InboxItem.ContractDoc(Contracts["tap_trial_2"])
        
        this.setItems(toAdd)
    }

}
