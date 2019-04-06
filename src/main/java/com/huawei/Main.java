package com.huawei;

import org.apache.log4j.Logger;

import java.util.*;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length != 5) {
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
        func.processPresetCar(carHashMap, presetAnswerPath);

        double alpha = 0;
        float priMinPlanTime = 0x3f3f3f3f;
        for (int i = 0; i < cars.size(); i++) {
            if (cars.get(i).isPriorityCar && priMinPlanTime > cars.get(i).backupStartTime)
                priMinPlanTime = cars.get(i).backupStartTime;
        }
        alpha = getAlpha(cars);


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
                if (dispatchCenter.cars.get(i).isFinish) {
                    num++;
                } else {
                    Car car = dispatchCenter.cars.get(i);
                    int ol = 0;
                }

            }
            System.out.println("finish cars num:" + num);

        }


        double score = 0;
        score = getScore(dispatchCenter.cars, dispatchCenter.MTime, priMinPlanTime, alpha);
        System.out.println("score:" + score);
        func.writeFile(dispatchCenter.cars, answerPath);
    }


    public static double getAlpha(ArrayList<Car> cars) {
        double alpha = 0;
        float pricarNum = 0;
        float carNum = 0;
        float pricarmaxSpeed = 0;
        float pricarminSpeed = 0x3f3f3f3f;
        float carmaxSpeed = 0;
        float carminSpeed = 0x3f3f3f3f;
        float pricarmaxSTime = 0;
        float pricarminSTime = 0x3f3f3f3f;
        float carmaxSTime = 0;
        float carminSTime = 0x3f3f3f3f;
        float pricarScount = 0;
        float carScount = 0;
        float pricarEcount = 0;
        float carEcount = 0;
        HashSet<Integer> carScountHashSet = new HashSet<>();
        HashSet<Integer> pricarScountHashSet = new HashSet<>();
        HashSet<Integer> carEcountHashSet = new HashSet<>();
        HashSet<Integer> pricarEcountHashSet = new HashSet<>();
        for (Car car : cars) {
            carScountHashSet.add(car.startCrossID);
            carEcountHashSet.add(car.endCrossID);
            carNum++;
            if (car.maxSpeedofCar > carmaxSpeed) carmaxSpeed = car.maxSpeedofCar;
            if (car.maxSpeedofCar < carminSpeed) carminSpeed = car.maxSpeedofCar;
            if (car.backupStartTime > carmaxSTime) carmaxSTime = car.backupStartTime;
            if (car.backupStartTime < carminSTime) carminSTime = car.backupStartTime;
            if (car.isPriorityCar) {
                pricarNum++;
                if (car.maxSpeedofCar > pricarmaxSpeed) pricarmaxSpeed = car.maxSpeedofCar;
                if (car.maxSpeedofCar < pricarminSpeed) pricarminSpeed = car.maxSpeedofCar;
                if (car.backupStartTime > pricarmaxSTime) pricarmaxSTime = car.backupStartTime;
                if (car.backupStartTime < pricarminSTime) pricarminSTime = car.backupStartTime;
                pricarScountHashSet.add(car.startCrossID);
                pricarEcountHashSet.add(car.endCrossID);
            }
        }
        pricarScount = pricarScountHashSet.size();
        carScount = carScountHashSet.size();
        pricarEcount = pricarEcountHashSet.size();
        carEcount = carEcountHashSet.size();
        alpha = 0.05 * carNum / pricarNum + 0.2375 * (carmaxSpeed / carminSpeed) / (pricarmaxSpeed / pricarminSpeed)
                + 0.2375 * (carEcount) / (pricarEcount) + 0.2375 * (carScount) / (pricarScount) + 0.2375 * (carmaxSTime / carminSTime) / (pricarmaxSTime / pricarminSTime);
        return alpha;
    }

    private static double getScore(ArrayList<Car> cars, int MTime, float PriminPlanTime, double alpha) {
        double score = 0;
        float maxPlanTime = 0;
        for (Car car : cars) {
            if (car.isPriorityCar) {
                if (maxPlanTime < car.maxTimeCount) maxPlanTime = car.maxTimeCount;
            }
        }
        score = alpha * (maxPlanTime - PriminPlanTime) + (double) MTime - 1;
        return score;

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
    public static final int NO_STATUS = -1;
    public static final int FORWORD_HAS_NO_CAR = 0;
    public static final int FORWORD_HAS_END_CAR = 1;
    public static final int FORWORD_HAS_WAIT_CAR = 2;
    public static final int FORWORD_NEED_THROUGH_CROSS = 3;
    public static final int NO_GO = 0;
    public static final int GOING = 1;
    public static final int GONE = 2;
    int fixNextRoad;
    boolean isPriorityCar;
    boolean isPresetCar;
    int presetRoadCount;
    int startCrossID;
    int endCrossID;
    int carID;
    Cross startCross;
    Cross endCross;
    int maxSpeedofCar;
    int minStartTime;
    int minTimeCount;
    int maxTimeCount;
    int backupStartTime;

    //是否是上个时间片留下的预置车辆
    int ProPresetCarCount;

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
    boolean isForward;

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
        if (isForward)
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
    public Car(int carID, int fromCrossID, int toCrossID, int maxSpeedofCar, int planTime, int pri, int preset) {
        this.carID = carID;
        this.startCrossID = fromCrossID;
        this.endCrossID = toCrossID;
        this.maxSpeedofCar = maxSpeedofCar;
        this.minStartTime = planTime;
        this.minTimeCount = planTime;
        this.isPriorityCar = pri == 1;
        this.isPresetCar = preset == 1;
        this.kasi = false;
        this.presetRoadCount = 0;
        this.backupStartTime = planTime;
        this.ProPresetCarCount = 0;
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

        if (isForward)
            cross = nowRoad.toCross;
        else
            cross = nowRoad.fromCross;
        if (getTypeOfForwordCar().forwardStatus != Car.FORWORD_NEED_THROUGH_CROSS) {
            return GO_NEVER;
        }

        if (!isPresetCar && cross.crossID == endCrossID) {
            return GO_DOWN;
        }
        if (isPresetCar && cross.crossID == endCrossID && presetRoadCount == roadChoiceList.size()) {
            return GO_DOWN;
        }
        if (nextRoad == null) {
            System.out.println("error");
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

    boolean getDirectionResult(HashMap<Pair<Integer, Integer>, PriorityQueue<Car>> crossRoadHash) {
        Cross cross = isForward ? nowRoad.toCross : nowRoad.fromCross;
        int direction = getDirection();
        int nowRoadSeq = cross.getRoadInClockSeq(nowRoad.roadID);
        int conLeftRoadInClockSeq = (nowRoadSeq + 1) % 4;//左路
        int conRightRoadInClockSeq = (nowRoadSeq + 3) % 4;//右路
        int conOppositeRoadInClockSeq = (nowRoadSeq + 2) % 4;//对面的路
        if (direction == Car.GO_DOWN) {
            if (isPriorityCar)
                return true;
            else {
                return !bStrightConflict(cross.crossRoads[conLeftRoadInClockSeq], cross.crossRoads[conRightRoadInClockSeq], cross, crossRoadHash);
            }
        } else if (direction == Car.GO_LEFT) {
            if (isPriorityCar) {
                return !bLeftTopConflict(Car.GO_DOWN, cross.crossRoads[conRightRoadInClockSeq], cross, crossRoadHash);
            } else {
                return !bLeftConflict(Car.GO_DOWN, cross.crossRoads[conRightRoadInClockSeq], Car.GO_RIGHT, cross.crossRoads[conOppositeRoadInClockSeq], cross, crossRoadHash);
            }
        } else if (direction == Car.GO_RIGHT) {
            if (isPriorityCar) {
                return !bRightTopConflict(Car.GO_DOWN, cross.crossRoads[conLeftRoadInClockSeq], Car.GO_LEFT, cross.crossRoads[conOppositeRoadInClockSeq], cross, crossRoadHash);
            } else {
                return !bRightConflict(Car.GO_DOWN, cross.crossRoads[conLeftRoadInClockSeq], Car.GO_LEFT, cross.crossRoads[conOppositeRoadInClockSeq], cross, crossRoadHash);
            }
        }
        return true;
    }

    private boolean bRightConflict(int goDown, Road leftRoad, int goLeft, Road oppositeRoad, Cross cross, HashMap<Pair<Integer, Integer>, PriorityQueue<Car>> crossRoadHash) {
        boolean bConflict = false;
        if (leftRoad != null && crossRoadHash.get(new Pair<>(cross.crossID, leftRoad.roadID)).size() != 0) {
            Car conflictCar = crossRoadHash.get(new Pair<>(cross.crossID, leftRoad.roadID)).peek();
            if (conflictCar != null && conflictCar.getDirection() == goDown) {
                bConflict = true;
            }
        }
        if (oppositeRoad != null && crossRoadHash.get(new Pair<>(cross.crossID, oppositeRoad.roadID)).size() != 0) {
            Car conflictCar = crossRoadHash.get(new Pair<>(cross.crossID, oppositeRoad.roadID)).peek();
            if (conflictCar != null && conflictCar.getDirection() == goLeft) {
                bConflict = true;
            }
        }
        return bConflict;
    }

    private boolean bRightTopConflict(int goDown, Road leftRoad, int goLeft, Road oppositeRoad, Cross cross, HashMap<Pair<Integer, Integer>, PriorityQueue<Car>> crossRoadHash) {
        boolean bConflict = false;
        if (leftRoad != null && crossRoadHash.get(new Pair<>(cross.crossID, leftRoad.roadID)).size() != 0) {
            Car conflictCar = crossRoadHash.get(new Pair<>(cross.crossID, leftRoad.roadID)).peek();
            if (conflictCar != null && conflictCar.isPriorityCar && conflictCar.getDirection() == goDown) {
                bConflict = true;
            }
        }
        if (oppositeRoad != null && crossRoadHash.get(new Pair<>(cross.crossID, oppositeRoad.roadID)).size() != 0) {
            Car conflictCar = crossRoadHash.get(new Pair<>(cross.crossID, oppositeRoad.roadID)).peek();
            if (conflictCar != null && conflictCar.isPriorityCar && conflictCar.getDirection() == goLeft) {
                bConflict = true;
            }
        }
        return bConflict;
    }

    private boolean bLeftConflict(int goDown, Road rightRoad, int goRight, Road oppositeRoad, Cross cross, HashMap<Pair<Integer, Integer>, PriorityQueue<Car>> crossRoadHash) {
        boolean bConflict = false;
        if (rightRoad != null && crossRoadHash.get(new Pair<>(cross.crossID, rightRoad.roadID)).size() != 0) {
            Car conflictCar = crossRoadHash.get(new Pair<>(cross.crossID, rightRoad.roadID)).peek();
            if (conflictCar != null && conflictCar.getDirection() == goDown) {
                bConflict = true;
            }
        }
        if (oppositeRoad != null && crossRoadHash.get(new Pair<>(cross.crossID, oppositeRoad.roadID)).size() != 0) {
            Car conflictCar = crossRoadHash.get(new Pair<>(cross.crossID, oppositeRoad.roadID)).peek();
            if (conflictCar != null && conflictCar.isPriorityCar && conflictCar.getDirection() == goRight) {
                bConflict = true;
            }
        }
        return bConflict;
    }

    private boolean bLeftTopConflict(int goDown, Road rightRoad, Cross cross, HashMap<Pair<Integer, Integer>, PriorityQueue<Car>> crossRoadHash) {
        boolean bConflict = false;
        if (rightRoad != null && crossRoadHash.get(new Pair<>(cross.crossID, rightRoad.roadID)).size() != 0) {
            Car conflictCar = crossRoadHash.get(new Pair<>(cross.crossID, rightRoad.roadID)).peek();
            if (conflictCar != null && conflictCar.isPriorityCar && conflictCar.getDirection() == goDown) {
                bConflict = true;
            }
        }
        return bConflict;
    }

    private boolean bStrightConflict(Road leftRoad, Road rightRoad, Cross cross, HashMap<Pair<Integer, Integer>, PriorityQueue<Car>> crossRoadHash) {
        boolean bConflict = false;
        if (leftRoad != null && crossRoadHash.get(new Pair<>(cross.crossID, leftRoad.roadID)).size() != 0) {
            Car conflictCar = crossRoadHash.get(new Pair<>(cross.crossID, leftRoad.roadID)).peek();
            if (conflictCar != null && conflictCar.isPriorityCar && conflictCar.getDirection() == GO_LEFT) {
                bConflict = true;
            }
        }
        if (rightRoad != null && crossRoadHash.get(new Pair<>(cross.crossID, rightRoad.roadID)).size() != 0) {
            Car conflictCar = crossRoadHash.get(new Pair<>(cross.crossID, rightRoad.roadID)).peek();
            if (conflictCar != null && conflictCar.isPriorityCar && conflictCar.getDirection() == GO_RIGHT) {
                bConflict = true;
            }
        }
        return bConflict;
    }

    /**
     * 道路最高优先级的拥堵车辆才可以选择下一个道路。所以分配时候每走一辆拥堵车辆都要更新当前道路车辆flag，并进行重新
     *
     * @param func
     */
    Random random = new Random(4050);

    public void getNxtChoice(Func func, int carNumInRoad) {
        // 判断是否轮得到这辆车选择下一个路口，如果轮到了，进行选择，没有轮到，return；
        Cross cross;
        if (isForward)
            cross = nowRoad.toCross;
        else
            cross = nowRoad.fromCross;

        //预置车辆得到下一条路。
        if (nextRoad != null) return;
        if (isPresetCar) {
            if (roadChoiceList.size() == presetRoadCount) return;
            if (nextRoad == null && getTypeOfForwordCar().forwardStatus == Car.FORWORD_NEED_THROUGH_CROSS) {
                for (int i = 0; i < 4; i++) {

                    if (cross.crossRoads[i] != null && cross.crossRoads[i].roadID == roadChoiceList.get(presetRoadCount)) { //道路不为空而且是那条路
                        nextRoad = cross.crossRoads[i];
                        presetRoadCount++;
                        return;
                    }


                }
            }
            return;
        }


        if (nextRoad == null && getTypeOfForwordCar().forwardStatus == Car.FORWORD_NEED_THROUGH_CROSS) {
            //随机选路法（仅限于卡死情况）
            int yu = 35;
            if (carNumInRoad > 4000) yu = 30;
            if (kasi || random.nextInt(100) % yu == 0) {
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

                            float tmp = func.dijReslove(graph.graphGlobal, graphMapCross.get(cross.crossRoads[i].fromCross.crossID), graphMapCross.get(endCrossID));
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
                            float tmp = func.dijReslove(graph.graphGlobal, graphMapCross.get(cross.crossRoads[i].toCross.crossID), graphMapCross.get(endCrossID));
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
                            float tmp = func.dijReslove(graph.graphGlobal, graphMapCross.get(cross.crossRoads[i].toCross.crossID), graphMapCross.get(endCrossID));
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
        //如果是预置车辆
        if (nextRoad != null) return;
        if (isPresetCar) {
            for (int i = 0; i < 4; i++) {
                if (startCross.crossRoads[i] != null && startCross.crossRoads[i].roadID == roadChoiceList.get(presetRoadCount)) { //道路不为空而且为所在的道路
                    nowRoad = startCross.crossRoads[i];
                    nextRoad = null;
                    roadOffset = nowRoad.roadLength;
                    if (startCross.crossID == startCross.crossRoads[i].fromCross.crossID) {
                        isForward = true;
                    } else {
                        isForward = false;
                    }
                    presetRoadCount++;
                    break;
                }
            }
        } else {
            //TODO:如果已经选过了第一条路则不再选
            if (nowRoad != null) return;
            float score = 0x3f3f3f3f;
            for (int i = 0; i < 4; i++) {
                if (startCross.crossRoads[i] != null) { //道路不为空
                    // 双车道时，起点或者终点为所选的第二个节点即可，单车道道时必须时终点为第二个节点。
                    if (startCross.crossRoads[i].isDuplex) {
                        if (startCross.crossID == startCross.crossRoads[i].toCross.crossID) {
                            float tmp = func.dijReslove(graph.graphGlobal, graphMapCross.get(startCross.crossRoads[i].fromCross.crossID), graphMapCross.get(endCrossID));
                            float len = startCross.crossRoads[i].roadLength;
                            float speed = Math.min(maxSpeedofCar, startCross.crossRoads[i].speedLimitofRoad);
                            tmp += len / speed;
                            tmp = tmp * (1 + startCross.crossRoads[i].getCarNumRate(startCross));
                            if (tmp < score) {
                                nowRoad = startCross.crossRoads[i];
                                nextRoad = null;//ccg开始上路，nextroad为null
                                isForward = false;
                                score = tmp;
                            }
                        } else if (startCross.crossID == startCross.crossRoads[i].fromCross.crossID) {
                            float tmp = func.dijReslove(graph.graphGlobal, graphMapCross.get(startCross.crossRoads[i].toCross.crossID), graphMapCross.get(endCrossID));
                            float len = startCross.crossRoads[i].roadLength;
                            float speed = Math.min(maxSpeedofCar, startCross.crossRoads[i].speedLimitofRoad);
                            tmp += len / speed;
                            tmp = tmp * (1 + startCross.crossRoads[i].getCarNumRate(startCross));
                            if (tmp < score) {
                                nowRoad = startCross.crossRoads[i];
                                nextRoad = null;//ccg开始上路，nextroad为null
                                isForward = true;
                                score = tmp;
                            }
                        }
                    } else if (!startCross.crossRoads[i].isDuplex) {
                        if (startCross.crossID == startCross.crossRoads[i].fromCross.crossID) {
                            float tmp = func.dijReslove(graph.graphGlobal, graphMapCross.get(startCross.crossRoads[i].toCross.crossID), graphMapCross.get(endCrossID));
                            float len = startCross.crossRoads[i].roadLength;
                            float speed = Math.min(maxSpeedofCar, startCross.crossRoads[i].speedLimitofRoad);
                            tmp += len / speed;
                            tmp = tmp * (1 + startCross.crossRoads[i].getCarNumRate(startCross));
                            if (tmp < score) {
                                nowRoad = startCross.crossRoads[i];
                                nextRoad = null;//ccg开始上路，nextroad为null。nextRoad = startCross.crossRoads[i];改
                                isForward = true;
                                score = tmp;
                            }
                        }
                    }
                }
            }
            roadOffset = nowRoad.roadLength;

        }
    }

    /*
   获取车辆前方道路信息，ForwordInfo.toPosition该车能到达的位置，如果有车阻挡，只能到达前车的尾后。
   如果当前路段行使最大距离大于偏移量，且前方没有任何车辆阻挡，返回FORWORD_NEED_THROUGH_CROSS
   */
    public ForwordInfo getTypeOfForwordCar() {
        try {
            Car[][] roadCars;
            if (isForward)
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
        } catch (Exception e) {
            System.out.println("hello");
            return null;
        }
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
    float[][][] graphGlobal; //第一维分别为距离，速度，路况拥挤
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

    /*
     * add by luzhen
     * getRoadInClockSeq()
     */
    public int getRoadInClockSeq(int roadID) {
        for (int i = 0; i < 4; ++i) {
            if (crossRoads[i] == null) continue;
            if (roadID == crossRoads[i].roadID) {
                return i;
            }
        }
        System.out.println("getRoadInClockSeq() error...");
        return -1;//error
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
    int carsNumLimit = 1000;
    int backTime;
    int MTime;
    HashSet<Integer> carOnRoadHash;
    ArrayList<Car> cars;
    ArrayList<Car> priCars;
    ArrayList<Car> presetPreTimeCars;
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
        this.carOnRoadHash = new HashSet<>();

    }

    public boolean DispatchOneTimePiece() {
        //新时间片更新路网矩阵，进行负载均衡
        updateGraph();
        //更新上个时间片留下的预置车辆
        updateProPresetCar();

        methodCholce();

        markAllCars();
        //路上车进行集中选路
        //先发上个时间片留下的预置车且优先车
        runPriFirstAndProPresetCar();
        //nextRoadChoiceOnRoad();
        if (dispatchWaitCar()) {
            backTime--;
            runPriFirstAndProPresetCar();
            runStartCar();
            checkCar();
            System.out.println("time:" + MTime);
            MTime++;
            return true;
        }
        return false;
    }

    private void updateProPresetCar() {
        this.presetPreTimeCars = new ArrayList<>();
        for (Car car : cars) {
            if (!car.isFinish && car.isPresetCar && car.ProPresetCarCount != 0 && car.isStart == 0) {
                presetPreTimeCars.add(car);
            }
        }
        Collections.sort(presetPreTimeCars, new Comparator<Car>() {
            @Override
            public int compare(Car o1, Car o2) {
                if (o1 != null && o2 != null) {
                    if (o2.ProPresetCarCount != o1.ProPresetCarCount)
                        return o2.ProPresetCarCount - o1.ProPresetCarCount;
                    else
                        return o1.carID - o2.carID;
                }
                return 0;
            }
        });
    }

    private void runPriFirstAndProPresetCar() {
        //发上个时间片留下的预置且优先车辆
        driveProPresetCars();
        //发剩下的优先车辆
        driveAllStartPriorityCars();
    }

    private void driveProPresetCars() {
        Iterator<Car> iterator = presetPreTimeCars.iterator();
        while (iterator.hasNext()) {
            Car car = iterator.next();
            if (car.isPriorityCar) {
                if (runPriStartCar(car)) {
                    iterator.remove();
                    priCars.remove(car);
                }
            }
        }
    }

    private void updateGraph() {
        Graph graph = cars.get(0).graph;
        //更新路网路口
        for (int j = 0; j < roads.size(); j++) {
            Road road = roads.get(j);
            float rate = road.getCarNumRate(road.fromCross);
            graph.graphGlobal[2][graph.graphMapCross.get(roads.get(j).fromCross.crossID)]
                    [graph.graphMapCross.get(roads.get(j).toCross.crossID)] = rate;

            if (roads.get(j).isDuplex) {
                graph.graphGlobal[2][graph.graphMapCross.get(roads.get(j).toCross.crossID)]
                        [graph.graphMapCross.get(roads.get(j).fromCross.crossID)] = rate;

            }
        }

    }

    private void nextRoadChoiceOnRoad() {
        for (int i = 0; i < cars.size(); i++) {
            Car car = cars.get(i);
            if (!car.isFinish && car.isStart == Car.GONE) {
                car.getNxtChoice(func, carNumInRoad);
            }
        }
    }

    private void methodCholce() {
        //路上车辆控制逻辑
        if (carNumInRoad < 2000) carsNumLimit = 2000;
        carsNumLimit += 20;
        if (carNumInRoad > 4500) carsNumLimit = 4500;

    }

    public void dispatchGoBack(CopyData originCopyData) {
        CopyData copyData = func.copyData(originCopyData.cars_copy, originCopyData.roads_copy, originCopyData.crosses_copy, originCopyData.roadHashMap_copy, originCopyData.carHashMap_copy,
                originCopyData.crossHashMap_copy, originCopyData.MTime, originCopyData.carNumInRoad, originCopyData.carsNumLimit);
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
            this.carsNumLimit = (int) (copyData.carsNumLimit - 500);
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
                        if (road.carOnRoad[i][j].flag == Car.WAIT_STATUS)
                            System.out.println("wrong");
                        allCarsNum++;
                        if (carsOnRoadHashMap.containsKey(road.carOnRoad[i][j].carID))
                            System.out.println("wrong1");
                        else
                            carsOnRoadHashMap.put(road.carOnRoad[i][j].carID, new CarPos(road.roadID, i, j));
                    }
                    if (road.isDuplex && road.carOnDuplexRoad[i][j] != null) {
                        if (road.carOnDuplexRoad[i][j].flag == Car.WAIT_STATUS)
                            System.out.println("wrong");
                        allCarsNum++;
                        if (carsOnRoadHashMap.containsKey(road.carOnDuplexRoad[i][j].carID))
                            System.out.println("wrong2");
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
    private boolean initialCrossRoadMap(Cross cross, HashMap<Pair<Integer, Integer>, PriorityQueue<Car>> crossRoadMap) {
        boolean isNowCrossNeedDispatch = false;
        for (Road road : cross.sortedCrossRoad) {
            Car[][] carRoad = road.getCarArray(cross);//根据出路口获取车辆数组
            PriorityQueue<Car> theTopPriorityCars = new PriorityQueue<>();
            if (carRoad == null) {
                crossRoadMap.put(new Pair<>(cross.crossID, road.roadID), theTopPriorityCars);
            } else {
                for (int channel = 0; channel < road.channelCount; channel++) {
                    for (int offset = 0; offset < road.roadLength; offset++) {
                        if (carRoad[channel][offset] != null && carRoad[channel][offset].flag == Car.WAIT_STATUS) {
                            //TODO：初始化选路
                            theTopPriorityCars.add(carRoad[channel][offset]);
                            isNowCrossNeedDispatch = true;
                            break;
                        }
                    }
                }
                if (theTopPriorityCars.peek() != null)
                    theTopPriorityCars.peek().getNxtChoice(func, carNumInRoad);

                crossRoadMap.put(new Pair<>(cross.crossID, road.roadID), theTopPriorityCars);
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
    private boolean putNextTopPriorityCarIntoHashMap(Cross cross, Road road, Car[][] carRoad, int lastCarChannel, int lastCarOffset,
                                                     HashMap<Pair<Integer, Integer>, PriorityQueue<Car>> crossRoadMap) {
        PriorityQueue<Car> priorityCar = crossRoadMap.get(new Pair<>(cross.crossID, road.roadID));
        for (int length = lastCarOffset; length < road.roadLength; ++length) {
            if (carRoad[lastCarChannel][length] != null && carRoad[lastCarChannel][length].flag == Car.WAIT_STATUS) {
                //TODO：初始化选路
                priorityCar.add(carRoad[lastCarChannel][length]);
                return true;
            }
        }
        return !priorityCar.isEmpty();
    }


    /*
     * add by luzhen
     * dispatchWaitCar()
     */
    private boolean dispatchWaitCar() {

        if (crosses.size() == 0)
            return true;
        HashMap<Integer, Boolean> crossMap = new HashMap<>();
        HashMap<Pair<Integer, Integer>, PriorityQueue<Car>> crossRoadMap = new HashMap<>();
        for (Cross cross : crosses)
            crossMap.put(cross.crossID, !initialCrossRoadMap(cross, crossRoadMap));

        while (true) {
            boolean bAllCrossesDispatch = false;
            countMove = 0;
            for (Cross cross : crosses) {
                if (crossMap.get(cross.crossID)) {//该路口调度结束
                    continue;
                }
                ArrayList<Road> sortRoads = cross.sortedCrossRoad;
                for (Road road : sortRoads) {//开始调度一个路口
                    if (crossRoadMap.get(new Pair<>(cross.crossID, road.roadID)).size() == 0)//该条路调度结束
                        continue;
                    Car[][] carRoad = road.getCarArray(cross);//根据出路口获取车辆数组
                    //如果是单向道，该出路口没有车道（这里在initialCrossRoadMap()函数已经考虑到了，自然时调度结束true）
                    if (carRoad == null)
                        continue;

                    while (crossRoadMap.get(new Pair<>(cross.crossID, road.roadID)).peek() != null) {
                        int tempChannel, tempOffset;
                        boolean tempForward;
                        Car topPriorityCar = crossRoadMap.get(new Pair<>(cross.crossID, road.roadID)).peek();//寻找到该车道第一优先级的第一辆车
                        tempChannel = topPriorityCar.nowRoadChannel;//保留topCar的初始位置channel信息
                        tempOffset = topPriorityCar.roadOffset;//保留topCar的初始位置offset信息
                        tempForward = topPriorityCar.isForward;
                        ForwordInfo forwordInfo = topPriorityCar.getTypeOfForwordCar();
                        if (forwordInfo.forwardStatus == Car.FORWORD_NEED_THROUGH_CROSS) {
                            if ((cross.crossID == topPriorityCar.endCrossID && !topPriorityCar.isPresetCar) ||
                                    (cross.crossID == topPriorityCar.endCrossID && topPriorityCar.isPresetCar &&
                                            topPriorityCar.presetRoadCount == topPriorityCar.roadChoiceList.size())) {
                                if (!topPriorityCar.getDirectionResult(crossRoadMap)) {//发生了冲突
                                    break;
                                }
                                topPriorityCar.getCarArray()[topPriorityCar.nowRoadChannel][topPriorityCar.roadOffset] = null;
                                topPriorityCar.isFinishedAtTimePiece = true;
                                topPriorityCar.isFinish = true;
                                topPriorityCar.nowRoad = null;
                                topPriorityCar.nextRoad = null;
                                topPriorityCar.flag = Car.END_STATUS;
                                topPriorityCar.maxTimeCount = MTime;
                                crossRoadMap.get(new Pair<>(cross.crossID, road.roadID)).poll();
                                carNumInRoad--;
                                carOnRoadHash.remove(topPriorityCar.carID);
                                countMove++;
                            } else {
                                topPriorityCar.getNxtChoice(func, carNumInRoad);
                                if (!topPriorityCar.getDirectionResult(crossRoadMap)) {//发生了冲突
                                    break;
                                }
                                int stepCounts = Math.min(topPriorityCar.maxSpeedofCar, topPriorityCar.nextRoad.speedLimitofRoad) - topPriorityCar.roadOffset;
                                stepCounts = stepCounts > 0 ? stepCounts : 0;
                                if (!setCarStatusWhenThroughCross(topPriorityCar, cross, stepCounts))
                                    break;
                                else
                                    crossRoadMap.get(new Pair<>(cross.crossID, road.roadID)).poll();
                            }
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
                            putNextTopPriorityCarIntoHashMap(cross, road, carRoad, tempChannel, tempOffset, crossRoadMap);
                            //driveAllStartPriorityCars();
                            driveSingleStartPriorityCars(road, tempForward);
                        } else {
                            System.out.println("error");
                        }
                    }
                }//end for sorted roads
                int flag = 1;
                for (Road road : sortRoads) {
                    if (crossRoadMap.get(new Pair<>(cross.crossID, road.roadID)).size() != 0) {//存在没有调度结束的路
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
            if (bAllCrossesDispatch) {
                return true;
            }
            if (countMove == 0 && carNumInRoad != 0) {
                //System.out.println("go back");
                return false;
            }
        }
    }

    private void driveSingleStartPriorityCars(Road road, boolean tempForward) {
        Iterator<Car> iteratorPreset = presetPreTimeCars.iterator();
        while (iteratorPreset.hasNext()) {
            Car car = iteratorPreset.next();
            if (car.isPriorityCar) {
                if (runPriStartCar(car)) {
                    iteratorPreset.remove();
                    priCars.remove(car);
                }
            }
        }

        Iterator<Car> iterator = priCars.iterator();
        while (iterator.hasNext()) {
            Car car = iterator.next();
            if (car.nowRoad.roadID == road.roadID && car.isStart == 1 && car.isForward == tempForward) {
                if (runPriStartCar(car))
                    iterator.remove();
            }
        }
    }

    private boolean runPriStartCar(Car priorityCar) {
        Car[][] carOnRoad = priorityCar.getCarArray();
        for (int j = 0; j < priorityCar.nowRoad.channelCount; j++) {
            //该车道可以进
            if (carOnRoad[j][priorityCar.nowRoad.roadLength - 1] == null) {
                priorityCar.nowRoadChannel = j;
                ForwordInfo forwordInfo = priorityCar.getTypeOfForwordCar();
                //如果前方没有车辆或者前方为终止车辆
                if (forwordInfo.forwardStatus == Car.FORWORD_HAS_NO_CAR || forwordInfo.forwardStatus == Car.FORWORD_HAS_END_CAR) {
                    priorityCar.isStart = 2;
                    priorityCar.flag = Car.END_STATUS;
                    priorityCar.isFinishedAtTimePiece = true;
                    carNumInRoad++;
                    priorityCar.realStartTime = MTime;
                    carOnRoadHash.add(priorityCar.carID);
                    priorityCar.roadOffset = forwordInfo.toPosition;
                    carOnRoad[j][priorityCar.roadOffset] = priorityCar;
                    //TODO:如果是预置车，清空计数
                    if (priorityCar.isPresetCar) priorityCar.ProPresetCarCount = 0;
                    if (!priorityCar.isPresetCar)
                        priorityCar.roadChoiceList.add(priorityCar.nowRoad.roadID);
                    return true;
                }
                //前方有等待车辆
                else if (forwordInfo.forwardStatus == Car.FORWORD_HAS_WAIT_CAR) {
                    return false;
                }
            } else if (carOnRoad[j][priorityCar.nowRoad.roadLength - 1].flag == Car.WAIT_STATUS) {
                //该车道最后位置为等待车辆
                return false;
            }
        }
        return false;
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
                            //更新车的新路况信息  roadChoiceList;isForward;isFinishedAtTimePiece
                            int newOffset = car.nextRoad.roadLength - eachSetp;
                            car.flag = Car.END_STATUS;
                            car.nowRoad = car.nextRoad;//更新车的当前路径为下一条路径
                            car.nextRoad = null;  //未知状态,下一条路径需要重新选择
                            car.roadOffset = newOffset;
                            car.nowRoadChannel = i;
                            nextRoad[i][newOffset] = car;
                            if (car.nowRoad.fromCross.crossID == nowCross.crossID) {//
                                car.isForward = true;
                            } else {
                                car.isForward = false;
                            }
                            car.isFinishedAtTimePiece = true;//是否一个时间片完成调度
                            countMove++;
                            if (!car.isPresetCar) car.roadChoiceList.add(car.nowRoad.roadID);//在路径列表中记录这条路
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
                if (!car.isPresetCar) car.roadChoiceList.add(car.nextRoad.roadID);//在路径列表中记录这条路
                car.nowRoad = car.nextRoad;//更新车的当前路径为下一条路径
                car.nextRoad = null;  //未知状态,下一条路径需要重新选择
                car.roadOffset = newOffset;
                car.nowRoadChannel = i;
                nextRoad[car.nowRoadChannel][newOffset] = car;
                car.isForward = car.nowRoad.fromCross.crossID == nowCross.crossID;
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


    private void markAllCars() {
        int thisCarStartNum = 0;
        priCars = new ArrayList<>();
        for (Car car : cars) {
            if (car.isStart == Car.GONE && !car.isFinish)
                car.flag = Car.NO_STATUS;
            if (!car.isFinish && car.isStart == Car.NO_GO && car.minStartTime == MTime && car.isPriorityCar) {
                if (carNumInRoad + thisCarStartNum < carsNumLimit || car.isPresetCar) {
                    car.isStart = Car.GOING;
                    car.getFistChoice(func);
                    car.minStartTime = MTime;
                    car.realStartTime = MTime;
                    priCars.add(car);
                    thisCarStartNum++;
                } else {
                    car.minStartTime++;
                    car.realStartTime++;
                }
            }
        }
        for (Road road : roads) {

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
        ForwordInfo forwordInfo = car.getTypeOfForwordCar();
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

    public void driveAllStartPriorityCars() {
        for (Road road : roads) {
            Iterator<Car> iterator = priCars.iterator();
            while (iterator.hasNext()) {
                Car car = iterator.next();
                if (car.nowRoad.roadID == road.roadID && car.isStart == 1) {
                    if (runPriStartCar(car))
                        iterator.remove();
                }
            }
        }
    }

    //发普通车辆
    public void runStartCar() {

        ArrayList<Car> limitSpeed = new ArrayList<>();
        for (int i = 0; i < cars.size(); i++) {
            if (cars.get(i).minStartTime == MTime && cars.get(i).isStart == 0 && !cars.get(i).isPriorityCar && !cars.get(i).isPresetCar) {
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
        int thisTimeStartCarNum = carsNumLimit - carNumInRoad;
        //预置车辆上路(非优先车辆)
        for (int i = 0; i < cars.size(); i++) {
            if (cars.get(i).isPresetCar && cars.get(i).minStartTime == MTime && cars.get(i).isStart == 0 && !cars.get(i).isPriorityCar) {
                cars.get(i).isStart = 1; // 上路出发
                cars.get(i).minStartTime = MTime;
                cars.get(i).realStartTime = MTime;
                cars.get(i).getFistChoice(func);
                thisTimeStartCarNum--;
            }
        }

        if (thisTimeStartCarNum > 0) {
            for (int k = 0; k < thisTimeStartCarNum; k++) {
                if (k >= limitSpeed.size())
                    break;
                limitSpeed.get(0).isStart = 1; // 上路出发
                limitSpeed.get(0).realStartTime = MTime;
                limitSpeed.get(0).getFistChoice(func);//得到首次的选择，并更新car信息
                limitSpeed.remove(0);
                thisTimeStartCarNum--;
            }
        }
        for (Car car : limitSpeed) {
            car.minStartTime++;
            car.realStartTime++;
        }

        //先发上个时间片留下来的预置车
        for (Car car : presetPreTimeCars) {
            if (car.isStart == 1 && !car.isFinish) {
                Car[][] carOnRoad = car.getCarArray();
                for (int i = 0; i < car.nowRoad.channelCount; i++) {
                    if (carOnRoad[i][car.nowRoad.roadLength - 1] == null) {
                        car.isStart = 2;
                        car.nowRoadChannel = i;
                        car.realStartTime = MTime;
                        car.flag = Car.END_STATUS;
                        car.isFinishedAtTimePiece = true;
                        carNumInRoad++;
                        carOnRoadHash.add(car.carID);
                        ForwordInfo forwordInfo = car.getTypeOfForwordCar();
                        car.roadOffset = forwordInfo.toPosition;
                        carOnRoad[i][car.roadOffset] = car;
                        car.ProPresetCarCount = 0;
                        break;
                    }
                }
                if (car.isStart == 1) {
                    car.ProPresetCarCount++;
                    car.isStart = 0;
                    car.minStartTime++;
                    car.realStartTime++;
                }
            }
        }

        //发不是留下来预置车的普通车
        for (Car car : cars) {
            if (car.isStart == 1 && !car.isFinish) {
                Car[][] carOnRoad = car.getCarArray();
                for (int i = 0; i < car.nowRoad.channelCount; i++) {
                    if (carOnRoad[i][car.nowRoad.roadLength - 1] == null) {
                        car.isStart = 2;
                        car.nowRoadChannel = i;
                        car.realStartTime = MTime;
                        car.flag = Car.END_STATUS;
                        car.isFinishedAtTimePiece = true;
                        carNumInRoad++;
                        carOnRoadHash.add(car.carID);
                        ForwordInfo forwordInfo = car.getTypeOfForwordCar();
                        car.roadOffset = forwordInfo.toPosition;
                        carOnRoad[i][car.roadOffset] = car;
                        if (!car.isPresetCar) car.roadChoiceList.add(car.nowRoad.roadID);
                        break;
                    }
                }
                if (car.isStart == 1) {
                    if (car.isPresetCar) car.ProPresetCarCount++;
                    car.isStart = 0;
                    car.minStartTime++;
                    car.realStartTime++;
                }
            }
        }

        //更新优先车的东西
        for (Car car : priCars) {
            if (car.isPresetCar) continue;
            car.isStart = 0;
            car.minStartTime++;
            car.realStartTime++;
        }
    }
}
