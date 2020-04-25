package org.ven.apps;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.crypto.Data;
import java.io.*;
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

    private static void printOpenPositionsBySymbols(Map<String, List<DataModel>> mapOfSymbolsToCSVs) {
        for (Map.Entry<String, List<DataModel>> entry: mapOfSymbolsToCSVs.entrySet() ) {

            int  cumBuysQty = 0;
            int  cumSellsQty = 0;

            List<DataModel> list = entry.getValue();
            for (DataModel record: list)
            {

            }


        }
    }
}


class DataModel
{
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataModel dataModel = (DataModel) o;
        return Double.compare(dataModel.price, price) == 0 &&
                Double.compare(dataModel.quantity, quantity) == 0 &&
                Double.compare(dataModel.amount, amount) == 0 &&
                Objects.equals(symbol, dataModel.symbol) &&
                Objects.equals(date, dataModel.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(price, quantity, amount, symbol, date);
    }

    @Override
    public String toString() {
        return "DataModel{" +
                "price=" + price +
                ", quantity=" + quantity +
                ", amount=" + amount +
                ", symbol='" + symbol + '\'' +
                ", date='" + date + '\'' +
                '}';
    }

    double price, quantity, amount;
    String symbol;
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
        d.quantity = Double.parseDouble(quantity);
        d.price = Double.parseDouble(price);
        d.amount = Double.parseDouble(amount);
        d.date = date;
        d.symbol = symbol;
        return d;
    }

}