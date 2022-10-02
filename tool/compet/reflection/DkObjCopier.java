/*
 * Copyright (c) 2017-2020 DarkCompet. All rights reserved.
 */

package tool.compet.reflection;

import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import tool.compet.core.DkCaller3;
import tool.compet.core.DkLogs;
import tool.compet.core.DkUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Copy fields from src-obj to dst-obj by reflect each field annotation.
 */
public class DkObjCopier {
	public static void copy(Object src, Object dst) {
		copy(src, dst, null, true, null);
	}

	public static void copy(Object src, Object dst, Class<? extends Annotation> annoClass) {
		copy(src, dst, annoClass, true, null);
	}

	public static void copy(Object src, Object dst, Class<? extends Annotation> annoClass, boolean upSuper) {
		copy(src, dst, annoClass, upSuper, null);
	}

	/**
	 * Copy all fields which be annotated with given annotation from src object to dst object.
	 *
	 * @param src From object.
	 * @param dst To object.
	 * @param annotation Annotation which be assigned to each copy-target field to mark them as target of copy.
	 * Null value means all fields are target for copy.
	 * @param upSuper True to include super fields, False to exclude super fields.
	 * @param filterCallback Filter before perform copy, return `true` to allow copy, `false` to ignore copy.
	 * This callback give back 3 params: field_name, src_value, dst_value to caller respectively.
	 */
	public static void copy(Object src, Object dst, @Nullable Class<? extends Annotation> annotation,
		boolean upSuper, @Nullable DkCaller3<String, Object, Object, Boolean> filterCallback) {

		DkReflectionFinder finder = DkReflectionFinder.getIns();
		List<Field> srcFields = finder.findFields(src.getClass(), annotation, upSuper);
		List<Field> dstFields = finder.findFields(dst.getClass(), annotation, upSuper);

		ArrayMap<String, Field> srcFieldMap = new ArrayMap<>();
		ArrayMap<String, Field> dstFieldMap = new ArrayMap<>();

		for (Field f : srcFields) {
			srcFieldMap.put(f.getName(), f);
		}
		for (Field f : dstFields) {
			dstFieldMap.put(f.getName(), f);
		}

		final int N = srcFieldMap.size();

		if (N != dstFieldMap.size()) {
			DkLogs.warning(DkObjCopier.class, "Different number of copy fields between %s(%d) vs %s(%d)",
				src.getClass().getName(), srcFields.size(), dst.getClass().getName(), dstFields.size());
		}

		for (int index = N - 1; index >= 0; --index) {
			String fieldName = srcFieldMap.keyAt(index);

			Field srcField = srcFieldMap.valueAt(index);
			Field dstField = dstFieldMap.get(fieldName);

			if (dstField != null) {
				try {
					if (! srcField.isAccessible()) {
						srcField.setAccessible(true);
					}
					if (! dstField.isAccessible()) {
						dstField.setAccessible(true);
					}

					Object srcFieldValue = srcField.get(src);
					Object dstFieldValue = dstField.get(dst);

					// Ask filter-callback, if ok (true) then perform copy
					if (filterCallback == null || filterCallback.call(fieldName, srcFieldValue, dstFieldValue)) {
						dstField.set(dst, srcFieldValue);
					}
				}
				catch (Exception e) {
					DkLogs.error(DkObjCopier.class, e);
					DkUtils.complainAt(DkObjCopier.class, "Could not copy field: %s.%s -> %s.%s",
						src.getClass().getName(), srcField.getName(), dst.getClass().getName(), dstField.getName());
				}
			}
		}
	}
}
