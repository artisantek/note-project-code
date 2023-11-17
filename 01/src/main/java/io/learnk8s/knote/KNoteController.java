package io.learnk8s.knote;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
public class KNoteController {

    @Autowired
    private NotesRepository notesRepository;

    @Value("${uploadDir}")
    private String uploadDir;

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    @GetMapping("/")
    public String index(Model model) {
        getAllNotes(model);
        return "index";
    }

    @PostMapping("/note")
    public String saveNotes(@RequestParam("image") MultipartFile file,
                            @RequestParam String description,
                            @RequestParam(required = false) String publish,
                            @RequestParam(required = false) String upload,
                            Model model) throws Exception {

        if (publish != null && "Publish".equals(publish)) {
            saveNote(description, model);
            getAllNotes(model);
            return "redirect:/";
        }
        if (upload != null && "Upload".equals(upload)) {
            if (file != null && !file.getOriginalFilename().isEmpty()) {
                String imageUrl = uploadImage(file);
                model.addAttribute("description",
                        description + " ![](" + imageUrl + ")");
            }
            getAllNotes(model);
            return "index";
        }
        return "index";
    }

    private void getAllNotes(Model model) {
        List<Note> notes = notesRepository.findAll();
        Collections.reverse(notes);
        model.addAttribute("notes", notes);
    }

    private String uploadImage(MultipartFile file) throws Exception {
        File uploadsDir = new File(uploadDir);
        if (!uploadsDir.exists()) {
            uploadsDir.mkdirs();
        }
        String fileId = UUID.randomUUID().toString() + "." +
                file.getOriginalFilename().split("\\.")[1];
        File destFile = new File(uploadDir + fileId);
        file.transferTo(destFile);
        return "/uploads/" + fileId;
    }

    private void saveNote(String description, Model model) {
        if (description != null && !description.trim().isEmpty()) {
            Node document = parser.parse(description.trim());
            String html = renderer.render(document);
            notesRepository.save(new Note(null, html));
            model.addAttribute("description", "");
        }
    }
}
