package com.tonghui.erp.Service;

import com.tonghui.erp.Common.Dto.PageRequestDto;
import com.tonghui.erp.Common.Dto.PagedResult;
import com.tonghui.erp.Data.Entity.Equipment;
import com.tonghui.erp.Data.Entity.EquipmentMaintenance;
import com.tonghui.erp.Data.Entity.RoomInfo;
import com.tonghui.erp.Data.Entity.TemperatureHumidityRecord;
import com.tonghui.erp.Data.Entity.PressureDifferenceRecord;
import com.tonghui.erp.Data.Entity.PersonnelFile;
import com.tonghui.erp.Data.Entity.PersonnelCertificate;
import com.tonghui.erp.Data.Entity.TrainingRecord;
import com.tonghui.erp.Data.Entity.Organization;
import com.tonghui.erp.Data.Entity.OrganizationCertificate;
import com.tonghui.erp.Data.Entity.CleaningRecord;
import com.tonghui.erp.Data.Entity.DisinfectionRecord;
import com.tonghui.erp.Data.Entity.VerificationRecord;
import com.tonghui.erp.Data.mapper.EquipmentMapper;
import com.tonghui.erp.Data.mapper.RoomInfoMapper;
import com.tonghui.erp.Service.EquipmentService;
import com.tonghui.erp.Service.EquipmentMaintenanceService;
import com.tonghui.erp.Service.RoomInfoService;
import com.tonghui.erp.Service.TemperatureHumidityRecordService;
import com.tonghui.erp.Service.PressureDifferenceRecordService;
import com.tonghui.erp.Service.PersonnelFileService;
import com.tonghui.erp.Service.PersonnelCertificateService;
import com.tonghui.erp.Service.TrainingRecordService;
import com.tonghui.erp.Service.OrganizationService;
import com.tonghui.erp.Service.OrganizationCertificateService;
import com.tonghui.erp.Service.CleaningRecordService;
import com.tonghui.erp.Service.DisinfectionRecordService;
import com.tonghui.erp.Service.VerificationRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 设备、环境、人员域综合测试
 * <p>
 * 覆盖设备、设备维护、洁净室、温湿度/压差记录、人员档案、人员证书、培训记录、
 * 机构资质、清洁/消毒记录、验证记录等核心业务逻辑，
 * 全部用例使用事务回滚，不污染数据库
 * </p>
 */
@SpringBootTest
public class EquipmentPersonnelModuleTest {

    // region 服务依赖注入
    // ===================================
    // 服务依赖注入
    // ===================================

    /**
     * 设备服务
     */
    @Autowired
    private EquipmentService equipmentService;

    /**
     * 设备维护服务
     */
    @Autowired
    private EquipmentMaintenanceService equipmentMaintenanceService;

    /**
     * 洁净室服务
     */
    @Autowired
    private RoomInfoService roomInfoService;

    /**
     * 温湿度记录服务
     */
    @Autowired
    private TemperatureHumidityRecordService temperatureHumidityRecordService;

    /**
     * 压差记录服务
     */
    @Autowired
    private PressureDifferenceRecordService pressureDifferenceRecordService;

    /**
     * 人员档案服务
     */
    @Autowired
    private PersonnelFileService personnelFileService;

    /**
     * 人员证书服务
     */
    @Autowired
    private PersonnelCertificateService personnelCertificateService;

    /**
     * 培训记录服务
     */
    @Autowired
    private TrainingRecordService trainingRecordService;

    /**
     * 机构资质服务
     */
    @Autowired
    private OrganizationService organizationService;

    /**
     * 机构证书服务
     */
    @Autowired
    private OrganizationCertificateService organizationCertificateService;

    /**
     * 清洁记录服务
     */
    @Autowired
    private CleaningRecordService cleaningRecordService;

    /**
     * 消毒记录服务
     */
    @Autowired
    private DisinfectionRecordService disinfectionRecordService;

    /**
     * 验证记录服务
     */
    @Autowired
    private VerificationRecordService verificationRecordService;

    /**
     * 设备数据访问层
     */
    @Autowired
    private EquipmentMapper equipmentMapper;

    /**
     * 洁净室数据访问层
     */
    @Autowired
    private RoomInfoMapper roomInfoMapper;

    // endregion

    // region 测试用例
    // ===================================
    // 测试用例
    // ===================================

    /**
     * 测试洁净室管理：新增→按名称查询→启用列表→删除
     */
    @Test
    @Transactional
    public void testRoomInfoCrud() {
        String roomName = "TESTROOM" + System.currentTimeMillis();
        RoomInfo room = new RoomInfo();
        room.setRoomCode("TST" + System.currentTimeMillis() % 100000);
        room.setRoomName(roomName);
        room.setRoomStatus(1);
        assertTrue(roomInfoService.save(room), "新增洁净室应成功");
        Integer roomId = room.getRoomId();

        // 按名称查询
        RoomInfo found = roomInfoService.getByName(roomName);
        assertNotNull(found, "按名称应查到洁净室");
        assertEquals(roomId, found.getRoomId(), "洁净室ID应一致");

        // 启用列表
        assertTrue(roomInfoService.listActive().stream().anyMatch(r -> r.getRoomId().equals(roomId)),
                "启用洁净室列表应包含新建洁净室");

        // 按名称搜索
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(-1);
        pageRequest.setPageSize(-1);
        PagedResult<RoomInfo> search = roomInfoService.searchByName(roomName, null, pageRequest);
        assertTrue(search.getTotalCount() >= 1, "按名称搜索应命中新建洁净室");

        // 删除
        assertTrue(roomInfoService.removeById(roomId), "删除洁净室应成功");
        assertNull(roomInfoService.getById(roomId), "删除后洁净室应不存在");
    }

    /**
     * 测试设备管理：新增→按编码查询→维护日期更新→启用列表
     */
    @Test
    @Transactional
    public void testEquipmentCrud() {
        String assetCode = "TSTEQ" + System.currentTimeMillis();
        Equipment equipment = new Equipment();
        equipment.setEquipmentName("测试设备");
        equipment.setFixedAssetCode(assetCode);
        equipment.setEquipmentStatus("正常");
        assertTrue(equipmentService.save(equipment), "新增设备应成功");
        Integer equipmentId = equipment.getEquipmentId();

        // 按资产编码查询
        Equipment found = equipmentService.getByFixedAssetCode(assetCode);
        assertNotNull(found, "按资产编码应查到设备");
        assertEquals(equipmentId, found.getEquipmentId(), "设备ID应一致");

        // 启用列表
        assertTrue(equipmentService.listActive().stream().anyMatch(e -> e.getEquipmentId().equals(equipmentId)),
                "启用设备列表应包含新建设备");

        // 维护日期更新
        assertTrue(equipmentService.updateMaintenanceDate(equipmentId.intValue(), LocalDate.now(), 1L),
                "更新维护日期应成功");
        Equipment after = equipmentService.getById(equipmentId);
        assertEquals(LocalDate.now(), after.getLastMaintenanceDate(), "维护日期应已更新");

        // 按名称搜索
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageIndex(-1);
        pageRequest.setPageSize(-1);
        PagedResult<Equipment> search = equipmentService.searchByName("测试设备", pageRequest);
        assertTrue(search.getTotalCount() >= 1, "按名称搜索应命中新建设备");

        // 删除设备（事务回滚兜底）
        assertTrue(equipmentService.removeById(equipmentId), "删除设备应成功");
    }

    /**
     * 测试设备维护管理：新增维护→按设备查询→即将维护列表
     */
    @Test
    @Transactional
    public void testEquipmentMaintenance() {
        String assetCode = "TSTEQM" + System.currentTimeMillis();
        Equipment equipment = new Equipment();
        equipment.setEquipmentName("维护测试设备");
        equipment.setFixedAssetCode(assetCode);
        equipmentService.save(equipment);
        Long equipmentId = equipment.getEquipmentId().longValue();

        // 新增维护记录
        EquipmentMaintenance maintenance = new EquipmentMaintenance();
        maintenance.setEquipmentId(equipmentId);
        maintenance.setMaintenanceType("定期维护");
        maintenance.setMaintenanceDate(LocalDate.now());
        maintenance.setMaintenanceContent("更换易损件");
        maintenance.setMaintenanceResult("正常");
        EquipmentMaintenance saved = equipmentMaintenanceService.saveWithAutoCalc(maintenance);
        assertNotNull(saved, "维护记录保存应成功");
        Long maintenanceId = saved.getMaintenanceId();

        // 按设备查询
        List<EquipmentMaintenance> list = equipmentMaintenanceService.findByEquipmentId(equipmentId);
        assertEquals(1, list.size(), "按设备应查到1条维护记录");

        // 即将维护列表不抛异常
        assertNotNull(equipmentMaintenanceService.findUpcomingMaintenance(30), "即将维护列表不应为空");

        // 更新维护内容
        maintenance.setMaintenanceId(maintenanceId);
        maintenance.setMaintenanceContent("更新后的维护内容");
        assertTrue(equipmentMaintenanceService.updateById(maintenance), "更新维护记录应成功");
        assertEquals("更新后的维护内容",
                equipmentMaintenanceService.getById(maintenanceId).getMaintenanceContent(),
                "维护内容应已更新");
    }

    /**
     * 测试温湿度与压差记录：新增→按洁净室查询
     */
    @Test
    @Transactional
    public void testEnvRecords() {
        // 温湿度记录
        TemperatureHumidityRecord tempRecord = new TemperatureHumidityRecord();
        tempRecord.setRoomId(1);
        tempRecord.setRecordDate(LocalDate.now());
        tempRecord.setInspectionArea("洁净区A");
        tempRecord.setTemperature(new BigDecimal("22.5"));
        tempRecord.setHumidity(new BigDecimal("45"));
        tempRecord.setRecorder("记录员A");
        assertTrue(temperatureHumidityRecordService.save(tempRecord), "新增温湿度记录应成功");
        Long tempId = tempRecord.getId();

        // 按洁净室查询
        List<TemperatureHumidityRecord> temps = temperatureHumidityRecordService.findByRoomId(1);
        assertTrue(temps.stream().anyMatch(r -> r.getId().equals(tempId)), "按洁净室应查到温湿度记录");

        // 压差记录
        PressureDifferenceRecord pressureRecord = new PressureDifferenceRecord();
        pressureRecord.setRoomId(1);
        pressureRecord.setRecordDate(LocalDate.now());
        pressureRecord.setInspectionArea("洁净区A");
        pressureRecord.setPressureValue(new BigDecimal("12"));
        pressureRecord.setRecorder("记录员A");
        assertTrue(pressureDifferenceRecordService.save(pressureRecord), "新增压差记录应成功");
        Long pressureId = pressureRecord.getId();

        List<PressureDifferenceRecord> pressures = pressureDifferenceRecordService.findByRoomId(1);
        assertTrue(pressures.stream().anyMatch(r -> r.getId().equals(pressureId)), "按洁净室应查到压差记录");
    }

    /**
     * 测试人员档案与证书：新增档案→批量保存证书→按人员查询
     */
    @Test
    @Transactional
    public void testPersonnelFileAndCertificates() {
        PersonnelFile file = new PersonnelFile();
        file.setEmployeeNo("TST" + System.currentTimeMillis());
        file.setName("测试人员");
        file.setStatus(1);
        assertTrue(personnelFileService.save(file), "新增人员档案应成功");
        Long fileId = file.getPersonnelFileId();

        // 批量保存证书
        PersonnelCertificate certificate = new PersonnelCertificate();
        certificate.setPersonnelFileId(fileId);
        certificate.setCertificateName("健康证");
        certificate.setExpiryDate(LocalDate.now().plusDays(90));
        personnelCertificateService.saveCertificates(fileId, List.of(certificate));

        // 按人员查询证书
        List<PersonnelCertificate> certificates = personnelCertificateService.getByPersonnelFileId(fileId);
        assertEquals(1, certificates.size(), "应查到1条证书");
        assertEquals("健康证", certificates.get(0).getCertificateName(), "证书名称应一致");

        // 到期证书提醒不抛异常
        assertNotNull(personnelCertificateService.findExpiringCertificates(30), "到期证书列表不应为空");

        // 人员档案分页查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PersonnelFile> page =
                personnelFileService.queryPersonnelFiles(file, 1, 10);
        assertTrue(page.getTotal() >= 1, "人员档案分页查询应命中");
    }

    /**
     * 测试培训记录管理：自动编号→新增→按编号查询→到期提醒
     */
    @Test
    @Transactional
    public void testTrainingRecordCrud() {
        TrainingRecord training = new TrainingRecord();
        training.setTrainingNo(trainingRecordService.generateTrainingNo());
        training.setTrainingName("GMP培训");
        training.setTrainingCategory("质量培训");
        training.setTrainingForm("集中授课");
        training.setTrainingDate(new Date());
        training.setTrainer("培训讲师A");
        assertTrue(trainingRecordService.addTrainingRecord(training), "新增培训记录应成功");
        Long trainingId = training.getId();

        // 按编号查询
        TrainingRecord found = trainingRecordService.getTrainingRecordByNo(training.getTrainingNo());
        assertNotNull(found, "按编号应查到培训记录");
        assertEquals(trainingId, found.getId(), "培训记录ID应一致");

        // 更新培训
        training.setTrainingName("GMP更新培训");
        assertTrue(trainingRecordService.updateTrainingRecord(training), "更新培训记录应成功");
        assertEquals("GMP更新培训", trainingRecordService.getTrainingRecordById(trainingId).getTrainingName(),
                "培训名称应已更新");

        // 到期提醒与下一次培训日期计算
        assertNotNull(trainingRecordService.getExpiringTrainings(30), "到期培训列表不应为空");
        Date next = trainingRecordService.calculateNextTrainingDate(new Date(), 12);
        assertNotNull(next, "下次培训日期计算不应为空");

        // 删除
        assertTrue(trainingRecordService.deleteTrainingRecord(trainingId), "删除培训记录应成功");
        assertNull(trainingRecordService.getTrainingRecordById(trainingId), "删除后培训记录应不存在");
    }

    /**
     * 测试机构资质与证书：创建/复用机构→保存证书→查询→更新
     */
    @Test
    @Transactional
    public void testOrganizationAndCertificates() {
        // 线上可能已存在机构：存在则复用，不存在则创建
        Organization organization = organizationService.getWithDetails();
        if (organization == null) {
            Organization create = new Organization();
            create.setLicenseNo("TSTLIC" + System.currentTimeMillis());
            create.setOrgName("测试机构");
            create.setOrgCategory("药品生产企业");
            create.setUnifiedSocialCreditCode("91" + System.currentTimeMillis() % 1000000000000000000L);
            create.setPracticeLicenseNo("TSTPRA" + System.currentTimeMillis());
            create.setLegalRepresentative("法定代表人");
            create.setEnterpriseLeader("企业负责人");
            create.setPrepRoomLeader("车间负责人");
            create.setPreparationAddress("测试地址");
            create.setPreparationScope("中药饮片");
            create.setIssuingAuthority("省药监局");
            create.setIssueDate(LocalDate.now());
            create.setLicenseStatus("有效");
            create.setStatus(1);
            organization = organizationService.createOrganization(create);
            assertNotNull(organization.getId(), "机构ID应已回填");
        }
        Long orgId = organization.getId();

        // 保存证书
        OrganizationCertificate certificate = new OrganizationCertificate();
        certificate.setOrganizationId(orgId);
        certificate.setCertificateName("GMP证书");
        certificate.setCertificateType("资质证书");
        certificate.setExpiryDate(LocalDate.now().plusYears(5));
        organizationCertificateService.saveCertificates(orgId, List.of(certificate));

        // 按机构查询证书
        List<OrganizationCertificate> certificates = organizationCertificateService.getByOrganizationId(orgId);
        assertEquals(1, certificates.size(), "应查到1条机构证书");

        // 详情查询
        assertNotNull(organizationService.getWithDetails(), "机构详情不应为空");

        // 更新机构
        organization.setOrgName("更新后的机构名");
        Organization updated = organizationService.updateOrganization(organization);
        assertEquals("更新后的机构名", updated.getOrgName(), "机构名称应已更新");
    }

    /**
     * 测试清洁与消毒记录：新增→按洁净室查询→即将执行列表
     */
    @Test
    @Transactional
    public void testCleaningAndDisinfection() {
        // 清洁记录
        CleaningRecord cleaning = new CleaningRecord();
        cleaning.setRoomId(1L);
        cleaning.setCleaningDate(LocalDate.now());
        cleaning.setCleaningArea("洁净区A");
        cleaning.setCleaningMethod("纯化水擦拭");
        cleaning.setCleaningPerson("清洁员A");
        cleaning.setCleaningCycle(7);
        assertTrue(cleaningRecordService.save(cleaning), "新增清洁记录应成功");
        Long cleaningId = cleaning.getId();

        List<CleaningRecord> cleanings = cleaningRecordService.findByRoomId(1);
        assertTrue(cleanings.stream().anyMatch(r -> r.getId().equals(cleaningId)), "按洁净室应查到清洁记录");
        assertNotNull(cleaningRecordService.findUpcomingCleaning(7), "即将清洁列表不应为空");

        // 消毒记录
        DisinfectionRecord disinfection = new DisinfectionRecord();
        disinfection.setRoomId(1);
        disinfection.setDisinfectionDate(LocalDate.now());
        disinfection.setDisinfectionMethod("紫外灯照射");
        disinfection.setDisinfectionPerson("消毒员A");
        disinfection.setDisinfectionCycle(7);
        assertTrue(disinfectionRecordService.save(disinfection), "新增消毒记录应成功");
        Long disinfectionId = disinfection.getId();

        List<DisinfectionRecord> disinfestations = disinfectionRecordService.findByRoomId(1);
        assertTrue(disinfestations.stream().anyMatch(r -> r.getId().equals(disinfectionId)),
                "按洁净室应查到消毒记录");
        assertNotNull(disinfectionRecordService.findUpcomingDisinfection(7), "即将消毒列表不应为空");
    }

    /**
     * 测试验证记录管理：自动编号→新增→按编号查询→到期提醒→删除
     */
    @Test
    @Transactional
    public void testVerificationRecordCrud() {
        VerificationRecord record = new VerificationRecord();
        record.setCategory("设备验证");
        record.setVerificationNo("TSTVER" + System.currentTimeMillis());
        record.setVerificationName("灭菌柜验证");
        record.setRelatedObject("灭菌柜");
        record.setExecuteDate(new Date());
        record.setExecutor("验证员A");
        record.setAuditor("审核员B");
        record.setNextVerifyDate(new Date());
        assertTrue(verificationRecordService.addVerificationRecord(record), "新增验证记录应成功");
        Long recordId = record.getId();

        // 按编号查询
        VerificationRecord found = verificationRecordService.getVerificationRecordById(recordId);
        assertNotNull(found, "按ID应查到验证记录");
        assertEquals(record.getVerificationNo(), found.getVerificationNo(), "验证编号应一致");

        // 分页查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<VerificationRecord> page =
                verificationRecordService.getVerificationRecordList(1, 10);
        assertTrue(page.getTotal() >= 1, "分页查询应有数据");

        // 到期提醒
        assertNotNull(verificationRecordService.getExpiringVerifications(365), "到期验证列表不应为空");

        // 更新
        record.setVerificationName("更新后的验证项目");
        assertTrue(verificationRecordService.updateVerificationRecord(record), "更新验证记录应成功");
        assertEquals("更新后的验证项目",
                verificationRecordService.getVerificationRecordById(recordId).getVerificationName(),
                "验证项目名称应已更新");

        // 删除
        assertTrue(verificationRecordService.deleteVerificationRecord(recordId), "删除验证记录应成功");
        assertNull(verificationRecordService.getVerificationRecordById(recordId), "删除后验证记录应不存在");
    }

    // endregion
}