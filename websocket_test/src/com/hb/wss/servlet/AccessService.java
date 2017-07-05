package com.hb.wss.servlet;


import com.hb.wss.dao.ConnectDB;
import com.hb.wss.domain.ArgumentDomain;
import com.hb.wss.utils.DataUtils;
import com.hb.wss.utils.HttpClientKeepSession;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 访问服务器，上传数据
 * Created by Hongbo on 2017/6/20.
 */
public class AccessService {
    static Logger logger = LogManager.getLogger(AccessService.class.getName());
    String uri; //上传路径
    String db_host; //源数据主机
    Integer db_port;    //源数据端口
    String db_sid;  //源数据实例名
    String db_username; //源数据用户名
    String db_passwd;   //源数据密码

    HttpClientKeepSession hcks = new HttpClientKeepSession();    //客户端连接对象
    int logincnt = 0;    //计数器：重复登陆次数
    int LOGIN_CNT = 100;   //允许重复登陆次数
    int PRE_UPLOAD_CNT = 300; //每次上传数据量
    Long max_cnt;   //最大上传条数
    int sqlerror_cnt = 0;   //
    Connection conn;

    AccessService(String upload_uri, String db_host, Integer db_port, String db_sid, String db_username, String db_passwd) {
        setUri(upload_uri);
        setDb_host(db_host);
        setDb_port(db_port);
        setDb_sid(db_sid);
        setDb_username(db_username);
        setDb_passwd(db_passwd);

        System.out.println("请输入本次程序执行最大上传条数(回车结束)：");
        Scanner scanner = new Scanner(System.in);
        max_cnt = scanner.nextLong();
        System.out.println("请输入每次上传数据的条数(回车结束)：");
        PRE_UPLOAD_CNT = scanner.nextInt();
        if (PRE_UPLOAD_CNT > max_cnt) {
            PRE_UPLOAD_CNT = max_cnt.intValue();
        }

        LOGIN_CNT = max_cnt.intValue() / PRE_UPLOAD_CNT << 3;
        logger.info("本次计划上传总数：" + max_cnt + "条，次均上传：" + PRE_UPLOAD_CNT + "条。允许最大错误次数：" + LOGIN_CNT + "次。");
        Date d1 = new Date();

        Long cnt = upload ();

        Date d2 = new Date();

        Long h = (d2.getTime() - d1.getTime()) / (1000 * 60 * 60);
        Long min = (d2.getTime() - d1.getTime()) / (1000 * 60) - h * 60;
        Long s = (d2.getTime() - d1.getTime()) / 1000 - h * 60 * 60 - min * 60;
        logger.info("耗时：" + h + "小时" + min + "分" + s + "秒" + "，共上传数据：" + cnt + "条。");
    }

    /**
     * 上传入口
     * @return 上传记录条数
     */
    public Long upload () {
        JSONObject json;
        //1.登陆，保留cookie
        try {
            json = login();
        } catch (NullPointerException e) {
            logger.warn("模拟登陆响应结果异常，释放资源后尝试重连！");
            logout();
            return upload();
        } catch (IOException e) {
            logger.warn("登陆POST请求异常，释放资源后尝试重连！");
            logout();
            return upload();
        }
        if (logincnt > LOGIN_CNT) {
            logger.fatal("重复登陆次数已达上限，退出！");
            return 0L;
        }
        Long cnt = 0L;
        do {
            //2-1.打开数据库连接
            setConn();
            ConnectDB.transBegin(getConn());

            try {
                //2-2.装配上传数据
                loadData(json);
                //2-3.报盘
                cnt += upload(json);
            } catch (SQLException e) {
                logout();
                return upload();
            } catch (IOException e) {
                logger.error("上传POST请求异常：" + e.getMessage());
                logout();
                return upload();
            } catch (Exception e) {
                if ("会话超时".equalsIgnoreCase(e.getMessage())) {
                    logout();
                    return upload();
                } else {
                    logger.fatal("新的异常：" + e.getMessage());
                }
            } finally {
                ConnectDB.transCommit(conn);
                ConnectDB.disconn(conn);
            }
            if (logincnt > LOGIN_CNT) return cnt;
        } while (cnt >= 0 && cnt < max_cnt);
        //3.退出
        logout();
        return cnt;
    }

    /**
     * 上传参数装配
     * @param json
     * @return
     */
    public JSONObject loadData (JSONObject json) throws SQLException {
        json.put("serviceid", "saveExcelDatasService");
        JSONArray rows;
        try {
            rows = query();   //取出用于上报的数据
        } catch (SQLException e) {
            logger.warn("取上报数据异常：" + e.getMessage());
            throw e;
        }
        JSONObject params = new JSONObject();
        params.put("rows", rows);
        JSONArray datas = new JSONArray();
        datas.add(params);
        json.put("params", params);
        json.put("datas", datas);
        logger.trace("报盘参数：" + json.toString());
        return json;
    }

    /**
     * 报盘
     * @param json
     * @return
     * @throws Exception
     */
    public int upload (JSONObject json) throws Exception {
        if (json == null) return 0; //判断上一步是否成功
        JSONArray rows = json.getJSONObject("params").getJSONArray("rows");
        int cnt = rows.size();

        String str_message;
        JSONObject result = hcks.post(uri, json);
        try {
            str_message = result.getString("message");
        } catch (NullPointerException e) {
            logger.warn("post请求结果为空：" + e.getMessage());
            return upload(json);
        }
        if ("批量参保数据保存成功，请到审核界面进行审核!".equalsIgnoreCase(str_message)) {
            if (cnt > 0) writeBack("", ""); //避免空提交
        } else if (str_message.matches("身份证为【\\d{17}(\\d|X|x)】的人员在【(社保机构核对数据|参保登记)审核】中还存在未审核的记录，不能重复参保!")) {
            Pattern p = Pattern.compile("\\d{17}(\\d|X|x)");
            Matcher m = p.matcher(str_message);
            while (m.find()) {
                String aac002 = m.group();
                for (int i = 0; i < rows.size(); i++) {
                    JSONObject jb = rows.getJSONObject(i);
                    if (aac002.equals(jb.getString("aac002"))) {
                        writeBack(str_message, jb.getString("aab001"));
//                        cnt -= 1;
                        break;  //只提示一个，找到即退出
                    }
                }
            }
            cnt = 0;    //服务端事务可能是遇到错误全部回滚，所以暂时将计数器归零
        } else if ("系统错误，请与系统管理员联系!".equalsIgnoreCase(str_message)) {
            logincnt += 1;
            if (logincnt > LOGIN_CNT) return 0;
            return upload(json);
        } else if ("用户未登录，或者超时，请重新登录！".equalsIgnoreCase(str_message)) {
            throw new Exception("会话超时");
        } else {
            //TODO 判断更新行数
            logger.fatal("遇到未知的响应信息：" + str_message);
            logincnt = LOGIN_CNT + 1;   //利用错误阈值退出
        }
        return cnt;
    }

    /**
     * 登陆
     * @return 登陆成功后信息
     * @throws IOException
     */
    public JSONObject login() throws IOException {
        String str_json = "{\"serviceid\":\"syslogin\",\"target\":\"\",\"sessionid\":null,\"loginname\":null,\"password\":null,\"params\":{\"username\":\"lf_1\",\"passwd\":\"21218cca77804d2ba1922c33e0151105\"},\"datas\":[{\"username\":\"lf_1\",\"passwd\":\"21218cca77804d2ba1922c33e0151105\"}]}";
        JSONObject json = JSONObject.fromObject(str_json);
        logger.info("用户登陆");
        JSONObject result = hcks.post(uri, json);
        logincnt += 1;
        if ("".equals(result.get("sessionid")) || !"lf_1".equalsIgnoreCase(result.getString("loginname"))) {
            if (logincnt > LOGIN_CNT) {
                logger.fatal("重复登陆次数已达上限，准备退出！");
                logout();
            } else {
                logger.error("登陆响应用户信息与请求不符，重新登陆！");
                return login();
            }
        }
        return result;
    }

    /**
     * 注销登陆
     */
    public void logout() {
        logger.info("退出登陆，事务回滚，释放连接！");
        ConnectDB.transRollback(conn); //事务在回写时提交，其他情况都回滚
        ConnectDB.disconn(conn);
        HttpClientUtils.closeQuietly(hcks.httpClient);
    }

    /**
     * 获取上传数据
     * @return
     * @throws SQLException
     */
    public JSONArray query() throws SQLException {
        JSONArray rows = new JSONArray();
        int cnt_1 = Integer.parseInt(DataUtils.execDQL(getConn(), "select count(*) cnt from tab_upload where flag = 1", new ArrayList<>()).get(0).get("CNT").toString());
        int cnt_2;
        if (cnt_1 <= PRE_UPLOAD_CNT) {
            cnt_2 = DataUtils.execDML(conn, "update tab_upload set flag = 1 where flag = 0 and rownum <= " + (PRE_UPLOAD_CNT - cnt_1), new ArrayList<>());
        } else {
            cnt_2 = DataUtils.execDML(conn, "update tab_upload set flag = 0 where flag = 1 and rownum <= " + (cnt_1 - PRE_UPLOAD_CNT), new ArrayList<>());
        }
        if (cnt_2 > 0) {
            Map<String, Object> datas = DataUtils.execPrc(conn, "prc_upload", new HashMap<>());
            rows = JSONArray.fromObject(datas.get("prm_upload"));
        }
        return rows;
    }

    /**
     * 标志回写
     * @param message
     * @param aac001
     * @return
     */
    public int writeBack(String message, String aac001) {
        int rows;
        try {
            //判断上传结果
            if ("".equals(message)) {
                rows = DataUtils.execDML(conn, "update tab_upload set flag = to_number(to_char(sysdate, 'yymmddhh24miss')) where flag = 1", new ArrayList<>());
            } else {
                List<ArgumentDomain> list = new ArrayList<>();
                list.add(new ArgumentDomain(1, "varchar2", message));
                list.add(new ArgumentDomain(2, "varchar2", aac001));
                rows = DataUtils.execDML(conn, "update tab_upload set flag = 4, msg = ? where flag = 1 and aac001 = ?", list);
            }
        } catch (SQLException e) {
            logger.warn("更新上传临时表标志异常：" + e.getMessage());
            return writeBack(message, aac001);
        }
        return rows;
    }

    /**
     * 获取数据库连接
     * @return
     * @throws SQLException
     */
    public Connection getConn() {
        try {
            if (conn != null && !conn.isClosed()) {
                return conn;
            } else {
                logger.error("数据库连接为空或已关闭！");
                setConn();
            }
        } catch (SQLException e) {
            logger.error("数据库连接异常！");
            setConn();
        }
        return getConn();
    }

    /**
     * 数据库连接
     */
    public void setConn() {
        this.conn = ConnectDB.Connect4Oracle(db_host, db_port, db_sid, db_username, db_passwd);
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setDb_host(String db_host) {
        this.db_host = db_host;
    }

    public void setDb_port(Integer db_port) {
        this.db_port = db_port;
    }

    public void setDb_sid(String db_sid) {
        this.db_sid = db_sid;
    }

    public void setDb_username(String db_username) {
        this.db_username = db_username;
    }

    public void setDb_passwd(String db_passwd) {
        this.db_passwd = db_passwd;
    }
}
