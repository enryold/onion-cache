package com.enryold.onioncache.models;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;


/**
 * Created by enryold on 17/01/17.
 */
@DynamoDBTable(tableName = Person.Attributes.TABLE_NAME)
public class Person extends DynamoDbDataModel
{
    public static class Attributes {
        public static final String TABLE_NAME = "person";
        public static final String HASH_KEY = "name";
        public static final String RANGE_KEY = "surname";
    }

    private String name;
    private String surname;

    public Person() {}

    public Person(String name, String surname)
    {
        this.name = name;
        this.surname = surname;
    }


    @DynamoDBHashKey(attributeName = Attributes.HASH_KEY)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @DynamoDBRangeKey(attributeName = Attributes.RANGE_KEY)
    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }



}
