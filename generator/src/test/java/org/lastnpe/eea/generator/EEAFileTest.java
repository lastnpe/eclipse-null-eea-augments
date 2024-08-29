/*
 * SPDX-FileCopyrightText: © Vegard IT GmbH (https://vegardit.com) and contributors.
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Sebastian Thomschke (Vegard IT GmbH) - initial API and implementation
 */
package org.lastnpe.eea.generator;

import static org.assertj.core.api.Assertions.*;
import static org.lastnpe.eea.generator.EEAFile.*;
import static org.lastnpe.eea.generator.internal.MiscUtils.remap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.lastnpe.eea.generator.EEAFile.SaveOption;

/**
 * @author Sebastian Thomschke (Vegard IT GmbH)
 */
@SuppressWarnings("null")
class EEAFileTest {

	public static final class TestEntity {
		public static final String STATIC_STRING = "MyStaticString";

		public String name;

		public TestEntity(final String name) { // CHECKSTYLE:IGNORE RedundantModifier
			this.name = name;
		}
	}

	public static final class TestEntity2 {
		public static final String STATIC_STRING = "MyStaticString";
	}

	static final String TEST_ENTITY_NAME_WITH_SLASHES = TestEntity.class.getName().replace('.', '/');
	static final String WRONG_TYPE_NAME = EEAFileTest.class.getName() + "$WrongType";
	static final String WRONG_TYPE_NAME_WITH_SLASHES = WRONG_TYPE_NAME.replace('.', '/');

	@Test
	void testEEAFile() throws IOException {
		final var eeaFile = load(Path.of("src/test/resources/valid"), TestEntity.class.getName());

		assertThat(eeaFile.classHeader.name.value).isEqualTo(TestEntity.class.getName());
		assertThat(eeaFile.classHeader.name.comment).isEqualTo("# a class comment");
		assertThat(eeaFile.classHeader.name).hasToString(TestEntity.class.getName() + " # a class comment");
		assertThat(eeaFile.relativePath).isEqualTo(Path.of(TEST_ENTITY_NAME_WITH_SLASHES + ".eea"));

		assertThat(eeaFile.getClassMembers()).isNotEmpty();
		final var field = eeaFile.findMatchingClassMember("STATIC_STRING", "Ljava/lang/String;");
		assert field != null;
		assertThat(field.name.comment).isEqualTo("# a field comment");
		assertThat(field.originalSignature.value).isEqualTo("Ljava/lang/String;");
		assertThat(field.originalSignature.comment).isEqualTo("# an original signature comment");

		final var annotatedSignature = field.annotatedSignature;
		assertThat(annotatedSignature).isNotNull();
		assert annotatedSignature != null;

		assertThat(annotatedSignature.value).isEqualTo("L1java/lang/String;");
		assertThat(annotatedSignature.comment).isEqualTo("# an annotated signature comment");

		assertThat(eeaFile.renderFileContent(Set.of())) //
				.isEqualTo(Files.readAllLines(Path.of("src/test/resources/valid").resolve(eeaFile.relativePath)));

		assertThat(eeaFile.renderFileContent(Set.of(SaveOption.OMIT_REDUNDANT_ANNOTATED_SIGNATURES))) //
				.isNotEqualTo(Files.readAllLines(Path.of("src/test/resources/valid").resolve(eeaFile.relativePath)));
	}

	@Test
	void testWrongTypeHeader() {
		final var wrongTypePath = Path.of("src/test/resources/invalid/" + WRONG_TYPE_NAME_WITH_SLASHES + ".eea");
		assertThatThrownBy(() -> { //
			load(wrongTypePath);
		}) //
				.isInstanceOf(java.io.IOException.class) //
				.hasMessage("Mismatch between file path of [" + wrongTypePath
						+ "] and contained class name definition [" + TestEntity.class.getName() + "]");
	}

	@Test
	void testApplyAnnotationsAndCommentsFrom() throws IOException {
		final var computedEEAFiles = remap(EEAGenerator.computeEEAFiles(EEAFileTest.class.getPackageName(), c -> true),
				v -> v.relativePath);
		final var computedEEAFile = computedEEAFiles.get(Path.of(TEST_ENTITY_NAME_WITH_SLASHES + ".eea"));
		assertThat(computedEEAFile).isNotNull();
		assert computedEEAFile != null;

		final var method = computedEEAFile.findMatchingClassMember("name", "Ljava/lang/String;");
		assert method != null;
		assertThat(method.hasNullAnnotations()).isFalse();
		assertThat(method.name.comment).isEmpty();

		final var loadedEEAFile = load(Path.of("src/test/resources/valid"), computedEEAFile.classHeader.name.value);
		computedEEAFile.applyAnnotationsAndCommentsFrom(loadedEEAFile, false, false);
		final var annotatedSignature = method.annotatedSignature;
		assert annotatedSignature != null;
		assertThat(annotatedSignature.value).isEqualTo("L1java/lang/String;");
		assertThat(method.name.comment).isEqualTo("# a method comment");
	}

	@Test
	void testRemoveNullAnnotations() {
		assertThat(removeNullAnnotations("L0java/lang/Object;")).isEqualTo("Ljava/lang/Object;");
		assertThat(removeNullAnnotations("L1java/lang/Class<*>;L1java/lang/Class<*>;)L1java/lang/invoke/MethodType;"))
				.isEqualTo("Ljava/lang/Class<*>;Ljava/lang/Class<*>;)Ljava/lang/invoke/MethodType;");
		assertThat(removeNullAnnotations("<T::Ljava/lang/annotation/Annotation;>(L1java/lang/Class<TT;>;)[1T1T;"))
				.isEqualTo("<T::Ljava/lang/annotation/Annotation;>(Ljava/lang/Class<TT;>;)[TT;");
		assertThat(removeNullAnnotations("<1T::Ljava/util/EventListener;>(TT;)V"))
				.isEqualTo("<T::Ljava/util/EventListener;>(TT;)V");

		// T1 is the name of the generic variable
		assertThat(removeNullAnnotations("<T1:Ljava/lang/Object;>")).isEqualTo("<T1:Ljava/lang/Object;>");
	}
}
