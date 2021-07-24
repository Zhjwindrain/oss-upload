package com.nuist.oss.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileService {
    @Autowired FileReps fileReps;

    public File save(File file){
        return fileReps.save(file);
    }

    public File findByLocalName(String localName){
        return fileReps.findByLocalName(localName);
    }

    public File findByRandomName(String randomName){
        return fileReps.findByRandomName(randomName);

    }
    public File findByFileType(String fileType){
        return fileReps.findByFileType(fileType);
    }

    public List<File> findAll(){
        return fileReps.findAll();
    }
}


