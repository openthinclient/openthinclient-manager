package org.openthinclient.pkgmgr;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * A provider for test data directories. This provider serves as a utility to provide other unit
 * tests with per-test temporary directories in the <code>target</code> folder of the build. <p>
 * Note that this class also contains unit tests to ensure that the functionality provided in this
 * class is actually working. Those tests are not testing any production logic of the package
 * manager </p>
 */
public class TestDirectoryProvider {

    public static Path get() {

        StackTraceElement caller = determineCaller();

        return get(caller.getClassName(), caller.getMethodName());
    }

    public static Path get(String className, String methodName) {

        if (className.indexOf('.') != -1)
            className = className.substring(className.lastIndexOf('.') + 1);

        // special treatment for / in names, as Lambdas tend to have them
        if (className.contains("/"))
            className = className.replace('/', '_');

        final Path testDataRoot = Paths.get("target", "test-data");


        int i = 0;
        Path path;
        do {
            path = testDataRoot.resolve(className + "_" + methodName + (i > 0 ? "-" + i : ""));
            i++;
        } while (Files.exists(path));

        return path;

    }

    private static StackTraceElement determineCaller() {
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // skipping the first entry as it will be Thread.getStackTrace();
        int i = 1;
        while (i < stackTrace.length) {

            final StackTraceElement element = stackTrace[i];

            if (element.getClassName().endsWith(TestDirectoryProvider.class.getSimpleName())) {
                i++;
            } else {
                // found the caller element
                return element;
            }

        }

        // we should never reach this state at all!
        throw new RuntimeException("Failed to determine caller");

    }


    @Test
    public void testDetermineCaller() throws Exception {

        class MyCallable1 implements Callable<StackTraceElement> {
            @Override
            public StackTraceElement call() throws Exception {
                return determineCaller();
            }
        }
        ;

        final StackTraceElement element = new MyCallable1().call();
        assertEquals("org.openthinclient.pkgmgr.TestDirectoryProvider$1MyCallable1", element.getClassName());
        assertEquals("call", element.getMethodName());
    }

    @Test
    public void testDetermineLambda() throws Exception {

        final Callable<StackTraceElement> callable = TestDirectoryProvider::determineCaller;

        final StackTraceElement element = callable.call();
        assertThat(element.getClassName(), startsWith("org.openthinclient.pkgmgr.TestDirectoryProvider$$Lambda"));
        assertEquals("call", element.getMethodName());
    }

    @Test
    public void testGet() throws Exception {

        class MyCallable2 implements Callable<Path> {
            @Override
            public Path call() throws Exception {
                return get();
            }
        }
        ;

        final Path element = new MyCallable2().call();
        assertEquals("TestDirectoryProvider$1MyCallable2_call", element.getFileName().toString());
    }

    @Test
    public void testGetLambda() throws Exception {

        final Callable<Path> callable = TestDirectoryProvider::get;

        final Path element = callable.call();
        assertThat(element.getFileName().toString(), endsWith("_call"));
        assertThat(element.getFileName().toString(), startsWith("TestDirectoryProvider$$Lambda$"));
    }

}
