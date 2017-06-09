package com.hb.wss.dao;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * 数据库连接类
 * 包含数据库操作工具方法
 * 适用于：Oracle
 * Created by Hongbo on 2017/6/9.
 */
public class ConnectDB {

    public String[] dburi;

    public String[] getDburi() {
        return dburi;
    }
    public void setDburi(String[] dburi) {
        this.dburi = dburi;
    }
    private String str_date = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]").format(new Date());

    public ConnectDB () {}
    /**
     * 连接Oracle
     * @param ip 主机IP
     * @param port 端口
     * @param sid 实例名
     * @param user 用户名
     * @param password 密码
     * @return
     */
    public Connection Connect4Oracle (String ip, Integer port, String sid, String user, String password) {
        Connection conn = null;
        String url = "jdbc:oracle:thin:@" + ip + ":" + port + ":" + sid;	//数据库连接URL
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");			//反射获取数据库驱动类
            conn = DriverManager.getConnection(url, user, password);	//数据库连接对象
        } catch (ClassNotFoundException e) {
            System.err.println(str_date + "Oracle数据库连接驱动找不到！");
        } catch (SQLException e) {
            System.err.println(str_date + "数据库连接异常！");
        }
        return conn;
    }
    /**
     * 断开连接
     * @param conn
     */
    public void disconn (Connection conn) {
        if (conn != null)
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
    }
    /**
     * 执行查询语句，按照结果返回list
     * @param ip 数据库IP
     * @param port 端口
     * @param sid 实例名
     * @param user 用户名
     * @param password 密码
     * @param sql 查询语句
     * @return
     */
    public List<Map<String, Object>> query(String ip, Integer port, String sid, String user, String password, String sql) {
        List<Map<String, Object>> list = new ArrayList<>();
        Connection conn = Connect4Oracle(ip, port, sid, user, password);
        if (conn == null) {
            return list;
        }
        PreparedStatement pre = null;   //创建预编译对象
        ResultSet result = null;        //创建结果集
        try {
            pre = conn.prepareStatement(sql);   //执行sql
            result = pre.executeQuery();	    //执行结果
            list.addAll(result2List(result));
        } catch (SQLException e) {
            System.err.println(str_date + "SQL执行异常！");
        } finally {
            closeAll(result, pre, conn);
        }
        return list;
    }
    /**
     * 执行查询语句，按照结果返回list
     * 数据库连接参数使用数组传递
     * @param sql 查询语句
     * @return
     */
    public List<Map<String, Object>> query(String sql){
        return query(dburi[0], Integer.parseInt(dburi[1]), dburi[2], dburi[3], dburi[4], sql);
    }
    /**
     * 将set集合转换为list
     * @param rs 结果集
     * @return
     */
    public List<Map<String, Object>> result2List(ResultSet rs) {
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
     * 执行出参为游标的存储过程
     * @param ip
     * @param port
     * @param sid
     * @param user
     * @param password
     * @return
     */
    public List<Map<String, Object>> callPrc4Cursor(String ip, Integer port, String sid, String user, String password){
        List<Map<String, Object>> list = new ArrayList<>();  //返回结果
        Connection conn = Connect4Oracle(ip, port, sid, user, password);        //建立数据库连接
        if (conn == null) {
            return list;
        }
        String sql = "{call prc_upload(?)}";    //存储过程名称
        CallableStatement proc = null;          //存储过程调用对象
        ResultSet result = null;                //结果集对象
        try {
            proc = conn.prepareCall(sql);
            proc.registerOutParameter(1, oracle.jdbc.OracleTypes.CURSOR);   //出参：游标类型
            proc.execute();                     //执行
            result = (ResultSet) proc.getObject(1);
            list.addAll(result2List(result));
        } catch (SQLException e) {
            System.err.println(str_date + "执行存储过程异常！");
        } finally {
            closeAll(result, proc, conn);
        }
        return list;
    }
    /**
     * 执行存储过程
     */
    public List<Map<String, Object>> callPrc4Cursor(){
        return callPrc4Cursor(dburi[0], Integer.parseInt(dburi[1]), dburi[2], dburi[3], dburi[4]);
    }
    /**
     * 执行DML语句
     * @param ip
     * @param port
     * @param sid
     * @param user
     * @param password
     * @param sql
     * @return 影响行数
     */
    public int update(String ip, Integer port, String sid, String user, String password, String sql) {
        int rows = 0;	//更新行数
        Connection conn = Connect4Oracle(ip, port, sid, user, password);
        if (conn == null) {
            return rows;	//直接返回0
        }
        PreparedStatement pre = null;//创建预编译对象
        ResultSet result = null;//创建结果集
        try {
            pre = conn.prepareStatement(sql);//预编译
            rows = pre.executeUpdate();	//执行DML语句
        } catch (SQLException e) {
            System.err.println(str_date + "SQL执行异常！");
        } finally {
            closeAll(result, pre, conn);
        }
        return rows;
    }
    /**
     * 执行DML语句
     * @param sql
     * @return
     */
    public int update(String sql) {
        return update(dburi[0], Integer.parseInt(dburi[1]), dburi[2], dburi[3], dburi[4], sql);
    }
    /******************* 增加事务控制 *********************/
    /**
     * 事务控制：开始
     * @param conn
     */
    public void transBegin (Connection conn) {
        if (conn == null) return;
        try {
            if (conn.getAutoCommit()) {
                conn.setAutoCommit(false);
            }
        } catch (SQLException e) {
            System.err.println(str_date + "事务开始异常！");
        }
    }
    /**
     * 事务提交
     * @param conn
     */
    public void transCommit (Connection conn) {
        if (conn == null) return;
        try {
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
        } catch (SQLException e) {
            System.err.println(str_date + "事务提交异常！");
        }
    }
    /**
     * 事务回滚
     * @param conn
     */
    public void transRollback (Connection conn) {
        if (conn == null) return;
        try {
            if (conn.getAutoCommit()) {
                conn.rollback();
            }
        } catch (SQLException e) {
            System.err.println(str_date + "事务回滚异常！");
        }
    }
    /**
     * 查询（引入数据库连接）
     * @param conn
     * @param sql
     * @return
     */
    public List<Map<String, Object>> query (Connection conn, String sql) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (conn == null) {
            return list;
        }
        PreparedStatement pre = null;   //预编译对象
        ResultSet result = null;        //结果集对象
        try {
            pre = conn.prepareStatement(sql);   //预编译
            result = pre.executeQuery();	    //执行语句
            list = result2List(result);
        } catch (SQLException e) {
            System.err.println(str_date + "语句编译异常！");
        } finally {
            closeAll(result, pre);
        }
        return list;
    }
    /**
     * 更新（引入数据库连接）
     * @param conn
     * @param sql
     * @return
     */
    public int update (Connection conn, String sql) {
        int rows = 0;	    //更新行数
        if (conn == null) {
            return rows;	//直接返回0
        }
        PreparedStatement pre = null;   //预编译对象
        ResultSet result = null;        //结果集对象
        try {
            pre = conn.prepareStatement(sql);   //预编译
            rows = pre.executeUpdate();         //更新返回影响行数
        } catch (SQLException e) {
            System.err.println(str_date + "SQL更新异常");
        } finally {
            closeAll(result, pre);
        }
        return rows;
    }

    /**
     * 关闭对象
     * @param rs
     * @param pre
     */
    public void closeAll (ResultSet rs, PreparedStatement pre) {
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
     * 关闭对象
     * @param rs
     * @param pre
     * @param conn
     */
    public void closeAll (ResultSet rs, PreparedStatement pre, Connection conn) {
        try {
            if (rs != null)
                rs.close();
            if (pre != null)
                pre.close();
            disconn(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
