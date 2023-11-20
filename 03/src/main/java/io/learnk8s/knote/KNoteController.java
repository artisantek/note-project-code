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

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
public class KNoteController {

    @Autowired
    private NotesRepository notesRepository;

    @Value("${aws.s3.region}")
    private String s3Region;
    
    @Value("${aws.accessKeyId}")
    private String awsId;

    @Value("${aws.secretKey}")
    private String awsKey;

    @Value("${aws.s3.bucket}")
    private String bucketName;

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
                String imageUrl = uploadImageToS3(file);
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

    private String uploadImageToS3(MultipartFile file) throws Exception {
        String fileId = UUID.randomUUID().toString() + "." +
                file.getOriginalFilename().split("\\.")[1];
        String fileName = "uploads/" + fileId; // Using 'uploads/' as a folder in S3

        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(awsId, awsKey);
        S3Client s3 = S3Client.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                    .region(Region.of(s3Region)) // Use the region from the properties file
                    .build();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                                .bucket(bucketName)
                                                .key(fileName)
                                                .build();

        s3.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        return "https://s3." + s3Region + ".amazonaws.com/" + bucketName + "/" + fileName;
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
