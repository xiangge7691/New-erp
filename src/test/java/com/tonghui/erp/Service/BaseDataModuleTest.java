package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.DosageForm;
import com.tonghui.erp.Data.Entity.Material;
import com.tonghui.erp.Data.Entity.Preparation;
import com.tonghui.erp.Data.Entity.ProductionUnit;
import com.tonghui.erp.Data.Entity.PurchaseSuppliers;
import com.tonghui.erp.Data.Entity.Unit;
import com.tonghui.erp.Data.mapper.MaterialMapper;
import com.tonghui.erp.Service.MaterialService;
import com.tonghui.erp.Service.UnitService;
import com.tonghui.erp.Service.DosageFormService;
import com.tonghui.erp.Service.ProductionUnitService;
import com.tonghui.erp.Service.PreparationService;
import com.tonghui.erp.Service.PurchaseSuppliersService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基础数据域综合测试
 * <p>
 * 覆盖计量单位、剂型、物料、生产单位、制剂、供应商等基础资料的核心业务逻辑，
 * 全部用例使用事务回滚，不污染数据库
 * </p>
 */
@SpringBootTest
public class BaseDataModuleTest {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 计量单位服务
     */
    @Autowired
    private UnitService unitService;

    /**
     * 剂型服务
     */
    @Autowired
    private DosageFormService dosageFormService;

    /**
     * 物料服务
     */
    @Autowired
    private MaterialService materialService;

    /**
     * 生产单位（仓库）服务
     */
    @Autowired
    private ProductionUnitService productionUnitService;

    /**
     * 制剂（药品）服务
     */
    @Autowired
    private PreparationService preparationService;

    /**
     * 供应商服务
     */
    @Autowired
    private PurchaseSuppliersService purchaseSuppliersService;

    /**
     * 物料数据访问层
     */
    @Autowired
    private MaterialMapper materialMapper;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试计量单位管理：创建→按名称搜索→更新→删除
     */
    @Test
    @Transactional
    public void testUnitCrud() {
        String unitName = "TESTUNIT" + System.currentTimeMillis();
        Unit unit = new Unit();
        unit.setUnitName(unitName);
        unit.setSymbol("T" + System.currentTimeMillis() % 100000);
        unit.setStatus(1);
        assertTrue(unitService.save(unit), "创建计量单位应成功");
        Long unitId = unit.getUnitId();

        // 按名称搜索
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(1);
        pageRequest.setPageSize(10);
        PagedResult<Unit> search = unitService.searchByName(unitName, pageRequest);
        assertTrue(search.getTotalCount() >= 1, "按名称搜索应命中新建单位");

        // 更新状态
        Unit update = new Unit();
        update.setUnitId(unitId);
        update.setStatus(0);
        assertTrue(unitService.updateById(update), "更新单位应成功");
        assertEquals(0, unitService.getById(unitId).getStatus().intValue(), "单位状态应已更新");

        // 删除
        assertTrue(unitService.removeById(unitId), "删除单位应成功");
        assertNull(unitService.getById(unitId), "删除后单位应不存在");
    }

    /**
     * 测试剂型管理：创建→按类名搜索→按类获取剂型名称列表
     */
    @Test
    @Transactional
    public void testDosageFormCrud() {
        String category = "TESTCAT" + System.currentTimeMillis();
        DosageForm form = new DosageForm();
        form.setDosageCategory(category);
        form.setDosageName("测试剂型");
        form.setStatus(1);
        assertTrue(dosageFormService.save(form), "创建剂型应成功");

        // 按类名搜索
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(1);
        pageRequest.setPageSize(10);
        PagedResult<DosageForm> search = dosageFormService.searchByName(category, pageRequest);
        assertTrue(search.getTotalCount() >= 1, "按类别搜索应命中新建剂型");

        // 按类别获取剂型名称列表
        List<String> names = dosageFormService.getDistinctDosageNamesByCategory(category);
        assertTrue(names.contains("测试剂型"), "剂型名称列表应包含新建剂型");

        // 分页查询
        DosageForm query = new DosageForm();
        query.setDosageCategory(category);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<DosageForm> page =
                dosageFormService.queryDosageForms(query, 1, 10);
        assertTrue(page.getTotal() >= 1, "分页查询应命中新建剂型");
    }

    /**
     * 测试物料管理：自动生成编码→新增→按编码查询→更新→删除
     */
    @Test
    @Transactional
    public void testMaterialCrud() {
        String category = "原料";
        String code = materialService.generateMaterialCode(category);
        assertNotNull(code, "物料编码生成不应为空");
        assertTrue(code.startsWith("Y"), "原料类物料编码应以Y开头");

        Material material = new Material();
        material.setMaterialCode(code);
        material.setMaterialName("测试物料" + System.currentTimeMillis());
        material.setCategoryName(category);
        material.setUnitName("kg");
        material.setMaterialStatus(1);
        materialService.addMaterial(material);
        assertNotNull(material.getMaterialId(), "新增物料应回填ID");

        // 按编码查询
        Material found = materialService.getMaterialByCode(code);
        assertNotNull(found, "按编码应查到物料");
        assertEquals(material.getMaterialName(), found.getMaterialName(), "物料名称应一致");

        // 更新物料
        material.setMaterialName("更新后的物料名");
        materialService.updateMaterial(material);
        assertEquals("更新后的物料名", materialService.getMaterialById(material.getMaterialId()).getMaterialName(),
                "物料名称应已更新");

        // 启用物料列表应包含
        assertTrue(materialService.getEnabledMaterials().stream()
                        .anyMatch(m -> m.getMaterialId().equals(material.getMaterialId())),
                "启用物料列表应包含新建物料");

        // 删除物料
        materialService.deleteMaterial(material.getMaterialId());
        assertNull(materialService.getMaterialById(material.getMaterialId()), "删除后物料应不存在");
    }

    /**
     * 测试生产单位（仓库）管理：新增→按编码查询→更新→启用列表
     */
    @Test
    @Transactional
    public void testProductionUnitCrud() {
        String code = "TESTPU" + System.currentTimeMillis();
        ProductionUnit unit = new ProductionUnit();
        unit.setProdUnitCode(code);
        unit.setProdUnitName("测试生产单位");
        unit.setProdUnitManager("测试负责人");
        unit.setProdUnitStatus(1);
        assertTrue(productionUnitService.addProductionUnit(unit), "新增生产单位应成功");
        Long unitId = unit.getProdUnitId();

        // 按编码查询
        ProductionUnit found = productionUnitService.getProductionUnitByCode(code);
        assertNotNull(found, "按编码应查到生产单位");

        // 更新
        unit.setProdUnitName("更新后的生产单位");
        assertTrue(productionUnitService.updateProductionUnit(unit), "更新生产单位应成功");
        assertEquals("更新后的生产单位", productionUnitService.getProductionUnitById(unitId).getProdUnitName(),
                "生产单位名称应已更新");

        // 启用列表包含
        assertTrue(productionUnitService.getEnabledProductionUnits().stream()
                        .anyMatch(u -> u.getProdUnitId().equals(unitId)),
                "启用生产单位列表应包含新建单位");

        // 分页列表
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(-1);
        pageRequest.setPageSize(-1);
        assertNotNull(productionUnitService.getProductionUnitList(pageRequest), "分页列表不应为空");
    }

    /**
     * 测试制剂管理：新增→按编码查询→更新→搜索
     */
    @Test
    @Transactional
    public void testPreparationCrud() {
        String code = "TESTPREP" + System.currentTimeMillis();
        Preparation preparation = new Preparation();
        preparation.setPreparationCode(code);
        preparation.setPreparationName("测试制剂");
        preparation.setUnitName("kg");
        preparation.setStatus(1);
        preparationService.addPreparation(preparation);
        assertNotNull(preparation.getPreparationId(), "新增制剂应回填ID");

        // 按编码查询
        Preparation found = preparationService.getPreparationByCode(code);
        assertNotNull(found, "按编码应查到制剂");

        // 更新
        preparation.setPreparationName("更新后的制剂名");
        preparationService.updatePreparation(preparation);
        assertEquals("更新后的制剂名", preparationService.getPreparationById(preparation.getPreparationId()).getPreparationName(),
                "制剂名称应已更新");

        // 分页查询
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(-1);
        pageRequest.setPageSize(-1);
        PagedResult<Preparation> list = preparationService.getPreparationList(pageRequest);
        assertNotNull(list, "制剂分页列表不应为空");
        assertTrue(list.getItems().stream().anyMatch(p -> p.getPreparationCode().equals(code)),
                "制剂列表应包含新建制剂");

        // 删除
        preparationService.deletePreparation(preparation.getPreparationId());
        assertNull(preparationService.getPreparationById(preparation.getPreparationId()), "删除后制剂应不存在");
    }

    /**
     * 测试供应商管理：新增→按编号查询→更新→列表→删除
     */
    @Test
    @Transactional
    public void testSupplierCrud() {
        String number = "TESTSUP" + System.currentTimeMillis();
        PurchaseSuppliers supplier = new PurchaseSuppliers();
        supplier.setSupplierNumber(number);
        supplier.setSupplierName("测试供应商");
        supplier.setStatus("active");
        assertTrue(purchaseSuppliersService.addPurchaseSupplier(supplier), "新增供应商应成功");
        Long supplierId = supplier.getId();

        // 按编号查询
        PurchaseSuppliers found = purchaseSuppliersService.getPurchaseSupplierByNumber(number);
        assertNotNull(found, "按编号应查到供应商");

        // 更新
        supplier.setSupplierName("更新后的供应商");
        assertTrue(purchaseSuppliersService.updatePurchaseSupplier(supplier), "更新供应商应成功");
        assertEquals("更新后的供应商", purchaseSuppliersService.getPurchaseSupplierById(supplierId).getSupplierName(),
                "供应商名称应已更新");

        // 启用列表
        assertTrue(purchaseSuppliersService.getEnabledPurchaseSuppliers().stream()
                        .anyMatch(s -> s.getId().equals(supplierId)),
                "启用供应商列表应包含新建供应商");

        // 关键字分页查询
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(-1);
        pageRequest.setPageSize(-1);
        PagedResult<PurchaseSuppliers> list = purchaseSuppliersService.getPurchaseSupplierList(pageRequest);
        assertTrue(list.getItems().stream().anyMatch(s -> s.getId().equals(supplierId)),
                "供应商列表应包含新建供应商");

        // 删除
        assertTrue(purchaseSuppliersService.deletePurchaseSupplier(supplierId), "删除供应商应成功");
        assertNull(purchaseSuppliersService.getPurchaseSupplierById(supplierId), "删除后供应商应不存在");
    }

    // endregion
}
