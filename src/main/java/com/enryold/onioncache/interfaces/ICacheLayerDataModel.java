package com.enryold.onioncache.interfaces;


/**
 * Created by enryold on 16/01/17.
 */
public interface ICacheLayerDataModel
{
    String getCustomDataKey();
    void setCustomDataKey(String key);
    String dataModelTableName();
}
