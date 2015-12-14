package org.entermedia.elasticsearch.searchers;

import org.openedit.Data;
import org.openedit.data.PropertyDetails;
import org.openedit.entermedia.orders.Order;
import org.openedit.entermedia.orders.OrderManager;

public class ElasticOrderSearcher extends BaseElasticSearcher
{
	
	protected OrderManager fieldOrderManager;
	
	
	public OrderManager getOrderManager()
	{
		return fieldOrderManager;
	}

	public void setOrderManager(OrderManager inOrderManager)
	{
		fieldOrderManager = inOrderManager;
	}

	public Data createNewData()
	{
		Order order = (Order)super.createNewData();
		order.setCatalogId(getCatalogId());
		order.setOrderManager(getOrderManager());
		return order;
	}
	
//	protected void updateIndex(Data inData, Document doc, PropertyDetails inDetails) 
//	{
//		getOrderManager().loadOrderHistory(getCatalogId(),(Order)inData);
//		super.updateIndex(inData, doc, getPropertyDetails());
//	}
	
	
	protected void updateElasticIndex(PropertyDetails details, Data inData) {
		//getOrderManager().loadOrderHistory(getCatalogId(),(Order)inData);
		super.updateElasticIndex(details, inData);
	}
	
}
