package com.enryold.onioncache.interfaces;


/**
 * Created by enryold on 16/01/17.
 */
public interface ICacheLayerDataModel
{
    ICacheLayerDataModelKey dataModelUniqueKey();
    ICacheLayerDataModelKey setCustomDataModelUniqueKey(String key);
    String dataModelTableName();
}
