/*
 * SPDX-FileCopyrightText: © Vegard IT GmbH (https://vegardit.com) and others.
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Sebastian Thomschke (Vegard IT GmbH) - initial API and implementation
 */
package org.lastnpe.eea.generator;

import static org.lastnpe.eea.generator.internal.MiscUtils.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationProvider;
import org.lastnpe.eea.generator.internal.MiscUtils;

/**
 * Represents an .eea file. See
 * https://wiki.eclipse.org/JDT_Core/Null_Analysis/External_Annotations#File_layout
 *
 * @author Sebastian Thomschke (Vegard IT GmbH)
 */
public class EEAFile {

	/**
	 * Represents a type/class member, e.g. a field or method incl. it's original
	 * signature.
	 */
	public static class ClassMember {

		public enum Type {
			FIELD, //
			CONSTRUCTOR, //
			METHOD
		}

		public boolean isFollowedByEmptyLine = false;
		public final ValueWithComment name;

		/** Plain signature without external Null Analysis annotations */
		public final ValueWithComment originalSignature;

		/** Signature with external Null Analysis annotations */
		public ValueWithComment annotatedSignature;

		public ClassMember(final String name, final String originalSignature) {
			this(ValueWithComment.parse(name), ValueWithComment.parse(originalSignature));
		}

		public ClassMember(final ValueWithComment name, final ValueWithComment originalSignature) {
			this.name = name;
			this.originalSignature = originalSignature;
			annotatedSignature = new ValueWithComment(originalSignature.value);
		}

		public ClassMember(final ValueWithComment name, final ValueWithComment originalSignature,
				final ValueWithComment annotatedSignature) {
			this.name = name;
			this.originalSignature = originalSignature;
			this.annotatedSignature = annotatedSignature;
		}

		@Override
		public ClassMember clone() {
			return new ClassMember(name.clone(), originalSignature.clone(), annotatedSignature.clone());
		}

		public boolean hasNullAnnotations() {
			return !annotatedSignature.value.equals(originalSignature.value);
		}

		public Type getType() {
			if (name.value.equals("<init>"))
				return Type.CONSTRUCTOR;
			return originalSignature.value.contains("(") //
					? Type.METHOD
					: Type.FIELD;
		}

		public void applyAnnotationsAndCommentsFrom(final ClassMember applyFrom, final boolean overrideOnConflict) {
			if (applyFrom.getType() != getType())
				throw new IllegalArgumentException("Type mismatch for [" + name.value + "]:\n" //
						+ "  Ours: " + getType() + "\n" //
						+ "Theirs: " + applyFrom.getType());

			if (!applyFrom.originalSignature.value.equals(originalSignature.value)) {
				LOG.log(Level.WARNING, "Signature mismatch for " + getType() + "[" + name.value + "]:\n" //
						+ "  Ours: " + originalSignature.value + "\n" //
						+ "Theirs: " + applyFrom.originalSignature.value);
				return;
			}

			// apply name comment
			if (overrideOnConflict || !name.hasComment()) {
				name.comment = applyFrom.name.comment;
			}

			// apply original signature comment
			if (overrideOnConflict || !originalSignature.hasComment()) {
				originalSignature.comment = applyFrom.originalSignature.comment;
			}

			if (overrideOnConflict || !hasNullAnnotations()) {
				if (applyFrom.hasNullAnnotations()) {
					annotatedSignature = applyFrom.annotatedSignature.clone();
				} else if (overrideOnConflict || !annotatedSignature.hasComment()) {
					annotatedSignature.comment = applyFrom.annotatedSignature.comment;
				}
			}
		}

		@Override
		public String toString() {
			return name + System.lineSeparator() //
					+ " " + originalSignature + System.lineSeparator() //
					+ " " + annotatedSignature;
		}
	}

	public enum SaveOption {
		DELETE_IF_EMPTY, //
		OMIT_COMMENTS, //
		OMIT_EMPTY_LINES, //
		OMIT_REDUNDANT_ANNOTATED_SIGNATURES, //
		OMIT_MEMBERS_WITH_INHERITED_ANNOTATED_SIGNATURES, //
		OMIT_MEMBERS_WITHOUT_ANNOTATED_SIGNATURE, //
		REPLACE_EXISTING, //
		QUIET
	}

	public static final class ValueWithComment implements Cloneable {
		public static final char COMMENT_SEPARATOR = ' ';

		public static ValueWithComment parse(String text) {
			text = text.strip();
			final int separatorPos = text.indexOf(COMMENT_SEPARATOR);
			if (separatorPos == -1)
				return new ValueWithComment(text);
			return new ValueWithComment(text.substring(0, separatorPos), text.substring(separatorPos + 1));
		}

		public static String toString(final String value, final String comment) {
			return comment.isBlank() ? value : value + COMMENT_SEPARATOR + comment;
		}

		public String value;
		public String comment;

		public ValueWithComment(final String value) {
			this(value, "");
		}

		public ValueWithComment(final String value, final String comment) {
			this.value = value;
			this.comment = comment;
		}

		@Override
		public ValueWithComment clone() {
			return new ValueWithComment(value, comment);
		}

		public boolean hasComment() {
			return !comment.isBlank();
		}

		@Override
		public String toString() {
			return toString(value, comment);
		}

		public String toString(final boolean omitComment) {
			return toString(value, omitComment ? "" : comment);
		}
	}

	private static final Logger LOG = System.getLogger(EEAFile.class.getName());

	public static final String MARKER_KEEP = "@Keep";
	public static final String MARKER_OVERRIDES = "@Overrides";
	public static final String MARKER_INHERITED = "@Inherited";

	/**
	 * Used to match the 0/1 null annotation of types generic type variables, which
	 * is especially tricky in cases such as
	 *
	 * <pre>
	 * {@code
	 * (L1com/example/L1;)V
	 * <T1:Ljava/lang/Object;>(Ljava/lang/Class<T1T1;>)V
	 * }
	 * </pre>
	 *
	 * where the name of the type variable itself is T0 or T1 or when the class name
	 * itself is L0 or L1.
	 */
	protected static final Pattern PATTERN_CAPTURE_NULL_ANNOTATION_OF_TYPENAMES = Pattern
			.compile("[TL]([01])[a-zA-Z_][a-zA-Z_0-9$\\/*]*[<;]");

	/**
	 * see
	 * https://wiki.eclipse.org/JDT_Core/Null_Analysis/External_Annotations#Textual_encoding_of_signatures
	 */
	protected static String removeNullAnnotations(final String annotatedSignature) {
		var strippedSignature = annotatedSignature //
				.replace("[0", "[") //
				.replace("[1", "[") //
				.replace("-0", "-") //
				.replace("-1", "-") //
				.replace("+0", "+") //
				.replace("+1", "+") //
				.replace("*0", "*") //
				.replace("<0", "<") //
				.replace("<1", "<") //
				.replace("*1", "*");

		strippedSignature = replaceAll(strippedSignature, PATTERN_CAPTURE_NULL_ANNOTATION_OF_TYPENAMES, 1, match -> "");
		return strippedSignature;
	}

	public final ClassMember classHeader;
	public final List<ClassMember> superTypes = new ArrayList<>();

	public final Path relativePath;

	/** ordered list of declared class members */
	private final List<ClassMember> members = new ArrayList<>();

	/**
	 * @throws IOException in case the file cannot be read or contains syntax errors
	 */
	public static @Nullable EEAFile loadIfExists(final Path rootPath, final String className) throws IOException {
		final Path eeaFilePath = rootPath.resolve(classNameToRelativePath(className));
		if (!Files.exists(eeaFilePath)) {
			LOG.log(Level.DEBUG, "File [{0}] does not exist, skipping.", eeaFilePath);
			return null;
		}
		return load(eeaFilePath);
	}

	/**
	 * @throws IOException in case the file cannot be read or contains syntax errors
	 */
	public static EEAFile load(final Path rootPath, final String className) throws IOException {
		final Path eeaFilePath = rootPath.resolve(classNameToRelativePath(className));
		return load(eeaFilePath);
	}

	/**
	 * @throws IOException in case the file cannot be read or contains syntax errors
	 */
	public static EEAFile load(final Path filePath) throws IOException {
		try (var reader = Files.newBufferedReader(filePath)) {
			return load(reader, filePath.toString());
		}
	}

	public static EEAFile load(final BufferedReader reader, final String path) throws IOException {
		final Deque<String> lines = reader.lines().collect(Collectors.toCollection(ArrayDeque::new));

		// read type header
		String line = lines.pollFirst();
		int lineNumber = 1;
		assert line != null;
		final var classNameRaw = ValueWithComment
				.parse(line.substring(ExternalAnnotationProvider.CLASS_PREFIX.length()));
		ExternalAnnotationProvider.assertClassHeader(line, classNameRaw.value);

		final var eeaFile = new EEAFile(classNameRaw.value.replace('/', '.'));
		eeaFile.classHeader.name.comment = classNameRaw.comment;

		if (!path.replace('\\', '/').endsWith(eeaFile.relativePath.toString().replace('\\', '/')))
			throw new IOException("Mismatch between file path of [" + path + "] and contained class name definition ["
					+ eeaFile.classHeader.name.value + "]");

		// read type signature if present
		line = lines.peekFirst();
		if (line != null && !line.isBlank()
				&& line.startsWith(" <" /* ExternalAnnotationProvider.TYPE_PARAMETER_PREFIX */ )) {
			lines.removeFirst();
			lineNumber++;
			final var originalSignature = ValueWithComment.parse(line);
			eeaFile.classHeader.originalSignature.value = originalSignature.value;
			eeaFile.classHeader.originalSignature.comment = originalSignature.comment;

			line = lines.peekFirst();
			if (line != null && !line.isBlank()
					&& line.startsWith(" <" /* ExternalAnnotationProvider.TYPE_PARAMETER_PREFIX */ )) {
				lines.removeFirst();
				lineNumber++;
				eeaFile.classHeader.annotatedSignature = ValueWithComment.parse(line);
			} else {
				eeaFile.classHeader.annotatedSignature.value = eeaFile.classHeader.originalSignature.value;
			}
		}

		// read type members
		while ((line = lines.pollFirst()) != null) {
			line = line.stripTrailing();
			lineNumber++;
			if (line.isEmpty()) {
				eeaFile.addEmptyLine();
				continue;
			}

			if (line.startsWith(" "))
				throw new IOException(
						"Illegal format for field or method name [" + line + "] at " + path + ":" + lineNumber);

			/*
			 * read "super" declarations
			 */
			if (line.startsWith(ExternalAnnotationProvider.SUPER_PREFIX)) {
				final var superType = ValueWithComment
						.parse(line.substring(ExternalAnnotationProvider.SUPER_PREFIX.length()));
				line = lines.peekFirst();
				if (line == null || line.isBlank()
						|| !line.startsWith(" <" /* ExternalAnnotationProvider.TYPE_PARAMETER_PREFIX */ )) {
					continue;
				}
				lines.removeFirst();
				lineNumber++;
				final var superTypeParamsOriginal = ValueWithComment.parse(line);

				// read optional annotated signature
				line = lines.peekFirst();
				if (line != null && !line.isBlank()
						&& line.startsWith(" <" /* ExternalAnnotationProvider.TYPE_PARAMETER_PREFIX */ )) {
					lines.removeFirst();
					lineNumber++;
					final var superTypeParamsAnnotated = ValueWithComment.parse(line);
					if (!superTypeParamsOriginal.value.equals(superTypeParamsAnnotated.value) //
							&& !superTypeParamsOriginal.value
									.equals(removeNullAnnotations(superTypeParamsAnnotated.value)))
						throw new IOException("Signature mismatch at " + path + ":" + lineNumber + "\n" //
								+ "          Original: " + superTypeParamsAnnotated + "\n" //
								+ "Annotated Stripped: " + removeNullAnnotations(superTypeParamsAnnotated.value) + "\n" //
								+ "         Annotated: " + superTypeParamsAnnotated + "\n");
					eeaFile.superTypes
							.add(new ClassMember(superType, superTypeParamsOriginal, superTypeParamsAnnotated));
				} else {
					eeaFile.superTypes.add(new ClassMember(superType, superTypeParamsOriginal));
				}

				/*
				 * read and validate class member, i.e. field or method name
				 */
			} else {
				final var memberName = ValueWithComment.parse(line);

				// read mandatory original signature
				line = lines.pollFirst();
				lineNumber++;
				if (line == null || line.isBlank() || !line.startsWith(" "))
					throw new IOException("Illegal format for original signature at " + path + ":" + lineNumber);

				final var originalSignature = ValueWithComment.parse(line);
				if (!originalSignature.value.equals(removeNullAnnotations(originalSignature.value)))
					throw new IOException("Original signature contains null annotations at " + path + ":" + lineNumber);

				final var member = new ClassMember(memberName, originalSignature);
				if (eeaFile.members.contains(member))
					throw new IOException("Duplicate entry \"" + memberName.value + " " + originalSignature.value
							+ "\" found at " + path + ":" + lineNumber);

				// read optional annotated signature
				line = lines.peekFirst();
				if (line != null && !line.isBlank() && line.startsWith(" ")) {
					lines.removeFirst();
					lineNumber++;
					final var annotatedSignature = ValueWithComment.parse(line);
					if (!originalSignature.value.equals(annotatedSignature.value) //
							&& !originalSignature.value.equals(removeNullAnnotations(annotatedSignature.value)))
						throw new IOException("Signature mismatch at " + path + ":" + lineNumber + "\n" //
								+ "          Original: " + originalSignature + "\n" //
								+ "Annotated Stripped: " + removeNullAnnotations(annotatedSignature.value) + "\n" //
								+ "         Annotated: " + annotatedSignature + "\n");
					if (!originalSignature.value.equals(annotatedSignature.value) || annotatedSignature.hasComment()) {
						member.annotatedSignature = annotatedSignature;
					}
				}

				// store the parsed member entry
				eeaFile.members.add(member);
			}
		}
		return eeaFile;
	}

	private static Path classNameToRelativePath(final String className) {
		return Path.of(className.replace('.', File.separatorChar) + ExternalAnnotationProvider.ANNOTATION_FILE_SUFFIX);
	}

	public EEAFile(final String className) {
		this(className, classNameToRelativePath(className));
	}

	private EEAFile(final String className, final Path relativePath) {
		classHeader = new ClassMember(new ValueWithComment(className), new ValueWithComment(""));
		this.relativePath = relativePath;
	}

	public void addEmptyLine() {
		final ClassMember lastMember = MiscUtils.findLastElement(members);
		if (lastMember != null) {
			lastMember.isFollowedByEmptyLine = true;
		}
	}

	/**
	 * Adds a new member declaration to the end of the file
	 */
	public void addMember(final ClassMember member) {
		members.add(member);
	}

	/**
	 * Adds a new member declaration to the end of the file
	 */
	public ClassMember addMember(final String name, final String originalSignature) {
		final var member = new ClassMember(name, originalSignature);
		members.add(member);
		return member;
	}

	/**
	 * @return a class member with the same name and the same original signature
	 */
	public @Nullable ClassMember findMatchingClassMember(final ClassMember member) {
		return findMatchingClassMember(member.name.value, member.originalSignature.value);
	}

	/**
	 * @return a class member with the same name and the same original signature
	 */
	public @Nullable ClassMember findMatchingClassMember(final String name, final String originalSignature) {
		return members.stream() //
				.filter(m -> m.name.value.equals(name) //
						&& m.originalSignature.value.equals(originalSignature)) //
				.findFirst() //
				.orElse(null);
	}

	/**
	 * Copies annotated signatures for compatible class members from the given EEA
	 * file
	 *
	 * @param overrideOnConflict if true existing annotated signatures are
	 *                           overridden
	 */
	public void applyAnnotationsAndCommentsFrom(final EEAFile their, final boolean overrideOnConflict,
			final boolean addNewMembers) {
		LOG.log(Level.DEBUG, "Applying annotations from [{0}]...", their.relativePath);

		classHeader.applyAnnotationsAndCommentsFrom(their.classHeader, overrideOnConflict);

		for (final ClassMember superType : superTypes) {
			for (final ClassMember theirSuperType : their.superTypes) {
				if (superType.originalSignature.value.equals(theirSuperType.originalSignature.value)) {
					superType.applyAnnotationsAndCommentsFrom(theirSuperType, overrideOnConflict);
					break;
				}
			}
		}

		for (final ClassMember theirMember : their.members) {
			final ClassMember ourMember = findMatchingClassMember(theirMember);
			if (ourMember == null) {
				if (addNewMembers) {
					addMember(theirMember.clone());
				}
			} else {
				ourMember.applyAnnotationsAndCommentsFrom(theirMember, overrideOnConflict);
			}
		}
	}

	/**
	 * @return true if a corresponding EEAFile exists on the local file system
	 */
	public boolean exists(final Path rootPath) {
		return Files.exists(rootPath.resolve(relativePath));
	}

	public Collection<ClassMember> getClassMembers() {
		return members;
	}

	private void renderLine(final List<String> lines, final Object... newLineContent) {
		final var sb = new StringBuilder();
		for (final Object obj : newLineContent) {
			sb.append(Objects.toString(obj));
		}
		lines.add(sb.toString());
	}

	protected List<String> renderFileContent(final Set<SaveOption> opts) {

		final boolean omitComments = opts.contains(SaveOption.OMIT_COMMENTS);
		final boolean omitEmptyLines = opts.contains(SaveOption.OMIT_EMPTY_LINES);
		final boolean omitMembersWithInheritedAnnotatedSignature = opts
				.contains(SaveOption.OMIT_MEMBERS_WITH_INHERITED_ANNOTATED_SIGNATURES);
		final boolean omitMembersWithoutAnnotatedSignature = opts
				.contains(SaveOption.OMIT_MEMBERS_WITHOUT_ANNOTATED_SIGNATURE);
		final boolean omitRedundantAnnotatedSignatures = opts.contains(SaveOption.OMIT_REDUNDANT_ANNOTATED_SIGNATURES);

		final var lines = new ArrayList<String>();

		/*
		 * render class signature
		 */
		renderLine(lines, ExternalAnnotationProvider.CLASS_PREFIX, //
				new ValueWithComment(classHeader.name.value.replace('.', '/'),
						omitComments ? "" : classHeader.name.comment));
		final ValueWithComment classSignatureOriginal = classHeader.originalSignature;
		if (!classSignatureOriginal.value.isEmpty()) {
			renderLine(lines, " ", classSignatureOriginal.toString(omitComments));
			if (classHeader.hasNullAnnotations()) {
				renderLine(lines, " ", classHeader.annotatedSignature.toString(omitComments));
			} else {
				if (!omitRedundantAnnotatedSignatures) {
					renderLine(lines, " ", classSignatureOriginal.toString(omitComments));
				}
			}
		}

		if (!omitEmptyLines) {
			renderLine(lines);
		}

		/*
		 * render super signature
		 */
		if (!superTypes.isEmpty()) {
			for (final ClassMember superType : superTypes) {
				if (omitMembersWithoutAnnotatedSignature && !superType.hasNullAnnotations()) {
					continue;
				}
				renderLine(lines, ExternalAnnotationProvider.SUPER_PREFIX, superType.name.toString(omitComments));
				if (!superType.originalSignature.value.isEmpty()) {
					renderLine(lines, " ", superType.originalSignature.toString(omitComments));
					if (superType.hasNullAnnotations()) {
						renderLine(lines, " ", superType.annotatedSignature.toString(omitComments));
					} else {
						if (!omitRedundantAnnotatedSignatures) {
							renderLine(lines, " ", superType.originalSignature.toString(omitComments));
						}
					}
				}
			}
			if (!omitEmptyLines) {
				renderLine(lines);
			}
		}

		/*
		 * render fields/methods
		 */
		final ClassMember lastMember = findLastElement(members);
		for (final ClassMember member : members) {
			final boolean keep = member.name.comment.contains(MARKER_KEEP) //
					|| member.originalSignature.comment.contains(MARKER_KEEP) //
					|| member.annotatedSignature.comment.contains(MARKER_KEEP);

			if (omitMembersWithoutAnnotatedSignature //
					&& !keep //
					&& !member.hasNullAnnotations()) {
				continue;
			}
			if (omitMembersWithInheritedAnnotatedSignature //
					&& !keep //
					&& member.hasNullAnnotations() //
					&& contains(member.annotatedSignature.comment, MARKER_OVERRIDES)) {
				continue;
			}

			renderLine(lines, member.name.toString(omitComments));
			renderLine(lines, " ", member.originalSignature.toString(omitComments));

			if (!omitRedundantAnnotatedSignatures || member.hasNullAnnotations()) {
				renderLine(lines, " ", member.annotatedSignature.toString(omitComments));
			}

			if (member != lastMember && member.isFollowedByEmptyLine && !omitEmptyLines) {
				renderLine(lines);
			}
		}

		removeTrailingBlanks(lines);
		return lines;
	}

	/**
	 * @return true if modifications where written to disk, false was already
	 *         up-to-date
	 */
	public boolean save(final Path rootPath, final @Nullable SaveOption... opts) throws IOException {
		return save(rootPath, Arrays.stream(opts).filter(Objects::nonNull).collect(Collectors.toSet()));
	}

	/**
	 * @return true if modifications where written to disk, false was already
	 *         up-to-date
	 */
	public boolean save(final Path rootPath, final Set<SaveOption> opts) throws IOException {
		final Path path = rootPath.resolve(relativePath);

		final boolean replaceExisting = opts.contains(SaveOption.REPLACE_EXISTING);
		final boolean deleteIfEmpty = opts.contains(SaveOption.DELETE_IF_EMPTY);
		final boolean quiet = opts.contains(SaveOption.QUIET);

		final List<String> content = renderFileContent(opts);

		if (exists(rootPath)) {

			if (replaceExisting) {

				if (deleteIfEmpty && members.isEmpty()) {
					LOG.log(Level.WARNING, "Deleting [{0}] (reason: no members)...", path.toAbsolutePath());
					Files.deleteIfExists(path);
					return true;
				}

				if (deleteIfEmpty && content.size() < 3) {
					LOG.log(Level.WARNING, "Deleting [{0}] (reason: no annotated signatures)...",
							path.toAbsolutePath());
					Files.deleteIfExists(path);
					return true;
				}

				final boolean needsUpdate = !content.equals(Files.readAllLines(rootPath.resolve(relativePath)));
				if (!needsUpdate) {
					LOG.log(Level.DEBUG, "Skipped saving [{0}] (reason: unchanged).", path.toAbsolutePath());
					return false;
				}

				if (!quiet) {
					LOG.log(Level.INFO, "Updating [{0}]...", path.toAbsolutePath());
				}

			} else { // !replaceExisting

				final boolean needsUpdate = !content.equals(Files.readAllLines(rootPath.resolve(relativePath)));
				if (!needsUpdate) {
					LOG.log(Level.DEBUG, "Skipped saving [{0}] (reason: unchanged).", path.toAbsolutePath());
					return false;
				}
				throw new IOException("File [" + path + "] already exists!");
			}

		} else { // !exists(rootPath)

			if (deleteIfEmpty && members.isEmpty()) {
				LOG.log(Level.DEBUG, "Skipped creating [{0}] (reason: no members).", path.toAbsolutePath());
				return false;
			}

			if (deleteIfEmpty && content.size() < 3) {
				LOG.log(Level.DEBUG, "Skipped creating [{0}] (reason: no annotated signatures).",
						path.toAbsolutePath());
				return false;
			}

			if (!quiet) {
				LOG.log(Level.INFO, "Creating [{0}]...", path.toAbsolutePath());
			}

			final Path parentDir = path.getParent();
			assert parentDir != null;
			Files.createDirectories(parentDir);
		}

		final var openOpts = replaceExisting //
				? List.of(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING) //
				: List.of(StandardOpenOption.CREATE_NEW);
		Files.write(path, content, openOpts.toArray(OpenOption[]::new));
		return true;
	}

	@Override
	public String toString() {
		return super.toString() + " [" + relativePath + "]";
	}
}
