package com.example.demo.contract.entity;

import lombok.Data;

import java.util.Date;

/**
 * 交付物实体类
 * 对应交付物表，存储项目交付物信息
 */
@Data
public class DeliveryItem {
    /**
     * 交付物ID
     */
    private String deliveryId;
    /**
     * 合同ID
     */
    private String contractId;
    /**
     * 项目ID
     */
    private String projectId;
    /**
     * 交付类型
     */
    private String deliveryType;
    /**
     * 交付截止日期
     */
    private Date deliveryDeadline;
    /**
     * 备注
     */
    private String remark;
    /**
     * 文件名称
     */
    private String fileName;
    /**
     * 文件路径
     */
    private String filePath;
    /**
     * 文件大小
     */
    private Long fileSize;
    /**
     * 上传时间
     */
    private Date uploadTime;
    /**
     * 上传人`r`n     */
    private String uploader;
    /**
     * 需求负责人
     */
    private String requirementOwner;
}
