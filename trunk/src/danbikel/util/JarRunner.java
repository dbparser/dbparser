package danbikel.util;

import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.lang.reflect.InvocationTargetException;

/**
 * Runs a jar application from a URL. Usage is
 * <pre>
 *    java JarRunner mainClass url [args...]
 * </pre>
 * where <code>url</code> is the URL of the jar file and args is optional
 * arguments to be passed to the application's main method.  Modified from a
 * version taken from Sun's Java website
 * (<a target="_blank" href="http://developer.java.sun.com/developer/Books/JAR/api/jarrunner.html">The JarRunner Class</a>).
 * @see JarClassLoader
 */
public class JarRunner {
    public static void main(String[] args) {
	if (args.length < 2) {
	    usage();
	}
	URL url = null;
	try {
	    url = new URL(args[0]);
	} catch (MalformedURLException e) {
	    fatal("Invalid URL: " + args[0]);
	}
	// Create the class loader for the application jar file
	JarClassLoader cl = new JarClassLoader(url);

	// set this thread's class loader to be the JarClassLoader above
	Thread.currentThread().setContextClassLoader(cl);

	// Get the application's main class name
	String name = args[1];
	// Get arguments for the application
	String[] newArgs = new String[args.length - 2];
	System.arraycopy(args, 2, newArgs, 0, newArgs.length);
	// Invoke application's main class
	try {
	    cl.invokeClass(name, newArgs);
	} catch (ClassNotFoundException e) {
	    fatal("Class not found: " + name);
	} catch (NoSuchMethodException e) {
	    fatal("Class does not define a 'main' method: " + name);
	} catch (InvocationTargetException e) {
	    e.getTargetException().printStackTrace();
	    System.exit(1);
	}
    }

    private static void fatal(String s) {
	System.err.println(s);
	System.exit(1);
    }

    private static void usage() {
	fatal("Usage: java MyJarRunner url mainClass [args..]");
    }
}
