package com.mbrlabs.mundus.editor.ui.modules.inspector.components.terrain

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.layout.GridGroup
import com.kotcrab.vis.ui.widget.Separator
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTextField
import com.mbrlabs.mundus.commons.assets.AssetType
import com.mbrlabs.mundus.commons.assets.ModelAsset
import com.mbrlabs.mundus.commons.utils.TextureProvider
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.tools.brushes.TerrainBrush
import com.mbrlabs.mundus.editor.tools.terrain.ModelPlacementBrushTool
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.widgets.AutoFocusScrollPane
import com.mbrlabs.mundus.editor.ui.widgets.FloatFieldWithLabel

class ModelPlacementBrushTab(parent: TerrainComponentWidget) : BaseBrushTab(parent, TerrainBrush.BrushMode.Model_Brush) {

    private val selectedModels = Array<ModelAsset>()
    private val projectManager: ProjectManager = Mundus.inject()

    private val table = VisTable()
    private val densityInput = FloatFieldWithLabel("Density", 65, false)

    private val assetTable = VisTable()

    private val searchField = VisTextField()
    private val filesView = GridGroup(80f, 4f) // Etwas größer und mehr Abstand
    private val selectNoneBtn = VisTextButton("clear selection")
    private val applyBtn = VisTextButton("apply")

    override fun getTabTitle(): String {
        return "Model Placement"
    }

    override fun getContentTable(): Table {
        return table
    }

    override fun onShow() {
        reloadAssets()
        densityInput.text = TerrainBrush.getDensity().toString()
        super.onShow()
    }

    init {
        table.align(Align.left)
        table.add(VisLabel("Places models randomly on the terrain")).center().row()

        table.add(terrainBrushGrid).expand().fill().row()

        table.add(assetTable).pad(10f).row()

        table.add(densityInput).width(20f).row()
        setupUI()
        setupListeners()
    }



    private fun setupUI() {
        assetTable.defaults().maxHeight(100f)
        // --- Top Bar (Suche & Tools) ---
        val topTable = VisTable()
        topTable.add(VisLabel("Search: ")).left()
        topTable.add(searchField).growX().padRight(5f)

        topTable.add(selectNoneBtn)

        assetTable.add(topTable).padBottom(5f).row()
        assetTable.add(Separator()).padBottom(5f).colspan(2).row()

        // --- Content Area ---
        filesView.touchable = Touchable.enabled
        assetTable.add(createScrollPane(filesView, false)).expandX().fill().row()

        assetTable.add(applyBtn).center().padTop(5f).row()
    }

    private fun setupListeners() {
        // Filter-Logik bei Texteingabe
        searchField.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                filterAssets(searchField.text)
            }
        })

        selectNoneBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                deselectAll()
            }
        })

        applyBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                ModelPlacementBrushTool.setModelAssets(selectedModels)

                val selectedGameObject = UI.outline.getSelectedGameObject()
                projectManager.current().currScene.currentSelection = selectedGameObject
            }
        })

        densityInput.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: com.badlogic.gdx.scenes.scene2d.Actor?) {
                if (densityInput.isValid()) TerrainBrush.setDensity(densityInput.float)
            }
        })
    }

    private fun createScrollPane(actor: Actor, disableX: Boolean): VisScrollPane {
        val scrollPane = AutoFocusScrollPane(actor)
        scrollPane.setFadeScrollBars(false)
        scrollPane.setScrollingDisabled(disableX, false)
        return scrollPane
    }

    fun reloadAssets() {
        filesView.clearChildren()
        val projectContext = projectManager.current()
        for (asset in projectContext.assetManager.assets) {
            if (asset.meta.type != AssetType.MODEL) continue
            val assetItem = AssetItem(asset as ModelAsset)
            filesView.addActor(assetItem)
            assetItem.layout()
        }
        filesView.layout()
    }

    private fun filterAssets(query: String) {
        val lowerQuery = query.lowercase()
        for (actor in filesView.children) {
            if (actor is AssetItem) {
                val match = actor.asset.name.lowercase().contains(lowerQuery)
                actor.isVisible = match
            }
        }
        filesView.invalidateHierarchy()
    }

    private fun deselectAll() {
        selectedModels.clear()
        for (actor in filesView.children) {
            if (actor is AssetItem) {
                actor.setSelected(false)
            }
        }
    }





    private inner class AssetItem(val asset: ModelAsset) : VisTable() {

        private val nameLabel: VisLabel
        private val stack = Stack()
        private val background = VisTable()
        // Jedes Item braucht sein eigenes Overlay Image!
        private val selectionOverlay = Image(VisUI.getSkin().getDrawable("default-select-selection"))

        init {
            setBackground("menu-bg") // Basis Hintergrund

            // Konfiguration des Overlays
            selectionOverlay.color.a = 0.6f
            selectionOverlay.setFillParent(true)
            selectionOverlay.touchable = Touchable.disabled
            selectionOverlay.isVisible = false // Standardmäßig aus


            // Bild laden
            loadThumbnail()

            // Name Label
            nameLabel = VisLabel(asset.name, "tiny")
            nameLabel.wrap = true
            nameLabel.setAlignment(Align.center)

            // Layout aufbauen
            stack.add(background)
            stack.add(selectionOverlay)

            add(stack).row()
            add(nameLabel).width(50f).top().expandX()

            // Click Listener
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    toggleSelect()
                }
            })
        }

        private fun loadThumbnail() {
            if (asset is TextureProvider) {
                val texture = asset.texture
                if (texture != null) {
                    val img = Image(texture)
                    img.setScaling(com.badlogic.gdx.utils.Scaling.fit)
                    background.add(img).grow()
                }
            } else {
                // Platzhalter anzeigen, falls keine Textur da ist
                background.add(VisLabel("3D", Align.center)).grow()
            }
        }

        fun toggleSelect() {
            val isSelected = selectedModels.contains(asset)
            setSelected(!isSelected)

            if (!isSelected) {
                selectedModels.add(asset)
            } else {
                selectedModels.removeValue(asset, true)
            }
        }

        fun setSelected(selected: Boolean) {
            selectionOverlay.isVisible = selected
        }
    }

}