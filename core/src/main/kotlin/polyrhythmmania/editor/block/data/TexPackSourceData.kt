package polyrhythmmania.editor.block.data

import com.eclipsesource.json.JsonObject
import paintbox.binding.BooleanVar
import paintbox.binding.Var
import paintbox.ui.StringConverter
import paintbox.ui.contextmenu.*
import polyrhythmmania.Localization
import polyrhythmmania.editor.Editor
import polyrhythmmania.world.texturepack.TexPackSrcSelectorMenuPane
import polyrhythmmania.world.texturepack.TexturePackSource


class TexPackSourceData(defaultSetting: TexturePackSource = TexturePackSource.StockGBA) {

    val texPackSrc: Var<TexturePackSource> = Var(defaultSetting)
    val inheritFromStarting: BooleanVar = BooleanVar(false)


    fun createMenuItems(editor: Editor): List<MenuItem> {
        val palette = editor.editorPane.palette
        val inheritedSrc = editor.container.texturePackSource.getOrCompute()
        val inheritCheckbox = CheckBoxMenuItem.create(inheritFromStarting,
                Localization.getValue("blockContextMenu.texturePackSource.inherit"),
                palette.markup)
        val texPackSrcSelectorMenuPane = TexPackSrcSelectorMenuPane(editor.editorPane,
                texPackSrc.getOrCompute()) { newSetting ->
            this.texPackSrc.set(newSetting)
        }.apply {
            this.combobox.apply {
                this.disabled.bind { inheritFromStarting.use() }
                this.markup.bind { if (inheritFromStarting.use()) palette.markupInvertedItalics else palette.markup }
                this.itemStringConverter.bind {
                    val conv = TexPackSrcSelectorMenuPane.TEXTURE_PACK_SOURCE_STRING_CONVERTER
                    if (inheritFromStarting.use()) {
                        StringConverter { ignored ->
                            // This StringConverter always returns the string for the inheritedSrc, since the inheritFromStarting checkbox is checked
                            TexPackSrcSelectorMenuPane.TEXTURE_PACK_SOURCE_STRING_CONVERTER.convert(inheritedSrc)
                        }
                    } else conv
                }
            }
        }

        return listOf(
                LabelMenuItem.create(Localization.getValue("blockContextMenu.texturePackSource"), palette.markup),
                SeparatorMenuItem(),
                inheritCheckbox,
                SeparatorMenuItem(),
                CustomMenuItem(texPackSrcSelectorMenuPane),
        )
    }

    fun writeToJson(obj: JsonObject) {
        obj.add("texPackSrc", texPackSrc.getOrCompute().jsonId)
        obj.add("inherit", inheritFromStarting.get())
    }

    fun readFromJson(obj: JsonObject) {
        texPackSrc.set(TexturePackSource.idToSource(obj.getInt("texPackSrc", 0)) ?: TexturePackSource.StockGBA)
        inheritFromStarting.set(obj.getBoolean("inherit", false))
    }
    
    fun copyFrom(other: TexPackSourceData) {
        this.texPackSrc.set(other.texPackSrc.getOrCompute())
        this.inheritFromStarting.set(other.inheritFromStarting.get())
    }

}