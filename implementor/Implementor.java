package ru.ifmo.rain.menshutin.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;


/**
 * Implementation of {@link JarImpler}
 * <p>
 * This class produces code implementing class or interface and creates jar file containing it.
 *
 * @author Alexey Menshutin
 */

public class Implementor implements Impler, JarImpler {

    /**
     * Constants for generating code of implementation, it equals to TAB.
     */
    private final static String TAB = "\t";

    /**
     * Constant for generating code of implementation, it equals to one space.
     */
    private final static String SPC = " ";

    /**
     * Constant for generating code of implementation, it equals to <code>System.lineSeparator</code>.
     */
    private final static String NEWLINE = System.lineSeparator();


    /**
     * Constant for generating code of implementation, it equals to one comma.
     */
    private final static String COMMA = ",";

    /**
     * Constant for generating code of implementation, it equals to one semicolon.
     */
    private final static String SEMI = ";";

    /**
     * Constant for generating code of implementation, it equals to one left round bracket.
     */
    private final static String BRL = "(";

    /**
     * Constant for generating code of implementation, it equals to one right round bracket.
     */
    private final static String BRR = ")";

    /**
     * Constant for generating code of implementation, it equals to one left curly bracket.
     */
    private final static String CBRL = "{";

    /**
     * Constant for generating code of implementation, it equal one right curly bracket.
     */
    private final static String CBRR = "}";

    /**
     * Gets console argument and executes implementation.
     * Two mode is possible:
     * <ul>
     * <li> 2 arguments: className rootPath - call {@link #implement(Class, Path)} with given arguments.
     * It's generate implementation of class or interface</li>
     * <li> 3 arguments: -jar className jarPath - call {@link #implementJar(Class, Path)} with two last arguments.
     * It's generate jar file with implementation of class or interface. </li>
     * </ul>
     * If arguments are incorrect or an error occurs during implementation returns message with information about error.
     *
     * @param args arguments for running an application.
     */
    public static void main(String[] args) {

        if (args == null || (args.length != 2 && args.length != 3)) {
            System.err.println("Two or three arguments expected");
            return;
        }

        for (var arg : args) {
            if (arg == null) {
                System.err.println("Expected non-null arguments");
                return;
            }
        }

        JarImpler implementor = new Implementor();
        try {
            if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else if (!args[0].equals("-jar")) {
                System.err.println("\'" + args[0] + "\'" + " -- unknown argument, -jar expected.");
            } else {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            }
        } catch (ClassNotFoundException e) {
            error("Invalid class-name in the first argument", e);
        } catch (InvalidPathException e) {
            error("Invalid path in the second argument", e);
        } catch (ImplerException e) {
            error("An error occurred during implementation", e);
        }
    }

    /**
     * Create new instance.
     */
    public Implementor() {
    }

    /**
     * Returns name for implementing class.
     *
     * @param aClass Class for implementation.
     * @return Simple name of given class with "Impl" suffix.
     */
    private String getClassName(Class<?> aClass) {
        return aClass.getSimpleName() + "Impl";
    }

    /**
     * Returns default value for given class.
     *
     * @param aClass A class to get default value.
     * @return Default value for given class.
     */
    private String getDefaultValue(Class<?> aClass) {
        if (aClass.equals(boolean.class)) {
            return " false";
        } else if (aClass.equals(void.class)) {
            return "";
        } else if (aClass.isPrimitive()) {
            return " 0";
        } else {
            return " null";
        }
    }

    /**
     * Resolve path file: convernt package to path and append extension;
     *
     * @param path   path file with code
     * @param aClass reflection of implementing class.
     * @param end    string representation of extension.
     * @return path to file
     */
    private Path resolveFilePath(Path path, Class<?> aClass, String end) {
        return path.resolve(aClass.getPackage()
                                  .getName()
                                  .replace('.', File.separatorChar))
                   .resolve(getClassName(aClass) + end);
    }

    /**
     * Create file for code implementation and directories on path to file.
     *
     * @param aClass reflection of implementing class.
     * @param path   path to implementing class.
     * @return writer on new file.
     * @throws IOException if occurred problem with create file or directories.
     */
    private BufferedWriter createFile(Class<?> aClass, Path path) throws IOException {
        Path pathFile = resolveFilePath(path, aClass, ".java");
        if (pathFile.getParent() != null) {
            Files.createDirectories(pathFile.getParent());
        }
        Files.deleteIfExists(pathFile);
        Files.createFile(pathFile);
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathFile.toString()), StandardCharsets.UTF_8));
    }

    /**
     * Generate the first part of <code>aClass</code>'s implementation. It contains:
     * <ul>
     * <li>package's declaration</li>
     * <li>class` declaration</li>
     * </ul>
     *
     * @param aClass reflection of implementing class.
     * @return string representation of the the first part of generating code.
     */
    private String genHead(Class<?> aClass) {
        StringBuilder result = new StringBuilder();
        if (aClass.getPackage() != null) {
            result.append("package" + SPC)
                  .append(aClass.getPackage().getName())
                  .append(SEMI)
                  .append(NEWLINE)
                  .append(NEWLINE);
        }
        result.append("public")
              .append(SPC)
              .append("class")
              .append(SPC)
              .append(getClassName(aClass))
              .append(SPC)
              .append(aClass.isInterface() ? "implements" : "extends")
              .append(SPC)
              .append(aClass.getSimpleName())
              .append(SPC)
              .append(CBRL)
              .append(NEWLINE);

        return result.toString();
    }

    /**
     * Generate last part of <code>aClass</code>'s implementation. Example, last back curly bracket.
     *
     * @param aClass reflection of implementing class.
     * @return string representation of last part of generating code.
     */
    private String getTail(Class<?> aClass) {
        return CBRR;
    }

    /**
     * add reflection of abstract methods form array to set.
     *
     * @param methods array of reflection of method.
     * @param storage set, witch collected abstract methods form <code>methods</code>
     */
    private void addToMethodStorage(Method[] methods, Set<Method> storage) {
        Arrays.stream(methods)
              .filter(method -> Modifier.isAbstract(method.getModifiers()))
              .collect(Collectors.toCollection(() -> storage));
    }

    /**
     * Generate implementation of given class's method.
     *
     * @param aClass reflection of implementing class.
     * @return string representation code of <code>aClass</code>'s abstract methods.
     */

    private String genAbstractMethods(Class<?> aClass) {
        var result = new StringBuilder();
        Set<Method> methods = new TreeSet<>(Comparator.comparingInt(
                method -> (method.getName() + Arrays.toString(method.getParameterTypes())).hashCode()));

        addToMethodStorage(aClass.getMethods(), methods);
        if (!aClass.isInterface()) {
            for (Class<?> token = aClass; token != null; token = token.getSuperclass()) {
                addToMethodStorage(token.getDeclaredMethods(), methods);
            }
        }

        return getMethodsParameters(result, methods);
    }

    /**
     * Generate implementation of given class` constructor or method.
     *
     * @param func       reflection of method or constructor.
     * @param returnType string representation of <code>func</code>'s return type. Example, for constructor it's must be empty.
     * @param funcName   string representation of <code>func</code>`s name.
     * @return string representation of this <code>func</code>.
     */
    private String genExecutable(Executable func, String returnType, String funcName) {
        var result = new StringBuilder();

        //TODO()
        result.append(TAB)
              .append(TAB + Modifier.toString(func.getModifiers() & ~(Modifier.ABSTRACT | Modifier.TRANSIENT | Modifier.VOLATILE)) + SPC)
              .append(returnType)
              .append(SPC)
              .append(funcName)
              .append(getParametersExecutable(func))
              .append(SPC)
              .append(getExceptionsExecutable(func))
              .append(CBRL)
              .append(NEWLINE);

        return result.toString();
    }

    /**
     * Generate implementation of executable object's parameters.
     *
     * @param func executable object.
     * @return string representation of <code>func</code>'s parameters.
     */

    private String getParametersExecutable(Executable func) {
        return Arrays.stream(func.getParameters())
                     .map(parameter -> parameter.getType().getCanonicalName() + SPC + parameter.getName())
                     .collect(Collectors.joining(COMMA + SPC, BRL, BRR));
    }

    /**
     * Generate implement of specifying exceptions for executable object.
     *
     * @param func executable object.
     * @return string representation of specifying exceptions thrown by <code>func</code>.
     */
    private String getExceptionsExecutable(Executable func) {
        if (func.getExceptionTypes().length == 0) {
            return "";
        }
        return Arrays.stream(func.getExceptionTypes())
                     .map(Class::getCanonicalName)
                     .collect(Collectors.joining(COMMA + SPC, "throws" + SPC, SPC)); //TODO()
    }

    /**
     * Generate implementation of given class' constructors.
     *
     * @param aClass reflection of implementing class.
     * @return string representation code of <code>aClass</code>'s constructors.
     */
    private String getConstructors(Class<?> aClass) {
        var result = new StringBuilder();
        if (!aClass.isInterface()) {
            for (var constructor : aClass.getDeclaredConstructors()) {
                if (!Modifier.isPrivate(constructor.getModifiers())) {
                    result.append(genExecutable(constructor, "", getClassName(aClass)))
                          .append(TAB + TAB + "super")
                          .append(Arrays.stream(constructor.getParameters())
                                        .map(Parameter::getName)
                                        .collect(Collectors.joining(COMMA + SPC, BRL, BRR)) + SEMI + NEWLINE)
                          .append(TAB + CBRR + NEWLINE)
                          .append(NEWLINE);

                }
            }
        }
        return result.toString();
    }


    /**
     * Returns implementation of given methods.
     *
     * @param result  {@link StringBuilder} for containing string representation.
     * @param methods set of methods of implementing class.
     * @return string representation code of given methods.
     */
    private String getMethodsParameters(StringBuilder result, Set<Method> methods) {
        for (var method : methods) {
            result.append(genExecutable(method, method.getReturnType().getCanonicalName(), method.getName()))
                  .append(TAB + TAB + "return" + getDefaultValue(method.getReturnType()) + SEMI + NEWLINE)
                  .append(TAB + CBRR + NEWLINE)
                  .append(NEWLINE);
        }

        return result.toString();
    }

    /**
     * Generates implementation of a class denoted by the provided type token and creates a <code>.jar</code>
     * file which contains that implementation in the provided path.
     *
     * @param aClass target type token
     * @param path target path
     * @throws ImplerException if:
     * <ul>
     *     <li>One or more arguments are <code>null</code></li>
     *     <li>Target class can't be extended</li>
     *     <li>An internal {@link IOException} has occurred when handling I/O processes</li>
     *     <li>There are no callable constructors in the target class</li>
     * </ul>
     */
    @Override
    public void implement(Class<?> aClass, Path path) throws ImplerException {
        if (aClass == null || path == null) {
            throw new ImplerException("Incorrect type");
        }

        if (aClass.isPrimitive() || aClass.isArray() || Modifier.isFinal(aClass.getModifiers()) || aClass == Enum.class) {
            throw new ImplerException("Incorrect type");
        }

        try (BufferedWriter code = createFile(aClass, path)) {
            try {
                code.write(genHead(aClass) + NEWLINE);
                if (!aClass.isInterface()) {
                    String stringOfConstructors = getConstructors(aClass);
                    if (stringOfConstructors.isEmpty()) {
                        throw new ImplerException("Couldn't access constructors of super class");
                    }
                    code.write(stringOfConstructors);
                }
                code.write(genAbstractMethods(aClass));
                code.write(getTail(aClass));
            } catch (IOException e) {
                throw new ImplerException("Problems with writing code", e);
            }
        } catch (IOException e) {
            throw new ImplerException("Problems with creating or closing generated file", e);
        }

    }

    /**
     * Generates implementation of a class denoted by the provided type token and creates a <code>.jar</code>
     * file which contains that implementation in the provided path.
     *
     * @param aClass target type token
     * @param path target path
     * @throws ImplerException if:
     * <ul>
     *     <li>One or more arguments are <code>null</code></li>
     *     <li>Target class can't be extended</li>
     *     <li>An internal {@link IOException} has occurred when handling I/O processes</li>
     *     <li>{@link javax.tools.JavaCompiler} failed to compile target source file</li>
     *     <li>There are no callable constructors in the target class</li>
     * </ul>
     */
    @Override
    public void implementJar(Class<?> aClass, Path path) throws ImplerException {
        if (aClass == null || path == null) {
            throw new ImplerException("Required non-null arguments");
        }
        if (aClass.isPrimitive() || aClass.isArray() || Modifier.isFinal(aClass.getModifiers()) || aClass == Enum.class) {
            throw new ImplerException("Incorrect type");
        }
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new ImplerException("Problems with creating directories for temp files", e);
            }
        }

        Path tempDir;
        try {
            tempDir = Files.createTempDirectory(path.toAbsolutePath().getParent(), "temp");
        } catch (IOException e) {
            throw new ImplerException("Problems with creating temp directories", e);
        }

        implement(aClass, tempDir);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Required non-null compiler");
        }

        String[] args = new String[]{
                "-cp",
                tempDir.toString() + File.pathSeparator + getClassPath(),
                resolveFilePath(tempDir, aClass, ".java").toString()
        };
        if (compiler.run(null, null, null, args) != 0) {
            throw new ImplerException("Problems with compiling generative file");
        }
//        compile(tempDir, aClass);

        var manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR, "Menshutin Alexey");

        try (var jarOutputStream = new JarOutputStream(Files.newOutputStream(path), manifest)) {
            try {
                jarOutputStream.putNextEntry(new ZipEntry(aClass.getName().replace('.', '/') + "Impl.class"));
                Files.copy(resolveFilePath(tempDir, aClass, ".class"), jarOutputStream);
            } catch (IOException e) {
                throw new ImplerException("Problems with writing to jar-class");
            }
        } catch (IOException e) {
            throw new ImplerException("Problems with creating or closing jar file");
        } finally {
            try {
                clean(tempDir);
            } catch (IOException e) {
                error("Problems with cleaning", e);
            }
        }
    }

    /**
     * Remove all file and directories from given file subtree.
     *
     * @param tempDir path root removed subtree.
     * @throws IOException if an I/O error occurs.
     */
    private void clean(Path tempDir) throws IOException {
        FileVisitor<Path> visitor = new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(tempDir, visitor);
    }

    /**
     * Print in stdout custom message and exception's message.
     *
     * @param msg custom message
     * @param e   exception
     */
    private static void error(String msg, Exception e) {
        System.err.println(msg);
        System.err.println("Exception's message: " + e.getMessage());
    }

    /**
     * Returns path to .jar that contains {@link JarImpler} class.
     *
     * @return {@link String} representation of path to .jar archive.
     */
    private static String getClassPath() {
        return JarImpler.class.getProtectionDomain().getCodeSource().getLocation().toString();
    }

}

