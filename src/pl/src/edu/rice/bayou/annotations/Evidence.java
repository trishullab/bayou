package edu.rice.bayou.annotations;

import java.lang.annotation.Repeatable;

public @Repeatable(Evidences.class) @interface Evidence {
    String keywords() default "";
    String[] sequence() default {};
}
