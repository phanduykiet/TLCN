package com.sc.scifunapi.util;

import org.bson.types.ObjectId;

public class MongoIdUtil {
    private MongoIdUtil() {}

    public static boolean isValidObjectId(String id) {
        return id != null && ObjectId.isValid(id);
    }
}
