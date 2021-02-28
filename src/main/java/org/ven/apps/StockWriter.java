package org.ven.apps;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StockWriter {
    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException, InterruptedException {

        //Class.forName ("org.h2.Driver");

        Class.forName("org.apache.derby.jdbc.ClientDriver");


      //  Connection conn = DriverManager.getConnection ("jdbc:h2:file:/c:/VenRoot/StockDB",
        //        "ven","venkat123");

                Connection conn = DriverManager.getConnection ("jdbc:derby://127.0.0.1:1527//VENROOT/DerbyDb/MyDbTest;create=true");

                        Statement st = conn.createStatement();

        Statement st2 = conn.createStatement();

        List<String> sql = new ArrayList<>();;
        ResultSet rs = st.executeQuery("Select Symbol from Ven.SYMBOLSTABLE order by Symbol");
        while(rs.next())
        {
            String symbol = rs.getString(1);
           // System.out.println("LOOKING FOR SYMBOL..." +symbol)  ;
            BigDecimal price = null;
            try
            {
                price =
                        YahooFinance.get(
                        symbol.replace(" ", "").replace(".", "")).getQuote().getPrice();
                if (price == null) {
                    System.out.println("NO PRICE FOR " + symbol);
                    continue;
                }
            }catch(Exception e){e.printStackTrace();
                System.out.println(symbol);}


           // System.out.println(symbol);
           // System.out.println(price);


            if (price != null) {
                String query = " Update Ven.MarketPrice set Price=" + price +
                        ", LUD=" + System.currentTimeMillis() + " " + " where Symbol='" + symbol + "'";
                sql.add(query);
               // System.out.println(query);

                int s = st2.executeUpdate(query);
            }
//            Thread.sleep(000);

        }


         rs = st.executeQuery("Select Symbol, Price from Ven.MarketPrice order by Symbol");
        while(rs.next())
        {
            String symbol = rs.getString(1);
            BigDecimal  price = rs.getBigDecimal(2);

            System.out.println("AFTER :"  + symbol + " " + price);

        }




        System.out.println( "price=" + YahooFinance.get(args[0]).getQuote().getPrice());
    }
}
