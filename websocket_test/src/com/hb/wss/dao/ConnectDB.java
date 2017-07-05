package com.hb.wss.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 数据库连接类
 * 包含数据库操作工具方法
 * 适用于：Oracle
 * Created by Hongbo on 2017/6/9.
 */
public class ConnectDB {
    protected static String str_date = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]").format(new Date());
    private static Connection conn = null;
    public static Connection getConnection () {
        if (conn == null) return null;
        try {
            return conn.isClosed() ? null : conn;
        } catch (SQLException e) {
            System.out.println("获取数据库连接异常：" + e.getMessage());
            return null;
        }
    }
    /**
     * 连接Oracle
     * @param ip 主机IP
     * @param port 端口
     * @param sid 实例名
     * @param user 用户名
     * @param password 密码
     * @return
     */
    public static Connection Connect4Oracle (String ip, Integer port, String sid, String user, String password) {
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
    public static void disconn (Connection conn) {
        if (conn != null)
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
    }

    /******************* 增加事务控制 *********************/
    /**
     * 事务控制：开始
     * @param conn
     */
    public static void transBegin (Connection conn) {
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
    public static void transCommit (Connection conn) {
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
    public static void transRollback (Connection conn) {
        if (conn == null) return;
        try {
            if (conn.getAutoCommit()) {
                conn.rollback();
            }
        } catch (SQLException e) {
            System.err.println(str_date + "事务回滚异常！");
        }
    }

}
