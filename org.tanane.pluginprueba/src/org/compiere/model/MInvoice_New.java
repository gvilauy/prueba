/**
 * 
 */
package org.compiere.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 * @author gabriel
 *
 */
public class MInvoice_New extends MInvoice {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4367050698071107722L;

	public MInvoice_New(MInvoice copy) {
		super(copy);
		// TODO Auto-generated constructor stub
	}

	public MInvoice_New(MInvoiceBatch batch, MInvoiceBatchLine line) {
		super(batch, line);
		// TODO Auto-generated constructor stub
	}

	public MInvoice_New(MOrder order, int C_DocTypeTarget_ID, Timestamp invoiceDate) {
		super(order, C_DocTypeTarget_ID, invoiceDate);
		// TODO Auto-generated constructor stub
	}

	public MInvoice_New(Properties ctx, int C_Invoice_ID, String trxName) {
		super(ctx, C_Invoice_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public MInvoice_New(Properties ctx, MInvoice copy, String trxName) {
		super(ctx, copy, trxName);
		// TODO Auto-generated constructor stub
	}

	public MInvoice_New(Properties ctx, MInvoice copy) {
		super(ctx, copy);
		// TODO Auto-generated constructor stub
	}

	public MInvoice_New(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	public MInvoice_New(MInOut ship, Timestamp invoiceDate) {
		super(ship, invoiceDate);
		// TODO Auto-generated constructor stub
	}

	
	private volatile static boolean recursiveCall = false;
	
	@Override
	protected boolean beforeSave(boolean newRecord) {
		
		log.fine("vamooooooo");
		
	
		//	No Partner Info - set Template
		if (getC_BPartner_ID() == 0)
			setBPartner(MBPartner.getTemplate(getCtx(), getAD_Client_ID()));
		if (getC_BPartner_Location_ID() == 0)
			setBPartner(new MBPartner(getCtx(), getC_BPartner_ID(), null));

		
		//	Price List
		if (getM_PriceList_ID() == 0)
		{
			int ii = Env.getContextAsInt(getCtx(), "#M_PriceList_ID");
			if (ii != 0)
			{
				MPriceList pl = new MPriceList(getCtx(), ii, null);
				if (isSOTrx() == pl.isSOPriceList())
					setM_PriceList_ID(ii);
			}
			
			if (getM_PriceList_ID() == 0)
			{
				String sql = "SELECT M_PriceList_ID FROM M_PriceList WHERE AD_Client_ID=? AND IsSOPriceList=? AND IsActive='Y' ORDER BY IsDefault DESC";
				ii = DB.getSQLValue (null, sql, getAD_Client_ID(), isSOTrx());
				if (ii != 0)
					setM_PriceList_ID (ii);
			}
		}

		//	Currency
		if (getC_Currency_ID() == 0)
		{
			String sql = "SELECT C_Currency_ID FROM M_PriceList WHERE M_PriceList_ID=?";
			int ii = DB.getSQLValue (null, sql, getM_PriceList_ID());
			if (ii != 0)
				setC_Currency_ID (ii);
			else
				setC_Currency_ID(Env.getContextAsInt(getCtx(), "#C_Currency_ID"));
		}

		//	Sales Rep
		if (getSalesRep_ID() == 0)
		{
			int ii = Env.getContextAsInt(getCtx(), "#SalesRep_ID");
			if (ii != 0)
				setSalesRep_ID (ii);
		}

		//	Document Type
		if (getC_DocType_ID() == 0)
			setC_DocType_ID (0);	//	make sure it's set to 0
		if (getC_DocTypeTarget_ID() == 0)
			setC_DocTypeTarget_ID(isSOTrx() ? MDocType.DOCBASETYPE_ARInvoice : MDocType.DOCBASETYPE_APInvoice);

		//	Payment Term
		if (getC_PaymentTerm_ID() == 0)
		{
			int ii = Env.getContextAsInt(getCtx(), "#C_PaymentTerm_ID");
			if (ii != 0)
				setC_PaymentTerm_ID (ii);
			else
			{
				String sql = "SELECT C_PaymentTerm_ID FROM C_PaymentTerm WHERE AD_Client_ID=? AND IsDefault='Y' AND IsActive='Y'";
				ii = DB.getSQLValue(null, sql, getAD_Client_ID());
				if (ii != 0)
					setC_PaymentTerm_ID (ii);
			}
		}
		
		// assign cash plan line from order
		if (getC_Order_ID() > 0 && getC_CashPlanLine_ID() <= 0) {
			MOrder order = new MOrder(getCtx(), getC_Order_ID(), get_TrxName());
			if (order.getC_CashPlanLine_ID() > 0)
				setC_CashPlanLine_ID(order.getC_CashPlanLine_ID());
		}

		// IDEMPIERE-1597 Price List and Date must be not-updateable
		if (!newRecord && (is_ValueChanged(COLUMNNAME_M_PriceList_ID) || is_ValueChanged(COLUMNNAME_DateInvoiced))) {
			int cnt = DB.getSQLValueEx(get_TrxName(), "SELECT COUNT(*) FROM C_InvoiceLine WHERE C_Invoice_ID=? AND M_Product_ID>0", getC_Invoice_ID());
			if (cnt > 0) {
				if (is_ValueChanged(COLUMNNAME_M_PriceList_ID)) {
					log.saveError("Error", Msg.getMsg(getCtx(), "CannotChangePlIn"));
					return false;
				}
				if (is_ValueChanged(COLUMNNAME_DateInvoiced)) {
					MPriceList pList =  MPriceList.get(getCtx(), getM_PriceList_ID(), null);
					MPriceListVersion plOld = pList.getPriceListVersion((Timestamp)get_ValueOld(COLUMNNAME_DateInvoiced));
					MPriceListVersion plNew = pList.getPriceListVersion((Timestamp)get_Value(COLUMNNAME_DateInvoiced));
					if (plNew == null || !plNew.equals(plOld)) {
						log.saveError("Error", Msg.getMsg(getCtx(), "CannotChangeDateInvoiced"));
						return false;
					}
				}
			}
		}

		if (! recursiveCall && (!newRecord && is_ValueChanged(COLUMNNAME_C_PaymentTerm_ID))) {
			recursiveCall = true;
			try {
				MPaymentTerm pt = new MPaymentTerm (getCtx(), getC_PaymentTerm_ID(), get_TrxName());
				boolean valid = pt.apply(this);
				setIsPayScheduleValid(valid);
			} catch (Exception e) {
				throw e;
			} finally {
				recursiveCall = false;
			}
		}

		if (!isProcessed())
		{
			MClientInfo info = MClientInfo.get(getCtx(), getAD_Client_ID(), get_TrxName()); 
			MAcctSchema as = MAcctSchema.get (getCtx(), info.getC_AcctSchema1_ID(), get_TrxName());
			if (as.getC_Currency_ID() != getC_Currency_ID())
			{
				if (isOverrideCurrencyRate())
				{
					if(getCurrencyRate() == null || getCurrencyRate().signum() == 0)
					{
						log.saveError("FillMandatory", Msg.getElement(getCtx(), COLUMNNAME_CurrencyRate));
						return false;
					}
				}
				else
				{
					setCurrencyRate(null);
				}
			}
			else
			{
				setCurrencyRate(null);
			}
		}
		
		return true;		
	}

	
	
	
}
