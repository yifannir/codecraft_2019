package com.huawei;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

class DispatchCenter {
    int priorityArrivedLastTime = 0;
    int priorityFirstStartTime = -1;
    int allDispatcherTime = 0;
    int priorityDispatcherTime = 0;
    int countMove = 0;
    int carNumInRoad;
    int MTime;
    HashSet<Integer> carOnRoadHash;
    ArrayList<Car> cars;
    ArrayList<Road> roads;
    ArrayList<Car> priCars;
    ArrayList<Cross> crosses;//升序排列的路口
    HashMap<Integer, Road> roadHashMap;
    HashMap<Integer, Car> carHashMap;
    HashMap<Integer, Cross> crossHashMap;
    Func func;

    ArrayList<RoadCarInfo> roadCarInfos = new ArrayList<>();

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
        MTime++;
        markAllCars();
        if (dispatchWaitCar()) {
            driveCarInitList(false);
            checkCar();
            return true;
        }
        return false;
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


    private void recordRoadCarInfo(int time, ArrayList<RoadCarInfo> roadCarInfos) {
        roadCarInfos.clear();
        for (Road road : roads) {
            for (int i = 0; i < road.channelCount; ++i) {
                for (int j = 0; j < road.roadLength; ++j) {
                    if (road.carOnRoad[i][j] != null) {
                        RoadCarInfo roadCarInfo = new RoadCarInfo();
                        roadCarInfo.carID = road.carOnRoad[i][j].carID;
                        roadCarInfo.roadID = road.roadID;
                        roadCarInfo.offset = j;
                        roadCarInfo.channel = i;
                        roadCarInfos.add(roadCarInfo);
                    }
                    if (road.isDuplex && road.carOnDuplexRoad[i][j] != null) {
                        RoadCarInfo roadCarInfo = new RoadCarInfo();
                        roadCarInfo.carID = road.carOnDuplexRoad[i][j].carID;
                        roadCarInfo.roadID = road.roadID;
                        roadCarInfo.offset = j;
                        roadCarInfo.channel = i;
                        roadCarInfos.add(roadCarInfo);
                    }
                }
            }
        }//end for Roads
        //将time时刻的路上车辆信息写入到time.txt文件中
        String logBasePath = "D:\\judgeLog\\";
        String logFileName = String.valueOf(time) + ".txt";
        File file = new File(logBasePath + logBasePath);
        if (file.exists() && file.isFile())
            file.delete();
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(logBasePath + logFileName));
            for (RoadCarInfo roadCarInfo : roadCarInfos) {
                String strLine = "(" + roadCarInfo.carID + ", " + roadCarInfo.roadID + ", " + roadCarInfo.channel + ", " + roadCarInfo.offset + ")";
                bw.write(strLine);
                bw.newLine();
                bw.flush();
            }
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /*
     * add by luzhen
     * initialCrossRoadMap()
     * find the first WAIT_STATUS car in every road 返回falsed代表这条路不需要调度，返回true代表这条路口需要调度了
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
                            theTopPriorityCars.add(carRoad[channel][offset]);
                            isNowCrossNeedDispatch = true;
                            break;
                        }
                    }
                }
                if (theTopPriorityCars.peek() != null)
                    theTopPriorityCars.peek().getNextChoice(cross, roadHashMap);
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
                priorityCar.add(carRoad[lastCarChannel][length]);
                return true;
            }
        }
        return !priorityCar.isEmpty();
    }

    private boolean dispatchWaitCar() {
        driveCarInitList(true);
        HashMap<Integer, Boolean> crossMap = new HashMap<Integer, Boolean>();
        HashMap<Pair<Integer, Integer>, PriorityQueue<Car>> crossRoadMap = new HashMap<>();
        for (Cross cross : crosses)
            crossMap.put(cross.crossID, !initialCrossRoadMap(cross, crossRoadMap));
        while (true) {
            boolean bAllCrossesDispatch = true;
            countMove = 0;
            for (Cross cross : crosses) {
                if (crossMap.get(cross.crossID)) {
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
                    int tempChannel, tempOffset;
                    while (crossRoadMap.get(new Pair<>(cross.crossID, road.roadID)).peek() != null) {
                        Car topPriorityCar = crossRoadMap.get(new Pair<>(cross.crossID, road.roadID)).peek();//寻找到该车道第一优先级的第一辆车
                        tempChannel = topPriorityCar.nowRoadChannel;//保留topCar的初始位置channel信息
                        tempOffset = topPriorityCar.roadOffset;//保留topCar的初始位置offset信息
                        ForwordInfo forwordInfo = topPriorityCar.getTypeOfForwordCar();
                        if (forwordInfo.forwardStatus == Car.FORWORD_NEED_THROUGH_CROSS) {
                            if ((cross.crossID == topPriorityCar.endCrossID && !topPriorityCar.isPresetCar) ||
                                    (cross.crossID == topPriorityCar.endCrossID && topPriorityCar.isPresetCar &&
                                            topPriorityCar.countChoice == topPriorityCar.roadChoiceList.size())) {
                                if (!topPriorityCar.getDirectionResult(crossRoadMap)) {
                                    break;
                                }
                                topPriorityCar.getCarArray()[topPriorityCar.nowRoadChannel][topPriorityCar.roadOffset] = null;
                                topPriorityCar.isFinishedAtTimePiece = true;
                                topPriorityCar.isFinish = true;
                                topPriorityCar.nowRoad = null;
                                topPriorityCar.nextRoad = null;
                                topPriorityCar.flag = Car.END_STATUS;
                                crossRoadMap.get(new Pair<>(cross.crossID, road.roadID)).poll();
                                carNumInRoad--;
                                carOnRoadHash.remove(topPriorityCar.carID);
                                allDispatcherTime = allDispatcherTime + MTime - topPriorityCar.planTime;
                                if (topPriorityCar.isPriorityCar)
                                    priorityDispatcherTime = priorityDispatcherTime + MTime - topPriorityCar.planTime;
                                if (topPriorityCar.isPriorityCar && MTime > priorityArrivedLastTime)
                                    priorityArrivedLastTime = MTime;
                                countMove++;
                            } else {
                                topPriorityCar.getNextChoice(cross, roadHashMap);
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
                            Iterator<Car> iterator = road.initCarList.iterator();
                            int crossID = (road.fromCross.crossID == cross.crossID ?
                                    road.toCross.crossID : road.fromCross.crossID);
                            while (iterator.hasNext()) {
                                Car car = iterator.next();
                                car.getFistChoice(roadHashMap);
                                if (!car.isPriorityCar)
                                    break;
                                if(car.startCrossID!=crossID)
                                    continue;
                                if (MTime < car.startTime)
                                    continue;
                                if (runStartCar(car))
                                    iterator.remove();
                            }
                            //driveCarInitList(true);
                            putNextTopPriorityCarIntoHashMap(cross, road, carRoad, tempChannel, tempOffset, crossRoadMap);
                        } else {
                            System.out.println("###############ERROR#############");
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
                if (flag == 1)
                    crossMap.put(cross.crossID, true);
            }
            for (Cross crosstmp : crosses) {
                if (!crossMap.get(crosstmp.crossID)) {
                    bAllCrossesDispatch = false;
                    break;
                }
            }
            if (bAllCrossesDispatch) {//所有路口调度结束
                return true;
            }
            //TODO：卡死时候强制退出
            if (countMove == 0) {
                System.out.println("我进入了卡死");
                return false;
            }
        }
    }

    private void driveCarInitList(boolean isOnlyPriority) {
        for (Road road : roads) {
            Iterator<Car> iterator = road.initCarList.iterator();
            while (iterator.hasNext()) {
                Car car = iterator.next();
                car.getFistChoice(roadHashMap);
                if (isOnlyPriority && !car.isPriorityCar)
                    break;
                if (MTime < car.startTime)
                    continue;
                if (runStartCar(car))
                    iterator.remove();
            }
        }
    }

    private boolean runStartCar(Car car) {
        try {
            Car[][] carOnRoad = car.getCarArray();
            for (int j = 0; j < car.nowRoad.channelCount; j++) {
                if (carOnRoad[j][car.nowRoad.roadLength - 1] == null) {
                    car.nowRoadChannel = j;
                    ForwordInfo forwordInfo = car.getTypeOfForwordCar();
                    if (forwordInfo.forwardStatus == car.FORWORD_HAS_NO_CAR || forwordInfo.forwardStatus == car.FORWORD_HAS_END_CAR) {
                        if (car.isPriorityCar && priorityFirstStartTime == -1) {
                            priorityFirstStartTime = MTime;
                        }
                        car.isStart = 2;
                        car.flag = car.END_STATUS;
                        car.isFinishedAtTimePiece = true;
                        carNumInRoad++;
                        carOnRoadHash.add(car.carID);
                        car.roadOffset = forwordInfo.toPosition;
                        carOnRoad[j][car.roadOffset] = car;
                        return true;
                    } else if (forwordInfo.forwardStatus == car.FORWORD_HAS_WAIT_CAR) {
                        return false;
                    }
                } else if (carOnRoad[j][car.nowRoad.roadLength - 1].flag == car.WAIT_STATUS) {
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            System.out.println("Error1");
        }
        return false;
    }


    private boolean setCarStatusWhenThroughCross(Car car, Cross nowCross, int stepCounts) {//当车要通过路口时，设置该车的最终状态（进入下一道路的某一车道或原地等待或者收到下一道路限速的影响，挪到原车道的0号位置，）。
        Car[][] nextRoad = null;
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
                        return true;
                    }
                }
            }
            if (nowCross.crossID == car.nowRoad.toCross.crossID) {
                car.nowRoad.carOnRoad[car.nowRoadChannel][car.roadOffset] = null;
            } else {
                car.nowRoad.carOnDuplexRoad[car.nowRoadChannel][car.roadOffset] = null;
            }
            int newOffset = car.nextRoad.roadLength - stepCounts;
            car.flag = Car.END_STATUS;
            car.nowRoad = car.nextRoad;//更新车的当前路径为下一条路径
            car.nextRoad = null;  //未知状态,下一条路径需要重新选择
            car.roadOffset = newOffset;
            car.nowRoadChannel = i;
            nextRoad[car.nowRoadChannel][newOffset] = car;
            car.isForwardRoad = car.nowRoad.fromCross.crossID == nowCross.crossID;
            car.isFinishedAtTimePiece = true;//是否一个时间片完成调度
            countMove++;
            return true;
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
        priCars = new ArrayList<>();
        for (Car car : cars) {
            if (car.isStart == Car.GONE && !car.isFinish)
                car.flag = Car.WAIT_STATUS;
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
        if (forwordInfo.forwardStatus == Car.FORWORD_NEED_THROUGH_CROSS) {
            car.flag = Car.WAIT_STATUS;
        } else if (forwordInfo.forwardStatus == Car.FORWORD_HAS_NO_CAR) {
            carOnRoad[car.nowRoadChannel][forwordInfo.toPosition] = car;
            car.flag = Car.END_STATUS;
            carOnRoad[car.nowRoadChannel][car.roadOffset] = null;
            car.roadOffset = forwordInfo.toPosition;
            car.isFinishedAtTimePiece = true;
        } else if (forwordInfo.forwardStatus == Car.FORWORD_HAS_END_CAR) {
            carOnRoad[car.nowRoadChannel][forwordInfo.toPosition] = car;
            car.flag = Car.END_STATUS;
            carOnRoad[car.nowRoadChannel][car.roadOffset] = null;
            car.roadOffset = forwordInfo.toPosition;
            car.isFinishedAtTimePiece = true;
        } else if (forwordInfo.forwardStatus == Car.FORWORD_HAS_WAIT_CAR) {
            car.flag = Car.WAIT_STATUS;
        }
    }
}