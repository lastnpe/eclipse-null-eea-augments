/*
 * SPDX-FileCopyrightText: © Vegard IT GmbH (https://vegardit.com) and contributors.
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Sebastian Thomschke (Vegard IT GmbH) - initial API and implementation
 */
package org.lastnpe.eea.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.lastnpe.eea.generator.internal.MiscUtils.remap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * @author Sebastian Thomschke (Vegard IT GmbH)
 */
class EEAFileInheritanceTest {

	public static class A0 {
	}

	public static class A<T extends Map<?, ?>> {
	}

	public static class B extends A<HashMap<?, ?>> {
	}

	public static class C<T extends Number> extends A<HashMap<?, ?>> {
	}

	public static class D<T extends ConcurrentMap<?, ?>> extends A<T> {
	}

	public interface I<T extends List<?>> {
	}

	public class J implements I<ArrayList<?>> {
	}

	public abstract class K extends Throwable implements I<ArrayList<?>>, Comparable<Number> {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void testGenericInheritance() {
		final var computedEEAFiles = EEAGenerator.computeEEAFiles(EEAFileInheritanceTest.class.getPackageName(),
				c -> true);
		final var classInfoByClassName = remap(computedEEAFiles, (k, v) -> v.classHeader.name.value, (k, v) -> k);
		final var computedEEAFilesByClassName = remap(computedEEAFiles, v -> v.classHeader.name.value);

		final var classInfoA = classInfoByClassName.get(A.class.getName());
		final var classInfoB = classInfoByClassName.get(B.class.getName());
		final var classInfoC = classInfoByClassName.get(C.class.getName());
		final var classInfoD = classInfoByClassName.get(D.class.getName());
		final var classInfoI = classInfoByClassName.get(I.class.getName());
		final var classInfoJ = classInfoByClassName.get(J.class.getName());
		final var classInfoK = classInfoByClassName.get(K.class.getName());
		assert classInfoA != null;
		assert classInfoB != null;
		assert classInfoC != null;
		assert classInfoD != null;
		assert classInfoI != null;
		assert classInfoJ != null;
		assert classInfoK != null;

		assertThat(classInfoA.getTypeSignatureStr()) //
				.isEqualTo("<T::Ljava/util/Map<**>;>Ljava/lang/Object;");
		assertThat(classInfoB.getTypeSignatureStr()) //
				.isEqualTo("Lorg/lastnpe/eea/generator/EEAFileInheritanceTest$A<Ljava/util/HashMap<**>;>;");
		assertThat(classInfoC.getTypeSignatureStr()) //
				.isEqualTo(
						"<T:Ljava/lang/Number;>Lorg/lastnpe/eea/generator/EEAFileInheritanceTest$A<Ljava/util/HashMap<**>;>;");
		assertThat(classInfoD.getTypeSignatureStr()) //
				.isEqualTo(
						"<T::Ljava/util/concurrent/ConcurrentMap<**>;>Lorg/lastnpe/eea/generator/EEAFileInheritanceTest$A<TT;>;");
		assertThat(classInfoI.getTypeSignatureStr()) //
				.isEqualTo("<T::Ljava/util/List<*>;>Ljava/lang/Object;");
		assertThat(classInfoJ.getTypeSignatureStr()) //
				.isEqualTo(
						"Ljava/lang/Object;Lorg/lastnpe/eea/generator/EEAFileInheritanceTest$I<Ljava/util/ArrayList<*>;>;");
		assertThat(classInfoK.getTypeSignatureStr()) //
				.isEqualTo(
						"Ljava/lang/Throwable;Lorg/lastnpe/eea/generator/EEAFileInheritanceTest$I<Ljava/util/ArrayList<*>;>;"
								+ "Ljava/lang/Comparable<Ljava/lang/Number;>;");

		final var eeaFileA = computedEEAFilesByClassName.get(A.class.getName());
		final var eeaFileB = computedEEAFilesByClassName.get(B.class.getName());
		final var eeaFileC = computedEEAFilesByClassName.get(C.class.getName());
		final var eeaFileD = computedEEAFilesByClassName.get(D.class.getName());
		final var eeaFileI = computedEEAFilesByClassName.get(I.class.getName());
		final var eeaFileJ = computedEEAFilesByClassName.get(J.class.getName());
		final var eeaFileK = computedEEAFilesByClassName.get(K.class.getName());
		assert eeaFileA != null;
		assert eeaFileB != null;
		assert eeaFileC != null;
		assert eeaFileD != null;
		assert eeaFileI != null;
		assert eeaFileJ != null;
		assert eeaFileK != null;

		final var linesA = eeaFileA.renderFileContent(Set.of()).stream().collect(Collectors.joining("\n"));
		final var linesB = eeaFileB.renderFileContent(Set.of()).stream().collect(Collectors.joining("\n"));
		final var linesC = eeaFileC.renderFileContent(Set.of()).stream().collect(Collectors.joining("\n"));
		final var linesD = eeaFileD.renderFileContent(Set.of()).stream().collect(Collectors.joining("\n"));
		final var linesI = eeaFileI.renderFileContent(Set.of()).stream().collect(Collectors.joining("\n"));
		final var linesJ = eeaFileJ.renderFileContent(Set.of()).stream().collect(Collectors.joining("\n"));
		final var linesK = eeaFileK.renderFileContent(Set.of()).stream().collect(Collectors.joining("\n"));

		assertThat(linesA).isEqualTo(List.of( //
				"class org/lastnpe/eea/generator/EEAFileInheritanceTest$A", //
				" <T::Ljava/util/Map<**>;>", //
				" <T::Ljava/util/Map<**>;>" //
		).stream().collect(Collectors.joining("\n")));

		assertThat(linesB).isEqualTo(List.of( //
				"class org/lastnpe/eea/generator/EEAFileInheritanceTest$B", //
				"", //
				"super org/lastnpe/eea/generator/EEAFileInheritanceTest$A", //
				" <Ljava/util/HashMap<**>;>", //
				" <Ljava/util/HashMap<**>;>" //
		).stream().collect(Collectors.joining("\n")));

		assertThat(linesC).isEqualTo(List.of( //
				"class org/lastnpe/eea/generator/EEAFileInheritanceTest$C", //
				" <T:Ljava/lang/Number;>", //
				" <T:Ljava/lang/Number;>", //
				"", //
				"super org/lastnpe/eea/generator/EEAFileInheritanceTest$A", //
				" <Ljava/util/HashMap<**>;>", //
				" <Ljava/util/HashMap<**>;>" //
		).stream().collect(Collectors.joining("\n")));

		assertThat(linesD).isEqualTo(List.of( //
				"class org/lastnpe/eea/generator/EEAFileInheritanceTest$D", //
				" <T::Ljava/util/concurrent/ConcurrentMap<**>;>", //
				" <T::Ljava/util/concurrent/ConcurrentMap<**>;>", //
				"", //
				"super org/lastnpe/eea/generator/EEAFileInheritanceTest$A", //
				" <TT;>", //
				" <TT;>" //
		).stream().collect(Collectors.joining("\n")));

		assertThat(linesI).isEqualTo(List.of( //
				"class org/lastnpe/eea/generator/EEAFileInheritanceTest$I", //
				" <T::Ljava/util/List<*>;>", //
				" <T::Ljava/util/List<*>;>" //
		).stream().collect(Collectors.joining("\n")));

		assertThat(linesJ).startsWith(List.of( //
				"class org/lastnpe/eea/generator/EEAFileInheritanceTest$J", //
				"", //
				"super org/lastnpe/eea/generator/EEAFileInheritanceTest$I", //
				" <Ljava/util/ArrayList<*>;>", //
				" <Ljava/util/ArrayList<*>;>" //
		).stream().collect(Collectors.joining("\n")));

		assertThat(linesK).startsWith(List.of( //
				"class org/lastnpe/eea/generator/EEAFileInheritanceTest$K", //
				"", //
				"super org/lastnpe/eea/generator/EEAFileInheritanceTest$I", //
				" <Ljava/util/ArrayList<*>;>", //
				" <Ljava/util/ArrayList<*>;>", //
				"super java/lang/Comparable", //
				" <Ljava/lang/Number;>", //
				" <Ljava/lang/Number;>" //
		).stream().collect(Collectors.joining("\n")));
	}
}
