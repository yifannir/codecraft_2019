package com.huawei;

import java.io.*;
import java.util.*;

public class Func {


    public void readFile(String carPath, String roadPath, String crossPath, ArrayList<int[]> carFileInfo, ArrayList<int[]> roadFileInfo, ArrayList<int[]> crossFileInfo) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(carPath));
            String str;
            while ((str = in.readLine()) != null) {
                if (str.contains("#")) continue;
                str = str.replace("(", "");
                str = str.replace(")", "");
                str = str.replace(" ", "");
                str = str.trim();
                String[] strlist = str.split(",");
                int[] intlist = new int[7];
                for (int i = 0; i < strlist.length; i++) {
                    intlist[i] = Integer.parseInt(strlist[i]);
                }
                carFileInfo.add(intlist);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //道路文件读取

        BufferedReader inroad = null;
        try {
            inroad = new BufferedReader(new FileReader(roadPath));
            String str;
            while ((str = inroad.readLine()) != null) {
                if (str.contains("#")) continue;
                str = str.replace("(", "");
                str = str.replace(")", "");
                str = str.replace(" ", "");
                str = str.trim();
                String[] strlist = str.split(",");
                int[] intlist = new int[7];
                for (int i = 0; i < strlist.length; i++) {
                    intlist[i] = Integer.parseInt(strlist[i]);
                }
                roadFileInfo.add(intlist);
            }
            inroad.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //路口文件读取

        BufferedReader incross = null;
        try {
            incross = new BufferedReader(new FileReader(crossPath));
            String str;
            while ((str = incross.readLine()) != null) {
                if (str.contains("#")) continue;
                str = str.replace("(", "");
                str = str.replace(")", "");
                str = str.replace(" ", "");
                str = str.trim();
                String[] strlist = str.split(",");
                int[] intlist = new int[5];
                for (int i = 0; i < strlist.length; i++) {
                    intlist[i] = Integer.parseInt(strlist[i]);
                }
                crossFileInfo.add(intlist);
            }
            incross.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void processData(ArrayList<int[]> carFileInfo, ArrayList<int[]> roadFileInfo, ArrayList<int[]> crossFileInfo,
                            ArrayList<Car> cars_in, ArrayList<Road> roads, ArrayList<Cross> crosses,
                            HashMap<Integer, Road> roadHashMap, HashMap<Integer, Car> carHashMap, HashMap<Integer, Cross> crossHashMap) {
        //(id,from,to,speed,planTime, isPriority, isPreset)
        for (int i = 0; i < carFileInfo.size(); i++) {
            cars_in.add(new Car(carFileInfo.get(i)[0], carFileInfo.get(i)[1], carFileInfo.get(i)[2],
                    carFileInfo.get(i)[3], carFileInfo.get(i)[4], carFileInfo.get(i)[5], carFileInfo.get(i)[6]));
        }
        //#(id,length,speed,channel,from,to,isDuplex)
        for (int i = 0; i < roadFileInfo.size(); i++) {
            roads.add(new Road(roadFileInfo.get(i)[0]));
        }
        for (int i = 0; i < cars_in.size(); i++) {
            carHashMap.put(cars_in.get(i).carID, cars_in.get(i));
        }
        for (int i = 0; i < roads.size(); i++) {
            roadHashMap.put(roads.get(i).roadID, roads.get(i));
        }
        //处理-1的道路
        //roadHashMap.put(-1,new Road(-1,0x3f3f3f,0,0,-1,-1,0));
        //#(id,roadId,roadId,roadId,roadId)
        for (int i = 0; i < crossFileInfo.size(); i++) {
            int crossID = crossFileInfo.get(i)[0];
            crosses.add(new Cross(crossID, roadHashMap.get(crossFileInfo.get(i)[1]), roadHashMap.get(crossFileInfo.get(i)[2]),
                    roadHashMap.get(crossFileInfo.get(i)[3]), roadHashMap.get(crossFileInfo.get(i)[4])));
        }

        for (int i = 0; i < crosses.size(); i++) {
            crossHashMap.put(crosses.get(i).crossID, crosses.get(i));
        }
        //处理road 的初始
        for (int i = 0; i < roadFileInfo.size(); i++) {
            roadHashMap.get(roadFileInfo.get(i)[0]).setRoad(roadFileInfo.get(i)[1], roadFileInfo.get(i)[2],
                    roadFileInfo.get(i)[3], crossHashMap.get(roadFileInfo.get(i)[4]), crossHashMap.get(roadFileInfo.get(i)[5]), roadFileInfo.get(i)[6]);
        }


        Collections.sort(cars_in, new Comparator<Car>() {
            @Override
            public int compare(Car o1, Car o2) {
                if (o1 != null && o2 != null) {
                    return o1.carID - o2.carID;
                }
                return 0;
            }
        });
        Collections.sort(roads, new Comparator<Road>() {
            @Override
            public int compare(Road o1, Road o2) {
                if (o1 != null && o2 != null) {
                    return o1.roadID - o2.roadID;
                }
                return 0;
            }
        });
        Collections.sort(crosses, new Comparator<Cross>() {
            @Override
            public int compare(Cross o1, Cross o2) {
                if (o1 != null && o2 != null) {
                    return o1.crossID - o2.crossID;
                }
                return 0;
            }
        });

        for (int i = 0; i < cars_in.size(); i++) {
            cars_in.get(i).startCross = crossHashMap.get(cars_in.get(i).startCrossID);
            cars_in.get(i).endCross = crossHashMap.get(cars_in.get(i).endCrossID);
        }
    }



    public boolean allCarFinished(ArrayList<Car> cars) {
        for (Car car:cars){
            if (!car.isFinish) return false;
        }
        return true;
    }


    public void processAnswerCar(HashMap<Integer, Car> carHashMap, String answerPath) {
        //预置路径文件读取
        BufferedReader inpreset = null;
        try {
            inpreset = new BufferedReader(new FileReader(answerPath));
            String str;
            while ((str = inpreset.readLine()) != null) {
                if (str.contains("#")) continue;
                str = str.replace("(", "");
                str = str.replace(")", "");
                str = str.replace(" ", "");
                str = str.trim();
                String[] strlist = str.split(",");
                int carId = Integer.parseInt(strlist[0]);
                int carStartTime = Integer.parseInt(strlist[1]);
                carHashMap.get(carId).startTime = carStartTime;
                for (int i = 2; i < strlist.length; i++) {
                    int roadId = Integer.parseInt(strlist[i]);
                    carHashMap.get(carId).roadChoiceList.add(roadId);
                }
            }
            inpreset.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
