package com.nuist.oss.controller;


import com.nuist.oss.model.File;
import com.nuist.oss.model.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
public class SaveFileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/save")
    public String saveFile(@RequestParam("local_name") String local_name,
                           @RequestParam("random_name") String random_name,
                           @RequestParam("file_type") String file_type) {

        File file = new File();
        file.setFileType(file_type);
        file.setLocalName(local_name);
        file.setRandomName(random_name);

        if(fileService.findByLocalName(local_name)==null){
            fileService.save(file);
            return "save_success";
        }
        return "repeated_name";

    }

    @GetMapping("/show")
    @CrossOrigin
    public List<File> showFile(){
        return fileService.findAll();
    }
}


