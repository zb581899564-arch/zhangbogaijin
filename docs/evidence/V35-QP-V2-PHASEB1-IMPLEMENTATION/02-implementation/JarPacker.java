import java.io.*;
import java.nio.file.*;
import java.util.jar.*;

public class JarPacker {
  public static void main(String[] args) throws Exception {
    File srcDir = new File(args[0]);
    File jarOut = new File(args[1]);
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    try (JarOutputStream target = new JarOutputStream(new FileOutputStream(jarOut), manifest)) {
      addDir(srcDir, srcDir, target);
    }
  }

  private static void addDir(File root, File source, JarOutputStream target) throws IOException {
    for (File nestedFile : source.listFiles()) {
      if (nestedFile.isDirectory()) {
        String name = root.toPath().relativize(nestedFile.toPath()).toString().replace('\\', '/') + "/";
        JarEntry entry = new JarEntry(name);
        entry.setTime(nestedFile.lastModified());
        target.putNextEntry(entry);
        target.closeEntry();
        addDir(root, nestedFile, target);
      } else {
        String name = root.toPath().relativize(nestedFile.toPath()).toString().replace('\\', '/');
        JarEntry entry = new JarEntry(name);
        entry.setTime(nestedFile.lastModified());
        target.putNextEntry(entry);
        try (InputStream in = new BufferedInputStream(new FileInputStream(nestedFile))) {
          byte[] buffer = new byte[8192];
          int count;
          while ((count = in.read(buffer)) != -1) {
            target.write(buffer, 0, count);
          }
        }
        target.closeEntry();
      }
    }
  }
}