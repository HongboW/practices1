package com.hb.wss.dao;

import com.hb.wss.domain.ArgumentDomain;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据处理工具集
 * Created by Hongbo on 2017/6/10.
 */
public class DataUtils extends ConnectDB {
    /**
     * 查询
     * @param conn 数据库连接
     * @param sql 查询语句
     * @param arguments 参数集合(初始化时需要指定数据类型)
     * @return
     */
    List<Map<String, Object>> execDQL(Connection conn, String sql, List<ArgumentDomain> arguments) {
        List<Map<String, Object>> result = null;
        if (sqlNotBegin(conn, sql, arguments)) return result;
        PreparedStatement pre = null;   //初始化预编译对象
        ResultSet rs = null;    //初始化结果集对象
        try {
            pre = conn.prepareStatement(sql); //预编译
            ArgumentDomain argument;
            for (int i = 0; i < arguments.size(); i++) {
                argument = arguments.get(i);    //获取参数对象
                pre.setObject(i, argument.getVal(), argument.getJdbcType());    //传值
            }
            rs = pre.executeQuery();
            result.addAll(result2List(rs));
        } catch (SQLException e) {
            System.err.println(str_date + "SQL编译异常！");
        } finally {
            closeAll(rs, pre, conn);
        }
        return result;
    }

    /**
     * 删、改、增
     * @param conn 数据库连接
     * @param sql 查询语句
     * @param arguments 参数集合(初始化时需要指定数据类型)
     * @return
     */
    int execDML(Connection conn, String sql, List<ArgumentDomain> arguments) {
        int sqlRowCount = 0;
        if (sqlNotBegin(conn, sql, arguments)) return sqlRowCount;
        PreparedStatement pre = null;   //初始化预编译对象
        ResultSet rs = null;    //初始化结果集对象
        try {
            pre = conn.prepareStatement(sql); //预编译
            ArgumentDomain argument;
            for (int i = 0; i < arguments.size(); i++) {
                argument = arguments.get(i);    //获取参数对象
                pre.setObject(i, argument.getVal(), argument.getJdbcType());    //传值
            }
            sqlRowCount = pre.executeUpdate();
        } catch (SQLException e) {
            System.err.println(str_date + "SQL编译异常！");
        } finally {
            closeAll(rs, pre, conn);
        }
        return sqlRowCount;
    }
    List<Map<String, Object>> execPrc(Connection conn, String sql, List<ArgumentDomain> arguments) {
        List<Map<String, Object>> result = null;
        if (conn == null) return result;    //数据库连接无效，退出
        String[] str = sql.split("\\.");
        String pkgName, prcName;
        ArgumentDomain arg = new ArgumentDomain();
        List<ArgumentDomain> argList = new ArrayList();
        switch (str.length) {
            case 1: //"prc_name"
                arg.setDataType("varchar");
                arg.setVal(str[0]);
                arg.setPosition(1);
                argList.add(arg);
                break;
            case 2: //"pkg_name.prc_name"
                arg.setDataType("varchar");
                arg.setVal(str[1]);
                arg.setPosition(1);
                argList.add(arg);
                arg.setDataType("varchar");
                arg.setVal(str[0]);
                arg.setPosition(2);
                argList.add(arg);
                break;
            default:return result;
        }
        String argSql = "SELECT /*+ RESULT_CACHE */ + lower(argument_name) as argument_name, lower(data_type) as data_type, lower(in_out) as in_out, position FROM user_arguments where object_name= upper(?) and package_name=upper(?) order by POSITION";
        List<Map<String, Object>> paramList = execDQL(conn, argSql, argList);
        CallableStatement cstmt = null;
        StringBuffer prcSql = new StringBuffer("");
        prcSql.append("{ call ");
        prcSql.append(sql);
        if (paramList.size() > 0) {
            prcSql.append("(");
            for (int i = 0; i < paramList.size(); i++) {
                prcSql.append("?,");
            }
            prcSql.replace(prcSql.length() - 1, prcSql.length() -1, ")");
        }
        prcSql.append("}");
        for (Map<String, Object> map : paramList) {

        }
        return result;
    }
    /**
     * 判断是否可以开始SQL操作
     * @param conn 数据库连接
     * @param sql 查询语句
     * @param arguments 参数集合(初始化时需要指定数据类型)
     * @return
     */
    boolean sqlNotBegin(Connection conn, String sql, List<ArgumentDomain> arguments) {
        int index = sql.length() - sql.replace("?", "").length();
        return conn == null || index != arguments.size();   //数据库连接无效或参数个数不匹配
    }
}
