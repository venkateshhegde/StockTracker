package org.ven.apps;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;


import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class StockTracker {


    static Map<String, List<DataModel>> mapOfSymbolsToCSVs = new TreeMap<>();
    static Map<String, List<DataModel>> mapOfDateToCSVs = new TreeMap<>();


    private static Logger logger = LoggerFactory.getLogger(StockTracker.class);
    public static void main(String[] args) throws IOException {
        File file = new File(args[0]);
        logger.info("File " + args[0] + " exists?" + file.exists());

        int count = 100000;
        Reader in = new FileReader(args[0]);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(in);
        for (CSVRecord record : records) {
            String quantity = record.get("QUANTITY");
            String price = record.get("PRICE");
            String symbol = record.get("SYMBOL");
            symbol = symbol.replaceAll("\\.", "").replaceAll(" " , "");

            String date  = record.get("DATE");
            date = date.substring(6)+"-" + date.substring(0, 2) +"-" + date.substring(3,5) + "-00:00:00." + count;

            String side  = record.get("DESCRIPTION");
            String amount  = record.get("AMOUNT");
            side =  (side.startsWith("Bought")  ? "B" :
                    (side.startsWith("Sold") ? "S" : "?"));

            if(side == "?") continue;
            count ++;
            List<DataModel> list = mapOfSymbolsToCSVs.get(symbol);
            if (list == null)
            {
                list = new ArrayList();
                mapOfSymbolsToCSVs.put(symbol, list);
            }
            list.add(DataModel.convertTo(record, count));

            List<DataModel> list2 = mapOfDateToCSVs.get(date);
            if (list2 == null)
            {
                list2 = new ArrayList();
                mapOfDateToCSVs.put(date, list2);
            }
            list2.add(DataModel.convertTo(record, count));


            logger.info("DATE=" + date +
                    " Symbol=" + symbol +
                    " Price=" + price +
                    " Qty=" + quantity +
                    " Side=" + side +
                    " Amount=" + amount
            );
        }

        logger.info("" + mapOfSymbolsToCSVs);
        logger.info("" + mapOfDateToCSVs);
        
        printOpenPositionsBySymbols(mapOfSymbolsToCSVs);


    }

    private static void printOpenPositionsBySymbols(Map<String, List<DataModel>> mapOfSymbolsToCSVs) throws IOException {
        Map<String, Double> mapOfSymbolsToPnLs = new TreeMap<>();
        Map<Double, List<String>> mapPnLToSymbols = new TreeMap<>();
        Map<String, Double> mapOfSymbolsToStrikePriceBuy = new TreeMap<>();
        Map<String, Double> mapOfSymbolsToStrikePriceSell = new TreeMap<>();

        Map<String, Boolean> mapOfSymbolsToInRamge = new TreeMap<>();
        Map<String, Boolean> mapOfSymbolsToLower = new TreeMap<>();

        Map<String, Double> mapOfSymbolsToMarketStrike = new TreeMap<>();
        Map<String, Double> mapOfSymbolsToMarketStrike2 = new TreeMap<>();

        Map<String, Double> mapOfSymbolsToLastBuyPrice = new TreeMap<>();


        Map<String, Double> mapOfSymbolsToBuyPrice = new TreeMap<>();

        Map<String, Double> mapOfSymbolsToMarketStrike3NoTolerance = new TreeMap<>();
        Map<String, Double> mapOfSymbolsToMarketStrike3NoTolerance2 = new TreeMap<>();
        Map<String, Double> mapOfSymbolsToMarketPrice = new TreeMap<>();
        Set<String> symbolsWithPos = new TreeSet<>();



        logger.info("=====================SYMBOL BY SYMBOL =============================");

        Double cumPnL = 0.0d;
        for (Map.Entry<String, List<DataModel>> entry: mapOfSymbolsToCSVs.entrySet() ) {


            int  cumBuysQty = 0;
            int  cumSellsQty = 0;
            String symbol = entry.getKey();

           // symbol = symbol.replaceAll("\\.", "").replaceAll(" " , "");

            System.out.println(symbol);

          //  if ( symbol != "BRKB") continue;
            Stock stock = YahooFinance.get(symbol);

            Double  price = stock.getQuote().getPrice().doubleValue();
            mapOfSymbolsToMarketPrice.put(symbol, price);
            System.out.println(symbol);

            List<DataModel> list = entry.getValue();

            List<DataModel> sequenceofBuysAndSells = new ArrayList<>();


            String prevSide = "x";
            DataModel dCum = null;
            for (DataModel record: list)
            {

                if (record.side == "S")
                {
                    cumSellsQty += record.quantity;
                    if (prevSide != record.side )
                    {
                         dCum = new DataModel();
                        sequenceofBuysAndSells.add(dCum);
                        dCum.copy(record);
                        prevSide = record.side;
                    }
                    else if (dCum != null)
                    {
                        dCum.amount +=  record.amount;
                        dCum.quantity += record.quantity;
                        dCum.price = dCum.amount/dCum.quantity;
                    }
                }
                if (record.side == "B")
                {
                    cumBuysQty += record.quantity;
                    if (prevSide != record.side )
                    {
                         dCum = new DataModel();
                        sequenceofBuysAndSells.add(dCum);
                        dCum.copy(record);
                        prevSide = record.side;
                    }
                    else if (dCum != null)
                    {
                        dCum.amount +=  record.amount;
                        dCum.quantity += record.quantity;
                        dCum.price = Math.abs(dCum.amount/dCum.quantity);
                        mapOfSymbolsToLastBuyPrice.put(symbol+dCum.date, dCum.price);
                    }

                    mapOfSymbolsToBuyPrice.put(symbol, dCum.price);
                }
            }
            logger.info(" " + sequenceofBuysAndSells);
            Double  pnl = 0.0d;
            Double strikePriceBuy = 0.0d;
            Double strikePriceSell = 0.0d;
            if (sequenceofBuysAndSells.size() > 1)
            {
                int size = sequenceofBuysAndSells.size();
                int count = 0;
                for (DataModel eachItem: sequenceofBuysAndSells) {
                    if (count %2 == 0)
                    {
                        strikePriceBuy = eachItem.price;
                    }
                    else
                    {
                        strikePriceSell = eachItem.price;
                    }

                    pnl += eachItem.amount;
                    count++;
                    if (size %2 == 0 && count == size) break;
                    if (size %2 == 1 &&  count == size)
                    {

                        pnl -=  eachItem.amount;
                         strikePriceBuy = 0.0d;
                         strikePriceSell = 0.0d;
                    }

                }
            }
           // if (pnl > -100 )
            {
                mapOfSymbolsToStrikePriceBuy.put(symbol, strikePriceBuy);
                mapOfSymbolsToStrikePriceSell.put(symbol, strikePriceSell);
                mapOfSymbolsToInRamge.put(symbol, price < strikePriceSell);
                mapOfSymbolsToLower.put(symbol, price < strikePriceBuy);


                if (strikePriceBuy > 0 )
                {

                    if (price <= (strikePriceBuy + strikePriceBuy*0.006))
                    {
                        mapOfSymbolsToMarketStrike.put(symbol, price);
                        mapOfSymbolsToMarketStrike2.put(symbol, strikePriceBuy);
                    }
                    if (price <= (strikePriceBuy ))
                    {
                        mapOfSymbolsToMarketStrike3NoTolerance.put(symbol, price);
                        mapOfSymbolsToMarketStrike3NoTolerance2.put(symbol, price);


                    }

                }

            }
            mapOfSymbolsToPnLs.put(symbol, pnl);
            List <String> lst = mapPnLToSymbols.get(pnl);
            if (lst == null)
            {
                lst = new ArrayList<>();
                mapPnLToSymbols.put(pnl, lst);
            }
            lst.add(symbol);
            cumPnL += pnl;

            if (cumBuysQty == cumSellsQty) {

                logger.info("Symbol=" + entry.getKey() + " is FLAT with PnL=" + pnl);
            }

            else if (cumBuysQty > cumSellsQty) {
                symbolsWithPos.add(symbol);
                logger.info("Symbol=" + entry.getKey() + " is BOUGHT with PnL"  + pnl);

            }
            else if (cumBuysQty < cumSellsQty) {
                symbolsWithPos.add(symbol);
                logger.info("Symbol=" + entry.getKey() + " is SOLD with PnL" + pnl);

            }

            logger.info("==================SYMBOL BY SYMBOL END ================================");


        }
        logger.info("" + mapOfSymbolsToPnLs);
        logger.info("Winners and losers " + mapPnLToSymbols);

        DateFormat df = new SimpleDateFormat("YYYY.MM.dd.HH.mm.ss");
        File fout = new File("PNLFile." + df.format(new Date() )+  "-"  + System.currentTimeMillis() + ".pnl.csv");
        logger.info("created file..  " + fout.getAbsolutePath());
        FileOutputStream fos = new FileOutputStream(fout);

        BufferedOutputStream baos = new BufferedOutputStream(fos);
        DataOutputStream dos = new DataOutputStream(baos);
        dos.write("Symbol,PnL\n".getBytes());
        for(Map.Entry<String, Double> entry: mapOfSymbolsToPnLs.entrySet())
        {
            dos.write((entry.getKey() +","+entry.getValue()+"\n" ).getBytes());
        }
        dos.write(("GRAND-TOTAL,"+ cumPnL+"\n").getBytes());
        dos.close();baos.close();fos.close();

        logger.info("==================================================");
        logger.info("Strike price  " + mapOfSymbolsToStrikePriceBuy);
         fout = new File("StrikePriceFile." + df.format(new Date() )+  "-"  + System.currentTimeMillis() + ".pnl.csv");
        logger.info("created file..   " + fout.getAbsolutePath());
         fos = new FileOutputStream(fout);

         baos = new BufferedOutputStream(fos);
         dos = new DataOutputStream(baos);
        dos.write("Symbol,CurrentPrice,LastBuyPrice,CurrentExposure,PnL,NextStrikePriceMin,NextStrikePriceMax,InRange,LowerThanBuyPrice,StrikePriceMarketPrice,StrikePriceWithNoTolerance,MyPrevPrice\n".getBytes());
        for(Map.Entry<String, Double> entry: mapOfSymbolsToStrikePriceBuy.entrySet())
        {
            Double currMktPrice = 0.0d, myPrice=0.0d;

            myPrice = findSymbolPriceWIthLatestDate(entry.getKey(), mapOfSymbolsToStrikePriceBuy);
            if (myPrice == null || myPrice <= 0.01)
            {
                myPrice = mapOfSymbolsToBuyPrice.get(entry.getKey());
            }

            String symbol = entry.getKey();
            currMktPrice=mapOfSymbolsToMarketPrice.get(entry.getKey());

            if (symbolsWithPos == null || (symbolsWithPos.contains(entry.getKey())) )
            {
                if (myPrice == null)
                {
                    System.out.println("Symbol" + symbol);
                }
                if (myPrice == null) myPrice = currMktPrice;
                double d = (currMktPrice-myPrice);
            }
            String lastBuyPrice = (symbolsWithPos.contains(entry.getKey()) ? ((double)(currMktPrice-myPrice)+"") :"FALSE");
            String pnl = "" +  mapOfSymbolsToPnLs.get(entry.getKey());

            dos.write(( symbol+","+//Symbol
                    (currMktPrice) + "," + //CurrentPrice
                    ( myPrice) + "," +//LastBuyPrice

                    lastBuyPrice + "," +
                    pnl + "," +
                    entry.getValue()+  "," +
                    mapOfSymbolsToStrikePriceSell.get(entry.getKey()) + "," +
                    mapOfSymbolsToInRamge.get(entry.getKey()) + "," +
                    mapOfSymbolsToLower.get(entry.getKey()) + "," +


                    mapOfSymbolsToMarketStrike2.get(entry.getKey()) +" ," +
                    mapOfSymbolsToMarketStrike3NoTolerance.get(entry.getKey()) + "," +
                    mapOfSymbolsToMarketStrike2.get(entry.getKey()) +
                    "\n" ).getBytes());



        }

        dos.close();baos.close();fos.close();

        logger.info("==================================================");
        logger.info("Market Strike Price is RIPE!!!   " + mapOfSymbolsToMarketStrike);
        logger.info("Market Strike Price is RIPE while I had traded on ..!!!   " + mapOfSymbolsToMarketStrike2);
        logger.info("==================================================");

        logger.info("Market Strike Price is SUPER RIPE!!!   " + mapOfSymbolsToMarketStrike3NoTolerance);
        logger.info("Market Strike Price is SUPER RIPE while I had traded on ..!!!   " + mapOfSymbolsToMarketStrike3NoTolerance2);

        logger.info("==================================================");

        logger.info("Cum PnL=" + cumPnL);


    }

    private static Double findSymbolPriceWIthLatestDate(String key, Map<String, Double> mapOfSymbolsToStrikePriceBuy) {
        Map<String, Double> dateSorted = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return 1-(o1.compareTo(o2));
            }
        });
        for(String symbol: mapOfSymbolsToStrikePriceBuy.keySet())
        {
            if (symbol.startsWith(key))
            {
                dateSorted.put(symbol, mapOfSymbolsToStrikePriceBuy.get(symbol));
            }
        }
        return dateSorted.entrySet().iterator().next().getValue();

    }
}


class DataModel
{
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataModel dataModel = (DataModel) o;
        return Double .compare(dataModel.price, price) == 0 &&
                Double .compare(dataModel.quantity, quantity) == 0 &&
                Double .compare(dataModel.amount, amount) == 0 &&
                Objects.equals(symbol, dataModel.symbol) &&
                Objects.equals(side, dataModel.side) &&
                Objects.equals(date, dataModel.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(price, quantity, amount, symbol, date, side);
    }

    @Override
    public String toString() {
        return "DataModel{" +
                "price=" + price +
                ", quantity=" + quantity +
                ", amount=" + amount +

                ", side='" + side + '\'' +
                ", date='" + date + '\'' +
                '}';
    }

    Double  price, quantity;
    int amount;
    String symbol, side;
    String date ;

    public static DataModel convertTo(CSVRecord record, int counter)
    {
        String quantity = record.get("QUANTITY");
        String price = record.get("PRICE");
        String symbol = record.get("SYMBOL");
        String date  = record.get("DATE");
        date = date.substring(6)+"-" + date.substring(0, 2) +"-" + date.substring(3,5) + "-00:00:00." + counter;

        String side  = record.get("DESCRIPTION");
        String amount  = record.get("AMOUNT");
        side =  (side.startsWith("Bought")  ? "B" :
                (side.startsWith("Sold") ? "S" : "?"));
        DataModel d = new DataModel();
        d.quantity = Double .parseDouble (quantity);
        d.price = Double .parseDouble (price);
        d.amount = (int)Double .parseDouble (amount);
        d.date = date;
        d.symbol = symbol;
        d.side = side;
        return d;
    }

    public void copy(DataModel record) {
        quantity = record.quantity;
        price = record.price;
        amount = record.amount;
        date = record.date;
        symbol = record.symbol;
        side = record.side;

    }
}