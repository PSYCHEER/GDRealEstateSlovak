package me.EtienneDx.RealEstate.Transactions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExitOffer
{
	public UUID offerBy;
	public double price;
	
	public ExitOffer(UUID offerBy, double price)
	{
		this.offerBy = offerBy;
		this.price = price;
	}
	
	public ExitOffer(Map<String, Object> map)
	{
		offerBy = UUID.fromString((String)map.get("offerBy"));
		price = (double)map.get("price");
	}
}
