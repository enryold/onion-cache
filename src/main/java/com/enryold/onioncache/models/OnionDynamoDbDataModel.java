package com.enryold.onioncache.models;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.enryold.onioncache.OnionCacheLayerDataModelKey;
import com.enryold.onioncache.interfaces.ICacheLayerDataModel;
import com.enryold.onioncache.interfaces.ICacheLayerDataModelKey;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Created by enryold on 26/01/17.
 */
public class OnionDynamoDbDataModel implements ICacheLayerDataModel {

    protected ICacheLayerDataModelKey customDataModelKey;

    @Override
    public ICacheLayerDataModelKey dataModelUniqueKey() {

        if(customDataModelKey != null) { return customDataModelKey; }

        try
        {
            Optional<Method> hashMethod = this.getDataModelAnnotationMethod(DynamoDBHashKey.class);
            Object hash = (hashMethod.isPresent()) ? String.valueOf(hashMethod.get().invoke(this)) : "";

            Optional<Method> rangeMethod = this.getDataModelAnnotationMethod(DynamoDBRangeKey.class);
            Object range =(rangeMethod.isPresent()) ? String.valueOf(rangeMethod.get().invoke(this)) : "";

            return new OnionCacheLayerDataModelKey(hash.toString()+range.toString());
        }
        catch(Exception e)
        {
            return null;
        }

    }

    @Override
    public ICacheLayerDataModelKey setCustomDataModelUniqueKey(String key) {
        this.customDataModelKey = new OnionCacheLayerDataModelKey(key);
        return customDataModelKey;
    }

    @Override
    public String dataModelTableName() {
        return this.hasDataModelClassAnnotation(DynamoDBTable.class).map(a -> ((DynamoDBTable)a).tableName()).orElse(null);
    }


    private Optional<Method> getDataModelAnnotationMethod(Class annotationClazz)
    {
        Method[] methods = this.getClass().getMethods();
        for( Method method : methods ) {

            Annotation annot = method.getAnnotation(annotationClazz);

            if(annot != null)
            {
                return Optional.of(method);
            }
        }

        return Optional.empty();
    }

    private Optional<Annotation> hasDataModelClassAnnotation(Class annotationClazz)
    {
        for( Annotation annotation : this.getClass().getAnnotations() ) {

            if(annotation != null)
            {
                return Optional.of(annotation);
            }
        }

        return Optional.empty();
    }
}
