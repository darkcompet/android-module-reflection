/*
 * Copyright (c) 2017-2020 DarkCompet. All rights reserved.
 */

package tool.compet.reflection;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import tool.compet.core.DkArrays;

/**
 * Find fields or methods which annotated with specified annotation in class.
 *
 * Note: the app maybe enable Proguard, so take care when use `prefixSearchClassPaths`
 * since prefix of 2 class paths after proguarded maybe NOT same prefix even if same in origin packages.
 * For eg,. `class tool.compet.A` and `tool.compet.binding.B` is same prefix `tool.compet`,
 * but NOT same prefix after proguarded (eg,. z4.q.A and b.a.B).
 */
@SuppressWarnings("unused")
public class DkReflectionFinder {
	private static DkReflectionFinder INS;

	// Target class paths which be searched in
	protected final Collection<String> searchPaths;

	// Reflection process is heavy, we provide common-cache for outer modules
	// This is called as common-cache since outer modules maybe has its own cache.
	private static ArrayMap<String, List<Field>> fieldCache;
	private static ArrayMap<String, List<Method>> methodCache;

	private DkReflectionFinder() {
		this.searchPaths = new ArraySet<>();
	}

	public static DkReflectionFinder getIns() {
		if (INS == null) {
			synchronized (DkReflectionFinder.class) {
				if (INS == null) {
					INS = new DkReflectionFinder();
				}
			}
		}
		return INS;
	}

	public static void setFieldCache(String key, List<Field> fields) {
		obtainFieldCache().put(key, fields);
	}

	public static List<Field> getFieldCache(String key) {
		return obtainFieldCache().get(key);
	}

	public static void setMethodCache(String key, List<Method> methods) {
		obtainMethodCache().put(key, methods);
	}

	public static List<Method> getMethodCache(String key) {
		return obtainMethodCache().get(key);
	}

	/**
	 * Find fields which be annotated with given annotation inside target class.
	 * By default, it recursively look up super class too.
	 */
	@NonNull
	public List<Field> findFields(Class<?> targetClass, Class<? extends Annotation> annotation) {
		return findFields(targetClass, annotation, true);
	}

	/**
	 * Search fields inside given class and its super if optioned by reflection.
	 *
	 * @param targetClass Target class.
	 * @param annotation If a field has one of this annotations, then target it.
	 * @param upSuper Search super class if true.
	 *
	 * @return List of fields inside given class and its super (if optioned).
	 */
	@NonNull
	public List<Field> findFields(Class<?> targetClass, Class<? extends Annotation> annotation, boolean upSuper) {
		List<Field> result = new ArrayList<>();
		// Only search when `prefixSearchClassPaths` is NOT provided,
		// or targetClass is inside `prefixSearchClassPaths`
		if (searchPaths.size() == 0 || isInSearchPaths(targetClass)) {
			// Search at target class
			for (Field item : targetClass.getDeclaredFields()) {
				if (item.isAnnotationPresent(annotation)) {
					item.setAccessible(true);
					result.add(item);
				}
			}
			// Recursively search its super
			if (upSuper) {
				Class<?> superClass = targetClass.getSuperclass();
				if (superClass != null) {
					result.addAll(findFields(superClass, annotation, true));
				}
			}
		}
		return result;
	}

	/**
	 * Find methods which be annotated with given annotation inside target class.
	 * By default, it recursively look up super class too.
	 */
	@NonNull
	public List<Method> findMethods(Class<?> targetClass, Class<? extends Annotation> annotation) {
		return findMethods(targetClass, annotation, true);
	}

	/**
	 * Search methods inside given class and its super if optioned by reflection.
	 *
	 * @param targetClass Target class.
	 * @param annotation If a method has one of this annotations, then target it.
	 * @param upSuper Search super class if true.
	 *
	 * @return List of methods inside given class and its super (if optioned).
	 */
	@NonNull
	public List<Method> findMethods(Class<?> targetClass, Class<? extends Annotation> annotation, boolean upSuper) {
		List<Method> result = new ArrayList<>();
		// Only search when `prefixSearchClassPaths` is NOT provided,
		// or targetClass is inside `prefixSearchClassPaths`
		if (searchPaths.size() == 0 || isInSearchPaths(targetClass)) {
			// Search at target class
			for (Method item : targetClass.getDeclaredMethods()) {
				if (item.isAnnotationPresent(annotation)) {
					item.setAccessible(true);
					result.add(item);
				}
			}
			// Recursively search its super
			if (upSuper) {
				Class<?> superClass = targetClass.getSuperclass();
				if (superClass != null) {
					result.addAll(findMethods(superClass, annotation, true));
				}
			}
		}
		return result;
	}

	/**
	 * Add to search-path from given `searchPaths`.
	 * If path `compet.bundle` is given, then it will be added to searchPaths.
	 *
	 * @param searchPaths For eg,. "compet.bundle", "tool.compet",...
	 */
	public void addSearchPaths(String... searchPaths) {
		if (searchPaths != null) {
			this.searchPaths.addAll(DkArrays.asList(searchPaths));
		}
	}

	/**
	 * Add to search-path from given `classes`.
	 * If class `compet.bundle.X.class` is given, then `compet.bundle` will be added to searchPaths.
	 *
	 * @param classes For eg,. compet.bundle.A.class, tool.compet.B.class,...
	 */
	public void addSearchPaths(Class<?>... classes) {
		if (classes != null) {
			for (Class<?> klass : classes) {
				String className = klass.getName();
				int lastDotIndex = className.lastIndexOf('.');

				this.searchPaths.add(className.substring(0, lastDotIndex));
			}
		}
	}

	/**
	 * Check whether given class is inside `prefixSearchClassPaths`.
	 */
	private boolean isInSearchPaths(Class<?> targetClass) {
		for (String pathPrefix : searchPaths) {
			if (targetClass.getName().startsWith(pathPrefix)) {
				return true;
			}
		}
		return false;
	}

	private static String cacheKey(Class<?> targetClass, Class<? extends Annotation> annotation) {
		return targetClass.getName() + "_" + annotation.getName();
	}

	private static ArrayMap<String, List<Field>> obtainFieldCache() {
		if (fieldCache == null) {
			synchronized (DkReflectionFinder.class) {
				if (fieldCache == null) {
					fieldCache = new ArrayMap<>();
				}
			}
		}
		return fieldCache;
	}

	private static ArrayMap<String, List<Method>> obtainMethodCache() {
		if (methodCache == null) {
			synchronized (DkReflectionFinder.class) {
				if (methodCache == null) {
					methodCache = new ArrayMap<>();
				}
			}
		}
		return methodCache;
	}
}
