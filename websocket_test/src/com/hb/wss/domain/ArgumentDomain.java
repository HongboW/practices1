package com.hb.wss.domain;

import java.sql.Types;

/**
 * 参数实体类
 * Created by Hongbo on 2017/6/10.
 */
public class ArgumentDomain {
    /*参数名*/
    private String name;
    /*参数序号*/
    private int position;
    /*参数数据类型*/
    private String dataType;
    /*参数jdbc类型*/
    private int jdbcType;
    /*参数值*/
    private Object val;
    public ArgumentDomain (int position, String dataType, Object val) {
        this.setPosition(position);
        this.setDataType(dataType);
        this.setVal(val);
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
        setJdbcType(transferSQLType(dataType));
    }

    public int getJdbcType() {
        return jdbcType;
    }

    public void setJdbcType(int jdbcType) {
        this.jdbcType = jdbcType;
    }

    public Object getVal() {
        return val;
    }

    public void setVal(Object val) {
        this.val = val;
    }

    public static int transferSQLType (String s_dataType) {
        int i_sqlType = 12; //默认VARCHAR类型
        if ("varchar2".equalsIgnoreCase(s_dataType) || "varchar".equalsIgnoreCase(s_dataType)) {
            i_sqlType = Types.VARCHAR;
        } else if ("int".equalsIgnoreCase(s_dataType)) {
            i_sqlType = Types.INTEGER;
        } else if ("smallint".equalsIgnoreCase(s_dataType)) {
            i_sqlType = Types.SMALLINT;
        } else if ("tinyint".equalsIgnoreCase(s_dataType)) {
            i_sqlType = Types.TINYINT;
        } else if ("numeric".equalsIgnoreCase(s_dataType)) {
            i_sqlType = Types.NUMERIC;
        } else if ("char".equalsIgnoreCase(s_dataType)) {
            i_sqlType = Types.CHAR;
        } else if ("text".equalsIgnoreCase(s_dataType) || "clob".equalsIgnoreCase(s_dataType)) {
            i_sqlType = Types.CLOB;
        } else if ("datetime".equalsIgnoreCase(s_dataType) || "date".equalsIgnoreCase(s_dataType)) {
            i_sqlType = Types.TIMESTAMP;
        } else if ("ref cursor".equalsIgnoreCase(s_dataType)) {
            i_sqlType = oracle.jdbc.OracleTypes.CURSOR;    //Oracle游标类型
        }
        return i_sqlType;
    }
}
