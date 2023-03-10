package ru.hogwarts.schoolhog.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.hogwarts.schoolhog.model.Avatar;
import ru.hogwarts.schoolhog.model.Student;
import ru.hogwarts.schoolhog.repositories.AvatarRepository;
import ru.hogwarts.schoolhog.repositories.FacultyRepository;
import ru.hogwarts.schoolhog.repositories.StudentRepository;

import javax.imageio.ImageIO;
import javax.transaction.Transactional;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

@Service
@Transactional
public class AvatarService {
    private final AvatarRepository avatarRepository;
    private final StudentRepository studentRepository;


    public AvatarService(AvatarRepository avatarRepository,StudentRepository studentRepository) {
        this.avatarRepository = avatarRepository;
        this.studentRepository=studentRepository;
    }
    @Value("${path.to.avatars.folder}")
    private String avatarsDir;
    public void uploadAvatar(Long studentId, MultipartFile avatarFile) throws IOException {
        Student student = studentRepository.getStudentById(studentId);
        Path filePath = Path.of(avatarsDir, student + "." + getExtensions(avatarFile.getOriginalFilename()));
        Files.createDirectories(filePath.getParent());
        Files.deleteIfExists(filePath);
        try (
                InputStream is = avatarFile.getInputStream();
                OutputStream os = Files.newOutputStream(filePath, CREATE_NEW);
                BufferedInputStream bis = new BufferedInputStream(is, 1024);
                BufferedOutputStream bos = new BufferedOutputStream(os, 1024);
        ) {
            bis.transferTo(bos);
        }
        Avatar avatar = findAvatar(studentId);
        avatar.setStudent(student);
        avatar.setFilePath(filePath.toString());
        avatar.setFileSize(avatarFile.getSize());
        avatar.setMediaType(avatarFile.getContentType());
        avatar.setData(generateImagePreview(filePath));
        avatarRepository.save(avatar);
    }

    public Avatar findAvatar(Long studentId) {
        return avatarRepository.findByStudentId(studentId).orElse(new Avatar());
    }
    private String getExtensions(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private byte[] generateImagePreview(Path filePath) throws IOException {
        try (InputStream is = Files.newInputStream(filePath);
             BufferedInputStream bis = new BufferedInputStream(is, 1024);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BufferedImage image = ImageIO.read(bis);
            int height = image.getHeight()/(image.getWidth()/100);
            BufferedImage preview = new BufferedImage(100,height,image.getType());

            Graphics2D graphics = preview.createGraphics();
            graphics.drawImage(image,0,0,100,height,null);
            graphics.dispose();

            ImageIO.write(preview,getExtensions(filePath.getFileName().toString()),baos);
            return  baos.toByteArray();
        }

    }

    public Collection<Avatar> findAll(Integer pageNumber, Integer pageSize) {
        var pageRequest = PageRequest.of(pageNumber-1,pageSize);
        return avatarRepository.findAll(pageRequest).getContent();
    }
}
