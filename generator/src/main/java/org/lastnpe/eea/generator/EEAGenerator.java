/*
 * SPDX-FileCopyrightText: © Vegard IT GmbH (https://vegardit.com) and others.
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Sebastian Thomschke (Vegard IT GmbH) - initial API and implementation
 */
package org.lastnpe.eea.generator;

import static org.lastnpe.eea.generator.internal.ClassGraphUtils.*;
import static org.lastnpe.eea.generator.internal.MiscUtils.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationProvider;
import org.lastnpe.eea.generator.EEAFile.ClassMember;
import org.lastnpe.eea.generator.EEAFile.SaveOption;
import org.lastnpe.eea.generator.EEAFile.ValueWithComment;
import org.lastnpe.eea.generator.internal.Props;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassMemberInfo;
import io.github.classgraph.ClassRefTypeSignature;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;

/**
 * @author Sebastian Thomschke (Vegard IT GmbH)
 */
public abstract class EEAGenerator {

	private static final Logger LOG = System.getLogger(EEAGenerator.class.getName());

	public static final Path DEFAULT_PROPERTES_FILE = Path.of("eea-generator.properties");

	public static final String JVM_PROPERTY_PREFIX = "eea-generator.";

	public static final String PROPERTY_ACTION = "action";
	public static final String PROPERTY_INPUT_DIRS = "input.dirs";
	public static final String PROPERTY_INPUT_DIRS_EXTRA = PROPERTY_INPUT_DIRS + ".extra";
	public static final String PROPERTY_OUTPUT_DIR = "output.dir";
	public static final String PROPERTY_OUTPUT_DIR_DEFAULT = PROPERTY_OUTPUT_DIR + ".default";
	public static final String PROPERTY_PACKAGES_INCLUDE = "packages.include";
	public static final String PROPERTY_CLASSES_EXCLUDE = "classes.exclude";
	public static final String PROPERTY_DELETE_IF_EMPTY = "deleteIfEmpty";
	public static final String PROPERTY_OMIT_REDUNDAND_ANNOTATED_SIGNATURES = "omitRedundantAnnotatedSignatures";
	public static final String PROPERTY_OMIT_CLASS_MEMBERS_WITHOUT_NULL_ANNOTATION = "omitClassMembersWithoutNullAnnotation";

	private static final EEAFile TEMPLATE_EXTERNALIZABLE;
	private static final EEAFile TEMPLATE_SERIALIZABLE;
	private static final EEAFile TEMPLATE_OBJECT;
	private static final EEAFile TEMPLATE_THROWABLE;

	private static final ClassInfo OBJECT_CLASS_INFO = new ClassInfo("java.lang.Object", Modifier.PUBLIC, null) {
	};

	static {
		try (var reader = getUTF8ResourceAsReader(EEAFile.class, "Externalizable.eea")) {
			TEMPLATE_EXTERNALIZABLE = EEAFile.load(reader, "classpath:java/io/Externalizable.eea");
		} catch (final Exception ex) {
			throw new IllegalStateException(ex);
		}

		try (var reader = getUTF8ResourceAsReader(EEAFile.class, "Serializable.eea")) {
			TEMPLATE_SERIALIZABLE = EEAFile.load(reader, "classpath:java/io/Serializable.eea");
		} catch (final Exception ex) {
			throw new IllegalStateException(ex);
		}

		try (var reader = getUTF8ResourceAsReader(EEAFile.class, "Object.eea")) {
			TEMPLATE_OBJECT = EEAFile.load(reader, "classpath:java/lang/Object.eea");
		} catch (final Exception ex) {
			throw new IllegalStateException(ex);
		}

		try (var reader = getUTF8ResourceAsReader(EEAFile.class, "Throwable.eea")) {
			TEMPLATE_THROWABLE = EEAFile.load(reader, "classpath:java/lang/Throwable.eea");
		} catch (final Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	public static class Config {
		public final String[] packages;
		public final List<Path> inputDirs = new ArrayList<>();
		public final Path outputDir;
		public Predicate<ClassInfo> classFilter = clazz -> true;
		public boolean deleteIfEmpty = true;
		public boolean omitClassMembersWithoutNullAnnotations;
		public boolean omitRedundantAnnotatedSignatures;

		public Config(final Path outputDir, final String... packages) {
			this.outputDir = outputDir;
			this.packages = packages;
		}
	}

	/**
	 * args[0]: optional path to properties file
	 */
	public static void main(final String... args) throws Exception {
		try {
			configureJUL();

			// load properties from file if specified
			Path filePropsPath = null;
			if (args.length > 0) {
				filePropsPath = Path.of(args[0]);
			} else if (Files.exists(DEFAULT_PROPERTES_FILE)) {
				filePropsPath = DEFAULT_PROPERTES_FILE;
			}
			final var props = new Props(JVM_PROPERTY_PREFIX, filePropsPath);

			final String action = props.get(PROPERTY_ACTION, null).value;

			final String[] packages = "minimize".equals(action) //
					? new @NonNull String[0] //
					: props.get(PROPERTY_PACKAGES_INCLUDE, null).value.split(",");

			final var classExclusionsStr = props.get(PROPERTY_CLASSES_EXCLUDE, "");
			final Pattern[] classExclusions = classExclusionsStr.value.isBlank() //
					? new Pattern[0] //
					: Arrays.stream(classExclusionsStr.value.split(",")).map(Pattern::compile).toArray(Pattern[]::new);

			final var outputDirPropDefault = props.get(PROPERTY_OUTPUT_DIR_DEFAULT, "").value;
			final var outputDirProp = props.get(PROPERTY_OUTPUT_DIR,
					outputDirPropDefault.isEmpty() ? null : outputDirPropDefault);
			Path outputDir = Path.of(outputDirProp.value);
			if (outputDirProp.source instanceof final Path outputDirsPropSource && !outputDir.isAbsolute()) {
				// if the specified outputDir value is relative and was source from properties
				// file, then make it relative to the properties file
				outputDir = outputDirsPropSource.getParent().resolve(outputDir);
			}
			outputDir = outputDir.normalize().toAbsolutePath();

			final var config = new Config(outputDir, packages);

			config.deleteIfEmpty = Boolean.parseBoolean(props.get(PROPERTY_DELETE_IF_EMPTY, "true").value);
			config.omitClassMembersWithoutNullAnnotations = Boolean
					.parseBoolean(props.get(PROPERTY_OMIT_CLASS_MEMBERS_WITHOUT_NULL_ANNOTATION, "false").value);
			config.omitRedundantAnnotatedSignatures = Boolean
					.parseBoolean(props.get(PROPERTY_OMIT_REDUNDAND_ANNOTATED_SIGNATURES, "false").value);
			config.classFilter = clazz -> {
				for (final Pattern classExclusion : classExclusions) {
					if (classExclusion.matcher(clazz.getName()).find())
						return false;
				}
				return true;
			};

			final var inputDirsProp = props.get(PROPERTY_INPUT_DIRS, "");
			final var inputDirsExtraProp = props.get(PROPERTY_INPUT_DIRS_EXTRA, "");
			for (final String inputDirStr : (inputDirsProp.value + ',' + inputDirsExtraProp.value).split(",")) {
				if (inputDirStr.isBlank()) {
					continue;
				}
				Path inputDir = Path.of(inputDirStr);
				if (inputDirsProp.source instanceof Path && !inputDir.isAbsolute()) {
					// if the specified inputDir value is relative and was source from properties
					// file,
					// then make it relative to the properties file
					inputDir = ((Path) inputDirsProp.source).getParent().resolve(inputDir);
				}
				inputDir = inputDir.toAbsolutePath().normalize();
				if (!config.inputDirs.contains(inputDir)) {
					config.inputDirs.add(inputDir);
					if (!Files.exists(inputDir)) {
						LOG.log(Level.WARNING, "Input directory: " + inputDir + " does not exist!");
					}
				}
			}

			LOG.log(Level.INFO, "Effective input directories: " + config.inputDirs);
			LOG.log(Level.INFO, "Effective output directory: " + outputDir);

			switch (action) {
			case "generate":
				generateEEAFiles(config);
				break;
			case "minimize":
				minimizeEEAFiles(config);
				break;
			case "validate":
				validateEEAFiles(config);
				break;
			default:
				throw new IllegalArgumentException("Unsupported value for [action] parameter: " + action);
			}
		} catch (final UncheckedIOException ex) {
			final Exception iox = ex.getCause();
			sanitizeStackTraces(iox);
			throw iox;
		} catch (final Exception ex) {
			sanitizeStackTraces(ex);
			throw ex;
		}
	}

	protected static ValueWithComment computeAnnotatedSignature(final EEAFile.ClassMember member,
			final ClassInfo classInfo, final ClassMemberInfo memberInfo) {

		final var templates = new ArrayList<EEAFile>();
		if (isThrowable(classInfo)) {
			templates.add(TEMPLATE_THROWABLE); // to inherit constructor parameter annotations
		}
		templates.add(TEMPLATE_EXTERNALIZABLE);
		templates.add(TEMPLATE_SERIALIZABLE);
		templates.add(TEMPLATE_OBJECT);

		for (final EEAFile template : templates) {
			final ClassMember matchingMember = template.findMatchingClassMember(member);
			if (matchingMember != null && matchingMember.hasNullAnnotations())
				return matchingMember.annotatedSignature;
		}

		// analyzing a method
		if (memberInfo instanceof final MethodInfo methodInfo) {
			/*
			 * mark the return value of builder methods as @NonNull.
			 */
			if (classInfo.getName().endsWith("Builder") //
					&& !methodInfo.isStatic() // non-static
					&& methodInfo.isPublic() //
					&& methodInfo.getTypeDescriptor()
							.getResultType() instanceof final ClassRefTypeSignature returnTypeSig //
					&& (methodInfo.getName().equals("build") && methodInfo.getParameterInfo().length == 0 //
							|| Objects.equals(returnTypeSig.getClassInfo(), classInfo)))
				// (...)Lcom/example/MyBuilder -> (...)L1com/example/MyBuilder;
				return new ValueWithComment(insert(member.originalSignature.value,
						member.originalSignature.value.lastIndexOf(")") + 2, "1"), "");

			/*
			 * mark the parameter of Comparable#compareTo(Object) as @NonNull.
			 */
			if (classInfo.implementsInterface("java.lang.Comparable") //
					&& !methodInfo.isStatic() // non-static
					&& member.originalSignature.value.endsWith(")I") // returns Integer
					&& methodInfo.isPublic() //
					&& methodInfo.getParameterInfo().length == 1 // only 1 parameter
					&& methodInfo.getParameterInfo()[0].getTypeDescriptor() instanceof ClassRefTypeSignature)
				// (Lcom/example/Entity;)I -> (L1com/example/Entity;)I
				return new ValueWithComment(insert(member.originalSignature.value, 2, "1"), "");

			/*
			 * mark the parameter of single-parameter void methods as @NonNull, if the class
			 * name matches "*Listener" and the parameter type name matches "*Event"
			 */
			if (classInfo.isInterface() //
					&& classInfo.getName().endsWith("Listener") //
					&& !methodInfo.isStatic() // non-static
					&& member.originalSignature.value.endsWith(")V") // returns void
					&& methodInfo.getParameterInfo().length == 1 // only 1 parameter
					&& methodInfo.getParameterInfo()[0].getTypeDescriptor().toString().endsWith("Event"))
				// (Ljava/lang/String;)V -> (L1java/lang/String;)V
				return new ValueWithComment(insert(member.originalSignature.value, 2, "1"), "");

			/*
			 * mark the parameter of single-parameter methods as @NonNull with signature
			 * matching: void (add|remove)*Listener(*Listener)
			 */
			if (!methodInfo.isStatic() // non-static
					&& (methodInfo.getName().startsWith("add") || methodInfo.getName().startsWith("remove")) //
					&& methodInfo.getName().endsWith("Listener") //
					&& member.originalSignature.value.endsWith(")V") // returns void
					&& methodInfo.getParameterInfo().length == 1 // only 1 parameter
					&& methodInfo.getParameterInfo()[0].getTypeDescriptor().toString().endsWith("Listener"))
				return new ValueWithComment( //
						member.originalSignature.value.startsWith("(") //
								// (Lcom/example/MyListener;)V -> (L1com/example/MyListener;)V
								// (TT;)V -> (T1T;)V
								? insert(member.originalSignature.value, 2, "1") //
								// <T::Lcom/example/MyListener;>(TT;)V --> <1T::Lcom/example/MyListener;>(TT;)V
								: insert(member.originalSignature.value, 1, "1"), //
						"");

			if (hasObjectReturnType(member)) { // returns non-void
				if (hasNullableAnnotation(methodInfo.getAnnotationInfo()))
					// ()Ljava/lang/String -> ()L0java/lang/String;
					return new ValueWithComment(insert(member.originalSignature.value,
							member.originalSignature.value.lastIndexOf(")") + 2, "0"), "");

				if (hasNonNullAnnotation(methodInfo.getAnnotationInfo()))
					// ()Ljava/lang/String -> ()L1java/lang/String;
					return new ValueWithComment(insert(member.originalSignature.value,
							member.originalSignature.value.lastIndexOf(")") + 2, "1"), "");
			}
		}

		// analyzing a field
		if (memberInfo instanceof final FieldInfo fieldInfo) {
			if (hasNullableAnnotation(fieldInfo.getAnnotationInfo()))
				return new ValueWithComment(insert(member.originalSignature.value, 1, "0"));

			// if the field is static and final we by default expect it to be non-null
			if (fieldInfo.isStatic() && fieldInfo.isFinal() || hasNonNullAnnotation(fieldInfo.getAnnotationInfo()) //
			)
				// Ljava/lang/String; -> L1java/lang/String;
				return new ValueWithComment(insert(member.originalSignature.value, 1, "1"));
		}

		return new ValueWithComment(member.originalSignature.value);
	}

	protected static boolean hasObjectReturnType(final EEAFile.ClassMember member) {
		final String sig = member.originalSignature.value;
		// object return type: (Ljava/lang/String;)Ljava/lang/String; or
		// (Ljava/lang/String;)TT;
		// void return type: (Ljava/lang/String;)V
		// primitive return type: (Ljava/lang/String;)B
		return sig.charAt(sig.length() - 2) != ')';
	}

	protected static EEAFile computeEEAFile(final ClassInfo classInfo) {
		LOG.log(Level.DEBUG, "Scanning class [{0}]...", classInfo.getName());

		final var eeaFile = new EEAFile(classInfo.getName());

		final var fields = classInfo.getDeclaredFieldInfo();
		final var methods = classInfo.getDeclaredMethodAndConstructorInfo();

		final String typeSigStr = classInfo.getTypeSignatureStr();
		if (typeSigStr != null) {

			// class signature
			final String superTypesSigStr;
			if (typeSigStr.startsWith("<")) {
				final String typeParams = substringBetweenBalanced(typeSigStr, '<', '>');
				if (typeParams != null) {
					eeaFile.classHeader.originalSignature.value = '<' + typeParams + '>';
					eeaFile.classHeader.annotatedSignature.value = eeaFile.classHeader.originalSignature.value;

					superTypesSigStr = typeSigStr.substring(typeParams.length() + 2);
				} else {
					superTypesSigStr = typeSigStr;
				}
			} else {
				superTypesSigStr = typeSigStr;
			}

			final Function<String, List<String>> superTypesSigSplitter = signature -> {
				final String[] chunks = signature.split(";");
				final var result = new ArrayList<String>();
				final var sb = new StringBuilder();
				for (final String chunk : chunks) {
					sb.append(chunk);

					if (sb.length() > 0 && countOccurrences(sb, '<') == countOccurrences(sb, '>')) {
						sb.deleteCharAt(0); // remove the leading L of e.g. Ljava/lang/Object;
						result.add(sb.toString());
						sb.setLength(0);
					} else {
						sb.append(';');
					}
				}
				return result;
			};

			// super signatures
			for (final String superTypeSig : superTypesSigSplitter.apply(superTypesSigStr)) {
				if (!superTypeSig.contains("<")) {
					continue;
				}
				final String superTypeName = superTypeSig.split("<", 2)[0];
				final String superTypeParams = '<' + substringBetweenBalanced(superTypeSig, '<', '>') + '>';
				assert superTypeParams != null;
				eeaFile.superTypes.add(
						new ClassMember(new ValueWithComment(superTypeName), new ValueWithComment(superTypeParams)));
			}
		}
		eeaFile.addEmptyLine();

		// static fields
		for (final FieldInfo f : getStaticFields(fields)) {
			if (classInfo.isEnum()) {
				// omit enum values as they are always treated as non-null by Eclipse compiler
				if (f.isFinal() && startsWith(classInfo.getTypeSignatureStr(),
						"Ljava/lang/Enum<" + f.getTypeDescriptorStr() + ">;")) {
					continue;
				}
			}

			final var member = eeaFile.addMember(f.getName(), f.getTypeSignatureOrTypeDescriptorStr());
			member.annotatedSignature = computeAnnotatedSignature(member, classInfo, f);
		}
		eeaFile.addEmptyLine();

		// static methods
		for (final MethodInfo m : getStaticMethods(methods)) {
			final var member = eeaFile.addMember(m.getName(), m.getTypeSignatureOrTypeDescriptorStr());
			member.annotatedSignature = computeAnnotatedSignature(member, classInfo, m);
		}
		eeaFile.addEmptyLine();

		// instance fields
		for (final FieldInfo f : getInstanceFields(fields)) {
			final var member = eeaFile.addMember(f.getName(), f.getTypeSignatureOrTypeDescriptorStr());
			member.annotatedSignature = computeAnnotatedSignature(member, classInfo, f);
		}
		eeaFile.addEmptyLine();

		// instance methods
		for (final MethodInfo m : getInstanceMethods(methods)) {
			final var member = eeaFile.addMember(m.getName(), m.getTypeSignatureOrTypeDescriptorStr());
			member.annotatedSignature = computeAnnotatedSignature(member, classInfo, m);
		}
		return eeaFile;
	}

	/**
	 * Instantiates {@link EEAFile} instances for all classes found in classpath in
	 * the given package or sub-packages.
	 *
	 * @param rootPackageName name the of root package to scan for classes
	 * @throws IllegalArgumentException if no class was found
	 */
	protected static SortedMap<ClassInfo, EEAFile> computeEEAFiles(final String rootPackageName,
			final Predicate<ClassInfo> filter) {
		final var result = new TreeMap<ClassInfo, EEAFile>();

		try (ScanResult scanResult = new ClassGraph() //
				.enableAllInfo() //
				.enableSystemJarsAndModules() //
				.acceptPackages(rootPackageName) //
				.scan() //
		) {
			final List<ClassInfo> classes = scanResult.getAllClasses();
			if (classes.isEmpty())
				throw new IllegalArgumentException(
						"No classes found for package [" + rootPackageName + "] on classpath");

			for (final ClassInfo classInfo : classes) {
				if (classInfo.getName().equals("java.lang.AbstractStringBuilder")) { // https://github.com/vegardit/no-npe/issues/257
					LOG.log(Level.DEBUG, "Scanning class [{0}]...", classInfo.getName());
					final var eeaFile = computeEEAFile(classInfo);
					result.put(classInfo, eeaFile);
					continue;
				}

				// skip uninteresting classes
				if (hasPackageVisibility(classInfo) || classInfo.isPrivate() || classInfo.isAnonymousInnerClass()) {
					LOG.log(Level.DEBUG, "Ignoring non-accessible classes [{0}]...", classInfo.getName());
					continue;
				}

				if (!filter.test(classInfo)) {
					LOG.log(Level.DEBUG, "Ignoring class excluded by filter [{0}]...", classInfo.getName());
					continue;
				}

				LOG.log(Level.DEBUG, "Scanning class [{0}]...", classInfo.getName());
				final var eeaFile = computeEEAFile(classInfo);
				result.put(classInfo, eeaFile);
			}
		}

		// TODO workaround for https://github.com/classgraph/classgraph/issues/703
		if ("java".equals(rootPackageName) || rootPackageName.startsWith("java.lang")) {
			result.putIfAbsent(OBJECT_CLASS_INFO, TEMPLATE_OBJECT);
		}
		return result;
	}

	/**
	 * Scans the classpath for classes of {@link Config#packages}, applies EEAs from
	 * files in {@link Config#inputDirs} and creates updated EEA files in
	 * {@link Config#outputDir}.
	 *
	 * @return number of updated and removed files
	 * @throws IllegalArgumentException if no class was found
	 */
	public static long generateEEAFiles(final Config cfg) throws IOException {
		final var saveOptions = Arrays.stream(new @Nullable SaveOption[] { //
				SaveOption.REPLACE_EXISTING, //
				cfg.deleteIfEmpty ? SaveOption.DELETE_IF_EMPTY : null, //
				cfg.omitRedundantAnnotatedSignatures ? SaveOption.OMIT_REDUNDANT_ANNOTATED_SIGNATURES : null, //
				cfg.omitClassMembersWithoutNullAnnotations ? SaveOption.OMIT_MEMBERS_WITHOUT_ANNOTATED_SIGNATURE : null //
		}).filter(Objects::nonNull).collect(Collectors.toSet());

		final var eeaFiles = new HashMap<ClassInfo, EEAFile>();

		long totalModifications = 0;
		for (final String packageName : cfg.packages) {
			LOG.log(Level.INFO, "Scanning EEA files of package [{0}]...", packageName);

			// create EEAFile instances based on class signatures found on classpath
			final Map<ClassInfo, EEAFile> eeaFilesOfPackage = computeEEAFiles(packageName, cfg.classFilter);
			LOG.log(Level.INFO, "Found {0} types on classpath.", eeaFilesOfPackage.size());

			// extend computed EEAFiles with annotations found on matching *.eea files in
			// input dirs
			for (final var computedEEAFile : eeaFilesOfPackage.values()) {
				for (final Path inputDir : cfg.inputDirs) {
					final var existingEEAFile = EEAFile.loadIfExists(inputDir, computedEEAFile.classHeader.name.value);
					if (existingEEAFile != null) {
						computedEEAFile.applyAnnotationsAndCommentsFrom(existingEEAFile, true, false);
					}
				}
			}

			eeaFiles.putAll(eeaFilesOfPackage);

			// remove obsolete files
			final var pkgDeletions = new LongAdder();
			final var eeaFilesByPath = new HashMap<Path, EEAFile>();
			eeaFilesOfPackage.values().forEach(f -> eeaFilesByPath.put(f.relativePath, f));
			forEachFileWithExtension(cfg.outputDir.resolve(packageName.replace('.', File.separatorChar)),
					ExternalAnnotationProvider.ANNOTATION_FILE_SUFFIX, path -> {
						final Path relativePath = cfg.outputDir.relativize(path);
						if (!eeaFilesByPath.containsKey(relativePath)) {
							LOG.log(Level.WARNING, "Removing obsolete annotation file [{0}]...", path.toAbsolutePath());
							Files.delete(path);
							pkgDeletions.increment();
						}
					});

			LOG.log(Level.INFO, "{0} EEA file(s) of package [{1}] updated or removed.", pkgDeletions.sum(),
					packageName);
			totalModifications += pkgDeletions.sum();
		}

		// will hold additional EEA files found in input dirs that are not part of the
		// packages defined in {@link Config#packages}
		final var superEEAFiles = new HashMap<ClassInfo, EEAFile>();
		final Function<ClassInfo, @Nullable EEAFile> getSuperEEAFile = classInfo -> {
			EEAFile eeaFile = eeaFiles.get(classInfo);
			if (eeaFile != null)
				return eeaFile;
			eeaFile = superEEAFiles.get(classInfo);
			if (eeaFile != null)
				return eeaFile;

			for (final Path inputDir : cfg.inputDirs) {
				try {
					eeaFile = EEAFile.loadIfExists(inputDir, classInfo.getName().replace('.', '/'));
					if (eeaFile != null) {
						superEEAFiles.put(classInfo, eeaFile);
						return eeaFile;
					}
				} catch (final IOException ex) {
					throw new UncheckedIOException(ex);
				}
			}
			return null;
		};

		// determine inherited annotated signatures
		final var recomputeInheritance = new AtomicBoolean(true);
		while (recomputeInheritance.get()) {
			recomputeInheritance.set(false);
			eeaFiles.forEach((classInfo, eeaFile) -> {
				if (classInfo == OBJECT_CLASS_INFO)
					return;

				final var superClasses = new ArrayList<>(classInfo.getSuperclasses());
				superClasses.add(OBJECT_CLASS_INFO);
				final var interfaces = classInfo.getInterfaces();

				for (final ClassMember member : eeaFile.getClassMembers()) {
					switch (member.getType()) {
					case CONSTRUCTOR:
						continue; // exclude constructors
					case FIELD:
						if (isStaticField(classInfo, member.name.value)) {
							continue; // exclude static fields
						}
						break;
					case METHOD:
						if (isStaticMethod(classInfo, member.name.value, member.originalSignature.value)) {
							continue; // exclude static methods
						}
						break;
					}

					ValueWithComment inheritableAnnotatedSignature = null;
					ClassInfo inheritedFrom = null;

					/*
					 * 1) scan super classes (which have precedence) for inheritable annotated
					 * signature
					 */
					for (final ClassInfo superClass : superClasses) {
						final EEAFile superClassEEA = superClass == OBJECT_CLASS_INFO //
								? TEMPLATE_OBJECT
								: getSuperEEAFile.apply(superClass);
						if (superClassEEA == null) {
							continue;
						}

						final var superClassMember = superClassEEA.findMatchingClassMember(member.name.value,
								member.originalSignature.value);
						if (superClassMember != null && superClassMember.hasNullAnnotations()) {
							inheritableAnnotatedSignature = superClassMember.annotatedSignature;
							inheritedFrom = superClass;
							break;
						}
					}

					/*
					 * 2) scan interfaces if no inheritable annotated signature was found in any
					 * super class
					 */
					boolean hasConflictingIFaceAnnotatedSignatures = false;
					if (inheritableAnnotatedSignature == null) {
						for (final ClassInfo iface : interfaces) {
							final EEAFile ifaceEEA = getSuperEEAFile.apply(iface);
							if (ifaceEEA == null) {
								continue;
							}

							final var ifaceMember = ifaceEEA.findMatchingClassMember(member.name.value,
									member.originalSignature.value);
							if (ifaceMember == null) {
								continue;
							}

							if (ifaceMember.hasNullAnnotations()) {
								if (inheritableAnnotatedSignature == null) {
									inheritableAnnotatedSignature = ifaceMember.annotatedSignature;
									inheritedFrom = iface;
								} else if (!inheritableAnnotatedSignature.value
										.equals(ifaceMember.annotatedSignature.value)) {
									hasConflictingIFaceAnnotatedSignatures = true;
									inheritableAnnotatedSignature = null;
									break;
								}
							}
						}
					}

					/*
					 * 3) apply inheritable annotated signature if applicable
					 */
					// handle case when currently no annotated signature is defined
					if (!member.hasNullAnnotations()) {
						if (inheritableAnnotatedSignature != null) { // apply the inherited annotated signature
							assert inheritedFrom != null;
							if (setInheritedAnnotatedSignature(member, inheritableAnnotatedSignature,
									inheritedFrom.getName())) {
								recomputeInheritance.set(true);
							}
						}

						// handle case when an annotated signature is present already
					} else {
						if (hasConflictingIFaceAnnotatedSignatures || inheritableAnnotatedSignature == null) {
							// do nothing
						} else {
							assert inheritedFrom != null;
							if (inheritableAnnotatedSignature.value.equals(member.annotatedSignature.value)) {
								if (setInheritedAnnotatedSignature(member, inheritableAnnotatedSignature,
										inheritedFrom.getName())) {
									recomputeInheritance.set(true);
								}
							} else if (contains(member.annotatedSignature.comment, EEAFile.MARKER_INHERITED)) {
								// if the current annotated signature states it was inherited but
								// a different inheritable annotated signature was found -> update the signature
								if (setInheritedAnnotatedSignature(member, inheritableAnnotatedSignature,
										inheritedFrom.getName())) {
									recomputeInheritance.set(true);
								}
							} else {
								// if the current annotated signature is different from the inheritable
								// annotated signature
								// -> add a comment that the annotated signature is overridden on purpose
								if (setOverridingAnnotatedSignatureComment(member.annotatedSignature,
										inheritedFrom.getName())) {
									recomputeInheritance.set(true);
								}
							}
						}
					}
				}
			});
		}

		// save updated EEA files
		long updates = 0;
		for (final var computedEEAFile : eeaFiles.values()) {
			if (computedEEAFile.save(cfg.outputDir, saveOptions)) {
				updates++;
			}
		}
		LOG.log(Level.INFO, "{0} EEA file(s) updated.", updates);
		totalModifications += updates;

		return totalModifications;
	}

	/**
	 * @return true if executing this method changes the value or comment of the
	 *         target's annotated signature
	 */
	private static boolean setInheritedAnnotatedSignature(final ClassMember target,
			final ValueWithComment inheritableAnnotatedSignature, final String parentType) {
		final var oldAnnotatedSignature = target.annotatedSignature;
		final var newAnnotatedSignature = new ValueWithComment(inheritableAnnotatedSignature.value);
		newAnnotatedSignature.comment = "# " + EEAFile.MARKER_INHERITED + "(" + parentType + ")";
		target.annotatedSignature = newAnnotatedSignature;
		return !Objects.equals(oldAnnotatedSignature.value, newAnnotatedSignature.value) //
				|| !Objects.equals(oldAnnotatedSignature.comment, newAnnotatedSignature.comment);
	}

	/**
	 * @return true if executing this method changes the value or comment of the
	 *         target's annotated signature
	 */
	private static boolean setOverridingAnnotatedSignatureComment(final ValueWithComment annotatedSignature,
			final String parentType) {
		final String oldComment = annotatedSignature.comment;
		annotatedSignature.comment = "# " + EEAFile.MARKER_OVERRIDES + "(" + parentType + ")";
		return !Objects.equals(oldComment, annotatedSignature.comment);
	}

	/**
	 * Merges and minimizes EEA files.
	 *
	 * @return number of updated and removed files
	 */
	public static long minimizeEEAFiles(final Config cfg) throws IOException {
		final var saveOptions = Set.of( //
				SaveOption.REPLACE_EXISTING, //
				SaveOption.DELETE_IF_EMPTY, //
				SaveOption.OMIT_COMMENTS, //
				SaveOption.OMIT_EMPTY_LINES, //
				SaveOption.OMIT_REDUNDANT_ANNOTATED_SIGNATURES, //

				// currently does not work reliable, see
				// https://github.com/eclipse-jdt/eclipse.jdt.core/issues/2512
				// SaveOption.OMIT_MEMBERS_WITH_INHERITED_ANNOTATED_SIGNATURES, //

				SaveOption.OMIT_MEMBERS_WITHOUT_ANNOTATED_SIGNATURE, //
				SaveOption.QUIET);

		if (cfg.inputDirs.isEmpty())
			throw new IllegalArgumentException("No input.dirs specified!");

		LOG.log(Level.INFO, "Minimizing EEA files...");

		final var mergedEEAFiles = new TreeMap<Path, EEAFile>();
		for (final Path inputDir : cfg.inputDirs) {
			LOG.log(Level.INFO, "Loading EEA files from [{0}]...", inputDir);
			forEachFileWithExtension(inputDir, ExternalAnnotationProvider.ANNOTATION_FILE_SUFFIX, //
					path -> {
						final Path relativePath = inputDir.relativize(path);
						final EEAFile mergedEEAFile = mergedEEAFiles.get(relativePath);
						final String expectedClassName = relativePathToClassName(relativePath);

						final EEAFile sourceEEAFile = EEAFile.load(inputDir, expectedClassName);
						if (mergedEEAFile == null) {
							mergedEEAFiles.put(relativePath, sourceEEAFile);
						} else {
							mergedEEAFile.applyAnnotationsAndCommentsFrom(sourceEEAFile, false, true);
						}
					});
		}
		LOG.log(Level.INFO, "Found {0} types.", mergedEEAFiles.size());

		final var totalModifications = new LongAdder();
		for (final EEAFile eeaFile : mergedEEAFiles.values()) {
			if (eeaFile.save(cfg.outputDir, saveOptions)) {
				totalModifications.increment();
			}
		}

		// remove obsolete files
		forEachFileWithExtension(cfg.outputDir, ExternalAnnotationProvider.ANNOTATION_FILE_SUFFIX, //
				path -> {
					final Path relativePath = cfg.outputDir.relativize(path);
					if (!mergedEEAFiles.containsKey(relativePath)) {
						LOG.log(Level.DEBUG, "Removing obsolete annotation file [{0}]...", path.toAbsolutePath());
						Files.delete(path);
						totalModifications.increment();
					}
				});

		LOG.log(Level.INFO, "{0} EEA file(s) minimized or removed.", totalModifications.sum());
		return totalModifications.sum();
	}

	private static String relativePathToClassName(final Path relativePath) {
		return removeSuffix(relativePath.toString(), ExternalAnnotationProvider.ANNOTATION_FILE_SUFFIX)
				.replace(File.separatorChar, '.');
	}

	/**
	 * Recursively validates all EEA files for the given {@link Config#packages} in
	 * {@link Config#outputDir}.
	 *
	 * @return number of validated files
	 * @throws IllegalArgumentException if no class was found
	 */
	public static long validateEEAFiles(final Config config) throws IOException {
		long totalValidations = 0;

		for (final String packageName : config.packages) {
			LOG.log(Level.INFO, "Validating EEA files of package [{0}]...", packageName);

			final Map<Path, EEAFile> eeaFilesOfPkg = remap(computeEEAFiles(packageName, config.classFilter),
					v -> v.relativePath);
			LOG.log(Level.INFO, "Found {0} types on classpath.", eeaFilesOfPkg.size());

			final Path packagePath = config.outputDir.resolve(packageName.replace('.', File.separatorChar));
			final long count = forEachFileWithExtension(packagePath, ExternalAnnotationProvider.ANNOTATION_FILE_SUFFIX, //
					path -> {
						final Path relativePath = config.outputDir.relativize(path);
						final String expectedClassName = relativePathToClassName(relativePath);

						// ensure if the type actually exists on the class path
						final var computedEEAFile = eeaFilesOfPkg.get(relativePath);
						if (computedEEAFile == null)
							throw new IllegalStateException("Type [" + expectedClassName + "] defined in [" + path
									+ "] no found on classpath.");

						// try to parse the EEA file
						final var parsedEEAFile = EEAFile.load(path);

						// ensure the EEA file does not contain declarations of non-existing
						// fields/methods
						for (final ClassMember parsedMember : parsedEEAFile.getClassMembers()) {
							if (computedEEAFile.findMatchingClassMember(parsedMember) == null) {
								final var candidates = computedEEAFile.getClassMembers().stream() //
										.filter(m -> m.name.equals(parsedMember.name)) //
										.map(m -> m.name + "\n" + " " + m.originalSignature) //
										.collect(Collectors.joining("\n"));
								throw new IllegalStateException("Unknown member declaration found in [" + path + "]:\n"
										+ parsedMember
										+ (candidates.length() > 0 ? "\nPotential candidates: \n" + candidates : ""));
							}
						}
					});
			LOG.log(Level.INFO, "{0} EEA file(s) of package [{1}] validated.", count, packageName);
			totalValidations += count;
		}
		return totalValidations;
	}
}
