package com.billzsoft.send.dao;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.billzsoft.dbHelper.WDbConnection;
import com.billzsoft.read.model.SmsServerOut;

/**
 * @description 在外网，串口端，接收串口数据。然后提交到mas映射的数据库
 * @class DbHelper
 * @author YHZ
 * @date 2013-6-24
 */
public class WDbHelper extends WDbConnection{

	/**
	 * 发送短信到外网
	 * @param sobs SmsOutBox
	 */
	public void insertSmsServerOutStatus(List<SmsServerOut> sobs){
		try {
			String sql = "insert into SMS_OUTBOX(SISMSID,DESTADDR,MESSAGECONTENT,REQDELIVERYREPORT,MSGFMT,SENDMETHOD,REQUESTTIME,APPLICATIONID) values(?,?,?,?,?,?,?,?)";
			conn = getConnection();
			conn.setAutoCommit(false); 
			pst = conn.prepareStatement(sql);
			// 批量修改
			for(SmsServerOut sob : sobs){
				if(StringUtils.isNotEmpty(sob.getRecipient())){
					pst.setString(1, java.util.UUID.randomUUID().toString());
					pst.setString(2, sob.getRecipient());
					pst.setString(3, sob.getText());
					pst.setInt(4, 1);
					pst.setInt(5, 8);
					pst.setInt(6, 0);
					pst.setDate(7, new java.sql.Date(new java.util.Date().getTime()));
					pst.setString(8, "P000000000000003");
					pst.addBatch();					
				}
			}
			pst.executeBatch();
			conn.commit(); 
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeAll(null, pst, conn);
		}
	}

}
