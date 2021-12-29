package polyrhythmmania.solitaire

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import paintbox.binding.BooleanVar
import paintbox.binding.FloatVar
import paintbox.ui.*
import paintbox.util.ColorStack
import paintbox.util.MathHelper
import paintbox.util.gdxutils.*
import polyrhythmmania.PRManiaGame
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt


class SolitaireGame : ActionablePane() {
    
    data class ZoneTarget(val zone: CardZone, val index: Int, val offsetX: Float, val offsetY: Float)
    
    inner class CardZone(initX: Float, initY: Float, val maxCapacity: Int, val canDragFrom: Boolean) {
        
        val x: FloatVar = FloatVar(initX)
        val y: FloatVar = FloatVar(initY)
        val stack: CardStack = CardStack(mutableListOf())
        
        init {
            stack.x.bind { x.use() }
            stack.y.bind { y.use()}
        }
        
        fun canDragAsGroup(startIndex: Int): Boolean {
            if (startIndex !in stack.cardList.indices) return false
            
            return checkStackingRules(stack.cardList.drop(startIndex))
        }
    }
    
    inner class DragInfo {
        
        val draggingStack: CardStack = CardStack(mutableListOf())
        
        // Active when dragging
        var oldZone: CardZone? = null
        val offset: Vector2 = Vector2(0f, 0f)
        
        fun isDragging(): Boolean {
            return oldZone != null
        }
        
        fun cancelDrag() {
            val myList = draggingStack.cardList.toList()
            draggingStack.cardList.clear()
            oldZone?.stack?.cardList?.addAll(myList)
            oldZone = null
        }
        
        fun endDrag(newZone: CardZone) {
            val myList = draggingStack.cardList.toList()
            draggingStack.cardList.clear()
            newZone.stack.cardList += myList
            oldZone = null
            
            checkTableauAfterDrag()
        }
        
        fun startDrag(zoneTarget: ZoneTarget) {
            val myList = draggingStack.cardList
            myList.clear()
            val zoneCardList = zoneTarget.zone.stack.cardList
            val newSet = zoneCardList.drop(zoneTarget.index)
            
            if (!checkStackingRules(newSet)) {
                return
            }
            
            myList.addAll(newSet)
            repeat(newSet.size) {
                zoneCardList.removeAt(zoneTarget.index)
            }
            
            oldZone = zoneTarget.zone
            offset.set(zoneTarget.offsetX, zoneTarget.offsetY)
            updateDrag()
        }
        
        fun updateDrag() {
            draggingStack.x.set(lastMouseRelative.x - offset.x)
            draggingStack.y.set(lastMouseRelative.y - offset.y)
        }
    }

    private val lastMouseAbsolute: Vector2 = Vector2()
    private val lastMouseRelative: Vector2 = Vector2()
    
    val cardWidth: Float = 64f
    val cardHeight: Float = 80f
    val cardStackOffset: Float = 20f
    
    val inputsEnabled: BooleanVar = BooleanVar(true)
    
    val deck: List<Card> = Card.STANDARD_DECK.toList().shuffled()
    
    private val freeCells: List<CardZone>
    private val playerZones: List<CardZone>
    private val dealZone: CardZone
    private val foundationZones: List<CardZone>
    
    private val placeableCardZones: List<CardZone>
    private val allCardZones: List<CardZone>
    private val dragInfo: DragInfo = DragInfo()
    
    init {
        this.bounds.width.set(800f)
        this.bounds.height.set(400f)
        
        // Board is 12.5 units wide (800/64) and 5 units tall (400/80)
        val zoneSpacingX = 0.5f
        val zoneSpacingY = 1f / 3f
        
        freeCells = listOf(
                CardZone((1 + zoneSpacingX) * 0, 0f, 1, true),
                CardZone((1 + zoneSpacingX) * 1, 0f, 1, true),
                CardZone((1 + zoneSpacingX) * 2, 0f, 1, true),
                CardZone((1 + zoneSpacingX) * 3, 0f, 1, true),
        )
        playerZones = listOf(
                CardZone((1 + zoneSpacingX) * 0, (1 + zoneSpacingY), 999, true),
                CardZone((1 + zoneSpacingX) * 1, (1 + zoneSpacingY), 999, true),
                CardZone((1 + zoneSpacingX) * 2, (1 + zoneSpacingY), 999, true),
                CardZone((1 + zoneSpacingX) * 3, (1 + zoneSpacingY), 999, true),
                CardZone((1 + zoneSpacingX) * 4, (1 + zoneSpacingY), 999, true),
                CardZone((1 + zoneSpacingX) * 5, (1 + zoneSpacingY), 999, true),
        )
        dealZone = CardZone((1 + zoneSpacingX) * 4.5f, 0f, 1, false)
        foundationZones = mutableListOf(
                CardZone((1 + zoneSpacingX) * 6.5f, 0f, 7, false),
                CardZone((1 + zoneSpacingX) * 6.5f, 0f, 7, false),
                CardZone((1 + zoneSpacingX) * 6.5f, 0f, 7, false),
        ).apply {
            val totalHeight = this.size * 1 + (this.size - 1) * zoneSpacingY
            this.forEachIndexed { index, zone -> 
                zone.y.set((5f - totalHeight) / 2 + index * (1 + zoneSpacingY))
            }
        }
        
        placeableCardZones = freeCells + playerZones + foundationZones
        allCardZones = freeCells + playerZones + dealZone + foundationZones
        
        // Horizontal center
        val totalWidth = (allCardZones.maxOf { it.x.get() } + 1) - allCardZones.minOf { it.x.get() }
        val hcOffset = (12.5f - totalWidth) / 2
        allCardZones.forEach { 
            it.x.set(it.x.get() + hcOffset)
        }
        
        // Convert card zones from "units" to px
        allCardZones.forEach { zone: CardZone ->
            zone.x.set(zone.x.get() * cardWidth)
            zone.y.set(zone.y.get() * cardHeight)
        }
    }
    
    init {
        this.doClipping.set(true)
        
        addInputEventListener { event ->
            if (inputsEnabled.get()) {
                if (event is MouseInputEvent) {
                    val x = event.x
                    val y = event.y
                    lastMouseAbsolute.set(x, y)
                    val thisPos = this.getPosRelativeToRoot(lastMouseRelative)
                    lastMouseRelative.x = x - thisPos.x
                    lastMouseRelative.y = y - thisPos.y
                }
                
                inputListener(event)
            } else false
        }
    }
    
    init {
        // FIXME REMOVE
        playerZones.forEachIndexed { index, zone ->
            zone.stack.cardList += deck[0 + index * 3]
            zone.stack.cardList += deck[1 + index * 3]
            zone.stack.cardList += deck[2 + index * 3]
        }
    }
    
    private fun inputListener(event: InputEvent): Boolean {
        return when (event) {
            is TouchDown -> {
                if (event.button == Input.Buttons.LEFT) {
                    if (!dragInfo.isDragging()) {
                        // Check if clicking on any of the zones
                        val selected = getSelectedCardIndex()
                        if (selected != null && selected.zone.canDragFrom) {
                            dragInfo.startDrag(selected)
                        }
                    }
                } else if (event.button == Input.Buttons.RIGHT && dragInfo.isDragging()) {
                    dragInfo.cancelDrag()
                }

                true
            }
            is TouchDragged -> {
                if (dragInfo.isDragging()) {
                    dragInfo.updateDrag()
                    
                    true
                } else false
            }
            is ClickReleased -> {
                if (event.button == Input.Buttons.LEFT && dragInfo.isDragging()) {
                    val nearestZone = getNearestOverlappingDragZone()
                    if (nearestZone == null || nearestZone == dragInfo.oldZone || !canPlaceDragOn(nearestZone)) {
                        dragInfo.cancelDrag()
                    } else {
                        dragInfo.endDrag(nearestZone)
                    }

                    true
                } else false
            }
            else -> false
        }
    }
    
    private fun checkTableauAfterDrag() {
        inputsEnabled.set(false)
        
        inputsEnabled.set(true)
    }
    
    fun checkStackingRules(stack: List<Card>): Boolean {
        stack.forEachIndexed { index, card -> 
            if (index > 0) {
                val prevCard = stack[index - 1]
                
                if (prevCard.suit == CardSuit.WIDGET) {
                    // Only alternating-symbol widgets can be here
                    if (!(card.suit == CardSuit.WIDGET && card.symbol != prevCard.symbol)) {
                        return false
                    }
                } else {
                    // Non-WIDGET cards: must be alternating suit and directly one up in ASCENDING scale order
                    if (!(prevCard.suit != card.suit && card.suit != CardSuit.WIDGET && prevCard.symbol.scaleOrder + 1 == card.symbol.scaleOrder)) {
                        return false
                    }
                }
            }
        }
        return true
    }
    
    private fun canPlaceDragOn(targetZone: CardZone): Boolean {
        val dragStack = dragInfo.draggingStack
        if (dragStack.cardList.size + targetZone.stack.cardList.size > targetZone.maxCapacity) {
            return false
        }
        
        return checkStackingRules(listOfNotNull(targetZone.stack.cardList.lastOrNull()) + dragStack.cardList)
    }
    
    private fun getNearestOverlappingDragZone(): CardZone? {
        var nearest: CardZone? = null
        var mostArea = 0f
        
        val dragX = dragInfo.draggingStack.x.get()
        val dragY = dragInfo.draggingStack.y.get()
        val dragW = cardWidth
        val dragH = cardHeight // Only the topmost card of the stack counts for area checking
        val dragRect = Rectangle(dragX, dragY, dragW, dragH)

        for (zone in placeableCardZones) {
            val zoneRect = Rectangle(zone.x.get(), zone.y.get(), cardWidth, cardHeight + (zone.maxCapacity - 1) * cardStackOffset)
            if (!dragRect.overlaps(zoneRect)) continue
            
            val minX = max(dragRect.x, zoneRect.x)
            val minY = max(dragRect.y, zoneRect.y)
            val maxX = min(dragRect.maxX, zoneRect.maxX)
            val maxY = min(dragRect.maxY, zoneRect.maxY)
            val overlap = Rectangle(minX, minY, maxX - minX, maxY - minY)
            val area = overlap.area()

            if (area > mostArea) {
                mostArea = area
                nearest = zone
            }
        }
        
        return nearest
    }
    
    private fun getSelectedCardIndex(): ZoneTarget? {
        for (zone in allCardZones) {
            val stack = zone.stack
            val cardList = stack.cardList
            if (cardList.isEmpty()) continue

            // Height of the zone is cardHeight + (n - 1) * cardStackOffset 
            val cardHeightMinusOffset = cardHeight - cardStackOffset
            if (lastMouseRelative.x in zone.x.get()..(zone.x.get() + cardWidth) &&
                    lastMouseRelative.y in zone.y.get()..(zone.y.get() + (cardList.size * cardStackOffset) + cardHeightMinusOffset)) {
                val cardIndex = floor((lastMouseRelative.y - zone.y.get()) / cardStackOffset).toInt().coerceIn(0, cardList.size - 1)
                return ZoneTarget(zone, cardIndex, lastMouseRelative.x - zone.x.get(), lastMouseRelative.y - (zone.y.get() + cardIndex * cardStackOffset))
            }
        }
        
        return null
    }

    override fun renderSelf(originX: Float, originY: Float, batch: SpriteBatch) {
        val renderBounds = this.paddingZone
        val x = renderBounds.x.get() + originX
        val y = originY - renderBounds.y.get()
        val w = renderBounds.width.get()
        val h = renderBounds.height.get()
        val lastPackedColor = batch.packedColor

        val opacity: Float = this.apparentOpacity.get()
        val tmpColor: Color = ColorStack.getAndPush().set(1f, 1f, 1f, 1f)
        tmpColor.a *= opacity
        
        batch.color = tmpColor
//        batch.fillRect(x, y - h, w, h) // From RectElement
        
        val paintboxFont = PRManiaGame.instance.fontMainMenuMain
        val bmFont = paintboxFont.begin()
        bmFont.scaleMul(0.75f)
        
        allCardZones.forEach { zone ->
            val cs = zone.stack
            val renderX = x + cs.x.get()
            val renderY = y - cs.y.get()
            
            batch.setColor(1f, 1f, 1f, 0.2f)
            batch.drawRect(renderX, renderY - cardHeight, cardWidth, cardHeight, 4f)
        }
        
        batch.color = tmpColor
        allCardZones.forEach { zone ->
            val cs = zone.stack
            val renderX = x + cs.x.get()
            val renderY = y - cs.y.get()
            
            renderCardStack(renderX, renderY, batch, cs, bmFont)
        }
        val dragStack = dragInfo.draggingStack
        renderCardStack(x + dragStack.x.get(), y - dragStack.y.get(), batch, dragStack, bmFont)
        
        paintboxFont.end()

        ColorStack.pop()
        batch.packedColor = lastPackedColor
    }
    
    private fun renderCardStack(x: Float, y: Float, batch: SpriteBatch, cardStack: CardStack, font: BitmapFont) {
        val lastPackedColor = batch.packedColor
        cardStack.cardList.forEachIndexed { index, card ->
            val cw = cardWidth
            val ch = cardHeight
            val grey = 1f - (index * 0.1f)
            val renderX = x
            val renderY = (y - ch) - index * cardStackOffset
            batch.setColor(1f, grey, grey, 1f)
            batch.fillRect(renderX, renderY, cw, ch)
            batch.setColor(1f, grey * 0.5f, grey * 0.5f, 0.75f)
            batch.drawRect(renderX, renderY, cw, ch, 4f)
            
            font.setColor(card.suit.color)
            font.drawCompressed(batch, "${card.symbol}", renderX + 8f, (renderY + ch) - 8f, cardWidth - 8f * 2, Align.left)
        }
        batch.packedColor = lastPackedColor
    }
}