package cn.gugufish.service.impl;

import cn.gugufish.entity.dto.Account;
import cn.gugufish.entity.dto.StoreImage;
import cn.gugufish.mapper.AccountMapper;
import cn.gugufish.mapper.ImageStoreMapper;
import cn.gugufish.service.ImageService;
import cn.gugufish.utils.Const;
import cn.gugufish.utils.FlowUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class ImageServiceImpl extends ServiceImpl<ImageStoreMapper, StoreImage> implements ImageService {
    @Resource
    MinioClient minioClient;
    @Resource
    AccountMapper accountMapper;
    @Resource
    FlowUtils flowUtils;

    private final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

    @Override
    public String uploadAvatar(int id, MultipartFile file) throws IOException {
        try {
            String imageName = this.uploadDirect("/avatar/", file);
            String avatar = accountMapper.selectById(id).getAvatar();
            if (accountMapper.update(null, Wrappers.<Account>update()
                    .eq("id", id).set("avatar", imageName)) > 0) {
                this.deleteImage(avatar);
                return imageName;
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("图片上传发生问题" + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String uploadImage(int id, MultipartFile file) throws IOException {
        String key = Const.CONSULTATION_IMAGE_COUNTER + id;
        if (!flowUtils.limitPeriodCounterCheck(key, 20, 3600))
            return null;
        try {
            Date date = new Date();
            String imageName = this.uploadDirect("/cache/" + format.format(date) + "/", file);
            if (this.save(new StoreImage(id, imageName, date))) {
                return imageName;
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("图片上传出现问题: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String uploadDoctorPhoto(MultipartFile file) throws IOException {
        try {
            return this.uploadDirect("/doctor/" + format.format(new Date()) + "/", file);
        } catch (Exception e) {
            log.error("医生照片上传出现问题: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String uploadHomepageImage(String type, MultipartFile file) throws IOException {
        try {
            return this.uploadDirect("/homepage/" + type + "/" + format.format(new Date()) + "/", file);
        } catch (Exception e) {
            log.error("棣栭〉鍥剧墖涓婁紶鍑虹幇闂: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void fetchImageFromMinio(OutputStream stream, String image) throws Exception {
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket("consultation")
                .object(image)
                .build();
        GetObjectResponse response = minioClient.getObject(args);
        IOUtils.copy(response, stream);
    }

    @Override
    public void deleteImage(String image) {
        if (image == null || image.isEmpty()) return;
        try {
            RemoveObjectArgs remove = RemoveObjectArgs.builder()
                    .bucket("consultation")
                    .object(image)
                    .build();
            minioClient.removeObject(remove);
        } catch (Exception e) {
            log.warn("对象存储删除图片失败: {}", image, e);
        }
    }

    private String uploadDirect(String prefix, MultipartFile file) throws Exception {
        String imageName = prefix + UUID.randomUUID().toString().replace("-", "");
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket("consultation")
                .stream(file.getInputStream(), file.getSize(), -1)
                .object(imageName)
                .build();
        minioClient.putObject(args);
        return imageName;
    }
}
