package io.github.chrislo27.paintbox.font

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Colors
import java.lang.NumberFormatException


/**
 * A very simplistic markup language for forming [TextBlock]s.
 *
 * ```
 * - The language has tags on a stack.
 * - A tag is a non-empty set of attributes enclosed in square brackets [b].
 * - A tag is ended by a pair of square brackets with nothing inside. []
 * - An attribute is defined as a key-value pair or a key.
 *   - A key-value pair is key=value
 *   - Attributes are separated by one or more spaces.
 *   - Keys and values are case sensitive.
 *   - A key without a value is treated as key=true.
 *   - Values are either text, numbers (decimal numbers), or booleans (true/false).
 * - The backslash character \ is the escape character and will escape ANY character after it, including another backslash.
 * - An unclosed start or end tag is counted as normal text.
 * ```
 *
 * The list of attributes is defined as follows:
 * ```
 * Core tags:
 * font=font_name : Defines the font for the text run. The font_name has to be pre-defined as part of the Markup object.
 * color=#RRGGBBAA : Defines the text run color.
 * color=#RRGGBB : Defines the text run color with alpha being FF.
 * color=color_name : Defines the text run color using the name defined in com.badlogic.gdx.graphics.Colors. If there is no color then it does not take effect.
 * scalex=1.0 : Defines the scaleX, should be a float
 * scaley=1.0 : Defines the scaleY, should be a float
 * offsetx=1.0 : Defines the offsetXEm, should be a float
 * offsety=1.0 : Defines the offsetXEm, should be a float
 * carryoverx=true : Defines carryOverOffsetX, should be a boolean
 * carryovery=true : Defines carryOverOffsetY, should be a boolean
 *
 * Utility tags:
 * b : Defines font=${styles.bold}
 * i : Defines font=${styles.italic}
 * (if bold and italic are present) : Defines font=${styles.boldItalic}
 * sub : Subscript. Defines scalex=0.58, scaley=0.58, offsety=-0.333
 * sup : Superscript. Defines offsety=1.333, carryovery=true
 * exp : Exponent. Defines offsety=1.333, carryovery=false, scalex=0.58, scaley=0.58
 * ```
 */
class Markup(fontMapping: Map<String, PaintboxFont>, val defaultTextRun: TextRun,
             val styles: FontStyles = FontStyles.DEFAULT) {

    companion object {
        val DEFAULT_FONT_NAME: String = "DEFAULT"
        val FONT_NAME_BOLD: String = "bold"
        val FONT_NAME_ITALIC: String = "italic"
        val FONT_NAME_BOLDITALIC: String = "bolditalic"

        val TAG_FONT: String = "font"
        val TAG_COLOR: String = "color"
        val TAG_SCALEX: String = "scalex"
        val TAG_SCALEY: String = "scaley"
        val TAG_OFFSETX: String = "offsetx"
        val TAG_OFFSETY: String = "offsety"
        val TAG_CARRYOVERX: String = "carryoverx"
        val TAG_CARRYOVERY: String = "carryovery"
        val TAG_BOLD: String = "bold"
        val TAG_BOLD2: String = "b"
        val TAG_ITALIC: String = "italic"
        val TAG_ITALIC2: String = "i"
        val TAG_SUBSCRIPT: String = "sub"
        val TAG_SUPERSCRIPT: String = "sup"
        val TAG_EXPONENT: String = "exp"

//        @JvmStatic
//        fun main(args: Array<String>) {
//            val markup = Markup()
//            println(markup.parseIntoSymbols("test [    font\\]name=bold_font    color=#ff3322  tag3 tag4 tag5 tag6=hi   ]subtag text[color]inner tag[]back to outer[]and finally outermost"))
//        }
    }

    data class FontStyles(val bold: String, val italic: String, val boldItalic: String) {
        companion object {
            val DEFAULT: FontStyles = FontStyles(DEFAULT_FONT_NAME, DEFAULT_FONT_NAME, DEFAULT_FONT_NAME)
        }
    }

    val fontMapping: Map<String, PaintboxFont> = mapOf((DEFAULT_FONT_NAME to defaultTextRun.font)) + fontMapping

    fun parse(markup: String): TextBlock {
        return parse(parseSymbolsToTags(parseIntoSymbols(markup)))
    }

    private fun parse(tags: List<Tag>): TextBlock {
        val runs = mutableListOf<TextRun>()

        tags.forEach { tag ->
            var font = fontMapping.getValue(tag.attrMap[TAG_FONT]?.valueAsString() ?: DEFAULT_FONT_NAME)
            val colorAttr = tag.attrMap[TAG_COLOR]?.value
            val color: Int = if (colorAttr is String) {
                if (colorAttr.startsWith("#")) {
                    try {
                        Color.argb8888(Color.valueOf(colorAttr))
                    } catch (ignored: NumberFormatException) {
                        Color.argb8888(Color.WHITE)
                    }
                } else {
                    val c = Colors.get(colorAttr)
                    Color.argb8888(c ?: Color.WHITE)
                }
            } else if (colorAttr is Int) colorAttr else Color.argb8888(Color.WHITE)
            var scaleX = tag.attrMap[TAG_SCALEX]?.valueAsFloatOr(1f) ?: 1f
            var scaleY = tag.attrMap[TAG_SCALEY]?.valueAsFloatOr(1f) ?: 1f
            var offsetX = tag.attrMap[TAG_OFFSETX]?.valueAsFloatOr(0f) ?: 0f
            var offsetY = tag.attrMap[TAG_OFFSETY]?.valueAsFloatOr(0f) ?: 0f
            var carryoverX = tag.attrMap[TAG_CARRYOVERX]?.valueAsBooleanOr(true) ?: true
            var carryoverY = tag.attrMap[TAG_CARRYOVERY]?.valueAsBooleanOr(false) ?: false


            val bold = (tag.attrMap[TAG_BOLD]?.valueAsBooleanOr(false)
                    ?: false) || (tag.attrMap[TAG_BOLD2]?.valueAsBooleanOr(false) ?: false)
            val italic = (tag.attrMap[TAG_ITALIC]?.valueAsBooleanOr(false)
                    ?: false) || (tag.attrMap[TAG_ITALIC2]?.valueAsBooleanOr(false) ?: false)
            val bolditalic = bold && italic

            if (bolditalic) {
                font = fontMapping[styles.boldItalic] ?: fontMapping.getValue(DEFAULT_FONT_NAME)
            } else if (bold) {
                font = fontMapping[styles.bold] ?: fontMapping.getValue(DEFAULT_FONT_NAME)
            } else if (italic) {
                font = fontMapping[styles.italic] ?: fontMapping.getValue(DEFAULT_FONT_NAME)
            }

            val subscript = tag.attrMap[TAG_SUBSCRIPT]?.valueAsBooleanOr(false) ?: false
            val superscript = tag.attrMap[TAG_SUPERSCRIPT]?.valueAsBooleanOr(false) ?: false
            val exponent = tag.attrMap[TAG_EXPONENT]?.valueAsBooleanOr(false) ?: false

            if (subscript) {
                // scalex=0.58, scaley=0.58, offsety=-0.333
                scaleX = 0.58f
                scaleY = 0.58f
                offsetY = -0.333f
            }
            if (superscript) {
                // offsety=1.333, carryovery=true
                offsetY = 1.333f
                carryoverY = true
            }
            if (exponent) {
                // offsety=1.333, carryovery=false, scalex=0.58, scaley=0.58
                offsetY = 1.333f
                carryoverY = false
                scaleX = 0.58f
                scaleY = 0.58f
            }

            runs += TextRun(font, tag.text, color, scaleX, scaleY, offsetX, offsetY, carryoverX, carryoverY)
        }

        return TextBlock(runs)
    }

    private fun parseSymbolsToTags(symbols: List<Symbol>): List<Tag> {
        if (symbols.isEmpty()) return parseSymbolsToTags(listOf(Symbol.Text("")))

        val defaultRootTag = Tag(setOf(Attribute(TAG_FONT, DEFAULT_FONT_NAME),
                Attribute(TAG_COLOR, defaultTextRun.color),
                Attribute(TAG_SCALEX, defaultTextRun.scaleX),
                Attribute(TAG_SCALEY, defaultTextRun.scaleY),
                Attribute(TAG_OFFSETX, defaultTextRun.offsetXEm),
                Attribute(TAG_OFFSETY, defaultTextRun.offsetYEm),
                Attribute(TAG_CARRYOVERX, defaultTextRun.carryOverOffsetX),
                Attribute(TAG_CARRYOVERY, defaultTextRun.carryOverOffsetY)),
                "")
        val tagStack = mutableListOf<Tag>(defaultRootTag)
        val tags = mutableListOf<Tag>()
        var text = ""

        symbols.forEach { symbol ->
            when (symbol) {
                is Symbol.Text -> {
                    text += symbol.str
                }
                is Symbol.StartTag -> {
                    // Push previous text segment as a Tag
                    val topOfStack = tagStack.last()
                    if (text.isNotEmpty()) {
                        val newTag = topOfStack.copy(text = text)
                        tags += newTag
                        text = ""
                    }
                    // Begin the new tag
                    tagStack += Tag(symbol.attr, "")
                }
                Symbol.EndTag -> {
                    // Push previous text segment as a Tag
                    val topOfStack = tagStack.last()
                    if (text.isNotEmpty()) {
                        val newTag = topOfStack.copy(text = text)
                        tags += newTag
                        text = ""
                    }
                    // Pop the tag stack
                    if (tagStack.size > 1) {
                        tagStack.removeLastOrNull()
                    }
                }
            }
        }

        if (text.isNotEmpty()) {
            val topOfStack = tagStack.last()
            val newTag = topOfStack.copy(text = text)
            tags += newTag
            text = ""
        }

        return tags
    }

    private fun parseIntoSymbols(markup: String): List<Symbol> {
        if (markup.isEmpty()) {
            return listOf(Symbol.Text(""))
        }

        val symbols = mutableListOf<Symbol>()

        var strIndex = 0
        var isEscaping = false

        var currentText = ""

        var inTag = false
        val currentTagAttr = mutableSetOf<Attribute>()
        var parsingAttributeValue = false
        var currentAttrKey = ""
        var startOfTagContent = "" // Used if the tag doesn't finish, then the entire content is a Text symbol instead
        while (strIndex <= markup.length) {
            val currentChar: Char? = if (strIndex >= markup.length) null else markup[strIndex]

            if (isEscaping) {
                // Accept next character as-is, don't attempt parsing on it
                currentText += if (currentChar == null) { // End of string, add a backslash by itself
                    "\\"
                } else "$currentChar"
                isEscaping = false
            } else {
                if (currentChar == '\\') {
                    isEscaping = true
                } else {
                    if (!inTag) {
                        if (currentChar == '[') {
                            // End the previous symbol as a Text
                            symbols += Symbol.Text(currentText)
                            // Start parsing the new tag's attributes
                            inTag = true
                            parsingAttributeValue = false
                            currentText = ""
                            currentAttrKey = ""
                            startOfTagContent = "$currentChar"
                            currentTagAttr.clear()
                        } else if (currentChar == null) {
                            symbols += Symbol.Text(currentText)
                        } else {
                            // Append to current text
                            currentText += "$currentChar"
                        }
                    } else { // Parse tag attributes
                        // End the tag definition
                        if (currentChar == null) {
                            symbols += Symbol.Text(startOfTagContent)
                        } else if (currentChar == ']') {
                            // Complete the last attribute if not done
                            if (parsingAttributeValue && currentAttrKey.isNotEmpty()) {
                                currentTagAttr.add(Attribute(currentAttrKey, currentText))
                                currentAttrKey = ""
                                currentText = ""
                            } else if (currentText.isNotEmpty()) {
                                // Boolean true
                                currentAttrKey = currentText
                                currentTagAttr.add(Attribute(currentAttrKey, "true"))
                                currentAttrKey = ""
                                currentText = ""
                            }

                            val attributes = currentTagAttr.toSet()
                            if (attributes.isEmpty()) {
                                // An ending tag [] indicator.
                                // Submit the EndTag symbol
                                symbols += Symbol.EndTag
                            } else {
                                // Submit the attributes as a StartTag symbol
                                symbols += Symbol.StartTag(attributes)
                            }
                            inTag = false
                            currentTagAttr.clear()
                            parsingAttributeValue = false
                        } else {
                            // Parse attributes
                            startOfTagContent += "$currentChar"
                            if (parsingAttributeValue) {
                                if (currentChar == ' ') {
                                    // End attribute value
                                    currentTagAttr.add(Attribute(currentAttrKey, currentText))
                                    currentAttrKey = ""
                                    currentText = ""
                                    parsingAttributeValue = false
                                } else {
                                    currentText += "$currentChar"
                                }
                            } else {
                                if (currentChar == '=') {
                                    parsingAttributeValue = true
                                    currentAttrKey = currentText
                                    currentText = ""
                                } else if (currentChar == ' ' && currentText.isNotEmpty()) {
                                    // Finished this attribute, the value is boolean true
                                    currentAttrKey = currentText
                                    currentText = ""
                                    currentTagAttr.add(Attribute(currentAttrKey, "true"))
                                    currentAttrKey = ""
                                    parsingAttributeValue = false
                                } else {
                                    if (currentText.isNotEmpty() || currentChar != ' ') {
                                        currentText += "$currentChar"
                                    }
                                }
                            }
                        }
                    }
                }
            }

            strIndex++
        }

        return symbols
    }

    data class Attribute(val key: String, val value: Any) {
        fun valueAsIntOr(default: Int): Int = if (value is Int) value else if (value is String) (value.toIntOrNull()
                ?: default) else default

        fun valueAsString(): String = value.toString()
        fun valueAsFloatOr(default: Float): Float = when (val v = value) {
            is Float -> v
            is Double -> v.toFloat()
            is Number -> v.toFloat()
            is String -> v.toFloatOrNull() ?: default
            else -> default
        }

        fun valueAsBooleanOr(default: Boolean): Boolean = when (val v = value) {
            is Boolean -> v
            is String -> if (v == "true") true else if (v == "false") false else default
            else -> default
        }

        override fun toString(): String {
            return "$key=$value"
        }
    }

    data class Tag(val attributes: Set<Attribute>, val text: String) {
        val attrMap: Map<String, Attribute> = attributes.associateBy { it.key }
    }

    private sealed class Symbol {
        class Text(val str: String) : Symbol() {
            override fun toString(): String {
                return "Text[\"$str\"]"
            }
        }

        class StartTag(val attr: Set<Attribute>) : Symbol() {
            override fun toString(): String {
                return "StartTag[${attr.toList().joinToString(separator = " ")}]"
            }
        }

        object EndTag : Symbol() {
            override fun toString(): String {
                return "EndTag"
            }
        }
    }
}