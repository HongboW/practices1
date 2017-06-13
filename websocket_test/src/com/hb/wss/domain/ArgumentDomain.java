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
    public ArgumentDomain () {}
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
        if (dataType.equalsIgnoreCase("varchar2") || dataType.equalsIgnoreCase("varchar")) {
            setJdbcType(Types.VARCHAR);
        } else if (dataType.equalsIgnoreCase("int")) {
            setJdbcType(Types.INTEGER);
        } else if (dataType.equalsIgnoreCase("smallint")) {
            setJdbcType(Types.SMALLINT);
        } else if (dataType.equalsIgnoreCase("tinyint")) {
            setJdbcType(Types.TINYINT);
        } else if (dataType.equalsIgnoreCase("numeric")) {
            setJdbcType(Types.NUMERIC);
        } else if (dataType.equalsIgnoreCase("char")) {
            setJdbcType(Types.CHAR);
        } else if (dataType.equalsIgnoreCase("text") || dataType.equalsIgnoreCase("clob")) {
            setJdbcType(Types.CLOB);
        } else if (dataType.equalsIgnoreCase("datetime") || dataType.equalsIgnoreCase("date")) {
            setJdbcType(Types.TIMESTAMP);
        } else if (dataType.equalsIgnoreCase("ref cursor")) {
            setJdbcType(oracle.jdbc.OracleTypes.CURSOR);    //Oracle游标类型
        }

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

    public int transferSQLType (String s_dataType) {
        int i_sqlType = 12; //默认VARCHAR类型
        if (dataType.equalsIgnoreCase("varchar2") || dataType.equalsIgnoreCase("varchar")) {
            i_sqlType = Types.VARCHAR;
        } else if (dataType.equalsIgnoreCase("int")) {
            i_sqlType = Types.INTEGER;
        } else if (dataType.equalsIgnoreCase("smallint")) {
            i_sqlType = Types.SMALLINT;
        } else if (dataType.equalsIgnoreCase("tinyint")) {
            i_sqlType = Types.TINYINT;
        } else if (dataType.equalsIgnoreCase("numeric")) {
            i_sqlType = Types.NUMERIC;
        } else if (dataType.equalsIgnoreCase("char")) {
            i_sqlType = Types.CHAR;
        } else if (dataType.equalsIgnoreCase("text") || dataType.equalsIgnoreCase("clob")) {
            i_sqlType = Types.CLOB;
        } else if (dataType.equalsIgnoreCase("datetime") || dataType.equalsIgnoreCase("date")) {
            i_sqlType = Types.TIMESTAMP;
        } else if (dataType.equalsIgnoreCase("ref cursor")) {
            i_sqlType = oracle.jdbc.OracleTypes.CURSOR;    //Oracle游标类型
        }
        return i_sqlType;
    }
}
