package com.raqun;

import com.squareup.javapoet.TypeSpec;

/**
 * Created by vincedelricco on 1/22/18.
 */

public interface Generatable {
    String getPackage();
    TypeSpec getTypeSpec();
}
