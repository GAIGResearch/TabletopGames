package utilities;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DirectoryCreation {


    @Test
    public void createNestedDirectoryWithMkDirs() {
        File folder = new File("SubOne\\SubTwo\\SubThree");
        assertFalse(folder.exists());
        Utils.createDirectory("SubOne\\SubTwo\\SubThree");
        assertTrue(folder.isDirectory());
        assertTrue(folder.exists());
        assertTrue((new File("SubOne").exists()));

        folder.delete();
        (new File("SubOne\\SubTwo")).delete();
        (new File("SubOne")).delete();
        assertFalse((new File("SubOne").exists()));
    }
}
