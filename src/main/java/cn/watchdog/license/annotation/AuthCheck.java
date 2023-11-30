package cn.watchdog.license.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

	/**
	 * 有任何一个权限
	 */
	String[] any() default "";

	/**
	 * 必须有某些权限
	 */
	String[] must() default "";

}

