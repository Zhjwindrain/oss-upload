package com.nuist.oss.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileReps extends JpaRepository<File,Long> {
    File save(File file);

    File findByLocalName(String localName);

    File findByRandomName(String randomName);

    File findByFileType(String fileType);

    List<File> findAll();
}
