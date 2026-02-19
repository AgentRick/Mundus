package com.mbrlabs.mundus.editor.tools.terrain;

import com.badlogic.gdx.utils.Array;
import com.mbrlabs.mundus.commons.assets.ModelAsset;

import com.mbrlabs.mundus.editor.tools.brushes.TerrainBrush;

public class ModelPlacementBrushTool extends RadiusTerrainTool{

    private static Array<ModelAsset> modelAssets = new Array<>();

    public static void setModelAssets(Array<ModelAsset> modelAssets) {
        ModelPlacementBrushTool.modelAssets = modelAssets;
    }

    @Override
    public void act(TerrainBrush brush) {
     brush.placeModels(radiusDistanceComparison, TerrainBrush.getDensity(), modelAssets, true);
    }
}
