package polyrhythmmania.storymode.inbox

import polyrhythmmania.storymode.contract.Contracts
import polyrhythmmania.storymode.inbox.InboxItem.ContractDoc.ContractSubtype


object InboxDB : InboxItems() {

    init {
        val toAdd = mutableListOf<InboxItem>()

        toAdd += InboxItem.Memo("intern_memo1", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.ContractDoc(Contracts["tutorial1"], subtype = ContractSubtype.TRAINING)
        toAdd += InboxItem.Memo("intern_memo2", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.InfoMaterial("info_on_contracts", hasSeparateListingName = true)
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_intern1", 1, desc = "Fillbots 1") // TODO first contract
        toAdd += InboxItem.Memo("intern_memo3", hasToField = false, hasSeparateListingName = false)
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_intern2", 2, desc = "Shoot-'em-up 1") // TODO second contract
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_intern3", 3, desc = "Rhythm Tweezers 1") // TODO third contract
        toAdd += InboxItem.Memo("intern_done", hasToField = false, hasSeparateListingName = false)
        // TODO possibly another "offer letter"? Separate inbox item type?

        // Post-internship
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_04", 4, desc = "Crop Stomp") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_05", 5, desc = "Air Rally 1") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_06", 6, desc = "First Contact") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_07", 7, desc = "Fruit Basket 1") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_08", 8, desc = "Bunny Hop") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_09", 9, desc = "Toss Boys") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_10", 10, desc = "Screwbots 1") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_11", 12, desc = "Ringside") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_12", 13, desc = "Spaceball") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_13", 13, desc = "Rhythm Rally 1") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_14", 14, desc = "Fillbots 2") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_15", 15, desc = "Rhythm Tweezers 2") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_16", 16, desc = "Bouncy Road 1") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_17", 17, desc = "Super Samurai Slice 1") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_18", 18, desc = "Hole in One 1") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_19", 19, desc = "Fork Lifter") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_20", 20, desc = "Tap Trial 1") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_21", 21, desc = "BtS DS 1") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_22", 22, desc = "Rhythm Rally 2") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_23", 23, desc = "Flock Step") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_24", 24, desc = "Fruit Basket 2") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_25", 25, desc = "Air Rally 2") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_26", 26, desc = "Tram and Pauline") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_27", 27, desc = "Monkey Watch") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_28", 28, desc = "Second Contact") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_29", 29, desc = "Hole in One 2") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_30", 30, desc = "Super Samurai Slice 2") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_31", 31, desc = "Bouncy Road 2") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_32", 32, desc = "Screwbots 2") // TODO
        toAdd += InboxItem.PlaceholderContract("placeholdercontr_33", 33, desc = "Tap Trial 2") // TODO
        
        this.setItems(toAdd)
    }

}
