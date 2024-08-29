/*
 * SPDX-FileCopyrightText: © Vegard IT GmbH (https://vegardit.com) and others.
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Sebastian Thomschke (Vegard IT GmbH) - initial API and implementation
 */
package org.lastnpe.eea.generator.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import io.github.classgraph.AnnotationInfoList;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.FieldInfoList;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.MethodInfoList;

/**
 * @author Sebastian Thomschke (Vegard IT GmbH)
 */
public final class ClassGraphUtils {

	private static final Set<String> NULLABLE_ANNOTATIONS = Set.of( //
			"android.annotation.Nullable", //
			"android.support.annotation.Nullable", //
			"androidx.annotation.Nullable", //
			"com.mongodb.lang.Nullable", //
			"com.sun.istack.internal.Nullable", //
			"edu.umd.cs.findbugs.annotations.Nullable", //
			"io.reactivex.annotations.Nullable", //
			"io.reactivex.rxjava3.annotations.Nullable", //
			"io.smallrye.common.constraint.Nullable", //
			"io.vertx.codegen.annotations.Nullable", //
			"jakarta.annotation.CheckForNull", //
			"jakarta.annotation.Nullable", //
			"javax.annotation.CheckForNull", //
			"javax.annotation.Nullable", //
			"net.bytebuddy.utility.nullability.AlwaysNull", //
			"net.bytebuddy.utility.nullability.MaybeNull", //
			"org.checkerframework.checker.nullness.compatqual.NullableDecl", //
			"org.checkerframework.checker.nullness.compatqual.NullableType", //
			"org.checkerframework.checker.nullness.qual.Nullable", //
			"org.eclipse.jdt.annotation.Nullable", //
			"org.eclipse.sisu.Nullable", //
			"org.jetbrains.annotations.Nullable", //
			"org.jmlspecs.annotation.Nullable", //
			"org.netbeans.api.annotations.common.CheckForNull", //
			"org.netbeans.api.annotations.common.NullAllowed", //
			"org.netbeans.api.annotations.common.NullUnknown", //
			"org.springframework.lang.Nullable", //
			"org.sonatype.inject.Nullable", //
			"org.wildfly.common.annotation.Nullable", //
			"reactor.util.annotation.Nullable");

	private static final Set<String> NONNULL_ANNOTATIONS = Set.of( //
			"android.annotation.NonNull", //
			"android.support.annotation.NonNull", //
			"androidx.annotation.NonNull", //
			"com.sun.istack.internal.NotNull", //
			"com.mongodb.lang.NonNull", //
			"edu.umd.cs.findbugs.annotations.NonNull", //
			"io.reactivex.annotations.NonNull", //
			"io.reactivex.rxjava3.annotations.NonNull", //
			"javax.annotation.Nonnull", //
			"javax.validation.constraints.NotNull", //
			"jakarta.annotation.Nonnull", //
			"jakarta.validation.constraints.NotNull", //
			"lombok.NonNull", //
			"net.bytebuddy.utility.nullability.NeverNull", //
			"org.checkerframework.checker.nullness.compatqual.NonNullDecl", //
			"org.checkerframework.checker.nullness.compatqual.NonNullType", //
			"org.checkerframework.checker.nullness.qual.NonNull", //
			"org.eclipse.jdt.annotation.NonNull", //
			"org.jetbrains.annotations.NotNull", //
			"org.jmlspecs.annotation.NonNull", //
			"org.netbeans.api.annotations.common.NonNull", //
			"org.springframework.lang.NonNull", //
			"org.wildfly.common.annotation.NotNull", //
			"reactor.util.annotation.NonNull");

	public static Set<ClassInfo> getDirectInterfaces(final ClassInfo classInfo) {
		final var directInterfaces = new HashSet<>(classInfo.getInterfaces());
		if (classInfo.isInterface()) {
			classInfo.getInterfaces().forEach(superIFace -> directInterfaces.removeAll(superIFace.getInterfaces()));
		} else {
			ClassInfo superclassInfo = classInfo.getSuperclass();
			while (superclassInfo != null) {
				directInterfaces.removeAll(superclassInfo.getInterfaces());
				superclassInfo = superclassInfo.getSuperclass();
			}
		}
		return directInterfaces;
	}

	/**
	 * @param selectStatic if true static fields are returned otherwise instance
	 *                     fields
	 * @return a sorted set of {@link FieldInfo} instances for all public or
	 *         protected non-synthetic non-primitive fields
	 */
	private static SortedSet<FieldInfo> getFilteredAndSortedFields(final FieldInfoList fields,
			final boolean selectStatic) {
		final var result = new TreeSet<FieldInfo>((f1, f2) -> {
			final int rc = f1.getName().compareTo(f2.getName());
			return rc == 0
					? f1.getTypeSignatureOrTypeDescriptorStr().compareTo(f2.getTypeSignatureOrTypeDescriptorStr())
					: rc;
		});

		for (final var f : fields) {
			if (!f.isSynthetic() //
					&& (selectStatic ? f.isStatic() : !f.isStatic()) //
					&& (f.isProtected() || f.isPublic()) //
					&& (f.getTypeSignatureOrTypeDescriptorStr().contains(";")
							|| f.getTypeSignatureOrTypeDescriptorStr().contains("["))) {
				result.add(f);
			}
		}
		return result;
	}

	/**
	 * @param selectStatic if true static methods are returned otherwise instance
	 *                     methods
	 * @return a sorted set of {@link MethodInfo} instances for all public or
	 *         protected non-synthetic methods with a non-primitive return value or
	 *         at least one non-primitive method parameter
	 */
	private static SortedSet<MethodInfo> getFilteredAndSortedMethods(final MethodInfoList methods,
			final boolean selectStatic) {
		final var result = new TreeSet<MethodInfo>((m1, m2) -> {
			final int rc = m1.getName().compareTo(m2.getName());
			return rc == 0
					? m1.getTypeSignatureOrTypeDescriptorStr().compareTo(m2.getTypeSignatureOrTypeDescriptorStr())
					: rc;
		});

		for (final var m : methods) {
			// omit auto-generated methods of enums as they are always treated as non-null
			// by eclipse compiler
			if (m.getClassInfo().isEnum()) {
				switch (m.getName()) {
				case "values":
					if (m.getParameterInfo().length == 0) {
						continue;
					}
					break;
				case "valueOf":
					if (m.getParameterInfo().length == 1
							&& String.class.getName().equals(m.getParameterInfo()[0].getTypeDescriptor().toString())) {
						continue;
					}
					break;
				}
			}
			if (!m.isSynthetic() //
					&& (selectStatic ? m.isStatic() : !m.isStatic()) //
					&& (m.isProtected() || m.isPublic()) //
					&& (m.getTypeSignatureOrTypeDescriptorStr().contains(";")
							|| m.getTypeSignatureOrTypeDescriptorStr().contains("["))) {
				result.add(m);
			}
		}
		return result;
	}

	public static SortedSet<FieldInfo> getInstanceFields(final FieldInfoList fields) {
		return getFilteredAndSortedFields(fields, false);
	}

	public static SortedSet<MethodInfo> getInstanceMethods(final MethodInfoList methods) {
		return getFilteredAndSortedMethods(methods, false);
	}

	public static SortedSet<FieldInfo> getStaticFields(final FieldInfoList fields) {
		return getFilteredAndSortedFields(fields, true);
	}

	public static SortedSet<MethodInfo> getStaticMethods(final MethodInfoList methods) {
		return getFilteredAndSortedMethods(methods, true);
	}

	public static boolean hasNonNullAnnotation(final AnnotationInfoList annos) {
		return annos.stream().anyMatch(a -> NONNULL_ANNOTATIONS.contains(a.getName()));
	}

	public static boolean hasNullableAnnotation(final AnnotationInfoList annos) {
		return annos.stream().anyMatch(a -> NULLABLE_ANNOTATIONS.contains(a.getName()));
	}

	public static boolean hasSuperclass(final ClassInfo classInfo, final String superClassName) {
		return !classInfo.getSuperclasses().filter(c -> c.getName().equals(superClassName)).isEmpty();
	}

	public static boolean hasPackageVisibility(final ClassInfo classInfo) {
		return !classInfo.isPublic() && !classInfo.isPrivate() && !classInfo.isProtected();
	}

	public static boolean isStaticField(final ClassInfo classInfo, final String fieldName) {
		final var fieldInfo = classInfo.getDeclaredFieldInfo(fieldName);
		if (fieldInfo == null)
			return false;
		return fieldInfo.isStatic();
	}

	public static boolean isStaticMethod(final ClassInfo classInfo, final String methodName,
			final String methodSignature) {
		return classInfo.getDeclaredMethodInfo(methodName).stream() //
				.anyMatch(methodInfo -> methodInfo.isStatic() //
						&& methodSignature.equals(methodInfo.getTypeSignatureOrTypeDescriptorStr()));
	}

	public static boolean isThrowable(final ClassInfo classInfo) {
		return hasSuperclass(classInfo, "java.lang.Throwable");
	}

	private ClassGraphUtils() {
	}
}
