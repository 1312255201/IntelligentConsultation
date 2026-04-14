package cn.gugufish.service;

import cn.gugufish.entity.vo.request.MedicineCreateVO;
import cn.gugufish.entity.vo.request.MedicineUpdateVO;
import cn.gugufish.entity.vo.response.MedicineCatalogVO;

import java.util.List;

public interface MedicineCatalogService {
    List<MedicineCatalogVO> listMedicines(boolean enabledOnly);
    String createMedicine(MedicineCreateVO vo);
    String updateMedicine(MedicineUpdateVO vo);
    String deleteMedicine(int id);
}
