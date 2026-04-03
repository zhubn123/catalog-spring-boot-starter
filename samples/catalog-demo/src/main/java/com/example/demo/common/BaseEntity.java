package com.example.demo.common;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private Date createTime;
    private Date updateTime;
}
