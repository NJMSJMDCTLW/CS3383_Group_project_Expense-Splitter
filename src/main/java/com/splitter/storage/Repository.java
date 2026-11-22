package com.splitter.storage;

import com.splitter.exception.StorageException;
import com.splitter.meta.AIGenerated;
import com.splitter.model.Group;
import java.nio.file.Path;

/** Saves and loads a whole group (members, expenses and the next expense id). */
@AIGenerated(tool = "Claude Code", prompt = "P05")
public interface Repository {

    /** @throws StorageException on an I/O failure */
    void save(Group g, Path file) throws StorageException;

    /** @throws StorageException if the file is missing, unreadable, empty, of a wrong version or corrupt */
    Group load(Path file) throws StorageException;
}
