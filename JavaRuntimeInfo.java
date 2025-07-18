import java.io.*;
import java.util.*;

public class JavaRuntimeInfo {
    public static void main(String[] args) {
        String outFile = null;
        for (String arg : args) {
            if (arg.startsWith("--out=")) {
                outFile = arg.substring(6);
            }
        }

        String[] keys = {
                "java.runtime.version",
                "java.vm.specification.version",
                "java.vendor",
                "java.vendor.version",
                "java.version",
                "java.vm.version",
                "java.vm.name",
                "java.version.date",
                "java.vm.vendor"
        };
        Properties props = new Properties();
        for (String key : keys) {
            String val = System.getProperty(key);
            if (val == null) {
                props.setProperty(key, "unavailable");
            } else {
                props.setProperty(key, val);
            }
        }

        // Output to stdout
        try {
            props.store(new OutputStreamWriter(System.out, "ISO-8859-1"),
                    "Java runtime properties");
        } catch (IOException e) {
            // Shouldn't happen with stdout
        }

        // Write to file if requested
        if (outFile != null) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(outFile);
                props.store(fos, "Java runtime properties");
                System.err.println("Info: saved properties to " + outFile);
            } catch (IOException ex) {
                System.err.println("Error: unable to write to '" + outFile + "': " + ex.getMessage());
            } finally {
                try {
                    if (fos != null)
                        fos.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
}
