package polyrhythmmania.storymode.screen.desktop

import polyrhythmmania.storymode.contract.Contract
import polyrhythmmania.storymode.inbox.InboxItem
import polyrhythmmania.storymode.inbox.InboxItemState
import polyrhythmmania.storymode.screen.ExitCallback


interface DesktopController {

    enum class SFXType {
        ENTER_LEVEL, CLICK_INBOX_ITEM, INBOX_ITEM_UNLOCKED, 
    }
    
    
    fun createExitCallback(contract: Contract, inboxItem: InboxItem?, inboxItemState: InboxItemState?): ExitCallback
    
    fun playLevel(contract: Contract, inboxItem: InboxItem?, inboxItemState: InboxItemState?)
    
    fun playSFX(sfx: SFXType)

}

object NoOpDesktopController : DesktopController {

    override fun createExitCallback(contract: Contract, inboxItem: InboxItem?,
                                    inboxItemState: InboxItemState?): ExitCallback = ExitCallback { }

    override fun playLevel(contract: Contract, inboxItem: InboxItem?, inboxItemState: InboxItemState?) {
    }

    override fun playSFX(sfx: DesktopController.SFXType) {
    }
}

