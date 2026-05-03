package com.example.fileservice.file.controller;

import com.example.fileservice.file.dto.FileMetaResponse;
import com.example.fileservice.file.service.FileService;
import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileMetaResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(fileService.upload(file));
    }

    @GetMapping("/{id}/meta")
    public ResponseEntity<FileMetaResponse> meta(@PathVariable long id) {
        return ResponseEntity.ok(fileService.getMeta(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable long id) {
        FileService.DownloadFile download = fileService.download(id);
        String contentType = download.meta().getContentType();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.meta().getOriginalFilename() + "\"")
                .body(download.resource());
    }
}

