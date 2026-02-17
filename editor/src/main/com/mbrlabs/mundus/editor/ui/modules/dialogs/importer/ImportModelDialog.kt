/*
 * Copyright (c) 2016. See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mbrlabs.mundus.editor.ui.modules.dialogs.importer

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Disposable
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.mbrlabs.mundus.commons.assets.ModelAsset
import com.mbrlabs.mundus.commons.assets.meta.MetaModel
import com.mbrlabs.mundus.commons.shaders.MundusPBRShaderProvider
import com.mbrlabs.mundus.commons.utils.ShaderUtils
import com.mbrlabs.mundus.editor.Mundus
import com.mbrlabs.mundus.editor.assets.FileHandleWithDependencies
import com.mbrlabs.mundus.editor.assets.MetaSaver
import com.mbrlabs.mundus.editor.assets.ModelImporter
import com.mbrlabs.mundus.editor.core.project.ProjectManager
import com.mbrlabs.mundus.editor.events.AssetImportEvent
import com.mbrlabs.mundus.editor.events.LogEvent
import com.mbrlabs.mundus.editor.events.LogType
import com.mbrlabs.mundus.editor.ui.UI
import com.mbrlabs.mundus.editor.ui.modules.dialogs.BaseDialog
import com.mbrlabs.mundus.editor.ui.widgets.FileChooserField
import com.mbrlabs.mundus.editor.utils.Log
import com.mbrlabs.mundus.editorcommons.exceptions.AssetAlreadyExistsException
import net.mgsx.gltf.scene3d.scene.SceneRenderableSorter
import net.mgsx.gltf.scene3d.shaders.PBRDepthShaderProvider
import java.io.IOException
import java.nio.file.Paths

/**
 * @author Marcus Brummer
 * @version 07-06-2016
 */
class ImportModelDialog : BaseDialog("Import Mesh"), Disposable {

    companion object {
        private val TAG = ImportModelDialog::class.java.simpleName
    }

    private val supportedFileFormats = listOf(
        "", "gltf", "g3db", "glb", "obj", "fbx", "dae"
    )

    private val importMeshTable: ImportModelTable
    private val modelImporter: ModelImporter = Mundus.inject()
    private val projectManager: ProjectManager = Mundus.inject()

    init {
        isModal = true
        isMovable = true

        val root = VisTable()
        add<Table>(root).expand().fill()
        importMeshTable = ImportModelTable()

        root.add(importMeshTable).minWidth(600f).expand().fill().left().top()
    }

    override fun dispose() {
        importMeshTable.dispose()
    }

    override fun close() {
        importMeshTable.deleteTempModel()
        importMeshTable.resetDialog()
        super.close()
    }

    /**
     */
    private inner class ImportModelTable : VisTable(), Disposable {
        // UI elements
        private val importBtn = VisTextButton("IMPORT")
        private val modelInput = FileChooserField(300)


        private var importedModels: List<FileHandleWithDependencies?> = emptyList()
        private var maxBones = 0

        init {
            this.setupUI()
            this.setupListener()
        }

        private fun setupUI() {
            val root = VisTable()
            // root.debugAll();
            root.padTop(6f).padRight(6f).padBottom(22f)
            add(root)

            val inputTable = VisTable()

            root.add(inputTable).width(300f).height(300f).padRight(10f)
            root.addSeparator(true)

            inputTable.left().top()

            val label = VisLabel()
            label.setText("The recommended format is '.gltf' separate (bin file, gltf file, textures). Mundus relies on textures being external image files," +
                    " so using binary files like .glb or embedded .gltf where the files are compressed and packed into the binary is " +
                    "not recommended. Automatic importing of material attributes only works with separate .gltf files currently.")
            label.wrap = true
            label.width = 300f
            inputTable.add(label).expandX().prefWidth(300f).padBottom(10f).row()

            inputTable.add(VisLabel("Model File")).left().padBottom(5f).row()
            inputTable.add(modelInput).fillX().expandX().padBottom(10f).row()
            inputTable.add(importBtn).fillX().expand().bottom()

            modelInput.setEditable(false)
            modelInput.setMultiselect(true)
            modelInput.setFileFilter { file -> supportedFileFormats.contains(file.extension) }
        }

        private fun setupListener() {

            // model chooser
            modelInput.setMultiCallback { files ->
                for (fileHandle in files) {
                    addToList(fileHandle)
                }
            }

            // import btn
            importBtn.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if (!importedModels.isEmpty()) {

                        importedModels.forEach { model ->
                            try {
                                val modelAsset = importModel(model!!)
                                val modelBoneCount = modelAsset.meta.model.numBones

                                // If the imported model has more bones than current shaders numBones, create new
                                // shader provider with new max bones.
                                if (modelBoneCount > projectManager.current().assetManager.maxNumBones) {
                                    val config = ShaderUtils.buildPBRShaderConfig(modelBoneCount)
                                    projectManager.modelBatch = ModelBatch(MundusPBRShaderProvider(config), SceneRenderableSorter())

                                    val depthConfig = ShaderUtils.buildPBRShaderDepthConfig(modelBoneCount)
                                    projectManager.setDepthBatch((ModelBatch(PBRDepthShaderProvider(depthConfig))))

                                    projectManager.current().assetManager.maxNumBones = modelBoneCount
                                    Mundus.postEvent(LogEvent(LogType.INFO, "Max Bone count increased to $modelBoneCount"))
                                }

                                Mundus.postEvent(AssetImportEvent(modelAsset))
                                UI.toaster.success("Mesh imported")
                            } catch (e: IOException) {
                                e.printStackTrace()
                                UI.toaster.error("Error while creating a ModelAsset")
                            } catch (ee: AssetAlreadyExistsException) {
                                Log.exception(TAG, ee)
                                UI.toaster.error("Error: There already exists a model with the same name")
                            }
                        }

                        dispose()
                        close()
                    } else {
                        UI.toaster.error("There is nothing to import")
                    }
                }
            })
        }

        @Throws(IOException::class, AssetAlreadyExistsException::class)
        private fun importModel(model: FileHandleWithDependencies): ModelAsset {

            // create model asset
            val assetManager = projectManager.current().assetManager
            val modelAsset = assetManager.createModelAsset(model)

            // create materials
            modelAsset.meta.model = MetaModel()
            for (mat in modelAsset.model.materials) {
                val materialAsset = assetManager.createMaterialAsset(modelAsset.id.substring(0, 4) + "_" + mat.id)

                modelImporter.populateMaterialAsset(model, projectManager.current().assetManager, mat, materialAsset)
                projectManager.current().assetManager.saveMaterialAsset(materialAsset)

                modelAsset.meta.model.defaultMaterials.put(mat.id, materialAsset.id)
                modelAsset.defaultMaterials[mat.id] = materialAsset
            }

            modelAsset.meta.model.numBones = maxBones

            // save meta file
            val saver = MetaSaver()
            saver.save(modelAsset.meta)

            modelAsset.applyDependencies()

            return modelAsset
        }

        private fun addToList(model: FileHandle) {
            val model = modelImporter.importToTempFolder(model)
            importedModels += model
        }


        override fun dispose() {
            modelInput.clear()
        }

        fun deleteTempModel() {
            if (importedModels.isEmpty()) return

            for (importedModel in importedModels) {
                val parentDirectory = importedModel?.file?.parent()
                val isDir = parentDirectory?.isDirectory
                if (isDir == true) {
                    val path = Paths.get("mundus", "temp")
                    val tempModelDirPath = parentDirectory.file().absolutePath
                    // A defensive check, just to make sure the directory we are deleting is in the temp directory
                    if (tempModelDirPath.contains(path.toString())) {
                        parentDirectory.deleteDirectory()
                        Mundus.postEvent(LogEvent("Deleted temporary model directory at $tempModelDirPath"))
                    }
                }
            }

            importedModels = emptyList()
        }

        fun resetDialog() {
            modelInput.clear()
        }
    }

}
