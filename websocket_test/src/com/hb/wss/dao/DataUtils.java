package com.hb.wss.dao;

import com.hb.wss.domain.ArgumentDomain;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

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
    public static List<Map<String, Object>> execDQL(Connection conn, String sql, List<ArgumentDomain> arguments) {
        List<Map<String, Object>> result = new ArrayList();
        if (sql.toUpperCase().indexOf("INSERT ") > 0 || sql.toUpperCase().indexOf("DELETE ") > 0 || sql.toUpperCase().indexOf("UPDATE ") > 0) return result;    //语句包含DML关键字
        if (sqlNotBegin(conn, sql, arguments)) return result;
        PreparedStatement pre = null;   //初始化预编译对象
        ResultSet rs = null;    //初始化结果集对象
        try {
            pre = conn.prepareStatement(sql); //预编译
            ArgumentDomain argument;
            for (int i = 0; i < arguments.size(); i++) {
                argument = arguments.get(i);    //获取参数对象
                pre.setObject(argument.getPosition(), argument.getVal(), argument.getJdbcType());    //传值
            }
            rs = pre.executeQuery();
            result.addAll(result2List(rs));
        } catch (SQLException e) {
            System.err.println(str_date + "SQL编译异常！");
        } finally {
            closeAll(rs, pre);
        }
        return result;
    }

    /**
     * 增、删、改
     * @param conn 数据库连接
     * @param sql 查询语句
     * @param arguments 参数集合(初始化时需要指定数据类型)
     * @return
     */
    public static int execDML(Connection conn, String sql, List<ArgumentDomain> arguments) {
        int sqlRowCount = 0;
        if (sqlNotBegin(conn, sql, arguments)) return sqlRowCount;
        PreparedStatement pre = null;   //初始化预编译对象
        ResultSet rs = null;    //初始化结果集对象
        try {
            pre = conn.prepareStatement(sql); //预编译
            ArgumentDomain argument;
            for (int i = 0; i < arguments.size(); i++) {
                argument = arguments.get(i);    //获取参数对象
                pre.setObject(argument.getPosition(), argument.getVal(), argument.getJdbcType());    //传值
            }
            sqlRowCount = pre.executeUpdate();
        } catch (SQLException e) {
            System.err.println(str_date + "SQL编译异常:" + e.getMessage());
        } finally {
            closeAll(rs, pre);
        }
        return sqlRowCount;
    }

    /**
     * 执行存储过程
     * 由于视图dba_arguments需要用户具有访问权限，目前只支持调用当前用户的存储过程
     * @param conn 数据库连接
     * @param sql 存储过程名[pkg_name.]prc_name
     * @param arguments 参数集合(初始化时需要指定数据类型)
     * @return
     */
    public static Map<String, Object> execPrc(Connection conn, String sql, Map<String, Object> arguments) {
        Map<String, Object> result = new HashMap();
        if (conn == null) return result;    //数据库连接无效，退出
        String[] str = sql.split("\\.");    //分割包名与存储过程名
        /*查询存储过程执行参数*/
        List<ArgumentDomain> argList = new ArrayList();
        switch (str.length) {
            case 1: //"prc_name"
                argList.add(new ArgumentDomain(1, "varchar", str[0]));
                break;
            case 2: //"pkg_name.prc_name"
                argList.add(new ArgumentDomain(1, "varchar", str[1]));
                argList.add(new ArgumentDomain(2, "varchar", str[0]));
                break;
            default:return result;
        }
        String argSql = "SELECT /*+ RESULT_CACHE */ + lower(argument_name) as argument_name, lower(data_type) as data_type, "
                        + "lower(in_out) as in_out, position FROM user_arguments where object_name= upper(?) "
                        + "and package_name " + ((str.length == 1) ? "is null" : "=upper(?)") + " order by POSITION";
        List<Map<String, Object>> paramList = execDQL(conn, argSql, argList);   //查询过程执行参数
        CallableStatement cstmt = null;
        /*存储过程执行语句初始化*/
        StringBuffer prcSql = new StringBuffer("");
        prcSql.append("{ call ");
        prcSql.append(sql);
        if (paramList.size() > 0) {
            prcSql.append("(");
            for (int i = 0; i < paramList.size(); i++) {
                prcSql.append("?,");
            }
            prcSql.replace(prcSql.length() - 1, prcSql.length(), ")");
        }
        prcSql.append("}");
        try {
            cstmt = conn.prepareCall(prcSql.toString());    //预编译
            String in_out, argument_name, data_type;
            int position;
            List<Map<String, Object>> outList = new ArrayList();
            for (Map<String, Object> map : paramList) {
                in_out = map.get("IN_OUT").toString();
                argument_name = map.get("ARGUMENT_NAME").toString();
                position = ((BigDecimal)map.get("POSITION")).intValue();
                data_type = map.get("DATA_TYPE").toString();
                if ("in".equalsIgnoreCase(in_out)) {
                    if (arguments.containsKey(argument_name)) {
                        cstmt.setObject(position, arguments.get(argument_name), ArgumentDomain.transferSQLType(data_type));
                    } else {
                        System.err.println(str_date + "参数不正确！");
                        return result;
                    }
                } else if ("out".equalsIgnoreCase(in_out)){
                    cstmt.registerOutParameter(position, ArgumentDomain.transferSQLType(data_type));
                    outList.add(map);
                }
            }
            cstmt.execute();    //执行
            /*出参处理*/
            for (Map<String, Object> map : outList) {
                in_out = map.get("IN_OUT").toString();
                argument_name = map.get("ARGUMENT_NAME").toString();
                position = ((BigDecimal)map.get("POSITION")).intValue();
                data_type = map.get("DATA_TYPE").toString();
                if ("out".equalsIgnoreCase(in_out)) {
                    if ("ref cursor".equalsIgnoreCase(data_type)) {
                        result.put(argument_name, result2List((ResultSet) cstmt.getObject(position)));
                    } else {
                        result.put(argument_name, cstmt.getObject(position));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(str_date + "SQL编译异常:" + e.getMessage());
        } finally {
            if (cstmt != null)
                try {
                    cstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
        }
        return result;
    }

    /**
     * 将set集合转换为list
     * 对异常进行了处理，并关闭了结果集
     * @param rs 结果集
     * @return
     */
    public static List<Map<String, Object>> result2List(ResultSet rs) {
        List<Map<String, Object>> list = new ArrayList<>();
        ResultSetMetaData rsmd = null;
        Map<String, Object> map = null;
        try {
            while (rs.next()) {
                rsmd = rs.getMetaData();
                map = new LinkedHashMap<>();	//确保各列顺序
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    map.put(rsmd.getColumnLabel(i), rs.getObject(i));
                }
                list.add(map);
            }
        } catch (SQLException e) {
            System.err.println(str_date + "读取结果集异常！");
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    System.err.println(str_date + "关闭结果集异常！");
                }
            }
        }
        return list;
    }

    /**
     * 关闭对象
     * @param rs
     * @param pre
     */
    private static void closeAll (ResultSet rs, PreparedStatement pre) {
        try {
            if (rs != null)
                rs.close();
            if (pre != null)
                pre.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断是否可以开始SQL操作
     * @param conn 数据库连接
     * @param sql 查询语句
     * @param arguments 参数集合(初始化时需要指定数据类型)
     * @return
     */
    private static boolean sqlNotBegin(Connection conn, String sql, List<ArgumentDomain> arguments) {
        int index = sql.length() - sql.replace("?", "").length();
        return conn == null || index != arguments.size();   //数据库连接无效或参数个数不匹配
    }

}
