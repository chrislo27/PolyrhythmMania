package polyrhythmmania.storymode.inbox

import polyrhythmmania.storymode.contract.Contracts
import polyrhythmmania.storymode.contract.SongInfo
import polyrhythmmania.storymode.inbox.progression.Progression
import polyrhythmmania.storymode.inbox.progression.UnlockStage
import polyrhythmmania.storymode.inbox.progression.UnlockStageChecker


open class InboxDB : InboxItems() {
    
    companion object {
        private const val ITEMID_INTERN_EMPLOYMENT_CONTRACT: String = "intern_employment_contract"
        private const val ITEMID_INTERMEDIATE_TECHNICIAN_MEMO: String = "memo_mgmt_performance"
        private const val ITEMID_TIM_BOSS_WARNING: String = "memo_tim_boss_warning"
        private val ITEMID_BOSS: String = InboxItem.ContractDoc.getDefaultContractDocID(Contracts.ID_BOSS)
        private const val ITEMID_POSTBOSS_LETTER_1: String = "postboss_letter_1"
        private const val ITEMID_POSTBOSS_LETTER_2: String = "postboss_letter_2"
        private const val ITEMID_POSTBOSS_LETTER_3: String = "postboss_letter_3"
        private const val ITEMID_POSTGAME_EMPLOYMENT_CONTRACT: String = "postgame_employment_contract"
        private const val ITEMID_WELCOME_BACK_POSTGAME: String = "welcome_back_postgame"
        private const val ITEMID_TIM_POSTGAME_COMPLETED: String = "memo_tim_postgame_completed"
        
        const val ITEM_TO_TRIGGER_MAIN_MUSIC_MIX: String = ITEMID_INTERN_EMPLOYMENT_CONTRACT
        const val ITEM_TO_TRIGGER_PREBOSS_QUIET_MUSIC_MIX: String = ITEMID_TIM_BOSS_WARNING
        val ITEM_TO_TRIGGER_POSTBOSS_SILENT_MUSIC_MIX: String = ITEMID_BOSS
        const val ITEM_TO_TRIGGER_POSTBOSS_MINIMAL_MUSIC_MIX: String = ITEMID_POSTBOSS_LETTER_1
        const val ITEM_TO_TRIGGER_POSTBOSS_QUIET_MUSIC_MIX: String = ITEMID_POSTBOSS_LETTER_2
        const val ITEM_TO_TRIGGER_POSTBOSS_MAIN_MUSIC_MIX: String = ITEMID_POSTBOSS_LETTER_3
        const val ITEM_TO_TRIGGER_POSTGAME_MUSIC_MIX: String = ITEMID_POSTGAME_EMPLOYMENT_CONTRACT
        
        const val ITEM_TO_TRIGGER_ACHIEVEMENT_JUNIOR_TECHNICIAN: String = ITEMID_INTERN_EMPLOYMENT_CONTRACT
        const val ITEM_TO_TRIGGER_ACHIEVEMENT_INTERMEDIATE_TECHNICIAN: String = ITEMID_INTERMEDIATE_TECHNICIAN_MEMO
        val ITEM_TO_TRIGGER_ACHIEVEMENT_DEFEATED_BOSS: String = ITEMID_POSTBOSS_LETTER_1
        const val ITEM_TO_TRIGGER_ACHIEVEMENT_COMPLETED_POSTGAME: String = ITEMID_TIM_POSTGAME_COMPLETED
        
        const val ITEM_WITH_END_OF_THE_ASSEMBLY_LINE_MUSIC: String = ITEMID_TIM_POSTGAME_COMPLETED
    }
    
    enum class Category {
        INTERNSHIP, MAIN, POSTGAME,
    }
    
    val progression: Progression
    val itemCategories: Map<InboxItem, Category>
    val itemsByCategory: Map<Category, List<InboxItem>>

    init {        
        val instructions = mutableListOf<Instruction>()

        instructions += SingleStageItem(Item(Category.INTERNSHIP, InboxItem.Memo("intern_memo1", hasToField = false, hasSeparateListingName = false)))
        instructions += SingleStageItem(Item(Category.INTERNSHIP, InboxItem.ContractDoc(Contracts[Contracts.ID_TUTORIAL1], subtype = IContractDoc.ContractSubtype.TRAINING)))
        instructions += SingleStageItem(Item(Category.INTERNSHIP, InboxItem.Memo("intern_memo2", hasToField = false, hasSeparateListingName = false)))
        instructions += SingleStageItem(Item(Category.INTERNSHIP, InboxItem.InfoMaterial("info_on_contracts", hasSeparateListingName = true)))
        instructions += SingleStageItem(Item(Category.INTERNSHIP, InboxItem.ContractDoc(Contracts["fillbots"])))
        instructions += SingleStageItem(Item(Category.INTERNSHIP, InboxItem.Memo("intern_memo3", hasToField = false, hasSeparateListingName = false)))
        instructions += SingleStageItem(Item(Category.INTERNSHIP, InboxItem.ContractDoc(Contracts["shootemup"])))
        instructions += SingleStageItem(Item(Category.INTERNSHIP, InboxItem.ContractDoc(Contracts["rhythm_tweezers"])))
        instructions += SingleStageItem(Item(Category.INTERNSHIP, InboxItem.Memo("intern_final_contract", hasToField = false, hasSeparateListingName = false)))
        instructions += SingleStageItem(Item(Category.INTERNSHIP, InboxItem.ContractDoc(Contracts["crop_stomp"])))
        instructions += SingleStageItem(Item(Category.INTERNSHIP, InboxItem.Memo("intern_done", hasToField = false, hasSeparateListingName = false)))
        instructions += SingleStageItem(Item(Category.INTERNSHIP, InboxItem.EmploymentContract(ITEMID_INTERN_EMPLOYMENT_CONTRACT, useSecondarySignedTexture = false)))

        // Post-internship
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.Memo("welcome_back", hasToField = false, hasSeparateListingName = false)))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["air_rally"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["first_contact"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["fruit_basket"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["bunny_hop"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["toss_boys"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.Memo("memo_tim_buildroid_contracts", hasToField = false, hasSeparateListingName = false, hasDifferentShortFrom = true)))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["screwbots"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["ringside"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["spaceball"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["rhythm_rally"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["bouncy_road"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["fillbots2"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.Memo(ITEMID_INTERMEDIATE_TECHNICIAN_MEMO, hasToField = false, hasSeparateListingName = false)))
        val rt2Instruction = SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["rhythm_tweezers_2"])))
        instructions += rt2Instruction
        instructions += NewItemNoStage(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["boosted_tweezers"])))
        instructions += NewUnlockStage(UnlockStage("boosted_tweezers", { progression, inboxState ->
            val rt2StageID = "rhythm_tweezers_2"
            UnlockStageChecker.stageToBeCompleted(rt2StageID).testShouldStageBecomeUnlocked(progression, inboxState) 
                    && inboxState.getItemState(rt2Instruction.item.inboxItem.id)?.stageCompletionData?.skillStar == true
        }, listOf("contract_boosted_tweezers")))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["super_samurai_slice"])), dependsOnStageID = "rhythm_tweezers_2")
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["hole_in_one"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.Memo("memo_tim_gossip", hasToField = false, hasSeparateListingName = false, hasDifferentShortFrom = true)))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["fork_lifter"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["working_dough"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["tap_trial"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.Memo("memo_mgmt_merger", hasToField = true, hasSeparateListingName = true)))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["built_to_scale_ds"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.Memo("memo_mgmt_layoffs", hasToField = false, hasSeparateListingName = false)))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["rhythm_rally_2"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.InfoMaterial("info_on_defective_rods", hasSeparateListingName = true)))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.Memo("memo_mgmt_late_info_on_defective_rods", hasToField = true, hasSeparateListingName = false)))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["flock_step"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["fruit_basket_2"])))
//        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.Memo("memo_tim_gossip2", hasToField = false, hasSeparateListingName = false, hasDifferentShortFrom = true)))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["air_rally_2"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["tram_and_pauline"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.Memo("memo_tim_assemble", hasToField = false, hasSeparateListingName = false, hasDifferentShortFrom = true)))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["bouncy_road_2"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["second_contact"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["hole_in_one_2"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["super_samurai_slice_2"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.Memo("memo_tim_external", hasToField = false, hasSeparateListingName = false, hasDifferentShortFrom = true)))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["working_dough_2"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["monkey_watch"])))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["screwbots2"])))

        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.RobotTest("robotTestResults_1")), dependsOnStageID = "screwbots2")
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.RobotTest("robotTestResults_2")), dependsOnStageID = "screwbots2")
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.RobotTest("robotTestResults_3")), dependsOnStageID = "screwbots2")

        instructions += NewUnlockStage(UnlockStage("unlock_confidential_docs_memo",
            UnlockStageChecker.stageToBeCompleted("screwbots2"),
            listOf("robotTestResults_1", "robotTestResults_2", "robotTestResults_3"), minRequiredToComplete = 2))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.Memo("memo_mgmt_confidential_docs", hasToField = false, hasSeparateListingName = false)), dependsOnStageID = "unlock_confidential_docs_memo")

        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts["tap_trial_2"])))

        // Pre-boss, boss
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.Memo(ITEMID_TIM_BOSS_WARNING, hasToField = false, hasSeparateListingName = false, hasDifferentShortFrom = true)))
        instructions += SingleStageItem(Item(Category.MAIN, InboxItem.ContractDoc(Contracts[Contracts.ID_BOSS], subtype = IContractDoc.ContractSubtype.BOSS)))

        // Post-boss/postgame
        instructions += SingleStageItem(Item(Category.POSTGAME, InboxItem.Memo(ITEMID_POSTBOSS_LETTER_1, hasToField = false, hasSeparateListingName = true, hasDifferentShortFrom = true)))
        instructions += SingleStageItem(Item(Category.POSTGAME, InboxItem.Memo(ITEMID_POSTBOSS_LETTER_2, hasToField = false, hasSeparateListingName = true, hasDifferentShortFrom = true)))
        instructions += SingleStageItem(Item(Category.POSTGAME, InboxItem.Memo(ITEMID_POSTBOSS_LETTER_3, hasToField = false, hasSeparateListingName = true, hasDifferentShortFrom = true)))
        instructions += SingleStageItem(Item(Category.POSTGAME, InboxItem.EmploymentContract(ITEMID_POSTGAME_EMPLOYMENT_CONTRACT, useSecondarySignedTexture = true)))
        
        instructions += SingleStageItem(Item(Category.POSTGAME, InboxItem.Memo(ITEMID_WELCOME_BACK_POSTGAME, hasToField = false, hasSeparateListingName = true)))
        instructions += SingleStageItem(Item(Category.POSTGAME, InboxItem.InfoMaterial("info_on_robot_mode", hasSeparateListingName = true)))
        
        instructions += SingleStageItem(Item(Category.POSTGAME, InboxItem.ContractDoc(Contracts["crop_stomp_superhard"])), dependsOnStageID = ITEMID_WELCOME_BACK_POSTGAME)
        instructions += SingleStageItem(Item(Category.POSTGAME, InboxItem.ContractDoc(Contracts["first_contact_superhard"])), dependsOnStageID = ITEMID_WELCOME_BACK_POSTGAME)
        instructions += SingleStageItem(Item(Category.POSTGAME, InboxItem.ContractDoc(Contracts["bunny_hop_superhard"])), dependsOnStageID = ITEMID_WELCOME_BACK_POSTGAME)
        instructions += SingleStageItem(Item(Category.POSTGAME, InboxItem.ContractDoc(Contracts["screwbots_superhard"])), dependsOnStageID = ITEMID_WELCOME_BACK_POSTGAME)
        instructions += SingleStageItem(Item(Category.POSTGAME, InboxItem.ContractDoc(Contracts["fillbots2_superhard"])), dependsOnStageID = ITEMID_WELCOME_BACK_POSTGAME)
        instructions += SingleStageItem(Item(Category.POSTGAME, InboxItem.ContractDoc(Contracts["air_rally_superhard"])), dependsOnStageID = ITEMID_WELCOME_BACK_POSTGAME)
        instructions += SingleStageItem(Item(Category.POSTGAME, InboxItem.ContractDoc(Contracts["tap_trial_2_superhard"])), dependsOnStageID = ITEMID_WELCOME_BACK_POSTGAME)
        instructions += SingleStageItem(Item(Category.POSTGAME, InboxItem.ContractDoc(Contracts["built_to_scale_ds_superhard"])), dependsOnStageID = ITEMID_WELCOME_BACK_POSTGAME)
        instructions += NewUnlockStage(
            UnlockStage(
                "finish_all_superhard",
                UnlockStageChecker.stageToBeCompleted(ITEMID_WELCOME_BACK_POSTGAME),
                listOf(
                    "contract_air_rally_superhard",
                    "contract_built_to_scale_ds_superhard",
                    "contract_bunny_hop_superhard",
                    "contract_crop_stomp_superhard",
                    "contract_fillbots2_superhard",
                    "contract_first_contact_superhard",
                    "contract_screwbots_superhard",
                    "contract_tap_trial_2_superhard",
                ),
            )
        )
        instructions += SingleStageItem(Item(Category.POSTGAME, InboxItem.Memo(ITEMID_TIM_POSTGAME_COMPLETED, hasToField = false, hasSeparateListingName = true, hasDifferentShortFrom = true).apply { 
            this.songInfo = SongInfo.prmaniaGeneric("End of the Assembly Line", songNameWithLineBreaks = "End of the\nAssembly Line")
        }))
        
        
        // Parse instructions
        val itemsToAdd = mutableListOf<Item>()
        val unlockStagesToAdd = mutableListOf<UnlockStage>()
        val noUnlockStageID = "!! NO UnlockStage ID YET !!"
        var lastUnlockStageID = noUnlockStageID
        for (instruction in instructions) {
            when (instruction) {
                NullInstruction -> {}
                is NewItemNoStage -> itemsToAdd += instruction.item
                is SingleStageItem -> {
                    itemsToAdd += instruction.item
                    val inboxItemID = instruction.item.inboxItem.id
                    val stageID = (instruction.item.inboxItem as? InboxItem.ContractDoc)?.contract?.id ?: inboxItemID
                    val unlockReqs = if (lastUnlockStageID == noUnlockStageID)
                        UnlockStageChecker.alwaysUnlocked()
                    else UnlockStageChecker.stageToBeCompleted(instruction.dependsOnStageID ?: lastUnlockStageID)
                    val newUnlockStage = UnlockStage.singleItem(inboxItemID, unlockReqs, stageID = stageID)
                    unlockStagesToAdd += newUnlockStage
                    lastUnlockStageID = newUnlockStage.id
                }
                is NewUnlockStage -> {
                    val newUnlockStage = instruction.unlockStage
                    unlockStagesToAdd += newUnlockStage
                    lastUnlockStageID = newUnlockStage.id
                }
            }
        }
        
        this.setItems(itemsToAdd.map { it.inboxItem })
        this.itemCategories = itemsToAdd.associate { it.inboxItem to it.category }
        this.itemsByCategory = itemsToAdd.groupBy { it.category }.mapValues { list -> list.value.map { item -> item.inboxItem } }
        
        this.progression = Progression(unlockStagesToAdd)
    }


    private data class Item(val category: Category, val inboxItem: InboxItem)
    
    private sealed class Instruction
    private object NullInstruction : Instruction()
    private class SingleStageItem(val item: Item, val dependsOnStageID: String? = null) : Instruction()
    private class NewItemNoStage(val item: Item) : Instruction()
    private class NewUnlockStage(val unlockStage: UnlockStage) : Instruction()
}
