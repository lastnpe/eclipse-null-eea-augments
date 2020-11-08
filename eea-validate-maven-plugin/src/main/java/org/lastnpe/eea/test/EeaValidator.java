package org.lastnpe.eea.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * EEA Validator. based on the work of Roland Praml.
 * 
 * @author Jason Faust
 *
 */
@Mojo(name = "validate", defaultPhase = LifecyclePhase.TEST)
public class EeaValidator extends AbstractMojo {

    @Parameter(property = "basedir", defaultValue = "${project.basedir}")
    private String baseDir;

    @Parameter(property = "builddir", defaultValue = "${project.build.directory}")
    private String buildDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        var base = Paths.get(baseDir);
        
        try {
            boolean ok = true;
            for (var eea : findEaa(base, buildDir)) {
                ok &= verify(base, eea);
            }
            if (!ok) {
                throw new MojoFailureException("Annotations failed validation");
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error getting EEA files", e);
        }

    }

    private enum State {
        CLASS, ENTRY, SIGNATURE, ANNOTATION,
    }

    private boolean verify(Path base, Path eea) throws MojoExecutionException {
        var log = getLog();

        log.debug("Verifying " + eea);

        var state = State.CLASS;
        try (var in = new LineReader(Files.newBufferedReader(base.resolve(eea)))) {
            String line;
            while ((line = in.line()) != null) {
                if (line.endsWith("\r\n") || !line.endsWith("\n")) {
                    log.error(eea + ": All lines must end with \\n only (unix)");
                    return false;
                }
                line = line.substring(0, line.length() - 1);

                switch (state) {
                case CLASS: {
                    if (!line.startsWith("class ")) {
                        log.error(eea + ": First line must start with 'class'");
                        return false;
                    }
                    var clazz = line.substring(6);
                    var eeaStr = eea.toString();
                    eeaStr = eeaStr.substring(0, eeaStr.length() - 4).replace('\\', '/');
                    if (!clazz.equals(eeaStr)) {
                        log.error(eea + ": Class " + eeaStr + " does not match the file");
                        return false;
                    }
                    state = State.ENTRY;
                    break;
                }
                case ENTRY:
                    // TODO verify method / member name
                    state = State.SIGNATURE;
                    break;
                case SIGNATURE:
                    // TODO verify signature
                    state = State.ANNOTATION;
                    break;
                case ANNOTATION:
                    // TODO verify annotated signature
                    state = State.ENTRY;
                    break;
                }
            }
            if (state != State.ENTRY) {
                log.error(eea + ": File is incomplete");
                return false;
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Error reading EEA file " + eea, e);
        }

        return true;
    }

    private List<Path> findEaa(Path b, String target) throws IOException {
        var t = Paths.get(target);
        try (var s = Files.walk(b)) {
            return s.filter(p -> !p.startsWith(t)).filter(p -> p.toString().endsWith(".eea")).map(p -> b.relativize(p))
                    .collect(Collectors.toList());
        }
    }

}
