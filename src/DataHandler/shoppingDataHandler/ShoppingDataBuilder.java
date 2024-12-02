package DataHandler.shoppingDataHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;


public class ShoppingDataBuilder {
    // mock 购物数据集

    public void mockShoppingEvent() {
        ArrayList<String> mallNames = getMallNames();
        ArrayList<HashMap<String, ArrayList<String>>> stores = getStoresAndItems();
        ArrayList<String> users = getAllUserIds();
        String startTime = "2019-03-01 00:00:00";
        String endTime = "2019-12-31 23:59:59";

        String shoppingEventFile = FilePathConstants.SHOPPIONG_EVENT_EXPERIMENT_FILE_PATH;
        BufferedWriter writer;

        long id = 1037074805834256618l;

        try {
            writer = new BufferedWriter(new FileWriter(shoppingEventFile));

            long shoppingEventId = 1037074805834256618l;
            // 现对每个用户mock一个购物事件
            for (int i = 0; i < 40; i++) {
                StringBuilder builder = new StringBuilder();
                builder.append(shoppingEventId);  // 事件 id
                builder.append(users.get((int) (Math.random() * users.size())) + "\t"); // 用户 id
                builder.append(mallNames.get((int) (Math.random() * mallNames.size())) + "\t"); // 商场名称

                int storesIdx = (int) (Math.random() * stores.size());
                HashMap<String, ArrayList<String>> map = stores.get(storesIdx);
                String storeId = (((map.keySet().toArray()))[0]).toString();
                ArrayList<String> items = map.get(storeId);

                builder.append(storeId + "\t");
                for (int j = 0; j < (int) (Math.random() * 10); j++) {
                    builder.append(items.get((int) (Math.random() * items.size())) + "\t");
                }

                builder.append(getRandomTime(startTime, endTime)); // 事件时间
                builder.append("\n");

                writer.write(builder.toString());

                shoppingEventId++;
            }

            writer.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }



    // ------------------------------------------- 用户的爱好数据 -------------------------------------------

    // 给hobby添加id
    public void hobbyHandler() {
        String hobbyData = FilePathConstants.HOBBY_DATA_UNUSED_FILE_PATH;
        String hobbyDataNew = FilePathConstants.HOBBY_DATA_FILE_PATH;
        BufferedReader bufferedReader;
        BufferedWriter bufferedWriter;

        try {
            bufferedReader = new BufferedReader(new FileReader(hobbyData));
            bufferedWriter = new BufferedWriter(new FileWriter(hobbyDataNew));
            String line;
            int i = 1;
            long hobbyId = 1037074805834256465l;

            while((line = bufferedReader.readLine()) != null) {
                String[] items = line.split("\\t");
                bufferedWriter.write(String.valueOf(hobbyId));
                bufferedWriter.write(items[1] + "\t");
                bufferedWriter.write(items[2] + "\t");
                bufferedWriter.write("\n");

                i++;
                hobbyId++;
            }

            bufferedReader.close();
            bufferedWriter.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // 得到所有的hobbyId
    public ArrayList<String> getHobbyIds() {
        String hobbyFilePath = FilePathConstants.HOBBY_DATA_FILE_PATH;
        BufferedReader bufferedReader;
        ArrayList<String> hobbys = new ArrayList<>();

        try {
            bufferedReader = new BufferedReader(new FileReader(hobbyFilePath));
            String line;

            while((line = bufferedReader.readLine()) != null) {
                String[] items = line.split("\\t");
                hobbys.add(items[0]);
            }

            System.out.println("hobbys.size(): " + hobbys.size());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return hobbys;
    }

    public HashMap<String,String> getHobbyId2NameMap() {
        String hobbyFilePath = FilePathConstants.HOBBY_DATA_FILE_PATH;
        BufferedReader bufferedReader;
        HashMap<String,String> hobbys = new HashMap<>();

        try {
            bufferedReader = new BufferedReader(new FileReader(hobbyFilePath));
            String line;

            while((line = bufferedReader.readLine()) != null) {
                String[] items = line.split("\\t");
                hobbys.put(items[0], items[2]);
            }

            System.out.println("hobbys.size(): " + hobbys.size());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return hobbys;
    }


    // ------------------------------------------- item store 以及 mall 数据文件 -------------------------------------------
    // 商品列表数据处理
    public void ItemDataHandle() {
        String itemData = FilePathConstants.ITEM_DATA_UNUSED_FILE_PATH;
        String itemDatanew = FilePathConstants.ITEM_DATA_UNUSED_FILE_PATH;
        BufferedReader bufferedReader;
        BufferedWriter bufferedWriter;

        try {
            bufferedReader = new BufferedReader(new FileReader(itemData));
            bufferedWriter = new BufferedWriter(new FileWriter(itemDatanew));
            String line;
            int i = 1;
            long id = 1037074805834256392l;

            while((line = bufferedReader.readLine()) != null) {
                String[] items = line.split("\\t");
                bufferedWriter.write(String.valueOf(id));
                bufferedWriter.write(items[1] + "\t");
                bufferedWriter.write(items[2]);
                bufferedWriter.write("\n");

                i++;
                id++;
            }

            bufferedReader.close();
            bufferedWriter.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // 商城列表数据处理
    public void mallDataHandle() {
        String mallData = FilePathConstants.MALL_DATA_UNUSED_FILE_PATH;
        String mallDatanew = FilePathConstants.MALL_DATA_FILE_PATH;
        BufferedReader bufferedReader;
        BufferedWriter bufferedWriter;

        try {
            bufferedReader = new BufferedReader(new FileReader(mallData));
            bufferedWriter = new BufferedWriter(new FileWriter(mallDatanew));
            String line;
            int i = 1;
            long id = 1037074805834256426l;

            while((line = bufferedReader.readLine()) != null) {
                String[] items = line.split("\\t");
                bufferedWriter.write(String.valueOf(id));
                bufferedWriter.write(items[4] + "\t");
                bufferedWriter.write(items[1] + "\t");
                bufferedWriter.write(items[2] + "\t");
                bufferedWriter.write(items[3] + "\t");
                bufferedWriter.write("\n");

                i++;
                id++;
            }

            bufferedReader.close();
            bufferedWriter.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // 给商店加个 id，将商品名改成商品 id
    public void storeDataHandle(){
        // 得到itemName到itemID的map映射
        HashMap<String, String> itemMap  = getItemName2IdMap();

        // 构建商店-商品数据
        BufferedReader reader;
        BufferedWriter writer;
        String storeData = FilePathConstants.STORE_DATA_UNUSED_FILE_PATH;
        String storeDataNew = FilePathConstants.STORE_DATA_FILE_PATH;

        try{
            reader = new BufferedReader(new FileReader(storeData));
            writer = new BufferedWriter(new FileWriter(storeDataNew));
            String line;
            int i = 1;
            long id = 1037074805834256447l;

            while ((line = reader.readLine()) != null) {
                String[] items = line.split("\\t");
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(id);
                stringBuilder.append(items[1] + "\t");

                // 对只售卖一种商品的商店条目分开处理
                for (int j = 2; j < items.length; j += 2) {
                    stringBuilder.append(itemMap.get(items[j+1]) + "\t");
                    stringBuilder.append(items[j+1]);

                    if (j != (items.length - 1))
                        stringBuilder.append("\t");
                }


                writer.write(stringBuilder.toString());
                writer.write("\n");

                i++;
                id++;
            }

            reader.close();
            writer.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // 得到 商品名-商品id 的hashmap
    public HashMap<String, String> getItemName2IdMap() {
        HashMap<String, String> map = new HashMap<>();
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(FilePathConstants.ITEM_DATA_FILE_PATH));
            String line;

            while((line = reader.readLine()) != null) {
                String[] items = line.split("\\t");

                map.put(items[1], items[0]);
            }

            reader.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return map;
    }

    public ArrayList<HashMap<String, ArrayList<String>>> getStoresAndItems() {
        String countryFilePath = FilePathConstants.STORE_DATA_FILE_PATH;
        BufferedReader bufferedReader;
        ArrayList<HashMap<String, ArrayList<String>>> stores = new ArrayList<>();

        try {
            bufferedReader = new BufferedReader(new FileReader(countryFilePath));
            String line;

            while((line = bufferedReader.readLine()) != null) {
                String[] items = line.split("\t");
                HashMap map = new HashMap();
                ArrayList<String> set = new ArrayList<>();

                for (int i = 3; i < items.length; i +=2 )
                    set.add(items[i]);

                map.put(items[1], set); // key 是store的名字，不是 id
                stores.add(map);
            }

            System.out.println("stores.size(): " + stores.size());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return stores;
    }

    public ArrayList<String> getMallIds() {
        String countryFilePath = FilePathConstants.MALL_DATA_FILE_PATH;
        BufferedReader bufferedReader;
        ArrayList<String> malls = new ArrayList<>();

        try {
            bufferedReader = new BufferedReader(new FileReader(countryFilePath));
            String line;

            while((line = bufferedReader.readLine()) != null) {
                String[] items = line.split("\t");
                malls.add(items[0]);
            }

            System.out.println("malls.size(): " + malls.size());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return malls;
    }

    public ArrayList<String> getMallNames() {
        String countryFilePath = FilePathConstants.MALL_DATA_FILE_PATH;
        BufferedReader bufferedReader;
        ArrayList<String> malls = new ArrayList<>();

        try {
            bufferedReader = new BufferedReader(new FileReader(countryFilePath));
            String line;

            while((line = bufferedReader.readLine()) != null) {
                String[] items = line.split("\t");
                malls.add(items[1]);
            }

            System.out.println("malls.size(): " + malls.size());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return malls;
    }

    // ------------------------------------------- country 和 city 数据文件 -------------------------------------------
    // cityData文件替换cityId
    public void cityHandler() {
        String countryData = FilePathConstants.CITY_DATA_UNUSED_FILE_PATH;
        String countryDataNew = FilePathConstants.CITY_DATA_FILE_PATH;
        BufferedReader bufferedReader;
        BufferedWriter bufferedWriter;

        HashMap<String, String> countryCode2IdMap = getCountryCode2IdMap();
        try {
            bufferedReader = new BufferedReader(new FileReader(countryData));
            bufferedWriter = new BufferedWriter(new FileWriter(countryDataNew));
            String line;
            int i = 1;
            long id = 1037074805834256384l;

            while((line = bufferedReader.readLine()) != null) {
                String[] items = line.split("\\t");
                bufferedWriter.write(String.valueOf(id));
                bufferedWriter.write("\t");
                bufferedWriter.write(items[1]);
                bufferedWriter.write("\t");
                bufferedWriter.write(items[2]);
                bufferedWriter.write("\t");
                bufferedWriter.write(countryCode2IdMap.get(items[2]));
                bufferedWriter.write("\n");

                i++;
                id++;
            }

            bufferedReader.close();
            bufferedWriter.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    // countryData文件替换countryId
    public void countryHandler() {
        String countryData = FilePathConstants.COUNTRY_DATA_UNUSED_FILE_PATH;
        String countryDataNew = FilePathConstants.COUNTRY_DATA_FILE_PATH;
        BufferedReader bufferedReader;
        BufferedWriter bufferedWriter;

        try {
            bufferedReader = new BufferedReader(new FileReader(countryData));
            bufferedWriter = new BufferedWriter(new FileWriter(countryDataNew));
            String line;
            int i = 1;
            long id = 1037087945401700352l;

            while((line = bufferedReader.readLine()) != null) {
                String[] items = line.split("\\t");
                bufferedWriter.write(String.valueOf(id));
                bufferedWriter.write("\t");
                bufferedWriter.write(items[1]);
                bufferedWriter.write("\n");

                i++;
                id++;
            }

            bufferedReader.close();
            bufferedWriter.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public ArrayList<String> getCountryIds() {
        String countryFilePath = FilePathConstants.COUNTRY_DATA_FILE_PATH;
        BufferedReader bufferedReader;
        ArrayList<String> countries = new ArrayList<>();

        try {
            bufferedReader = new BufferedReader(new FileReader(countryFilePath));
            String line;

            while((line = bufferedReader.readLine()) != null) {
                String[] items = line.split("\\t");
                countries.add(items[0]);
            }

            System.out.println("countries.size(): " + countries.size());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return countries;
    }

    // 得到 count id 到 country code 的映射
    public HashMap<String, String> getCountryCode2IdMap() {
        HashMap<String, String> map = new HashMap<>();
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(FilePathConstants.COUNTRY_DATA_FILE_PATH));
            String line;

            while((line = reader.readLine()) != null) {
                String[] items = line.split("\\t");

                map.put(items[1], items[0]);
            }

            reader.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return map;
    }

    public HashMap<String, String> getCountryId2CodeMap() {
        HashMap<String, String> map = new HashMap<>();
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(FilePathConstants.COUNTRY_DATA_FILE_PATH));
            String line;

            while((line = reader.readLine()) != null) {
                String[] items = line.split("\\t");

                map.put(items[0], items[1]);
            }

            reader.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return map;
    }



    // ------------------------------------------- 用户数据 -------------------------------------------
    // 生成用户数据

    public void generateUserProfile() {
        ArrayList<String> hobbys = getHobbyIds();
        HashMap<String,String> hobbyMap = getHobbyId2NameMap();
        ArrayList<String> countries = getCountryIds();
        HashMap<String,String> countryMap = getCountryId2CodeMap();

        String friendData = FilePathConstants.FRIEND_DATA_FILE_PATH;
        String userDataNew = FilePathConstants.USER_DATA_FILE_PATH;
        BufferedReader reader;
        BufferedWriter writer;

        try{
            reader = new BufferedReader(new FileReader(friendData));
            writer = new BufferedWriter(new FileWriter(userDataNew));
            String line;

            while ((line = reader.readLine()) != null) {
                StringBuilder builder = new StringBuilder();
                String hobby = hobbys.get((int) (Math.random() * hobbys.size()));
                String country = countries.get((int) (Math.random() * countries.size()));

                builder.append(line + "\t");
                builder.append(hobby + "\t" + hobbyMap.get(hobby) + "\t");
                builder.append(country + "\t" + countryMap.get(country));
                builder.append("\n");

                writer.write(builder.toString());
            }

            reader.close();
            writer.close();
        } catch (Exception e) {
            System.out.println(e.getMessage() + "is you?");
        }
    }


    // 得到所有的用户用于 mock 购物事件
    public ArrayList<String> getAllUserIds() {
        String userDataFile = FilePathConstants.USER_DATA_EXPERIMENT_PATH;
        BufferedReader bufferedReader;
        ArrayList<String> users = new ArrayList<>();

        try {
            bufferedReader = new BufferedReader(new FileReader(userDataFile));
            String line;

            while((line = bufferedReader.readLine()) != null) {
                String[] items = line.split("\t");
                users.add(items[0]);
            }

            System.out.println("stores.size(): " + users.size());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return users;
    }

    // ------------------------------------------- 其他 -------------------------------------------
    public String getRandomTime(String beginDate, String endDate) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date begin = format.parse(beginDate);
            Date end = format.parse(endDate);

            if (begin.getTime() >= end.getTime())
                return null;


            long temp = begin.getTime() + (long)(Math.random() * (end.getTime() - begin.getTime()));
            return format.format(new Date(temp));

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return null;
    }



    // 将地点所在的国家缩写换成对应的id
    public void POICountryHandler() {
        String POIFile = "/Users/wqian/kgstate/kgstate-test/datasets/CheckInEventDataset/POIData.txt";
        String POIFileCountry = "/Users/wqian/kgstate/kgstate-test/datasets/CheckInEventDataset/countryList.txt";

        BufferedReader reader;
        BufferedWriter writer;

        try{
            reader = new BufferedReader(new FileReader(POIFile));
            writer = new BufferedWriter(new FileWriter(POIFileCountry));

            HashSet<String> countries = new HashSet<>();
            String line;

            while((line = reader.readLine()) != null) {
                String[] items = line.split("\\t");
                countries.add(items[4]);
            }

            reader.close();

            for (String country : countries) {
                writer.write(country+"\n");
            }

            writer.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    // 国家代号换成国家id
    public void POIDataHandler() {
        HashMap<String, String> map = getCountryCode2IdMap();
        BufferedReader reader;
        BufferedWriter writer;

        try {
            reader = new BufferedReader(new FileReader("/Users/wqian/kgstate/kgstate-test/datasets/CheckInEventDataset/POIData.txt"));
            writer = new BufferedWriter(new FileWriter("/Users/wqian/kgstate/kgstate-test/datasets/CheckInEventDataset/POIData.txt"));
            String line;

            while((line = reader.readLine()) != null) {
                String[] items = line.split("\t");
                StringBuilder builder = new StringBuilder();

                for(int i =0 ;i < items.length -1; i++)
                    builder.append(items[i] + "\t");
                builder.append(map.get(items[4]));
                builder.append("\n");

                writer.write(builder.toString());
            }

            reader.close();
            writer.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
