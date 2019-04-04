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

    //copy 主要数据
    public CopyData copyData(ArrayList<Car> cars_in, ArrayList<Road> roads, ArrayList<Cross> crosses, ArrayList<Car> priCars,
                             HashMap<Integer, Road> roadHashMap, HashMap<Integer, Car> carHashMap,
                             HashMap<Integer, Cross> crossHashMap, int MTime, int carNumInRoad,
                             int carsNumLimit) {
        ArrayList<Car> cars_copy = new ArrayList<>();
        ArrayList<Road> roads_copy = new ArrayList<>();
        ArrayList<Cross> crosses_copy = new ArrayList<>();
        ArrayList<Car> priCars_copy = new ArrayList<>();
        HashMap<Integer, Road> roadHashMap_copy = new HashMap<>();
        HashMap<Integer, Car> carHashMap_copy = new HashMap<>();
        HashMap<Integer, Cross> crossHashMap_copy = new HashMap<>();

        for (int i = 0; i < cars_in.size(); i++) {
            Car car = cars_in.get(i).clone();
            cars_copy.add(car);
            carHashMap_copy.put(car.carID, car);
        }

        for (int i = 0; i < roads.size(); i++) {
            Road road = roads.get(i).clone();
            roads_copy.add(road);
            roadHashMap_copy.put(road.roadID, road);
        }
        for (int i = 0; i < crosses.size(); i++) {
            Cross cross = crosses.get(i).clone();
            crosses_copy.add(cross);
            crossHashMap_copy.put(cross.crossID, cross);
        }
        for (int i = 0; i < priCars.size(); i++) {
            priCars_copy.add(carHashMap_copy.get(priCars.get(i).carID));
        }
        for (int i = 0; i < cars_copy.size(); i++) {
            Car car = cars_copy.get(i);
            car.nextRoad = car.nextRoad == null ? null : roadHashMap_copy.get(car.nextRoad.roadID);
            car.nowRoad = car.nowRoad == null ? null : roadHashMap_copy.get(car.nowRoad.roadID);
            ArrayList<Integer> choiceList = new ArrayList<>();
            for (int j = 0; j < car.roadChoiceList.size(); j++) {
                choiceList.add(car.roadChoiceList.get(j));
            }
            car.roadChoiceList = choiceList;
            car.startCross = car.startCross == null ? null : crossHashMap_copy.get(car.startCrossID);
            car.endCross = car.endCross == null ? null : crossHashMap_copy.get(car.endCrossID);

        }
        for (int i = 0; i < roads_copy.size(); i++) {
            Road road = roads_copy.get(i);
            Car[][] carsCopy = new Car[road.channelCount][road.roadLength];
            Car[][] carsOnDulpexCopy = road.carOnDuplexRoad == null ? null : new Car[road.channelCount][road.roadLength];
            for (int j = 0; j < road.channelCount; j++) {
                for (int k = 0; k < road.roadLength; k++) {
                    if (road.carOnRoad[j][k] == null) {
                        carsCopy[j][k] = null;
                    } else {
                        carsCopy[j][k] = carHashMap_copy.get(road.carOnRoad[j][k].carID);
                    }
                    if (road.isDuplex) {
                        if (road.carOnDuplexRoad[j][k] == null) {
                            carsOnDulpexCopy[j][k] = null;
                        } else {
                            carsOnDulpexCopy[j][k] = carHashMap_copy.get(road.carOnDuplexRoad[j][k].carID);
                        }
                    }
                }
            }
            road.carOnRoad = carsCopy;
            road.carOnDuplexRoad = carsOnDulpexCopy;
            road.fromCross = road.fromCross == null ? null : crossHashMap_copy.get(road.fromCross.crossID);
            road.toCross = road.toCross == null ? null : crossHashMap_copy.get(road.toCross.crossID);
        }
        for (int i = 0; i < crosses_copy.size(); i++) {
            Cross cross = crosses_copy.get(i);
            Road[] roadsCopy = new Road[4];
            for (int j = 0; j < cross.crossRoads.length; j++) {
                roadsCopy[j] = cross.crossRoads[j] == null ? null : roadHashMap_copy.get(cross.crossRoads[j].roadID);
            }
            cross.crossRoads = roadsCopy;
            ArrayList<Road> sortRoadsCopy = new ArrayList<>();
            for (int j = 0; j < cross.sortedCrossRoad.size(); j++) {
                sortRoadsCopy.add(roadHashMap_copy.get(cross.sortedCrossRoad.get(j).roadID));
            }
            cross.sortedCrossRoad = sortRoadsCopy;
        }
        CopyData copyData = new CopyData();
        copyData.carHashMap_copy = carHashMap_copy;
        copyData.cars_copy = cars_copy;
        copyData.crosses_copy = crosses_copy;
        copyData.crossHashMap_copy = crossHashMap_copy;
        copyData.roads_copy = roads_copy;
        copyData.roadHashMap_copy = roadHashMap_copy;
        copyData.MTime = MTime;
        copyData.priCars_copy = priCars_copy;
        copyData.carNumInRoad = carNumInRoad;
        copyData.carsNumLimit = carsNumLimit;
        return copyData;
    }


    public void processData(ArrayList<int[]> carFileInfo, ArrayList<int[]> roadFileInfo, ArrayList<int[]> crossFileInfo,
                            ArrayList<Car> cars_in, ArrayList<Road> roads, ArrayList<Cross> crosses,
                            HashMap<Integer, Road> roadHashMap, HashMap<Integer, Car> carHashMap, HashMap<Integer, Cross> crossHashMap) {
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
        Collections.sort(crosses, new Comparator<Cross>() {
            @Override
            public int compare(Cross o1, Cross o2) {
                if (o1 != null && o2 != null) {
                    return o1.crossID - o2.crossID;
                }
                return 0;
            }
        });
        //处理car的道路矩阵处理
        Graph graphGlobal = new Graph();
        graphGlobal.graphGlobal = new float[2][crosses.size()][crosses.size()];
        graphGlobal.crossMapgraph = new HashMap<>();
        graphGlobal.graphMapCross = new HashMap<>();

        for (int k = 0; k < crosses.size(); k++) {
            graphGlobal.graphMapCross.put(crosses.get(k).crossID, k);
            graphGlobal.crossMapgraph.put(k, crosses.get(k).crossID);
            for (int j = 0; j < crosses.size(); j++) {
                if (k == j) {
                    graphGlobal.graphGlobal[0][k][j] = 0;
                } else {
                    graphGlobal.graphGlobal[0][k][j] = 0x3f3f3f;
                }
            }
        }

        for (int j = 0; j < roads.size(); j++) {
            Road road = roads.get(j);
            float len = roads.get(j).roadLength;
            //float speed = Math.min(cars_in.get(i).maxSpeedofCar, roads.get(j).speedLimitofRoad);
            graphGlobal.graphGlobal[0][graphGlobal.graphMapCross.get(roads.get(j).fromCross.crossID)]
                    [graphGlobal.graphMapCross.get(roads.get(j).toCross.crossID)] = len;
            graphGlobal.graphGlobal[1][graphGlobal.graphMapCross.get(roads.get(j).fromCross.crossID)]
                    [graphGlobal.graphMapCross.get(roads.get(j).toCross.crossID)] = road.speedLimitofRoad;
            if (roads.get(j).isDuplex) {
                graphGlobal.graphGlobal[0][graphGlobal.graphMapCross.get(roads.get(j).toCross.crossID)]
                        [graphGlobal.graphMapCross.get(roads.get(j).fromCross.crossID)] = len;
                graphGlobal.graphGlobal[1][graphGlobal.graphMapCross.get(roads.get(j).toCross.crossID)]
                        [graphGlobal.graphMapCross.get(roads.get(j).fromCross.crossID)] = road.speedLimitofRoad;

            }
        }

        for (int i = 0; i < cars_in.size(); i++) {
            cars_in.get(i).graph = graphGlobal;
            cars_in.get(i).graphMapCross = graphGlobal.graphMapCross;
            cars_in.get(i).crossMapgraph = graphGlobal.crossMapgraph;
        }

    }

    public void writeFile(ArrayList<Car> cars_out, String answerPath) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(answerPath));
            for (Car car : cars_out) {
                if (car.isPresetCar)
                    continue;
                String road = "";
                for (int i = 0; i < car.roadChoiceList.size(); i++) {
                    road = road + ", " + car.roadChoiceList.get(i);
                }
                String string = "(" + car.carID + ", " + car.realStartTime + road + ")";
                bw.write(string);
                bw.newLine();
                bw.flush();
            }
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean allCarFinished(ArrayList<Car> cars) {
        for (Car car : cars) {
            if (!car.isFinish) return false;
        }
        return true;
    }

    /**
     * 在用邻接矩阵adjMat表示的图中，求解从点s到点t的最短路径
     *
     * @param adjMat 邻接矩阵
     * @param s      起点
     * @param t      终点
     * @param
     * @return
     */
    public float dijReslove(float[][] adjMat, int s, int t) {

        int IMAX = 0x3f3f3f3f;
        //判断参数是否正确
        if (s < 0 || t < 0 || s >= adjMat.length || t >= adjMat.length) {
            System.out.println("错误，顶点不在图中!");
            return IMAX;
        }

        //用来记录某个顶点是否已经完成遍历，即替代优先队列的"移出队列"动作
        boolean[] isVisited = new boolean[adjMat.length];
        //用于存储从s到各个点之间的最短路径长度
        float[] d = new float[adjMat.length];

        //初始化数据
        for (int i = 0; i < adjMat.length; i++) {
            isVisited[i] = false;
            d[i] = IMAX;
        }
        d[s] = 0; //s到s的距离是0
        isVisited[s] = true; //将s标记为已访问过的

        //尚未遍历的顶点数目，替代优先队列是否为空的判断即Queue.isEmpty()
        int unVisitedNum = adjMat.length;
        //用于表示当前所保存的子路径中权重最小的顶点的索引,对应优先队列中,默认是起点
        int index = s;
        while (unVisitedNum > 0 && index != t) {
            float min = IMAX;
            for (int i = 0; i < adjMat.length; i++) { //取到第i行的最小元素的索引
                if (min > d[i] && !isVisited[i]) {
                    min = d[i];
                    index = i;
                }
            }
            if (index == t || unVisitedNum == 0) {
                //System.out.print(index); //打印最短路径
            } else {
                //System.out.print(index + "=>"); //打印最短路径
            }
            for (int i = 0; i < adjMat.length; i++) {
                if (d[index] + adjMat[index][i] < d[i]) {
                    d[i] = d[index] + adjMat[index][i];
                }
            }
            unVisitedNum--;
            isVisited[index] = true;
        }
        return d[t];
    }


    public void processPresetCar(HashMap<Integer, Car> carsHashMap, String presetAnswerPath) {
        //预置路径文件读取
        BufferedReader inpreset = null;
        try {
            inpreset = new BufferedReader(new FileReader(presetAnswerPath));
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
                carsHashMap.get(carId).minStartTime = carStartTime;
                carsHashMap.get(carId).realStartTime = carStartTime;
                for (int i = 2; i < strlist.length; i++) {
                    int roadId = Integer.parseInt(strlist[i]);
                    carsHashMap.get(carId).roadChoiceList.add(roadId);
                }
            }
            inpreset.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
