package org.spongepowered.asm.mixin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Mixin { Class<?>[] value(); }
