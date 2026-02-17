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

package com.mbrlabs.mundus.editor.ui.widgets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooserListener;
import com.mbrlabs.mundus.editor.ui.UI;

import java.io.FileFilter;

/**
 * @author Marcus Brummer
 * @version 30-12-2015
 */
public class FileChooserField extends VisTable {

    public interface SingleFileSelected {
        void selected(FileHandle fileHandle);
    }

    public interface MultiFileSelected {
        void selected(Array<FileHandle> files);
    }

    private final int width;

    private FileChooser.SelectionMode mode = FileChooser.SelectionMode.FILES;
    private SingleFileSelected singleFileSelected;
    private MultiFileSelected multiFileSelected;
    private final VisTextField textField;
    private final VisTextButton fcBtn;

    private Boolean multiselect = false;
    private FileFilter fileFilter = null;

    private String path;
    private FileHandle fileHandle;

    public FileChooserField(int width) {
        super();
        this.width = width;
        textField = new VisTextField();
        fcBtn = new VisTextButton("Select");

        setupUI();
        setupListeners();
    }

    public FileChooserField() {
        this(-1);
    }

    public void setEditable(boolean editable) {
        textField.setDisabled(!editable);
    }
    public void setMultiselect(boolean multiSelectable) {multiselect = multiSelectable;}
    public void setFileFilter(FileFilter filter) {fileFilter = filter;}

    public String getPath() {
        return path;
    }

    public FileHandle getFile() {
        return this.fileHandle;
    }

    public void setSingleCallback(SingleFileSelected fileSelected) {
        this.singleFileSelected = fileSelected;
    }
    public void setMultiCallback(MultiFileSelected fileSelected) {
        this.multiFileSelected = fileSelected;
    }

    public void setFileMode(FileChooser.SelectionMode mode) {
        this.mode = mode;
    }

    public void clear() {
        textField.setText("");
        fileHandle = null;
    }

    public void setText(String text) {
        textField.setText(text);
    }

    private void setupUI() {
        if (width <= 0) {
            add(textField).expandX().fillX().padRight(5);
            add(fcBtn).expandX().fillX().row();
        } else {
            add(textField).width(width * 0.75f).padRight(5);
            add(fcBtn).expandX().fillX();
        }
    }

    private void setupListeners() {

        fcBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                FileChooser fileChooser = UI.INSTANCE.getFileChooser();
                fileChooser.setSelectionMode(mode);
                fileChooser.setMultiSelectionEnabled(multiselect);
                fileChooser.setFileFilter(fileFilter);

                fileChooser.setListener(new FileChooserListener() {
                     @Override
                     public void selected(Array<FileHandle> files) {
                        fileHandle = files.first();
                        path = files.first().path();

                        textField.setText(files.first().parent().path());
                        if (singleFileSelected != null) {
                            singleFileSelected.selected(files.first());
                        } else if (multiFileSelected != null) {
                            multiFileSelected.selected(files);
                        }
                    }

                    @Override
                    public void canceled() {}
                });

                UI.INSTANCE.addActor(fileChooser.fadeIn());
            }
        });

    }



}
