/*
 * The MIT License
 *
 * Copyright (c) 2011, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.util.io;

import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher.LocalLauncher;
import hudson.util.StreamTaskListener;
import junit.framework.TestCase;
import org.jvnet.hudson.test.Bug;

import java.io.File;
import java.io.FileOutputStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class TarArchiverTest extends TestCase {
    /**
     * Makes sure that permissions are properly stored in the tar file.
     */
    @Bug(9397)
    public void testPermission() throws Exception {
        if (Functions.isWindows())  return; // can't test on Windows

        File tar = File.createTempFile("test","tar");
        File zip = File.createTempFile("test","zip");

        FilePath dir = new FilePath(File.createTempFile("test","dir"));

        try {
            dir.delete();
            dir.child("subdir").mkdirs();

            FilePath f = dir.child("a.txt");
            f.touch(0);
            f.chmod(0755);

            f = dir.child("subdir/b.txt");
            f.touch(0);
            f.chmod(0644);
            int dirMode = dir.child("subdir").mode();

            dir.tar(new FileOutputStream(tar),"**/*");
            dir.zip(new FileOutputStream(zip));


            FilePath e = dir.child("extract");
            e.mkdirs();

            // extract via the tar command
            assertEquals(0, new LocalLauncher(new StreamTaskListener(System.out)).launch().cmds("tar", "xvpf", tar.getAbsolutePath()).pwd(e).join());

            assertEquals(0100755,e.child("a.txt").mode());
            assertEquals(dirMode,e.child("subdir").mode());
            assertEquals(0100644,e.child("subdir/b.txt").mode());


            // extract via the zip command
            e.deleteContents();
            assertEquals(0, new LocalLauncher(new StreamTaskListener(System.out)).launch().cmds("unzip", zip.getAbsolutePath()).pwd(e).join());
            e = e.listDirectories().get(0);

            assertEquals(0100755, e.child("a.txt").mode());
            assertEquals(dirMode,e.child("subdir").mode());
            assertEquals(0100644,e.child("subdir/b.txt").mode());
        } finally {
            tar.delete();
            zip.delete();
            dir.deleteRecursive();
        }
    }
}
