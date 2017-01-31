package com.enryold.onioncache.models;

import com.enryold.onioncache.interfaces.ICacheLayerDataModel;

/**
 * Created by enryold on 31/01/17.
 */
public class OnionDataModel implements ICacheLayerDataModel
{
    public String customDataKey;

    @Override
    public String getCustomDataKey() {
        return customDataKey;
    }

    @Override
    public void setCustomDataKey(String customDataKey) {
        this.customDataKey = customDataKey;
    }

    @Override
    public String dataModelTableName() {
        return null;
    }
}
