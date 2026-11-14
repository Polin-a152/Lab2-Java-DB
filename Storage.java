import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Storage {
    private String fileName;

    public Storage(String fileName) {
        this.fileName = fileName;
    } 
    public boolean exists() {
        return new File(fileName).exists();
    }
    public void save(Database db) throws Exception {
        File file = new File(fileName);
        File parent = file.getParentFile();

        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        File temp = new File(fileName + ".tmp");
        try (ObjectOutputStream out =
                     new ObjectOutputStream(new FileOutputStream(temp))) {
            out.writeObject(db);
            out.flush();
        }

        try {
            Files.move(temp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            Files.move(temp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Database load() throws Exception {
        try (ObjectInputStream in =
                     new ObjectInputStream(new FileInputStream(fileName))) {
            Database db = (Database) in.readObject();
            db.rebuildIndexes();
            return db;
        }
    }
}
