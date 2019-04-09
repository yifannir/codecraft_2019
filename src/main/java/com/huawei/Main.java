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
        LinkedList<CopyData> copyDatas = new LinkedList<>();
        while (!func.allCarFinished(dispatchCenter.cars)) {
            if (copyDatas.size() == 8) {
                copyDatas.pollFirst();
            }
            copyDatas.offerLast(dispatchCenter.CopyDispatchTimePieceData());
            while (true) {
                if (dispatchCenter.DispatchOneTimePiece()) {
                    break;
                } else {
                    dispatchCenter.dispatchGoBack(copyDatas);
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
        score = getScore(dispatchCenter.cars, dispatchCenter.mTime, priMinPlanTime, alpha);
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


class Car implements Cloneable {
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
    boolean isPriorityCar;
    boolean isPresetCar;
    int presetRoadCount;
    int startCrossID;
    int endCrossID;
    int carID;
    int carTurnTime;
    Cross startCross;
    Cross endCross;
    int maxSpeedofCar;
    int minStartTime;
    int minTimeCount;
    int maxTimeCount;
    int backupStartTime;
    static Comparator<Car> startCarComparator = (o1, o2) -> {
        if (o1.isPriorityCar ^ o2.isPriorityCar) {
            return o1.isPriorityCar ? -1 : 1;
        } else {
            if (o1.realStartTime == o2.realStartTime)
                return o1.carID - o2.carID;
            else
                return o1.realStartTime - o2.realStartTime;
        }
    };

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

    public Car(int carID, int fromCrossID, int toCrossID, int maxSpeedofCar, int planTime, int pri, int preset) {
        this.carID = carID;
        this.startCrossID = fromCrossID;
        this.endCrossID = toCrossID;
        this.maxSpeedofCar = maxSpeedofCar;
        this.minStartTime = planTime;
        this.realStartTime = planTime;
        this.minTimeCount = planTime;
        this.isPriorityCar = pri == 1;
        this.isPresetCar = preset == 1;
        this.kasi = false;
        this.presetRoadCount = 0;
        this.backupStartTime = planTime;

    }

    int getRealSpeedOnNowRoad() {
        if (nowRoad == null)
            return GO_WRONG;
        return maxSpeedofCar < nowRoad.speedLimitofRoad ? maxSpeedofCar : nowRoad.speedLimitofRoad;
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


    public void getNextChoice(Func func, int carNumInRoad, Random random, int MTime) {
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
                carTurnTime = MTime;
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
			carTurnTime = MTime;
            int yu = 88;
            if (carNumInRoad > 4000) yu = 88;
            if (kasi || random.nextInt(100) % yu == 0) {
                int tmp = random.nextInt(4);
                nextRoad = cross.sortedCrossRoad.get(tmp % cross.sortedCrossRoad.size());
                while (nextRoad.roadID == nowRoad.roadID || (!nextRoad.isDuplex && (nextRoad.fromCross.crossID != cross.crossID))) {
                    tmp = random.nextInt(4);
                    nextRoad = cross.sortedCrossRoad.get(tmp % cross.sortedCrossRoad.size());
                }
                return;
            }
            //最短路选择法
            double score = 0x3f3f3f3f;//= func.dijReslove(car.graph,graphMapCross.get(cross.crossID),graphMapCross.get(car.endCrossID),pathList);
            for (int i = 0; i < 4; i++) {
                if (cross.crossRoads[i] != null && cross.crossRoads[i].roadID != nowRoad.roadID) { //道路不为空
                    // 双车道时，起点或者终点为所选的第二个节点即可，单车道道时必须时终点为第二个节点。
                    if (cross.crossRoads[i].isDuplex) {
                        if (cross.crossRoads[i].toCross.crossID == cross.crossID) {
                            double tmp = graph.dist[graphMapCross.get(cross.crossRoads[i].fromCross.crossID)][graphMapCross.get(endCrossID)];
                            //float tmp = func.dijReslove(graph.adjMat, graphMapCross.get(cross.crossRoads[i].fromCross.crossID), graphMapCross.get(endCrossID));
                            float len = cross.crossRoads[i].roadLength;
                            float speed = Math.min(maxSpeedofCar, cross.crossRoads[i].speedLimitofRoad);
                            tmp += len / speed;
                            //tmp += len;
                            //TODO：调参位置 加上拥堵惩罚
                            int ap = 1;
                            if (carNumInRoad < 3500) ap = 0;
                            tmp = tmp * (1 + ap * cross.crossRoads[i].getCarNumRate(cross));// + 10/(float)cross.crossRoads[i].channelCount;

                            if (tmp < score) {
                                nextRoad = cross.crossRoads[i];
                                score = tmp;
                            }
                        } else if (cross.crossRoads[i].fromCross.crossID == cross.crossID) {
                            double tmp = graph.dist[graphMapCross.get(cross.crossRoads[i].toCross.crossID)][graphMapCross.get(endCrossID)];
                            //float tmp = func.dijReslove(graph.adjMat, graphMapCross.get(cross.crossRoads[i].toCross.crossID), graphMapCross.get(endCrossID));
                            float len = cross.crossRoads[i].roadLength;
                            float speed = Math.min(maxSpeedofCar, cross.crossRoads[i].speedLimitofRoad);
                            tmp += len / speed;

                            int ap = 1;
                            if (carNumInRoad < 3500) ap = 0;
                            tmp = tmp * (1 + ap * cross.crossRoads[i].getCarNumRate(cross));
                            if (tmp < score) {
                                nextRoad = cross.crossRoads[i];
                                score = tmp;
                            }
                        }
                    } else if (!cross.crossRoads[i].isDuplex) {
                        if (cross.crossRoads[i].fromCross.crossID == cross.crossID) {
                            double tmp = graph.dist[graphMapCross.get(cross.crossRoads[i].toCross.crossID)][graphMapCross.get(endCrossID)];

//                            float tmp = func.dijReslove(graph.adjMat, graphMapCross.get(cross.crossRoads[i].toCross.crossID), graphMapCross.get(endCrossID));
                            float len = cross.crossRoads[i].roadLength;
                            float speed = Math.min(maxSpeedofCar, cross.crossRoads[i].speedLimitofRoad);
                            tmp += len / speed;
                            int ap = 1;
                            if (carNumInRoad < 3500) ap = 0;
                            tmp = tmp * (1 + ap * cross.crossRoads[i].getCarNumRate(cross));// + 10/(float)cross.crossRoads[i].channelCount;
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
        if (nowRoad != null) return;
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
            double score = 0x3f3f3f3f;
            for (int i = 0; i < 4; i++) {
                if (startCross.crossRoads[i] != null) { //道路不为空
                    // 双车道时，起点或者终点为所选的第二个节点即可，单车道道时必须时终点为第二个节点。
                    if (startCross.crossRoads[i].isDuplex) {
                        if (startCross.crossID == startCross.crossRoads[i].toCross.crossID) {
                            double tmp = graph.dist[graphMapCross.get(startCross.crossRoads[i].fromCross.crossID)][graphMapCross.get(endCrossID)];
                            //float tmp = func.dijReslove(graph.adjMat, graphMapCross.get(startCross.crossRoads[i].fromCross.crossID), graphMapCross.get(endCrossID));
                            float len = startCross.crossRoads[i].roadLength;
                            float speed = Math.min(maxSpeedofCar, startCross.crossRoads[i].speedLimitofRoad);
                            tmp += len / speed;
                            //tmp += len;
                            tmp = tmp * (1 + startCross.crossRoads[i].getCarNumRate(startCross));
                            if (tmp < score) {
                                nowRoad = startCross.crossRoads[i];
                                nextRoad = null;//ccg开始上路，nextroad为null
                                isForward = false;
                                score = tmp;
                            }
                        } else if (startCross.crossID == startCross.crossRoads[i].fromCross.crossID) {
                            double tmp = graph.dist[graphMapCross.get(startCross.crossRoads[i].toCross.crossID)][graphMapCross.get(endCrossID)];
                            //float tmp = func.dijReslove(graph.adjMat, graphMapCross.get(startCross.crossRoads[i].toCross.crossID), graphMapCross.get(endCrossID));
                            float len = startCross.crossRoads[i].roadLength;
                            float speed = Math.min(maxSpeedofCar, startCross.crossRoads[i].speedLimitofRoad);
                            tmp += len / speed;
                            //tmp += len;
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
                            double tmp = graph.dist[graphMapCross.get(startCross.crossRoads[i].toCross.crossID)][graphMapCross.get(endCrossID)];
                            //float tmp = func.dijReslove(graph.adjMat, graphMapCross.get(startCross.crossRoads[i].toCross.crossID), graphMapCross.get(endCrossID));
                            float len = startCross.crossRoads[i].roadLength;
                            float speed = Math.min(maxSpeedofCar, startCross.crossRoads[i].speedLimitofRoad);
                            tmp += len / speed;
                            //tmp += len;
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
}

class ForwordInfo {
    int forwardStatus;
    int toPosition;

    ForwordInfo(int forwardStatus, int toPosition) {
        this.forwardStatus = forwardStatus;
        this.toPosition = toPosition;
    }
}

class Road implements Cloneable {
    int roadID;
    int roadLength;
    int speedLimitofRoad;
    int channelCount;//车道数
    Cross fromCross;
    Cross toCross;
    TreeSet<Car> forwordStartCars;
    TreeSet<Car> endStartCars;
    public Road clone() {
        try {
            return (Road) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    boolean isDuplex;// 是否是单双道
    Car[][] carOnRoad;
    Car[][] carOnDuplexRoad;

    public Road(int roadId) {
        this.roadID = roadId;
        forwordStartCars = new TreeSet<>(Car.startCarComparator);
        endStartCars = new TreeSet<>(Car.startCarComparator);
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
    float[][] adjMat;
    double dist[][];
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
    int carsNumLimit = 10000;
    int backTime;
    int mTime;
    HashSet<Integer> carOnRoadHash;
    ArrayList<Car> cars;
    ArrayList<Road> roads;
    ArrayList<Cross> crosses;
    HashMap<Integer, Road> roadHashMap;
    HashMap<Integer, Car> carHashMap;
    HashMap<Integer, Cross> crossHashMap;
    HashMap<Pair<Integer, Integer>, PriorityQueue<Car>> crossRoadMap;
    Func func;
    Random random = new Random(5000);

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
        updateGraph();
        methodCholce();
        markAllCars();
        if (dispatchWaitCar()) {
            backTime--;
            runStartCars();
            checkCar();
            System.out.println("time:" + mTime);
            mTime++;
            return true;
        }
        return false;
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
        int a = 1;
        int b = 1;
        if (carNumInRoad < 5000) {
            a = 200;
            b = 2;
        } else {
            a = 200;
            b = 2;
        }
        if (carNumInRoad <4000){
            a=0;
            b=0;
        }
        graph.adjMat = new float[graph.graphGlobal[0].length][graph.graphGlobal[0].length];
        graph.dist = new double[graph.adjMat.length][graph.adjMat.length];
        float[][] adjMat = graph.adjMat;
        for (int i = 0; i < adjMat.length; i++) {
            for (int j = 0; j < adjMat.length; j++) {
                adjMat[i][j] = (graph.graphGlobal[0][i][j]) * (1 + a * graph.graphGlobal[2][i][j]) / (1 + b * graph.graphGlobal[3][i][j]);
                //adjMat[i][j] = graph.graphGlobal[0][i][j]/(1+2*graph.graphGlobal[3][i][j])+1*graph.graphGlobal[2][i][j];
                graph.dist[i][j] = adjMat[i][j];
            }
        }
        func.floyd(graph.dist);
    }

    private void methodCholce() {
        carsNumLimit = 6000;
        if (mTime > 750) carsNumLimit = 8000;

        if (mTime > 850) carsNumLimit = 80000;
    }

    public void dispatchGoBack(LinkedList<CopyData> originCopyDatas) {
        CopyData originCopyData = originCopyDatas.peek();
        while (originCopyDatas.size() > 1) {
            originCopyDatas.pollLast();
        }
        for (PriorityQueue<Car> priorityQueue : crossRoadMap.values()) {
            while(priorityQueue.size() != 0) {
                originCopyData.carHashMap_copy.get(priorityQueue.poll().carID).kasi = true;
            }
        }
        CopyData copyData = func.copyData(originCopyData.cars_copy, originCopyData.roads_copy, originCopyData.crosses_copy, originCopyData.roadHashMap_copy, originCopyData.carHashMap_copy,
                originCopyData.crossHashMap_copy, originCopyData.MTime, originCopyData.carNumInRoad, originCopyData.carsNumLimit);
        this.mTime = copyData.MTime;
        this.carNumInRoad = copyData.carNumInRoad;
        this.cars = copyData.cars_copy;
        this.roads = copyData.roads_copy;
        this.crosses = copyData.crosses_copy;
        this.crossHashMap = copyData.crossHashMap_copy;
        this.carHashMap = copyData.carHashMap_copy;
        this.roadHashMap = copyData.roadHashMap_copy;
    }

    public CopyData CopyDispatchTimePieceData() {
        return func.copyData(cars, roads, crosses, roadHashMap, carHashMap, crossHashMap, mTime,
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
            PriorityQueue<Car> theTopPriorityCars = new PriorityQueue<>((o1, o2) -> {
                if(o1.isPriorityCar ^ o2.isPriorityCar){
                    return o1.isPriorityCar?-1:1;
                }else {
                    if(o1.roadOffset!=o2.roadOffset)
                        return o1.roadOffset - o2.roadOffset;
                    else
                        return o1.nowRoadChannel - o2.nowRoadChannel;
                }
            });
            if (carRoad == null) {
                crossRoadMap.put(new Pair<>(cross.crossID, road.roadID), theTopPriorityCars);
            } else {
                for (int channel = 0; channel < road.channelCount; channel++) {
                    for (int offset = 0; offset < road.roadLength; offset++) {
                        if (carRoad[channel][offset] != null && carRoad[channel][offset].flag == Car.WAIT_STATUS) {
                            theTopPriorityCars.add(carRoad[channel][offset]);
                            isNowCrossNeedDispatch = true;
                            break;
                        }
                    }
                }
                if (theTopPriorityCars.peek() != null)
                    theTopPriorityCars.peek().getNextChoice(func, carNumInRoad, random, mTime);

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
        driveCarInitList(true);
        if (crosses.size() == 0)
            return true;
        HashMap<Integer, Boolean> crossMap = new HashMap<>();
        crossRoadMap = new HashMap<>();
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
                for (Road road : sortRoads) {
                    if (crossRoadMap.get(new Pair<>(cross.crossID, road.roadID)).size() == 0)
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
                                topPriorityCar.maxTimeCount = mTime;
                                topPriorityCar.kasi = false;
                                crossRoadMap.get(new Pair<>(cross.crossID, road.roadID)).poll();
                                carNumInRoad--;
                                carOnRoadHash.remove(topPriorityCar.carID);
                                countMove++;
                            } else {
                                topPriorityCar.getNextChoice(func, carNumInRoad, random, mTime);
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
                                        channelNextCar.kasi = false;
                                        countMove++;
                                    }
                                }
                            }
                            putNextTopPriorityCarIntoHashMap(cross, road, carRoad, tempChannel, tempOffset, crossRoadMap);
                            if (tempForward)
                                runRoadInitList(road.forwordStartCars, true);
                            else
                                runRoadInitList(road.endStartCars, true);
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
                System.out.println("go back");
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
            car.kasi = false;
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
                            car.kasi = false;
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
                car.kasi = false;
                countMove++;
                return true;
            } catch (Exception e) {
                System.out.println("hello");
            }
        }
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
        car.kasi = false;
        countMove++;
        return true;
    }


    private void markAllCars() {
        for (Car car : cars) {
            if (car.isStart == Car.GONE && !car.isFinish)
                car.flag = Car.NO_STATUS;
            if (!car.isFinish && car.isStart == Car.NO_GO && car.realStartTime == mTime && car.isPriorityCar) {
                car.getFistChoice(func);
                car.isStart = Car.GOING;
                if (car.isForward)
                    car.nowRoad.forwordStartCars.add(car);
                else
                    car.nowRoad.endStartCars.add(car);
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
        if (car == null)
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

    private void driveCarInitList(boolean isOnlyPriority) {
        for (Road road : roads) {
            runRoadInitList(road.forwordStartCars, isOnlyPriority);
            runRoadInitList(road.endStartCars, isOnlyPriority);
        }
    }

    public void runRoadInitList(TreeSet<Car> startCars, boolean isOnlyPriority) {
        Iterator<Car> startIterator = startCars.iterator();
        while (startIterator.hasNext()) {
            Car car = startIterator.next();
            if (isOnlyPriority && !car.isPriorityCar)
                break;
            if (mTime < car.minStartTime) {
                System.out.println("driveAllStartPriorityCars is wrong");
                continue;
            }
            if (carNumInRoad <= carsNumLimit || car.isPresetCar || car.isPriorityCar) {
                if (runStartCar(car))
                    startIterator.remove();
            }
        }
    }

    private boolean runStartCar(Car car) {
        Car[][] carOnRoad = car.getCarArray();
        for (int j = 0; j < car.nowRoad.channelCount; j++) {
            if (carOnRoad[j][car.nowRoad.roadLength - 1] == null) {
                car.nowRoadChannel = j;
                ForwordInfo forwordInfo = car.getTypeOfForwordCar();
                if (forwordInfo.forwardStatus == car.FORWORD_HAS_NO_CAR || forwordInfo.forwardStatus == car.FORWORD_HAS_END_CAR) {
                    car.isStart = Car.GONE;
                    if(!car.isPresetCar)
                        car.realStartTime = mTime;
                    car.flag = car.END_STATUS;
                    car.isFinishedAtTimePiece = true;
                    carNumInRoad++;
                    carOnRoadHash.add(car.carID);
                    car.roadOffset = forwordInfo.toPosition;
                    carOnRoad[j][car.roadOffset] = car;
                    if (!car.isPresetCar) car.roadChoiceList.add(car.nowRoad.roadID);
                    return true;
                } else if (forwordInfo.forwardStatus == car.FORWORD_HAS_WAIT_CAR) {
                    return false;
                }
            } else if (carOnRoad[j][car.nowRoad.roadLength - 1].flag == car.WAIT_STATUS) {
                return false;
            }
        }
        return false;
    }

    public void runStartCars() {
        cleanAllCarsInInitList();//清理本回合没有成功出发的优先非预置车
        driveCarInitList(false);//发以前回合的预置车(本回合预置车也发，但是发不出去无妨)
        int thisTimeStartCarNum = carsNumLimit - carNumInRoad;
        for (Car car:cars) {//标记本回合的预置车
            if (car.realStartTime == mTime && car.isStart == Car.NO_GO && car.isPresetCar) {
                car.getFistChoice(func);
                car.isStart = Car.GOING;
                thisTimeStartCarNum--;
            }
        }
        TreeSet<Car> limitSpeed = new TreeSet<>((o1, o2) -> {
            if(o2.maxSpeedofCar != o1.maxSpeedofCar)
                return o2.maxSpeedofCar - o1.maxSpeedofCar;
            else {
                return o1.carID - o2.carID;
            }
        });
        //标记限速内的高速车
        for (Car car:cars) {
            if (car.realStartTime == mTime && car.isStart == Car.NO_GO &&
                    !car.isPriorityCar && !car.isPresetCar) {
                limitSpeed.add(car);
            }
        }
        while(thisTimeStartCarNum>0 && limitSpeed.size()!=0){
            Car maxSpeedCar = limitSpeed.pollFirst();
            maxSpeedCar.getFistChoice(func);
            if (maxSpeedCar.nowRoad.getCarNumRate(maxSpeedCar.startCross) < 0.65) {
                maxSpeedCar.isStart = Car.GOING; // 上路出发
                maxSpeedCar.realStartTime = mTime;
                thisTimeStartCarNum--;
            } else {
                maxSpeedCar.nowRoad = null;
                maxSpeedCar.realStartTime++;
            }
        }
        for (Car car : limitSpeed) {//不能上路的高速车下回合走
            car.realStartTime++;
        }
        for (Car car : cars) {//发本回合的预置车和普通车
            if(car.isStart == 1 && !car.isFinish && car.realStartTime == mTime){
                if(!runStartCar(car)){
                    if(car.isPresetCar){//如果发车失败，预置车放到道路发车队列中
                        if(car.isForward)
                            car.nowRoad.forwordStartCars.add(car);
                        else
                            car.nowRoad.endStartCars.add(car);
                    }else {//普通车下回合继续
                        car.isStart = 0;
                        car.realStartTime++;
                    }
                }
            }
        }
    }
    public void cleanAllCarsInInitList() {
        for (Road road : roads) {
            for (Iterator<Car> iterator = road.forwordStartCars.iterator(); iterator.hasNext(); ) {
                Car car = iterator.next();
                if (!car.isPresetCar){
                    car.realStartTime++;
                    iterator.remove();
                    car.isStart = Car.NO_GO;
                }
            }
            for (Iterator<Car> iterator = road.endStartCars.iterator(); iterator.hasNext(); ) {
                Car car = iterator.next();
                if (!car.isPresetCar){
                    car.realStartTime++;
                    iterator.remove();
                    car.isStart = Car.NO_GO;
                }
            }
        }
    }

    public int getAllStartingCarsNum() {
        int num = 0;
        for (Road road : roads) {
            num += road.forwordStartCars.size();
            num += road.endStartCars.size();
        }
        return num;
    }
}
