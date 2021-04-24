/**
 * 
 */
package org.compiere.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import org.adempiere.base.DefaultDocumentFactory;
import org.adempiere.base.IDocFactory;
import org.compiere.acct.Doc;
import org.compiere.acct.Doc_Invoice;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * @author gabriel
 *
 */
public class MyDocFactory implements IDocFactory {


	private final static CLogger s_log = CLogger.getCLogger(DefaultDocumentFactory.class);
	
	@Override
	public Doc getDocument(MAcctSchema as, int AD_Table_ID, int Record_ID, String trxName) {

		String tableName = MTable.getTableName(Env.getCtx(), AD_Table_ID);
		Doc doc = null;
		StringBuffer sql = new StringBuffer(" select * from ")
				.append(tableName)
				.append(" where ").append(tableName).append("_ID =? and processed ='Y'");
		
		PreparedStatement pstm = null;
		ResultSet rs = null;
		
		try {
			pstm = DB.prepareStatement(sql.toString(), trxName);
			pstm.setInt(1, Record_ID);
			rs = pstm.executeQuery();
			if (rs.next()) {
				doc = getDocument(as, AD_Table_ID, rs, trxName);
			}
			else {
				s_log.severe("No se encuentra: " + tableName + "_ID = " + Record_ID);
			}
			
		} 
		catch (Exception e) {
			s_log.log(Level.SEVERE, sql.toString(), e);
		}
		finally {
			DB.close(rs, pstm);
			rs = null;
			pstm = null;				
		}
	
		return doc;
	}

	@Override
	public Doc getDocument(MAcctSchema as, int AD_Table_ID, ResultSet rs, String trxName) {
		
		Doc doc = null;
		String tableName = MTable.getTableName(Env.getCtx(), AD_Table_ID);
		
		if (tableName.equalsIgnoreCase("C_Invoice")) {
			doc = new org.tanane.acct.Doc_Invoice(as, rs, trxName);
		}
		return doc;
	}

}
