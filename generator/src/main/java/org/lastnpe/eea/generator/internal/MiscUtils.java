/*
 * SPDX-FileCopyrightText: © Vegard IT GmbH (https://vegardit.com) and others.
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Sebastian Thomschke (Vegard IT GmbH) - initial API and implementation
 */
package org.lastnpe.eea.generator.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.lastnpe.eea.generator.EEAGenerator;

/**
 * @author Sebastian Thomschke (Vegard IT GmbH)
 */
public final class MiscUtils {

	@FunctionalInterface
	public interface ThrowingConsumer<V, T extends Exception> {
		void accept(V v) throws T;
	}

	public static boolean arrayContains(final Object @Nullable [] searchIn, final Object searchFor) {
		if (searchIn == null || searchIn.length == 0)
			return false;
		for (final var e : searchIn) {
			if (Objects.equals(e, searchFor))
				return true;
		}
		return false;
	}

	private static boolean isJULConfigured = false;

	public static boolean contains(final @Nullable String searchIn, final String searchFor) {
		return searchIn != null && searchIn.contains(searchFor);
	}

	public static void configureJUL() {
		if (isJULConfigured)
			return;
		final var mainLogger = Logger.getLogger(EEAGenerator.class.getPackageName());
		mainLogger.setUseParentHandlers(false);
		final var handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter() {
			@Override
			public synchronized String format(final LogRecord lr) {
				var sourceClassName = lr.getSourceClassName();
				if (sourceClassName != null) {
					sourceClassName = sourceClassName.substring(sourceClassName.lastIndexOf('.') + 1);
				}
				var msg = lr.getMessage();
				if (msg != null) {
					msg = MessageFormat.format(msg, lr.getParameters());
				}
				return String.format("[%1$s] %2$s | %3$s %n", lr.getLevel().getLocalizedName(), sourceClassName, msg);
			}
		});
		mainLogger.addHandler(handler);
		isJULConfigured = true;
	}

	public static BufferedReader getUTF8ResourceAsReader(final Class<?> clazz, final String resourceName) {
		final var is = clazz.getResourceAsStream(resourceName);
		if (is == null)
			throw new IllegalArgumentException("Resource not found: " + resourceName);
		return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
	}

	public static <E> @Nullable E findLastElement(final List<E> list) {
		if (list.isEmpty())
			return null;
		return list.get(list.size() - 1);
	}

	/**
	 * @return total number of matching files
	 */
	public static long forEachFileWithExtension(final Path startPath, final String fileExtension,
			final ThrowingConsumer<Path, IOException> onFile) throws IOException {
		if (!Files.exists(startPath))
			return 0;

		final long[] count = { 0 };
		try (Stream<Path> paths = Files.walk(startPath)) {
			paths //
					.filter(Files::isRegularFile) //
					.filter(path -> path.getFileName().toString().endsWith(fileExtension)) //
					.forEach(path -> {
						try {
							count[0]++;
							onFile.accept(path);
						} catch (final IOException ex) {
							throw new UncheckedIOException(ex);
						}
					});
		} catch (final UncheckedIOException ex) {
			throw ex.getCause();
		}
		return count[0];
	}

	/**
	 * Replaces the given capturing group of all matches.
	 */
	public static String replaceAll(final String searchIn, final Pattern searchFor, final int groupToReplace,
			final UnaryOperator<String> replaceWith) {
		if (searchIn.isEmpty())
			return searchIn;
		final var matcher = searchFor.matcher(searchIn);
		int lastPos = 0;
		final var sb = new StringBuilder();
		while (matcher.find()) {
			final var start = matcher.start(groupToReplace);
			sb.append(searchIn.substring(lastPos, start));
			final var textToReplace = matcher.group(groupToReplace);
			assert textToReplace != null;
			sb.append(replaceWith.apply(textToReplace));
			lastPos = matcher.end(groupToReplace);
		}
		if (lastPos == 0)
			return searchIn;

		sb.append(searchIn.substring(lastPos));
		return sb.toString();
	}

	public static String removeSuffix(final String searchIn, final String remove) {
		return !remove.isEmpty() && searchIn.endsWith(remove) //
				? searchIn.substring(0, searchIn.length() - remove.length())
				: searchIn;
	}

	public static boolean startsWith(@Nullable final String searchIn, final String searchFor) {
		if (searchIn == null)
			return false;
		return searchIn.startsWith(searchFor);
	}

	public static @Nullable String substringBetweenBalanced(final String searchIn, final char startDelimiter,
			final char endDelimiter) {
		int depth = 0;
		int lastStartDelimIdx = -1;
		for (int i = 0, l = searchIn.length(); i < l; i++) {
			final char c = searchIn.charAt(i);
			if (c == startDelimiter) {
				depth++;
				if (depth == 1) {
					lastStartDelimIdx = i + 1;
				}
			} else if (c == endDelimiter) {
				if (depth == 1)
					return searchIn.substring(lastStartDelimIdx, i);
				if (depth > 0) {
					depth--;
				}
			}
		}
		return null;
	}

	public static int countOccurrences(final CharSequence searchIn, final char searchFor) {
		return (int) searchIn.chars() //
				.filter(ch -> ch == searchFor) //
				.count();
	}

	public static <T extends Throwable> @Nullable T sanitizeStackTraces(final @Nullable T ex) {
		if (ex == null)
			return null;

		final var stacktrace = ex.getStackTrace();
		if (stacktrace.length < 3)
			return ex;

		final var sanitized = new ArrayList<StackTraceElement>(stacktrace.length - 2);
		// we leave the first two elements untouched to keep the context
		sanitized.add(stacktrace[0]);
		sanitized.add(stacktrace[1]);
		for (int i = 2, l = stacktrace.length; i < l; i++) {
			final StackTraceElement ste = stacktrace[i];
			final String className = ste.getClassName();
			if ("java.lang.reflect.Method".equals(className) //
					|| className.startsWith("java.util.stream.") //
					|| "java.util.Iterator".equals(className) && "forEachRemaining".equals(ste.getMethodName())//
					|| "java.util.Spliterators$IteratorSpliterator".equals(className)
							&& "forEachRemaining".equals(ste.getMethodName())//
					|| className.startsWith("sun.reflect.") //
					|| className.startsWith("sun.proxy.$Proxy", 4) //
					|| className.startsWith("org.codehaus.groovy.runtime.") //
					|| className.startsWith("org.codehaus.groovy.reflection.") //
					|| className.startsWith("groovy.lang.Meta") //
					|| className.startsWith("groovy.lang.Closure") //
			) {
				continue;
			}
			sanitized.add(ste);
		}

		@SuppressWarnings("null")
		final @NonNull StackTraceElement[] arr = sanitized.toArray(StackTraceElement[]::new);
		ex.setStackTrace(arr);
		if (ex.getCause() != null) {
			sanitizeStackTraces(ex.getCause());
		}
		return ex;
	}

	public static String insert(final String str, final int pos, final String insertion) {
		return str.substring(0, pos) + insertion + str.substring(pos);
	}

	public static <K, V> Map<K, V> remap(final Map<?, V> map, final Function<V, K> keyMapper) {
		final var newMap = new HashMap<K, V>();
		for (final V v : map.values()) {
			newMap.put(keyMapper.apply(v), v);
		}
		return newMap;
	}

	public static <K, KK, V, VV> Map<KK, VV> remap(final Map<K, V> map, final BiFunction<K, V, KK> keyMapper,
			final BiFunction<K, V, VV> valueMapper) {
		final var newMap = new HashMap<KK, VV>();
		for (final var e : map.entrySet()) {
			newMap.put(keyMapper.apply(e.getKey(), e.getValue()), valueMapper.apply(e.getKey(), e.getValue()));
		}
		return newMap;
	}

	public static void removeTrailingBlanks(final List<String> strings) {
		int i = strings.size() - 1;
		while (i >= 0 && strings.get(i).isBlank()) {
			strings.remove(i);
			i--;
		}
	}

	private MiscUtils() {
	}
}
