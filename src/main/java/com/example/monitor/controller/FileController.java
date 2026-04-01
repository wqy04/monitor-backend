package com.example.monitor.controller;

import com.example.monitor.dto.Result;
import com.example.monitor.entity.FileManage;
import com.example.monitor.service.FileManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/monitor/api/files")
public class FileController {

    @Autowired
    private FileManageService fileManageService;

    @GetMapping("/list")
    public Result<Map<String, Object>> listFiles(@RequestParam("clusterId") Integer clusterId,
                                                  @RequestParam(value = "path", required = false, defaultValue = "/") String path) {
        List<FileManage> files = fileManageService.list();
        List<Map<String, Object>> items = files.stream()
                .filter(f -> f.getClusterId().equals(clusterId))
                .map(f -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", f.getFileName());
                    item.put("type", "file");
                    item.put("size", f.getFileSize());
                    item.put("modifyTime", f.getUploadTime());
                    return item;
                }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("currentPath", path);
        data.put("items", items);
        return Result.ok(data);
    }

    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadFile(@RequestParam("clusterId") Integer clusterId,
                                                  @RequestParam(value = "path", required = false, defaultValue = "/") String path,
                                                  @RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return Result.fail(400, "file required");
        }
        String fileName = file.getOriginalFilename();
        if (!StringUtils.hasText(fileName)) {
            return Result.fail(400, "invalid file name");
        }

        File localDir = new File("uploaded-files" + path);
        if (!localDir.exists()) {
            localDir.mkdirs();
        }
        File localFile = new File(localDir, fileName);
        file.transferTo(localFile);

        FileManage fileManage = new FileManage();
        fileManage.setClusterId(clusterId);
        fileManage.setFileName(fileName);
        fileManage.setFilePath(path + (path.endsWith("/") ? "" : "/") + fileName);
        fileManage.setFileSize(file.getSize());
        fileManage.setUploadTime(LocalDateTime.now());
        fileManageService.save(fileManage);

        Map<String, Object> data = new HashMap<>();
        data.put("fileName", fileName);
        data.put("filePath", fileManage.getFilePath());
        data.put("fileSize", file.getSize());

        return Result.ok("文件上传成功", data);
    }

    @GetMapping("/download")
    public void downloadFile(@RequestParam("clusterId") Integer clusterId,
                             @RequestParam("path") String path,
                             HttpServletResponse response) throws IOException {
        File file = new File("uploaded-files" + path);
        if (!file.exists()) {
            response.setStatus(404);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":404,\"message\":\"文件不存在\",\"data\":null}");
            return;
        }
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
        try (FileInputStream in = new FileInputStream(file); OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
    }

    @DeleteMapping("/delete")
    public Result<Void> deleteFile(@RequestParam("clusterId") Integer clusterId,
                                   @RequestParam("path") String path) {
        FileManage fileManage = fileManageService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileManage>()
                .eq(FileManage::getClusterId, clusterId)
                .eq(FileManage::getFilePath, path));
        if (fileManage == null) {
            return Result.fail(404, "文件记录不存在");
        }

        File file = new File("uploaded-files" + path);
        if (file.exists()) {
            file.delete();
        }
        fileManageService.removeById(fileManage.getFileId());
        return Result.ok("删除成功", null);
    }
}
