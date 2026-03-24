package cn.gugufish.service;

import cn.gugufish.entity.dto.StoreImage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;

public interface ImageService extends IService<StoreImage> {
    String uploadAvatar(int id, MultipartFile file) throws Exception;
    String uploadImage(int id,MultipartFile file) throws Exception;
    String uploadDoctorPhoto(MultipartFile file) throws Exception;
    String uploadHomepageImage(String type, MultipartFile file) throws Exception;
    void fetchImageFromMinio(OutputStream stream, String image) throws Exception;
    void deleteImage(String image);
}
