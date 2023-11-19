package io.learnk8s.knote;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
public class KNoteController {

    private final NotesRepository notesRepository;
    private final AmazonS3 s3Client;
    private final String bucketName;

    @Autowired
    public KNoteController(NotesRepository notesRepository, AmazonS3 s3Client,
                           @Value("${aws.s3.bucketName}") String bucketName) {
        this.notesRepository = notesRepository;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @GetMapping("/")
    public String index(Model model) {
        getAllNotes(model);
        return "index";
    }

    @GetMapping("/notes")
    public String displayNotes(Model model) {
        getAllNotes(model);
        return "notes";
    }

    @PostMapping("/note")
    public String saveNote(@RequestParam("image") MultipartFile file,
                           @RequestParam String description,
                           Model model) throws IOException {
        if (!file.isEmpty()) {
            uploadImage(file, description, model);
        }
        saveNote(description);
        return "redirect:/notes";
    }

    private void getAllNotes(Model model) {
        List<Note> notes = notesRepository.findAll();
        Collections.reverse(notes);
        model.addAttribute("notes", notes);
    }

    private void uploadImage(MultipartFile file, String description, Model model) throws IOException {
        String fileName = generateFileName(file);
        File fileObj = convertMultiPartFileToFile(file);
        s3Client.putObject(new PutObjectRequest(bucketName, fileName, fileObj));
        fileObj.delete(); // Delete the local file after upload

        String imageUrl = "https://" + bucketName + ".s3.amazonaws.com/" + fileName;
        description += " ![](" + imageUrl + ")";
        model.addAttribute("description", description);
    }

    private File convertMultiPartFileToFile(MultipartFile file) throws IOException {
        File convertedFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convertedFile)) {
            fos.write(file.getBytes());
        }
        return convertedFile;
    }

    private String generateFileName(MultipartFile multiPart) {
        return UUID.randomUUID() + "_" + multiPart.getOriginalFilename().replace(" ", "_");
    }

    private void saveNote(String description) {
        Note note = new Note();
        note.setDescription(description);
        notesRepository.save(note);
    }
}
