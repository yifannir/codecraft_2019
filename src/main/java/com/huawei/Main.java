package com.huawei;

import org.apache.log4j.Logger;
//import sun.awt.geom.AreaOp;

import java.util.*;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length != 4) {
            logger.error("please input args: inputFilePath, resultFilePath");
            return;
        }
        logger.info("Start...");

        String carPath = args[0];
        String roadPath = args[1];
        String crossPath = args[2];
        String presetAnswerPath = args[3];
        String answerPath = args[4];

        logger.info("carPath = " + carPath + " roadPath = " + roadPath + " crossPath = " + crossPath + " presetAnswerPath = " + presetAnswerPath + " and answerPath = " + answerPath);

        logger.info("start read input files");

        /**
         * 读取文件
         */
        ArrayList<Car> cars = new ArrayList<>(); //

        ArrayList<Road> roads = new ArrayList<>();
        ArrayList<Cross> crosses = new ArrayList<>();
        HashMap<Integer, Road> roadHashMap = new HashMap<>();
        HashMap<Integer, Car> carHashMap = new HashMap<>();
        HashMap<Integer, Cross> crossHashMap = new HashMap<>();

        // car的信息读到carFileInfo里面，每一行5个数字，分别为(id,from,to,speed,planTime,是否优先，是否预置)
        ArrayList<int[]> carFileInfo = new ArrayList<>();

        // road的信息读到roadFileInfo里面，每一行7个数字，分别为#(id,length,speed,channel,from,to,isDuplex)
        ArrayList<int[]> roadFileInfo = new ArrayList<>();

        // cross的信息读到crossFileInfo里面，每一行5个数字，分别为#(id,roadId,roadId,roadId,roadId)
        ArrayList<int[]> crossFileInfo = new ArrayList<>();

        Func func = new Func();
        func.readFile(carPath, roadPath, crossPath, carFileInfo, roadFileInfo, crossFileInfo);

        //处理输入数据到类
        func.processData(carFileInfo, roadFileInfo, crossFileInfo, cars, roads, crosses, roadHashMap, carHashMap, crossHashMap);

        //处理预置车辆的choicelist。
        func.processPresetCar(carHashMap,presetAnswerPath);



//        Random random = new Random(2000);
//        for (int i = 0; i < cars.size(); i++) {
//            cars.get(i).minStartTime += random.nextInt(100);
//        }
        /**
         * 开始上路调度
         */
        DispatchCenter dispatchCenter = new DispatchCenter(func, cars, roads, crosses, roadHashMap, carHashMap, crossHashMap);
        CopyData copyData = null;
        while (!func.allCarFinished(dispatchCenter.cars)) {
            if (dispatchCenter.MTime % 5 == 0)
                copyData = dispatchCenter.CopyDispatchTimePieceData();
            while (true) {
                if (dispatchCenter.DispatchOneTimePiece()) {
                    break;
                } else {
                    dispatchCenter.dispatchGoBack(copyData);
                }
            }
            int num = 0;
            for (int i = 0; i < dispatchCenter.cars.size(); i++) {
                if (dispatchCenter.cars.get(i).isFinish)
                    num++;
            }
            System.out.println("finish cars num:" + num);
        }

        int totleTime = 0;
        for (Car car : dispatchCenter.cars) {
            totleTime += car.maxTimeCount - car.minTimeCount;
        }
        System.out.println("totle Time:" + totleTime);
        func.writeFile(dispatchCenter.cars, answerPath);
    }


}

class CopyData {
    ArrayList<Car> cars_copy;
    ArrayList<Road> roads_copy;
    ArrayList<Cross> crosses_copy;
    HashMap<Integer, Road> roadHashMap_copy;
    HashMap<Integer, Car> carHashMap_copy;
    HashMap<Integer, Cross> crossHashMap_copy;
    int carNumInRoad;
    int MTime;
    int carsNumLimit;
}

class Pair<K, V> {
    private K k;
    private V v;

    public Pair(K k, V v) {
        this.k = k;
        this.v = v;
    }

    public K getK() {
        return k;
    }

    public void setK(K k) {
        this.k = k;
    }

    public V getV() {
        return v;
    }

    public void setV(V v) {
        this.v = v;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof Pair))
            return false;
        Pair o = (Pair) obj;
        if ((k == null && ((Pair) obj).k != null) || (k != null && ((Pair) obj).k == null))
            return false;
        if ((v == null && ((Pair) obj).v != null) || (v != null && ((Pair) obj).v == null))
            return false;
        if (v != null && k != null)
            return (this.k.equals(o.k)) && (this.v.equals(o.v));
        else if (v == null && k != null)
            return this.k.equals(o.k);
        else
            return this.v.equals(o.v);
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (k != null)
            result = result * 31 + k.hashCode();
        if (v != null)
            result = result * 31 + v.hashCode();
        return result;
    }
}


class Car implements Cloneable, Comparable<Car> {
    public static final int GO_WRONG = -1;
    public static final int GO_DOWN = 0;
    public static final int GO_LEFT = 1;
    public static final int GO_RIGHT = 2;
    public static final int GO_NEVER = 3;
    public static final int END_STATUS = 0;
    public static final int WAIT_STATUS = 1;
    public static final int FORWORD_HAS_NO_CAR = 0;
    public static final int FORWORD_HAS_END_CAR = 1;
    public static final int FORWORD_HAS_WAIT_CAR = 2;
    public static final int FORWORD_NEED_THROUGH_CROSS = 3;
    int fixNextRoad;
    boolean isPriorityCar;
    boolean isPresetCar;
    int startCrossID;
    int endCrossID;
    int carID;
    Cross startCross;
    Cross endCross;
    int maxSpeedofCar;
    int minStartTime;
    int minTimeCount;
    int maxTimeCount;

    public Car clone() {
        try {
            return (Car) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    int realStartTime;
    boolean isFinishedAtTimePiece;//在这个时间片内该车是否完成调度
    //int eval;//状态评估值
    Graph graph;// 该车辆的视野地图
    HashMap<Integer, Integer> graphMapCross; //实际CrossID映射路网0123456...的ID
    HashMap<Integer, Integer> crossMapgraph; //0123...映射路网crossID

    //车辆所在的道路
    Road nowRoad;

    // 车辆在该道路上的车道ID
    int nowRoadChannel;

    // 车辆在该道路车道上的下标,下标就是距离路口的距离
    int roadOffset;

    // 当前车辆选择的下条路径
    Road nextRoad;

    //该车辆是在正向路（从该条道路起点路口到终点路口）还是反向路（从该条道路终点路口到起点路口）
    boolean isForwardRoad;

    // 该车辆是否是卡死的车辆
    boolean kasi;

    //车辆选择路径的ID list
    ArrayList<Integer> roadChoiceList = new ArrayList<>();

    /*
     * 获取该车辆所在的车道数组，请务必保证nowroad不为null，否则会返回null
     * */
    Car[][] getCarArray() {
        if (nowRoad == null)
            return null;
        if (isForwardRoad)
            return nowRoad.carOnRoad;
        else
            return nowRoad.carOnDuplexRoad;
    }


    boolean isFinish;//车辆是否到达终点，true为到达状态。
    int isStart;//车辆是否出发，0为未出发状态，1为标记出发，2已出发
    int flag; //车辆是等待状态还是终止状态,0为终止状态，1为等待状态。

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof Car))
            return false;
        return carID == ((Car) obj).carID;
    }

    @Override
    public int hashCode() {
        return carID;
    }

    /**
     * 构造初始话
     *
     * @return
     */
    public Car(int carID, int fromCrossID, int toCrossID, int maxSpeedofCar, int planTime,int pri,int preset) {
        this.carID = carID;
        this.startCrossID = fromCrossID;
        this.endCrossID = toCrossID;
        this.maxSpeedofCar = maxSpeedofCar;
        this.minStartTime = planTime;
        this.minTimeCount = planTime;
        this.isPriorityCar = pri==1;
        this.isPresetCar = preset==1;
        this.kasi = false;
    }

    int getRealSpeedOnNowRoad() {
        if (nowRoad == null)
            return GO_WRONG;
        return maxSpeedofCar < nowRoad.speedLimitofRoad ? maxSpeedofCar : nowRoad.speedLimitofRoad;
    }

    int getRealSpeedThroughCross() {//当需要经过路口时的理论最大速度，这时需要设定nextRoad
        //该条道路上该车实际速度
        int nowRoadRealSpeed = maxSpeedofCar < nowRoad.speedLimitofRoad ? maxSpeedofCar : nowRoad.speedLimitofRoad;
        if (nextRoad == null || roadOffset >= nowRoadRealSpeed)
            return GO_WRONG;
        int nextRoadRealSpeed = maxSpeedofCar < nextRoad.speedLimitofRoad ? maxSpeedofCar : nextRoad.speedLimitofRoad;
        if (roadOffset >= nextRoadRealSpeed)//当前车道行使的长度比下一车道的限速要大
            return roadOffset;//最大行使速度为当前与路口的距离，也就是不能到达下一车道
        else//当前车道行使的长度小于下一车道的限速，所以这辆车可以行使下一车道限速的长度
            return nextRoadRealSpeed;
    }

    int getDirection() {
        Cross cross;
        if (isForwardRoad)
            cross = nowRoad.toCross;
        else
            cross = nowRoad.fromCross;
        if (getTypeOfForwordCar().forwardStatus != Car.FORWORD_NEED_THROUGH_CROSS) {
            return GO_NEVER;
        }
        if (cross.crossID == endCrossID) {
            return GO_DOWN;
        }
        int nowRoadOrder, nextRoadOrder;
        for (nowRoadOrder = 0; nowRoadOrder < 4; nowRoadOrder++) {
            if (cross.crossRoads[nowRoadOrder] == nowRoad)
                break;
        }
        for (nextRoadOrder = 0; nextRoadOrder < 4; nextRoadOrder++) {
            if (cross.crossRoads[nextRoadOrder] == nextRoad)
                break;
        }
        if ((nowRoadOrder - nextRoadOrder) % 4 == 2 || (nowRoadOrder - nextRoadOrder) % 4 == -2)//顺时针顺序排序下标，差为2代表对面
            return GO_DOWN;
        else if ((nowRoadOrder - nextRoadOrder) % 4 == 3 || (nowRoadOrder - nextRoadOrder) % 4 == -1)//差为-1代表左转
            return GO_LEFT;
        else if ((nowRoadOrder - nextRoadOrder) % 4 == 1 || (nowRoadOrder - nextRoadOrder) % 4 == -3)//差为1代表右转
            return GO_RIGHT;
        return GO_WRONG;
    }

    boolean getDirectionResult(HashMap<Pair<Integer, Integer>, Pair<Boolean, PriorityQueue<Car>>> crossRoadHash) {
        Cross cross = isForwardRoad?nowRoad.toCross:nowRoad.fromCross;
        int direction = getDirection();
        if (direction == Car.GO_DOWN) {//左转
            if(isPriorityCar)
                return true;
            else{
                
            }
        }
    }

    /**
     * 道路最高优先级的拥堵车辆才可以选择下一个道路。所以分配时候每走一辆拥堵车辆都要更新当前道路车辆flag，并进行重新
     *
     * @param func
     */
    Random random = new Random(4050);

    public void getNxtChoice(Func func) {
        // 判断是否轮得到这辆车选择下一个路口，如果轮到了，进行选择，没有轮到，return；
        Cross cross;
        if (isForwardRoad)
            cross = nowRoad.toCross;
        else
            cross = nowRoad.fromCross;


        if (nextRoad == null && getTypeOfForwordCar().forwardStatus == Car.FORWORD_NEED_THROUGH_CROSS) {
            //随机选路法（仅限于卡死情况）
            if (kasi || random.nextInt(100) % 35 == 0) {
                //随机选择这个cross的下个路，然后break出当前路。双向车道都要选择。
                //kasi =  random.nextInt(100) % 5 == 0?true:false;
                kasi = false;
                int tmp = random.nextInt(4);
                nextRoad = cross.sortedCrossRoad.get(tmp % cross.sortedCrossRoad.size());
                // 不能掉头，不能选择单车道的逆行。
                while (nextRoad.roadID == nowRoad.roadID || (!nextRoad.isDuplex && (nextRoad.fromCross.crossID != cross.crossID))) {
                    tmp = random.nextInt(4);
                    nextRoad = cross.sortedCrossRoad.get(tmp % cross.sortedCrossRoad.size());
                    //kasi = false;
                }
                return;
            }
            //最短路选择法
            float score = 0x3f3f3f3f;//= func.dijReslove(car.graph,graphMapCross.get(cross.crossID),graphMapCross.get(car.endCrossID),pathList);
            for (int i = 0; i < 4; i++) {
                if (cross.crossRoads[i] != null && cross.crossRoads[i].roadID != nowRoad.roadID) { //道路不为空
                    // 双车道时，起点或者终点为所选的第二个节点即可，单车道道时必须时终点为第二个节点。
                    if (cross.crossRoads[i].isDuplex) {
                        if (cross.crossRoads[i].toCross.crossID == cross.crossID) {

                            float tmp = func.dijReslove(graph.graphGlobal[0], graphMapCross.get(cross.crossRoads[i].fromCross.crossID), graphMapCross.get(endCrossID));
                            float len = cross.crossRoads[i].roadLength;
                            float speed = Math.min(maxSpeedofCar, cross.crossRoads[i].speedLimitofRoad);
                            tmp += len / speed;
                            //TODO：调参位置 加上拥堵惩罚
                            //tmp = tmp * (1 + (3- ((float)realStartTime)/300)*cross.crossRoads[i].getCarNumRate(cross));
                            tmp = tmp * (1 + cross.crossRoads[i].getCarNumRate(cross));
                            //TODO:调参位置 优先选下条小车道为END的或者没车的路
                            int stepCounts = Math.min(maxSpeedofCar, cross.crossRoads[i].speedLimitofRoad) - roadOffset;
                            stepCounts = stepCounts > 0 ? stepCounts : 0;
                            Car[][] cars = cross.crossRoads[i].getCarArray(cross);
                            //for (int j = 0; j < cross.crossRoads[i].channelCount; j++) {
                            for (int k = 0; k < stepCounts; k++) {
                                if (cars == null) break;
                                if (cars[0][k] != null && cars[0][k].flag == Car.WAIT_STATUS) {
                                    tmp = tmp + 0;
                                    break;
                                }
                            }
                            //}
                            if (tmp < score) {
                                nextRoad = cross.crossRoads[i];
                                score = tmp;
                            }
                        } else if (cross.crossRoads[i].fromCross.crossID == cross.crossID) {
                            float tmp = func.dijReslove(graph.graphGlobal[0], graphMapCross.get(cross.crossRoads[i].toCross.crossID), graphMapCross.get(endCrossID));
                            float len = cross.crossRoads[i].roadLength;
                            float speed = Math.min(maxSpeedofCar, cross.crossRoads[i].speedLimitofRoad);
                            tmp += len / speed;
                            //TODO：调参位置 加上拥堵惩罚
                            //tmp = tmp * (1 + (3- ((float)realStartTime)/300)*cross.crossRoads[i].getCarNumRate(cross));
                            tmp = tmp * (1 + cross.crossRoads[i].getCarNumRate(cross));
                            //TODO:调参位置 优先选下条小车道为END的或者没车的路
                            int stepCounts = Math.min(maxSpeedofCar, cross.crossRoads[i].speedLimitofRoad) - roadOffset;
                            stepCounts = stepCounts > 0 ? stepCounts : 0;
                            Car[][] cars = cross.crossRoads[i].getCarArray(cross);
                            //for (int j = 0; j < cross.crossRoads[i].channelCount; j++) {
                            for (int k = 0; k < stepCounts; k++) {
                                if (cars == null) break;
                                if (cars[0][k] != null && cars[0][k].flag == Car.WAIT_STATUS) {
                                    tmp = tmp + 0;
                                    break;
                                }
                            }
                            //}
                            if (tmp < score) {
                                nextRoad = cross.crossRoads[i];
                                score = tmp;
                            }
                        }
                    } else if (!cross.crossRoads[i].isDuplex) {
                        if (cross.crossRoads[i].fromCross.crossID == cross.crossID) {
                            float tmp = func.dijReslove(graph.graphGlobal[0], graphMapCross.get(cross.crossRoads[i].toCross.crossID), graphMapCross.get(endCrossID));
                            float len = cross.crossRoads[i].roadLength;
                            float speed = Math.min(maxSpeedofCar, cross.crossRoads[i].speedLimitofRoad);
                            tmp += len / speed;
                            //TODO：调参位置 加上拥堵惩罚
                            //tmp = tmp * (1 + (3- ((float)realStartTime)/300)*cross.crossRoads[i].getCarNumRate(cross));
                            tmp = tmp * (1 + cross.crossRoads[i].getCarNumRate(cross));
                            //TODO:调参位置 优先选下条小车道为END的或者没车的路
                            int stepCounts = Math.min(maxSpeedofCar, cross.crossRoads[i].speedLimitofRoad) - roadOffset;
                            stepCounts = stepCounts > 0 ? stepCounts : 0;
                            Car[][] cars = cross.crossRoads[i].getCarArray(cross);
                            //for (int j = 0; j < cross.crossRoads[i].channelCount; j++) {
                            for (int k = 0; k < stepCounts; k++) {
                                if (cars == null) break;
                                if (cars[0][k] != null && cars[0][k].flag == Car.WAIT_STATUS) {
                                    tmp = tmp + 0;
                                    break;
                                }
                            }
                            //}
                            if (tmp < score) {
                                nextRoad = cross.crossRoads[i];
                                score = tmp;
                            }
                        }
                    }
                }
            }
        }
    }


    //首次出发得到第一条路
    public void getFistChoice(Func func) {
        float score = 0x3f3f3f3f;
        for (int i = 0; i < 4; i++) {
            if (startCross.crossRoads[i] != null) { //道路不为空
                // 双车道时，起点或者终点为所选的第二个节点即可，单车道道时必须时终点为第二个节点。
                if (startCross.crossRoads[i].isDuplex) {
                    if (startCross.crossID == startCross.crossRoads[i].toCross.crossID) {
                        float tmp = func.dijReslove(graph.graphGlobal[0], graphMapCross.get(startCross.crossRoads[i].fromCross.crossID), graphMapCross.get(endCrossID));
                        float len = startCross.crossRoads[i].roadLength;
                        float speed = Math.min(maxSpeedofCar, startCross.crossRoads[i].speedLimitofRoad);
                        tmp += len / speed;

                        if (tmp < score) {
                            nowRoad = startCross.crossRoads[i];
                            nextRoad = null;//ccg开始上路，nextroad为null
                            isForwardRoad = false;
                            score = tmp;
                        }
                    } else if (startCross.crossID == startCross.crossRoads[i].fromCross.crossID) {
                        float tmp = func.dijReslove(graph.graphGlobal[0], graphMapCross.get(startCross.crossRoads[i].toCross.crossID), graphMapCross.get(endCrossID));
                        float len = startCross.crossRoads[i].roadLength;
                        float speed = Math.min(maxSpeedofCar, startCross.crossRoads[i].speedLimitofRoad);
                        tmp += len / speed;

                        if (tmp < score) {
                            nowRoad = startCross.crossRoads[i];
                            nextRoad = null;//ccg开始上路，nextroad为null
                            isForwardRoad = true;
                            score = tmp;
                        }
                    }
                } else if (!startCross.crossRoads[i].isDuplex) {
                    if (startCross.crossID == startCross.crossRoads[i].fromCross.crossID) {
                        float tmp = func.dijReslove(graph.graphGlobal[0], graphMapCross.get(startCross.crossRoads[i].toCross.crossID), graphMapCross.get(endCrossID));
                        float len = startCross.crossRoads[i].roadLength;
                        float speed = Math.min(maxSpeedofCar, startCross.crossRoads[i].speedLimitofRoad);
                        tmp += len / speed;

                        if (tmp < score) {
                            nowRoad = startCross.crossRoads[i];
                            nextRoad = null;//ccg开始上路，nextroad为null。nextRoad = startCross.crossRoads[i];改
                            isForwardRoad = true;
                            score = tmp;
                        }
                    }
                }
            }
        }
        roadOffset = nowRoad.roadLength;
    }

    /*
   获取车辆前方道路信息，ForwordInfo.toPosition该车能到达的位置，如果有车阻挡，只能到达前车的尾后。
   如果当前路段行使最大距离大于偏移量，且前方没有任何车辆阻挡，返回FORWORD_NEED_THROUGH_CROSS
   */
    public ForwordInfo getTypeOfForwordCar() {
        Car[][] roadCars;
        if (isForwardRoad)
            roadCars = nowRoad.carOnRoad;
        else
            roadCars = nowRoad.carOnDuplexRoad;
        int channelNum = nowRoadChannel;
        int nowPosition = roadOffset;
        int exceptPosition = (roadOffset - getRealSpeedOnNowRoad()) >= 0 ? (roadOffset - getRealSpeedOnNowRoad()) : 0;
        for (; nowPosition > exceptPosition; nowPosition--) {
            //(car.roadOffset-car.getRealSpeedOnNowRoad())为车辆预计到达的偏移量，如果该偏移量小于0，则最后到达位置取0。
            if (roadCars[channelNum][nowPosition - 1] != null) {
                if (roadCars[channelNum][nowPosition - 1].flag == Car.END_STATUS)
                    return new ForwordInfo(FORWORD_HAS_END_CAR, nowPosition);
                else if (roadCars[channelNum][nowPosition - 1].flag == Car.WAIT_STATUS)
                    return new ForwordInfo(FORWORD_HAS_WAIT_CAR, nowPosition);
            }
        }
        if (roadOffset >= getRealSpeedOnNowRoad())
            return new ForwordInfo(FORWORD_HAS_NO_CAR, nowPosition);
        else
            return new ForwordInfo(FORWORD_NEED_THROUGH_CROSS, nowPosition);
    }

    @Override
    public int compareTo(Car other) {
        if (isPriorityCar && other.isPriorityCar) {
            if (roadOffset != other.roadOffset)
                return roadOffset - other.roadOffset;
            else
                return nowRoadChannel - other.nowRoadChannel;
        } else if (isPriorityCar ^ other.isPriorityCar) {
            return isPriorityCar ? -1 : 1;
        } else {
            if (roadOffset != other.roadOffset)
                return roadOffset - other.roadOffset;
            else
                return nowRoadChannel - other.nowRoadChannel;
        }
    }
}

class ForwordInfo {
    int forwardStatus;
    int toPosition;

    ForwordInfo(int forwardStatus, int toPosition) {
        this.forwardStatus = forwardStatus;
        this.toPosition = toPosition;
    }

    ForwordInfo() {
    }
}

class Road implements Cloneable {
    int roadID;
    int roadLength;
    int speedLimitofRoad;
    int channelCount;//车道数
    Cross fromCross;
    Cross toCross;

    public Road clone() {
        try {
            return (Road) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    boolean isDuplex;// 是否是单双道
    //该道路from->to上的车辆ID。按照选路优先级排列。//int[roadLength][channelCount],
    Car[][] carOnRoad;

    //该道路双车道to->from上的车辆ID。按照选路优先级排列。//int[roadLength][channelCount],
    Car[][] carOnDuplexRoad;

    public Road(int roadId) {
        this.roadID = roadId;
    }

    Car[][] getCarArray(Cross toCross) {//给定这条道路出路口，获取车辆数组
        if (this.toCross.crossID == toCross.crossID) {
            return this.carOnRoad;
        } else {
            if (!isDuplex) {
                return null;
            } else {
                return this.carOnDuplexRoad;
            }
        }
    }

    public float getCarNumRate(Cross toCross) {
        Car[][] cars = getCarArray2(toCross);
        if (cars == null) return 1;
        float count = 0;
        for (int i = 0; i < channelCount; i++) {
            for (int j = 0; j < roadLength; j++) {
                if (cars[i][j] != null) {
                    count++;
                }
            }
        }
        return count / (channelCount * roadLength);
    }

    Car[][] getCarArray2(Cross fromCross) {//给定这条道路入路口，获取车辆数组
        if (this.fromCross.crossID == fromCross.crossID) {
            return this.carOnRoad;
        } else {
            if (!isDuplex) {
                return null;
            } else {
                return this.carOnDuplexRoad;
            }
        }
    }


    public void setRoad(int roadLength, int speedLimitofRoad, int channelCount, Cross fromCross, Cross toCross, int isDuplexT) {
        this.roadLength = roadLength;
        this.speedLimitofRoad = speedLimitofRoad;
        this.channelCount = channelCount;

        this.fromCross = fromCross;
        this.toCross = toCross;
        if (isDuplexT == 1) {
            this.isDuplex = true;
        } else {
            this.isDuplex = false;
        }

        carOnRoad = new Car[channelCount][roadLength];
        if (isDuplex)
            carOnDuplexRoad = new Car[channelCount][roadLength];
    }


}

class Graph {
    float[][][] graphGlobal;
    HashMap<Integer, Integer> graphMapCross; //实际CrossID映射路网0123456...的ID
    HashMap<Integer, Integer> crossMapgraph; //0123...映射路网crossID
}

class Cross implements Cloneable {
    int crossID;
    Road[] crossRoads;//按照顺时针顺序存放
    ArrayList<Road> sortedCrossRoad = new ArrayList<>();//按照id从小到大存放

    public Cross clone() {
        try {
            return (Cross) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Cross(int crossID, Road fristRoad, Road secondRoad, Road thirdRoad, Road forthRoad) {
        this.crossID = crossID;
        crossRoads = new Road[4];
        crossRoads[0] = fristRoad;
        if (fristRoad != null)
            sortedCrossRoad.add(fristRoad);
        crossRoads[1] = secondRoad;
        if (secondRoad != null)
            sortedCrossRoad.add(secondRoad);
        crossRoads[2] = thirdRoad;
        if (thirdRoad != null)
            sortedCrossRoad.add(thirdRoad);
        crossRoads[3] = forthRoad;
        if (forthRoad != null)
            sortedCrossRoad.add(forthRoad);
        Collections.sort(sortedCrossRoad, new Comparator<Road>() {
            @Override
            public int compare(Road o1, Road o2) {
                if (o1 != null && o2 != null) {
                    return o1.roadID - o2.roadID;
                }
                return 0;
            }
        });
    }
}

class CarPos {
    int roadID;
    int channel;
    int roadLength;

    public CarPos(int roadID, int channel, int roadLength) {
        this.roadID = roadID;
        this.channel = channel;
        this.roadLength = roadLength;
    }
}

class DispatchCenter {
    int countMove = 0;
    int carNumInRoad;
    int carsNumLimit = 5000;
    int backTime;
    int MTime;
    ArrayList<Car> cars;
    ArrayList<Road> roads;
    ArrayList<Cross> crosses;//升序排列的路口
    HashMap<Integer, Road> roadHashMap;
    HashMap<Integer, Car> carHashMap;
    HashMap<Integer, Cross> crossHashMap;
    Func func;

    DispatchCenter(Func func, ArrayList<Car> cars, ArrayList<Road> roads,
                   ArrayList<Cross> crosses,
                   HashMap<Integer, Road> roadHashMap,
                   HashMap<Integer, Car> carHashMap,
                   HashMap<Integer, Cross> crossHashMap) {
        this.func = func;
        this.cars = cars;
        this.roads = roads;
        this.crosses = crosses;
        this.crossHashMap = crossHashMap;
        this.carHashMap = carHashMap;
        this.roadHashMap = roadHashMap;
    }

    public boolean DispatchOneTimePiece() {
        markAllCars();
        if (dispatchWaitCar()) {
            carsNumLimit += 16;
            if (MTime >= 750)
                carsNumLimit += 32;
            backTime--;
            runStartCar();
            checkCar();
            System.out.println("time:" + MTime);
            MTime++;
            return true;
        }
        return false;
    }

    public void dispatchGoBack(CopyData originCopyData) {
        CopyData copyData = func.copyData(originCopyData.cars_copy, originCopyData.roads_copy, originCopyData.crosses_copy,
                originCopyData.roadHashMap_copy, originCopyData.carHashMap_copy, originCopyData.crossHashMap_copy, originCopyData.MTime,
                originCopyData.carNumInRoad, originCopyData.carsNumLimit);

        this.MTime = copyData.MTime;
        this.carNumInRoad = copyData.carNumInRoad;
        for (int i = 0; i < cars.size(); i++) {
            //增加阻塞强范围控制
            if (cars.get(i).isStart == 2 && cars.get(i).flag == Car.WAIT_STATUS) {
                copyData.cars_copy.get(i).kasi = true;
            }
        }
        this.cars = copyData.cars_copy;
        this.roads = copyData.roads_copy;
        this.crosses = copyData.crosses_copy;
        this.crossHashMap = copyData.crossHashMap_copy;
        this.carHashMap = copyData.carHashMap_copy;
        this.roadHashMap = copyData.roadHashMap_copy;
        if (backTime <= 0) {
            this.carsNumLimit = (int) (copyData.carsNumLimit * 0.9668);
//            this.carsNumLimit = (int) (copyData.carsNumLimit - 200);
//            this.carsNumLimit = this.carsNumLimit<10000?this.carsNumLimit:10000;
            backTime = 5;
        }
    }

    public CopyData CopyDispatchTimePieceData() {
        return func.copyData(cars, roads, crosses, roadHashMap, carHashMap, crossHashMap, MTime,
                carNumInRoad, carsNumLimit);
    }

    public boolean checkCar() {
        int allCarsNum = 0;
        HashMap<Integer, CarPos> carsOnRoadHashMap = new HashMap<>();
        for (Road road : roads) {
            for (int i = 0; i < road.channelCount; i++) {
                for (int j = 0; j < road.roadLength; j++) {
                    if (road.carOnRoad[i][j] != null) {
                        allCarsNum++;
                        if (carsOnRoadHashMap.containsKey(road.carOnRoad[i][j].carID))
                            System.out.println("wrong");
                        else
                            carsOnRoadHashMap.put(road.carOnRoad[i][j].carID, new CarPos(road.roadID, i, j));
                    }
                    if (road.isDuplex && road.carOnDuplexRoad[i][j] != null) {
                        allCarsNum++;
                        if (carsOnRoadHashMap.containsKey(road.carOnDuplexRoad[i][j].carID))
                            System.out.println("wrong");
                        else
                            carsOnRoadHashMap.put(road.carOnDuplexRoad[i][j].carID, new CarPos(road.roadID, i, j));
                    }
                }
            }
        }
        if (allCarsNum != carNumInRoad) {
            System.out.println("Wrong,i think car num is " + carNumInRoad + "，but the real car on roads is" + allCarsNum);
            return false;
        }
        System.out.println("Yes,car num on road is " + carNumInRoad);
        return true;
    }

    /*
     * add by luzhen
     * initialCrossRoadMap()
     * find the first WAIT_STATUS car in every road 返回falsed代表这条路口还需要调度，返回true代表这条路口不需要调度了
     * */
    private boolean initialCrossRoadMap(Cross cross, HashMap<Pair<Integer, Integer>, Pair<Boolean, PriorityQueue<Car>>> crossRoadMap) {
        boolean isNowCrossNeedDispatch = false;
        for (Road road : cross.sortedCrossRoad) {
            Car[][] carRoad = road.getCarArray(cross);//根据出路口获取车辆数组
            if (carRoad == null) {//如果是单向道，该出路口没有车道
                crossRoadMap.put(new Pair<>(cross.crossID, road.roadID), new Pair<>(true, null));
            } else {
                PriorityQueue<Car> priorityCars = new PriorityQueue<>();
                for (int channel = 0; channel < road.channelCount; channel++) {
                    for (int offset = 0; offset < road.roadLength; offset++) {
                        if (carRoad[channel][offset] != null && carRoad[channel][offset].flag == Car.WAIT_STATUS) {
                            priorityCars.add(carRoad[channel][offset]);
                            break;
                        }
                    }
                }
                if (priorityCars.isEmpty())
                    crossRoadMap.put(new Pair<>(cross.crossID, road.roadID), new Pair<>(true, null));
                else
                    crossRoadMap.put(new Pair<>(cross.crossID, road.roadID), new Pair<>(false, priorityCars));
            }
        }
        return isNowCrossNeedDispatch;
    }

    /*
     * add by luzhen
     * updateCrossRoadMap()
     * find the next WAIT_STATUS car in this road
     * nowCar = topPriorityCar 已经调度完成
     * 寻找该条道路上的下一个WAIT_STATUS的车辆, 将nowCar更新为nextCar
     * 如果没有nextCar，则该条道路调度结束
     * return Boolean  true 本条路调度完成 false 本条路调度未完成
     * */
    private boolean putNextTopPriorityCarIntoHashMap(Cross cross, Road road, Car[][] carRoad, int lastCarChannel, int lastCarOffset,HashMap<Pair<Integer, Integer>, Pair<Boolean, PriorityQueue<Car>>> crossRoadMap) {
        PriorityQueue<Car> priorityCar = crossRoadMap.get(new Pair<>(cross.crossID, road.roadID)).getV();
        for (int length = lastCarOffset; length < road.roadLength; ++length) {
            if(carRoad[lastCarChannel][length]!=null&&carRoad[lastCarChannel][length].flag==Car.WAIT_STATUS){
                priorityCar.add(carRoad[lastCarChannel][length]);
                return true;
            }
        }
        return false;
    }


    /*
     * add by luzhen
     * dispatchWaitCar()
     */
    private boolean dispatchWaitCar() {
        if (crosses.size() == 0)
            return true;
        HashMap<Integer, Boolean> crossMap = new HashMap<Integer, Boolean>();
        HashMap<Pair<Integer, Integer>, Pair<Boolean, PriorityQueue<Car>>> crossRoadMap = new HashMap<>();
        for (Cross cross : crosses)
            crossMap.put(cross.crossID, !initialCrossRoadMap(cross, crossRoadMap));
        int maxDispCount = 0;
        for (int i = 0; i < cars.size(); i++) {
            if (cars.get(i).flag == Car.WAIT_STATUS && cars.get(i).isStart == 2) {
                maxDispCount++;
            }
        }
        int dispCount = 0;
        while (true) {
            dispCount++;
            boolean bAllCrossesDispatch = false;
            countMove = 0;
            for (Cross cross : crosses) {

                if (crossMap.get(cross.crossID)) {//该路口调度结束
                    continue;
                }
                ArrayList<Road> sortRoads = cross.sortedCrossRoad;

                for (Road road : sortRoads) {//开始调度一个路口
                    if (crossRoadMap.get(new Pair<>(cross.crossID, road.roadID)).getK())//该条路调度结束
                        continue;
                    Car[][] carRoad = road.getCarArray(cross);//根据出路口获取车辆数组
                    //如果是单向道，该出路口没有车道（这里在initialCrossRoadMap()函数已经考虑到了，自然时调度结束true）
                    if (carRoad == null)
                        continue;
                    int tempChannel,tempOffset;
                    do {//依次调度这条道路上的WAIT_STATUS车辆
                        //该topPriorityCar车不为空，其已经初始化为某条路上的第一辆WAIT_STATUS的车
                        Car topPriorityCar = crossRoadMap.get(new Pair<>(cross.crossID, road.roadID)).getV().poll();//寻找到该车道第一优先级的第一辆车
                        if (crossRoadMap.get(new Pair<>(cross.crossID, road.roadID)).getK()) {
                            System.out.println("Wrong WrongWrongWrongWrongWrongWrongWrongWrongWrongWrongWrong");
                        }
                        tempChannel = topPriorityCar.nowRoadChannel;//保留topCar的初始位置channel信息
                        tempOffset = topPriorityCar.roadOffset;//保留topCar的初始位置offset信息
                        ForwordInfo forwordInfo = topPriorityCar.getTypeOfForwordCar();
                        if (forwordInfo.forwardStatus == Car.FORWORD_NEED_THROUGH_CROSS) {
                            if (cross.crossID == topPriorityCar.endCrossID) {
                                topPriorityCar.getCarArray()[topPriorityCar.nowRoadChannel][topPriorityCar.roadOffset] = null;
                                topPriorityCar.isFinishedAtTimePiece = true;
                                topPriorityCar.isFinish = true;
                                topPriorityCar.nowRoad = null;
                                topPriorityCar.nextRoad = null;
                                topPriorityCar.flag = Car.END_STATUS;
                                topPriorityCar.maxTimeCount = MTime;
                                carNumInRoad--;
                                countMove++;
                            } else {
                                topPriorityCar.getNxtChoice(func);
                                int roadInClockSeq = getRoadInClockSeq(road.roadID, cross.crossRoads, 4);
                                if (topPriorityCar.getDirection() == Car.GO_WRONG)
                                    System.out.println(topPriorityCar.carID + "转向出错，我下一条路是我自己");
                                else if (topPriorityCar.getDirection() == Car.GO_LEFT) {//左转
                                    int conRoadInClockSeq = (roadInClockSeq + 3) % 4;//冲突车道在clockRoads中的顺序
                                    if (bLeftConflict(Car.GO_DOWN, cross.crossRoads[conRoadInClockSeq], cross, crossRoadMap)) {//发生冲突
                                        break;//发生冲突，转向下一条路
                                    }
                                } else if (topPriorityCar.getDirection() == Car.GO_RIGHT) {//右转
                                    int goDownClockSeq = (roadInClockSeq + 1) % 4;//冲突车道在clockRoads中的顺序,左方直行
                                    int goLeftClockSeq = (roadInClockSeq + 2) % 4;//冲突车道在clockRoads中的顺序,对方左转
                                    if (bRightConflict(Car.GO_DOWN, cross.crossRoads[goDownClockSeq], Car.GO_LEFT, cross.crossRoads[goLeftClockSeq], cross, crossRoadMap)) {
                                        break;//发生冲突，转向下一条路
                                    }
                                }
                                int stepCounts = Math.min(topPriorityCar.maxSpeedofCar, topPriorityCar.nextRoad.speedLimitofRoad) - topPriorityCar.roadOffset;
                                stepCounts = stepCounts > 0 ? stepCounts : 0;
                                if (!setCarStatusWhenThroughCross(topPriorityCar, cross, stepCounts))
                                    break;
                            }
                            //dispatch this channel，前车变成了终止状态，更新这条车道上的后续车辆
                            int offset = tempOffset + 1;
                            for (; offset < road.roadLength; ++offset) {
                                if (carRoad[tempChannel][offset] == null)
                                    continue;
                                Car channelNextCar = carRoad[tempChannel][offset];
                                ForwordInfo channelNextCarForwordInfo = channelNextCar.getTypeOfForwordCar();
                                if (channelNextCarForwordInfo.forwardStatus == Car.FORWORD_NEED_THROUGH_CROSS)
                                    break;
                                else {
                                    if (channelNextCar.flag == Car.END_STATUS) {
                                        break;
                                    } else {
                                        channelNextCar.getCarArray()[channelNextCar.nowRoadChannel][channelNextCar.roadOffset] = null;
                                        channelNextCar.roadOffset = channelNextCarForwordInfo.toPosition;
                                        channelNextCar.getCarArray()[channelNextCar.nowRoadChannel][channelNextCar.roadOffset] = channelNextCar;
                                        channelNextCar.isFinishedAtTimePiece = true;
                                        channelNextCar.flag = Car.END_STATUS;
                                        countMove++;
                                    }
                                }
                            }
                        }
                    }
                    while (putNextTopPriorityCarIntoHashMap(cross, road, carRoad,tempChannel,tempOffset,crossRoadMap));
                    //runCar后 车的状态变为终止状态，则继续遍历该车道的后续的WAIT_STATUS车，并更新 crossRoadMap 存放的第一辆车的信息
                }//end for sorted roads
                //if (!bCrossDispatch)
                //该路口为阻塞状态或者该路口的四条路已经全部调度结束,开始调度下一个路口
                //检测该路口是否调度结束
                int flag = 1;
                for (Road road : sortRoads) {
                    if (!crossRoadMap.get(new Pair<>(cross.crossID, road.roadID)).getK()) {//存在没有调度结束的路
                        crossMap.put(cross.crossID, false);
                        flag = 0;
                        break;
                    }
                }
                if (flag == 1) {
                    crossMap.put(cross.crossID, true);
                }
            }

            for (Cross crosstmp : crosses) {
                if (!crossMap.get(crosstmp.crossID)) {
                    bAllCrossesDispatch = false;
                    break;
                } else {
                    bAllCrossesDispatch = true;
                }
            }
            if (bAllCrossesDispatch) {//所有路口调度结束
                return true;
            }
            //卡死时候强制退出
            if (dispCount > maxDispCount) {
                //System.out.println("我进入了卡死");
                return false;
            }
        }

    }


    /*
     * add by ccg & lz
     *setCarStatusWhenThroughCross()通过路口
     * */
    private boolean setCarStatusWhenThroughCross(Car car, Cross nowCross, int stepCounts) {//当车要通过路口时，设置该车的最终状态（进入下一道路的某一车道或原地等待或者收到下一道路限速的影响，挪到原车道的0号位置，）。

        Car[][] nextRoad = null;//这条路为next路的数组
        if (car.nextRoad.fromCross == nowCross)
            nextRoad = car.nextRoad.carOnRoad;
        else {
            nextRoad = car.nextRoad.carOnDuplexRoad;
        }
        if (stepCounts <= 0) {
            if (nowCross.crossID == car.nowRoad.toCross.crossID) {
                car.nowRoad.carOnRoad[car.nowRoadChannel][car.roadOffset] = null;
                car.nowRoad.carOnRoad[car.nowRoadChannel][0] = car;
            } else {
                car.nowRoad.carOnDuplexRoad[car.nowRoadChannel][car.roadOffset] = null;
                car.nowRoad.carOnDuplexRoad[car.nowRoadChannel][0] = car;
            }
            car.flag = Car.END_STATUS;
            car.roadOffset = 0;
            car.isFinishedAtTimePiece = true;//是否一个时间片完成调度
            countMove++;
            return true;
        }
        for (int i = 0; i < car.nextRoad.channelCount; i++) {
            try {
                //如果前方小车道满了，就去找大车道
                if (nextRoad[i][car.nextRoad.roadLength - 1] != null && nextRoad[i][car.nextRoad.roadLength - 1].flag == Car.END_STATUS)
                    continue;
                //找到一个车道，在该车到模拟每一步行走
                for (int eachSetp = 0; eachSetp < stepCounts; eachSetp++) {
                    //没有走完全程发现前方有车
                    if (nextRoad[i][car.nextRoad.roadLength - 1 - eachSetp] != null) {
                        if (nextRoad[i][car.nextRoad.roadLength - 1 - eachSetp].flag == Car.WAIT_STATUS) {
                            car.flag = Car.WAIT_STATUS;//前方车辆为等待状态，那么该车标为等待状态
                            return false;
                        } else if (nextRoad[i][car.nextRoad.roadLength - 1 - eachSetp].flag == Car.END_STATUS) {
                            // 发现前方有辆终止状态的车，把该车挪到终止车辆的后方，然后标为终止状态。
                            // 更新旧路况信息
                            if (car.nowRoad.toCross.crossID == nowCross.crossID) {
                                car.nowRoad.carOnRoad[car.nowRoadChannel][car.roadOffset] = null;
                            } else {
                                car.nowRoad.carOnDuplexRoad[car.nowRoadChannel][car.roadOffset] = null;
                            }
                            //更新车的新路况信息  roadChoiceList;isForwardRoad;isFinishedAtTimePiece
                            int newOffset = car.nextRoad.roadLength - eachSetp;
                            car.flag = Car.END_STATUS;
                            car.nowRoad = car.nextRoad;//更新车的当前路径为下一条路径
                            car.nextRoad = null;  //未知状态,下一条路径需要重新选择
                            car.roadOffset = newOffset;
                            car.nowRoadChannel = i;
                            nextRoad[i][newOffset] = car;
                            if (car.nowRoad.fromCross.crossID == nowCross.crossID) {//
                                car.isForwardRoad = true;
                            } else {
                                car.isForwardRoad = false;
                            }
                            car.isFinishedAtTimePiece = true;//是否一个时间片完成调度
                            countMove++;
                            car.roadChoiceList.add(car.nowRoad.roadID);//在路径列表中记录这条路
                            return true;
                        }
                    }
                }
                //所有step都走完了，下一条路没有车阻挡。把车挪到最远的地方。
                if (nowCross.crossID == car.nowRoad.toCross.crossID) {
                    car.nowRoad.carOnRoad[car.nowRoadChannel][car.roadOffset] = null;
                } else {
                    car.nowRoad.carOnDuplexRoad[car.nowRoadChannel][car.roadOffset] = null;
                }
                int newOffset = car.nextRoad.roadLength - stepCounts;
                car.flag = Car.END_STATUS;
                car.roadChoiceList.add(car.nextRoad.roadID);//在路径列表中记录这条路
                car.nowRoad = car.nextRoad;//更新车的当前路径为下一条路径
                car.nextRoad = null;  //未知状态,下一条路径需要重新选择
                car.roadOffset = newOffset;
                car.nowRoadChannel = i;
                nextRoad[car.nowRoadChannel][newOffset] = car;
                car.isForwardRoad = car.nowRoad.fromCross.crossID == nowCross.crossID;
                car.isFinishedAtTimePiece = true;//是否一个时间片完成调度
                countMove++;
                return true;
            } catch (Exception e) {
                System.out.println("hello");
            }
        }
        //所有channel都遍历完了，该车前方车道完全堵死，过不了路口，只能挪到路口头部处标为终止
        if (nowCross.crossID == car.nowRoad.toCross.crossID) {
            car.nowRoad.carOnRoad[car.nowRoadChannel][car.roadOffset] = null;
            car.nowRoad.carOnRoad[car.nowRoadChannel][0] = car;
        } else {
            car.nowRoad.carOnDuplexRoad[car.nowRoadChannel][car.roadOffset] = null;
            car.nowRoad.carOnDuplexRoad[car.nowRoadChannel][0] = car;
        }
        car.flag = Car.END_STATUS;
        car.roadOffset = 0;
        car.isFinishedAtTimePiece = true;//是否一个时间片完成调度
        countMove++;
        return true;
    }

    /*
     * add by luzhen
     * getRoadInClockSeq()
     */
    private int getRoadInClockSeq(int roadID, Road[] clockRoads, int clockNum) {
        for (int i = 0; i < clockNum; ++i) {
            if (clockRoads[i] == null) continue;
            if (roadID == clockRoads[i].roadID) {
                return i;
            }
        }
        System.out.println("getRoadInClockSeq() error...");
        return -1;//error
    }

    /*
     * add by luzhen
     * bRightConflict()
     * 判断汽车右转可能发生的冲突
     * 1、对面车道左转
     * 2、左边车道直行
     * direction1 go down
     * goLeftDirection go left
     */
    private boolean bRightConflict(int goDownDirection, Road goDownConflictRoad, int goLeftDirection, Road goLeftConflictRoad, Cross cross, HashMap<Pair<Integer, Integer>, Pair<Boolean, Car>> crossRoadMap) {
        boolean bGoDownConflict = false;
        boolean bGoLeftConflict = false;

        if (goDownConflictRoad != null && !crossRoadMap.get(new Pair<>(cross.crossID, goDownConflictRoad.roadID)).getK()) {//冲突的路未调度结束
            //先检测
            if (crossRoadMap.get(new Pair<>(cross.crossID, goDownConflictRoad.roadID)).getV().getDirection() == goDownDirection) {//右转与左路直行冲突
                bGoDownConflict = true;
            }
        }
        if (goLeftConflictRoad != null && !crossRoadMap.get(new Pair<>(cross.crossID, goLeftConflictRoad.roadID)).getK()) {//冲突的路未调度结束
            if (crossRoadMap.get(new Pair<>(cross.crossID, goLeftConflictRoad.roadID)).getV().getDirection() == goLeftDirection) {//右转与对路左转冲突
                bGoLeftConflict = true;
            }
        }
        return bGoDownConflict || bGoLeftConflict;
    }

    /*
     * add by luzhen
     * bLeftConflict()
     * 判断汽车行驶左转冲突
     * 1、右边车道直行冲突
     */
    private boolean bLeftConflict(int direction, Road conflictRoad, Cross cross, HashMap<Pair<Integer, Integer>, Pair<Boolean, PriorityQueue<Car>>> crossRoadMap) {
        if (conflictRoad != null && !crossRoadMap.get(new Pair<>(cross.crossID, conflictRoad.roadID)).getK()) {//冲突的路未调度结束
            Car conflictCar = crossRoadMap.get(new Pair<>(cross.crossID, conflictRoad.roadID)).getV().peek();
            //先检测车方向是否与当前车的方向冲突
            //检测车的优先级
            if (conflictCar.getDirection() == direction) {//左转与直行冲突
                return true;
            }
        }
        return false;
    }

    private void markAllCars() {
        for (Car car : cars) {
            if (car.isStart == 2 && !car.isFinish)
                car.flag = Car.WAIT_STATUS;
        }
        for (Road road : roads) {
            Car[][] carOnRoad = road.carOnRoad;
            Car[][] carOnDuplexRoad = road.carOnDuplexRoad;
            for (int eachChannel = 0; eachChannel < road.channelCount; eachChannel++) {
                for (int eachOffset = 0; eachOffset < road.roadLength; eachOffset++) {
                    markCar(road.carOnRoad[eachChannel][eachOffset]);
                    if (road.isDuplex)
                        markCar(road.carOnDuplexRoad[eachChannel][eachOffset]);
                }
            }
        }
    }

    private void markCar(Car car) {
        if (car == null)//该位置没有车辆
            return;
        Car[][] carOnRoad = car.getCarArray();
        ForwordInfo forwordInfo = car.getTypeOfForwordCar();//获取前方车辆信息
        //通过路口且前方没有阻挡
        if (forwordInfo.forwardStatus == Car.FORWORD_NEED_THROUGH_CROSS) {
            car.flag = Car.WAIT_STATUS;
        } else if (forwordInfo.forwardStatus == Car.FORWORD_HAS_NO_CAR) {
            carOnRoad[car.nowRoadChannel][forwordInfo.toPosition] = car;
            car.flag = Car.END_STATUS;
            carOnRoad[car.nowRoadChannel][car.roadOffset] = null;
            car.roadOffset = forwordInfo.toPosition;
            car.isFinishedAtTimePiece = true;
        }
        //前方有车辆阻挡,且前方车辆为终止状态
        else if (forwordInfo.forwardStatus == Car.FORWORD_HAS_END_CAR) {
            carOnRoad[car.nowRoadChannel][forwordInfo.toPosition] = car;
            car.flag = Car.END_STATUS;
            carOnRoad[car.nowRoadChannel][car.roadOffset] = null;
            car.roadOffset = forwordInfo.toPosition;
            car.isFinishedAtTimePiece = true;
        }
        //前方有车辆阻挡，且阻挡车辆为等待状态。
        else if (forwordInfo.forwardStatus == Car.FORWORD_HAS_WAIT_CAR) {
            car.flag = Car.WAIT_STATUS;
        }
    }

    /*
     * 该函数作用：运行出发车辆，该车辆被置为出发标记，且nowroad被正确设定。偏移量被设定为nowroad的长度。
     * */
    public void runStartCar() {
        int thisTimeStartCarNum = 0;

        ArrayList<Car> limitSpeed = new ArrayList<>();
        int limitS = 0;
        //Todo:调参地方车辆流控制

        for (int i = 0; i < cars.size(); i++) {
            if (cars.get(i).minStartTime == MTime && cars.get(i).isStart == 0) {
                limitSpeed.add(cars.get(i));
            }
        }
        Collections.sort(limitSpeed, new Comparator<Car>() {
            @Override
            public int compare(Car o1, Car o2) {
                if (o1 != null && o2 != null) {
                    return o2.maxSpeedofCar - o1.maxSpeedofCar;
                }
                return 0;
            }
        });
        //limitS = limitSpeed.size()>(CarsNum-carNumInRoad)?limitSpeed.get(CarsNum-1-carNumInRoad):0;
        int slowCarNum = 2;
        thisTimeStartCarNum = carsNumLimit - carNumInRoad;
        if (thisTimeStartCarNum > 0) {
            for (int j = thisTimeStartCarNum < slowCarNum ? thisTimeStartCarNum : slowCarNum; j > 0; j--) {
                if (j > limitSpeed.size()) {
                    continue;
                }
                limitSpeed.get(limitSpeed.size() - 1).isStart = 1; // 上路出发
                limitSpeed.get(limitSpeed.size() - 1).realStartTime = MTime;
                limitSpeed.get(limitSpeed.size() - 1).startCross = crossHashMap.get(limitSpeed.get(limitSpeed.size() - 1).startCrossID);
                limitSpeed.get(limitSpeed.size() - 1).endCross = crossHashMap.get(limitSpeed.get(limitSpeed.size() - 1).endCrossID);
                limitSpeed.get(limitSpeed.size() - 1).getFistChoice(func);//得到首次的选择，并更新car信息
                limitSpeed.remove(limitSpeed.size() - 1);
                thisTimeStartCarNum--;
            }
        }
        if (thisTimeStartCarNum > 0) {
            for (int k = 0; k < thisTimeStartCarNum; k++) {
                if (k >= limitSpeed.size())
                    break;
                limitSpeed.get(0).isStart = 1; // 上路出发
                limitSpeed.get(0).realStartTime = MTime;
                limitSpeed.get(0).startCross = crossHashMap.get(limitSpeed.get(0).startCrossID);
                limitSpeed.get(0).endCross = crossHashMap.get(limitSpeed.get(0).endCrossID);
                limitSpeed.get(0).getFistChoice(func);//得到首次的选择，并更新car信息
                limitSpeed.remove(0);
                thisTimeStartCarNum--;
            }
        }
        for (Car car : limitSpeed) {
            car.minStartTime++;
        }


        for (Car car : cars) {
            if (car.isStart == 1 && !car.isFinish) {
                Car[][] carOnRoad = car.getCarArray();
                for (int i = 0; i < car.nowRoad.channelCount; i++) {
                    if (carOnRoad[i][car.nowRoad.roadLength - 1] == null) {
                        car.isStart = 2;
                        car.nowRoadChannel = i;
                        car.flag = Car.END_STATUS;
                        car.isFinishedAtTimePiece = true;
                        carNumInRoad++;
                        ForwordInfo forwordInfo = car.getTypeOfForwordCar();
                        car.roadOffset = forwordInfo.toPosition;
                        carOnRoad[i][car.roadOffset] = car;
                        car.roadChoiceList.add(car.nowRoad.roadID);
                        break;
                    }
                }
                if (car.isStart == 1) {
                    car.isStart = 0;
                    car.minStartTime++;
                }
            }
        }
    }
}
