package com.example.demo.contract.entity;

import lombok.Data;

import java.util.Date;

/**
 * 合同实体类
 * 对应合同表，存储合同基本信息
 */
@Data
public class Contract {
    /**
     * 合同ID
     */
    private String contractId;
    /**
     * 项目ID
     */
    private String projectId;
    /**
     * 合同编号
     */
    private String contractNo;
    /**
     * 合同名称
     */
    private String contractName;
    /**
     * 合同类型
     */
    private String contractType;
    /**
     * 合同金额
     */
    private Double contractAmount;
    /**
     * 签约人
     */
    private String signer;
    /**
     * 签约日期
     */
    private Date signDate;
    /**
     * 开始时间
     */
    private Date startTime;
    /**
     * 结束时间
     */
    private Date endTime;
    /**
     * 更新时间
     */
    private Date updateTime;
}