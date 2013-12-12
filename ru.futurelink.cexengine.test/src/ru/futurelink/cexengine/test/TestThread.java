package ru.futurelink.cexengine.test;

import java.math.BigDecimal;
import java.util.ArrayList;

import ru.futurelink.cexengine.orm.TradeTool;
import ru.futurelink.cexengine.orm.TradeAccount;
import ru.futurelink.cexengine.trade.OrderException;
import ru.futurelink.cexengine.trade.TradeService;
import ru.futurelink.cexengine.trade.TradeServiceInstance;

class TestThread extends Thread {
    private Short 			mType;
    private TradeService	mTradeService;
    private ArrayList<TradeAccount> mAccounts;

    private static Double mMiddleAmount = 40.0;
    private static Double mMiddlePrice = 30.0;

    public TestThread(Short type, TradeService tradeService, ArrayList<TradeAccount> accounts) {
    	mType = type;
    	mTradeService = tradeService;
    	mAccounts = accounts;
    }

    @Override
    public void run() {
		TradeServiceInstance instance = mTradeService.CreateInstance();

		TradeTool tool = instance.GetTool("USD/RUR");
		if (tool != null) {
			// Запускаем в бесконечном цикле генерацию ордеров
			for(;;) {
				BigDecimal amount = new BigDecimal(Math.round(Math.random()*mMiddleAmount)+1);
				BigDecimal price = new BigDecimal(Math.round(Math.random()*mMiddlePrice)+1);
				Integer accountNumber = (int) Math.round(Math.random()*9)+1; // Всего аккаунтов от 0 до 10

				try {
					instance.PlaceOrder(tool, mType, amount, price, mAccounts.get(accountNumber-1));
				} catch (OrderException e) {
					e.printStackTrace();
				}
				
				try {
					sleep(200); // Ждем 100мс
				} catch (InterruptedException ex) {
					
				}
			}
		}
    }
}