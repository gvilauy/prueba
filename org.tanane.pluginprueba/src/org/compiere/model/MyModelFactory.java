/**
 * 
 */
package org.compiere.model;

import java.sql.ResultSet;

import org.adempiere.base.IModelFactory;
import org.compiere.util.Env;

/**
 * @author gabriel
 *
 */
public class MyModelFactory implements IModelFactory {

	@Override
	public Class<?> getClass(String tableName) {

		if (tableName.equalsIgnoreCase(MInvoice_New.Table_Name)) {
			return MInvoice_New.class;
		}
		
		return null;
	}

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName) {

		if (tableName.equalsIgnoreCase(MInvoice_New.Table_Name)) {
			return new MInvoice_New(Env.getCtx(), Record_ID, trxName);
		}		
		
		return null;
	}

	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName) {

		if (tableName.equalsIgnoreCase(MInvoice_New.Table_Name)) {
			return new MInvoice_New(Env.getCtx(), rs, trxName);
		}		
		
		return null;
	}

}
