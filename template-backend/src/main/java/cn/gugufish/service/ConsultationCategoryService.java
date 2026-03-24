package cn.gugufish.service;

import cn.gugufish.entity.dto.ConsultationCategory;
import cn.gugufish.entity.vo.request.ConsultationCategoryCreateVO;
import cn.gugufish.entity.vo.request.ConsultationCategoryUpdateVO;

import java.util.List;

public interface ConsultationCategoryService {
    List<ConsultationCategory> listCategories();
    String createCategory(ConsultationCategoryCreateVO vo);
    String updateCategory(ConsultationCategoryUpdateVO vo);
    String deleteCategory(int id);
}
