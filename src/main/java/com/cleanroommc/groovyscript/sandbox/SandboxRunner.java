package com.cleanroommc.groovyscript.sandbox;

import com.cleanroommc.groovyscript.GroovyScript;
import com.cleanroommc.groovyscript.sandbox.interception.SandboxSecurityException;
import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import net.minecraft.client.Minecraft;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

public class SandboxRunner {

    public static boolean run() {
        try {
            runScript();
            return true;
        } catch (IOException | ScriptException | ResourceException e) {
            GroovyLog.LOG.error("An Exception occurred trying to run groovy!");
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            GroovyLog.LOG.exception(e);
            return false;
        }
    }

    public static void runScript() throws IOException, ScriptException, ResourceException, SandboxSecurityException {
        GroovyLog.LOG.info("Running scripts");
        URL[] urls = getURLs();
        GroovyScript.LOGGER.info("URLs: {}", Arrays.toString(urls));
        GroovyScriptEngine engine = new GroovyScriptEngine(urls);
        CompilerConfiguration config = new CompilerConfiguration(CompilerConfiguration.DEFAULT);
        config.addCompilationCustomizers(new SandboxTransformer());

        engine.setConfig(config);
        Binding binding = new Binding();

        for (File file : getStartupFiles()) {
            GroovyLog.LOG.info(" - executing %s", file.toString());
            engine.run(file.toString(), binding);
        }
    }

    private static File[] getStartupFiles() throws IOException {
        Path path = GroovyScript.startupPath.toPath();
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        }
        File[] files = GroovyScript.startupPath.listFiles();
        if (files == null || files.length == 0) {
            Files.createFile(new File(path + "/main.groovy").toPath());
        }
        files = GroovyScript.startupPath.listFiles();
        Path mainPath = new File(GroovyScript.scriptPath).toPath();
        return Arrays.stream(files).map(file -> mainPath.relativize(file.toPath()).toFile()).toArray(File[]::new);
    }

    public static URL[] getURLs() throws MalformedURLException {
        return new URL[]{
                new File(GroovyScript.scriptPath).toURI().toURL(),
                getBasePathFor(GroovyScript.class),
                getBasePathFor(Minecraft.class)
        };
    }

    public static URL getBasePathFor(Class<?> clazz) throws MalformedURLException {
        String className = clazz.getName().replace(".", "/") + ".class";
        ClassLoader loader = clazz.getClassLoader();
        String url = Objects.requireNonNull(loader.getResource(className)).toString();
        url = url.substring(0, url.indexOf(className));
        return new URL(url);
    }

    public static void logError(String msg, String sourceName, int lineNumber) throws SandboxSecurityException {
        throw new SandboxSecurityException("SandboxSecurityException: " + msg + " in script " + relativizeSource(sourceName) + " in line " + lineNumber);
    }

    public static String relativizeSource(String source) {
        try {
            Path path = Paths.get(new URL(source).toURI());
            Path mainPath = new File(GroovyScript.scriptPath).toPath();
            return mainPath.relativize(path).toString();
        } catch (URISyntaxException | MalformedURLException e) {
            e.printStackTrace();
            return source;
        }
    }
}