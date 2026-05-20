package io.sedna.reverse.parse;

import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.reverse.model.ParsedClass;
import io.sedna.reverse.model.ParsedProject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/** Adds bytecode-discovered edges (ASM) to source-parsed dependencies. */
public final class BytecodeDependencyAugmenter {

  private static final List<String> CLASS_ROOT_SUFFIXES =
      List.of(
          "build/classes/java/main",
          "build/classes/kotlin/main",
          "target/classes");

  public Result<ParsedProject, SemanticError> augment(ParsedProject project) {
    Path projectRoot = project.projectRoot();
    Set<String> projectTypes = new TreeSet<>(project.classesByName().keySet());
    Map<String, Set<String>> bytecodeDeps = new TreeMap<>();

    for (String rootSuffix : CLASS_ROOT_SUFFIXES) {
      Path classRoot = projectRoot.resolve(rootSuffix);
      if (!Files.isDirectory(classRoot)) {
        continue;
      }
      try (Stream<Path> paths = Files.walk(classRoot)) {
        List<Path> classFiles = paths.filter(path -> path.toString().endsWith(".class")).sorted().toList();
        for (Path classFile : classFiles) {
          String sourceQualified = toSourceQualifiedName(classRoot, classFile);
          if (!projectTypes.contains(sourceQualified)) {
            continue;
          }
          Set<String> refs = readReferencedProjectTypes(classFile, projectTypes);
          if (!refs.isEmpty()) {
            bytecodeDeps
                .computeIfAbsent(sourceQualified, ignored -> new TreeSet<>())
                .addAll(refs);
          }
        }
      } catch (IOException ex) {
        return Result.err(
            io.sedna.core.SemanticError.global(
                io.sedna.core.ErrorCode.INTERNAL, ex.getMessage()));
      }
    }

    if (bytecodeDeps.isEmpty()) {
      return Result.ok(project);
    }

    Map<String, ParsedClass> merged = new TreeMap<>(project.classesByName());
    for (ParsedClass parsed : project.classes()) {
      Set<String> extra = bytecodeDeps.getOrDefault(parsed.qualifiedName(), Set.of());
      if (extra.isEmpty()) {
        continue;
      }
      List<String> dependencies = new ArrayList<>(parsed.dependencyQualifiedNames());
      dependencies.addAll(extra);
      List<String> ordered = dependencies.stream().distinct().sorted().toList();
      merged.put(
          parsed.qualifiedName(),
          new ParsedClass(
              parsed.qualifiedName(),
              parsed.packageName(),
              parsed.simpleName(),
              parsed.annotationSimpleNames(),
              ordered,
              parsed.publicMethodSignatures()));
    }
    ParsedProject augmented = new ParsedProject(projectRoot, merged);
    return Result.ok(ParsedClassAssembler.resolveDependencies(augmented));
  }

  private static String toSourceQualifiedName(Path classRoot, Path classFile) {
    Path relative = classRoot.relativize(classFile);
    String withoutSuffix = relative.toString().replace('\\', '/').replace('/', '.');
    if (withoutSuffix.endsWith(".class")) {
      withoutSuffix = withoutSuffix.substring(0, withoutSuffix.length() - ".class".length());
    }
    return withoutSuffix;
  }

  private static Set<String> readReferencedProjectTypes(Path classFile, Set<String> projectTypes)
      throws IOException {
    Set<String> refs = new TreeSet<>();
    try (InputStream input = Files.newInputStream(classFile)) {
      ClassReader reader = new ClassReader(input);
      reader.accept(
          new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(
                int version,
                int access,
                String name,
                String signature,
                String superName,
                String[] interfaces) {
              addTypeRef(name, projectTypes, refs);
              addTypeRef(superName, projectTypes, refs);
              if (interfaces != null) {
                for (String iface : interfaces) {
                  addTypeRef(iface, projectTypes, refs);
                }
              }
            }

            @Override
            public MethodVisitor visitMethod(
                int access, String name, String descriptor, String signature, String[] exceptions) {
              return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitMethodInsn(
                    int opcode,
                    String owner,
                    String methodName,
                    String methodDescriptor,
                    boolean isInterface) {
                  addTypeRef(owner, projectTypes, refs);
                  for (Type argType : Type.getArgumentTypes(methodDescriptor)) {
                    addTypeRef(argType.getInternalName(), projectTypes, refs);
                  }
                  addTypeRef(Type.getReturnType(methodDescriptor).getInternalName(), projectTypes, refs);
                }

                @Override
                public void visitFieldInsn(
                    int opcode, String owner, String fieldName, String fieldDescriptor) {
                  addTypeRef(owner, projectTypes, refs);
                  addTypeRef(Type.getType(fieldDescriptor).getInternalName(), projectTypes, refs);
                }
              };
            }
          },
          ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }
    return refs;
  }

  private static void addTypeRef(String internalName, Set<String> projectTypes, Set<String> refs) {
    if (internalName == null || internalName.isBlank()) {
      return;
    }
    String qualified = internalName.replace('/', '.');
    if (projectTypes.contains(qualified)) {
      refs.add(qualified);
    }
  }
}
