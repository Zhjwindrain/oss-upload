package com.nuist.oss.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import com.nuist.oss.model.FileService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.region.Region;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@CrossOrigin("*")
@RestController
public class ApiController {

    @Autowired
    FileService fileService;

    //注入需要的属性，已经全部配置在yml文件中
    @Value("${oss.accessKey}")
    private String ossId;
    @Value("${oss.secretKey}")
    private String ossSecret;
    @Value("${oss.region}")
    private String ossRegion;
    @Value("${oss.bucketName}")
    private String ossBucketName;

    @Value("${cos.accessKey}")
    private String cosId;
    @Value("${cos.secretKey}")
    private String cosSecret;
    @Value("${cos.region}")
    private String cosRegion;
    @Value("${cos.bucketName}")
    private String cosBucketName;

    /**
     * 腾讯云 cos 后端上传
     * @param key
     * @param localname
     * @param multipartFile
     * @return
     */
    @PostMapping("/cosupload")
    public String cosUpload(@RequestParam("key")String key, @RequestParam("localname")String localname, @RequestParam("file") MultipartFile multipartFile){
        String secretId = cosId;
        String secretKey = cosSecret;
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        String bucketName = cosBucketName;
        String regionName = cosRegion;
        Region region = new Region(regionName);
        ClientConfig clientConfig = new ClientConfig(region);
        COSClient cosClient = new COSClient(cred, clientConfig);
        File file = new File(multipartFile.getOriginalFilename());
        try {
            InputStream ins=multipartFile.getInputStream();
            OutputStream os = new FileOutputStream(file);
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while((bytesRead = ins.read(buffer, 0, 8192)) != -1){
                os.write(buffer, 0, bytesRead);
            }
            os.close(); ins.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        String keys = key;
        com.qcloud.cos.model.PutObjectRequest putObjectRequest = new com.qcloud.cos.model.PutObjectRequest(bucketName, keys, file);

        if(fileService.findByLocalName(localname)==null){
            cosClient.putObject(putObjectRequest);
            cosClient.shutdown();
            return "upload_success";
        }
        else {
            cosClient.shutdown();
            return "repeated_name";
        }




    }

    /**
     * 阿里云 oss 后端上传
     * @param key
     * @param localname
     * @param multipartFile
     * @return
     */
    @PostMapping("/ossupload")
    public String ossUpload(@RequestParam("key")String key,@RequestParam("localname") String localname,@RequestParam("file") MultipartFile multipartFile){
        String endpoint = ossRegion;
        String accessKeyId = ossId;
        String accessKeySecret = ossSecret;
        String bucketName = ossBucketName;
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        File file = new File(multipartFile.getOriginalFilename());
        try {
            InputStream ins=multipartFile.getInputStream();
            OutputStream os = new FileOutputStream(file);
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while((bytesRead = ins.read(buffer, 0, 8192)) != -1){
                os.write(buffer, 0, bytesRead);
            }
            os.close(); ins.close();
        } catch (Exception e){
            e.printStackTrace();
        }

        if(fileService.findByLocalName(localname)==null) {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, file);
            // 上传文件。
            ossClient.putObject(putObjectRequest);
            ossClient.shutdown();
            return "upload_success";
        }
        else {
            ossClient.shutdown();
            return "repeated_name";
        }

    }

    /**
     * 阿里云 oss 后端打包批量下载
     * @param key
     * @param request
     * @param response
     */
    @RequestMapping(value = "/download",method = RequestMethod.GET)
    public void getOssFile(@RequestParam String key, HttpServletRequest request, HttpServletResponse response){


// endpoint以杭州为例，其它region请按实际情况填写，1改为自己的
        String endpoint = ossRegion;
// 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建
        String accessKeyId = ossId;
        String accessKeySecret = ossSecret;
        String bucketName = ossBucketName;
//要下载的文件名（Object Name）字符串，中间用‘,’间隔。文件名从bucket目录开始.5改为自己的

//todo 这个key 是上传时候的key，如下有上传案例的讲解


        try {
// 初始化
            OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);;
//6改为自己的名称
            String fileName = "test.zip";
// 创建临时文件
            File zipFile = File.createTempFile("test", ".zip");
            FileOutputStream f = new FileOutputStream(zipFile);
/**
 * 作用是为任何OutputStream产生校验和
 * 第一个参数是制定产生校验和的输出流，第二个参数是指定Checksum的类型 （Adler32（较快）和CRC32两种）
 */
            CheckedOutputStream csum = new CheckedOutputStream(f, new Adler32());
// 用于将数据压缩成Zip文件格式
            ZipOutputStream zos = new ZipOutputStream(csum);

            String[] keylist = key.split("-");
            for (String ossfile : keylist) {
// 获取Object，返回结果为OSSObject对象
                System.out.println(ossfile);
//                OSSObject ossObject = ossClient.getObject(bucketName, fileService.findByLocalName(ossfile).getRandomName());
                OSSObject ossObject = ossClient.getObject(bucketName, fileService.findByLocalName(ossfile).getRandomName());
// 读去Object内容 返回
                InputStream inputStream = ossObject.getObjectContent();
// 对于每一个要被存放到压缩包的文件，都必须调用ZipOutputStream对象的putNextEntry()方法，确保压缩包里面文件不同名

                zos.putNextEntry(new ZipEntry(ossfile));
                int bytesRead = 0;
// 向压缩文件中输出数据
                while((bytesRead=inputStream.read())!=-1){
                    zos.write(bytesRead);
                }
                inputStream.close();
                zos.closeEntry(); // 当前文件写完，定位为写入下一条项目
            }
            zos.close();
            String header = request.getHeader("User-Agent").toUpperCase();
            if (header.contains("MSIE") || header.contains("TRIDENT") || header.contains("EDGE")) {
                fileName = URLEncoder.encode(fileName, "utf-8");
                fileName = fileName.replace("+", "%20"); //IE下载文件名空格变+号问题
            } else {
                fileName = new String(fileName.getBytes(), "ISO8859-1");
            }
            response.reset();
            response.setContentType("text/plain");
            response.setContentType("application/octet-stream; charset=utf-8");
            response.setHeader("Location", fileName);
            response.setHeader("Cache-Control", "max-age=0");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

            FileInputStream fis = new FileInputStream(zipFile);
            BufferedInputStream buff = new BufferedInputStream(fis);
            BufferedOutputStream out=new BufferedOutputStream(response.getOutputStream());
            byte[] car=new byte[1024];
            int l=0;
            while (l < zipFile.length()) {
                int j = buff.read(car, 0, 1024);
                l += j;
                out.write(car, 0, j);
            }
// 关闭流
            fis.close();
            buff.close();
            out.close();

            ossClient.shutdown();
// 删除临时文件
            zipFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 腾讯云 cos 后端打包批量下载
     * @param key
     * @param request
     * @param response
     * @return
     */

    @RequestMapping("/cosdownload")
    public String getCosFile(@RequestParam String key, HttpServletRequest request, HttpServletResponse response){


// endpoint以杭州为例，其它region请按实际情况填写，1改为自己的
        String endpoint = cosRegion;
// 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建
        String accessKeyId = cosId;
        String accessKeySecret = cosSecret;
        String bucketName = cosBucketName;
//要下载的文件名（Object Name）字符串，中间用‘,’间隔。文件名从bucket目录开始.5改为自己的

//todo 这个key 是上传时候的key，如下有上传案例的讲解


        try {
// 初始化
            COSCredentials cred = new BasicCOSCredentials(accessKeyId, accessKeySecret);
            Region region = new Region(endpoint);
            ClientConfig clientConfig = new ClientConfig(region);
            COSClient cosClient = new COSClient(cred,clientConfig);

//6改为自己的名称
            String fileName = "test.zip";
// 创建临时文件
            File zipFile = File.createTempFile("test", ".zip");
            FileOutputStream f = new FileOutputStream(zipFile);
/**
 * 作用是为任何OutputStream产生校验和
 * 第一个参数是制定产生校验和的输出流，第二个参数是指定Checksum的类型 （Adler32（较快）和CRC32两种）
 */
            CheckedOutputStream csum = new CheckedOutputStream(f, new Adler32());
// 用于将数据压缩成Zip文件格式
            ZipOutputStream zos = new ZipOutputStream(csum);

            String[] keylist = key.split("-");
            for (String cosfile : keylist) {
// 获取Object，返回结果为OSSObject对象
                COSObject cosObject = cosClient.getObject(bucketName,fileService.findByLocalName(cosfile).getRandomName());

// 读去Object内容 返回
                InputStream inputStream = cosObject.getObjectContent();
// 对于每一个要被存放到压缩包的文件，都必须调用ZipOutputStream对象的putNextEntry()方法，确保压缩包里面文件不同名

                zos.putNextEntry(new ZipEntry(cosfile));
                int bytesRead = 0;
// 向压缩文件中输出数据
                while((bytesRead=inputStream.read())!=-1){
                    zos.write(bytesRead);
                }
                inputStream.close();
                zos.closeEntry(); // 当前文件写完，定位为写入下一条项目
            }
            zos.close();
            String header = request.getHeader("User-Agent").toUpperCase();
            if (header.contains("MSIE") || header.contains("TRIDENT") || header.contains("EDGE")) {
                fileName = URLEncoder.encode(fileName, "utf-8");
                fileName = fileName.replace("+", "%20"); //IE下载文件名空格变+号问题
            } else {
                fileName = new String(fileName.getBytes(), "ISO8859-1");
            }
            response.reset();
            response.setContentType("text/plain");
            response.setContentType("application/octet-stream; charset=utf-8");
            response.setHeader("Location", fileName);
            response.setHeader("Cache-Control", "max-age=0");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

            FileInputStream fis = new FileInputStream(zipFile);
            BufferedInputStream buff = new BufferedInputStream(fis);
            BufferedOutputStream out=new BufferedOutputStream(response.getOutputStream());
            byte[] car=new byte[1024];
            int l=0;
            while (l < zipFile.length()) {
                int j = buff.read(car, 0, 1024);
                l += j;
                out.write(car, 0, j);
            }
// 关闭流
            fis.close();
            buff.close();
            out.close();

            cosClient.shutdown();
// 删除临时文件
            zipFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
